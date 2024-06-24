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

package org.apache.jena.riot.process.normalize;

import java.util.function.Function;

import org.apache.jena.graph.Node ;

/**
 * Convert literals to canonical form.
 * <p>
 * Strictly, this is "normalization" - XSD Schema 1.1 does not define a canonical form for all cases.
 * <p>
 * N.B. The normalization does produce forms for decimals and doubles that are correct as Turtle syntactic forms.
 * For doubles, but not floats, zero is "0.0e0", whereas Java produces "0.0".
 * For floats, the Java is returned for values with low precision.
 *
 * @deprecated Use {@link NormalizeRDFTerms}.
 */
@Deprecated
public class CanonicalizeLiteral implements Function<Node, Node>
{
    private static CanonicalizeLiteral singleton = new CanonicalizeLiteral();

    /**
     * Suitable for use in Turtle output syntax.
     * @deprecated Use {@code NormalizeRDFTerms.get().normalize}.
     */
    @Deprecated
    public static CanonicalizeLiteral get() { return singleton ; }

    @Override
    public Node apply(Node node) {
        return NormalizeRDFTerms.getTTL().normalize(node);
    }

    /** Convert the lexical form to a canonical form if one of the known datatypes,
     * otherwise return the node argument. (same object :: {@code ==})
     * @deprecated Use {@link NormalizeRDFTerms#normalizeValue}.
     */
    @Deprecated
    public static Node canonicalValue(Node node) {
        return NormalizeRDFTerms.getTTL().normalize(node);
    }

}

//  private CanonicalizeLiteral(Map<RDFDatatype, DatatypeHandler> mapping) {
//  this.dispatchMapping = Map.copyOf(mapping);
//}


