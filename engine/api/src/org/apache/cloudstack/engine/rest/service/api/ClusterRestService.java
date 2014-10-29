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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cloudstack.engine.datacenter.entity.api.ClusterEntity;
import org.apache.cloudstack.engine.service.api.ProvisioningService;

@Produces("application/json")
public class ClusterRestService {
//    @Inject
    ProvisioningService _provisioningService;

    @GET
    @Path("/clusters")
    public List<ClusterEntity> listAll() {
        return null;
    }

    @GET
    @Path("/cluster/{clusterid}")
    public ClusterEntity get(@PathParam("clusterid") String clusterId) {
        return null;
    }

    @POST
    @Path("/cluster/{clusterid}/enable")
    public String enable(@PathParam("clusterid") String clusterId) {
        return null;
    }

    @POST
    @Path("/cluster/{clusterid}/disable")
    public String disable(@PathParam("clusterid") String clusterId) {
        return null;
    }

    @POST
    @Path("/cluster/{clusterid}/deactivate")
    public String deactivate(@PathParam("clusterid") String clusterId) {
        return null;
    }

    @POST
    @Path("/cluster/{clusterid}/reactivate")
    public String reactivate(@PathParam("clusterid") String clusterId) {
        return null;
    }

    @PUT
    @Path("/cluster/create")
    public ClusterEntity create(@QueryParam("xid") String xid, @QueryParam("display-name") String displayName) {
        return null;
    }

    @PUT
    @Path("/cluster/{clusterid}/update")
    public ClusterEntity update(@QueryParam("display-name") String displayName) {
        return null;
    }
}
