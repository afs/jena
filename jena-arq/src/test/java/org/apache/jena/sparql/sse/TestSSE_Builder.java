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

package org.apache.jena.sparql.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpLib;
import org.apache.jena.sparql.algebra.TableFactory;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_IsNumeric;
import org.apache.jena.sparql.expr.E_SameTerm;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.sparql.sse.builders.BuilderNode;
import org.apache.jena.sparql.sse.builders.SSE_ExprBuildException;
import org.apache.jena.vocabulary.XSD;

public class TestSSE_Builder
{
    @Test public void test_01() { SSE.parseTriple("[triple ?s ?p ?o]"); }
    @Test public void test_02() { SSE.parseTriple("[?s ?p ?o]"); }
    @Test public void test_03() { SSE.parseTriple("[?s ?p ?o]"); }
    @Test public void test_04() { SSE.parseTriple("(?s ?p ?o)"); }

    @Test public void test_05() { SSE.parseQuad("(_ ?s ?p ?o)"); }
    @Test public void test_06() { SSE.parseQuad("(quad _ ?s ?p ?o)"); }

    @Test public void test_10() { SSE.parseExpr("1"); }
    @Test public void test_11() { SSE.parseExpr("(+ 1 2)"); }

    @Test public void testOp_01() { opSame("(null)"); }
    @Test public void testOp_02() { opSame("(null)", OpNull.create()); }
    @Test public void testOp_03() { opSame("(bgp [triple ?s ?p ?o])"); }

    @Test public void testOp_04() { opSame("(label 'ABC' (table unit))", OpLabel.create("ABC", OpLib.unit())); }

    private static void opSame(String str) {
        opSame(str, SSE.parseOp(str));
    }

    private static void opSame(String str, Op other) {
        Op op = SSE.parseOp(str);
        assertEquals(op, other);
    }

    @Test
    public void testBuildInt_01() {
        Item item = SSE.parseItem("1");
        int i = BuilderNode.buildInt(item);
        assertEquals(1, i);
    }

    @Test
    public void testBuildInt_02() {
        Item item = SSE.parseItem("1");
        int i = BuilderNode.buildInt(item, 23);
        assertEquals(1, i);
    }

    @Test
    public void testBuildInt_03() {
        Item item = SSE.parseItem("_");
        int i = BuilderNode.buildInt(item, 23);
        assertEquals(23, i);
    }

    @Test
    public void testBuildNode_01() {
        Item item = SSE.parseItem("ANY");
        Node n = BuilderNode.buildNode(item);
        assertSame(Node.ANY, n);
    }

    @Test
    public void testBuildNode_02() {
        Item item = SSE.parseItem("_");
        Node n = BuilderNode.buildNode(item);
        assertSame(Node.ANY, n);
    }

    @Test
    public void testBuildNode_03() {
        Item item = SSE.parseItem("<http://example/>");
        Node n = BuilderNode.buildNode(item);
        assertTrue(n.isURI());
        assertEquals("http://example/", n.getURI());
    }

    @Test
    public void testBuildNode_04() {
        // Jena skolemized blank node.
        Item item = SSE.parseItem("<_:cba>");
        Node n = BuilderNode.buildNode(item);
        assertTrue(n.isBlank());
        assertEquals("cba", n.getBlankNodeLabel());
    }

    @Test
    public void testBuildNode_05() {
        Item item = SSE.parseItem("?variable");
        Node n = BuilderNode.buildNode(item);
        assertTrue(Var.isVar(n));
        assertEquals("variable", ((Var)n).getVarName());
    }

    @Test
    public void testBuildNode_06() {
        Item item = SSE.parseItem("true");
        Node n = BuilderNode.buildNode(item);
        assertTrue(n.isLiteral());
        assertEquals("true", n.getLiteralLexicalForm());
        assertEquals(XSD.xboolean.getURI(), n.getLiteralDatatype().getURI());
    }

    @Test
    public void testBuildLong_01() {
        Item item = SSE.parseItem("100000000000");
        long i = BuilderNode.buildLong(item);
        assertEquals(100000000000L, i);
    }