//
//    /** General canonicalization following XSD . */
//    public static CanonicalizeLiteral getXSD() { return mapXSD ; }
//
//    private static CanonicalizeLiteral mappingGeneral() {
//       Map<RDFDatatype, DatatypeHandler> mapping = initMap();
//       return new CanonicalizeLiteral(mapping);
//    }
//
//    private static CanonicalizeLiteral mappingXSD() {
//        Map<RDFDatatype, DatatypeHandler> mapping = initMap();
//        mapping.put(XSDDatatype.XSDdecimal, NormalizeValue.dtDecimalXSD);
//        mapping.put(XSDDatatype.XSDdouble, NormalizeValue.dtDoubleXSD);
//        mapping.put(XSDDatatype.XSDfloat, NormalizeValue.dtFloatXSD);
//        return new CanonicalizeLiteral(mapping);
//     }
//
//    /**
//     * Get the term normalization suitable for Turtle output.
//     * <ul>
//     * <li>xsd:decimals always have a decimal point.</li>
//     * <li>xsd:doubles always have an exponent. For ones that are less that 10E7, add
//     *     "e0", otherwise normalize the mantissa and have an exponent ('E').
//     * <li>xsd:floats For ones that are less that 10E7, just the decimal, no expoent.
//     *     Otherwise normalize the mantissa and have an expoent ('E').
//     * </ul>
//     */
//    public static CanonicalizeLiteral getTTL() { return singleton ; }
//
//
//
//    private CanonicalizeLiteral(Map<RDFDatatype, DatatypeHandler> mapping) {
//        this.dispatchMapping = Map.copyOf(mapping);
//    }
//
//    /**
//     * Canonicalize a literal, both lexical form and language tag
//     */
//    @Override
//    public Node apply(Node node) {
//        return canonicalValue(node);
//    }
//
//    /** Convert the lexical form to a canonical form if one of the known datatypes,
//     * otherwise return the node argument. (same object :: {@code ==})
//     */
//    public static Node canonicalValue(Node node) {
//        if ( ! node.isLiteral() )
//            return node ;
//        if ( NodeUtils.isLangString(node) )
//            return canonicalLangtag(node);
//        if ( NodeUtils.isSimpleString(node) )
//            return node;
//        // Is it a valid value?
//        // (Can we do this in the normal case code?)
//        if ( ! node.getLiteralDatatype().isValid(node.getLiteralLexicalForm()) )
//            // Invalid lexical form for the datatype - do nothing.
//            return node;
//
//        RDFDatatype dt = node.getLiteralDatatype() ;
//        DatatypeHandler handler = dispatch.get(dt) ;
//        if ( handler == null )
//            return node ;
//        Node n2 = handler.handle(node, node.getLiteralLexicalForm(), dt) ;
//        if ( n2 == null )
//            return node ;
//        return n2 ;
//    }
//
//    /** Convert the language tag of a lexical form to a canonical form if one of the known datatypes,
//     * otherwise return the node argument. (same object; compare by {@code ==})
//     */
//    private static Node canonicalLangtag(Node node) {
//        String langTag = node.getLiteralLanguage();
//        String langTag2 = LangTag.canonical(langTag);
//        if ( langTag2.equals(langTag) )
//            return node;
//        //String textDir = n.getLiteralTextDirection();
//        String lexicalForm = node.getLiteralLexicalForm();
//        return NodeFactory.createLiteralLang(lexicalForm, langTag2);
//    }
//
//    private static final RDFDatatype dtPlainLiteral = NodeFactory.getType(RDF.PlainLiteral.getURI());
//
//    private final static Map<RDFDatatype, DatatypeHandler> dispatch = initMap();
//
//    private static Map<RDFDatatype, DatatypeHandler> initMap() {
//        // Nulls are not allowed in this map.
//        Map<RDFDatatype, DatatypeHandler> map = new HashMap<>();
//        addAll(map);
//        return map;
//    }
//
//    /**
//     * Add the standard set of datatype handlers.
//     * This is
//     *  */
//    private static void addAll(Map<RDFDatatype, DatatypeHandler> map) {
//        map.put(XSDDatatype.XSDinteger,             NormalizeValue.dtInteger) ;
//
//        map.put(XSDDatatype.XSDdecimal,             NormalizeValue.dtDecimalTTL) ;
//        map.put(XSDDatatype.XSDfloat,               NormalizeValue.dtFloatTTL ) ;
//        map.put(XSDDatatype.XSDdouble,              NormalizeValue.dtDoubleTTL ) ;
//
//        // Subtypes. Changes the datatype.
//        map.put(XSDDatatype.XSDint,                 NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDlong,                NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDshort,               NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDbyte,                NormalizeValue.dtInteger) ;
//
//        map.put(XSDDatatype.XSDunsignedInt,         NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDunsignedLong,        NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDunsignedShort,       NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDunsignedByte,        NormalizeValue.dtInteger) ;
//
//        map.put(XSDDatatype.XSDnonPositiveInteger,  NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDnonNegativeInteger,  NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDpositiveInteger,     NormalizeValue.dtInteger) ;
//        map.put(XSDDatatype.XSDnegativeInteger,     NormalizeValue.dtInteger) ;
//
//        // Only fractional seconds part can vary for the same value.
//        map.put(XSDDatatype.XSDdateTime,            NormalizeValue.dtDateTime) ;
//        map.put(XSDDatatype.XSDboolean,             NormalizeValue.dtBoolean) ;
//
//        // Not covered.
//        //map.put(XSDDatatype.XSDduration,   null) ;
//
//        // These are fixed format
////        map.put(XSDDatatype.XSDdate,       null) ;
////        map.put(XSDDatatype.XSDtime,       null) ;
////        map.put(XSDDatatype.XSDgYear,      null) ;
////        map.put(XSDDatatype.XSDgYearMonth, null) ;
////        map.put(XSDDatatype.XSDgMonth,     null) ;
////        map.put(XSDDatatype.XSDgMonthDay,  null) ;
////        map.put(XSDDatatype.XSDgDay,       null) ;
//
//        // Convert (illegal) rdf:PlainLiteral to a legal RDF term.
//        //map.put(dtPlainLiteral,            NormalizeValue.dtPlainLiteral) ;
//
//
//    }
//}
