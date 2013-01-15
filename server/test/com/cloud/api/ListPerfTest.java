// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;


/**
 * Test fixture to do performance test for list command
 * Currently we commented out this test suite since it requires a real MS and Db running.
 *
 * @author Min Chen
 *
 */
public class ListPerfTest extends APITest {



    @Before
    public void setup(){
        // always login for each testcase
        login("admin", "password");
    }

    @Test
    public void testListVM(){
        // issue list VM calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listVirtualMachines", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list VM: " + (after - before) + " ms");

    }

    @Test
    public void testListVMXML(){
        // issue list VM calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listVirtualMachines", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list VM: " + (after - before) + " ms");

    }

    @Test
    public void testListRouter(){
        // issue list VM calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listRouters", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list Routers: " + (after - before) + " ms");

    }

    @Test
    public void testListRouterXML(){
        // issue list VM calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listRouters", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list Routers: " + (after - before) + " ms");

    }

    @Test
    public void testListHosts(){
        // issue list Hosts calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listHosts", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list Hosts: " + (after - before) + " ms");

    }

    @Test
    public void testListVolumes(){
        // issue list Volumes calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listVolumes", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list Volumes: " + (after - before) + " ms");

    }

    @Test
    public void testListAccounts(){
        // issue list Accounts calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listAccounts", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list Accounts: " + (after - before) + " ms");

    }

    @Test
    public void testListUsers(){
        // issue list Users calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listUsers", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list Users: " + (after - before) + " ms");

    }

    @Test
    public void testListStoragePools(){
        // issue list Storage pool calls
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        long before = System.currentTimeMillis();
        String result = this.sendRequest("listStoragePools", params);
        long after = System.currentTimeMillis();
        System.out.println("Time taken to list StoragePools: " + (after - before) + " ms");

    }

    @Test
    public void testMultiListAccounts() throws Exception {
        // log in using normal user
        login("demo", "password");
        // issue list Accounts calls
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("response", "json");
        params.put("listAll", "true");
        params.put("sessionkey", sessionKey);
        int clientCount = 6;
        Runnable[] clients = new Runnable[clientCount];
        final boolean[] isUsable = new boolean[clientCount];

        final CountDownLatch startGate = new CountDownLatch(1);

        final CountDownLatch endGate = new CountDownLatch(clientCount);


        for (int i = 0; i < isUsable.length; ++i) {
            final int j = i;
            clients[j] = new Runnable() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    try {
                        startGate.await();

                        System.out.println(sendRequest("listAccounts", params));

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        endGate.countDown();
                    }
                }
            };
        }

        ExecutorService executor = Executors.newFixedThreadPool(clientCount);

        for (Runnable runnable : clients) {
            executor.execute(runnable);
        }

        startGate.countDown();

        endGate.await();

    }

}
