/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 */

package org.apache.jena.tdb2.solver.skipscan_3995;

import static org.apache.jena.tdb2.solver.skipscan_3995.SkipScanTestLib.rows;
import static org.apache.jena.tdb2.solver.skipscan_3995.SkipScanTestLib.tdb2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.apache.jena.tdb2.solver.skipscan.OpExtSkipScan;
import org.apache.jena.tdb2.solver.skipscan.SkipScanRewrite;
import org.apache.jena.tdb2.store.DatasetGraphTDB;

/**
 * Finding 15 - dead code shipped with the new package, and one assertion too weak to catch a
 * regression.
 *
 * <p>Unlike the other findings this one is not a defect in behaviour, so the checks here are
 * mechanical: they state what should no longer be present. The remaining items from the finding
 * cannot be asserted from a test and are listed here for the record:</p>
 * <ul>
 *   <li>{@code OpExecutorTDB2.java:50} imports {@code OpExecutorTDB2SkipScan} but never uses it.</li>
 *   <li>{@code SystemTDB} imports {@code OpExecutorTDB2} only for a javadoc {@code @link},
 *       creating a sys to solver dependency where solver already depends on sys.</li>
 *   <li>{@code TransformDistinctPlacement.tryPush} takes a {@code BiPredicate} whose second
 *       argument both call sites ignore.</li>
 *   <li>{@code jena-tdb2/pom.xml} adds the whole {@code commons-math4-legacy} beta artifact to
 *       enumerate {@code Combinations.of(4, k)}; 16 bitmask iterations would cover it.</li>
 * </ul>
 */
public class Test15_UnusedCodeAndWeakAssertion {

    /** {@code CandidateIndex} is referenced from nowhere. */
    @Test
    public void unusedClassIsNotShipped() {
        assertFalse(Internals.classExists("CandidateIndex"),
                    "The record CandidateIndex is never used and should not be shipped");
    }

    /** Four public methods in the new package are referenced from nowhere. */
    @Test
    public void unusedMethodsAreNotShipped() {
        assertFalse(Internals.hasMethod(OpExtSkipScan.class, "setOpExecutor"),
                    "OpExtSkipScan.setOpExecutor is never called");
        assertFalse(Internals.hasMethod(SkipScanRewrite.class, "usesSkipScan"),
                    "SkipScanRewrite.usesSkipScan is never called");
        assertFalse(Internals.hasMethod(Internals.cls("TuplePatternSpec"), "getVar"),
                    "TuplePatternSpec.getVar is never called");
        assertFalse(Internals.hasMethod(Internals.cls("TuplePatternSpec"), "getIdxVar"),
                    "TuplePatternSpec.getIdxVar is never called");
    }

    /**
     * {@code Test_SPARQL_TDB.sparql10} checks that a DISTINCT over {@code rand()} is not
     * collapsed, but asserts only {@code assertNotEquals(actualTable.size(), 1)} - which also
     * passes for 0 rows, and for any wrong number of rows other than 1. The row count is exactly
     * determined: one row per triple, since each gets its own {@code rand()} value.
     *
     * <p>This test passes against the PR as it stands; it is here so that a future change to the
     * rewrite cannot quietly weaken the result.</p>
     */
    @Test
    public void distinctOverRandIsNotCollapsed() {
        int n = 100;
        StringBuilder data = new StringBuilder();
        for ( int i = 0; i < n; i++ )
            data.append(":s1 :p1 :o").append(i).append(" .\n");
        DatasetGraphTDB dsg = tdb2(data.toString());

        assertEquals(n, rows(dsg, "SELECT DISTINCT ?p ?x { ?s ?p ?o BIND(rand() AS ?x) }", true, null, null).size(),
                     "One row per triple is expected: each gets its own rand() value");
    }
}
