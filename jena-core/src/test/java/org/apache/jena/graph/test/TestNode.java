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

package org.apache.jena.graph.test;


import junit.framework.TestSuite;
import org.apache.jena.atlas.lib.Creator;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.NodeVisitor;
import org.apache.jena.graph.Node_ANY;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Node_Graph;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_Triple;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.graph.impl.LiteralLabelFactory;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.SplitIRI;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RSS;
import org.apache.jena.vocabulary.VCARD;

/**
    Exercise nodes. Make sure that the different node types do not overlap
    and that the test predicates work properly on the different node kinds.
 */
public class TestNode extends GraphTestBase
{
    public TestNode( String name )
    { super( name ); }

    public static TestSuite suite()
    { return new TestSuite( TestNode.class ); }

    private static final String U = "http://some.domain.name/magic/spells.incant";
    private static final String N = "Alice";
    private static final LiteralLabel L = LiteralLabelFactory.createLang( "ashes are burning", "en" );
    private static final String A = BlankNodeId.createFreshId();

    public void testBlanks()
    {
        assertTrue( "anonymous nodes are blank", NodeFactory.createBlankNode().isBlank() );
        assertFalse( "anonymous nodes aren't literal", NodeFactory.createBlankNode().isLiteral() );
        assertFalse( "anonymous nodes aren't URIs", NodeFactory.createBlankNode().isURI() );
        assertFalse( "anonymous nodes aren't variables", NodeFactory.createBlankNode().isVariable() );
        assertEquals( "anonymous nodes have the right id", NodeFactory.createBlankNode(A).getBlankNodeLabel(), A);
    }

    @SuppressWarnings("deprecation")
    public void testLiterals()
    {
        assertFalse( "literal nodes aren't blank", NodeFactory.createLiteral( L ).isBlank() );
        assertTrue( "literal nodes are literal", NodeFactory.createLiteral( L ).isLiteral() );
        assertFalse( "literal nodes aren't variables", NodeFactory.createLiteral( L ).isVariable() );
        assertFalse( "literal nodes aren't URIs", NodeFactory.createLiteral( L ).isURI() );
        assertEquals( "literal nodes preserve value", NodeFactory.createLiteral( L ).getLiteral(), L );
    }

    public void testURIs()
    {
        assertFalse( "URI nodes aren't blank", NodeFactory.createURI( U ).isBlank() );
        assertFalse( "URI nodes aren't literal", NodeFactory.createURI( U ).isLiteral() );
        assertFalse( "URI nodes aren't variables", NodeFactory.createURI( U ).isVariable() );
        assertTrue( "URI nodes are URIs", NodeFactory.createURI( U ).isURI() );
        assertEquals( "URI nodes preserve URI", NodeFactory.createURI( U ).getURI(), U );
    }

    public void testVariables()
    {
        assertFalse( "variable nodes aren't blank", NodeFactory.createVariable( N ).isBlank() );
        assertFalse( "variable nodes aren't literal", NodeFactory.createVariable( N ).isLiteral() );
        assertFalse( "variable nodes aren't URIs", NodeFactory.createVariable( N ).isURI() );
        assertTrue( "variable nodes are variable", NodeFactory.createVariable( N ).isVariable() );
        assertEquals( "variable nodes keep their name", N, NodeFactory.createVariable( N ).getName() );
        assertEquals( "variable nodes keep their name", N + "x", NodeFactory.createVariable( N + "x" ).getName() );
    }

    public void testANY()
    {
        assertFalse( "ANY nodes aren't blank", Node.ANY.isBlank() );
        assertFalse( "ANY nodes aren't literals", Node.ANY.isLiteral() );
        assertFalse( "ANY nodes aren't URIs", Node.ANY.isURI() );
        assertFalse( "ANY nodes aren't variables", Node.ANY.isVariable() );
        assertFalse( "ANY nodes aren't blank", Node.ANY.isBlank() );
        assertFalse( "ANY nodes aren't blank", Node.ANY.isBlank() );
    }

    public void testNodeVariableConstructor()
    {
        assertEquals( NodeFactory.createVariable( "hello" ), new Node_Variable( "hello" ) );
        assertEquals( NodeFactory.createVariable( "world" ), new Node_Variable( "world" ) );
        assertDiffer( NodeFactory.createVariable( "hello" ), new Node_Variable( "world" ) );
        assertEquals( "myName", new Node_Variable( "myName" ).getName() );
    }

