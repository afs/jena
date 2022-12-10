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

package org.apache.jena.sparql.expr;

import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.nodevalue.NodeFunctions;
import org.apache.jena.sparql.sse.Tags;
import org.apache.jena.sparql.util.FmtUtils;


/** Glob matching */
public class E_Like extends ExprFunctionN {

    private static final String name = Tags.tagLike;

    public E_Like(Expr expr, Expr pattern, Expr flags) {
        super(name, expr, pattern, flags);
        init(pattern, flags);
    }

    public E_Like(Expr expr, String pattern) {
        this(expr, pattern, "");
    }

    public E_Like(Expr expr, String pattern, String flags) {
        super(name, expr, NodeValue.makeString(pattern), NodeValue.makeString(flags));
        init(getArg(2), getArg(3));
    }

    // Preparation, if possible.
    private void init(Expr pattern, Expr flags) {
//        if ( !ARQ.isStrictMode() ) {
//            try {
//                if ( pattern.isConstant() && pattern.getConstant().isString() && (flags == null || flags.isConstant()) )
//                    regexEngine = makeGlobMatcher(pattern.getConstant(), (flags == null) ? null : flags.getConstant());
//            } catch (ExprEvalException ex) {
//                // Here, we are doing static compilation of the pattern.
//                // ExprEvalException does not have a stacktrace.
//                // We could throw a non-eval exception.
//                throw ex; // new ExprException(ex.getMessage(), ex.getCause());
//            }
//        }
//    }
//
//    public static RegexEngine makeGlobMatcher(NodeValue vPattern, NodeValue vFlags) {
//        if ( !vPattern.isString() )
//            throw new ExprException("LIKE: Pattern is not a string: " + vPattern);
//        if ( vFlags != null && !vFlags.isString() )
//            throw new ExprException("LIKE: Pattern flags are not a string: " + vFlags);
//        String s = (vFlags == null) ? null : vFlags.getString();
//        checkFlags(s);
//        return makeGlobMatcher(vPattern.getString(), s);
//    }
//
//    private RegexEngine makeGlobMatcher(String patternStr, String flags) {
//        return null;
    }

    private String currentFailMessage = null;

    @Override
    public NodeValue eval(List<NodeValue> args) {
        Node arg = NodeFunctions.checkAndGetStringLiteral("LIKE", args.get(0));

        NodeValue vPattern = args.get(1);
        if ( !vPattern.isString() )
            throw new ExprException("LIKE: Pattern is not a string: " + vPattern);
        String pattern = vPattern.getString();

        NodeValue vFlags = (args.size() == 2 ? null : args.get(2));
        if ( vFlags != null && !vFlags.isString() )
          throw new ExprException("LIKE: Pattern flags are not a string: " + vFlags);
        String flags = (vFlags == null) ? null : vFlags.getString();
        checkFlags(flags);

        // Character classes?
        // Many glob matchers for filenames include e.g. "**" for any depth.
        // Possible string glob implementations:
        // * ORO GlobCompiler (retired project - source code)
        //    http://svn.apache.org/repos/asf/jakarta/oro/trunk/src/java/org/apache/oro/text/
        // * com.hrakaroo:glob (MIT)

        IOCase textCase = StringUtils.equals(flags, "i") ? IOCase.INSENSITIVE : IOCase.SENSITIVE;
        pattern = fixup(pattern);
        boolean b = FilenameUtils.wildcardMatch(arg.getLiteralLexicalForm(), pattern, textCase);
        return NodeValue.booleanReturn(b);
    }

    // https://issues.apache.org/jira/browse/IO-246
    // Trailing "...*?" does not work but it is equivalent to "...?*"
    private String fixup(String pattern) {
        if ( ! pattern.endsWith("*?") )
            return pattern;
        String x = pattern.substring(0, pattern.length()-2);
        return x+"?*";
    }

    private static void checkFlags(String flags) {
        if ( flags == null )
            return;
        if ( !StringUtils.containsOnly(flags, "i") )
            throw new ExprEvalException("LIKE: Only 'i' is a legal pattern flags: got \"" + FmtUtils.stringEsc(flags) + "\"");
    }

    @Override
    public Expr copy(ExprList newArgs) {
        if ( newArgs.size() == 2 )
            return new E_Like(newArgs.get(0), newArgs.get(1), null);
        return new E_Like(newArgs.get(0), newArgs.get(1), newArgs.get(2));
    }
}
