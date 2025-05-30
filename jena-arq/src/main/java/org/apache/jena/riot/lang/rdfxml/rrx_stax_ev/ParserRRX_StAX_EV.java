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

package org.apache.jena.riot.lang.rdfxml.rrx_stax_ev;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.jena.riot.SysRIOT.fmtMessage;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.lib.EscapeStr;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.impl.XMLLiteralType;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.irix.IRIException;
import org.apache.jena.irix.IRIx;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.rdfxml.RDFXMLParseException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.XML11Char;
import org.apache.jena.vocabulary.RDF;

/** StAX events */
class ParserRRX_StAX_EV {
    private static int IRI_CACHE_SIZE = 8192;
    private static boolean EVENTS = false;
    private final IndentedWriter trace;

    private final XMLEventReader xmlEventReader;

    private Cache<String, IRIx> iriCacheForBaseNull = null;
    private Cache<String, IRIx> currentIriCache = null;
    private final Map<IRIx, Cache<String, IRIx>> mapBaseIriToCache = new HashMap<>();
    // Stacks.

    // Constants
    private static final String rdfNS = RDF.uri;
    private static final String xmlNS = "http://www.w3.org/XML/1998/namespace";
    private boolean hasRDF = false;

    private final ParserProfile parserProfile;
    private final ErrorHandler errorHandler;
    private final StreamRDF destination;

    private void updateCurrentIriCacheForCurrentBase() {
        if(currentBase != null) {
            currentIriCache = mapBaseIriToCache
                    .computeIfAbsent(currentBase,
                            b -> CacheFactory.createSimpleCache(IRI_CACHE_SIZE)
                    );
        } else {
            if(iriCacheForBaseNull == null) {
                iriCacheForBaseNull = CacheFactory.createSimpleCache(IRI_CACHE_SIZE);
            }
            currentIriCache = iriCacheForBaseNull;
        }
    }

    private boolean isDifferentFromCurrentBase(IRIx base) {
        if(currentBase != null) {
            return !currentBase.equals(base);
        } else if(base == null) {
            return false;
        }
        return true;
    }

    private record BaseLang(IRIx base, String lang, Cache<String, IRIx> iriCache) {}
    private Deque<BaseLang> stack = new ArrayDeque<>();
    // Just these operations:

    private void pushFrame(IRIx base, String lang) {
        BaseLang frame = new BaseLang(currentBase, currentLang, currentIriCache);
        stack.push(frame);
        currentLang = lang;
        if(isDifferentFromCurrentBase(base)) {
            currentBase = base;
            updateCurrentIriCacheForCurrentBase();
        }
    }

    private void popFrame() {
        BaseLang frame = stack.pop();
        currentLang = frame.lang;
        if(isDifferentFromCurrentBase(frame.base)) {
            currentBase = frame.base;
            currentIriCache = frame.iriCache;
        }
    }

    /** Mark the usage of a QName */
    private enum QNameUsage {
        TypedNodeElement("typed node element"), PropertyElement("property element");
        final String msg;
        private QNameUsage(String msg) { this.msg = msg; }
    }

    // ---- Error handlers

    private RiotException RDFXMLparseError(String message, XMLEvent event) {
        Location location = (event!=null) ? event.getLocation() : null;
        return RDFXMLparseError(message, location);
    }

    private void RDFXMLparseWarning(String message, XMLEvent event) {
        Location location = (event!=null) ? event.getLocation() : null;
        RDFXMLparseWarning(message, location);
    }

    private RiotException RDFXMLparseError(String message, Location location) {
        if ( location != null )
            errorHandler.error(message, location.getLineNumber(), location.getColumnNumber());
        else
            errorHandler.error(message, -1, -1);
        // The error handler normally throws an exception - for RDF/XML parsing it is required.
        return new RiotException(fmtMessage(message, location.getLineNumber(), location.getColumnNumber())) ;
    }

    private void RDFXMLparseWarning(String message, Location location) {
        if ( location != null )
            errorHandler.warning(message, location.getLineNumber(), location.getColumnNumber());
        else
            errorHandler.warning(message, -1, -1);
    }

    // Tracking for ID on nodes (not reification usage)
    // We limit the number of local fragment IDs tracked because map only grows.
    // A base URI may be re-introduced so this isn't nested scoping.
    private int countTrackingIDs = 0;
    private Map<IRIx, Map<String,  Location>> trackUsedIDs = new HashMap<>();
    private Location previousUseOfID(String idStr, Location location) {
        Map<String, Location> scope = trackUsedIDs.computeIfAbsent(currentBase, k->new HashMap<>());
        Location prev = scope.get(idStr);
        if ( prev != null )
            return prev;
        if ( countTrackingIDs > 10000 )
            return null;
        scope.put(idStr, location);
        countTrackingIDs++;
        return null;
    }

    // Collecting characters does not need to be a stack because there are
    // no nested objects while gathering characters for lexical or XMLLiterals.
    private StringBuilder accCharacters = new StringBuilder(100);

    private IRIx currentBase = null;
    private String currentLang = null;

    /** Integer holder for rdf:li */
    private static class Counter { int value = 1; }

    ParserRRX_StAX_EV(XMLEventReader reader, String xmlBase, ParserProfile parserProfile, StreamRDF destination, Context context) {
        // Debug
        IndentedWriter out = IndentedWriter.stdout.clone();
        out.setFlushOnNewline(true);
        out.setUnitIndent(4);
        out.setLinePrefix("# ");
        this.trace = out;
        EVENTS = ReaderRDFXML_StAX_EV.TRACE;
        // Debug

        this.xmlEventReader = reader;
        this.parserProfile = parserProfile;
        this.errorHandler = parserProfile.getErrorHandler();
        if ( xmlBase != null ) {
            this.currentBase = IRIx.create(xmlBase);
            parserProfile.setBaseIRI(currentBase.str());
        } else {
            this.currentBase = null;
        }
        updateCurrentIriCacheForCurrentBase();
        this.currentLang = "";
        this.destination = destination;
    }

    private static final QName rdfRDF = new QName(rdfNS, "RDF");
    private static final QName rdfDescription = new QName(rdfNS, "Description");
    private static final QName rdfID = new QName(rdfNS, "ID");
    private static final QName rdfNodeID = new QName(rdfNS, "nodeID");
    private static final QName rdfAbout = new QName(rdfNS, "about");
    private static final QName rdfType = new QName(rdfNS, "type");

    private static final QName rdfContainerItem = new QName(rdfNS, "li");
    private static final QName rdfDatatype = new QName(rdfNS, "datatype");
    private static final QName rdfParseType = new QName(rdfNS, "parseType");
    private static final QName rdfResource = new QName(rdfNS, "resource");

    private static final QName rdfAboutEach = new QName(rdfNS, "aboutEach");
    private static final QName rdfAboutEachPrefix = new QName(rdfNS, "aboutEachPrefix");
    private static final QName rdfBagID = new QName(rdfNS, "bagID");

    private static final QName xmlQNameBase = new QName(XMLConstants.XML_NS_URI, "base");
    private static final QName xmlQNameLang = new QName(XMLConstants.XML_NS_URI, "lang");
    // xml:space is a now-deprecated XML attribute that related to handing
    // whitespace characters inside elements. Skip it.
    private static final QName xmlQNameSpace = new QName(XMLConstants.XML_NS_URI, "space");

    private static final String parseTypeCollection    = "Collection";
    private static final String parseTypeLiteral       = "Literal";
    private static final String parseTypeLiteralAlt    = "literal";
    private static final String parseTypeLiteralStmts  = "Statements";    // CIM Github issue 2473
    private static final String parseTypeResource      = "Resource";
    // This is a dummy parseType for when there is no given rdf:parseType.
    private static final String parseTypePlain = "$$";

