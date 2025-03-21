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

package org.apache.jena.sparql.engine.binding ;

import java.util.Collection ;

import org.apache.jena.sparql.core.Var ;

/** Project a binding, given a specific collection of visible variables */
public class BindingProject extends BindingProjectBase {
    final Collection<Var> projectionVars ;

    public BindingProject(Collection<Var> vars, Binding bind) {
        super(bind) ;
        this.projectionVars = vars ;
    }

    @Override
    protected boolean accept(Var var) {
        return projectionVars.contains(var) ;
    }

    @Override
    public Binding detach() {
        Binding b = binding.detach();
        return b == binding
            ? this
            : new BindingProject(projectionVars, b);
    }

    @Override
    protected Binding detachWithNewParent(Binding newParent) {
        throw new UnsupportedOperationException("Should never be called.");
    }
}