    /**
        test cases for equality: an array of (Node, String) pairs. [It's not worth
        making a special class for these pairs.] The nodes are created with caching
        off, to make sure that caching effects don't hide the effect of using .equals().
        The strings are "equality groups": the nodes should test equal iff their
        associated strings test equal.
     */
    @SuppressWarnings("deprecation")
    private Object [][] eqTestCases()
    {
        String id = BlankNodeId.createFreshId();
        LiteralLabel L2 = LiteralLabelFactory.createString(id.toString());

        LiteralLabel LLang1 = LiteralLabelFactory.createLang( "xyz", "en") ;
        LiteralLabel LLang2 = LiteralLabelFactory.createLang( "xyz", "EN") ;

        String U2 = id.toString();
        String N2 = id.toString();
        return new Object [][]
            {
            { Node.ANY, "0" },
            { NodeFactory.createBlankNode( id ), "1" },
            { NodeFactory.createBlankNode(), "2" },
            { NodeFactory.createBlankNode( id ), "1" },
            { NodeFactory.createLiteral( L ), "3" },

            { NodeFactory.createLiteral( L2 ), "4" },
            { NodeFactory.createLiteral( L ), "3" },
            { NodeFactory.createURI( U ), "5" },
            { NodeFactory.createURI( U2 ), "6" },
            { NodeFactory.createURI( U ), "5" },
            { NodeFactory.createVariable( N ), "7" },
            { NodeFactory.createVariable( N2 ), "8" },
            { NodeFactory.createVariable( N ), "7" } ,

            { NodeFactory.createLiteral( LLang1 ), "9" },
            { NodeFactory.createLiteral( LLang2 ), "10" },
            };
    }

    public void testNodeEquals()
    {
        Object [][] tests = eqTestCases();
        for ( Object[] I : tests )
        {
            assertFalse( I[0] + " should not equal null", I[0].equals( null ) );
            assertFalse( I[0] + "should not equal 'String'", I[0].equals( "String" ) );
            for ( Object[] J : tests )
            {
                testEquality( I[1].equals( J[1] ), I[0], J[0] );
            }
        }
    }

    private void testEquality( boolean testEq, Object x, Object y )
    {
        String testName = getType( x ) + " " + x + " and " + getType( y ) + " " + y;
        if (testEq)
            assertEquals( testName + "should be equal", x, y );
        else
            assertDiffer( testName + " should differ", x, y );
    }

    private String getType( Object x )
    {
        String fullName = x.getClass().getName();
        return fullName.substring( fullName.lastIndexOf( '.' ) + 1 );
    }

    @SuppressWarnings("deprecation")
    public void testEquals()
    {
        assertDiffer( "different variables", NodeFactory.createVariable( "xx" ), NodeFactory.createVariable( "yy" ) );
        assertEquals( "same vars", NodeFactory.createVariable( "aa" ), NodeFactory.createVariable( "aa" ) );
        assertEquals( "same URI", NodeFactory.createURI( U ), NodeFactory.createURI( U ) );
        assertEquals( "same anon", NodeFactory.createBlankNode( A ), NodeFactory.createBlankNode( A ) );
        assertEquals( "same literal", NodeFactory.createLiteral( L ), NodeFactory.createLiteral( L ) );
        assertFalse( "distinct URIs", NodeFactory.createURI( U ) == NodeFactory.createURI( U ) );
        assertFalse( "distinct hyphens", NodeFactory.createBlankNode( A ) == NodeFactory.createBlankNode( A ) );
        assertFalse( "distinct literals", NodeFactory.createLiteral( L ) == NodeFactory.createLiteral( L ) );
        assertFalse( "distinct vars", NodeFactory.createVariable( "aa" ) == NodeFactory.createVariable( "aa" ) );
    }

    /**
        test that the label of a Node can be retrieved from that Node in
        a way appropriate to that Node.
     */
    @SuppressWarnings("deprecation")
    public void testLabels()
    {
        String id = BlankNodeId.createFreshId();
        assertEquals( "get URI value", U, NodeFactory.createURI( U ).getURI() );
        assertEquals( "get blank value", id, NodeFactory.createBlankNode( id ).getBlankNodeLabel() );
        assertEquals( "get literal value", L, NodeFactory.createLiteral( L ).getLiteral() );
        assertEquals( "get variable name", N, NodeFactory.createVariable( N ).getName() );
    }

