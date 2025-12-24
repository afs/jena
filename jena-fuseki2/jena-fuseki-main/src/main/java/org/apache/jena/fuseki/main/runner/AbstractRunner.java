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

import java.net.BindException;

import org.apache.jena.fuseki.FusekiException;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModule;

/**
 * Machinary to construct a Fuseki server from modules and the command line.
 * <p>
 * See {@link FusekiServer.Builder} for programmatic construction.
 * See {@link FusekiRunner} for including {@link FusekiModule FusekiModules}.
 *
 */
abstract /*package*/ class AbstractRunner implements Runner {

    /** {@inheritDoc} */
    @Override
    public void run(String... args) {
        process(args).join();
    }

    /** {@inheritDoc} */
    @Override
    public FusekiServer runAsync(String...args) {
        return process(args);
    }

    private static Object lock = new Object();

    /**
     * Build and start.
     */
    protected FusekiServer process(String...args) {
        synchronized(lock) {
            FusekiServer server = construct(args);
            startAsync(server);
            return server;
        }
    }

    private static FusekiServer startAsync(FusekiServer server) {
        try {
            return server.start();
        } catch (FusekiException ex) {

            if ( ex.getCause() instanceof BindException bindEx) {
                String msg = "Failed to start server: "+bindEx.getMessage()+ ": port in use";
                throw new FusekiBindException(msg, bindEx);
            }
            throw ex;
        } catch (Exception ex) {
            throw exception("Failed to start server: " + ex.getMessage(), ex);
        }
    }

    private static RuntimeException exception(String message, Throwable ex) {
        throw new FusekiException(message, ex);
    }

    /**
     * Build a {@link FusekiServer} from command line arguments.
     * Return the server, which has not be started.
     * @param args line arguments.
     *
     */
    protected FusekiServer construct(String... args) {
        FusekiServer.Builder builder = builder(args);
        return builder.build();
    }

    /**
     * Create a {@link org.apache.jena.fuseki.main.FusekiServer.Builder}
     * initialized according to the command line arguments processed.
     */
    abstract protected FusekiServer.Builder builder(String... args);
}
