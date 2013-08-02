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
package org.apache.cloudstack.engine.service.api;

import java.net.URL;
import java.util.List;

import com.cloud.alert.Alert;

public interface OperationsServices {
//    List<AsyncJob> listJobs();
//
//    List<AsyncJob> listJobsInProgress();
//
//    List<AsyncJob> listJobsCompleted();
//
//    List<AsyncJob> listJobsCompleted(Long from);
//
//    List<AsyncJob> listJobsInWaiting();

    void cancelJob(String job);

    List<Alert> listAlerts();

    Alert getAlert(String uuid);

    void cancelAlert(String alert);

    void registerForAlerts();

    String registerForEventNotifications(String type, String topic, URL url);

    boolean deregisterForEventNotifications(String notificationId);

    /**
     * @return the list of event topics someone can register for
     */
    List<String> listEventTopics();

}