    // Grammar productions.
    // 6.2.2 Production coreSyntaxTerms
    // rdf:RDF | rdf:ID | rdf:about | rdf:parseType | rdf:resource | rdf:nodeID | rdf:datatype
    private static Set<QName> $coreSyntaxTerms =
            Set.of(rdfRDF, rdfID, rdfAbout, rdfParseType, rdfResource, rdfNodeID, rdfDatatype);

//    // 6.2.3 Production syntaxTerms
//    // coreSyntaxTerms | rdf:Description | rdf:li
//    private static Set<QName> $syntaxTerms =
//            Set.of(rdfRDF, rdfID, rdfAbout, rdfParseType, rdfResource, rdfNodeID, rdfDatatype,
//                   rdfDescription, rdfContainerItem);

    // 6.2.4 Production oldTerms
    // rdf:aboutEach | rdf:aboutEachPrefix | rdf:bagID
    private static Set<QName> $oldTerms = Set.of(rdfAboutEach, rdfAboutEachPrefix, rdfBagID);

    private static Set<String> $allowedUnqualified =
            Set.of(rdfAbout.getLocalPart(), rdfID.getLocalPart(), rdfResource.getLocalPart(),
                   rdfParseType.getLocalPart(), rdfType.getLocalPart());

    private boolean coreSyntaxTerm(QName qName) {
        if ( ! rdfNS.equals(qName.getNamespaceURI()) )
            return false;
        return $coreSyntaxTerms.contains(qName);
    }

    // 6.2.5 Production nodeElementURIs
    // anyURI - ( coreSyntaxTerms | rdf:li | oldTerms )
    private static boolean allowedNodeElementURIs(QName qName) {
        if ( ! rdfNS.equals(qName.getNamespaceURI()) )
            return true;
        if ( $coreSyntaxTerms.contains(qName) )
            return false;
        if ( rdfContainerItem.equals(qName) )
            return false;
        if ( $oldTerms.contains(qName) )
            return false;
        return true;
    }

    // 6.2.6 Production propertyElementURIs
    // anyURI - ( coreSyntaxTerms | rdf:Description | oldTerms )
    private static boolean allowedPropertyElementURIs(QName qName) {
        if ( ! rdfNS.equals(qName.getNamespaceURI()) )
            return true;
        if ( $coreSyntaxTerms.contains(qName) )
            return false;
        if ( rdfDescription.equals(qName) )
            return false;
        if ( $oldTerms.contains(qName) )
            return false;
        return true;
    }

    // 6.2.7 Production propertyAttributeURIs
    // anyURI - ( coreSyntaxTerms | rdf:Description | rdf:li | oldTerms )
    private static boolean allowedPropertyAttributeURIs(QName qName) {
        if ( ! rdfNS.equals(qName.getNamespaceURI()) )
            return true;
        if ( $coreSyntaxTerms.contains(qName) )
            return false;
        if ( rdfDescription.equals(qName) )
            return false;
        if ( rdfContainerItem.equals(qName) )
            return false;
        if ( $oldTerms.contains(qName) )
            return false;
        return true;
    }

    private static boolean allowedUnqualifiedTerm(String localName) {
        return $allowedUnqualified.contains(localName);
    }

    /** The attributes that guide the RDF/XML parser. */
    private static Set<QName> $rdfSyntaxAttributes =
            Set.of(rdfRDF, rdfAbout, rdfNodeID, rdfID, rdfParseType, rdfDatatype, rdfResource);

    private static boolean isSyntaxAttribute(QName qName) {
        return $rdfSyntaxAttributes.contains(qName);
    }

    // xml:space is a now-deprecated XML attribute that related to handing
    // whitespace characters inside elements.
    private static Set<QName> $xmlReservedTerms = Set.of(xmlQNameBase, xmlQNameLang, xmlQNameSpace);

    /** Recognized XML namespace Qname */
    private static boolean isXMLQName(QName qName) {
        return $xmlReservedTerms.contains(qName);
    }

    private static boolean isXMLNamespace(QName qName) {
        return xmlNS.equals(qName.getNamespaceURI());
    }

    // start: whole doc
    //   6.2.8 Production doc
    //   6.2.11 Production nodeElement
    // doc or production nodeElement.
    // start:
    //  6.2.9 Production RDF
    void parse() {
        boolean hasDocument = false;

        XMLEvent event = nextEventAny();

        if ( event.isStartDocument() ) {
            if ( ReaderRDFXML_StAX_EV.TRACE )
                trace.println("Start document");
            hasDocument = true;
            event = nextEventTag();
        }

        if ( event.isEndDocument() )
            throw RDFXMLparseError("Empty document", event);

        // rdf:RDF or rdf:Description.
        if ( ! event.isStartElement() )
            throw RDFXMLparseError("Not a start element", event);

        StartElement rdfStartElt = event.asStartElement();

        boolean hasFrame = false;

        // <rdf:RDF>
        if ( qNameMatches(rdfRDF, rdfStartElt.getName()) ) {
            if ( ReaderRDFXML_StAX_EV.TRACE )
                trace.println("rdf:RDF");
            // Not necessary to track this element when parsing.
            hasFrame = startElement(rdfStartElt);
            // Only the XML base and namespaces that apply throughout rdf:RDF are parser output.
            emitInitialBaseAndNamespaces(rdfStartElt);
            hasRDF = true;
            event = nextEventTag();
        }

        incIndent();
        if ( hasRDF )
            event = nodeElementLoop(event);
        else
            event = nodeElementSingle(event);
        decIndent();

        // Possible </rdf:RDF>
        if ( hasRDF ) {
            // StAX will have matched this.
            if ( !qNameMatches(rdfRDF, event.asEndElement().getName()) )
                throw RDFXMLparseError("Expected </"+rdfStartElt.getName().getPrefix()+":RDF>  got "+str(event), event);
            endElement(hasFrame);
            if ( ReaderRDFXML_StAX_EV.TRACE )
                trace.println("/rdf:RDF");
            event = nextEventAny();
        }
        // Now past <rdf:RDF...></rdf:RDF>

        while ( isWhitespace(event) )
            event = nextEventAny();

        if ( hasDocument ) {
            if ( !event.isEndDocument() )
                throw RDFXMLparseError("Expected end of document: got "+str(event), event);
            if ( ReaderRDFXML_StAX_EV.TRACE )
                trace.println("End document");
        }
    }

    // ---- Node elements

    /**
     * Top level node element loop inside &lt;rdf:RDF&gt;, &lt;/rdf:RDF&gt;.
     * This is zero or more node elements.
     */
    private XMLEvent nodeElementLoop(XMLEvent event) {
        // There was an rdf:RDF

        // ---- Node Loop - sequence of zero or more RDF/XML node elements.
        // generic move over multiple elements
        while ( event != null ) {
            if ( !event.isStartElement() )
                break;
            StartElement startElt = event.asStartElement();
            nodeElement(startElt);
            event = nextEventTag();
        }
        return event;
    }

    /** Top level single node element. (no &lt;rdf:RDF&gt;, &lt;/rdf:RDF&gt;) */
    private XMLEvent nodeElementSingle(XMLEvent event) {
        if ( !event.isStartElement() )
            // Protective "not recognized"
            return event;
        StartElement startElt = event.asStartElement();
        nodeElement(startElt);
        return peekEvent();
    }

    /**
     * One node element.
     * On entry, the start of node element has been read.
     * On exit, have read the end tag for the node element.
     */
    private void nodeElement(StartElement startElt) {
        nodeElement(null, startElt);
    }