    /**
        this is where we test that using the wrong accessor on a Node gets you
        an exception.
     */
    @SuppressWarnings("deprecation")
    public void testFailingLabels()
    {
        Node u = NodeFactory.createURI( U ), b = NodeFactory.createBlankNode();
        Node l = NodeFactory.createLiteral( L ), v = NodeFactory.createVariable( N );
        Node a = Node.ANY;
        /* */
        testGetURIFails( a );
        testGetURIFails( b );
        testGetURIFails( l );
        testGetURIFails( v );
        /* */
        testGetLiteralFails( a );
        testGetLiteralFails( u );
        testGetLiteralFails( b );
        testGetLiteralFails( v );
        /* */
        testGetNameFails( a );
        testGetNameFails( u );
        testGetNameFails( b );
        testGetNameFails( l );
        /* */
        testGetBlankNodeIdFails( a );
        testGetBlankNodeIdFails( u );
        testGetBlankNodeIdFails( l );
        testGetBlankNodeIdFails( v );
    }

    public void testGetBlankNodeIdFails( Node n )
    { try { n.getBlankNodeLabel(); fail( n.getClass() + " should fail getName()" ); } catch (UnsupportedOperationException e) {} }

    public void testGetURIFails( Node n )
    { try { n.getURI(); fail( n.getClass() + " should fail getURI()" ); } catch (UnsupportedOperationException e) {} }

    public void testGetNameFails( Node n )
    { try { n.getName(); fail( n.getClass() + " should fail getName()" ); } catch (UnsupportedOperationException e) {} }

    public void testGetLiteralFails( Node n )
    { try { n.getLiteral(); fail( n.getClass() + " should fail getLiteral()" ); } catch (UnsupportedOperationException e) {} }


    public void testGetBlankNodeLabelString()
    {
        Node n = NodeFactory.createBlankNode();
        assertNotNull(n.getBlankNodeLabel());
    }

    public void testVariableSupport()
    {
        assertEquals( new Node_Variable( "xxx" ), new Node_Variable( "xxx" ) );
        assertDiffer( new Node_Variable( "xxx" ), new Node_Variable( "yyy" ) );
    }

    /**
        Test that the create method does sensible things on null and ""
     */
    public void testCreateBadString()
    {
        try { NodeCreateUtils.create( null ); fail( "must catch null argument" ); }
        catch (NullPointerException e) {}
        catch (JenaException e) {}
        try { NodeCreateUtils.create( "" ); fail("must catch empty argument" ); }
        catch (JenaException e) {}
    }

    /**
        Test that anonymous nodes are created with the correct labels
     */
    public void testCreateBlankNode()
    {
        String idA = "_xxx";
        String idB = "_yyy";
        Node a = NodeCreateUtils.create( idA );
        Node b = NodeCreateUtils.create( idB );
        assertTrue( "both must be bnodes", a.isBlank() && b.isBlank() );
        assertEquals( NodeFactory.createBlankNode( idA ).getBlankNodeLabel(), a.getBlankNodeLabel() );
        assertEquals( NodeFactory.createBlankNode( idB ).getBlankNodeLabel(), b.getBlankNodeLabel() );
    }

    public void testCreateVariable()
    {
        String V = "wobbly";
        Node v = NodeCreateUtils.create( "?" + V );
        assertTrue( "must be a variable", v.isVariable() );
        assertEquals( "name must be correct", V, v.getName() );
    }

    public void testCreateANY()
    {
        assertEquals( "?? must denote ANY", Node.ANY, NodeCreateUtils.create( "??" ) );
    }

    public void testCreatePlainLiteralSingleQuotes()
    {
        Node n = NodeCreateUtils.create( "'xxx'" );
        assertEquals( "xxx", n.getLiteralLexicalForm() );
        assertString(n);
    }

    public void testCreatePlainLiteralDoubleQuotes()
    {
        Node n = NodeCreateUtils.create( "\"xxx\"" );
        assertEquals( "xxx", n.getLiteralLexicalForm() );
        assertString(n);
    }

    public void testCreateLiteralBackslashEscape()
    {
        testStringConversion( "xx\\x", "'xx\\\\x'" );
        testStringConversion( "xx\\x\\y", "'xx\\\\x\\\\y'" );
        testStringConversion( "\\xyz\\", "'\\\\xyz\\\\'" );
    }

