/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.query.text ;

import java.io.IOException ;
import java.util.*;
import java.util.Map.Entry ;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.RDFDatatype ;
import org.apache.jena.datatypes.TypeMapper ;
import org.apache.jena.datatypes.xsd.XSDDatatype ;
import org.apache.jena.graph.Node ;
import org.apache.jena.graph.NodeFactory ;
import org.apache.jena.query.text.analyzer.IndexingMultilingualAnalyzer;
import org.apache.jena.query.text.analyzer.MultilingualAnalyzer;
import org.apache.jena.query.text.analyzer.QueryMultilingualAnalyzer;
import org.apache.jena.query.text.analyzer.Util;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.lucene.analysis.Analyzer ;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer ;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper ;
import org.apache.lucene.analysis.standard.StandardAnalyzer ;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException ;
import org.apache.lucene.queryparser.classic.QueryParser ;
import org.apache.lucene.queryparser.classic.QueryParserBase ;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser ;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

public class TextIndexLucene implements TextIndex {
    private static Logger          log      = LoggerFactory.getLogger(TextIndexLucene.class) ;

    private static int             MAX_N    = 10000 ;
    // prefix for storing datatype URIs in the index, to distinguish them from language tags
    private static final String    DATATYPE_PREFIX = "^^";

    private static final String    RIGHT_ARROW = "\u21a6";
    private static final String    LEFT_ARROW  = "\u21a4";
    private static final String    DIVIDES  =    "\u2223";
    private static final String    Z_MORE_SEPS = "([\\p{Z}\u0f0b\0f0c\0f0d\180e]*?)";

    public static final FieldType  ftIRI ;
    static {
        ftIRI = new FieldType() ;
        ftIRI.setTokenized(false) ;
        ftIRI.setStored(true) ;
        ftIRI.setIndexOptions(IndexOptions.DOCS);
        ftIRI.freeze() ;
    }
    public static final FieldType  ftString = StringField.TYPE_NOT_STORED ;

    private final EntityDefinition docDef ;
    private final Directory        directory ;
    private final Analyzer         indexAnalyzer ;
    private       Analyzer         defaultAnalyzer ;
    private       Map<String, Analyzer> analyzerPerField;
    private final Analyzer         queryAnalyzer ;
    private final String           queryParserType ;
    private final FieldType        ftText ;
    private final FieldType        ftTextNotStored ;      // used for lang derived fields
    private final FieldType        ftTextStoredNoIndex ;  // used for lang derived fields
    private final boolean          isMultilingual ;
    private final int              maxBasicQueries ;
    private final boolean          ignoreIndexErrors ;

    private Map<String, Analyzer> multilingualQueryAnalyzers = new HashMap<>();

    // The IndexWriter can't be final because we may have to recreate it if rollback() is called.
    // However, it needs to be volatile in case the next write transaction is on a different thread,
    // but we do not need locking because we are assuming that there can only be one writer
    // at a time (enforced elsewhere).
    private volatile IndexWriter   indexWriter ;