    /**
     * Process one node element. The subject may have been determined;
     * this is needed for nested node elements.
     */
    private void nodeElement(Node subject, StartElement startElt) {

        if ( ReaderRDFXML_StAX_EV.TRACE )
            trace.println(">> nodeElement: "+str(startElt.getLocation())+" "+str(startElt));

        if ( ! allowedNodeElementURIs(startElt.getName()) )
            throw RDFXMLparseError("Not allowed as a node element tag: '"+str(startElt.getName())+"'", startElt);

        // rdf:resource not allowed on a node element
        Attribute attrRdfResource= startElt.getAttributeByName(rdfResource);
        if ( attrRdfResource != null )
            throw RDFXMLparseError("rdf:resource not allowed as attribute here: "+str(startElt.getName()), startElt);

        incIndent();
        boolean hasFrame = startElement(startElt);
        if ( subject == null )
            subject = attributesToSubjectNode(startElt);
        EndElement endElt = nodeElementProcess(subject, startElt);
        endElement(hasFrame);
        decIndent();

        if ( ReaderRDFXML_StAX_EV.TRACE )
            trace.println("<< nodeElement: "+str(endElt.getLocation())+" "+str(endElt));
    }

    private EndElement nodeElementProcess(Node subject, StartElement startElt) {
        Location location = startElt.getLocation();
        QName qName = startElt.getName();

        if ( ! qNameMatches(qName, rdfDescription) ) {
            // Typed Node Element
            if ( isMemberProperty(qName) )
                RDFXMLparseWarning(str(qName)+" is being used on a typed node", location);
            else {
                if ( isNotRecognizedRDFtype(qName) )
                    RDFXMLparseWarning(str(qName)+" is not a recognized RDF term for a type", location);
            }

            Node object = qNameToIRI(qName, QNameUsage.TypedNodeElement, location);
            emit(subject, NodeConst.nodeRDFType, object, location);
        }

        // Other attributes are properties.
        // rdf:type is special.

        processPropertyAttributes(subject, startElt, true, location);

        // Finished with the node start tag.
        XMLEvent event = nextEventTag();
        event = propertyElementLoop(subject, event);

        if ( event == null )
            throw RDFXMLparseError("Expected end element for "+startElt.getName(), event);

        // Cause of end of properties.
        if ( ! event.isEndElement() )
            throw RDFXMLparseError("Expected end element "+str(startElt)+" but found "+str(event), event);
        return event.asEndElement();
    }

    // ---- Property elements

    private XMLEvent propertyElementLoop(Node subject, XMLEvent event) {
        Counter listElementCounter = new Counter();
        while (event != null) {
            if ( ! event.isStartElement() )
                break;
            propertyElement(subject, event.asStartElement(), listElementCounter);
            event = nextEventTag();
        }
        return event;
    }

    /**
     * Property elements.
     * On entry, the start of property has been inspected but not consumed.
     * Consume up to the end of the property.
     * Return the next event to be processed.
     */
    private void propertyElement(Node subject, StartElement startElt, Counter listElementCounter) {
        // Move logging inside?
        if ( ReaderRDFXML_StAX_EV.TRACE )
            trace.println(">> propertyElement: "+str(startElt.getLocation())+" "+str(startElt));

        incIndent();
        boolean hasFrame = startElement(startElt);
        QName qName = startElt.getName();

        if ( ! allowedPropertyElementURIs(startElt.getName()) )
            throw RDFXMLparseError("QName not allowed for property: "+str(qName), startElt);

        if ( isNotRecognizedRDFproperty(qName) )
            RDFXMLparseWarning(str(qName)+" is not a recognized RDF property", startElt);

        XMLEvent event = propertyElementProcess(subject, startElt, listElementCounter);
        endElement(hasFrame);
        decIndent();

        if ( ReaderRDFXML_StAX_EV.TRACE )
            trace.println("<< propertyElement: "+str(event.getLocation())+" "+str(event));
    }

    private XMLEvent propertyElementProcess(Node subject, StartElement startElt, Counter listElementCounter) {
        final Location location = startElt.getLocation();
        Node property;
        if ( qNameMatches(rdfContainerItem, startElt.getName()) )
            property = iriDirect(rdfNS+"_"+Integer.toString(listElementCounter.value++), location);
        else
            property = qNameToIRI(startElt.getName(), QNameUsage.PropertyElement, location);

        Node reify = reifyStatement(startElt);
        Emitter emitter = (reify==null) ? this::emit : (s,p,o,loc)->emitReify(reify, s, p, o, loc);

        // If there is a blank node label, the element must be empty,
        // Check NCName if blank node created
        String objBlankNodeLabel = attribute(startElt, rdfNodeID);
        String rdfResourceStr = attribute(startElt, rdfResource);
        String datatype = attribute(startElt, rdfDatatype);
        String parseType = objectParseType(startElt);

        // Checking
        if ( datatype != null ) {
            if ( parseType != null && parseType != parseTypePlain )
                throw RDFXMLparseError("rdf:datatype can not be used with rdf:parseType.", startElt);
            if ( rdfResourceStr != null )
                throw RDFXMLparseError("rdf:datatype can not be used with rdf:resource.", startElt);
            if ( objBlankNodeLabel != null )
                throw RDFXMLparseError("rdf:datatype can not be used with rdf:NodeId.", startElt);
        }

        if ( rdfResourceStr != null && objBlankNodeLabel != null )
            throw RDFXMLparseError("Can't have both rdf:nodeId and rdf:resource on a property element", startElt);

        if ( rdfResourceStr != null && parseType != parseTypePlain )
            throw RDFXMLparseError("Both rdf:resource and rdf:ParseType on a property element. Only one allowed", startElt);

        if ( objBlankNodeLabel != null && parseType != parseTypePlain )
            throw RDFXMLparseError("Both rdf:NodeId and rdf:ParseType on a property element. Only one allowed", startElt);

        Node resourceObj = null;

        if ( rdfResourceStr != null )
            resourceObj = iriResolve(rdfResourceStr, location);

        if ( objBlankNodeLabel != null )
            resourceObj = blankNode(objBlankNodeLabel, location);

        Node innerSubject = processPropertyAttributes(resourceObj, startElt, true, location);
        if ( resourceObj == null && innerSubject != null ) {
            emitter.emit(subject, property, innerSubject, location);
            XMLEvent event = nextEventAny();
            if ( !event.isEndElement() )
                throw RDFXMLparseError("Expecting end element tag when using property attributes on a property element", startElt);
            return event;
        }

        if ( resourceObj != null ) {
            emitter.emit(subject, property, resourceObj, location);
            // Must be an empty element.
            XMLEvent event = nextEventAny();
            if ( !event.isEndElement() )
                throw RDFXMLparseError("Expecting end element tag when using rdf:resource or rdf:NodeId on a proeprty.", startElt);
            return event;
        }

        String parseTypeName = parseType;
        switch( parseTypeName) {
            case parseTypeLiteralAlt -> {
                RDFXMLparseWarning("Encountered rdf:parseType='literal'. Treated as rdf:parseType='Literal'", location);
                parseTypeName = "Literal";
            }
            case parseTypeLiteralStmts -> {
                RDFXMLparseWarning("Encountered rdf:parseType='Statements'. Treated as rdf:parseType='Literal'", location);
                parseTypeName = "Literal";
            }
        }

        switch(parseTypeName) {
            case parseTypeResource -> {
                // Implicit <rdf:Description><rdf:Description> i.e. fresh blank node
                if ( ReaderRDFXML_StAX_EV.TRACE )
                    trace.println("rdfParseType=Resource");
                XMLEvent event = parseTypeResource(subject, property, emitter, startElt);
                return event;
            }
            case parseTypeLiteral -> {
                if ( ReaderRDFXML_StAX_EV.TRACE )
                    trace.println("rdfParseType=Literal");
                XMLEvent event = parseTypeLiteral(subject, property, emitter, startElt);
                return event;
            }
            case parseTypeCollection -> {
                if ( ReaderRDFXML_StAX_EV.TRACE )
                    trace.println("rdfParseType=Collection");
                XMLEvent event = parseTypeCollection(subject, property, emitter, startElt);
                return event;
            }
            case parseTypePlain -> {} // The code below.
            default ->
                throw RDFXMLparseError("Not a legal defined rdf:parseType: "+parseType, startElt);
        }

        // General read anything.
        // Can't peek, which would have allowed using getElementText.
        // We need a lookahead of 2 to see if this is a text-only element, then an end element
        // or ignorable whitespace then a nested node start element.
        XMLEvent event = nextEventAny();

        // Need to see if text or nested.
        if ( event.isCharacters() ) {
            // Initial characters.
            // This may include comments. Slurp until not characters.
            // This does not nest  - we can use the global string buffer object.
            accCharacters.setLength(0);
            StringBuilder sBuff = accCharacters;
            while(event.isCharacters()) {
                Characters initialChars = event.asCharacters();
                sBuff.append(initialChars.getData());
                event = nextEventAny();
            }
            if ( event.isStartElement() ) {
                // Striped - inner node element.
                if ( ! isWhitespace(sBuff) ) {
                    String msg = nonWhitespaceMsg(sBuff.toString());
                    throw RDFXMLparseError("Content before node element. '"+msg+"'", event);
                }
                event = processNestedNodeElement(event, subject, property, emitter);
                return event;
            }
            if ( event.isEndElement() ) {
                String lexicalForm = sBuff.toString();
                accCharacters.setLength(0);
                Location loc = event.getLocation();
                Node obj;
                // Characters - lexical form.
                if ( datatype != null )
                    obj = literalDatatype(lexicalForm, datatype, loc);
                else if ( currentLang() != null )
                    obj = literal(lexicalForm, currentLang, loc);
                else
                    obj = literal(lexicalForm, loc);
                emitter.emit(subject, property, obj, loc);
                return event;
            }
            throw RDFXMLparseError("Unexpected element: "+str(event), event);
        } else if ( event.isStartElement() ) {
            event = processNestedNodeElement(event, subject, property, emitter);
            return event;
        } else if ( event.isEndElement() ) {
            emitter.emit(subject, property, NodeConst.emptyString, event.getLocation());
        } else {
            throw RDFXMLparseError("Malformed property. "+str(event), event);
        }
        return event;
    }

