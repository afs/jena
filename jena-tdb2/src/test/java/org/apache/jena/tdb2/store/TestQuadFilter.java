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

package org.apache.jena.tdb2.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.function.Predicate;

import org.apache.jena.atlas.lib.tuple.Tuple;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.tdb2.store.nodetable.NodeTable;
import org.apache.jena.tdb2.sys.SystemTDB;
import org.apache.jena.tdb2.sys.TDBInternal;
import org.junit.jupiter.api.Test;

public class TestQuadFilter
{
    private static String graphToHide = "http://example/g2";
    private static Dataset ds1 = setup1();
    private static Dataset ds2 = setup2();

    private static Dataset createDataset() { return  TDB2Factory.createDataset(); }

    /** Example setup - in-memory dataset with two graphs, one triple in each */
    private static Dataset setup1() {
        Dataset ds = createDataset();
        DatasetGraph dsg = ds.asDatasetGraph();
        Txn.executeWrite(dsg, () -> {
            Quad q1 = SSE.parseQuad("(<http://example/g1> <http://example/s> <http://example/p> <http://example/o1>)");
            Quad q2 = SSE.parseQuad("(<http://example/g2> <http://example/s> <http://example/p> <http://example/o2>)");
            dsg.add(q1);
            dsg.add(q2);
        });
        return ds;
    }

    private static Dataset setup2() {
        Dataset ds = createDataset();
        DatasetGraph dsg = ds.asDatasetGraph();
        Txn.executeWrite(dsg, () -> {
            Quad q1 = SSE.parseQuad("(:g1 :r1 rdf:reifies <<( :s :p :o1 )>>)");
            Quad q2 = SSE.parseQuad("(:g2 :r2 rdf:reifies <<( :s :p :o2 )>>)");
            dsg.add(q1);
            dsg.add(q2);
        });
        return ds;
    }

    /** Create a filter to exclude the graph http://example/g2 */
    private static Predicate<Tuple<NodeId>> createFilter(Dataset dataset) {
        return Txn.calculateRead(dataset, ()->{
            DatasetGraphTDB dsg = TDBInternal.getDatasetGraphTDB(dataset);
            final NodeTable nodeTable = dsg.getQuadTable().getNodeTupleTable().getNodeTable();
            final NodeId target = nodeTable.getNodeIdForNode(NodeFactory.createURI(graphToHide));
            // Check g slot. Exclude graphToHide
            return item -> !(item.len() == 4 && item.get(0).equals(target));
        });
    }

    @Test public void quad_filter_1()   { test(ds1, "SELECT * { GRAPH ?g { ?s ?p ?o } }", 1, 2); }
    @Test public void quad_filter_2()   { test(ds1, "SELECT * { ?s ?p ?o }", 1, 2); }
    @Test public void quad_filter_3()   { test(ds1, "SELECT * { GRAPH ?g { } }", 1, 2); }

    @Test public void quad_filter_4()   { test(ds2, "SELECT * { GRAPH ?g { ?x ?q <<( ?s ?p ?o )>> } }", 1, 2); }
    @Test public void quad_filter_5()   { test(ds2, "SELECT * { ?x ?q <<( ?s ?p ?o )>> }", 1, 2); }

    @Test public void quad_filter_6()   { test(ds2, "SELECT * { << ?s ?p ?o >> }", 1, 2); }
    @Test public void quad_filter_7()   { test(ds2, "SELECT * { <http://example/r1> ?P ?O }", 1, 1); }

    // Reified triple.
    @Test public void quad_filter_8()   { test(ds2, "SELECT * { << ?s ?p <http://example/o2> >> }", 0, 1); }


    private static void test(Dataset dataset, String qs, int withFilter, int withoutFilter) {
        Predicate<Tuple<NodeId>> filter = createFilter(dataset);
        Query query = QueryFactory.create(qs);

        Txn.executeRead(dataset, ()->{
            try(QueryExecution qExec = QueryExecutionFactory.create(query, dataset)) {
                // Install filter for this query only.
                qExec.getContext().set(SystemTDB.symTupleFilter, filter);
                qExec.getContext().setTrue(TDB2.symUnionDefaultGraph);
                long x1 = ResultSetFormatter.consume(qExec.execSelect());
                assertEquals(withFilter, x1);
            }
            // No filter.
            try(QueryExecution qExec = QueryExecutionFactory.create(query, dataset)) {
                qExec.getContext().setTrue(TDB2.symUnionDefaultGraph);
                Dataset dsx = qExec.getDataset();
                assertNotNull(dsx);
                long x2 = ResultSetFormatter.consume(qExec.execSelect());
                assertEquals(withoutFilter, x2);
            }
        });
    }
}
