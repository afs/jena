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

package org.apache.jena.riot.system;

import static org.apache.jena.riot.RDFLanguages.NQUADS;
import static org.apache.jena.riot.RDFLanguages.NTRIPLES;
import static org.apache.jena.riot.RDFLanguages.RDFJSON;
import static org.apache.jena.riot.RDFLanguages.sameLang;

import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.lib.Chars;
import org.apache.jena.atlas.lib.EscapeStr;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIs;
import org.apache.jena.irix.IRIx;
import org.apache.jena.irix.IRIxResolver;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.*;
import org.apache.jena.riot.lang.LabelToNode;
import org.apache.jena.riot.writer.DirectiveStyle;
import org.apache.jena.riot.writer.WriterGraphRIOTBase;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Misc RIOT code
 */
public class RiotLib {
    // ---- BlankNode skolemization as IRIs
    private final static boolean skolomizedBNodes = ARQ.isTrueOrUndef(ARQ.constantBNodeLabels);
    /**
     * BNodes as pseudo-IRIs
     */
    private final static String bNodeLabelStart = "_:";

    /**
     * "Skolemize" to a node.
     * Returns a Node_URI.
     */
    public static Node blankNodeToIri(Node node) {
        if ( node.isBlank() )
            return NodeFactory.createURI(blankNodeToIriString(node));
        return node;
    }

    /**
     * "Skolemize" to a string.
     */
    public static String blankNodeToIriString(Node node) {
        if ( node.isBlank() ) {
            String x = node.getBlankNodeLabel();
            return bNodeLabelStart + x;
        }
        if ( node.isURI() )
            return node.getURI();
        throw new RiotException("Not a blank node or URI");
    }

    /**
     * Convert an ARQ-encoded blank node URI to a blank node, otherwise return the argument node unchanged.
     */
    public static Node fromIRIorBNode(Node node) {
        if ( ! node.isURI() )
            return node;
        String uristr = node.getURI();
        return createIRIorBNode(uristr);
    }

    /**
     * Implement {@code <_:....>} as a "Node IRI"
     * that is, use the given label as the BNode internal label.
     * Use with care.
     * Returns a Node_URI.
     */
    public static Node createIRIorBNode(String str) {
        // Is it a bNode label? i.e. <_:xyz>
        if ( skolomizedBNodes && isBNodeIRI(str) ) {
            String s = str.substring(bNodeLabelStart.length());
            return NodeFactory.createBlankNode(s);
        }
        return NodeFactory.createURI(str);
    }

    /**
     * Test whether a IRI is an ARQ-encoded blank node.
     */
    public static boolean isBNodeIRI(String iri) {
        return iri.startsWith(bNodeLabelStart);
    }

    /**
     * Test whether a node is an ARQ-encoded blank node IRI.
     */
    public static boolean isBNodeIRI(Node node) {
        if ( ! node.isURI() )
            return false;
        return isBNodeIRI(node.getURI());
    }

    private static final String URI_PREFIX_FIXUP = "local:";

    // These two must be in-step.
    /**
     * Function applied to undefined prefixes to convert to a URI string
     */
    public static final Function<String, String> fixupPrefixes = URI_PREFIX_FIXUP::concat;

    /**
     * Function to test for undefined prefix URIs
     */
    public static final Predicate<String> testFixupedPrefixURI = (x) -> x.startsWith(URI_PREFIX_FIXUP);

    /**
     * Test whether a IRI is an ARQ-encoded blank node.
     */
    public static boolean isPrefixIRI(String iri) {
        return testFixupedPrefixURI.test(iri);
    }

    /**
     * Convert an prefix name (qname) to an IRI, for when the prefix is not defined.
     *
     * @see ARQ#fixupUndefinedPrefixes
     */
    public static String fixupPrefixIRI(String prefix, String localPart) {
        return fixupPrefixIRI(prefix + ":" + localPart);
    }

    /**
     * Convert an prefix name (qname) to an IRI, for when the prefix is not defined.
     *
     * @see ARQ#fixupUndefinedPrefixes
     */
    public static String fixupPrefixIRI(String prefixedName) {
        return fixupPrefixes.apply(prefixedName);
    }

    /**
     * Parse a string to get one Node (the first token in the string)
     */
    public static Node parse(String string) {
        return NodeFactoryExtra.parseNode(string, null);
    }

    public static ParserProfile profile(Lang lang, String baseIRI, ErrorHandler handler) {
        if ( sameLang(NTRIPLES, lang) || sameLang(NQUADS, lang) ) {
            boolean checking = SysRIOT.isStrictMode();
            // If strict mode, do checking e.g. URIs
            return profile(baseIRI, false, checking, handler);
        }
        if ( sameLang(RDFJSON, lang) )
            return profile(baseIRI, false, true, handler);
        return profile(baseIRI, true, true, handler);
    }

