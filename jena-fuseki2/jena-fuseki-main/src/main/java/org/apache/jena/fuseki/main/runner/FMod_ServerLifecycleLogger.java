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

import java.util.Set;

import org.apache.jena.cmd.CmdGeneral;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Startup logging */
public class FMod_ServerLifecycleLogger implements FusekiModule {

    private static Logger logger = LoggerFactory.getLogger("SERVER"); //Fuseki.fusekiLog; Fuseki.serverLog;
    private static Logger serverLogger = Fuseki.serverLog;

    public static FusekiModule create() { return new FMod_ServerLifecycleLogger(); }

    public FMod_ServerLifecycleLogger() {}

    @Override public String name() { return "Fuseki Server lifecycle logging"; }

    // arguments lifecycle
    @Override
    public void serverArgsModify(CmdGeneral fusekiCmd, ServerArgs serverArgs) {
    }

    @Override
    public void serverArgsPrepare(CmdGeneral fusekiCmd, ServerArgs serverArgs) {
        //serverArgs.isQuiet();
        //serverArgs.isVerbose();
        fusekiCmd.isQuiet();
        fusekiCmd.isVerbose();
    }

    @Override
    public void serverArgsBuilder(FusekiServer.Builder serverBuilder, Model configModel) {}


    // Build lifecycle.

    @Override
    public void prepare(FusekiServer.Builder serverBuilder, Set<String> datasetNames, Model configModel) {
        boolean cmdLine = ( configModel == null );
        // && set.size == 0 or 1.
    }

//
////    @Override
////    public void configured(FusekiServer.Builder serverBuilder, DataAccessPointRegistry dapRegistry, Model configModel) {
////        dapRegistry.accessPoints().forEach(accessPoint->configDataAccessPoint(accessPoint, configModel));
////    }
//
//    @Override
//    public void configDataAccessPoint(DataAccessPoint dap, Model configModel) {}
//
//    @Override
//    public void server(FusekiServer server) { }
//
//    @Override
//    public boolean serverConfirmReload(FusekiServer server) { return true; }
//
//    @Override
//    public void serverReload(FusekiServer server) { }

    // ---- Server start-stop lifecycle.

    @Override
    public void serverBeforeStarting(FusekiServer server) {
        // If logging. Servlet attribute
        FusekiRunnerLogging.logCode(logger);
        FusekiRunnerLogging.logServerSetup(logger, server);
    }

    @Override
    public void serverAfterStarting(FusekiServer server) {
        FusekiRunnerLogging.logServerStart(logger, server);
    }

    @Override
    public void serverStopped(FusekiServer server) {
        FusekiRunnerLogging.logServerStop(logger, server);
    }
}
