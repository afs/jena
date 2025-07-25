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

package org.apache.jena.tdb2.store;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDFS;

public class TestVisibilityOfChanges {
    private Dataset dataset;

    @BeforeEach
    public void before() {
        dataset = TDB2Factory.connectDataset(Location.mem());
    }

    @AfterEach
    public void after() {
        dataset.close();
    }

    @Test
    public void visibilityOfNewlyCreatedResources() {
        Resource resource = ResourceFactory.createResource();
        assertFalse(Txn.calculateRead(dataset, () -> dataset.getDefaultModel().containsResource(resource)));
        Txn.executeWrite(dataset, () -> {
            dataset.getDefaultModel().add(resource, RDFS.label, "I exist!");
            assertTrue(dataset.getDefaultModel().containsResource(resource));
            rinseCache();
            assertTrue(dataset.getDefaultModel().containsResource(resource),
                       ()->"A newly created resource should be visible within the write transaction");
        });
        assertTrue(Txn.calculateRead(dataset, () -> dataset.getDefaultModel().containsResource(resource)),
                   ()->"A newly created resource should be visible after commit");
    }

    @Test
    public void testIsolation() {
        Resource resource = ResourceFactory.createResource();
        Txn.executeWrite(dataset, () -> {
            assertFalse(dataset.getDefaultModel().containsResource(resource));
            dataset.getDefaultModel().add(resource, RDFS.label, "I exist!");
            rinseCache();
            executeAndWait(() -> Txn.executeRead(dataset, () -> dataset.getDefaultModel().containsResource(resource)));
            assertTrue(dataset.getDefaultModel().containsResource(resource),
                       ()->"A read transaction should not pollute the nonPresent cache if there's a write transaction");
        });
    }

    private void rinseCache() {
        int cacheSize = 2000;
        for (int i = 0; i < cacheSize; i++) {
            Resource newResource = ResourceFactory.createResource();
            dataset.getDefaultModel().add(newResource, RDFS.label, "");
        }
    }

    private void executeAndWait(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail();
        }
    }
}
