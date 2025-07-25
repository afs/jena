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
import org.junit.jupiter.api.BeforeAll;

import org.apache.jena.sparql.engine.optimizer.reorder.ReorderLib;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation;
import org.apache.jena.sparql.graph.GraphsTests;
import org.apache.jena.tdb2.sys.SystemTDB;

public abstract class AbstractTestGraphsTDB2 extends GraphsTests
{
    private static ReorderTransformation reorder ;

    @BeforeAll public static void setupClass()
    {
        reorder = SystemTDB.getDefaultReorderTransform();
        SystemTDB.setDefaultReorderTransform(ReorderLib.identity());
    }

    @AfterAll public static void afterClass() {  SystemTDB.setDefaultReorderTransform(reorder); }
}