    /**
     * Constructs a new TextIndexLucene.
     *
     * @param directory The Lucene Directory for the index
     * @param config The config definition for the index instantiation.
     */
    public TextIndexLucene(Directory directory, TextIndexConfig config) {
        this.directory = directory ;
        this.docDef = config.getEntDef() ;

        this.maxBasicQueries = config.getMaxBasicQueries();

        this.isMultilingual = config.isMultilingualSupport();
        if (this.isMultilingual &&  config.getEntDef().getLangField() == null) {
            //multilingual index cannot work without lang field
            docDef.setLangField("lang");
        }

        this.ignoreIndexErrors = config.ignoreIndexErrors ;


        // create the analyzer as a wrapper that uses KeywordAnalyzer for
        // entity and graph fields and the configured analyzer(s) for all other
        analyzerPerField = new HashMap<>() ;
        analyzerPerField.put(docDef.getEntityField(), new KeywordAnalyzer()) ;
        if ( docDef.getGraphField() != null )
            analyzerPerField.put(docDef.getGraphField(), new KeywordAnalyzer()) ;
        if ( docDef.getLangField() != null )
            analyzerPerField.put(docDef.getLangField(), new KeywordAnalyzer()) ;

        for (String field : docDef.fields()) {
            Analyzer _analyzer = docDef.getAnalyzer(field);
            if (_analyzer != null) {
                analyzerPerField.put(field, _analyzer);
            }
        }

        defaultAnalyzer = (null != config.getAnalyzer()) ? config.getAnalyzer() : new StandardAnalyzer();
        Analyzer indexDefault = defaultAnalyzer;
        Analyzer queryDefault = defaultAnalyzer;
        if (this.isMultilingual) {
            queryDefault = new MultilingualAnalyzer(defaultAnalyzer);
            indexDefault = Util.usingIndexAnalyzers() ? new IndexingMultilingualAnalyzer(defaultAnalyzer) : queryDefault;
        }
        this.indexAnalyzer = new PerFieldAnalyzerWrapper(indexDefault, analyzerPerField) ;
        this.queryAnalyzer = (null != config.getQueryAnalyzer()) ? config.getQueryAnalyzer() : new PerFieldAnalyzerWrapper(queryDefault, analyzerPerField) ;
        this.queryParserType = config.getQueryParser() ;
        log.debug("TextIndexLucene defaultAnalyzer: {}, indexAnalyzer: {}, queryAnalyzer: {}, queryParserType: {}", defaultAnalyzer, indexAnalyzer, queryAnalyzer, queryParserType);
        this.ftText = config.isValueStored() ? TextField.TYPE_STORED : TextField.TYPE_NOT_STORED ;
        // the following is used for lang derived fields
        this.ftTextNotStored = TextField.TYPE_NOT_STORED ;
        this.ftTextStoredNoIndex = new FieldType();
        this.ftTextStoredNoIndex.setIndexOptions(IndexOptions.NONE);
        this.ftTextStoredNoIndex.setStored(true);
        this.ftTextStoredNoIndex.freeze();
        if (config.isValueStored() && docDef.getLangField() == null)
            log.warn("Values stored but langField not set. Returned values will not have language tag or datatype.");

        openIndexWriter();
    }

    private void openIndexWriter() {
        IndexWriterConfig wConfig = new IndexWriterConfig(indexAnalyzer) ;
        try
        {
            indexWriter = new IndexWriter(directory, wConfig) ;
            // Force a commit to create the index, otherwise querying before writing will cause an exception
            indexWriter.commit();
        }
        catch (IndexFormatTooOldException e) {
            throw new TextIndexException("jena-text/Lucene cannot use indexes created before Jena 3.3.0. "
                + "Please rebuild your text index using jena.textindexer from Jena 3.3.0 or above.", e);
        }
        catch (IOException e)
        {
            throw new TextIndexException("openIndexWriter", e) ;
        }
    }

    public Directory getDirectory() {
        return directory ;
    }

    public Analyzer getAnalyzer() {
        return indexAnalyzer ;
    }

    public Analyzer getQueryAnalyzer() {
        return queryAnalyzer ;
    }

    public IndexWriter getIndexWriter() {
        return indexWriter;
    }

    @Override
    public void prepareCommit() {
        try {
            indexWriter.prepareCommit();
        }
        catch (IOException e) {
            throw new TextIndexException("prepareCommit", e);
        }
    }

    @Override
    public void commit() {
        try {
            indexWriter.commit();
        }
        catch (IOException e) {
            throw new TextIndexException("commit", e);
        }
    }

    @Override
    public void rollback() {
        IndexWriter idx = indexWriter;
        indexWriter = null;
        try {
            idx.rollback();
        }
        catch (IOException e) {
            throw new TextIndexException("rollback", e);
        }

        // The rollback will close the indexWriter, so we need to reopen it
        openIndexWriter();
    }

    @Override
    public void close() {
        try {
            indexWriter.close() ;
        }
        catch (IOException ex) {
            throw new TextIndexException("close", ex) ;
        }
    }

    @Override public void updateEntity(Entity entity) {
        if ( log.isDebugEnabled() )
            if (log.isTraceEnabled() && entity != null)
                log.trace("Update entity: " + entity.toStringDetail()) ;
            else
                log.debug("Update entity: " + entity) ;
        try {
            updateDocument(entity);
        } catch (IOException e) {
            throw new TextIndexException("updateEntity", e) ;
        }
    }

