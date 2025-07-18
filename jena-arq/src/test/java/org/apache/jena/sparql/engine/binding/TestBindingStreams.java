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

package org.apache.jena.sparql.engine.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.jena.atlas.legacy.BaseTest2;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.riot.tokens.TokenizerText;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.graph.NodeConst;
import org.apache.jena.sparql.sse.Item;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sparql.sse.builders.BuilderBinding;

public class TestBindingStreams
{
    @BeforeAll
    public static void beforeClass() {
        BaseTest2.setTestLogging();
    }

    @AfterAll
    public static void afterClass() {
        BaseTest2.unsetTestLogging();
    }

    static Binding b12 = build("(?a 1) (?b 2)");
    static Binding b19 = build("(?a 1) (?b 9)");
    static Binding b02 = build("(?b 2)");
    static Binding b10 = build("(?a 1)");
    static Binding b0  = build("");
    static Binding bb1 = build("(?a _:XYZ) (?b 1)");

    static Binding bb2 = build("(?a 'a\"b\"c') (?b 1)");
    static Binding bb3 = build("(?a 'aΩc') (?b 1)");

    static PrefixMap pmap = PrefixMapFactory.create();
    static {
        pmap.add(":", "http://example/");
    }

    static Binding x10 = build("(?x <http://example/abc>)");

    @Test public void bindingStream_01()        { testRead("VARS ?a ?b . 1 2 .", b12); }
    @Test public void bindingStream_02()        { testRead("VARS ?a ?b . - 2 .", b02); }
    @Test public void bindingStream_03()        { testRead("VARS ?a ?b . - 2 . 1 - . ", b02, b10); }
    @Test public void bindingStream_04()        { testRead("VARS ?a . 1 . VARS ?b . 2 . ", b10, b02); }

    @Test
    public void bindingStream_05() {
        assertThrows(RiotException.class, ()->testRead("VARS ?a ?b . 99 . "));
    }

    @Test
    public void bindingStream_06() {
        assertThrows(RiotException.class, ()->testRead("VARS ?a ?b . 99 11 22 . "));
    }

    @Test public void bindingStream_10()        { testRead("VARS ?a ?b . 1 2 . * 9 .", b12, b19); }
    @Test public void bindingStream_11()        { testRead("VARS ?a ?b ?c . 1 2 - . * 9 - .", b12, b19); }

    @Test
    public void bindingStream_12() {
        BindingBuilder builder = Binding.builder();
        builder.add(Var.alloc("a"), NodeConst.nodeTrue);
        builder.add(Var.alloc("c"), NodeConst.nodeFalse);
        Binding b = builder.build();
        testRead("VARS ?a ?b ?c . true - false . ", b);
    }

    @Test public void bindingStream_20()        { testRead("PREFIX : <http://example/> . VARS ?x .\n:abc  .\n- .", x10, b0); }

    @Test public void bindingStream_50()        { testWriteRead(b12); }
    @Test public void bindingStream_51()        { testWriteRead(b0); }
    @Test public void bindingStream_52()        { testWriteRead(pmap, b12,x10,b19); }

    @Test public void bindingStream_60()              { testWriteRead(bb1); }

    @Test
    public void bindingStream_61() {
        Node bn = NodeFactory.createBlankNode("unusual");
        Binding b = BindingFactory.binding(Var.alloc("v"), bn);
        testWriteRead(b);
    }

    @Test public void bindingStream_62()              { testWriteRead(bb2); }

    @Test public void bindingStream_63()              { testWriteRead(bb3); }

    static void testRead(String x, Binding...bindings) {
        Tokenizer t = TokenizerText.create().fromString(x).build();;
        BindingInputStream inStream = new BindingInputStream(t);

        if ( bindings.length == 0 ) {
            for (; inStream.hasNext(); )
                inStream.next();
            return;
        }

        int i;
        for ( i = 0; inStream.hasNext(); i++ ) {
            Binding b = inStream.next();
            assertTrue(equalBindings(bindings[i], b), "Bindings do not match: expected=" + bindings[i] + " got=" + b);
        }

        assertEquals(bindings.length, i, "Wrong length: expect= " + bindings.length + " got=" + i);
    }

    static void testWriteRead(Binding ... bindings) { testWriteRead(null, bindings); }

    static void testWriteRead(PrefixMap prefixMap, Binding...bindings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BindingOutputStream output = new BindingOutputStream(out, prefixMap);

        for ( Binding b : bindings )
            output.write(b);
        output.flush();

        // When the going gets tough, the tough put in trace statements:
        // System.out.println("T: \n"+out.toString());

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        BindingInputStream input = new BindingInputStream(in);

        List<Binding> results = new ArrayList<>();
        for (; input.hasNext(); ) {
            results.add(input.next());
        }
        assertEquals(bindings.length, results.size());
        for ( int i = 0; i < bindings.length; i++ ) {
            Binding b1 = bindings[i];
            Binding b2 = results.get(i);
            assertTrue(equalBindings(b1, b2), "Bindings do not match: expected=" + b1 + " got=" + b2);
        }
    }

    private static boolean equalBindings(Binding binding1, Binding binding2) {
        // Identical
        return BindingLib.equals(binding1, binding2);
    }

    private static Binding build(String string) {
        Item item = SSE.parse("(binding " + string + ")");
        return BuilderBinding.build(item);
    }
}
