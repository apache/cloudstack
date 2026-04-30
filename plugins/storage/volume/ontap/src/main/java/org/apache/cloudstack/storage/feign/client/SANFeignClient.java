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
import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.IscsiService;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunMap;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.Map;

public interface SANFeignClient {
    // iSCSI Service APIs
    @RequestLine("GET /api/protocols/san/iscsi/services")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<IscsiService> getIscsiServices(@Param("authHeader") String authHeader, @QueryMap Map<String, Object> queryMap);

    // LUN Operation APIs
    @RequestLine("POST /api/storage/luns?return_records={returnRecords}")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<Lun> createLun(@Param("authHeader") String authHeader, @Param("returnRecords") boolean returnRecords, Lun lun);

    @RequestLine("GET /api/storage/luns")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<Lun> getLunResponse(@Param("authHeader") String authHeader, @QueryMap Map<String, Object> queryMap);

    @RequestLine("GET /{uuid}")
    @Headers({"Authorization: {authHeader}"})
    Lun getLunByUUID(@Param("authHeader") String authHeader, @Param("uuid") String uuid);

    @RequestLine("PATCH /{uuid}")
    @Headers({"Authorization: {authHeader}"})
    void updateLun(@Param("authHeader") String authHeader, @Param("uuid") String uuid, Lun lun);

    @RequestLine("DELETE /api/storage/luns/{uuid}")
    @Headers({"Authorization: {authHeader}"})
    void deleteLun(@Param("authHeader") String authHeader, @Param("uuid") String uuid, @QueryMap Map<String, Object> queryMap);

    // iGroup Operation APIs
    @RequestLine("POST /api/protocols/san/igroups?return_records={returnRecords}")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<Igroup> createIgroup(@Param("authHeader") String authHeader, @Param("returnRecords") boolean returnRecords, Igroup igroupRequest);

    @RequestLine("GET /api/protocols/san/igroups")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<Igroup> getIgroupResponse(@Param("authHeader") String authHeader, @QueryMap Map<String, Object> queryMap);

    @RequestLine("GET /{uuid}")
    @Headers({"Authorization: {authHeader}"})
    Igroup getIgroupByUUID(@Param("authHeader") String authHeader, @Param("uuid") String uuid);

    @RequestLine("DELETE /api/protocols/san/igroups/{uuid}")
    @Headers({"Authorization: {authHeader}"})
    void deleteIgroup(@Param("authHeader") String authHeader, @Param("uuid") String uuid);

    // LUN Maps Operation APIs
    @RequestLine("POST /api/protocols/san/lun-maps")
    @Headers({"Authorization: {authHeader}", "return_records: {returnRecords}"})
    OntapResponse<LunMap> createLunMap(@Param("authHeader") String authHeader, @Param("returnRecords") boolean returnRecords, LunMap lunMap);


    @RequestLine("GET /api/protocols/san/lun-maps")
    @Headers({"Authorization: {authHeader}"})
    OntapResponse<LunMap> getLunMapResponse(@Param("authHeader") String authHeader,  @QueryMap Map<String, Object> queryMap);

    @RequestLine("DELETE /api/protocols/san/lun-maps/{lunUuid}/{igroupUuid}")
    @Headers({"Authorization: {authHeader}"})
    void deleteLunMap(@Param("authHeader") String authHeader,
                      @Param("lunUuid") String lunUUID,
                      @Param("igroupUuid") String igroupUUID);
}