    protected void updateDocument(Entity entity) throws IOException {
        Document doc = doc(entity);
        Term term = new Term(docDef.getEntityField(), entity.getId());
        try {
            indexWriter.updateDocument(term, doc) ;
        } catch (Exception ex) {
            log.error("Error updating {} with term: {} message: {}", doc, term, ex.getMessage());
            if (ignoreIndexErrors) {
                return;
            } else {
                throw ex; // the original behavior
            }
        }
        log.trace("updated: {}", doc) ;
    }

    @Override
    public void addEntity(Entity entity) {
        if ( log.isDebugEnabled() )
            if (log.isTraceEnabled() && entity != null)
                log.trace("Add entity: " + entity.toStringDetail()) ;
            else
                log.debug("Add entity: " + entity) ;
        try {
            addDocument(entity);
        }
        catch (IOException e) {
            throw new TextIndexException("addEntity", e) ;
        }
    }

    protected void addDocument(Entity entity) throws IOException {
        Document doc = doc(entity) ;
        try {
            indexWriter.addDocument(doc) ;
        } catch (Exception ex) {
            log.error("Error adding {} message: {}", doc, ex.getMessage());
            if (ignoreIndexErrors) {
                return;
            } else {
                throw ex; // the original behavior
            }
        }
        log.trace("added: {}", doc) ;
    }

    @Override
    public void deleteEntity(Entity entity) {
        if (docDef.getUidField() == null)
            return;

        if ( log.isDebugEnabled() )
            if (log.isTraceEnabled() && entity != null)
                log.trace("Delete entity: " + entity.toStringDetail()) ;
            else
                log.debug("Delete entity: "+entity) ;
        try {
            Map<String, Object> map = entity.getMap();
            String property = map.keySet().iterator().next();
            String value = (String)map.get(property);
            String hash = entity.getChecksum(property, value);
            Term uid = new Term(docDef.getUidField(), hash);
            indexWriter.deleteDocuments(uid);

        } catch (Exception e) {
            throw new TextIndexException("deleteEntity", e) ;
        }
    }

    protected Document doc(Entity entity) {
        Document doc = new Document() ;
        Field entField = new Field(docDef.getEntityField(), entity.getId(), ftIRI) ;
        doc.add(entField) ;

        String graphField = docDef.getGraphField() ;
        if ( graphField != null ) {
            Field gField = new Field(graphField, entity.getGraph(), ftIRI) ;
            doc.add(gField) ;
        }

        String langField = docDef.getLangField() ;
        String uidField = docDef.getUidField() ;

        for ( Entry<String, Object> e : entity.getMap().entrySet() ) {
            String field = e.getKey();
            String value = (String) e.getValue();
            FieldType ft = (docDef.getNoIndex(field)) ? ftTextStoredNoIndex : ftText ;
            doc.add( new Field(field, value, ft) );
            if (langField != null) {
                String lang = entity.getLanguage();
                RDFDatatype datatype = entity.getDatatype();
                if (lang != null && !"".equals(lang)) {
                    doc.add(new Field(langField, lang, StringField.TYPE_STORED));
                    if (this.isMultilingual) {
                        // add a field that uses a language-specific analyzer via MultilingualAnalyzer
                        doc.add(new Field(field + "_" + lang, value, ftTextNotStored));
                        // add fields for any defined auxiliary indexes
                        List<String> auxIndexes = Util.getAuxIndexes(lang);
                        if (auxIndexes != null) {
                            for (String auxTag : auxIndexes) {
                                doc.add(new Field(field + "_" + auxTag, value, ftTextNotStored));
                            }
                        }
                    }
                } else if (datatype != null && !datatype.equals(XSDDatatype.XSDstring)) {
                    // for non-string and non-langString datatypes, store the datatype in langField
                    doc.add(new Field(langField, DATATYPE_PREFIX + datatype.getURI(), StringField.TYPE_STORED));
                }
            }
            if (uidField != null) {
                String hash = entity.getChecksum(field, value);
                doc.add(new Field(uidField, hash, StringField.TYPE_STORED));
            }
        }
        return doc ;
    }

    @Override
    public Map<String, Node> get(String uri) {
        try {
            IndexReader indexReader = DirectoryReader.open(directory);
            List<Map<String, Node>> x = get$(indexReader, uri) ;
            if ( x.size() == 0 )
                return null ;
            // if ( x.size() > 1)
            // throw new TextIndexException("Multiple entires for "+uri) ;
            return x.get(0) ;
        }
        catch (Exception ex) {
            throw new TextIndexException("get", ex) ;
        }
    }

