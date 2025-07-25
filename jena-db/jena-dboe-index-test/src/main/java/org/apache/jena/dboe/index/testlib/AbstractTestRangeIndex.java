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

package org.apache.jena.dboe.index.testlib;

import static org.apache.jena.dboe.index.testlib.IndexTestLib.add;
import static org.apache.jena.dboe.index.testlib.IndexTestLib.randTest;
import static org.apache.jena.dboe.index.testlib.IndexTestLib.testInsert;
import static org.apache.jena.dboe.index.testlib.IndexTestLib.testInsertDelete;
import static org.apache.jena.dboe.test.RecordLib.intToRecord;
import static org.apache.jena.dboe.test.RecordLib.r;
import static org.apache.jena.dboe.test.RecordLib.toIntList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.apache.jena.dboe.base.record.Record;
import org.apache.jena.dboe.index.RangeIndex;

public abstract class AbstractTestRangeIndex {

    protected RangeIndex makeRangeIndex(int order) {
        return makeRangeIndex(order, order);
    }

    protected abstract RangeIndex makeRangeIndex(int order, int minRecords);

    @Test public void __basic() {
         makeRangeIndex(4,0);
    }

    @Test
    public void tree_ins_0_0() {
        // Empty tree
        int[] keys = {};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertTrue(rIndex.isEmpty());
        assertNull(rIndex.minKey());
        assertNull(rIndex.maxKey());
    }

    @Test
    public void tree_ins_0_1() {
        int[] keys = {0, 1, 2};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertFalse(rIndex.isEmpty());
        assertEquals(0, r(rIndex.minKey()));
        assertEquals(2, r(rIndex.maxKey()));
    }

    @Test
    public void tree_ins_2_01() {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertEquals(0, r(rIndex.minKey()));
        assertEquals(9, r(rIndex.maxKey()));
    }

    @Test
    public void tree_ins_2_02() {
        int[] keys = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertEquals(0, r(rIndex.minKey()));
        assertEquals(9, r(rIndex.maxKey()));
    }

    @Test
    public void tree_ins_2_03() {
        int[] keys = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertEquals(0, r(rIndex.minKey()));
        assertEquals(9, r(rIndex.maxKey()));
    }

    @Test
    public void tree_ins_2_04() {
        int[] keys = {0, 9, 2, 7, 4, 5, 6, 3, 8, 1};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertEquals(0, r(rIndex.minKey()));
        assertEquals(9, r(rIndex.maxKey()));
    }

    @Test
    public void tree_ins_2_05() {
        int[] keys = {0, 18, 4, 14, 8, 10, 12, 6, 16, 2};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsert(rIndex, keys);
        assertFalse(rIndex.contains(r(1)));
        assertFalse(rIndex.contains(r(999)));
        assertFalse(rIndex.contains(r(-9)));
        assertFalse(rIndex.contains(r(7)));
        assertEquals(0, r(rIndex.minKey()));
        assertEquals(18, r(rIndex.maxKey()));
    }

