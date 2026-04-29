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
package org.apache.cloudstack.engine.rest.service.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;
import org.apache.cloudstack.engine.service.api.ProvisioningService;

@Produces({"application/json"})
public class ZoneRestService {
//    @Inject
    ProvisioningService _provisioningService;

    @GET
    @Path("/zones")
    public List<ZoneEntity> listAll() {
        return _provisioningService.listZones();
    }

    @GET
    @Path("/zone/{zone-id}")
    public ZoneEntity get(@PathParam("zone-id") String zoneId) {
        return _provisioningService.getZone(zoneId);
    }

    @POST
    @Path("/zone/{zone-id}/enable")
    public String enable(String zoneId) {
        return null;
    }

    @POST
    @Path("/zone/{zone-id}/disable")
    public String disable(@PathParam("zone-id") String zoneId) {
        ZoneEntity zoneEntity = _provisioningService.getZone(zoneId);
        zoneEntity.disable();
        return null;
    }

    @POST
    @Path("/zone/{zone-id}/deactivate")
    public String deactivate(@PathParam("zone-id") String zoneId) {
        return null;
    }

    @POST
    @Path("/zone/{zone-id}/activate")
    public String reactivate(@PathParam("zone-id") String zoneId) {
        return null;
    }

    @PUT
    @Path("/zone/create")
    public ZoneEntity createZone(@QueryParam("xid") String xid, @QueryParam("display-name") String displayName) {
        return null;
    }

    @DELETE
    @Path("/zone/{zone-id}")
    public String deleteZone(@QueryParam("zone-id") String xid) {
        return null;
    }
}