    private Node processPropertyAttributes(Node resourceObj, StartElement startElt, boolean isPropertyElement, Location location) {
        // Subject may not yet be decided.
        List<Attribute> propertyAttributes = gatherPropertyAttributes(startElt, location);
        if ( propertyAttributes.isEmpty() )
            return null;
        if ( isPropertyElement ) {
            String parseTypeStr = objectParseType(startElt);
            if ( parseTypeStr != parseTypePlain ) {
                  // rdf:parseType found.
                  throw RDFXMLparseError("The attribute rdf:parseType is not permitted with property attributes on a property element: "+str(startElt.getName()), location);
            }
        }

        Node innerSubject = (resourceObj==null) ? blankNode(location) : resourceObj;
        outputPropertyAttributes(innerSubject, propertyAttributes, location);
        return innerSubject;
    }

    private List<Attribute> gatherPropertyAttributes(StartElement startElt, Location location) {
        Iterator<Attribute> x = startElt.getAttributes();
        if ( ! x.hasNext() )
            return List.of();
        List<Attribute> propertyAttributes = new ArrayList<>();
        while(x.hasNext()) {
            Attribute attribute = x.next();
            boolean isPropertyAttribute = checkPropertyAttribute(attribute.getName(), location);
            if ( isPropertyAttribute )
                propertyAttributes.add(attribute);
        }
        return propertyAttributes;
    }

    private void outputPropertyAttributes(Node subject, List<Attribute> propertyAttributes, Location location) {
        for ( Attribute attribute : propertyAttributes ) {
            QName qName =  attribute.getName();
            if ( rdfType.equals(qName) ) {
                String iriStr = attribute.getValue();
                Node type = iriResolve(iriStr, location);
                emit(subject, RDF.Nodes.type, type, location);
                return;
            }
            Node property = attributeToIRI(qName, location);
            String lexicalForm = attribute.getValue();
            Node object = literal(lexicalForm, currentLang, location);
            emit(subject, property, object, location);
        }
    }

    /** Return true if this is a property attribute. */
    private boolean checkPropertyAttribute(QName qName, Location location) {
        if ( isSyntaxAttribute(qName) )
            return false;

        // The default namespace (i.e. prefix "") does not apply to attributes.

        // 6.1.2 Element Event - attributes

        if (coreSyntaxTerm(qName) )
            return false;

        if ( ! allowedPropertyAttributeURIs(qName) )
            throw RDFXMLparseError("Not allowed as a property attribute: '"+str(qName)+"'", location);

        if ( isNotRecognizedRDFproperty(qName) )
            RDFXMLparseWarning(str(qName)+" is not a recognized RDF term for a property attribute", location);

        if ( isXMLQName(qName) )
            return false;

        if ( isXMLNamespace(qName) ) {
            // Unrecognized qnames in the XMLnamespace are a warning and are ignored.
            RDFXMLparseWarning("Unrecognized XML attribute: '"+str(qName)+"'", location);
            return false;
        }

        if ( StringUtils.isBlank(qName.getNamespaceURI()) ) {
            String localName = qName.getLocalPart();
            boolean valid = checkPropertyAttributeUnqualifiedTerm(localName, location);
            return valid;
        }
        return true;
    }

    private boolean checkPropertyAttributeUnqualifiedTerm(String localName, Location location) {
        if ( allowedUnqualifiedTerm(localName) )
            return true;
        if ( localName.length() >= 3 ) {
            String chars3 = localName.substring(0, 3);
            if ( chars3.equalsIgnoreCase("xml") ) {
                // 6.1.2 Element Event
                // "all attribute information items with [prefix] property having no value and which
                // have [local name] beginning with xml (case independent comparison)"
                // Test: unrecognised-xml-attributes/test002.rdf
                RDFXMLparseWarning("Unrecognized XML non-namespaced attribute '"+localName+"' - ignored", location);
                return false;
            }
        }
        // 6.1.4 Attribute Event
        // "Other non-namespaced ·local-name· accessor values are forbidden."
        throw RDFXMLparseError("Non-namespaced attribute not allowed as a property attribute: '"+localName+"'", location);
    }

    private String objectParseType(StartElement startElt) {
        String parseTypeStr = attribute(startElt, rdfParseType);
        return  ( parseTypeStr != null ) ? parseTypeStr : parseTypePlain;
    }

    /**
     * Accumulate text for a text element, given some characters already read.
     * This method skips comments.
     * This method returns upto and including the closing end element tag.
     */
    private String accumulateLexicalForm(XMLEvent initialEvent, StringBuilder sBuff) {
        // Can't use getText. We had to peek to know it's text-only.

        String lexicalForm;
        XMLEvent event = initialEvent;
        while(event != null ) {
            if ( event.isEndElement() )
                break;
            if ( !event.isCharacters() )
                throw RDFXMLparseError("Unexpected element in text element: "+str(event), initialEvent);
            sBuff.append(event.asCharacters().getData());
            event = nextEventAny();
        }
        lexicalForm = sBuff.toString();
        return lexicalForm;
    }

    private XMLEvent parseTypeResource(Node subject, Node property, Emitter emitter, StartElement startElt) {
        Node innerSubject = blankNode(startElt.getLocation());
        emitter.emit(subject, property, innerSubject, startElt.getLocation());
        // Move to first property
        XMLEvent event = nextEventTag();
        // Maybe endElement <element/>
        event = propertyElementLoop(innerSubject, event);
        return event;
    }

