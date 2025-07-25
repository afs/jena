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

package org.apache.jena.fuseki.mod.admin;

import static org.apache.jena.fuseki.mgt.ServerMgtConst.opServer;
import static org.apache.jena.fuseki.server.ServerConst.opPing;
import static org.apache.jena.http.HttpOp.httpGet;
import static org.apache.jena.http.HttpOp.httpGetJson;
import static org.apache.jena.http.HttpOp.httpPost;
import static org.apache.jena.http.HttpOp.httpPostRtnJSON;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.atlas.lib.Lib;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.fuseki.mgt.ServerMgtConst;
import org.apache.jena.fuseki.server.ServerConst;
import org.apache.jena.fuseki.test.HttpTest;
import org.apache.jena.web.HttpSC;
import org.junit.jupiter.api.Test;

/**
 *  Tests of the admin functionality using a pre-configured dataset.
 *  This class does not test adding and deleting of datasets.
 */
public class TestAdmin extends FusekiServerPerTestClass {

    // --- Ping

    @Test public void ping_1() {
        httpGet(urlRoot()+"$/"+opPing);
    }

    @Test public void ping_2() {
        httpPost(urlRoot()+"$/"+opPing);
    }

    // --- Server status

    @Test public void server_1() {
        JsonValue jv = httpGetJson(urlRoot()+"$/"+opServer);
        JsonObject obj = jv.getAsObject();
        // Now optional : assertTrue(obj.hasKey(JsonConst.admin));
        assertTrue(obj.hasKey(ServerConst.datasets));
        assertTrue(obj.hasKey(ServerMgtConst.uptime));
        assertTrue(obj.hasKey(ServerMgtConst.startDT));
    }

    @Test public void server_2() {
        httpPost(urlRoot()+"$/"+opServer);
    }

    @Test public void sleep_1() {
        String x = execSleepTask(1);
    }

    @Test public void sleep_2() {
        try {
            String x = execSleepTask(-1);
            fail("Sleep call unexpectedly succeed");
        } catch (HttpException ex) {
            assertEquals(400, ex.getStatusCode());
        }
    }

    @Test public void sleep_3() {
        try {
            String x = execSleepTask(20*1000+1);
            fail("Sleep call unexpectedly succeed");
        } catch (HttpException ex) {
            assertEquals(400, ex.getStatusCode());
        }
    }

    // Async task testing

    @Test public void task_1() {
        String x = execSleepTask(10);
        assertNotNull(x);
        Integer.parseInt(x);
    }

    @Test public void task_2() {
        String x = "NoSuchTask";
        String url = urlRoot()+"$/tasks/"+x;
        HttpTest.expect404(()->httpGetJson(url) );
        try {
            checkInTasks(x);
            fail("No failure!");
        } catch (AssertionError ex) {}
    }


    @Test public void task_3() {
        // Timing dependent.
        // Create a "long" running task so we can find it.
        String x = execSleepTask(100);
        checkTask(x);
        checkInTasks(x);
        assertNotNull(x);
        Integer.parseInt(x);
    }

    @Test public void task_4() {
        // Timing dependent.
        // Create a "short" running task
        String x = execSleepTask(1);
        // Check exists in the list of all tasks (should be "finished")
        checkInTasks(x);
        String url = urlRoot()+"$/tasks/"+x;

        boolean finished = false;
        for ( int i = 0; i < 10; i++ ) {
            if ( i != 0 )
                Lib.sleep(25);
            JsonValue v = httpGetJson(url);
            checkTask(v);
            if ( v.getAsObject().hasKey("finished") ) {
                finished = true;
                break;
            }
        }
        if ( ! finished )
            fail("Task has not finished");
    }

    @Test public void task_5() {
        // Short running task - still in info API call.
        String x = execSleepTask(1);
        checkInTasks(x);
    }

    @Test public void task_6() {
        String x1 = execSleepTask(1000);
        String x2 = execSleepTask(1000);
        await().timeout(500,TimeUnit.MILLISECONDS).until(() -> runningTasks().size() > 1);
        await().timeout(2000, TimeUnit.MILLISECONDS).until(() -> runningTasks().isEmpty());
    }

