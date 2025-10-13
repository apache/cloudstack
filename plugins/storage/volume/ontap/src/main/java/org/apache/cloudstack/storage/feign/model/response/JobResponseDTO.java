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
package org.apache.cloudstack.storage.feign.model.response;

public class JobResponseDTO {
    private Job job;
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public static class Job {
        private String uuid;
        private Links links;
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public Links getLinks() { return links; }
        public void setLinks(Links links) { this.links = links; }
    }

    public static class Links {
        private Self self;
        public Self getSelf() { return self; }
        public void setSelf(Self self) { this.self = self; }
    }

    public static class Self {
        private String href;
        public String getHref() { return href; }
        public void setHref(String href) { this.href = href; }
    }
}