    private XMLEvent parseTypeLiteral(Node subject, Node property, Emitter emitter, StartElement startElt) {

        //NamespaceContext nsCxt = startElt.getNamespaceContext();

        String text = xmlLiteralAccumulateText(startElt);
        Node object = literalDatatype(text, XMLLiteralType.rdfXMLLiteral, startElt.getLocation());
        emitter.emit(subject, property, object, startElt.getLocation());
        // Skip trailer then end property tag.
        XMLEvent event = peekEvent();
        return event;
    }

    private static final String openStartTag = "<";
    private static final String closeStartTag = ">";
    private static final String openEndTag = "</";
    private static final String closeEndTag = ">";

    private String xmlLiteralAccumulateText(StartElement startElt) {

        // Map prefix -> URI
        Map<String, String> namespaces = Map.of();
        Deque<Map<String, String>> stackNamespaces = new ArrayDeque<>();

        // This does not nest  - we can use the global string buffer object.
        accCharacters.setLength(0);
        StringBuilder sBuff = new StringBuilder();
        XMLEvent event = startElt;
        int depth = 0;
        while( event != null ) {
            // Really, really any event.
            event = nextEventRaw();
            if ( event.isStartElement() ) {
                StartElement evStartElt = event.asStartElement();
                depth++;
                incIndent();
                // -- Namespaces
                // Note that the StAX parser has already processed the xmlns for the
                // declarations element and the setting  are in the NamespaceContext.
                stackNamespaces.push(namespaces);
                namespaces = new HashMap<>(namespaces);

                sBuff.append(openStartTag);
                QName qname = evStartElt.getName();
                if ( qname.getPrefix() != null && ! XMLConstants.DEFAULT_NS_PREFIX.equals(qname.getPrefix()) ) {
                    sBuff.append(qname.getPrefix());
                    sBuff.append(":");
                }
                sBuff.append(qname.getLocalPart());

                // -- namespaces
                xmlLiteralNamespaces(namespaces, sBuff, evStartElt);

                xmlLiteralAttributes(sBuff, evStartElt);

                sBuff.append(closeStartTag);

            } else if ( event.isEndElement() ) {
                decIndent();
                depth--;
                if ( depth < 0 ) {
                    //stackNamespaces.clear();
                    //namespaces = Map.of();
                    break;
                }
                namespaces = stackNamespaces.pop();
                sBuff.append(openEndTag);
                QName qname = event.asEndElement().getName();
                if ( qname.getPrefix() != null && ! XMLConstants.DEFAULT_NS_PREFIX.equals(qname.getPrefix()) ) {
                    sBuff.append(qname.getPrefix());
                    sBuff.append(":");
                }
                sBuff.append(qname.getLocalPart());
                sBuff.append(closeEndTag);
            } else if ( event.isCharacters() ) {
                String s = event.asCharacters().getData();
                s = xmlLiteralEscapeText(s);
                sBuff.append(s);
            } else if ( event.getEventType() == XMLStreamConstants.COMMENT ) {
                Comment x = (Comment)event;
                sBuff.append("<!--");
                sBuff.append(x.getText());
                sBuff.append("-->");
            } else if ( event.isProcessingInstruction() ) {
                ProcessingInstruction procInst= (ProcessingInstruction)event;
                String target = procInst.getTarget();
                String data = procInst.getData();
                sBuff.append("<?");
                sBuff.append(target);
                sBuff.append(' ');
                sBuff.append(data);
                sBuff.append("?>");
            } else {
                throw RDFXMLparseError("Unexpected event in rdf:XMLLiteral: "+event.toString(), event);
            }
        }
        String x = sBuff.toString();
        accCharacters.setLength(0);
        return x;
    }

    private void xmlLiteralAttributes(StringBuilder sBuff, StartElement evStartElt) {
        // Sort attributes
        Iterator<Attribute> iterAttr0 = evStartElt.getAttributes();
        Map<String, Attribute> attrs = new TreeMap<>();
        while(iterAttr0.hasNext()) {
            Attribute attr = iterAttr0.next();
            String k = str(attr.getName());
            attrs.put(k, attr);
        }

        Iterator<Attribute> iterAttr = attrs.values().iterator();
        while(iterAttr.hasNext()) {
            Attribute attr = iterAttr.next();
            sBuff.append(" ");
            sBuff.append(str(attr.getName()));
            sBuff.append("=\"");
            sBuff.append(xmlLiteralEscapeAttr(attr.getValue()));
            sBuff.append("\"");
        }
    }

    /**
     * Process all QNames used in this element.
     */
    private void xmlLiteralNamespaces(Map<String, String> namespaces, StringBuilder sBuff, StartElement startElt) {
        NamespaceContext nsCxt = startElt.getNamespaceContext();

        // Map prefix -> URI, sorted by prefix. This is only the required namespaces.
        // The other map, namespaces, is all the namespaces in scope.
        Map<String, String> outputNS = new TreeMap<>();

        xmlLiteralNamespaceQName(outputNS, namespaces, nsCxt, startElt.getName());
        Iterator<Attribute> iterAttr = startElt.getAttributes();
        while(iterAttr.hasNext()) {
            Attribute attr = iterAttr.next();
            xmlLiteralNamespaceQName(outputNS, namespaces, nsCxt, attr.getName());
        }
        // Output.
        for ( String prefix : outputNS.keySet() ) {
            String uri = outputNS.get(prefix);
            if ( uri == null )
                // Undefined default namespace.
                continue;
            sBuff.append(" ");
            if ( prefix.isEmpty() ) {
                sBuff.append("xmlns=\"");
                sBuff.append(uri);
                sBuff.append("\"");
            } else {
                sBuff.append("xmlns:");
                sBuff.append(prefix);
                sBuff.append("=\"");
                sBuff.append(uri);
                sBuff.append("\"");
            }
        }
    }

    /** Process one QName - insert a namespace if the prefix is not in scope as given by the amespace mapping. */
    private void xmlLiteralNamespaceQName(Map<String, String> outputNS, Map<String, String> namespaces, NamespaceContext nsCxt, QName qName) {
        String prefix = qName.getPrefix();
        String namespaceURI = nsCxt.getNamespaceURI(prefix);
        if ( namespaceURI == null ) {
            // No default in scope.
        }

        // Not seen this prefix or it was a different value.
        if ( namespaceURI != "" &&  // this first condition is needed for woodstox and allto to work
                (! namespaces.containsKey(prefix) ||
                 ( namespaceURI != null && ! namespaces.get(prefix).equals(namespaceURI)) )) {
            // Define in current XML subtree.
            outputNS.put(prefix, namespaceURI);
            namespaces.put(prefix, namespaceURI);
        }
    }

