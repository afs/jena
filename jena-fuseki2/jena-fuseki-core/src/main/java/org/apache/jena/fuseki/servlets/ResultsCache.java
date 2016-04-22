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

package org.apache.jena.fuseki.servlets;

import java.util.concurrent.ConcurrentHashMap ;

import org.apache.jena.atlas.lib.Cache ;
import org.apache.jena.atlas.lib.CacheFactory ;
import org.apache.jena.query.Query ;
import org.apache.jena.sparql.resultset.SPARQLResult ;

public class ResultsCache {
    
    private static ResultsCache singleton = new ResultsCache() ; 
    
    public static ResultsCache get() { return singleton ; }
    private ResultsCache() {}
    
    // Invalidation : Update GSP, Upload -- Done in HttpAction.beginWrite
    //    better - bing able to lock entries in the cache and brack begin-end for read/create and write
    // Configuration
    // Deletion

    private ConcurrentHashMap<String, Cache<Query, SPARQLResult>> x = new ConcurrentHashMap<>() ;

    public Cache<Query, SPARQLResult> getByDataset(HttpAction action) {
        return x.get(name(action)) ;
    }

    public Cache<Query, SPARQLResult> getCreateByDataset(HttpAction action) {
        return x.computeIfAbsent(name(action), this::build) ;
    }

    private Cache<Query, SPARQLResult> build(String name) {
        // Improve!
        return CacheFactory.createCache(10) ;
    }
    
    public void updateAction(HttpAction action) {
        clearDatasetCache(action);
    }
    
    public void clearDatasetCache(HttpAction action) {
        x.remove(name(action)) ;
        //x.computeIfPresent(name, (n,cache) -> { cache.clear() ; return cache ; }) ;
    }
    
    private static String name(HttpAction action) {
        return action.getAccessPointName() ;
    }
}
