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

package org.apache.jena.atlas.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public abstract class AbstractTestPeekReader {
    static int INIT_LINE = PeekReader.INIT_LINE;
    static int INIT_COL = PeekReader.INIT_COL;

    abstract PeekReader make(String contents, int size);

    @Test
    public void read0() {
        assertEquals(1, INIT_LINE, ()->"Init line");
        assertEquals(1, INIT_COL, ()->"Init col");
    }

    @Test
    public void read1() {
        PeekReader r = make("");
        checkLineCol(r, INIT_LINE, INIT_COL);

        int x = r.peekChar();
        assertEquals(-1, x);
        x = r.readChar();
        assertEquals(-1, x);
        x = r.readChar();
        assertEquals(-1, x);
    }

    @Test
    public void read2() {
        // Assumes we start at (1,1)
        PeekReader r = make("a");
        checkLineCol(r, INIT_LINE, INIT_COL);

        int x = r.peekChar();
        assertEquals('a', x);
        checkLineCol(r, INIT_LINE, INIT_COL);

        x = r.readChar();
        checkLineCol(r, INIT_LINE, INIT_COL + 1);
        assertEquals('a', x);

        x = r.peekChar();
        assertEquals(-1, x);

        x = r.readChar();
        assertEquals(-1, x);
    }

    @Test
    public void read3() {
        String c = "abcde";
        PeekReader r = make(c);

        for ( int i = 0 ; i < c.length() ; i++ ) {
            checkLineCol(r, INIT_LINE, i + INIT_COL);
            assertEquals(i, r.getPosition());
            assertEquals(c.charAt(i), r.readChar());
        }
        assertTrue(r.eof());
    }

    @Test
    public void read4() {
        position("abcde");
    }

    @Test
    public void read5() {
        position("abc\nde");
    }

    @Test
    public void read6() {
        position("abc\nde\n");
    }

    @Test
    public void read7() {
        position("");
    }

    @Test
    public void read8() {
        position("x");
    }

    @Test
    public void read9() {
        PeekReader r = make("a\nb\n");
        checkLineCol(r, INIT_LINE, INIT_COL);
        int x = r.peekChar();
        assertEquals('a', x);
        checkLineCol(r, INIT_LINE, INIT_COL);

        x = r.readChar();
        assertEquals('a', x);
        checkLineCol(r, INIT_LINE, INIT_COL + 1);

        x = r.readChar();
        assertEquals('\n', x);
        checkLineCol(r, INIT_LINE + 1, INIT_COL);
    }

    @Test
    public void unread1() {
        PeekReader r = make("abc");
        assertEquals('a', r.peekChar());
        r.pushbackChar('Z');
        assertEquals('Z', r.peekChar());
        contains(r, "Zabc");
    }

    @Test
    public void unread2() {
        PeekReader r = make("abc");
        checkLineCol(r, INIT_LINE, INIT_COL);
        r.readChar();
        // Pushback does not move line/col backwards.
        checkLineCol(r, INIT_LINE, INIT_COL + 1);
        assertEquals('b', r.peekChar());
        checkLineCol(r, INIT_LINE, INIT_COL + 1);
        r.pushbackChar('a');
        checkLineCol(r, INIT_LINE, INIT_COL + 1);
        contains(r, "abc");
    }

    @Test
    public void unread3() {
        PeekReader r = make("");
        r.readChar();
        assertEquals(-1, r.peekChar());
        r.pushbackChar('a');
        contains(r, "a");
    }

    @Test
    public void unread4() {
        PeekReader r = make("");
        r.readChar();
        assertEquals(-1, r.peekChar());
        r.pushbackChar('0');
        r.pushbackChar('1');
        r.pushbackChar('2');
        r.pushbackChar('3');
        contains(r, "3210");   // Backwards!
    }

    @Test
    public void unread5() {
        PeekReader r = make("");
        long lineNum = r.getLineNum();
        long colNum = r.getColNum();

        checkLineCol(r, lineNum, colNum);

        r.pushbackChar('0');
        checkLineCol(r, lineNum, colNum);  // Unmoved.
        r.pushbackChar('1');
        checkLineCol(r, lineNum, colNum);
        assertEquals('1', r.readChar());

        checkLineCol(r, lineNum, colNum);  // Unmoved.
        r.pushbackChar('2');
        r.pushbackChar('3');
        checkLineCol(r, lineNum, colNum);  // Unmoved.
        assertEquals('3', r.peekChar());
        contains(r, "320");
    }

    private static void checkLineCol(PeekReader r, long lineNum, long colNum) {
        assertEquals(lineNum, r.getLineNum(), ()->"Line");
        assertEquals(colNum, r.getColNum(), ()->"Column");
    }

    private void position(String contents) {
        PeekReader r = make(contents);

        int line = INIT_LINE;
        int col = INIT_COL;
        checkLineCol(r, line, col);
        assertEquals(0, r.getPosition());

        for ( int i = 0 ; i < contents.length() ; i++ ) {
            int x = r.readChar();
            if ( x != -1 ) {
                if ( x == '\n' ) {
                    line++;
                    col = INIT_COL;
                } else
                    col++;
            }
            assertEquals(contents.charAt(i), x);
            assertEquals(i + 1, r.getPosition());
            checkLineCol(r, line, col);
        }
        assertTrue(r.eof());
    }

    private static void contains(PeekReader r, String contents) {
        for ( int i = 0 ; i < contents.length() ; i++ ) {
            int x = r.readChar();
            assertEquals(contents.charAt(i), x,
                         "\"" + contents + "\" -- Index " + i + " Expected:'" + contents.charAt(i) + "' Got: '" + (char)x + "'");
        }
        assertTrue(r.eof());
    }

    private PeekReader make(String contents) {
        return make(contents, 2);
    }
}
