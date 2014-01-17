/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.util;

public class NexentaNmsUrl {
    protected final boolean isAuto;
    protected String schema;
    protected final String username;
    protected final String password;
    protected final String host;
    protected int port;

    public NexentaNmsUrl(boolean isAuto, String schema, String username, String password, String host, int port) {
        this.isAuto = isAuto;
        this.schema = schema;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    public boolean isAuto() {
        return this.isAuto;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getSchema() {
        return this.schema;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getPath() {
        return "/rest/nms";
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        if (isAuto) {
            b.append("auto://");
        } else {
            b.append(schema).append("://");
        }
        if (username != null && password != null) {
            b.append(username).append(":").append(password).append("@");
        } else if (username != null) {
            b.append(username).append("@");
        }
        b.append(host).append(":").append(port).append("/rest/nms/");
        return b.toString();
    }
}
