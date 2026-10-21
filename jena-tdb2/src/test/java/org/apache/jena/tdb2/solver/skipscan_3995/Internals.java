/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 */

package org.apache.jena.tdb2.solver.skipscan_3995;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.jena.atlas.lib.tuple.TupleMap;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.tdb2.solver.skipscan.OpExecutorTDB2SkipScan;
import org.apache.jena.tdb2.store.nodetable.NodeTable;
import org.apache.jena.tdb2.store.tupletable.TupleIndex;

/**
 * Reflective access to the package-private internals of
 * {@code org.apache.jena.tdb2.solver.skipscan}.
 *
 * <p>These tests live in a separate package, so {@code TuplePatternSpec}, {@code IndexMatch},
 * {@code SkipScanCandidate} and {@code ValueFilter} - all package-private - cannot be named
 * directly. Everything that can be reached through public API is tested through it; this class
 * exists only for the handful of findings that are about those internal types.</p>
 */
class Internals {

    static final String PKG = "org.apache.jena.tdb2.solver.skipscan.";

    static Class<?> cls(String simpleName) {
        try {
            return Class.forName(PKG + simpleName);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Class not found: " + PKG + simpleName, e);
        }
    }

    static boolean classExists(String simpleName) {
        try {
            Class.forName(PKG + simpleName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** Any declared method with this name, regardless of signature. */
    static boolean hasMethod(Class<?> owner, String name) {
        for ( Method m : owner.getDeclaredMethods() ) {
            if ( m.getName().equals(name) )
                return true;
        }
        return false;
    }

    /** {@code TuplePatternSpec.create(Node[], List<Var>)} */
    static Object tuplePatternSpec(Node[] tuple, List<Var> projection) {
        return invoke(cls("TuplePatternSpec"), "create", new Class<?>[] {Node[].class, List.class},
                      null, tuple, projection);
    }

    /** {@code OpExecutorTDB2SkipScan.computeIndexMatch(TupleMap, TuplePatternSpec)}; null if the index is unusable. */
    static Object computeIndexMatch(TupleMap tupleMap, Object tuplePatternSpec) {
        return invoke(OpExecutorTDB2SkipScan.class, "computeIndexMatch",
                      new Class<?>[] {TupleMap.class, cls("TuplePatternSpec")},
                      null, tupleMap, tuplePatternSpec);
    }

    /** {@code OpExecutorTDB2SkipScan.planCandidate(TuplePatternSpec, TupleIndex)}; null if the index is rejected. */
    static Object planCandidate(Object tuplePatternSpec, TupleIndex tupleIndex) {
        return invoke(OpExecutorTDB2SkipScan.class, "planCandidate",
                      new Class<?>[] {cls("TuplePatternSpec"), TupleIndex.class},
                      null, tuplePatternSpec, tupleIndex);
    }

    /** {@code OpExecutorTDB2SkipScan.remapValueConditions(ValueFilter[], TupleMap, NodeTable)}. */
    static Object[] remapValueConditions(Object[] conditions, TupleMap tupleMap, NodeTable nodeTable) {
        Class<?> arrayType = Array.newInstance(cls("ValueFilter"), 0).getClass();
        Object conditionArray = Array.newInstance(cls("ValueFilter"), conditions.length);
        for ( int i = 0; i < conditions.length; i++ )
            Array.set(conditionArray, i, conditions[i]);
        return (Object[])invoke(OpExecutorTDB2SkipScan.class, "remapValueConditions",
                                new Class<?>[] {arrayType, TupleMap.class, NodeTable.class},
                                null, conditionArray, tupleMap, nodeTable);
    }

    /** A {@code ValueFilter(idx, value)}. */
    static Object valueFilter(int idx, Object value) {
        try {
            Constructor<?> ctor = cls("ValueFilter").getDeclaredConstructor(int.class, Object.class);
            ctor.setAccessible(true);
            return ctor.newInstance(idx, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot create a ValueFilter", e);
        }
    }

    /** Read a no-argument accessor, e.g. {@code keyLen()} of an IndexMatch. */
    static Object get(Object target, String accessor) {
        return invoke(target.getClass(), accessor, new Class<?>[0], target);
    }

    static int getInt(Object target, String accessor) {
        return ((Number)get(target, accessor)).intValue();
    }

    static boolean getBoolean(Object target, String accessor) {
        return (Boolean)get(target, accessor);
    }

    /** Read a private static field, e.g. {@code OpExtSkipScan.symOpExecutor}. */
    static Object staticField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot read " + owner.getName() + "." + name, e);
        }
    }

    private static Object invoke(Class<?> owner, String name, Class<?>[] paramTypes, Object target, Object... args) {
        try {
            Method method = owner.getDeclaredMethod(name, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // Surface the real failure (e.g. an ArrayIndexOutOfBoundsException from the code under test).
            Throwable cause = e.getCause();
            if ( cause instanceof RuntimeException rte )
                throw rte;
            if ( cause instanceof Error err )
                throw err;
            throw new AssertionError(cause);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot invoke " + owner.getName() + "." + name, e);
        }
    }
}
