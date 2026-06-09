//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Communicates with the cloudstack-image-server control socket via socat.
 *
 * Protocol: newline-delimited JSON over a Unix domain socket.
 * Actions: register, unregister, status.
 */
public class ImageServerControlSocket {
    private static final Logger LOGGER = LogManager.getLogger(ImageServerControlSocket.class);
    static final String CONTROL_SOCKET_PATH = "/var/run/cloudstack/image-server.sock";
    private static final Gson GSON = new GsonBuilder().create();

    private ImageServerControlSocket() {
    }

    /**
     * Send a JSON message to the image server control socket and return the
     * parsed response, or null on communication failure.
     */
    static JsonObject sendMessage(Map<String, Object> message) {
        String json = GSON.toJson(message);
        Script script = new Script("/bin/bash", LOGGER);
        script.add("-c");
        script.add(String.format("echo '%s' | socat -t5 - UNIX-CONNECT:%s",
                json.replace("'", "'\\''"), CONTROL_SOCKET_PATH));
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        if (result != null) {
            LOGGER.error("Control socket communication failed: {}", result);
            return null;
        }
        String output = parser.getLines();
        if (output == null || output.trim().isEmpty()) {
            LOGGER.error("Empty response from control socket");
            return null;
        }
        try {
            return JsonParser.parseString(output.trim()).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to parse control socket response: {}", output, e);
            return null;
        }
    }

    /**
     * Register a transfer config with the image server.
     * @return true if the server accepted the registration.
     */
    public static boolean registerTransfer(String transferId, Map<String, Object> config) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("action", "register");
        msg.put("transfer_id", transferId);
        msg.put("config", config);
        JsonObject resp = sendMessage(msg);
        if (resp == null) {
            return false;
        }
        return "ok".equals(resp.has("status") ? resp.get("status").getAsString() : null);
    }

    /**
     * Unregister a transfer from the image server.
     * @return the number of remaining active transfers, or -1 on error.
     */
    public static int unregisterTransfer(String transferId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("action", "unregister");
        msg.put("transfer_id", transferId);
        JsonObject resp = sendMessage(msg);
        if (resp == null) {
            return -1;
        }
        if (!"ok".equals(resp.has("status") ? resp.get("status").getAsString() : null)) {
            return -1;
        }
        return resp.has("active_transfers") ? resp.get("active_transfers").getAsInt() : -1;
    }

    /**
     * Check whether the image server control socket is responsive.
     * @return true if the server responded with status "ok".
     */
    public static boolean isReady() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("action", "status");
        JsonObject resp = sendMessage(msg);
        if (resp == null) {
            return false;
        }
        return "ok".equals(resp.has("status") ? resp.get("status").getAsString() : null);
    }
}
