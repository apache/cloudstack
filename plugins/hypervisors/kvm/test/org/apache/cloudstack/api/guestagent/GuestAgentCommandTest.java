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
package org.apache.cloudstack.api.guestagent;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GuestAgentCommandTest {

    @Test
    public void testWithoutArgument() {
        String command = "guest-sync";
        Gson gson = new GsonBuilder().create();
        GuestAgentCommand cmd = new GuestAgentCommand(command, null);
        String data = gson.toJson(cmd);
        assertEquals("{\"execute\":\"" + command + "\"}", data);
    }

    @Test
    public void testWithArgument() {
        HashMap arguments = new HashMap();
        int id = (int)(Math.random() * 1000000) + 1;
        arguments.put("id", id);
        String command = "guest-sync";
        Gson gson = new GsonBuilder().create();
        GuestAgentCommand cmd = new GuestAgentCommand(command, arguments);
        String data = gson.toJson(cmd);
        assertEquals("{\"execute\":\"" + command + "\",\"arguments\":{\"id\":" + id + "}}", data);
    }
}