    @Test public void task_7() {
        try {
            String x1 = execSleepTask(1000);
            String x2 = execSleepTask(1000);
            String x3 = execSleepTask(1000);
            String x4 = execSleepTask(1000);
            try {
                // Try to make test more stable on a loaded CI server.
                // Unloaded the first sleep will fail but due to slowness/burstiness
                // some tasks above may have completed.
                String x5 = execSleepTask(4000);
                String x6 = execSleepTask(4000);
                String x7 = execSleepTask(4000);
                String x8 = execSleepTask(10);
                fail("Managed to add a 5th test");
            } catch (HttpException ex) {
                assertEquals(HttpSC.BAD_REQUEST_400, ex.getStatusCode());
            }
        } finally {
            waitForTasksToFinish(1000, 250, 4000);
        }
    }

    private String execSleepTask(int millis) {
        String url = urlRoot()+"$/sleep";
        JsonValue v = httpPostRtnJSON(url+"?interval="+millis);
        String id = v.getAsObject().getString("taskId");
        return id;
    }

    private void checkTask(String x) {
        String url = urlRoot()+"$/tasks/"+x;
        JsonValue v = httpGetJson(url);
        checkTask(v);
    }

    private void checkTask(JsonValue v) {
        assertNotNull(v);
        assertTrue(v.isObject());
        //System.out.println(v);
        JsonObject obj = v.getAsObject();
        try {
            assertTrue(obj.hasKey("task"));
            assertTrue(obj.hasKey("taskId"));
            // Not present until it runs : "started"
        } catch (AssertionError ex) {
            System.out.println(obj);
            throw ex;
        }
    }

   private void checkInTasks(String x) {
       String url = urlRoot()+"$/tasks";
       JsonValue v = httpGetJson(url);
       assertTrue(v.isArray());
       JsonArray array = v.getAsArray();
       int found = 0;
       for ( int i = 0; i < array.size(); i++ ) {
           JsonValue jv = array.get(i);
           assertTrue(jv.isObject());
           JsonObject obj = jv.getAsObject();
           checkTask(obj);
           if ( obj.getString("taskId").equals(x) ) {
               found++;
           }
        }
       assertEquals(1, found, "Occurrence of taskId count");
    }

   private List<String> runningTasks(String... x) {
       String url = urlRoot()+"$/tasks";
       JsonValue v = httpGetJson(url);
       assertTrue(v.isArray());
       JsonArray array = v.getAsArray();
       List<String> running = new ArrayList<>();
       for ( int i = 0; i < array.size(); i++ ) {
           JsonValue jv = array.get(i);
           assertTrue(jv.isObject());
           JsonObject obj = jv.getAsObject();
           if ( isRunning(obj) )
               running.add(obj.getString("taskId"));
       }
       return running;
   }

   /**
    * Wait for tasks to all finish.
    * Algorithm: wait for {@code pause}, then start polling for upto {@code maxWaitMillis}.
    * Intervals in milliseconds.
    * @param pauseMillis
    * @param pollInterval
    * @param maxWaitMillis
    * @return
    */
   private boolean waitForTasksToFinish(int pauseMillis, int pollInterval, int maxWaitMillis) {
       // Wait for them to finish.
       // Divide into chunks
       if ( pauseMillis > 0 )
           Lib.sleep(pauseMillis);
       long start = System.currentTimeMillis();
       long endTime = start + maxWaitMillis;
       final int intervals = maxWaitMillis/pollInterval;
       long now = start;
       for (int i = 0 ; i < intervals ; i++ ) {
           // May have waited (much) longer than the pollInterval : heavily loaded build systems.
           if ( now-start > maxWaitMillis )
               break;
           List<String> x = runningTasks();
           if ( x.isEmpty() )
               return true;
           Lib.sleep(pollInterval);
           now = System.currentTimeMillis();
       }
       return false;
   }

   private boolean isRunning(JsonObject taskObj) {
       checkTask(taskObj);
       return taskObj.hasKey("started") &&  ! taskObj.hasKey("finished");
   }
}

