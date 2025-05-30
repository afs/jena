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

package org.apache.jena.riot.lang;

import java.util.Iterator ;

import org.apache.jena.graph.Node ;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.ParserProfile ;
import org.apache.jena.riot.system.StreamRDF ;
import org.apache.jena.riot.tokens.StringType;
import org.apache.jena.riot.tokens.Token ;
import org.apache.jena.riot.tokens.TokenType ;
import org.apache.jena.riot.tokens.Tokenizer ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

/** N-Quads, N-triples parser framework, with both push and pull interfaces.
 *
 * <ul>
 * <li>The {@link #parse} method processes the whole stream of tokens,
 *   sending each to a {@link org.apache.jena.atlas.lib.Sink} object.</li>
 * <li>The {@code Iterator&lt;X&gt;} interface yields triples one-by-one.</li>
 *  </ul>
 *
 * Normally, bad terms causes the parser to stop (i.e. treat them as errors).
 * In addition, the NTuples subsystem allows triples/quads with "bad" terms
 * to be skipped.
 *
 * Checking can be switched off completely. If the data is known to be correct,
 * no checking can be a large performance gain. <i>Caveat emptor</i>.
 */

public abstract class LangNTuple<X> extends LangBase implements Iterator<X>
{
    private static Logger log = LoggerFactory.getLogger(LangNTuple.class) ;

    protected boolean skipOnBadTerm = false ;

    protected LangNTuple(Tokenizer tokens, ParserProfile profile, StreamRDF dest) {
        super(tokens, profile, dest);
    }

    // Assumes no syntax errors.
    @Override
    public final boolean hasNext() {
        return super.moreTokens();
    }

    @Override
    public final X next() {
        return parseOne();
    }

    /** Parse one tuple - return object to be sent to the sink or null for none */
    protected abstract X parseOne() ;

    /** Note a tuple not being output */
    protected void skipOne(X object, String printForm, long line, long col) {
        errorHandler.warning("Skip: " + printForm, line, col);
    }

    protected abstract Node tokenAsNode(Token token) ;

    // One triple, not including terminator.
    protected final Triple parseTriple() {
        Token token = peekToken();
        Node s = parseSubject();
        Node p = parsePredicate();
        Node o = parseObject();
        return profile.createTriple(s, p, o, token.getLine(), token.getColumn());
    }

    protected final Node parseSubject() {
        // If we allow triple term in the subject position.
        // These would be rejected when the triple is created by the standard parser profile.
        //Node s = parseRDFTerm("subject");

        Token sToken = nextToken();
        if ( sToken.isEOF() )
            exception(sToken, "Premature end of file: %s", sToken);
        checkIRIOrBNode(sToken);
        Node s = tokenAsNode(sToken);
        return s;
    }

    // Parse, must be a URI.
    protected final Node parsePredicate() {
        Token pToken = nextToken();
        if ( pToken.isEOF() )
            exception(pToken, "Premature end of file: %s", pToken);
        checkIRI(pToken);
        Node p = tokenAsNode(pToken);
        return p;
    }

    // Parse, must be a URI.
    protected final Node parseObject() {
        return parseRDFTerm("object");
    }

    // General subject/object term
    protected final Node parseRDFTerm(String posn) {
        Token token = nextToken();
        if ( token.isEOF() )
            exception(token, "Premature end of file: %s", token);
        Node term;
        if ( token.hasType(TokenType.L_TRIPLE) )
            term = parseTripleTerm();
        else {
            checkRDFTerm(posn, token);
            term = tokenAsNode(token);
        }
        return term;
    }

    // Looking at "<<(" (L_TRIPLE)
    protected final Node parseTripleTerm() {
        Triple t = parseTriple();
        Token x = nextToken();
        if ( x.getType() != TokenType.R_TRIPLE )
            exception(x, "Triple term not terminated by )>>: %s", x);
        return NodeFactory.createTripleTerm(t);
    }

    protected final void checkIRIOrBNode(Token token) {
        if ( token.hasType(TokenType.IRI) )
            return;
        if ( token.hasType(TokenType.BNODE) )
            return;
        exception(token, "Expected BNode or IRI: Got: %s", token);
    }

    protected final void checkIRI(Token token) {
        if ( token.hasType(TokenType.IRI) )
            return;
        exception(token, "Expected IRI: Got: %s", token);
    }

    protected final void checkRDFTerm(String posn, Token token) {
        switch (token.getType()) {
            case IRI:
            case BNODE:
                return;
            case STRING:
                checkString(token);
                return ;
            case LITERAL_LANG:
            case LITERAL_DT:
                checkString(token.getSubToken1());
                return ;
            default:
                exception(token, "Illegal %s: %s", posn, token) ;
        }
    }

    private void checkString(Token token) {
        if ( token.isLongString() )
            exception(token, "Triple quoted string not permitted: %s", token) ;
        if ( isStrictMode() && ! token.hasStringType(StringType.STRING2) )
            exception(token, "Not a \"\"-quoted string: %s", token);
    }

    /** SkipOnBadTerm - do not output tuples with bad RDF terms */
    public boolean  getSkipOnBadTerm()                      { return skipOnBadTerm ; }
    /** SkipOnBadTerm - do not output tuples with bad RDF terms */
    public void     setSkipOnBadTerm(boolean skipOnBadTerm) { this.skipOnBadTerm = skipOnBadTerm ; }
}
