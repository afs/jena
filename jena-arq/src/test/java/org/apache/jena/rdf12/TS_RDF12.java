package org.apache.jena.rdf12;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import org.apache.jena.rdf12.basic.TS_RDFStar_Basic;

// Currently, RDF1.2 and SPARQL 1.2 specific tests.
// Split sometime / replace with scripted tests.
@Suite
@SelectClasses({
    // Old - to be removed.
    TS_RDFStar_Basic.class,

    TestRDF12LangSyntax.class,
    TestSPARQL12Syntax.class,
    TestSPARQL12Results.class
})
public class TS_RDF12 {
}
