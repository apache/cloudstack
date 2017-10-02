// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.doc;

import java.io.Serializable;
import java.util.ArrayList;

public class Command implements Serializable{

    /**
     *
     */
    private static final long serialVersionUID = -4318310162503004975L;
    private String name;
    private String description;
    private String usage;
    private boolean isAsync;
    private String sinceVersion = null;
    private ArrayList<Argument> request;
    private ArrayList<Argument> response;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Command() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Argument> getRequest() {
        return request;
    }

    public void setRequest(ArrayList<Argument> request) {
        this.request = request;
    }

    public ArrayList<Argument> getResponse() {
        return response;
    }

    public void setResponse(ArrayList<Argument> response) {
        this.response = response;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    public String getSinceVersion() {
        return sinceVersion;
    }

    public void setSinceVersion(String sinceVersion) {
        this.sinceVersion = sinceVersion;
    }

    public Argument getReqArgByName(String name) {
        for (Argument a : getRequest()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public Argument getResArgByName(String name) {
        for (Argument a : getResponse()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }
}
