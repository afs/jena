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

package org.apache.jena.sparql.syntax;

/**
 * An element visitor that walks the graph pattern tree for one query level. applying
 * a visitor at each Element traversed.<br/>
 * Does not process subqueries.<br/>
 * Does not process (NOT)EXISTS in filters.<br/>
 * These will need to call down themselves if it is meaningful for the visitor.
 * Bottom-up walk - apply to subelements before applying to current element.
 */

public class ElementWalker {
    public static void walk(Element el, ElementVisitor visitor) {
        walk(el, visitor, null, null);
    }

    public static void walk(Element el, ElementVisitor visitor, ElementVisitor beforeVisitor, ElementVisitor afterVisitor) {
        EltWalker w = new EltWalker(visitor, beforeVisitor, afterVisitor);
        el.visit(w);
    }

    protected static void walk$(Element el, EltWalker walker) {
        el.visit(walker);
    }

    static class EltWalker implements ElementVisitor {
        protected final ElementVisitor proc;
        protected final ElementVisitor beforeVisitor;
        protected final ElementVisitor afterVisitor;

        protected EltWalker(ElementVisitor visitor, ElementVisitor beforeVisitor, ElementVisitor afterVisitor) {
            proc = visitor;
            this.beforeVisitor = beforeVisitor;
            this.afterVisitor = afterVisitor;
        }

        private void before(Element elt) {
            if ( beforeVisitor != null )
                elt.visit(beforeVisitor);
        }

        private void after(Element elt) {
            if ( afterVisitor != null )
                elt.visit(afterVisitor);
        }

        @Override
        public void visit(ElementTriplesBlock el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementFilter el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementAssign el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementBind el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementUnfold el)
        {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementData el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementUnion el) {
            before(el);
            for ( Element e : el.getElements() )
                e.visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementGroup el) {
            before(el);
            for ( Element e : el.getElements() )
                e.visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementOptional el) {
            before(el);
            if ( el.getOptionalElement() != null )
                el.getOptionalElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementLateral el) {
            before(el);
            if ( el.getLateralElement() != null )
                el.getLateralElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementSemiJoin el) {
            before(el);
            if ( el.getSubElement() != null )
                el.getSubElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementAntiJoin el) {
            before(el);
            if ( el.getSubElement() != null )
                el.getSubElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementDataset el) {
            before(el);
            if ( el.getElement() != null )
                el.getElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementNamedGraph el) {
            before(el);
            if ( el.getElement() != null )
                el.getElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementService el) {
            before(el);
            if ( el.getElement() != null )
                el.getElement().visit(this);
            proc.visit(el);
            after(el);
        }

        // EXISTs, NOT EXISTs also occur in FILTERs via expressions.

        @Override
        public void visit(ElementExists el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementNotExists el) {
            before(el);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementMinus el) {
            before(el);
            if ( el.getMinusElement() != null )
                el.getMinusElement().visit(this);
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementSubQuery el) {
            before(el);
            // This does not automatically walk into the subquery.
            proc.visit(el);
            after(el);
        }

        @Override
        public void visit(ElementPathBlock el) {
            before(el);
            proc.visit(el);
            after(el);
        }
    }
}
