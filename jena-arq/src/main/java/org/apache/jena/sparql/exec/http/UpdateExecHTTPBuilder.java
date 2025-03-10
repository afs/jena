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

package org.apache.jena.sparql.exec.http;

import static org.apache.jena.http.HttpLib.copyArray;

import java.net.http.HttpClient;
import java.util.HashMap;

import org.apache.jena.http.sys.ExecUpdateHTTPBuilder;
import org.apache.jena.sparql.exec.UpdateExecBuilder;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.UpdateRequest;

public class UpdateExecHTTPBuilder extends ExecUpdateHTTPBuilder<UpdateExecHTTP, UpdateExecHTTPBuilder> implements UpdateExecBuilder {

    public static UpdateExecHTTPBuilder create() { return new UpdateExecHTTPBuilder(); }

    private UpdateExecHTTPBuilder() {}

    @Override
    protected UpdateExecHTTPBuilder thisBuilder() {
        return this;
    }

    @Override
    protected UpdateExecHTTP buildX(HttpClient hClient, UpdateRequest updateActual, String updateStringActual, Context cxt) {
        return new UpdateExecHTTP(serviceURL, updateActual, updateStringActual, hClient, params,
                                  copyArray(usingGraphURIs),
                                  copyArray(usingNamedGraphURIs),
                                  new HashMap<>(httpHeaders),
                                  sendMode, cxt, timeout, timeoutUnit);
    }
}
