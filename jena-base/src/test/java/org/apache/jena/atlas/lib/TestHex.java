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

package org.apache.jena.atlas.lib;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test ;

public class TestHex
{
    @Test
    public void hex_01() {
        byte[] b = new byte[16];
        test(0L, 0, b, 16);
    }

    @Test
    public void hex_02() {
        byte[] b = new byte[16];
        test(1L, 0, b, 16);
    }

    @Test
    public void hex_03() {
        byte[] b = new byte[16];
        test(Long.MAX_VALUE, 0, b, 16);
    }

    @Test
    public void hex_04() {
        byte[] b = new byte[16];
        test(Long.MIN_VALUE, 0, b, 16);
    }

    @Test
    public void hex_05() {
        byte[] b = new byte[16];
        // -1L
        test(0xFFFFFFFFFFFFFFFFL, 0, b, 16);
    }

    @Test
    public void hex_06() {
        byte[] b = new byte[16];
        test(-1L, 0, b, 16);
    }

    private static void test(long value, int idx, byte[] b, int width) {
        Hex.formatUnsignedLongHex(b, idx, value, width);
        for ( int i = 0 ; i < width ; i++ ) {
            int v = b[i];
            if ( v >= '0' && v <= '9' )
                continue;
            if ( v >= 'a' && v <= 'f' )
                continue;
            if ( v >= 'A' && v <= 'F' )
                continue;
            fail(String.format("Not a hex digit: %02X", b[i]));
        }

        long v = Hex.getLong(b, idx);
        assertEquals(value, v);
    }

    private static void testStr2Val(String str, int expected) {
        testStr2Val(str, 0, str.length(), expected) ;
    }

    private static void testStr2Val(String str, int start, int length, int expected) {
        /* int i = */ Hex.hexStringToInt(str, start, length) ;
    }

    @Test public void hexValue_01()     { testStr2Val("A", 10); }
    @Test public void hexValue_02()     { testStr2Val("0A", 10); }
    @Test public void hexValue_03()     { testStr2Val("AA", 16*10+10); }
    @Test public void hexValue_04()     { testStr2Val("FF", 255); }
    @Test public void hexValue_05()     { testStr2Val("00FF00", 2, 2, 255); }

}
