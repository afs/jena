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

package org.apache.jena.rdf12.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.vocabulary.RDF;

// RDF Star CG tests, converted to SPARQL 1.2

public class TestSPARQL12Parse {

    static { JenaSystem.init(); }

    // See also testing/ARQ/Syntax/Syntax-ARQ
    private static final String PREFIXES = StrUtils.strjoinNL
                ("PREFIX rdf: <"+RDF.getURI()+">"
                ,"PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>"
                ,"PREFIX xsd:     <http://www.w3.org/2001/XMLSchema#>"
                ,"PREFIX :    <http://example/>"
                ,""
                );

    private static Query parse(String string) {
        Query query = QueryFactory.create(PREFIXES+"SELECT * "+string, Syntax.syntaxARQ);
        return query;
    }

    private static void parseVars(String string, String... varNames) {
        Query query = parse(string);

        List<String> vars = query.getResultVars();
        assertEquals(varNames.length, vars.size(),
                     ()->"Wrong number of variables: "+Arrays.asList(varNames)+" : query: "+vars);
        for ( String v : varNames )
            assertTrue(vars.contains(v), ()->"Expected variable ?"+v);
    }

    @Test public void parse_good_1()    { parse("{ << :s :p :o >> :q 456 }"); }

    @Test public void parse_good_2()    { parse("{ ?X :q << ?s ?p 123 >> }"); }

    @Test public void parse_good_3()    { parse("{ << ?s ?p 123 >> :q << ?s ?p 123 >> }"); }

    @Test public void parse_good_4()    { parse("{ << << :s :p 12 >> ?p << :s :p 34 >> >> :q << ?s ?p 123 >> }"); }

    @Test public void parse_vars_1()    { parseVars("{ ?X :q << ?s ?p 123 >> }", "X", "s", "p"); }

    @Test public void parse_vars_2()    { parseVars("{ ?X :q << ?s :p <<:s :q ?q>> >> }", "X", "s", "q"); }

    // Test structures created.
    @Test public void build_1()    {
        Triple t = build1("{ <<( :s :p :o )>> :q 456 }");
        assertTrue(t.isConcrete());
        assertTrue(t.getSubject().isTripleTerm());
    }

    @Test public void build_2()    {
        Triple t = build1("{ :x  :q <<( :a :b <<( :s ?p :o )>> )>> }");
        assertFalse(t.isConcrete());
        assertTrue(t.getObject().isTripleTerm());
        assertTrue(t.getObject().getTriple().getObject().isTripleTerm());
    }

    @Test public void build_3()    {
        Triple t = build1("{ <<( :s ?p :o )>> :q 456 }");
        assertFalse(t.isConcrete());
        assertTrue(t.getSubject().isTripleTerm());
    }

    // OpAsQuery
    @Test public void queryToOpToQuery_1()    {
        queryToOpToQuery("{ << :s :p :o >> :q 456 }");
    }

    @Test public void queryToOpToQuery_2()    {
        queryToOpToQuery("{ << ?s ?p ?o >> :q 456 }");
    }

    @Test public void queryToOpToQuery_3()    {
        queryToOpToQuery("{ << << :s :p ?x2 >> ?p << :s :p ?x2 >> >> :q << ?s ?p 123 >> }");
    }

    private static void queryToOpToQuery(String string) {
        Query query = parse(string);
        Op op = Algebra.compile(query);
        query.getPrefixMapping().clearNsPrefixMap();
        Query query2 = OpAsQuery.asQuery(op);
        assertEquals(query, query2);
    }

    private static Triple build1(String string) {
        Query query = parse(string);
        Op op = Algebra.compile(query);
        BasicPattern bgp = ((OpBGP)op).getPattern();
        assertEquals(1, bgp.size());
        Triple t = bgp.get(0);
        return t ;
    }

    @Test
    public void reifier_declaration() {
        parse("{ <<:s :p :o>> }");
    }

    @Test
    public void parse_bad_2() {
        assertThrows(QueryParseException.class,
                     () -> parse("{ ?X << :s :p 123 >> ?Z }"));
    }

    @Test
    public void parse_bad_3() {
        assertThrows(QueryParseException.class,
                     () -> parse("{ << :subject << :s :p 12 >> :object >> :q 123 }"));
    }
}