    /**
     * Create a parser profile for the given setup
     */
    private static ParserProfile profile(String baseIRI, boolean resolveIRIs, boolean checking, ErrorHandler handler) {
        LabelToNode labelToNode = SyntaxLabels.createLabelToNode();
        IRIx base = resolveIRIs
                ? IRIs.resolveIRI(baseIRI)
                : IRIx.create(baseIRI);
        IRIxResolver resolver = IRIxResolver
                .create(base)
                .resolve(resolveIRIs)
                .allowRelative(false)
                .build();
        return RiotLib.createParserProfile(factoryRDF(labelToNode), handler, resolver, checking);
    }

    /**
     * Create a new (not influenced by anything else) {@code FactoryRDF}
     * using the label to blank node scheme provided.
     */
    public static FactoryRDF factoryRDF(LabelToNode labelMapping) {
        return new FactoryRDFCaching(FactoryRDFCaching.DftNodeCacheSize, labelMapping);
    }

    /**
     * Create a new (not influenced by anything else) {@code FactoryRDF}
     * using the default label to blank node scheme.
     */
    public static FactoryRDF factoryRDF() {
        return factoryRDF(SyntaxLabels.createLabelToNode());
    }

    /**
     * Create a {@link ParserProfile} with default settings.
     */
    public static ParserProfile dftProfile() {
        return createParserProfile(RiotLib.factoryRDF(), ErrorHandlerFactory.errorHandlerStd, true);
    }

    /**
     * Create a {@link ParserProfile} with default settings, and a specific error handler.
     */
    public static ParserProfile createParserProfile(FactoryRDF factory, ErrorHandler errorHandler, boolean checking) {
        return new CDTAwareParserProfile(factory,
                                    errorHandler,
                                    IRIxResolver.create(IRIs.getSystemBase()).build(),
                                    PrefixMapFactory.create(),
                                    RIOT.getContext().copy(),
                                    checking,
                                    false);
    }

    /**
     * Create a {@link ParserProfile}.
     */
    public static ParserProfile createParserProfile(FactoryRDF factory, ErrorHandler errorHandler,
                                                    IRIxResolver resolver, boolean checking) {
        return new CDTAwareParserProfile(factory,
                errorHandler,
                resolver,
                PrefixMapFactory.create(),
                RIOT.getContext().copy(),
                checking, false);
    }

    /**
     * Get triples with the same subject
     */
    public static Collection<Triple> triplesOfSubject(Graph graph, Node subj) {
        return triples(graph, subj, Node.ANY, Node.ANY);
    }

    /**
     * Get all the triples for the graph.find
     */
    public static List<Triple> triples(Graph graph, Node s, Node p, Node o) {
        List<Triple> acc = new ArrayList<>();
        accTriples(acc, graph, s, p, o);
        return acc;
    }

    /* Count the triples for the graph.find */
    public static long countTriples(Graph graph, Node s, Node p, Node o) {
        ExtendedIterator<Triple> iter = graph.find(s, p, o);
        try {
            return Iter.count(iter);
        } finally {
            iter.close();
        }
    }

    /* Count the matches to a pattern across the dataset  */
    public static long countTriples(DatasetGraph dsg, Node s, Node p, Node o) {
        Iterator<Quad> iter = dsg.find(Node.ANY, s, p, o);
        return Iter.count(iter);
    }

    /**
     * Collect all the matching triples
     */
    public static void accTriples(Collection<Triple> acc, Graph graph, Node s, Node p, Node o) {
        graph.find(s, p, o).forEach(acc::add);
    }

    public static void writeBase(IndentedWriter out, String base) {
        writeBase(out, base, DirectiveStyle.systemDefault);
    }

    public static void writeBase(IndentedWriter out, String base, DirectiveStyle writeStyle) {
        if ( writeStyle == DirectiveStyle.AT )
            writeBaseOldStyle(out, base);
        else
            writeBaseNewStyle(out, base);
    }

    private static void writeBaseNewStyle(IndentedWriter out, String base) {
        if (base != null) {
            out.print("BASE ");
            out.pad(7);   // Align to possible prefixes. 7 is the length of "prefix "
            out.print("<");
            out.print(base);
            out.print(">");
            out.println();
        }
    }

    private static void writeBaseOldStyle(IndentedWriter out, String base) {
        if (base != null) {
            out.print("@base ");
            out.pad(8);   // Align to possible prefixes. 8 is the length of "@prefix "
            out.print("<");
            out.print(base);
            out.print(">");
            out.print(" .");
            out.println();
        }
    }

    public static void writePrefixes(IndentedWriter out, PrefixMap prefixMap) {
        writePrefixes(out, prefixMap, DirectiveStyle.systemDefault);
    }