    private Query parseQuery(String queryString, Analyzer analyzer) throws ParseException {
        Query query = null;
        QueryParser qp = null;

        switch(queryParserType) {
            case "QueryParser":
                // Drop to default
                break;
            case "SurroundQueryParser":
                try {
                    query = org.apache.lucene.queryparser.surround.parser.QueryParser.parse(queryString).makeLuceneQueryField(docDef.getPrimaryField(), new BasicQueryFactory(this.maxBasicQueries));
                } catch(org.apache.lucene.queryparser.surround.parser.ParseException e) {
                    throw new ParseException(e.getMessage());
                }
                return query;
            case "ComplexPhraseQueryParser":
                qp = new ComplexPhraseQueryParser(docDef.getPrimaryField(), analyzer);
                break;
            case "AnalyzingQueryParser": // since Lucene 7 analyzing is done by QueryParser
                log.warn("Deprecated query parser type 'AnalyzingQueryParser'. Defaulting to standard QueryParser");
                break;
            default:
                log.warn("Unknown query parser type '" + queryParserType + "'. Defaulting to standard QueryParser");
        }

        if (qp == null) {
            qp = new QueryParser(docDef.getPrimaryField(), analyzer);
        }
        qp.setAllowLeadingWildcard(true);

        // Some analyzers are not thread safe, at least when used via ConfigurableAnalyzer
        // Could be more selective here about when to synchronize but the suspect analyzer
        // can appear at several places in the wrapped nest of analyzers so being conservative
        synchronized (this) {
            query = qp.parse(queryString);
            return query;
        }
    }

    private List<Map<String, Node>> get$(IndexReader indexReader, String uri) throws ParseException, IOException {
        String escaped = QueryParserBase.escape(uri) ;
        String qs = docDef.getEntityField() + ":" + escaped ;
        Query query = parseQuery(qs, queryAnalyzer) ;
        IndexSearcher indexSearcher = new IndexSearcher(indexReader) ;
        ScoreDoc[] sDocs = indexSearcher.search(query, 1).scoreDocs ;
        List<Map<String, Node>> records = new ArrayList<>() ;
        StoredFields sFields = indexSearcher.storedFields();

        for ( ScoreDoc sd : sDocs ) {
            Document doc = sFields.document(sd.doc);

            String[] x = doc.getValues(docDef.getEntityField()) ;
            if ( x.length != 1 ) {}
            String uriStr = x[0] ;
            Map<String, Node> record = new HashMap<>() ;
            Node entity = NodeFactory.createURI(uriStr) ;
            record.put(docDef.getEntityField(), entity) ;

            for ( String f : docDef.fields() ) {
                // log.info("Field: "+f) ;
                String[] values = doc.getValues(f) ;
                for ( String v : values ) {
                    Node n = entryToNode(v) ;
                    record.put(f, n) ;
                }
                records.add(record) ;
            }
        }
        return records ;
    }

    @Override
    public List<TextHit> query(Node property, String qs, String graphURI, String lang) {
        return query(property, qs, graphURI, lang, MAX_N) ;
    }

    @Override
    public List<TextHit> query(Node property, String qs, String graphURI, String lang, int limit) {
        return query(property, qs, graphURI, lang, limit, null) ;
    }

    @Override
    public List<TextHit> query(Node propNode, String qs, String graphURI, String lang, int limit, String highlight) {
        List<Resource> props = new ArrayList<>();
        if (propNode != null) {
            props.add(ResourceFactory.createProperty(propNode.getURI()));
        }
        return query((String) null, props, qs, graphURI, lang, limit, highlight);    }

    @Override
    public List<TextHit> query(String subjectUri, Node propNode, String qs, String graphURI, String lang, int limit, String highlight) {
        List<Resource> props = new ArrayList<>();
        if (propNode != null) {
            props.add(ResourceFactory.createProperty(propNode.getURI()));
        }
        return query(subjectUri, props, qs, graphURI, lang, limit, highlight);
    }

    @Override
    public List<TextHit> query(List<Resource> props, String qs, String graphURI, String lang, int limit, String highlight) {
        return query((String) null, props, qs, graphURI, lang, limit, highlight);
    }

