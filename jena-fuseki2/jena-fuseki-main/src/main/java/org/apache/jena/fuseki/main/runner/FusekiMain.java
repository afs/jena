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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.apache.jena.fuseki.main.runner;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.main.sys.FusekiModules;

/** Programmatic ways to create a server builder using command line syntax. */
public class FusekiMain {

    /**
     * Build, but do not start, a server based on command line syntax.
     */
    public static FusekiServer build(String... args) {
        return FusekiArgs.applyArgs(FusekiServer.create(), args).build();
    }

    /**
     * Create a {@link org.apache.jena.fuseki.main.FusekiServer.Builder} which has
     * been setup according to the command line arguments.
     * The builder can be further modified.
     * @param fusekiModules  {@link FusekiModule Fuseki modules} to include
     * @param args Command line syntax
     */
    public static FusekiServer.Builder builder(FusekiModules fusekiModules, String... args) {
        FusekiArgs fusekiArgs = new FusekiArgs(fusekiModules, args);
        // Process command line args according to the arguments specified.
        fusekiArgs.process();

        FusekiServer.Builder builder = FusekiServer.create();
        if ( fusekiModules != null )
            builder.fusekiModules(fusekiModules);

        // Apply serverArgs to a builder.
        fusekiArgs.applyServerArgs(builder, fusekiArgs.serverArgs);
        return builder;
    }

    /**
     * Run a server asynchronously based on the command line arguments.
     */
    public static FusekiServer runAsync(String... args) {
        return FusekiRunner.runAsyncBasic(args);
    }

    /**
     * Run a server synchronously, based on the command line arguments.
     * This function does not return.
     */
    public static void run(String... args) {
        FusekiRunner.runBasic(args);
    }

}
