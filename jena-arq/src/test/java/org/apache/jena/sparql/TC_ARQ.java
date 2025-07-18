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

package org.apache.jena.sparql;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.apache.jena.query.TS_Query;
import org.apache.jena.rdf12.TS_RDF12;
import org.apache.jena.sparql.algebra.TS_Algebra;
import org.apache.jena.sparql.algebra.optimize.TS_Optimization;
import org.apache.jena.sparql.api.TS_API;
import org.apache.jena.sparql.core.TS_SparqlCore;
import org.apache.jena.sparql.core.assembler.TS_Assembler;
import org.apache.jena.sparql.core.mem.TS_DatasetTxnMem;
import org.apache.jena.sparql.engine.TS_Engine;
import org.apache.jena.sparql.engine.binding.TS_EngineBinding;
import org.apache.jena.sparql.engine.index.TS_Minus;
import org.apache.jena.sparql.engine.iterator.TS_QueryIterators;
import org.apache.jena.sparql.engine.join.TS_Join;
import org.apache.jena.sparql.exec.TS_ExecSPARQL;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.TS_Expr;
import org.apache.jena.sparql.function.library.TS_LibraryFunctions;
import org.apache.jena.sparql.function.scripting.TS_FunctionScripting;
import org.apache.jena.sparql.function.user.TS_UserFunctions;
import org.apache.jena.sparql.graph.TS_Graph;
import org.apache.jena.sparql.lang.TS_LangSPARQL;
import org.apache.jena.sparql.lib.TS_SparqlLib;
import org.apache.jena.sparql.modify.TS_Update;
import org.apache.jena.sparql.negation.TS_Negation;
import org.apache.jena.sparql.path.TS_Path;
import org.apache.jena.sparql.pfunction.library.TS_PFunction;
import org.apache.jena.sparql.resultset.TS_ResultSet;
import org.apache.jena.sparql.solver.TS_Solver;
import org.apache.jena.sparql.sse.TS_SSE;
import org.apache.jena.sparql.syntax.TS_Syntax;
import org.apache.jena.sparql.transaction.TS_Transaction;
import org.apache.jena.sparql.util.TS_DyadicDatasetGraphs;
import org.apache.jena.sparql.util.TS_Util;
import org.apache.jena.sparql.util.compose.TS_DatasetCollectors;
import org.apache.jena.sparql.util.iso.TS_Iso;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
@Suite
@SelectClasses({

    TS_SSE.class
    , TS_LangSPARQL.class

    , TS_Graph.class
    , TS_DyadicDatasetGraphs.class
    , TS_DatasetCollectors.class
    , TS_Util.class
    , TS_Iso.class

    , TS_Expr.class
    , TS_LibraryFunctions.class
    , TS_UserFunctions.class
    , TS_FunctionScripting.class
    , TS_PFunction.class

    , TS_EngineBinding.class
    , TS_Engine.class
    , TS_Negation.class
    , TS_Solver.class
    , TS_Algebra.class
    , TS_Join.class
    , TS_Minus.class
    , TS_QueryIterators.class
    , TS_Optimization.class
    , TS_ResultSet.class
    , TS_Syntax.class
    , TS_API.class
    , TS_ExecSPARQL.class
    , TS_SparqlCore.class
    , TS_Assembler.class
    , TS_DatasetTxnMem.class
    , TS_Path.class
    , TS_Update.class
    , TS_Transaction.class
    , TS_SparqlLib.class

    // Outside org.apache.jena.sparql
    , TS_Query.class

    // RDF 1.2 tests. These track the working group.
    , TS_RDF12.class
    // And scripted tests.
})

public class TC_ARQ
{
    private static boolean bVerboseWarnings;
    private static boolean bWarnOnUnknownFunction;

    @BeforeAll
    public static void beforeClass() {
        bVerboseWarnings = NodeValue.VerboseWarnings;
        bWarnOnUnknownFunction = E_Function.WarnOnUnknownFunction;
        NodeValue.VerboseWarnings = false;
        E_Function.WarnOnUnknownFunction = false;
    }

    @AfterAll
    public static void afterClass() {
        NodeValue.VerboseWarnings = bVerboseWarnings;
        E_Function.WarnOnUnknownFunction = bWarnOnUnknownFunction;
    }
}
