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

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cloudstack.engine.datacenter.entity.api.PodEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;
import org.apache.cloudstack.engine.rest.datacenter.entity.api.PodRestTO;
import org.apache.cloudstack.engine.rest.datacenter.entity.api.ZoneRestTO;
import org.apache.cloudstack.engine.service.api.ProvisioningService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Service("provisioningService")
@Path("/provisioning")
@Produces({"application/xml", "application/json"})
@Component
public class ProvisioningRestService {
    @Inject
    ProvisioningService _provisioningService;

    @GET
    @Path("/{zoneid}")
    public ZoneRestTO getZone(@Context UriInfo ui, @PathParam("zoneid") String id) {
        UriBuilder ub = ui.getAbsolutePathBuilder().path(this.getClass(), "getZone");
        ZoneEntity entity = _provisioningService.getZone(id);
        return new ZoneRestTO(ui, entity, ub.build(entity.getUuid()));
    }

    @GET
    @Path("/zones")
    public ZoneRestTO[] listZones(@Context UriInfo ui) {
        List<ZoneEntity> zones = _provisioningService.listZones();
        ZoneRestTO[] tos = new ZoneRestTO[zones.size()];
        UriBuilder ub = ui.getAbsolutePathBuilder().path(this.getClass(), "getZone");
        Iterator<ZoneEntity> it = zones.iterator();
        for (int i = 0; i < tos.length; i++) {
            ZoneEntity entity = it.next();
            tos[i] = new ZoneRestTO(ui, entity, ub.build(entity.getUuid()));
        }
        return tos;
    }

    @GET
    @Path("/zone/{zoneid}/pods") 
    public PodRestTO[] listPods(@PathParam("zoneid") String zoneId) {
        List<PodEntity> pods = _provisioningService.listPods();
        PodRestTO[] tos = new PodRestTO[pods.size()];
        Iterator<PodEntity> it = pods.iterator();
        for (int i = 0; i < tos.length; i++) {
            PodEntity pod = it.next();
            tos[i] = new PodRestTO(pod);
        }
        return tos;
    }
}
