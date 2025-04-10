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

package org.apache.jena.rdf12.basic;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.junit.Test;

/**
 * Basic parsing tests of RDF-star constructs for N-Triples
 */
public class TestNTriplesStarParse {

    private ErrorHandler silent = ErrorHandlerFactory.errorHandlerStrictNoLogging;
    private static StreamRDF sink = StreamRDFLib.sinkNull();

    @Test public void parse_nt_good_1()    { parse("<x:s> <x:q> <<( <x:s> <x:p> <x:o>)>> . "); }

    @Test public void parse_nt_good_2()    { parse("_:b <x:p> <<(_:b <x:p> _:o)>>. "); }

    @Test public void parse_nt_good_3()    { parse("<x:x> <x:y> <<(<x:s1> <x:p1> <<( <x:s> <x:p> '1' )>> )>> ."); }

    @Test(expected=RiotException.class)
    // No reified triples.
    public void parse_nt_bad_1()           { parse("<< <x:s> <x:p> <x:o>>> . "); }

    @Test(expected=RiotException.class)
    public void parse_nt_bad_2()           { parse("<<( <x:s> <x:p> <x:o>)>> <x:y> <x:z> . "); }

    @Test(expected=RiotException.class)
    public void parse_nt_bad_3()           { parse("<x:s> <<( <x:s> <x:p> <x:o>)>> <x:x>  . "); }

    private void parse(String string) {
        RDFParser.fromString(string, Lang.NTRIPLES).errorHandler(silent).parse(sink);
    }
}
