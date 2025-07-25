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

package org.apache.jena.sparql.lang;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;

public class TestVarScope
{
    private static void scope(String queryStr)
    {
        Query query = QueryFactory.create(queryStr);
    }

    @Test public void scope_01() { scope("SELECT ?x { ?s ?p ?o }"); }
    @Test public void scope_02() { scope("SELECT ?s { ?s ?p ?o }"); }

    @Test public void scope_03() { scope("SELECT (?o+1 AS ?x) { ?s ?p ?o }"); }

    @Test
    public void scope_04() { assertThrows(QueryException.class, ()-> scope("SELECT (?o+1 AS ?o) { ?s ?p ?o }") ); }

    @Test
    public void scope_05() { assertThrows(QueryException.class, ()-> scope("SELECT (?o+1 AS ?x) (?o+1 AS ?x) { ?s ?p ?o }") ); }

    @Test public void scope_06() { scope("SELECT (?z+1 AS ?x) { ?s ?p ?o } GROUP BY (?o+5 AS ?z)"); }

    @Test
    public void scope_07() { assertThrows(QueryException.class, ()-> scope("SELECT (?o+1 AS ?x) { ?s ?p ?o } GROUP BY (?o+5 AS ?x)") ); }

    @Test public void scope_08() { scope("SELECT (count(*) AS ?X) (?X+1 AS ?Z) { ?s ?p ?o }"); }

    @Test public void scope_09() { scope("SELECT (count(*) AS ?X) (?X+?o AS ?Z) { ?s ?p ?o } GROUP BY ?o"); }

    @Test public void scope_10() { scope("SELECT (?o+1 AS ?x) { ?s ?p ?o MINUS { ?s ?p ?x} } "); }

    @Test
    public void scope_15() { assertThrows(QueryException.class, ()-> scope("SELECT (?o+1 AS ?x) { { SELECT (123 AS ?x) {?s ?p ?o } } } ") ); }

    @Test
    public void scope_16() { assertThrows(QueryException.class, ()-> scope("SELECT (?o+1 AS ?o) { { SELECT (123 AS ?x) {?s ?p ?o } } } ") ); }

    @Test
    public void scope_17() { assertThrows(QueryException.class, ()-> scope("SELECT (?o+1 AS ?o) { { SELECT (123 AS ?x) {?s ?p ?o FILTER(?x > 57)} } } ") ); }

    @Test public void scope_20() { scope("SELECT ?x { ?x ?p ?o } GROUP BY ?x"); }

    @Test
    public void scope_21() { assertThrows(QueryException.class, ()-> scope("SELECT ?o { ?x ?p ?o } GROUP BY ?x") ); }

    @Test
    public void scope_22() { assertThrows(QueryException.class, ()-> scope("SELECT * { ?s ?p ?o BIND(5 AS ?o) }") ); }

    @Test public void scope_23() { scope("SELECT * { ?s ?p ?o { BIND(5 AS ?o) } }"); }

    @Test
    public void scope_24() { assertThrows(QueryException.class, ()-> scope("SELECT * { { ?s ?p ?o } BIND(5 AS ?o) }") ); }

    @Test public void scope_25() { scope("SELECT * { { ?s ?p ?o } { BIND(5 AS ?o) } }"); }

    @Test public void scope_26() { scope("SELECT * { ?s ?p ?o OPTIONAL{?s ?p2 ?o2} BIND(?o2+5 AS ?z) }"); }

    @Test
    public void scope_27() { assertThrows(QueryException.class, ()-> scope("SELECT * { ?s ?p ?o OPTIONAL{?s ?p2 ?o2} BIND(5 AS ?o2) }") ); }

    @Test
    public void scope_28() { assertThrows(QueryException.class, ()-> scope("SELECT * { { ?s ?p ?o OPTIONAL{?s ?p2 ?o2} } BIND(?o+5 AS ?o2) }") ); }

    @Test
    public void scope_29() { assertThrows(QueryException.class, ()-> scope("SELECT * { ?s ?p ?o OPTIONAL{?s ?p2 ?o2} BIND(5 AS ?o) }") ); }

    @Test
    public void scope_30() { assertThrows(QueryException.class, ()-> scope("SELECT * { { ?s ?p ?o } OPTIONAL{?s ?p2 ?o2} BIND(5 AS ?o) }") ); }

    @Test
    public void scope_34() { assertThrows(QueryException.class, ()-> scope("SELECT * { { ?s ?p ?o } UNION {?s ?p2 ?o2} BIND(5 AS ?o) }") ); }

    @Test
    public void scope_35() { scope("SELECT * { ?s1 ?p1 ?z { ?s ?p ?z } UNION { BIND(5 AS ?z) } }"); }

    // Subqueries

    @Test
    public void scope_50() { assertThrows(QueryException.class, ()-> scope("SELECT * { SELECT (?o+1 AS ?o) { ?s ?p ?o }}") ); }

    @Test
    public void scope_51() {
        scope("""
                SELECT ?y {
                   { { SELECT (?x AS ?y) { ?s ?p ?x } } }
                   UNION
                   { { SELECT (?x AS ?y) { ?s ?p ?x } } }
                 }
                 """
              );
    }

    @Test
    public void scope_52() {
		assertThrows(QueryException.class,
					 ()->scope("""
					         SELECT ?y {
                               { { SELECT (?o+1 AS ?x) (?o+1 AS ?x) { ?s ?p ?o } }
                               UNION
                               { ?s ?p ?x } }
                             }
					         """
					         ));
    }

    @Test
    public void scope_63() {
        // Check nested things get checked.
        assertThrows(QueryException.class,
					 ()->scope("SELECT * { { ?s ?p ?o } UNION { ?s ?p ?o1 BIND(5 AS ?o1) } }"));
    }

    @Test
    public void scope_64() {
        // Check nested things get checked.
        assertThrows(QueryException.class,
					 ()->scope("SELECT * { { { ?s ?p ?o1 BIND(5 AS ?o1) } } }"));
    }

    @Test
    public void scope_65() {
        assertThrows(QueryException.class,
                     ()->scope("SELECT ( (?x+1) AS ?x ) {}"));
    }

    @Test
    public void scope_66() {
		assertThrows(QueryException.class,
					 ()->  scope("SELECT ( (?x+1) AS ?y)  (2 AS ?x) {}"));
    }

    // GH-3164
    @Test public void scope_70() {
        String qsVarScope = """
                SELECT ?s (MIN(?v) as ?min) WHERE { ?s <value> ?v }
                GROUP BY ?s
                VALUES ?min { 2 5 }
                """;
        scope(qsVarScope);
    }

    @Test public void scope_71() {
        scope("SELECT (MIN(?v) as ?min) WHERE {} VALUES ?min { 2 5 }");
    }
}