    @Test
    public void tree_del_0_1() {
        int[] keys1 = {0, 1, 2};
        int[] keys2 = {0, 1, 2};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_0_2() {
        int[] keys1 = {0, 1, 2};
        int[] keys2 = {2, 1, 0};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_0_3() {
        int[] keys1 = {0, 1, 2};
        int[] keys2 = {1, 0, 2};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_01() {
        int[] keys1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] keys2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_02() {
        int[] keys1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] keys2 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_03() {
        int[] keys1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] keys2 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_04() {
        int[] keys1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] keys2 = {0, 9, 2, 7, 4, 5, 6, 3, 8, 1};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_05() {
        int[] keys1 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        int[] keys2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_06() {
        int[] keys1 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        int[] keys2 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_07() {
        int[] keys1 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        int[] keys2 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_08() {
        int[] keys1 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        int[] keys2 = {0, 9, 2, 7, 4, 5, 6, 3, 8, 1};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_09() {
        int[] keys1 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        int[] keys2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_10() {
        int[] keys1 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        int[] keys2 = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_11() {
        int[] keys1 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        int[] keys2 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_del_2_12() {
        int[] keys1 = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        int[] keys2 = {0, 9, 2, 7, 4, 5, 6, 3, 8, 1};
        RangeIndex rIndex = makeRangeIndex(2);
        testInsertDelete(rIndex, keys1, keys2);
    }

    @Test
    public void tree_iter_2_01() {
        int[] keys = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(4), r(6)));
        List<Integer> expected = toIntList(4, 5);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_02() {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(4), r(7)));
        List<Integer> expected = toIntList(4, 5, 6);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_03() {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(4), null));
        List<Integer> expected = toIntList(4, 5, 6, 7, 8, 9);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_04() {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(null, null));
        List<Integer> expected = toIntList(keys);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_05() {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(null, r(4)));
        List<Integer> expected = toIntList(0, 1, 2, 3);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_07() {
        int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(null, r(99)));
        List<Integer> expected = toIntList(keys);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_08() {
        int[] keys = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(0), r(99)));
        List<Integer> expected = toIntList(keys);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_2_09() {
        int[] keys = {1, 2, 3, 4, /* 5, 6, */7, 8, 9, 10, 11};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(5), r(7)));
        List<Integer> expected = toIntList();
        assertEquals(expected, x);
    }

    // Root
    @Test
    public void tree_iter_0_01() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(5);
        add(rIndex, keys);
        Iterator<Record> iter = rIndex.iterator(r(2), r(4));
        while ( iter.hasNext() )
            iter.next();

        List<Integer> x = toIntList(rIndex.iterator(r(2), r(4)));
        List<Integer> expected = toIntList(2, 3);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_0_02() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(5);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(null, null));
        List<Integer> expected = toIntList(keys);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_0_03() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(5);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(5), null));
        List<Integer> expected = toIntList(5);
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_0_04() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(5);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(0), r(0)));
        List<Integer> expected = toIntList();
        assertEquals(expected, x);
    }

    @Test
    public void tree_iter_0_05() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(5);
        add(rIndex, keys);
        List<Integer> x = toIntList(rIndex.iterator(r(1), r(0)));
        List<Integer> expected = toIntList();
        assertEquals(expected, x);
    }

    @Test
    public void tree_ret_1() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        boolean b = rIndex.insert(intToRecord(3));
        assertFalse(b);
        b = rIndex.insert(intToRecord(9));
        assertTrue(b);
    }

    @Test
    public void tree_ret_2() {
        int[] keys = {1, 2, 3, 4, 5};
        RangeIndex rIndex = makeRangeIndex(2);
        add(rIndex, keys);
        boolean b = rIndex.delete(intToRecord(9));
        assertFalse(b);
        b = rIndex.insert(intToRecord(1));
        assertFalse(b);
    }

    @Test
    public void tree_2_N() {
        for ( int i = 0; i < 10 ; i++ )
            randTest(makeRangeIndex(2), 999, 20);
    }

    @Test
    public void tree_3_N() {
        for ( int i = 0; i < 10 ; i++ )
            randTest(makeRangeIndex(3), 9999, 100);
    }

    @Test public void tree_clear_00()   { testClear(0); }

    @Test public void tree_clear_01()   { testClear(5); }

    @Test public void tree_clear_02()   { testClear(15); }

    @Test public void tree_clear_03()   { testClear(998); }

    @Test public void tree_clear_04()   { testClear(999); }

    @Test public void tree_clear_05()   { testClear(1000); }

    @Test public void tree_clear_06()   { testClear(1001); }

    @Test public void tree_clear_07()   { testClear(5500); }

    protected void testClear(int N) {
        int[] keys = new int[N]; // Slice is 1000.
        for ( int i = 0; i < keys.length ; i++ )
            keys[i] = i;
        RangeIndex rIndex = makeRangeIndex(3);
        add(rIndex, keys);
        if ( N > 0 )
            assertFalse(rIndex.isEmpty());
        rIndex.clear();
        assertTrue(rIndex.isEmpty());
    }
}
