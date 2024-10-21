/**
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

package org.apache.jena.riot.system;

//import org.junit.platform.suite.api.SelectClasses;
//import org.junit.platform.suite.api.Suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.apache.jena.riot.system.irix.TestIRIxRIOT_JenaIRI;
import org.apache.jena.riot.system.irix.TestIRIxRIOT_iri3986;
@RunWith(Suite.class)
@SuiteClasses({

    // 2024-10 Using Junit5 can confuse Eclipse testing.
    // Test classes get missed
//@Suite
//@SelectClasses({
      TestChecker.class
    , TestStreamRDF.class
    , TestFactoryRDF.class
    , TestFactoryRDFCaching.class

    // Prefix Map implementations
    , TestPrefixMap.class
    , TestPrefixMapOverPrefixMapping.class
    , TestPrefixMapWrapper.class
    , TestPrefixMapOther.class

    , TestIO_JenaReaders.class
    , TestIO_JenaWriters.class
    , TestLangRegistration.class
    , TestFormatRegistration.class
    , TestJsonLDReadWrite.class         // Some simple testing of the jsonld-java engine.
    , TestSerializable.class

    , TestIRIxRIOT_iri3986.class
    , TestIRIxRIOT_JenaIRI.class

    , TestRiotLib.class
    , TestAsyncParser.class
})

/**
 * Test suite for RIOT system
 */
public class TS_RiotSystem
{}
