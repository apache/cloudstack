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
package org.apache.cloudstack.engine.rest.datacenter.entity.api;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;
import org.apache.cloudstack.engine.service.api.ProvisioningService;
import org.springframework.stereotype.Service;

@Service("zoneService")
@Path("/zone/{zoneid}")
public class ZoneRestTO {
    @Inject
    protected static ProvisioningService s_provisioningService;


    public String id;
    public URI uri;
    public String name;
    public URI[] pods;

    public ZoneRestTO(UriInfo ui, ZoneEntity zone, URI uri) {
        this.id = zone.getUuid();
        this.name = zone.getName();
        this.uri = uri;
        List<String> podIds = zone.listPodIds();
        this.pods = new URI[podIds.size()];
        UriBuilder ub = ui.getAbsolutePathBuilder().path(this.getClass(), "getPod");
        Iterator<String> it = podIds.iterator();
        for (int i = 0; i < pods.length; i++) {
            String pod = it.next();
            pods[i] = ub.build(pod);
        }
    }

    public ZoneRestTO() {
    }

    @GET
    @Path("/pods")
    public URI[] listPods(@PathParam("zoneid") String zoneId) {
        return this.pods;
    }

    @GET
    @Path("/pod/{podid}")
    public PodRestTO getPod(@Context UriInfo ui, @PathParam("podid") String podId) {
        return null;
    }

}
