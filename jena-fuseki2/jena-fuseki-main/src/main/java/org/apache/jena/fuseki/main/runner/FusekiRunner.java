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
import org.apache.jena.fuseki.main.FusekiServer.Builder;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.mod.admin.FMod_Admin;
import org.apache.jena.fuseki.mod.prometheus.FMod_Prometheus;
import org.apache.jena.fuseki.mod.shiro.FMod_Shiro;
import org.apache.jena.fuseki.mod.ui.FMod_UI;
import org.apache.jena.sys.JenaSystem;

public class FusekiRunner {

    static { JenaSystem.init(); }

    // XXX [NewStart] Naming

    // @formatter:off
    public static void runBasic(String... args)    { runnerMinimal().run(args); }
    public static void runMain(String... args)     { runnerPlain().run(args); }
    public static void runServerUI(String... args) { runnerServerUI().run(args); }

    public static FusekiServer runAsyncBasic(String... args)    { return runnerMinimal().runAsync(args); }
    public static FusekiServer runAsyncMain(String... args)     { return runnerPlain().runAsync(args); }
    public static FusekiServer runAsyncServerUI(String... args) { return runnerServerUI().runAsync(args); }

    // XXX [NewStart] Move to FusekiServer?
    public static Runner basic(String... args)    { return runnerMinimal(); }
    public static Runner main(String... args)     { return runnerPlain(); }
    public static Runner serverUI(String... args) { return runnerServerUI(); }


    // @formatter:on

    // Modules. Fuseki Modules may have internal state in which case they should not
    // be used more than once; otherwise they are small objects.
    // We need to have fresh setups every time a server is created.

    // @formatter:off
    /** Minimal server - e.g. quiet, embedded use*/
    private static Runner runnerMinimal() { return setup(fmodsBasic()); }
    /** Basic server */
    private static Runner runnerPlain() { return setup(fmodsPlain()); }
    /** General server, no UI */
    private static Runner runnerServer() { return setup(fmodsServer()); }
    /** General server, with UI */
    private static Runner runnerServerUI() { return setup(fmodsServerUI()); }

    // @formatter:on

    // Setups.

    // Embedded, No on-disk. No logging(?) XXX [NewStart]
    public static FusekiModules fmodsBasic() {
        return FusekiModules.empty();
    }

    // RDF publishing. No on-disk. Logging.
    public static FusekiModules fmodsPlain() {
        return FusekiModules.create(FMod_Prometheus.create(), FMod_ServerLifecycleLogger.create());
    }

    // Fuseki Server. All functionality, no admin, no UI, needs a Shiro file.
    public static FusekiModules fmodsServer() {
        return FusekiModules.create(FMod_Shiro.create(),
                                    //FMod_Admin.create(),
                                    //FMod_UI.create(),
                                    FMod_Prometheus.create(),
                                    FMod_ServerLifecycleLogger.create());
    }

    // Fuseki Server. Includes admin - needs on-disk server space.
    public static FusekiModules fmodsServerUI() {
        return FusekiModules.create(FMod_Shiro.create(),
                                    FMod_Admin.create(),
                                    FMod_UI.create(),
                                    FMod_Prometheus.create(),
                                    FMod_ServerLifecycleLogger.create());
    }

    private static FusekiServer.Builder builder(FusekiModules fmods, String...args) {
        return FusekiMain.builder(fmods, args);
    }

    static class RunnerMods extends AbstractRunner {
        private final FusekiModules modules;

        RunnerMods(FusekiModules modules) {
            this.modules = modules;
        }

        @Override
        protected Builder builder(String...args) {
            return FusekiRunner.builder(modules, args);
        }
    }

    private static Runner setup(FusekiModules fmods) { return new RunnerMods(fmods); }


}