    public void testCreateLiteralQuoteEscapes()
    {
        testStringConversion( "x\'y", "'x\\'y'" );
        testStringConversion( "x\"y", "'x\\\"y'" );
        testStringConversion( "x\'y\"z", "'x\\\'y\\\"z'" );
    }

    public void testCreateLiteralOtherEscapes()
    {
        testStringConversion( " ", "'\\s'" );
        testStringConversion( "\t", "'\\t'" );
        testStringConversion( "\n", "'\\n'" );
    }

    protected void testStringConversion(String wanted, String template) {
        Node n = NodeCreateUtils.create(template) ;
        assertEquals(wanted, n.getLiteralLexicalForm()) ;
        assertEquals("", n.getLiteralLanguage()) ;
        assertString(n) ;
    }

    public void testCreateLanguagedLiteralEN1()
    {
        Node n = NodeCreateUtils.create( "'chat'en-UK" );
        assertEquals( "chat", n.getLiteralLexicalForm() );
        assertLangString(n);
        assertEquals( "en-UK", n.getLiteralLanguage() );
    }

    public void testCreateLanguagedLiteralEN2()
    {
        Node n1 = NodeCreateUtils.create( "'chat'en-UK" );
        Node n2 = NodeCreateUtils.create( "'chat'EN-UK" );
        assertTrue( n1.sameValueAs(n2) ) ;
        // Jena5: normalized language tags.
        assertTrue( n1.equals(n2) ) ;
    }

    public void testCreateLanguagedLiteralXY()
    {
        Node n = NodeCreateUtils.create( "\"chat\"xy-AB" );
        assertEquals( "chat", n.getLiteralLexicalForm() );
        assertEquals( "xy-AB", n.getLiteralLanguage() );
        assertLangString(n) ;
    }

    public void testCreateTypedLiteralInteger()
    {
        Node n = NodeCreateUtils.create( "'42'xsd:integer" );
        assertEquals( "42", n.getLiteralLexicalForm() );
        assertEquals( "", n.getLiteralLanguage() );
        assertEquals( expand( "xsd:integer" ), n.getLiteralDatatypeURI() );
    }

    public void testCreateTypedLiteralBoolean()
    {
        Node n = NodeCreateUtils.create( "\"true\"xsd:boolean" );
        assertEquals( "true", n.getLiteralLexicalForm() );
        assertEquals( "", n.getLiteralLanguage() );
        assertEquals( expand( "xsd:boolean" ), n.getLiteralDatatypeURI() );
    }

    public void testGetPlainLiteralLexicalForm()
    {
        Node n = NodeCreateUtils.create( "'stuff'" );
        assertEquals( "stuff", n.getLiteralLexicalForm() );
    }

    public void testGetNumericLiteralLexicalForm()
    {
        Node n = NodeCreateUtils.create( "17" );
        assertEquals( "17", n.getLiteralLexicalForm() );
    }

    public void testTypesExpandPrefix()
    {
        testTypeExpandsPrefix( "rdf:spoo" );
        testTypeExpandsPrefix( "rdfs:bar" );
        testTypeExpandsPrefix( "owl:henry" );
        testTypeExpandsPrefix( "xsd:bool" );
        testTypeExpandsPrefix( "unknown:spoo" );
    }

    private void testTypeExpandsPrefix( String type )
    {
        Node n = NodeCreateUtils.create( "'stuff'" + type );
        String wanted = PrefixMapping.Extended.expandPrefix( type );
        assertEquals( wanted, n.getLiteralDatatypeURI() );
    }

    public void testCreateURI()
    {
        String uri = "http://www.electric-hedgehog.net/";
        testCreateURI( uri );
        testCreateURI( "rdf:trinket", "http://www.w3.org/1999/02/22-rdf-syntax-ns#trinket" );
        testCreateURI( "rdfs:device", "http://www.w3.org/2000/01/rdf-schema#device" );
        testCreateURI( "dc:creator", DC.getURI() + "creator" );
        testCreateURI( "rss:something", RSS.getURI() + "something" );
        testCreateURI( "vcard:TITLE", VCARD.getURI() + "TITLE" );
        testCreateURI( "owl:wol", OWL.getURI() + "wol" );
    }

    public void testCreateURIOtherMap()
    {
        String myNS = "eh:foo/bar#", suffix = "something";
        PrefixMapping mine = PrefixMapping.Factory.create().setNsPrefix( "mine", myNS );
        Node n = NodeCreateUtils.create( mine, "mine:" + suffix );
        assertEquals( myNS + suffix, n.getURI() );
    }