    @Override
    public List<TextHit> query(Node subj, List<Resource> props, String qs, String graphURI, String lang, int limit, String highlight) {
        String subjectUri = subj == null || Var.isVar(subj) || !subj.isURI() ? null : subj.getURI();
        return query(subjectUri, props, qs, graphURI, lang, limit, highlight);
    }

    @Override
    public List<TextHit> query(String subjectUri, List<Resource> props, String qs, String graphURI, String lang, int limit, String highlight) {
        try (IndexReader indexReader = DirectoryReader.open(directory)) {
            return query$(indexReader, props, qs, addUriPredicate(subjectUri), graphURI, lang, limit, highlight) ;
        }
        catch (ParseException ex) {
            throw new TextIndexParseException(qs, ex.getMessage()) ;
        }
        catch (Exception ex) {
            throw new TextIndexException("query", ex) ;
        }
    }

    //In case of making text search query for concrete subject
    //adding uri predicate will make query much more efficient
    private UnaryOperator<Query> addUriPredicate(String subjectUri) {
        if (subjectUri != null) {
            return (Query textQuery) -> {
                String uriField = docDef.getEntityField();
                return new BooleanQuery.Builder()
                        .add(textQuery, BooleanClause.Occur.MUST)
                        .add(new TermQuery(new Term(uriField, subjectUri)), BooleanClause.Occur.FILTER)
                        .build();
            };
        } else {
            return  UnaryOperator.identity();
        }
    }

    private String getDocField(Document doc, List<String> fields) {
        for (String field : fields) {
            if (doc.get(field) != null) {
                return field;
            }
        }

        return null;
    }

    private List<TextHit> simpleResults(ScoreDoc[] sDocs, IndexSearcher indexSearcher, Query query, List<String> fields)
            throws IOException
    {
        List<TextHit> results = new ArrayList<>() ;
        StoredFields sFields = indexSearcher.storedFields();

        for ( ScoreDoc sd : sDocs ) {
            Document doc = sFields.document(sd.doc);
            log.trace("simpleResults[{}]: fields: {} doc: {}", sd.doc, fields, doc) ;
            String entity = doc.get(docDef.getEntityField()) ;

            Node literal = null;

            String field = getDocField(doc, fields) ;
            String lexical = doc.get(field);
            Collection<Node> props = docDef.getPredicates(field);
            Node prop = props.isEmpty() ? null : props.iterator().next();

            if (lexical != null) {
                String doclang = doc.get(docDef.getLangField()) ;
                if (doclang != null) {
                    if (doclang.startsWith(DATATYPE_PREFIX)) {
                        String datatype = doclang.substring(DATATYPE_PREFIX.length());
                        TypeMapper tmap = TypeMapper.getInstance();
                        literal = NodeFactory.createLiteralDT(lexical, tmap.getSafeTypeByName(datatype));
                    } else {
                        literal = NodeFactory.createLiteralLang(lexical, doclang);
                    }
                } else {
                    literal = NodeFactory.createLiteralString(lexical);
                }
            }

            String graf = docDef.getGraphField() != null ? doc.get(docDef.getGraphField()) : null ;
            Node graph = graf != null ? TextQueryFuncs.stringToNode(graf) : null;

            Node subject = TextQueryFuncs.stringToNode(entity) ;
            TextHit hit = new TextHit(subject, sd.score, literal, graph, prop);
            results.add(hit) ;
        }

        return results ;
    }

    class HighlightOpts {
        int maxFrags = 3;
        int fragSize = 128;
        String start = RIGHT_ARROW;
        String end = LEFT_ARROW;
        String fragSep = DIVIDES;
        String patternExpr = null;
        boolean joinHi = true;
        boolean joinFrags = true;

        public HighlightOpts(String optStr) {
            String[] opts = optStr.trim().split("\\|");
            for (String opt : opts) {
                opt = opt.trim();
                if (opt.startsWith("m:")) {
                    try {
                        maxFrags = Integer.parseInt(opt.substring(2));
                    } catch (Exception ex) { }
                } else if (opt.startsWith("z:")) {
                    try {
                        fragSize = Integer.parseInt(opt.substring(2));
                    } catch (Exception ex) { }
                } else if (opt.startsWith("s:")) {
                    start = opt.substring(2);
                } else if (opt.startsWith("e:")) {
                    end = opt.substring(2);
                } else if (opt.startsWith("f:")) {
                    fragSep = opt.substring(2);
                } else if (opt.startsWith("jh:")) {
                    String v = opt.substring(3);
                    if ("n".equals(v)) {
                        joinHi = false;
                    }
                } else if (opt.startsWith("jf:")) {
                    String v = opt.substring(3);
                    if ("n".equals(v)) {
                        joinFrags = false;
                    }
                }
            }
            patternExpr = end+Z_MORE_SEPS+start;
        }
    }

