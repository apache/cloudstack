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

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;

@Produces("application/json")
public class VolumeRestService {

    @PUT
    @Path("/vol/create")
    public VolumeEntity create(@QueryParam("xid") String xid, @QueryParam("display-name") String displayName) {
        return null;
    }

    @POST
    @Path("/vol/{volid}/deploy")
    public String deploy(@PathParam("volid") String volumeId) {
        return null;
    }

    @GET
    @Path("/vols")
    public List<VolumeEntity> listAll() {
        return null;
    }

    @POST
    @Path("/vol/{volid}/attach-to")
    public String attachTo(@PathParam("volid") String volumeId, @QueryParam("vmid") String vmId, @QueryParam("device-order") short device) {
        return null;
    }

    @DELETE
    @Path("/vol/{volid}")
    public String delete(@PathParam("volid") String volumeId) {
        return null;
    }

    @POST
    @Path("/vol/{volid}/detach")
    public String detach(@QueryParam("volid") String volumeId) {
        return null;
    }

}
