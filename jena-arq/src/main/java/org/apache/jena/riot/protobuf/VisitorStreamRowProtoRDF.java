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

package org.apache.jena.riot.protobuf;

import org.apache.jena.riot.protobuf.wire.PB_RDF.RDF_IRI;
import org.apache.jena.riot.protobuf.wire.PB_RDF.RDF_PrefixDecl;
import org.apache.jena.riot.protobuf.wire.PB_RDF.RDF_Quad;
import org.apache.jena.riot.protobuf.wire.PB_RDF.RDF_Triple;

/** Visitor for RDF_StreamRow */
public interface VisitorStreamRowProtoRDF {
    public void visit(RDF_Triple triple);

    public void visit(RDF_Quad quad);

    public void visit(RDF_IRI base);

    public void visit(RDF_PrefixDecl prefix);
}
