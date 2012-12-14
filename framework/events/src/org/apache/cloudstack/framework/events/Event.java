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

package org.apache.cloudstack.framework.events;

import com.google.gson.Gson;

public class Event {

    String category;
    String type;
    String routingKey;
    String description;
    String publisher;
    String date;
    String resourceType;

    public Event(String category, String type, String routingKey) {
        this.category = category;
        this.type = type;
        this.routingKey = routingKey;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription (Object message) {
        Gson gson = new Gson();
        this.description = gson.toJson(message).toString();
    }

    public String getEventPublisher() {
        return publisher;
    }

    void setEventPublisher(String source) {
        this.publisher = source;
    }

    public String getDate() {
        return date;
    }

    void setDate(String date) {
        this.date = date;
    }
}
