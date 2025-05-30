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

package org.apache.jena.tdb1.sys;

import org.apache.jena.sys.JenaSubsystemLifecycle ;
import org.apache.jena.tdb1.TDB1;

/** Jena subsystem initialization */
@SuppressWarnings("removal")
public class InitTDB implements JenaSubsystemLifecycle {

    @Override
    public void start() {
        TDB1.init() ;
    }

    @Override
    public void stop() {
        // This is savage and does not take account of in-flight transactions.
        TDB1.closedown() ;
    }

    @Override
    public int level() {
        return 40 ;
    }
}
