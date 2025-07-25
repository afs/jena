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

package org.apache.jena.tdb2.setup;

import static org.junit.Assert.*;

import org.junit.jupiter.api.Test;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.dboe.base.block.FileMode;
import org.apache.jena.tdb2.TDBException;
import org.apache.jena.tdb2.params.StoreParams;
import org.apache.jena.tdb2.params.StoreParamsBuilder;
import org.apache.jena.tdb2.params.StoreParamsCodec;

public class TestStoreParams {

    private static int counter = 0;
    private static String label() { return TestStoreParams.class.getSimpleName()+"-"+(++counter); }

    @Test public void store_params_01() {
        assertEqualsStoreParams(StoreParams.getDftStoreParams(), StoreParams.getDftStoreParams());
    }

    @Test public void store_params_02() {
        StoreParams input = StoreParams.getDftStoreParams();
        StoreParams sp = StoreParams.builder(label(), input).build();
        assertEqualsStoreParams(StoreParams.getDftStoreParams(), sp);
    }

    @Test public void store_params_03() {
        StoreParams sp = StoreParams.builder(label()).build();
        assertEqualsStoreParams(StoreParams.getDftStoreParams(), sp);
    }

    @Test public void store_params_04() {
        StoreParams params = StoreParams.builder(label()).build();
        StoreParams params2 = roundTrip(params);
        assertEqualsStoreParams(params,params2);
    }

    @Test public void store_params_05() {
        Double initialCapacityFactor = Double.valueOf(0.125);
        StoreParams params = StoreParams.builder(label()).nodeCacheInitialCapacityFactor(initialCapacityFactor).build();
        StoreParams params2 = roundTrip(params);
        assertEqualsStoreParams(params, params2);
        assertEquals(params.getNodeCacheInitialCapacityFactor(), initialCapacityFactor);
        assertEquals(params2.getNodeCacheInitialCapacityFactor(), initialCapacityFactor);
    }

    @Test public void store_params_06() {
        Double initialCapacityFactor = Double.valueOf(0.33333);
        String xs = "{ \"tdb.node_cache_initial_capacity_factor\": " + initialCapacityFactor + " }";
        JsonObject x = JSON.parse(xs);
        StoreParams paramsExpected = StoreParams.builder(label()).nodeCacheInitialCapacityFactor(initialCapacityFactor).build();
        StoreParams paramsActual = StoreParamsCodec.decode(x);
        assertEqualsStoreParams(paramsExpected,paramsActual);
    }

    // ----

    @Test public void store_params_10() {
        StoreParams params = StoreParams.builder(label()).fileMode(FileMode.direct).blockSize(1024).build();
        StoreParams params2 = roundTrip(params);
        assertEqualsStoreParams(params,params2);
        assertEquals(params.getFileMode(), params2.getFileMode());
        assertEquals(params.getBlockSize(), params2.getBlockSize());
    }

    @Test public void store_params_11() {
        String xs = "{ \"tdb.block_size\": 2048 }";
        JsonObject x = JSON.parse(xs);
        StoreParams paramsExpected = StoreParams.builder(label()).blockSize(2048).build();
        StoreParams paramsActual = StoreParamsCodec.decode(x);
        assertEqualsStoreParams(paramsExpected,paramsActual);
    }

    @Test public void store_params_12() {
        String xs = "{ \"tdb.file_mode\": \"direct\" , \"tdb.block_size\": 2048 }";
        JsonObject x = JSON.parse(xs);
        StoreParams paramsExpected = StoreParams.builder(label()).blockSize(2048).fileMode(FileMode.direct).build();
        StoreParams paramsActual = StoreParamsCodec.decode(x);
        assertEqualsStoreParams(paramsExpected,paramsActual);
    }

    @Test public void store_params_13() {
        String xs = "{ \"tdb.triple_indexes\" : [ \"POS\" , \"PSO\"] } ";
        JsonObject x = JSON.parse(xs);
        StoreParams params = StoreParamsCodec.decode(x);
        String[] expected =  { "POS" , "PSO" };
        assertArrayEquals(expected, params.getTripleIndexes());
    }

    @Test
    public void store_params_14() {
        String xs = "{ \"tdb.triples_indexes\" : [ \"POS\" , \"PSO\"] } "; // Misspelt.
        JsonObject x = JSON.parse(xs);
        assertThrows(TDBException.class, ()-> StoreParamsCodec.decode(x) );
    }

    // Check that setting gets recorded and propagated.

    @Test public void store_params_20() {
        StoreParams params = StoreParams.builder(label()).blockReadCacheSize(0).build();
        assertTrue(params.isSetBlockReadCacheSize());
        assertFalse(params.isSetBlockWriteCacheSize());
    }

    @Test public void store_params_21() {
        StoreParams params1 = StoreParams.builder(label()).blockReadCacheSize(0).build();
        assertTrue(params1.isSetBlockReadCacheSize());
        assertFalse(params1.isSetBlockWriteCacheSize());
        StoreParams params2 = StoreParams.builder(label(), params1).blockWriteCacheSize(0).build();
        assertTrue(params2.isSetBlockReadCacheSize());
        assertTrue(params2.isSetBlockWriteCacheSize());
        assertFalse(params2.isSetNodeMissCacheSize());
    }

    // Modify
    @Test public void store_params_22() {
        StoreParams params1 = StoreParams.builder(label())
            .blockReadCacheSize(0)
            .blockWriteCacheSize(1)
            .build();
        StoreParams params2 = StoreParams.builder(label())
            .blockReadCacheSize(5)
            .build();
        StoreParams params3 = StoreParamsBuilder.modify(params1, params2);
        assertFalse(params2.isSetBlockWriteCacheSize());
        assertTrue(params3.isSetBlockReadCacheSize());
        assertTrue(params3.isSetBlockWriteCacheSize());
        assertEquals(5, params3.getBlockReadCacheSize().intValue());   // From params2
        assertEquals(1, params3.getBlockWriteCacheSize().intValue());  // From params1, not params2(unset)

    }


    // --------

    private static StoreParams roundTrip(StoreParams params) {
        JsonObject obj = StoreParamsCodec.encodeToJson(params);
        StoreParams params2 = StoreParamsCodec.decode(obj);
        return params2;
    }

    private static void assertEqualsStoreParams(StoreParams params1, StoreParams params2) {
        assertTrue(StoreParams.sameValues(params1, params2));
    }
}
