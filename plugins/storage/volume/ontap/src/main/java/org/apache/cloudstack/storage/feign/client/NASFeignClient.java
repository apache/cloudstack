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

import feign.QueryMap;
import org.apache.cloudstack.storage.feign.model.ExportPolicy;
import org.apache.cloudstack.storage.feign.model.FileInfo;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.Map;

public interface NASFeignClient {

    // File Operations
    @RequestLine("GET /api/storage/volumes/{volumeUuid}/files/{path}")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<FileInfo> getFileResponse(@Param("authHeader") String authHeader,
                                            @Param("volumeUuid") String volumeUUID,
                                            @Param("path") String filePath);

    @RequestLine("DELETE /api/storage/volumes/{volumeUuid}/files/{path}")
    @Headers({"Authorization: {authHeader}"})
    void deleteFile(@Param("authHeader") String authHeader,
                    @Param("volumeUuid") String volumeUUID,
                    @Param("path") String filePath);

    @RequestLine("PATCH /api/storage/volumes/{volumeUuid}/files/{path}")
    @Headers({"Authorization: {authHeader}"})
    void updateFile(@Param("authHeader") String authHeader,
                    @Param("volumeUuid") String volumeUUID,
                    @Param("path") String filePath,
                    FileInfo fileInfo);

    @RequestLine("POST /api/storage/volumes/{volumeUuid}/files/{path}")
    @Headers({"Authorization: {authHeader}"})
    void createFile(@Param("authHeader") String authHeader,
                    @Param("volumeUuid") String volumeUUID,
                    @Param("path") String filePath,
                    FileInfo file);

    // Export Policy Operations
    @RequestLine("POST /api/protocols/nfs/export-policies")
    @Headers({"Authorization: {authHeader}"})
    void createExportPolicy(@Param("authHeader") String authHeader,
                                    ExportPolicy exportPolicy);

    @RequestLine("GET /api/protocols/nfs/export-policies")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<ExportPolicy> getExportPolicyResponse(@Param("authHeader") String authHeader, @QueryMap Map<String, Object> queryMap);

    @RequestLine("GET /api/protocols/nfs/export-policies/{id}")
    @Headers({"Authorization: {authHeader}"})
    ExportPolicy getExportPolicyById(@Param("authHeader") String authHeader,
                                                    @Param("id") String id);

    @RequestLine("DELETE /api/protocols/nfs/export-policies/{id}")
    @Headers({"Authorization: {authHeader}"})
    void deleteExportPolicyById(@Param("authHeader") String authHeader,
                                @Param("id") String id);

    @RequestLine("PATCH /api/protocols/nfs/export-policies/{id}")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<ExportPolicy> updateExportPolicy(@Param("authHeader") String authHeader,
                                                   @Param("id") String id,
                                                   ExportPolicy request);
}