    /**
     * Escape text used in an XML content.
     */
    private String xmlLiteralEscapeText(CharSequence stringAcc) {
        StringBuilder sBuff = new StringBuilder();
        int len = stringAcc.length() ;
        for (int i = 0; i < len; i++) {
            char c = stringAcc.charAt(i);
            String replace = switch (c) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                //case '"' -> "&quot;";
                //case '\'' -> replace = "&apos;";
                default -> null;
            };
            if ( replace == null )
                sBuff.append(c);
            else
                sBuff.append(replace);
        }
        return sBuff.toString();
    }

    /**
     * Escape text used in an XML attribute value.
     */
    private String xmlLiteralEscapeAttr(CharSequence stringAcc) {
        StringBuilder sBuff = new StringBuilder();
        int len = stringAcc.length() ;
        for (int i = 0; i < len; i++) {
            char c = stringAcc.charAt(i);
            String replace = switch (c) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                //case '>' -> "&gt;";
                case '"' -> "&quot;";
                //case '\'' -> replace = "&apos;";
                default -> null;
            };
            if ( replace == null )
                sBuff.append(c);
            else
                sBuff.append(replace);
        }
        return sBuff.toString();
    }

    // --- RDF Collections

    private XMLEvent parseTypeCollection(Node subject, Node property, Emitter emitter, StartElement startElt) {
        XMLEvent event = startElt;
        Node lastCell = null ;
        Location location = startElt.getLocation();

        while(true) {
            event = nextEventTag();
            if ( ! event.isStartElement() )
                break;
            location = event.getLocation();
            Node thisCell = blankNode(event.getLocation());
            if ( lastCell == null ) {
                // First list item. Link the list in.
                lastCell = thisCell;
                emitter.emit(subject, property, thisCell, location);
            } else {
                // Link to the previous element. No reificiation.
                emit(lastCell, NodeConst.nodeRest, thisCell, location);
            }
            StartElement itemStart = event.asStartElement();
            Node itemSubject = attributesToSubjectNode(itemStart);
            emit(thisCell, RDF.Nodes.first, itemSubject, itemStart.getLocation());
            nodeElement(itemSubject, itemStart);
            lastCell = thisCell;
        }

        // Finish the list.
        if ( lastCell != null ) {
            emit(lastCell, NodeConst.nodeRest, NodeConst.nodeNil, location);
        } else {
            // It was an empty list
            emitter.emit(subject, property, NodeConst.nodeNil, location);
        }

        return event;
    }

    private Node reifyStatement(StartElement startElt) {
        String reifyId = attribute(startElt, rdfID);
        if ( reifyId == null )
            return null;
        Node reify = iriFromID(reifyId, startElt.getLocation());
        return reify;
    }

    /** Return the lang in-scope. Return null for none. */
    private String currentLang() {
        if ( currentLang == null || currentLang.isEmpty() )
            return null;
        return currentLang;
    }

    private XMLEvent processNestedNodeElement(XMLEvent event, Node subject, Node property, Emitter emitter) {
        // Nested / RDF/XML striped syntax.
        StartElement startEltInner = event.asStartElement();
        boolean hasFrame = startElement(startEltInner);
        Node subjectInner = attributesToSubjectNode(startEltInner);
        // subject property INNER
        emitter.emit(subject, property, subjectInner, startEltInner.getLocation());

        // needs to receive subject
        nodeElement(subjectInner, startEltInner);

        // End property tag.
        event = nextEventTag();
        if ( event.isStartElement() )
            throw RDFXMLparseError("Start tag after inner node element (only one node element permitted): got "+str(event), event);
        if ( ! event.isEndElement() )
            throw RDFXMLparseError("Expected an end element: got "+str(event), event);
        // -- end
        endElement(hasFrame);
        return event;
    }

    /** Subject for a node element */
    private Node attributesToSubjectNode(StartElement startElt) {
        Location location = startElt.getLocation();

        // Subject
        // The spec implies these happen in order (i.e later overrides earlier)
        // but the test suite has negative test cases.
        // If there is an attribute a with a.URI == rdf:ID, then e.subject := uri(identifier := resolve(e, concat("#", a.string-value))).
        // If there is an attribute a with a.URI == rdf:nodeID, then e.subject := bnodeid(identifier:=a.string-value).
        // If there is an attribute a with a.URI == rdf:about then e.subject := uri(identifier := resolve(e, a.string-value)).

        String iriStr = attribute(startElt, rdfAbout);
        // Check NCName if URI created,
        String idStr = attribute(startElt, rdfID);
        // Check NCName if blank node created
        String nodeId = attribute(startElt, rdfNodeID);

        if ( nodeId != null && iriStr != null && nodeId != null )
            throw RDFXMLparseError("All of rdf:about, rdf:NodeId and rdf:ID found. Must be only one.", startElt);

        if ( iriStr != null && idStr != null )
            throw RDFXMLparseError("Both rdf:about and rdf:ID found. Must be only one.", startElt);

        if ( nodeId != null && iriStr != null )
            throw RDFXMLparseError("Both rdf:about and rdf:NodeID found. Must be only one.", startElt);

        if ( nodeId != null && idStr != null )
            throw RDFXMLparseError("Both rdf:NodeID rdf:ID found. Must be only one.", startElt);

        if ( iriStr != null )
            return iriResolve(iriStr, location);

        if ( idStr != null )
            return iriFromID(idStr, location);

        if ( nodeId != null )
            return blankNode(nodeId, location);

        // None of the above. It's a fresh blank node.
        return blankNode(location);
    }

    private String attribute(StartElement startElt, QName attrName) {
        Attribute attr = startElt.getAttributeByName(attrName);
        if ( attr == null )
            return null;
        return attr.getValue();
    }

    // ---- Nodes

    private Node qNameToIRI(QName qName, QNameUsage usage, Location location) {
        if ( StringUtils.isBlank(qName.getNamespaceURI()) )
            throw RDFXMLparseError("Unqualified "+usage.msg+" not allowed: <"+qName.getLocalPart()+">", location);
        String uriStr = strQNameToIRI(qName);
        return iriDirect(uriStr, location);
    }

    /** This is the RDF rule for creating an IRI from a QName. */
    private String strQNameToIRI(QName qName) {
        return qName.getNamespaceURI()+qName.getLocalPart();
    }

    private Node attributeToIRI(QName qName, Location location) {
        String namespaceURI = qName.getNamespaceURI();
        String localName = qName.getLocalPart();
        if ( StringUtils.isBlank(namespaceURI) ) {
            if ( allowedUnqualifiedTerm(localName) )
                namespaceURI = rdfNS;
            else
                // else rejected in checkPropertyAttribute
                throw RDFXMLparseError("Unqualified property attribute not allowed: '"+localName+"'", location);
        }
        String uriStr = namespaceURI+localName;
        return iriDirect(uriStr, location);
    }

    // ---- Reading XMLEvents

    /** XML parsing error. */
    private RiotException handleXMLStreamException(XMLStreamException ex) {
        String msg = xmlStreamExceptionMessage(ex);
        if ( ex.getLocation() != null ) {
            int line = ex.getLocation().getLineNumber();
            int col = ex.getLocation().getColumnNumber();
            errorHandler.fatal(msg, line, col);
        } else
            errorHandler.fatal(msg, -1, -1);
        // Should not happen.
        return new RiotException(ex.getMessage(), ex);
    }

    /** Get the detail message from an XMLStreamException */
    private String xmlStreamExceptionMessage(XMLStreamException ex) {
        String msg = ex.getMessage();
        if ( ex.getLocation() != null ) {
            // XMLStreamException with a location is two lines, and has the line/col in the first line.
            // Deconstruct the XMLStreamException message to get the detail part.
            String marker = "\nMessage: ";
            int i = msg.indexOf(marker);
            if ( i > 0 ) {
                msg = msg.substring(i+marker.length());
            }
        }
        return msg;
    }

    /** Look at the next event, but don't read (consume) it. */
    private XMLEvent peekEvent() {
        if ( ! xmlEventReader.hasNext() )
            return null;
        try {
            return xmlEventReader.peek();
        } catch (XMLStreamException ex) {
            throw handleXMLStreamException(ex);
        }
    }

    /**
     * Move to next tag, skipping DTDs and "skipping unimportant whitespace and comments".
     * Returns a start event, endEvent or null.
     */
    private XMLEvent nextEventTag() {
        // Similar to xmlEventReader.nextTag(). This code works with DTD local
        // character entities and assumes the StAX parser
        // IS_REPLACING_ENTITY_REFERENCES so that it processes the DTD to define
        // character entities and then manages the text replacement.

        try {
            while(xmlEventReader.hasNext()) {
                XMLEvent ev = xmlEventReader.nextEvent();
                int evType = ev.getEventType();
                switch (evType) {
                    case START_ELEMENT, END_ELEMENT -> {
                        if ( EVENTS )
                            System.out.println("-- Tag: "+str(ev));
                        return ev;
                    }
                    case CHARACTERS, CDATA -> {
                        Characters chars = ev.asCharacters();
                        if ( ! isWhitespace(ev) ) {
                            String str = ev.asCharacters().getData();
                            String text = nonWhitespaceMsg(str);
                            throw RDFXMLparseError("Expecting a start or end element. Got characters '"+text+"'", ev);
                        }
                    }
                    case COMMENT, DTD -> { } // Skip
                    //case SPACE ->
                    //case PROCESSING_INSTRUCTION ->
                    //case ENTITY_DECLARATION ->
                    default ->
                        // Not expecting any other type of event.
                        throw RDFXMLparseError("Unexpected  event "+str(ev), ev);
                }
                // and loop
            }
            return null;
        } catch (XMLStreamException ex) {
            throw handleXMLStreamException(ex);
        }
    }

    /** Move to XMLEvent, skipping comments. */
    private XMLEvent nextEventAny() {
        try {
            XMLEvent ev = null;
            while(xmlEventReader.hasNext()) {
                ev = xmlEventReader.nextEvent();
                if ( isComment(ev) )
                    continue;
                if ( ev.isProcessingInstruction() ) {
                    RDFXMLparseWarning("XML Processing instruction - ignored", ev);
                    continue;
                }
                break;
            }
            if ( EVENTS ) {
                if ( ev == null )
                    System.out.println("-- Read: end of events");
                else
                    System.out.println("-- Event: "+str(ev));
            }
            return ev;
        } catch (XMLStreamException ex) {
            throw handleXMLStreamException(ex);
        }
    }

    /** Move to next XMLEvent regardless of the event type. */
    private XMLEvent nextEventRaw() {
        try {
            XMLEvent ev = null;
            if ( xmlEventReader.hasNext() )
                ev = xmlEventReader.nextEvent();
            if ( EVENTS ) {
                if ( ev == null )
                    System.out.println("-- Read: end of events");
                else
                    System.out.println("-- Event: "+str(ev));
            }
            return ev;
        } catch (XMLStreamException ex) {
            throw handleXMLStreamException(ex);
        }
    }

    // ---- XML parsing

    /**
     * XML namespaces are handled by the StAX parser.
     * This is only for tracking.
     * Return true if there is a namespace declaration.
     */
    private boolean processNamespaces(StartElement startElt) {
        //Object x = startElt.getNamespaceContext();
        Iterator<Namespace> iter = startElt.getNamespaces();
        boolean result = iter.hasNext();
        if ( ReaderRDFXML_StAX_EV.TRACE ) {
            iter.forEachRemaining(namespace->{
                String prefix = namespace.getPrefix();
                String iriStr = namespace.getValue();
                trace.printf("+ PREFIX %s: <%s>\n", prefix, iriStr);
            });
        }
        return result;
    }

    /**
     * State an element (rdf:RDF, node, property)
     * Process namespace (development: XML namespaces are handled by the StAX parser).
     * Process xml:base, xml:lang.
     * These set the context for inner elements and need a stack.
     */
    private boolean startElement(StartElement startElt) {
        processNamespaces(startElt);
        boolean hasFrame = processBaseAndLang(startElt);
        return hasFrame;
    }

    private boolean processBaseAndLang(StartElement startElt) {
        IRIx xmlBase = xmlBase(startElt);
        String xmlLang = xmlLang(startElt);
        if ( ReaderRDFXML_StAX_EV.TRACE ) {
            if ( xmlBase != null )
                trace.printf("+ BASE <%s>\n", xmlBase);
            if ( xmlLang != null )
                trace.printf("+ LANG @%s\n", xmlLang);
        }
        boolean hasFrame = (xmlBase != null || xmlLang != null);
        if ( hasFrame ) {
            pushFrame(xmlBase != null ? xmlBase : currentBase,
                    xmlLang != null ? xmlLang : currentLang);
        }
        return hasFrame;
    }

    private void endElement(boolean hasFrame) {
        if ( hasFrame )
            popFrame();
    }

    // ---- Parser output

    private void emitInitialBaseAndNamespaces(StartElement startElt) {
        String xmlBase = attribute(startElt, xmlQNameBase);
        if ( xmlBase != null )
            emitBase(xmlBase, startElt.getLocation());
        startElt.getNamespaces().forEachRemaining(namespace->{
            String prefix = namespace.getPrefix();
            String iriStr = namespace.getValue();
            emitPrefix(prefix, iriStr, startElt.getLocation());
        });
    }

    interface Emitter { void emit(Node subject, Node property, Node object, Location location); }

    private void emit(Node subject, Node property, Node object, Location location) {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(property);
        Objects.requireNonNull(object);
        Objects.requireNonNull(location);
        destination.triple(Triple.create(subject, property, object));
    }

    private void emitReify(Node reify, Node subject, Node property, Node object, Location location) {
        emit(subject, property, object, location);
        if ( reify != null ) {
            emit(reify, NodeConst.nodeRDFType, RDF.Nodes.Statement, location);
            emit(reify, RDF.Nodes.subject, subject, location);
            emit(reify, RDF.Nodes.predicate, property, location);
            emit(reify, RDF.Nodes.object, object, location);
        }
    }

    private void emitBase(String base, Location location) {
        destination.base(base);
    }

    private void emitPrefix(String prefix, String iriStr, Location location) {
        destination.prefix(prefix, iriStr);
    }

    /**
     * Generate a new base IRIx.
     * If this is relative, issue a warning.
     * It is an error to use it and the error is generated
     * sin {@link #resolveIRIx}.
     */
    private IRIx xmlBase(StartElement startElt) {
        String baseStr = attribute(startElt, xmlQNameBase);
        if ( baseStr == null )
            return null;
        Location location = startElt.getLocation();
        IRIx irix = resolveIRIxNoWarning(baseStr, location);
        if ( irix.isRelative() )
            RDFXMLparseWarning("Relative URI for base: <"+baseStr+">", location);
        return irix;
    }

    private String xmlLang(StartElement startElt) {
        return attribute(startElt, xmlQNameLang);
    }

    // ---- RDF Terms (Nodes)

    private Node iriFromID(String idStr, Location location) {
        checkValidNCName(idStr, location);
        Location prev = previousUseOfID(idStr, location);
        if ( prev != null )
            // Already in use
            RDFXMLparseWarning("Reuse of rdf:ID '"+idStr+"' at "+str(prev), location);
        Node uri = iriResolve("#"+idStr, location);
        return uri;
    }

    /** Create a URI. The IRI is known to be resolved. */
    private Node iriDirect(String uriStr, Location location) {
        Objects.requireNonNull(uriStr);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        return parserProfile.createURI(uriStr, line, col);
    }

    /** Create a URI. The IRI is resolved by this operation. */
    private Node iriResolve(String uriStr, Location location) {
        Objects.requireNonNull(uriStr);
        Objects.requireNonNull(location);
        final int line = location.getLineNumber();
        final int col = location.getColumnNumber();
        return uriStr.startsWith("_:")
                ?  parserProfile.createURI(uriStr, line, col) // <_:label> syntax. Handled by the FactoryRDF via the parser profile.
                :  parserProfile.createURI(resolveIRIx(uriStr, location), line, col);
    }

    private IRIx resolveIRIx(String uriStr, Location location) {
        // This does not use the parser profile because the base stacks and unstacks in RDF/XML.
        try {
            IRIx iri = resolveIRIxNoWarning(uriStr, location);
            if ( iri.isRelative() )
                throw RDFXMLparseError("Relative URI encountered: <"+iri.str()+">" , location);
            return iri;
        } catch (IRIException ex) {
            throw RDFXMLparseError(ex.getMessage(), location);
        }
    }

    private IRIx resolveIRIxNoWarning(String uriStr, Location location) {
        try {
            return currentIriCache.get(uriStr, uri -> {
                if( currentBase != null ) {
                    return currentBase.resolve(uri);
                } else {
                    return IRIx.create(uriStr);
                }
            });
        } catch (IRIException ex) {
            throw RDFXMLparseError(ex.getMessage(), location);
        }
    }

    private Node blankNode(Location location) {
        Objects.requireNonNull(location);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        return parserProfile.createBlankNode(null, line, col);
    }

    private Node blankNode(String label, Location location) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(location);
        checkValidNCName(label, location);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        return parserProfile.createBlankNode(null, label, line, col);
    }

    private Node literal(String lexical, Location location) {
        Objects.requireNonNull(lexical);
        Objects.requireNonNull(location);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        return parserProfile.createStringLiteral(lexical, line, col);
    }

    /**
     * Create literal with a language (rdf:langString).
     * If lang is null or "", create an xsd:string
     */
    private Node literal(String lexical, String lang, Location location) {
        if ( lang == null )
            return literal(lexical, location);
        Objects.requireNonNull(lexical);
        Objects.requireNonNull(location);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        return parserProfile.createLangLiteral(lexical, lang, line, col);
    }

    private Node literalDatatype(String lexical, String datatype, Location location) {
        Objects.requireNonNull(lexical);
        Objects.requireNonNull(datatype);
        Objects.requireNonNull(location);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        RDFDatatype dt = NodeFactory.getType(datatype);
        return parserProfile.createTypedLiteral(lexical, dt, line, col);
    }

    private Node literalDatatype(String lexical, RDFDatatype datatype, Location location) {
        Objects.requireNonNull(lexical);
        Objects.requireNonNull(datatype);
        Objects.requireNonNull(location);
        int line = location.getLineNumber();
        int col = location.getColumnNumber();
        return parserProfile.createTypedLiteral(lexical, datatype, line, col);
    }

    // ---- Functions

    private boolean qNameMatches(QName qName1, QName qName2) {
        // QName actually ignores prefix for QName.equals.
        return Objects.equals(qName1.getNamespaceURI(), qName2.getNamespaceURI()) &&
                Objects.equals(qName1.getLocalPart(), qName2.getLocalPart());
    }

    private void checkValidNCName(String string, Location location) {
        //boolean isValid = XMLChar.isValidNCName(string);
        boolean isValid = XML11Char.isXML11ValidNCName(string);
        if ( ! isValid )
            RDFXMLparseWarning("Not a valid XML NCName: '"+string+"'", location);
    }

    private void noContentAllowed(XMLEvent event) {
        if ( event.isCharacters() ) {
            String content = event.asCharacters().getData();
            content = nonWhitespaceMsg(content);
            throw RDFXMLparseError("Expected XML start tag or end tag. Found text content (possible striping error): \""+content+"\"", event);
        }
    }

    private static boolean isRDF(QName qName) {
        return rdfNS.equals(qName.getNamespaceURI());
    }

    /** Test for {@code rdf:_NNNNN}. */
    private static boolean isMemberProperty(QName qName) {
        if ( ! isRDF(qName) )
            return false;
        return isMemberPropertyLocalName(qName.getLocalPart());
    }

    private static boolean isMemberPropertyLocalName(String localName) {
        if ( ! localName.startsWith("_") )
            return false;
        String number = localName.substring(1);
        if (number.startsWith("-") || number.startsWith("0"))
            return false;

        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            try {
                // It might be > Integer.MAX_VALUE !!
                java.math.BigInteger i = new java.math.BigInteger(number);
                return true;
            } catch (NumberFormatException ee) {
                return false;
            }
        }
    }

    // "nil" is in the W3C RDF test suite
    private static final Set<String> knownRDF = Set.of
            ("Bag", "Seq", "Alt", "List","XMLLiteral", "Property", "Statement",
             "type", "li", "subject", "predicate","object","value","first","rest", "nil");

    private static final Set<String> knownRDFProperties = knownRDF;
    private static final Set<String> knownRDFTypes = knownRDF;

    /**
     * Return false if acceptable: not the RDF namespace, in the RDF namespace but
     * recognized for a type.
     * If return true, issue a warning.
     */
    private boolean isNotRecognizedRDFtype(QName qName) {
        if ( ! isRDF(qName) )
            return false;
        String ln = qName.getLocalPart();
        return ! knownRDFTypes.contains(ln);
    }

    private boolean isNotRecognizedRDFproperty(QName qName) {
        if ( ! isRDF(qName) )
            return false;
        String ln = qName.getLocalPart();
        if ( isMemberPropertyLocalName(ln) )
            return false;
        return ! knownRDFProperties.contains(ln);
    }

    private void incIndent() {
        if ( ReaderRDFXML_StAX_EV.TRACE )
            trace.incIndent();
    }

    private void decIndent() {
        if ( ReaderRDFXML_StAX_EV.TRACE )
            trace.decIndent();
    }

    // ---- XMLEvents functions

