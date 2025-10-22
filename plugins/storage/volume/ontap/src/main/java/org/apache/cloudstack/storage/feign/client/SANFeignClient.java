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

import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.FeignConfiguration;
import org.apache.cloudstack.storage.feign.model.LunMap;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

@Lazy
@FeignClient(name = "SANClient", url = "", configuration = FeignConfiguration.class )
public interface SANFeignClient {

    //Lun Operation APIs
    @RequestMapping(method = RequestMethod.POST)
    OntapResponse<Lun> createLun(URI baseURL, @RequestHeader("Authorization") String authHeader, @RequestHeader("return_records") boolean value,
                                 @RequestBody Lun lun);

    //this method to get all luns and also filtered luns based on query params as a part of URL
    @RequestMapping(method = RequestMethod.GET)
    OntapResponse<Lun> getLunResponse(URI baseURL, @RequestHeader("Authorization") String authHeader);

    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
    Lun getLunByUUID(URI baseURL, @RequestHeader("Authorization") String authHeader, @PathVariable(name="uuid", required=true) String uuid);

    @RequestMapping(method = RequestMethod.PATCH, value = "/{uuid}")
    void updateLun(URI uri, @RequestHeader("Authorization") String authHeader, @PathVariable(name="uuid", required=true) String uuid,
                     @RequestBody Lun lun);

    @RequestMapping(method = RequestMethod.DELETE, value = "/{uuid}")
    void deleteLun(URI baseURL, @RequestHeader("Authorization") String authHeader, @PathVariable(name="uuid", required=true) String uuid);


    //iGroup Operation APIs

    @RequestMapping(method = RequestMethod.POST)
    OntapResponse<Igroup> createIgroup(URI uri, @RequestHeader("Authorization") String header, @RequestHeader("return_records") boolean value,
                               @RequestBody Igroup igroupRequest);

    //this method to get all igroups and also filtered igroups based on query params as a part of URL
    @RequestMapping(method = RequestMethod.GET)
    OntapResponse<Igroup> getIgroupResponse(URI baseURL, @RequestHeader("Authorization") String header, @PathVariable(name = "uuid", required = true) String uuid);
    @RequestMapping(method = RequestMethod.GET, value = "/{uuid}")
    Igroup getIgroupByUUID(URI baseURL, @RequestHeader("Authorization") String header, @PathVariable(name = "uuid", required = true) String uuid);
    @RequestMapping(method = RequestMethod.DELETE, value = "/{uuid}")
    void deleteIgroup(URI baseUri, @RequestHeader("Authorization") String authHeader, @PathVariable(name = "uuid", required = true) String uuid);

    @RequestMapping(method = RequestMethod.POST, value = "/{uuid}/igroups")
    OntapResponse<Igroup> addNestedIgroups(URI uri, @RequestHeader("Authorization") String header, @PathVariable(name = "uuid", required = true) String uuid,
                                                 @RequestBody Igroup igroupNestedRequest, @RequestHeader(value="return_records", defaultValue = "true") boolean value);


    //Lun Maps Operation APIs

    @RequestMapping(method = RequestMethod.POST)
    OntapResponse<LunMap> createLunMap(URI baseURL, @RequestHeader("Authorization") String authHeader, @RequestBody LunMap lunMap);

    @RequestMapping(method = RequestMethod.GET)
    OntapResponse<LunMap> getLunMapResponse(URI baseURL, @RequestHeader("Authorization") String authHeader);

    @RequestMapping(method = RequestMethod.GET, value = "/{lun.uuid}/{igroup.uuid}")
    void deleteLunMap(URI baseURL, @RequestHeader("Authorization") String authHeader, @PathVariable(name="lun.uuid", required=true) String uuid,
                        @PathVariable(name="igroup.uuid", required=true) String igroupUUID);

}
