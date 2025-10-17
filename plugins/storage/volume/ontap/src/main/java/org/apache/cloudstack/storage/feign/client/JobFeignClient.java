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
package org.apache.cloudstack.storage.feign.client;

import org.apache.cloudstack.storage.feign.FeignConfiguration;
import org.apache.cloudstack.storage.feign.model.Job;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.net.URI;

/**
 * @author Administrator
 *
 */
@Lazy
@FeignClient(name = "JobClient", url = "https://{clusterIP}/api/cluster/jobs" , configuration = FeignConfiguration.class)
public interface JobFeignClient {

    @RequestMapping(method = RequestMethod.GET, value="/{uuid}")
    Job getJobByUUID(URI baseURL, @RequestHeader("Authorization") String header, @PathVariable(name = "uuid", required = true) String uuid);

}