    @Test
    public void testBuildLong_02() {
        Item item = SSE.parseItem("100000000000");
        long i = BuilderNode.buildLong(item, 23);
        assertEquals(100000000000L, i);
    }

    @Test
    public void testBuildLong_03() {
        Item item = SSE.parseItem("_");
        long i = BuilderNode.buildLong(item, 23);
        assertEquals(23, i);
    }

    @Test
    public void testBuildExpr_01() {
        Expr e = SSE.parseExpr("(sameTerm (?x) (?y))");
        assertTrue(e instanceof E_SameTerm);
    }

    @Test
    public void testBuildExpr_02() {
        Expr e = SSE.parseExpr("(isNumeric ?x)");
        assertTrue(e instanceof E_IsNumeric);
    }

    private static void testExprForms(String str1, String str2) {
        Expr e1 = SSE.parseExpr(str1);
        Expr e2 = SSE.parseExpr(str2);
        assertEquals(e1, e2);
    }

    @Test
    public void testBuildExpr_03()  {
        testExprForms("(add ?x ?y)",
                      "(+ ?x ?y)");
    }

    @Test
    public void testBuildExpr_04() {
        testExprForms("(subtract ?x ?y)",
                      "(- ?x ?y)");
    }

    @Test
    public void testBuildExpr_05() {
        testExprForms("(multiply ?x ?y)",
                      "(* ?x ?y)");
    }

    @Test
    public void testBuildExpr_06() {
        testExprForms("(divide ?x ?y)",
                      "(/ ?x ?y)");
    }

    @Test
    public void testBuildExpr_07() {
        testExprForms("(lt ?x ?y)",
                      "(< ?x ?y)");
    }

    @Test
    public void testBuildExpr_08() {
        testExprForms("(le ?x ?y)",
                      "(<= ?x ?y)");
    }

    @Test
    public void testBuildExpr_09() {
        testExprForms("(gt ?x ?y)",
                      "(> ?x ?y)");
    }

    @Test
    public void testBuildExpr_10() {
        testExprForms("(ge ?x ?y)",
                      "(>= ?x ?y)");
    }

    @Test
    public void testBuildExpr_11() {
        testExprForms("(unaryplus ?x)",
                      "(+ ?x)");
    }

    @Test
    public void testBuildExpr_12() {
        testExprForms("(unaryminus ?x)",
                      "(- ?x)");
    }

    @Test
    public void testBuildExpr_13() {
        testExprForms("(eq ?x ?y)",
                      "(= ?x ?y)");
    }

    @Test
    public void testBuildExpr_14() {
        testExprForms("(ne ?x ?y)",
                      "(!= ?x ?y)");
    }

    @Test
    public void testBuildTable_01() {
        Op expected = OpLib.unit();
        Op actual = SSE.parseOp("(table unit)");
        assertEquals(expected, actual);
    }

    @Test
    public void testBuildTable_02() {
        Op expected = OpLib.empty();
        Op actual = SSE.parseOp("(table empty)");
        assertEquals(expected, actual);
    }

    @Test
    public void testBuildTable_03() {
        Op expected = OpTable.create(TableFactory.create(Var.alloc("x"), NodeConst.nodeTrue));
        Op actual = SSE.parseOp("(table (vars ?x) (row (?x true)))");
        assertEquals(expected, actual);
    }

    @Test
    public void testBuildTable_04() {
        // Can't test for equality because can't create a BNode in a way that equality will
        // succeed because OpTable does strict equality and ignores NodeIsomorphismMap
        SSE.parseOp("(table (vars ?x) (row (?x _:test)))");
    }

    @Test
    public void testBuildTableBad_01() {
        assertThrows(SSE_ExprBuildException.class, ()->
            SSE.parseOp("(table (vars ?x) (row (?x (table unit))))")
            );
    }

    @Test
    public void testBuildTableBad_02() {
        assertThrows(SSE_ExprBuildException.class, ()->
            SSE.parseOp("(table (vars ?x) (row (?x _)))")
            );
    }
}
