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

package org.apache.jena.tdb1.graph;

import org.apache.jena.query.Dataset ;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderLib ;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderTransformation ;
import org.apache.jena.sparql.graph.GraphsTests ;
import org.apache.jena.tdb1.TDB1Factory;
import org.apache.jena.tdb1.sys.SystemTDB;
import org.junit.AfterClass ;
import org.junit.BeforeClass ;

@SuppressWarnings("removal")
public abstract class AbstractTestGraphsTDB1 extends GraphsTests
{
    private static ReorderTransformation reorder  ;

    @BeforeClass public static void setupClass()
    {
        reorder = SystemTDB.defaultReorderTransform ;
        SystemTDB.defaultReorderTransform = ReorderLib.identity() ;
    }

    @AfterClass public static void afterClass() {  SystemTDB.defaultReorderTransform = reorder ; }

    @Override
    protected Dataset createDataset()
    {
        return TDB1Factory.createDataset() ;
    }
}
