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
package org.apache.cloudstack.schedule;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ResourceScheduleManager {

    ResourceScheduleResponse createSchedule(ApiCommandResourceType resourceType, String resourceUuid,
                                            String description, String schedule, String timeZone, String action,
                                            Date startDate, Date endDate, boolean enabled, Map<String, String> details);

    ResourceScheduleResponse updateSchedule(Long id, String description, String schedule, String timeZone,
                                            Date startDate, Date endDate, Boolean enabled, Map<String, String> details);

    ListResponse<ResourceScheduleResponse> listSchedule(Long id, List<Long> ids, ApiCommandResourceType resourceType,
                                                        String resourceUuid, String action, Boolean enabled,
                                                        Long startIndex, Long pageSize);

    Long removeSchedule(ApiCommandResourceType resourceType, String resourceUuid, Long id, List<Long> ids);

    void removeSchedulesForResource(ApiCommandResourceType resourceType, long resourceId);
}