    private void testCreateURI( String inOut )
    { testCreateURI( inOut, inOut ); }

    private void testCreateURI( String in, String wanted )
    {
        String got = NodeCreateUtils.create( in ).getURI();
        if (!wanted.equals( got ))
        {
            if (in.equals( wanted )) fail( "should preserve " + in );
            else fail( "should translate " + in + " to " + wanted + " not " + got );
        }
    }

    public void testCreatePrefixed()
    {
        PrefixMapping pm = PrefixMapping.Factory.create();
        NodeCreateUtils.create( pm, "xyz" );
    }

    public void testToStringWithPrefixMapping()
    {
        PrefixMapping pm = PrefixMapping.Factory.create();
        String prefix = "spoo", ns = "abc:def/ghi#";
        pm.setNsPrefix( prefix, ns );
        String suffix = "bamboozle";
        assertEquals( prefix + ":" + suffix, NodeCreateUtils.create( ns + suffix ).toString( pm ) );
    }

    public void testNodeHelp()
    {
        assertTrue( "node() making URIs", node( "hello" ).isURI() );
        assertTrue( "node() making literals", node( "123" ).isLiteral() );
        assertTrue( "node() making literals", node( "'hello'" ).isLiteral() );
        assertTrue( "node() making hyphens", node( "_x" ).isBlank() );
        assertTrue( "node() making variables", node( "?x" ).isVariable() );
    }

    public void testVisitorPatternNode()
    {
        NodeVisitor returnNode = new NodeVisitor()
        {
            @Override
            public Object visitAny( Node_ANY it ) { return it; }
            @Override
            public Object visitBlank( Node_Blank it, String id ) { return it; }
            @Override
            public Object visitLiteral( Node_Literal it, String lex, String lang, RDFDatatype dtype) { return it; }
            @Override
            public Object visitURI( Node_URI it, String uri ) { return it; }
            @Override
            public Object visitVariable( Node_Variable it, String name ) { return it; }
            @Override
            public Object visitTriple(Node_Triple it, Triple triple) { return it; }
            @Override
            public Object visitGraph(Node_Graph it, Graph graph) { return it; }
        };
        testVisitorPatternNode( "sortOfURI", returnNode );
        testVisitorPatternNode( "?variable", returnNode );
        testVisitorPatternNode( "_anon", returnNode );
        testVisitorPatternNode( "11", returnNode );
        testVisitorPatternNode( "??", returnNode );
    }

    private void testVisitorPatternNode( String ns, NodeVisitor v )
    {
        Node n = node( ns );
        assertEquals( n, n.visitWith( v ) );
    }

    private void visitExamples( NodeVisitor nv )
    {
        node( "sortOfURI" ).visitWith( nv );
        node( "?variableI" ).visitWith( nv );
        node( "_anon" ).visitWith( nv );
        node( "11" ).visitWith( nv );
        node( "??" ).visitWith( nv );
        // ---
        Node s = node( "uri1" );
        Node p = node( "uri2" );
        Node o = node( "uri1" );
        Node nt = NodeFactory.createTripleTerm(s, p, o);
        nt.visitWith(nv);
        // ---
        Graph g = GraphMemFactory.empty();
        Node_Graph ng = new Node_Graph(g);
        ng.visitWith(nv);
    }

    public void testVisitorPatternValue()
    {
        NodeVisitor checkValue = new NodeVisitor()
        {
            @Override
            public Object visitAny( Node_ANY it )
            { return null; }
            @Override
            public Object visitBlank( Node_Blank it, String label )
            { assertTrue( it.getBlankNodeLabel() == label ); return null; }
            @Override
            public Object visitLiteral( Node_Literal it, String lex, String lang, RDFDatatype dtype) {
                assertEquals(lex, it.getLiteralLexicalForm());
                assertEquals(lang, it.getLiteralLanguage());
                assertEquals(dtype, it.getLiteralDatatype());
                return null;
            }

            @Override
            public Object visitURI( Node_URI it, String uri )
            { assertTrue( it.getURI() == uri ); return null; }
            @Override
            public Object visitVariable( Node_Variable it, String name )
            { assertEquals( it.getName(), name ); return null; }
            @Override
            public Object visitTriple(Node_Triple it, Triple triple)
            { assertEquals( it.getTriple(), triple ); return null; }
            @Override
            public Object visitGraph(Node_Graph it, Graph graph)
            { assertEquals( it.getGraph(), graph ); return null; }
        };
        visitExamples( checkValue );
    }

