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

package org.apache.jena.sparql.engine.iterator;

import java.util.Iterator;

import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;

/** SemiJoin and AntiJoin */
public class QueryIterHalfJoin extends QueryIter2LoopOnLeft {

    private final MATCHES_PARITY parity;

    private QueryIterHalfJoin(MATCHES_PARITY parity, QueryIterator left, QueryIterator right, ExecutionContext qCxt) {
        super(left, right, qCxt);
        this.parity = parity;
    }

    enum MATCHES_PARITY { KEEP, REJECT }

    public static QueryIterator semiJoin(QueryIterator left, QueryIterator right, ExecutionContext execCxt) {
        return new QueryIterHalfJoin(MATCHES_PARITY.KEEP, left, right, execCxt);
    }

    public static QueryIterator antiJoin(QueryIterator left, QueryIterator right, ExecutionContext execCxt) {
        return new QueryIterHalfJoin(MATCHES_PARITY.REJECT, left, right, execCxt);
    }

    // This needs improving - take the hashjoin code from QueryIterMinus.
    @Override
    protected Binding getNextSlot(Binding bindingLeft) {
        boolean accept = true;

        for ( Iterator<Binding> iter = tableRight.iterator(null) ; iter.hasNext() ; ) {
            Binding bindingRight = iter.next();
            boolean matches = Algebra.compatible(bindingLeft, bindingRight);
            boolean keep = ( parity == MATCHES_PARITY.KEEP ) ? true : false ;
            if ( ! keep ) {
                accept = false;
                break;
            }
        }

        if ( accept )
            return bindingLeft;
        return null;
    }

}
