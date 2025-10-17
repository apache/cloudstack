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
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import java.net.URI;

/**
 * @author Administrator
 *
 */
@Lazy
@FeignClient(name = "NASClient", url = "" , configuration = FeignConfiguration.class)
public interface NASFeignClient {

    //File Operations

    @RequestMapping(method = RequestMethod.GET, value="/{volume.uuid}/files/{path}")
    OntapResponse<FileInfo> getFileResponse(URI uri, @RequestHeader("Authorization") String header, @PathVariable(name = "volume.uuid", required = true) String volumeUUID,
                                            @PathVariable(name = "path", required = true) String filePath);
    @RequestMapping(method = RequestMethod.DELETE, value="/{volume.uuid}/files/{path}")
    void deleteFile(URI uri, @RequestHeader("Authorization") String header, @PathVariable(name = "volume.uuid", required = true) String volumeUUID,
                    @PathVariable(name = "path", required = true) String filePath);
    @RequestMapping(method = RequestMethod.PATCH, value="/{volume.uuid}/files/{path}")
    void updateFile(URI uri, @RequestHeader("Authorization") String header, @PathVariable(name = "volume.uuid", required = true) String volumeUUID,
                    @PathVariable(name = "path", required = true) String filePath, @RequestBody FileInfo fileInfo);
    @RequestMapping(method = RequestMethod.POST, value="/{volume.uuid}/files/{path}")
    void createFile(URI uri, @RequestHeader("Authorization") String header, @PathVariable(name = "volume.uuid", required = true) String volumeUUID,
                    @PathVariable(name = "path", required = true) String filePath, @RequestBody FileInfo file);



    //Export Policy Operations

    @RequestMapping(method = RequestMethod.POST)
    ExportPolicy createExportPolicy(URI uri, @RequestHeader("Authorization") String header, @RequestHeader("return_records") boolean value,
                                    @RequestBody ExportPolicy exportPolicy);

    //this method to get all export policies and also filtered export policy based on query params as a part of URL
    @RequestMapping(method = RequestMethod.GET)
    OntapResponse<ExportPolicy> getExportPolicyResponse(URI baseURL, @RequestHeader("Authorization") String header);

    @RequestMapping(method = RequestMethod.GET, value="/{id}")
    OntapResponse<ExportPolicy> getExportPolicyById(URI baseURL, @RequestHeader("Authorization") String header, @PathVariable(name = "id", required = true) String id);

    @RequestMapping(method = RequestMethod.DELETE, value="/{id}")
    void deleteExportPolicyById(URI baseURL, @RequestHeader("Authorization") String header, @PathVariable(name = "id", required = true) String id);

    @RequestMapping(method = RequestMethod.PATCH, value="/{id}")
    OntapResponse<ExportPolicy> updateExportPolicy(URI baseURL, @RequestHeader("Authorization") String header, @PathVariable(name = "id", required = true) String id,
                                                   @RequestBody ExportPolicy request);
}