    /**
     * Write prefixes
     */
    public static void writePrefixes(IndentedWriter out, PrefixMap prefixMap, DirectiveStyle writeStyle) {
        if ( prefixMap != null && !prefixMap.isEmpty() ) {
            int maxPrefixLength = prefixMap.getMapping().keySet().stream()
                    .map(String::length)
                    .max(Comparator.naturalOrder())
                    .orElse(0);
            for (Map.Entry<String, String> e : sortPrefixes(prefixMap)) {
                writePrefix(out, e.getKey(), e.getValue(), writeStyle, maxPrefixLength);
            }
        }
    }

    /**
     * Sort the entries of a prefix map by their key
     */
    private static List<Map.Entry<String, String>> sortPrefixes(PrefixMap prefixMap) {
        List<Map.Entry<String, String>> prefixesMappings = new ArrayList<>(prefixMap.getMapping().entrySet());
        prefixesMappings.sort(Map.Entry.comparingByKey());
        return prefixesMappings;
    }

    /**
     * Write a prefix.
     * Write using {@code @prefix} or {@code PREFIX}.
     */
    public static void writePrefix(IndentedWriter out, String prefix, String uri, DirectiveStyle writeStyle) {
        writePrefix(out, prefix, uri, writeStyle, 0);
    }

    private static void writePrefix(IndentedWriter out, String prefix, String uri, DirectiveStyle writeStyle, int maxPrefixLength) {
        if ( writeStyle == DirectiveStyle.AT )
            writePrefixOldStyle(out, prefix, uri, maxPrefixLength);
        else
            writePrefixNewStyle(out, prefix, uri, maxPrefixLength);
    }

    /**
     * Write prefix, using {@code PREFIX}
     */
    private static void writePrefixNewStyle(IndentedWriter out, String prefix, String uri, int intent) {
        out.print("PREFIX ");
        out.print(prefix);
        out.print(": ");
        out.pad(9 + intent); // 9 is length of "PREFIX : "
        out.print("<");
        out.print(uri);
        out.print(">");
        out.println();
    }

    /**
     * Write prefixes, using {@code @prefix}
     */
    private static void writePrefixOldStyle(IndentedWriter out, String prefix, String uri, int intent) {
        out.print("@prefix ");
        out.print(prefix);
        out.print(": ");
        out.pad(10 + intent); // 10 is length of "@prefix : "
        out.print("<");
        out.print(uri);
        out.print(">");
        out.print(" .");
        out.println();
    }

    /**
     * Write a version.
     * Write using {@code version} or {@code VERSION}.
     */
    public static void writeVersion(IndentedWriter out, String version, DirectiveStyle writeStyle) {
        if ( writeStyle == DirectiveStyle.AT )
            writeVersionOldStyle(out, version);
        else
            writeVersionNewStyle(out, version);
    }

    private static void writeVersionNewStyle(IndentedWriter out, String version) {
        char chQuote = Chars.CH_QUOTE2;
        String escVersion = EscapeStr.stringEsc(version, Chars.CH_QUOTE2);
        out.print("VERSION ");
        out.print(chQuote);
        out.print(escVersion);
        out.print(chQuote);
        out.println();
    }

    private static void writeVersionOldStyle(IndentedWriter out, String version) {
        char chQuote = Chars.CH_QUOTE2;
        String escVersion = EscapeStr.stringEsc(version, Chars.CH_QUOTE2);
        out.print("@version ");
        out.print(chQuote);
        out.print(escVersion);
        out.print(chQuote);
        out.print(" .");
        out.println();
    }

    /**
     * IndentedWriter over a java.io.Writer (better to use an IndentedWriter over an OutputStream)
     */
    public static IndentedWriter create(Writer writer) {
        return new IndentedWriterWriter(writer);
    }

    public static WriterGraphRIOTBase adapter(WriterDatasetRIOT writer) {
        return new WriterAdapter(writer);
    }

    /**
     * Hidden to direct program to using OutputStreams (for RDF, that gets the charset right)
     */
    private static class IndentedWriterWriter extends IndentedWriter {
        IndentedWriterWriter(Writer w) {
            super(w);
        }
    }

    private static class WriterAdapter extends WriterGraphRIOTBase {
        private WriterDatasetRIOT writer;

        WriterAdapter(WriterDatasetRIOT writer) {
            this.writer = writer;
        }

        @Override
        public Lang getLang() {
            return writer.getLang();
        }

        @Override
        public void write(OutputStream out, Graph graph, PrefixMap prefixMap, String baseURI, Context context) {
            writer.write(out, DatasetGraphFactory.wrap(graph), prefixMap, baseURI, context);
        }

        @Override
        public void write(Writer out, Graph graph, PrefixMap prefixMap, String baseURI, Context context) {
            writer.write(out, DatasetGraphFactory.wrap(graph), prefixMap, baseURI, context);
        }
    }
}