    private String frags2string(final TextFragment[] frags, final HighlightOpts opts) {
    	final StringBuilder sb = new StringBuilder();
    	String sep = "";

        for (final TextFragment f : frags) {
        	final String fragStr = f.toString();
        	log.trace("found fragment {}", f);
        	sb.append(sep);
            sb.append(opts.joinHi ? fragStr.replaceAll(opts.patternExpr, "$1") : fragStr);
            sep = opts.fragSep;
        }

        return sb.toString();
    }

    private List<TextHit> highlightResults(ScoreDoc[] sDocs, IndexSearcher indexSearcher, Query query, List<String> fields, String highlight, String queryLang)
            throws IOException, InvalidTokenOffsetsException {
        List<TextHit> results = new ArrayList<>() ;

        HighlightOpts opts = new HighlightOpts(highlight);

        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter(opts.start, opts.end);
        Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter(opts.fragSize));
        StoredFields sFields = indexSearcher.storedFields();

        for ( ScoreDoc sd : sDocs ) {
            Document doc = sFields.document(sd.doc);
            String entity = doc.get(docDef.getEntityField()) ;

            Node literal = null;
            String field = getDocField(doc, fields) ;
            String lexical = doc.get(field);
            Collection<Node> props = docDef.getPredicates(field);
            Node prop = props.isEmpty() ? null : props.iterator().next(); // pick one - should be only one normally

            String docLang = doc.get(docDef.getLangField()) ;
            String effectiveField = queryLang != null ? field + "_" + Util.getEffectiveLang(docLang, queryLang) : field;
            log.trace("highlightResults[{}]: {}, field: {}, lexical: {}, docLang: {}, effectiveField: {}", sd.doc, doc, field, lexical, docLang, effectiveField) ;
            if (lexical != null) {
                TokenStream tokenStream = indexAnalyzer.tokenStream(effectiveField, lexical);
                log.trace("tokenStream: {}", tokenStream.toString());
                TextFragment[] frags = highlighter.getBestTextFragments(tokenStream, lexical, opts.joinFrags, opts.maxFrags);
                String rez = frags2string(frags, opts);
                log.trace("result: {}, #frags: {}", rez, frags.length) ;
                literal = NodeFactory.createLiteralLang(rez, docLang);
            }

            String graf = docDef.getGraphField() != null ? doc.get(docDef.getGraphField()) : null ;
            Node graph = graf != null ? TextQueryFuncs.stringToNode(graf) : null;

            Node subject = TextQueryFuncs.stringToNode(entity) ;
            TextHit hit = new TextHit(subject, sd.score, literal, graph, prop);
            results.add(hit) ;
        }
        return results ;
    }

    private Analyzer getQueryAnalyzer(boolean usingSearchFor, String lang) {
        if (usingSearchFor) {
            Analyzer qa = multilingualQueryAnalyzers.get(lang);
            if (qa == null) {
                qa = new PerFieldAnalyzerWrapper(new QueryMultilingualAnalyzer(defaultAnalyzer, lang), analyzerPerField);
                multilingualQueryAnalyzers.put(lang, qa);
            }
            return qa;
        } else {
            return queryAnalyzer;
        }
    }

    private String composeQField(String qs, String textField, String lang, boolean usingSearchFor, List<String> searchForTags) {
        String textClause = "";
        String fieldGroupingQueryString = "(" + qs + ")";

        if (usingSearchFor) {
            for (String tag : searchForTags) {
                String tf = textField + "_" + tag;
                textClause += tf + ": " + fieldGroupingQueryString + " ";
            }
        } else {
            if (this.isMultilingual && StringUtils.isNotEmpty(lang) && !lang.equals("none")) {
                textField += "_" + lang;
            }
            textClause = textField + ": " + fieldGroupingQueryString + " ";
        }

        return textClause;
    }

    private List<TextHit> query$(IndexReader indexReader, List<Resource> props, String qs, UnaryOperator<Query> textQueryExtender, String graphURI, String lang, int limit, String highlight)
            throws ParseException, IOException, InvalidTokenOffsetsException
    {
        List<String> textFields = new ArrayList<>();
        String qString = "";
        String langField = getDocDef().getLangField();

        // the list will have at least the lang as an element
        // UNLESS lang is not present in the original PF call
        List<String> searchForTags = Util.getSearchForTags(lang);
        boolean usingSearchFor = Util.usingSearchFor(lang);

        // The set will reduce lucene text field expressions
        // to unique expressions
        Set<String> uniquePropListQueryStrings = new LinkedHashSet<>();

        if (props.isEmpty()) {
            // we got here via
            //    ?s text:query "some query string"
            // or
            //    ?s text:query ( "some query string" ... )
            // so we just need the qs and process additional args below
            // the qs may be a mutli-field query or just a simple query
            qString = qs + " ";
            log.trace("query$ processed EMPTY LIST of properties: {}; Lucene queryString: {}; textFields: {}", props, qString, textFields) ;
        } else {
            // otherwise there are one or more properties to search over
            // possibly with a searchFor list for each property
            // we are guaranteed that the props are all indexed by the way things are called from
            // TextQueryPF which is the only way into this code - DatasetGraphText isn't used anywhere
            // or documented as far as I can tell
            for (Resource prop : props) {
                String textField = docDef.getField(prop.asNode());
                textFields.add(textField);
            }
        }

        log.trace("query$ PROCESSING LIST of properties: {}; Lucene queryString: {}; textFields: {} ", props, qString, textFields) ;
        for (String textField : textFields) {
            uniquePropListQueryStrings.add(composeQField(qs, textField, lang, usingSearchFor, searchForTags));
        }
        qString += String.join("", uniquePropListQueryStrings);

        // we need to check whether there was a lang arg either on the query string
        // or explicitly as an input arg and add it to the qString; otherwise, Lucene
        // won't be able to properly process the query
        if (textFields.isEmpty() && lang != null) {
            qString += composeQField(qs, docDef.getPrimaryField(), lang, usingSearchFor, searchForTags);
        }

        log.trace("query$ PROCESSED LIST of properties: {} with resulting qString: {} ", props, qString) ;

        // add a clause for the lang if not usingSearchFor and there is a defined langFIeld in the config
        if (!usingSearchFor && langField != null && StringUtils.isNotBlank(lang)) {
            qString = "(" + qString + ") AND " + (!lang.equals("none") ? langField+":"+lang : "-"+langField+":*");
            log.trace("query$ ADDING LANG qString: {} ", qString) ;
        }

        if (graphURI != null) {
            String escaped = QueryParserBase.escape(graphURI) ;
            qString = "(" + qString + ") AND " + getDocDef().getGraphField() + ":" + escaped ;
        }

        Analyzer qa = getQueryAnalyzer(usingSearchFor, lang);
        Query textQuery = parseQuery(qString, qa);
        Query query = textQueryExtender.apply(textQuery);

        if ( limit <= 0 )
            limit = MAX_N ;

        log.debug("query$ with LIST: {}; INPUT qString: {}; with queryParserType: {}; parseQuery with {} YIELDS: {}; parsed query: {}; limit: {}", props, qString, queryParserType, qa, textQuery, query, limit) ;

        IndexSearcher indexSearcher = new IndexSearcher(indexReader) ;

        ScoreDoc[] sDocs = indexSearcher.search(query, limit).scoreDocs ;

        // if there were no explicit textFields supplied then Lucene used
        // the default field if defined otherwise Lucene simply interpreted the qs
        // as presented - perhaps with multiple fields indexed on a separate system.
        // In order to handle the results we need to supply the default field to
        // complete the processing in TextQueryPF
        if (textFields.isEmpty()) {
            textFields.add(docDef.getPrimaryField());
        }

        if (highlight != null) {
            return highlightResults(sDocs, indexSearcher, query, textFields, highlight, lang);
        } else {
            return simpleResults(sDocs, indexSearcher, query, textFields);
        }
    }

    @Override
    public EntityDefinition getDocDef() {
        return docDef ;
    }

    private Node entryToNode(String v) {
        return NodeFactory.createLiteralString(v) ;
    }
}
