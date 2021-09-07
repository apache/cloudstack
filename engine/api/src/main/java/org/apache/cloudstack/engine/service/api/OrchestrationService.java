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
package org.apache.cloudstack.engine.service.api;

import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cloudstack.engine.cloud.entity.api.NetworkEntity;
import org.apache.cloudstack.engine.cloud.entity.api.TemplateEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VirtualMachineEntity;
import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.offering.DiskOffering;
import com.cloud.vm.NicProfile;

@Path("orchestration")
@Produces({"application/json", "application/xml"})
public interface OrchestrationService {
    /**
     * creates a new virtual machine
     *
     * @param id externally unique name to reference the virtual machine
     * @param owner owner reference
     * @param template reference to the template
     * @param hostName name of the host
     * @param displayName name to look at
     * @param cpu # of cpu cores
     * @param speed speed of the cpu core in MHZ
     * @param memory memory to allocate in bytes
     * @param computeTags tags for the compute
     * @param rootDiskTags tags for the root disk
     * @param networkNicMap map networks to nic profiles that this VM should join
     * @param rootDiskSize size the root disk in case of templates.
     * @return VirtualMachineEntity
     */
    @POST
    @Path("/createvm")
    VirtualMachineEntity createVirtualMachine(@QueryParam("id") String id, @QueryParam("owner") String owner, @QueryParam("template-id") String templateId,
        @QueryParam("host-name") String hostName, @QueryParam("display-name") String displayName, @QueryParam("hypervisor") String hypervisor,
        @QueryParam("cpu") int cpu, @QueryParam("speed") int speed, @QueryParam("ram") long memory, @QueryParam("disk-size") Long diskSize,
        @QueryParam("compute-tags") List<String> computeTags, @QueryParam("root-disk-tags") List<String> rootDiskTags,
        @QueryParam("network-nic-map") Map<String, List<NicProfile>> networkNicMap, @QueryParam("deploymentplan") DeploymentPlan plan,
        @QueryParam("root-disk-size") Long rootDiskSize, @QueryParam("extra-dhcp-option-map") Map<String, Map<Integer, String>> extraDhcpOptionMap,
        @QueryParam("datadisktemplate-diskoffering-map") Map<Long, DiskOffering> datadiskTemplateToDiskOfferingMap) throws InsufficientCapacityException;

    @POST
    VirtualMachineEntity createVirtualMachineFromScratch(@QueryParam("id") String id, @QueryParam("owner") String owner, @QueryParam("iso-id") String isoId,
        @QueryParam("host-name") String hostName, @QueryParam("display-name") String displayName, @QueryParam("hypervisor") String hypervisor,
        @QueryParam("os") String os, @QueryParam("cpu") int cpu, @QueryParam("speed") int speed, @QueryParam("ram") long memory, @QueryParam("disk-size") Long diskSize,
        @QueryParam("compute-tags") List<String> computeTags, @QueryParam("root-disk-tags") List<String> rootDiskTags,
        @QueryParam("network-nic-map") Map<String, List<NicProfile>> networkNicMap, @QueryParam("deploymentplan") DeploymentPlan plan, @QueryParam("extra-dhcp-option-map") Map<String,  Map<Integer, String>> extraDhcpOptionMap) throws InsufficientCapacityException;

    @POST
    NetworkEntity createNetwork(String id, String name, String domainName, String cidr, String gateway);

    @DELETE
    void destroyNetwork(String networkUuid);

    @POST
    VolumeEntity createVolume();

    @DELETE
    void destroyVolume(String volumeEntity);

    @POST
    TemplateEntity registerTemplate(String name, URL path, String os, Hypervisor hypervisor);

    VirtualMachineEntity getVirtualMachine(@QueryParam("id") String id);
}
