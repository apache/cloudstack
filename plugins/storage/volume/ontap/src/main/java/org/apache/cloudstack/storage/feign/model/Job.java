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
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Administrator
 *
 */
@JsonInclude(Include.NON_NULL)
public class Job {

    @JsonProperty("uuid")
    String uuid;
    @JsonProperty("description")
    String description;
    @JsonProperty("state")
    String state;
    @JsonProperty("message")
    String message;
    @JsonProperty("code")
    String code;
    @JsonProperty("_links")
    private Links links;

    @JsonProperty("error")
    private JobError error;
    public JobError getError () { return error; }
    public void setError (JobError error) { this.error = error; }
    public Links getLinks() { return links; }
    public void setLinks(Links links) { this.links = links; }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    @Override
    public String toString() {
        return "JobDTO [uuid=" + uuid + ", description=" + description + ", state=" + state + ", message="
                + message + ", code=" + code + "]";
    }

    public static class Links {
        @JsonProperty("message")
        private Self self;
        public Self getSelf() { return self; }
        public void setSelf(Self self) { this.self = self; }
    }

    public static class Self {
        @JsonProperty("message")
        private String href;
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
    }

    public static class JobError {
        @JsonProperty("message")
        String errorMesssage;
        @JsonProperty("code")
        String code;
        public String getErrorMesssage () { return errorMesssage; }
        public void setErrorMesssage (String errorMesssage) { this.errorMesssage = errorMesssage; }
        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }
        @Override
        public String toString() {
            return "JobError [errorMesssage=" + errorMesssage + ", code=" + code + "]";
        }
    }
}
