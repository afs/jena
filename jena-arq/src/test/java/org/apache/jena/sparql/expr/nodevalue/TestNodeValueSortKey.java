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

package org.apache.jena.sparql.expr.nodevalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.NodeValue;

/**
 * Tests for {@link NodeValueSortKey}.
 */
public class TestNodeValueSortKey {

    @Test
    public void testSortKeyNodeValue() {
        NodeValue noveValue = NodeValue.makeString("Casa");
        NodeValue nv = NodeFunctions.sortKey(noveValue, "es");
        assertTrue(nv instanceof NodeValueSortKey);
        assertEquals("es", nv.getSortKey().getCollation());
    }

    @Test
    public void testCreateNodeValueSortKey() {
        NodeValueSortKey nv = new NodeValueSortKey("", null);
        assertTrue(nv.isSortKey());
    }

    @Test
    public void testCreateNodeValueSortKeyWithNode() {
        Node n = Node.ANY;
        NodeValueSortKey nv = new NodeValueSortKey("", null, n);
        assertEquals(n, nv.getNode());
    }

    @Test
    public void testGetCollation() {
        NodeValueSortKey nv = new NodeValueSortKey("", null);
        assertNull(nv.getCollation());
        nv = new NodeValueSortKey("", "fi");
        assertEquals("fi", nv.getCollation());
    }

    @Test
    public void testGetString() {
        NodeValueSortKey nv = new NodeValueSortKey("Casa", "pt-BR");
        assertEquals("Casa", nv.asString());
        assertEquals("Casa", nv.getString());
    }

    @Test
    public void testMakeNode() {
        NodeValueSortKey nv = new NodeValueSortKey("Casa", "pt-BR");
        Node n = nv.makeNode();
        assertTrue(n.isLiteral());
        assertEquals("\"Casa\"", n.getLiteral().toString());
    }

    @Test
    public void testToString() {
        NodeValueSortKey nv = new NodeValueSortKey("Tutte", "it");
        assertEquals("'Tutte'", nv.toString());
    }

    @Test
    public void testCompareTo() {
        final String languageTag = "pt";
        NodeValueSortKey nv = new NodeValueSortKey("Bonito", languageTag);
        assertEquals(1, nv.compareTo(new NodeValueSortKey("Bonita", languageTag)));
        assertEquals(-1, nv.compareTo(new NodeValueSortKey("Bonitos", languageTag)));
        // comparing string, regardless of the collations
        assertEquals(1, nv.compareTo(new NodeValueSortKey("Bonita", "es")));
        assertEquals(0, nv.compareTo(new NodeValueSortKey("Bonito", "es")));
    }
}
