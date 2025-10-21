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
import org.apache.cloudstack.storage.feign.model.Volume;
import org.apache.cloudstack.storage.feign.model.response.JobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;

import java.net.URI;


@Lazy
@FeignClient(name = "VolumeClient", url = "https://{clusterIP}/api/storage/volumes", configuration = FeignConfiguration.class)
public interface VolumeFeignClient {

    @RequestMapping(method = RequestMethod.DELETE, value="/{uuid}")
    void deleteVolume(URI baseURL, @RequestHeader("Authorization") String authHeader, @PathVariable("uuid") String uuid);

    @RequestMapping(method = RequestMethod.POST)
    JobResponse createVolumeWithJob(URI baseURL, @RequestHeader("Authorization") String authHeader, @RequestBody Volume volumeRequest);

    @RequestMapping(method = RequestMethod.GET, value="/{uuid}")
    Volume getVolumeByUUID(URI baseURL, @RequestHeader("Authorization") String authHeader, @PathVariable("uuid") String uuid);

    @RequestMapping(method = RequestMethod.PATCH)
    JobResponse updateVolumeRebalancing(URI baseURL, @RequestHeader("accept") String acceptHeader, @PathVariable("uuid") String uuid, @RequestBody Volume volumeRequest);

}