//    /** Return the contents of a text-only element.*/
//    private String nextText()

    private static boolean isComment(XMLEvent event) {
        if ( event == null )
            return false;
        return event.getEventType() == XMLStreamReader.COMMENT;
    }

    private static boolean isWhitespace(XMLEvent event) {
        if ( event == null )
            return false;
        if ( event.isCharacters() ) {
            String x = event.asCharacters().getData();
            return isWhitespace(x);
        }
        return false;
    }

    private static boolean isWhitespace(CharSequence x) {
        return StringUtils.isWhitespace(x);
    }

    private static String str(Location location) {
        if ( location == null )
            return "[-,-]";
        if ( location.getLineNumber() < 0 && location.getColumnNumber() < 0 )
            return "[?,?]";
        if ( location.getLineNumber() < 0 )
            return String.format("[-, Col: %d]", location.getColumnNumber());
        if ( location.getColumnNumber() < 0 )
            return String.format("[Line: %d, -]", location.getLineNumber());
        return String.format("[Line: %d, Col: %d]", location.getLineNumber(), location.getColumnNumber());
    }

    private static String str(QName qName) {
        String prefix = qName.getPrefix();
        if ( prefix == null || prefix.isEmpty() )
            return String.format("%s", qName.getLocalPart());
        return String.format("%s:%s", qName.getPrefix(), qName.getLocalPart());
    }

    private static String str(XMLEvent event) {
        return switch (event.getEventType()) {
            case XMLEvent.START_ELEMENT ->   str(event.asStartElement().getName());
            case XMLEvent.END_ELEMENT ->     "/"+str(event.asEndElement().getName());
            case XMLEvent.CHARACTERS ->      "Event Characters";
            // @see #ATTRIBUTE
            // @see #NAMESPACE
            // @see #PROCESSING_INSTRUCTION
            // @see #SPACE:
            case XMLEvent.COMMENT ->         "Event Comment";
            case XMLEvent.START_DOCUMENT ->  "Event StartDocument";
            case XMLEvent.END_DOCUMENT ->    "Event EndDocument";
            case XMLEvent.DTD ->             "DTD";
            case XMLEvent.ENTITY_DECLARATION -> "DTD Entity Decl";
            case XMLEvent.ENTITY_REFERENCE ->   "DTD Entity Ref";
            // @see #DTD
                default ->""+event.getEventType();
        };
    }

    /** The string for the first non-whitespace */
    private static String nonWhitespaceMsg(String string) {
        final int MaxLen = 10; // Short - this is for error messages
        // Find the start of non-whitespace.
        // Slice, truncate if necessary.
        // Make safe.
        int length = string.length();
        for ( int i = 0 ; i < length ; i++ ) {
            if ( !Character.isWhitespace(string.charAt(i)) ) {
                int len = Math.min(MaxLen, length - i);
                String x = string.substring(i, i+len);
                if ( length > MaxLen )
                    x = x+"...";
                // Escape characters, especially newlines and backspaces.
                x = EscapeStr.stringEsc(x);
                x = x.stripTrailing();
                return x;
            }
        }
        throw new RDFXMLParseException("Failed to find any non-whitespace characters");
    }
}
