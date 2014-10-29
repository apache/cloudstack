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
package org.apache.cloudstack.api;

public interface ResponseObject {
    /**
     * Get the name of the API response
     *
     * @return the name of the API response
     */
    String getResponseName();

    /**
     * Set the name of the API response
     *
     * @param name
     */
    void setResponseName(String name);

    /**
     * Get the name of the API object
     *
     * @return the name of the API object
     */
    String getObjectName();

    /**
     *
     * @param name
     */
    void setObjectName(String name);

    /**
     * Returns the object UUid
     */
    String getObjectId();

    /**
     * Returns the job id
     *
     * @return
     */
    String getJobId();

    /**
     * Sets the job id
     *
     * @param jobId
     */
    void setJobId(String jobId);

    /**
     * Returns the job status
     *
     * @return
     */
    Integer getJobStatus();

    /**
     *
     * @param jobStatus
     */
    void setJobStatus(Integer jobStatus);

    public enum ResponseView {
        Full,
        Restricted
    }
}
