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

package org.apache.jena.sparql.expr;

import static org.junit.Assert.fail;

import org.apache.jena.sparql.engine.binding.BindingFactory ;
import org.junit.Test ;

public class TestLike
{
    @Test public void testLike01() { likeTest( "ABC",  "ABC",  null,   true) ; }
    @Test public void testLike02() { likeTest( "ABC",  "abc",  null,   false) ; }
    @Test public void testLike03() { likeTest( "ABC",  "abc",  "",     false) ; }
    @Test public void testLike04() { likeTest( "ABC",  "abc",  "i",    true) ; }

    @Test public void testLike05() { likeTest( "abc",  "b",    "",     false) ; }
    @Test public void testLike06() { likeTest( "abc",  "B",    "i",    false) ; }

    @Test public void testLike07() { likeTest( "ABC",  "ABC",  "q",   true) ; }
    @Test public void testLike08() { likeTest( "ABC",  "abc",  "qi",  true) ; }
    @Test public void testLike09() { likeTest( "A*C",  "ABC",  "",     false) ; }
    @Test public void testLike10() { likeTest( "A*C",  "A*c",  "iq",     false) ; }

    @Test public void testLike20() { likeTest( "ABC",  "A*",   null,   true) ; }
    @Test public void testLike21() { likeTest( "ABC",  "*BC",  null,   true) ; }
    @Test public void testLike22() { likeTest( "ABC",  "A?C",  null,   true) ; }
    @Test public void testLike23() { likeTest( "ABC",  "A?",   null,   false) ; }

    @Test public void testLike24() { likeTest( "ABC",  "*",    null,   true) ; }
    @Test public void testLike25() { likeTest( "ABC",  "*C",   null,   true) ; }

    // Tests of the fixup for a FilenameUtils.wildcardMatch corner case.
    @Test public void testLike30() { likeTest( "ABC",  "A*?C", null, true) ; }
    @Test public void testLike31() { likeTest( "ABC",  "A*?",  null,  true) ; }
    @Test public void testLike32() { likeTest( "ABC",  "*?",   null,   true) ; }

    @Test public void testLike33() { likeTest( "ABC",  "A?*",    null,  true) ; }
    @Test public void testLike34() { likeTest( "ABCD",  "A?**?", null,  true) ; }
    @Test public void testLike35() { likeTest( "ABC",  "?*",     null,  true) ; }

    @Test public void testLike40() { likeTest( "ABC",  "[A-B]*", null,  true) ; }
    // Glob uses ! for negative character sets
    @Test public void testLike41() { likeTest( "ABC",  "[^Z]*[^Z]", null,  true) ; }
    @Test public void testLike42() { likeTest( "ABC",  "[!Z]*[!Z]", null,  true) ; }

    public void likeTest(String value, String pattern, String flags, boolean expected) {
        Expr s = NodeValue.makeString(value) ;
        E_Like r = new E_Like(s, pattern, flags) ;
        NodeValue nv = r.eval(BindingFactory.empty(), null) ;
        boolean b = nv.getBoolean() ;
        if ( b != expected )
            fail(fmtTest(value, pattern, flags)+" ==> "+b+" expected "+expected) ;
    }

    private String fmtTest(String value, String pattern, String flags) {
        String tmp = "like(\""+value+"\", \""+pattern+"\"" ;
        if ( flags != null )
            tmp = tmp + ", \""+flags+"\"" ;
        tmp = tmp + ")" ;
        return tmp ;
    }

    // No such flag
    @Test(expected=ExprEvalException.class)
    public void testLikeErr1() { likeTest("ABC", "abc", "g", false) ; }
}
