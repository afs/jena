/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jena.arq.querybuilder.updatebuilder;

import java.util.function.Function;

import org.apache.jena.arq.querybuilder.Converters;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;

/**
 * A class to construct a quad iterator from the elements.
 *
 */
class QuadIteratorBuilder implements ElementVisitor {
    // the default graph name
    private final Node defaultGraph;
    // the extended iterator we will add to.
    ExtendedIterator<Quad> iter = WrappedIterator.emptyIterator();

    // a function to map triples to quads using the default graph name.
    private final Function<Triple, Quad> MAP = new Function<Triple, Quad>() {

        @Override
        public Quad apply(Triple triple) {
            return new Quad(defaultGraph, Converters.checkVar(triple.getSubject()),
                    Converters.checkVar(triple.getPredicate()), Converters.checkVar(triple.getObject()));
        }
    };

    /**
     * Constructor.
     *
     * @param defaultGraph the default graph name.
     */
    QuadIteratorBuilder(Node defaultGraph) {
        this.defaultGraph = defaultGraph;
    }

    @Override
    public void visit(ElementTriplesBlock el) {
        iter = iter.andThen(WrappedIterator.create(el.getPattern().getList().iterator()).mapWith(MAP));
    }

    @Override
    public void visit(ElementPathBlock el) {
        for (final TriplePath pth : el.getPattern().getList()) {
            if (!pth.isTriple()) {
                throw new QueryParseException("Paths not permitted in data quad", -1, -1);
            }
        }
        iter = iter.andThen(WrappedIterator.create(el.getPattern().getList().iterator()).mapWith(pth -> pth.asTriple())
                .mapWith(MAP));
    }

    @Override
    public void visit(ElementFilter el) {
        throw new QueryParseException("Paths not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementAssign el) {
        throw new QueryParseException("element assignment not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementBind el) {
        throw new QueryParseException("bind not permitted in data quad", -1, -1);

    }

    @Override
    public void visit(ElementUnfold el) {
        throw new QueryParseException("unfold not permitted in data quad", -1, -1);

    }

    @Override
    public void visit(ElementData el) {
        throw new QueryParseException("element data not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementUnion el) {
        for (final Element e : el.getElements()) {
            e.visit(this);
        }
    }

    @Override
    public void visit(ElementLateral el) {
        throw new QueryParseException("lateral not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementSemiJoin el) {
        throw new QueryParseException("semijoin not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementAntiJoin el) {
        throw new QueryParseException("antijoin not permitted in data quad", -1, -1);
    }


    @Override
    public void visit(ElementOptional el) {
        throw new QueryParseException("optional not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementGroup el) {
        for (final Element e : el.getElements()) {
            e.visit(this);
        }
    }

    @Override
    public void visit(ElementDataset el) {
        iter = iter.andThen(el.getDataset().find());
    }

    @Override
    public void visit(ElementNamedGraph el) {
        final QuadIteratorBuilder bldr = new QuadIteratorBuilder(el.getGraphNameNode());
        el.getElement().visit(bldr);
        iter = iter.andThen(bldr.iter);
    }

    @Override
    public void visit(ElementExists el) {
        throw new QueryParseException("exists not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementNotExists el) {
        throw new QueryParseException("not exists not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementMinus el) {
        throw new QueryParseException("minus not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementService el) {
        throw new QueryParseException("service not permitted in data quad", -1, -1);
    }

    @Override
    public void visit(ElementSubQuery el) {
        final Query q = el.getQuery();
        q.getQueryPattern().visit(this);
    }

}
