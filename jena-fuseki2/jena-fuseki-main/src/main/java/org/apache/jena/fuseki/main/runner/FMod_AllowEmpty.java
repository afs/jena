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

import org.apache.jena.cmd.CmdGeneral;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.rdf.model.Model;

public class FMod_AllowEmpty implements FusekiModule {

    // arguments lifecycle
    @Override
    public void serverArgsModify(CmdGeneral fusekiCmd, ServerArgs serverArgs) {
        // After std args added, before argument processing.
        serverArgs.allowEmpty = true;
        //serverArgs.fusekiModules = serverModules;
    }

    @Override
    public void serverArgsPrepare(CmdGeneral fusekiCmd, ServerArgs serverArgs) { }

    @Override
    public void serverArgsBuilder(FusekiServer.Builder serverBuilder, Model configModel) {}
}
