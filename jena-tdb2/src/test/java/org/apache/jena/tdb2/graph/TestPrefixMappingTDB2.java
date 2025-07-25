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

package org.apache.jena.tdb2.graph;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.apache.jena.query.TxnType;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.graph.AbstractTestPrefixMappingView;
import org.apache.jena.tdb2.DatabaseMgr;
import org.apache.jena.tdb2.sys.TDBInternal;

public class TestPrefixMappingTDB2 extends AbstractTestPrefixMappingView
{
    static DatasetGraph dsg = null;
    static PrefixMapping last = null;

    @BeforeAll public static void beforeClass() { }
    @AfterAll  public static void afterClass()  { TDBInternal.reset(); }

    @BeforeEach
    public void before() {
        dsg = createTestingMem();
        dsg.begin(TxnType.READ_PROMOTE);
    }

    @AfterEach
    public void after() {
        dsg.commit();
        dsg.end();
        last = null;
        dsg = null;
    }

    @Override
    protected PrefixMapping create() {
        last = dsg.getDefaultGraph().getPrefixMapping();
        return view();
    }

    @Override
    protected PrefixMapping view() {
        return last;
    }

    static DatasetGraph createTestingMem() {
        return DatabaseMgr.createDatasetGraph();
    }
}
