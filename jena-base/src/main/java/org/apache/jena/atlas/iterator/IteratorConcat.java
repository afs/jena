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

package org.apache.jena.atlas.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * Iterator of Iterators IteratorConcat is better when there are lots of iterators to
 * be joined.
 * <p>
 * {@link IteratorCons} is slightly better for two iterators.
 */

public class IteratorConcat<T> implements IteratorCloseable<T> {
    private List<Iterator<T>> iterators = new ArrayList<>();
    int idx = -1;
    private Iterator<T> current = null;
    boolean finished = false;

    /**
     * Usually, it is better to create an IteratorConcat explicitly and add iterator
     * if there are going to be many.
     *
     * @param iter1
     * @param iter2
     * @return Iterator
     * @see IteratorCons
     */
    public static <T> Iterator<T> concat(Iterator<T> iter1, Iterator<T> iter2) {
        if ( iter2 == null )
            return iter1;
        if ( iter1 == null )
            return iter2;
        IteratorConcat<T> c = new IteratorConcat<>();
        c.add(iter1);
        c.add(iter2);
        return c;
    }

    public IteratorConcat() {}

    public void add(Iterator<T> iter) {
        iterators.add(iter);
    }

    @Override
    public boolean hasNext() {
        if ( finished )
            return false;

        if ( current != null && current.hasNext() )
            return true;
        if ( current != null )
            Iter.close(current);
        idx++;
        current = null;
        for ( ; idx < iterators.size() ; idx++ ) {
            current = iterators.get(idx);
            if ( current.hasNext() )
                // Next
                return true;
            // Nothing here - move on.
            current = null;
        }
        // idx has run off the end.
        return false;
    }

    @Override
    public T next() {
        if ( !hasNext() )
            throw new NoSuchElementException();
        return current.next();
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        if( finished )
            return;
        if( current != null ) {
            current.forEachRemaining(action);
            Iter.close(current);
        }
        idx++;
        for ( ; idx < iterators.size() ; idx++ ) {
            current = iterators.get(idx);
            current.forEachRemaining(action);
            Iter.close(current);
        }
        current = null;
        finished = true;
    }

    @Override
    public void close() {
        //iterators.forEach(Iter::close);
        // Earlier iterators already closed
        for ( int i = idx ; i < iterators.size() ; i++ ) {
            Iterator<T> iter = iterators.get(idx);
            Iter.close(iter);
        }

    }
}
