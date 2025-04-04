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

package org.apache.jena.rfc3986;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Relativize tests - overall structure, not paths.
 * @See {@link TestRelativePaths}
 * @See {@link TestRelativeVariants}
 */
public class TestRelative {
    @Test
    // Same scheme, same authority
    public void relative_01() { testRelative("http://host/dir/", "http://host/dir/file", "file"); }

    @Test
    // Same scheme, different authority
    public void relative_02() { testRelative("http://host/dir/", "http://elsewhere/dir/file", null); }

    @Test
    // Different scheme
    public void relative_03() { testRelative("https://host/dir/", "http://host/dir/file", null); }

    @Test
    // Same scheme, same host, same authority
    public void relative_04() { testRelative("http://host:1234/dir/", "http://host:1234/dir/file", "file"); }

    @Test
    // Same scheme, same host, different authority because of port
    public void relative_05() { testRelative("https://host:1234/dir/", "http://host:5678/dir/file", null); }

    @Test
    // Query string after /
    public void relative_06() { testRelative("http://ex/path/?query", "http://ex/path/file", null); }

    @Test
    // Fragment after /
    public void relative_07() { testRelative("http://ex/path/#frag", "http://ex/path/file", "file"); }

    @Test
    // Query string, not after /
    public void relative_08() { testRelative("http://ex/path/", "http://ex/path/file?q=x", "file?q=x"); }

    @Test
    // Fragment, not after /
    public void relative_09() { testRelative("http://ex/path/", "http://ex/path/file#frag", "file#frag"); }

    @Test
    // Fragment in base
    public void relative_10() { testRelative("http://example/ns#", "http://example/x", "x") ; }

    @Test
    // Fragment in base and target
    public void relative_11() { testRelative("http://example/ns#", "http://example/ns#x", "#x") ; }

    private void testRelative(String baseStr, String argStr, String expectedRelStr) {
        IRI3986 base = IRI3986.create(baseStr);
        IRI3986 arg = IRI3986.create(argStr);
        IRI3986 rel = expectedRelStr == null ? null : IRI3986.create(expectedRelStr);

        IRI3986 result = base.relativize(arg);
        assertEquals(rel, result);
        if ( rel != null ) {
            IRI3986 path2 = base.resolve(rel);
            assertEquals(arg, path2);
            assertEquals(arg.toString(), path2.toString());
        }
    }
}

