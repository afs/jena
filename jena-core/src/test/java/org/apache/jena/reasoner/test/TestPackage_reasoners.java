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

package org.apache.jena.reasoner.test;

import junit.framework.*;
import org.apache.jena.reasoner.rulesys.test.TestRuleReasoners;

/**
 * Aggregate tester that runs all the test associated with the reasoner package.
 */

public class TestPackage_reasoners extends TestSuite {

    static public TestSuite suite() {
        return new TestPackage_reasoners();
    }

    /** Creates new TestPackage */
    private TestPackage_reasoners() {
        super("reasoners");
        addTest( "TestTransitiveGraphCache", TestTransitiveGraphCache.suite() );
        addTest( "TestReasoners", TestReasoners.suite() );
        addTest( "TestRDFSReasoners", TestRDFSReasoners.suite() );
        addTest( "TestRuleReasoners",  TestRuleReasoners.suite() );
        addTest( "TestReasonerPrefixMapping", TestInfPrefixMapping.suite() );
        addTest( "TestInfGraph", TestInfGraph.suite() );
        addTest( "TestInfModel", TestInfModel.suite() );
        addTest( "TestSafeModel", TestSafeModel.suite() );
    }

    // helper method
    private void addTest(String name, TestSuite tc) {
        tc.setName(name);
        addTest(tc);
    }

}
