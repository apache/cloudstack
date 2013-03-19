// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.service;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cloudstack.entity.cloud.InstanceGroupResource;
import org.apache.cloudstack.entity.cloud.VirtualMachineResource;
import org.springframework.stereotype.Service;

/**
 * Instance Group rest service
 */
@Service("instanceGroupService") // Use Spring IoC to create and manage this bean.
public class InstanceGroupServiceImpl implements InstanceGroupService {

    /* (non-Javadoc)
     * @see org.apache.cloudstack.service.InstanceGroupService#create(org.apache.cloudstack.entity.cloud.InstanceGroupResource)
     */
    @Override
    @POST
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public InstanceGroupResource create(InstanceGroupResource instGrp) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.service.InstanceGroupService#delete(java.lang.String)
     */
    @Override
    @DELETE
    @Path("{uuid}")
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public void delete(@PathParam("uuid") String uuid) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.service.InstanceGroupService#update(java.lang.String, org.apache.cloudstack.entity.cloud.InstanceGroupResource)
     */
    @Override
    @PUT
    @Path("{uuid}")
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public void update(@PathParam("uuid") String uuid, InstanceGroupResource instGrp) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.service.InstanceGroupService#get(java.lang.String)
     */
    @Override
    @GET
    @Path("{uuid}")
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public InstanceGroupResource get(@PathParam("uuid") String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.service.InstanceGroupService#listAll()
     */
    @Override
    @GET
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public List<InstanceGroupResource> listAll() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.service.InstanceGroupService#listByName(java.lang.String)
     */
    @Override
    @GET
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public List<InstanceGroupResource> listByName(@QueryParam("name") String grpName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @GET
    @Path("{uuid}/vms")
    @Consumes({ "application/xml", "application/json" })
    @Produces({ "application/xml", "application/json" })
    public List<VirtualMachineResource> listVms(@PathParam("uuid") String uuid) {
        // TODO Auto-generated method stub
        return null;
    }

}
