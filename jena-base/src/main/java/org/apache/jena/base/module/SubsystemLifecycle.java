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

package org.apache.jena.base.module;

/** Lifecycle interface for modules and subsystems. */
public interface SubsystemLifecycle {

    /**
     * start - a module should be ready to operate when this returns.
     */
    public void start();

    /**
     * stop - a module should have performed any shutdown operations by the time this
     * returns. Caution: code must be prepared to operate without assuming this
     * called. Abrupt termination of the JVM is always possible.
     */
    public void stop();

    /**
     * Provide a marker as to the level to order initialization, 10,20,30,...
     * See {@link Subsystem} for details.
     */
    default public int level() {
        return 9999;
    }
}