    /**
        Test that the appropriate elements of the visitor are called exactly once;
        this relies on the order of the visits in visitExamples.
     */
    public void testVisitorPatternCalled()
    {
        final String [] strings = new String [] { "" };
        NodeVisitor checkCalled = new NodeVisitor()
        {
            @Override
            public Object visitAny( Node_ANY it )
            { strings[0] += " any"; return null; }
            @Override
            public Object visitBlank( Node_Blank it, String id )
            { strings[0] += " blank"; return null; }
            @Override
            public Object visitLiteral( Node_Literal it, String lex, String lang, RDFDatatype dtype)
            { strings[0] += " literal"; return null; }
            @Override
            public Object visitURI( Node_URI it, String uri )
            { strings[0] += " uri"; return null; }
            @Override
            public Object visitVariable( Node_Variable it, String name )
            { strings[0] += " variable"; return null; }
            @Override
            public Object visitTriple(Node_Triple it, Triple triple)
            { strings[0] += " termTriple"; return null; }
            @Override
            public Object visitGraph(Node_Graph it, Graph graph)
            { strings[0] += " termGraph"; return null; }

        };
        String desired = " uri variable blank literal any termTriple termGraph";
        visitExamples( checkCalled );
        assertEquals( "all visits must have been made", desired, strings[0] );
    }

    public void testSimpleMatches()
    {
        assertTrue( NodeCreateUtils.create( "S").matches( NodeCreateUtils.create( "S" ) ) );
        assertFalse(  "", NodeCreateUtils.create( "S").matches( NodeCreateUtils.create( "T" ) ) );
        assertFalse( "", NodeCreateUtils.create( "S" ).matches( null ) );
        assertTrue( NodeCreateUtils.create( "_X").matches( NodeCreateUtils.create( "_X" ) ) );
        assertFalse( "", NodeCreateUtils.create( "_X").matches( NodeCreateUtils.create( "_Y" ) ) );
        assertFalse( "", NodeCreateUtils.create( "_X").matches( null ) );
        assertTrue( NodeCreateUtils.create( "10" ).matches( NodeCreateUtils.create( "10" ) ) );
        assertFalse( "", NodeCreateUtils.create( "10" ).matches( NodeCreateUtils.create( "11" ) ) );
        assertFalse( "", NodeCreateUtils.create( "10" ).matches( null ) );
        assertTrue( Node.ANY.matches( NodeCreateUtils.create( "S" ) ) );
        assertTrue( Node.ANY.matches( NodeCreateUtils.create( "_X" ) ) );
        assertTrue( Node.ANY.matches( NodeCreateUtils.create( "10" ) ) );
        assertFalse( "", Node.ANY.matches( null ) );
    }

    public void testDataMatches()
    {
        TypeMapper tm = TypeMapper.getInstance();
        RDFDatatype dt1 = tm.getTypeByValue( Integer.valueOf( 10 ) );
        RDFDatatype dt2 = tm.getTypeByValue( Short.valueOf( (short) 10 ) );
        Node a = NodeFactory.createLiteralDT( "10", dt1 );
        Node b = NodeFactory.createLiteralDT( "10", dt2 );
        assertDiffer( "types must make a difference", a, b );
        assertTrue( "A and B must express the same value", a.sameValueAs( b ) );
        assertTrue( "matching literals must respect sameValueAs", a.matches( b ) );
    }

    public void testLiteralToString()
    {
        TypeMapper tm = TypeMapper.getInstance();
        RDFDatatype dtInt = tm.getTypeByValue( Integer.valueOf( 10 ) );
        Node plain = NodeFactory.createLiteralLang( "rhubarb", "");
        Node english = NodeFactory.createLiteralLang( "eccentric", "en-UK");
        Node typed = NodeFactory.createLiteralDT( "10", dtInt );
        assertEquals( "\"rhubarb\"", plain.toString() );
        assertEquals( "\"eccentric\"@en-UK", english.toString() );
        assertEquals( "\"10\"^^xsd:int", typed.toString() );
    }

    public void testGetIndexingValueURI()
    {
        Node u = NodeCreateUtils.create( "eh:/telephone" );
        assertSame( u, u.getIndexingValue() );
    }

