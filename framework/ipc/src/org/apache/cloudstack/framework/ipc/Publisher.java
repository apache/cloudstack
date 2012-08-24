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
package org.apache.cloudstack.framework.ipc;

import java.util.Map;

/**
 * Publish the Event 
 *
 */
public interface Publisher {
    /**
     * Publish a topic 
     *  
     * @param topic topic being published 
     * @param content  content published
     * @return true if the topic has been picked up; false if not.
     */
    boolean publish(String topic, Map<String, Object> content);

    /**
     * @return the name of this publisher
     */
    String getName();

}
