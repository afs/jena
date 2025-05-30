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

import org.apache.jena.arq.junit.manifest.Manifests;
import org.apache.jena.arq.junit.manifest.Prefix;
import org.apache.jena.arq.junit.runners.Label;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.expr.E_Function;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.junit.RunnerSPARQL_DatasetMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(RunnerSPARQL_DatasetMap.class)
@Label("SPARQL [dataset Map]")
@Prefix("DS_Map-")
@Manifests
({
    "testing/ARQ/manifest-arq.ttl"
    // [rdf-star CG]
    //, "testing/rdf-star-cg/sparql/eval/manifest.ttl"
})

public class Scripts_DatasetMap
{
    @BeforeClass static public void beforeClass() {
        ARQ.setNormalMode();
        NodeValue.VerboseWarnings = false;
        E_Function.WarnOnUnknownFunction = false;
    }

    @AfterClass static public void afterClass() {
        NodeValue.VerboseWarnings = true;
        E_Function.WarnOnUnknownFunction = true;
    }
}