    public void testGetIndexingValueBlank()
    {
        Node b = NodeCreateUtils.create( "_television" );
        assertSame( b, b.getIndexingValue() );
    }

    public void testGetIndexingValuePlainString()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "'literally'" ) ); }

    public void testGetIndexingValueLanguagedString()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "'chat'fr" ) ); }

    public void testGetIndexingValueXSDString()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "'string'xsd:string" ) ); }

    // JENA-1936
    public void testGetIndexingValueHexBinary1()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "''xsd:hexBinary" ) ); }

    public void testGetIndexingValueHexBinary2()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "'ABCD'xsd:hexBinary" ) ); }

    public void testGetIndexingValueBase64Binary1()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "''xsd:base64Binary" ) ); }

    // "sure." encodes to "c3VyZS4="
    public void testGetIndexingValueBase64Binary2()
    { testIndexingValueLiteral( ()->NodeCreateUtils.create( "'c3VyZS4='xsd:base64Binary" ) ); }

    private void testIndexingValueLiteral( Creator<Node> creator) {
        Node n1 = creator.create();
        Node n2 = creator.create();
        testIndexingValueLiteral(n1,n2);
    }

    private void testIndexingValueLiteral(Node n1, Node n2) {
        assertNotSame(n1, n2); // Test the test.
        assertEquals(n1.getLiteral().getIndexingValue(), n2.getIndexingValue());
        assertEquals(n1.getLiteral().getIndexingValue().hashCode(), n2.getIndexingValue().hashCode());
    }

    public void  testGetLiteralValuePlainString()
    {
        Node s = NodeCreateUtils.create( "'aString'" );
        assertSame( s.getLiteral().getValue(), s.getLiteralValue() );
    }

    public void testGetLiteralDatatypePlainString()
    {
        assertString(NodeCreateUtils.create( "'plain'" )) ;
    }

    public void testConcrete()
    {
        assertTrue( NodeCreateUtils.create( "S" ).isConcrete() );
        assertTrue( NodeCreateUtils.create( "_P" ).isConcrete() );
        assertTrue( NodeCreateUtils.create( "11" ).isConcrete() );
        assertTrue( NodeCreateUtils.create( "'hello'" ).isConcrete() );
        assertFalse( NodeCreateUtils.create( "??" ).isConcrete() );
        assertFalse( NodeCreateUtils.create( "?x" ).isConcrete() );
    }

    static String [] someURIs = new String []
        {
        "http://domainy.thing/stuff/henry",
        "http://whatever.com/stingy-beast/bee",
        "ftp://erewhon/12345",
        "potatoe:rhubarb"
        };

    /**
        test that URI nodes have namespace/localname splits which are consistent
        with Util.splitNamepace.
     */
    public void testNamespace()
    {
        for ( String uri : someURIs )
        {
            int split = SplitIRI.splitXML( uri );
            Node n = NodeCreateUtils.create( uri );
            assertEquals( "check namespace", uri.substring( 0, split ), n.getNameSpace() );
            assertEquals( "check localname", uri.substring( split ), n.getLocalName() );
        }
    }

    protected static String [] someNodes =
    {
        "42",
        "'hello'",
        "_anon",
        "'robotic'tick",
        "'teriffic'abc:def"
    };

    public void testHasURI()
    {
        for ( String someURI : someURIs )
        {
            testHasURI( someURI );
        }
        for ( String someNode : someNodes )
        {
            testHasURI( someNode );
        }
    }

    protected void testHasURI( String uri )
    {
        Node n = NodeCreateUtils.create( uri );
        assertTrue( uri, !n.isURI() || n.hasURI( uri ) );
        assertFalse( uri, n.hasURI( uri + "x" ) );
    }

    private static void assertString(Node n) {
        RDFDatatype dt = n.getLiteralDatatype() ;
        assertEquals("", n.getLiteralLanguage() ) ;
        assertEquals(XSDDatatype.XSDstring, dt) ;
    }

    private static void assertLangString(Node n) {
        RDFDatatype dt = n.getLiteralDatatype() ;
        assertDiffer("", n.getLiteralLanguage() ) ;    // "" is not legal.
        assertEquals(RDF.dtLangString, dt) ;
    }

    /**
     	Answer the string <code>s</code> prefix-expanded using the built-in
     	PrefixMapping.Extended.
     */
    private String expand( String s )
    { return PrefixMapping.Extended.expandPrefix( s ); }
}
