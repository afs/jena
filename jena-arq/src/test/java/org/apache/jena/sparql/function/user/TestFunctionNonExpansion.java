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

package org.apache.jena.sparql.function.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueInteger;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * Tests which check that functions are not expanded when {@link UserDefinedFunctionFactory#setPreserveDependencies(boolean)} is set to true
 *
 */
public class TestFunctionNonExpansion {

    @BeforeAll
    public static void setup() {
        UserDefinedFunctionFactory.getFactory().clear();
        UserDefinedFunctionFactory.getFactory().setPreserveDependencies(true);
    }

    @AfterAll
    public static void teardown() {
        UserDefinedFunctionFactory.getFactory().clear();
        UserDefinedFunctionFactory.getFactory().setPreserveDependencies(false);
    }

    @Test
    public void test_function_non_expansion_01() {
        Expr square = new E_Multiply(new ExprVar("x"), new ExprVar("x"));
        UserDefinedFunctionFactory.getFactory().add("http://example/square", square, new ArrayList<>(square.getVarsMentioned()));

        //Test that with preserveDependencies set to true that the definition of cube is not expanded
        Expr cube = new E_Multiply(new E_Function("http://example/square", new ExprList(new ExprVar("x"))), new ExprVar("x"));
        UserDefinedFunctionFactory.getFactory().add("http://example/cube", cube, new ArrayList<>(cube.getVarsMentioned()));

        UserDefinedFunctionDefinition def = UserDefinedFunctionFactory.getFactory().get("http://example/cube");
        Expr base = def.getBaseExpr();
        assertTrue(base instanceof E_Multiply);
        E_Multiply multiply = (E_Multiply)base;
        assertTrue(multiply.getArg1() instanceof E_Function);
        assertTrue(multiply.getArg2() instanceof ExprVar);
        E_Function lhs = (E_Function)multiply.getArg1();
        assertEquals("http://example/square", lhs.getFunctionIRI());
        assertEquals(1, base.getVarsMentioned().size());
    }

    @Test
    public void test_function_non_expansion_02() {
        Expr square = new E_Multiply(new ExprVar("x"), new ExprVar("x"));
        UserDefinedFunctionFactory.getFactory().add("http://example/square", square, new ArrayList<>(square.getVarsMentioned()));

        //This test illustrates that if we change the definition of square and call our function again we can
        //get a different result with dependencies preserved because the definition of the dependent function can change
        Expr cube = new E_Multiply(new E_Function("http://example/square", new ExprList(new ExprVar("x"))), new ExprVar("x"));
        UserDefinedFunctionFactory.getFactory().add("http://example/cube", cube, new ArrayList<>(cube.getVarsMentioned()));

        UserDefinedFunction f = (UserDefinedFunction) UserDefinedFunctionFactory.getFactory().create("http://example/cube");
        f.build("http://example/cube", new ExprList(new NodeValueInteger(2)));

        Expr actual = f.getActualExpr();
        NodeValue result = actual.eval(BindingFactory.empty(), LibTestExpr.createTest());
        assertEquals(8, NodeFactoryExtra.nodeToInt(result.asNode()));

        //Change the definition of the function we depend on
        square = new ExprVar("x");
        UserDefinedFunctionFactory.getFactory().add("http://example/square", square, new ArrayList<>(square.getVarsMentioned()));
        f.build("http://example/cube", new ExprList(new NodeValueInteger(2)));

        actual = f.getActualExpr();
        result = actual.eval(BindingFactory.empty(), LibTestExpr.createTest());
        assertEquals(4, NodeFactoryExtra.nodeToInt(result.asNode()));
    }
}
