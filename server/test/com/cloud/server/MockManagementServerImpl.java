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
package com.cloud.server;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.alert.Alert;
import com.cloud.api.commands.CreateSSHKeyPairCmd;
import com.cloud.api.commands.DeleteSSHKeyPairCmd;
import com.cloud.api.commands.DestroySystemVmCmd;
import com.cloud.api.commands.ExtractVolumeCmd;
import com.cloud.api.commands.GetVMPasswordCmd;
import com.cloud.api.commands.ListAlertsCmd;
import com.cloud.api.commands.ListAsyncJobsCmd;
import com.cloud.api.commands.ListCapabilitiesCmd;
import com.cloud.api.commands.ListCapacityCmd;
import com.cloud.api.commands.ListCfgsByCmd;
import com.cloud.api.commands.ListClustersCmd;
import com.cloud.api.commands.ListDiskOfferingsCmd;
import com.cloud.api.commands.ListEventsCmd;
import com.cloud.api.commands.ListGuestOsCategoriesCmd;
import com.cloud.api.commands.ListGuestOsCmd;
import com.cloud.api.commands.ListHostsCmd;
import com.cloud.api.commands.ListIsosCmd;
import com.cloud.api.commands.ListPodsByCmd;
import com.cloud.api.commands.ListPublicIpAddressesCmd;
import com.cloud.api.commands.ListRoutersCmd;
import com.cloud.api.commands.ListSSHKeyPairsCmd;
import com.cloud.api.commands.ListServiceOfferingsCmd;
import com.cloud.api.commands.ListStoragePoolsCmd;
import com.cloud.api.commands.ListSystemVMsCmd;
import com.cloud.api.commands.ListTemplatesCmd;
import com.cloud.api.commands.ListVMGroupsCmd;
import com.cloud.api.commands.ListVlanIpRangesCmd;
import com.cloud.api.commands.ListZonesByCmd;
import com.cloud.api.commands.RebootSystemVmCmd;
import com.cloud.api.commands.RegisterSSHKeyPairCmd;
import com.cloud.api.commands.StopSystemVmCmd;
import com.cloud.api.commands.UpdateHostPasswordCmd;
import com.cloud.api.commands.UpdateIsoCmd;
import com.cloud.api.commands.UpdateTemplateCmd;
import com.cloud.api.commands.UpdateVMGroupCmd;
import com.cloud.api.commands.UpgradeSystemVMCmd;
import com.cloud.api.commands.UploadCustomCertificateCmd;
import com.cloud.async.AsyncJob;
import com.cloud.capacity.Capacity;
import com.cloud.configuration.Configuration;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.event.Event;
import com.cloud.event.EventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilities;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.network.IpAddress;
import com.cloud.network.router.VirtualRouter;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.SSHKeyPair;
import com.cloud.utils.Pair;
import com.cloud.vm.InstanceGroup;
import com.cloud.vm.VirtualMachine;

public class MockManagementServerImpl implements ManagementServer {

	@Override
	public List<? extends DataCenter> listDataCenters(ListZonesByCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Configuration> searchForConfigurations(ListCfgsByCmd c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends ServiceOffering> searchForServiceOfferings(
			ListServiceOfferingsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Cluster> searchForClusters(ListClustersCmd c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Cluster> searchForClusters(long zoneId,
			Long startIndex, Long pageSizeVal, String hypervisorType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Pod> searchForPods(ListPodsByCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Host> searchForServers(ListHostsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachineTemplate updateTemplate(UpdateIsoCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachineTemplate updateTemplate(UpdateTemplateCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Event> searchForEvents(ListEventsCmd c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends VirtualRouter> searchForRouters(ListRoutersCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends IpAddress> searchForIPAddresses(
			ListPublicIpAddressesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends GuestOS> listGuestOSByCriteria(ListGuestOsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends GuestOsCategory> listGuestOSCategoriesByCriteria(
			ListGuestOsCategoriesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachine stopSystemVM(StopSystemVmCmd cmd)
			throws ResourceUnavailableException, ConcurrentOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachine startSystemVM(long vmId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachine rebootSystemVM(RebootSystemVmCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachine destroySystemVM(DestroySystemVmCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualMachine upgradeSystemVM(UpgradeSystemVMCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Alert> searchForAlerts(ListAlertsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Capacity> listCapacities(ListCapacityCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Pair<Long, Long>> listIsos(ListIsosCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Pair<Long, Long>> listTemplates(ListTemplatesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends DiskOffering> searchForDiskOfferings(
			ListDiskOfferingsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends StoragePool> searchForStoragePools(
			ListStoragePoolsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends VirtualMachine> searchForSystemVm(ListSystemVMsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getCloudIdentifierResponse(long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateHostPassword(UpdateHostPasswordCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InstanceGroup updateVmGroup(UpdateVMGroupCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends InstanceGroup> searchForVmGroups(ListVMGroupsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> listCapabilities(ListCapabilitiesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long extractVolume(ExtractVolumeCmd cmd) throws URISyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getHypervisors(Long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String uploadCertificate(UploadCustomCertificateCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Vlan> searchForVlans(ListVlanIpRangesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends AsyncJob> searchForAsyncJobs(ListAsyncJobsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String generateRandomPassword() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long saveStartedEvent(Long userId, Long accountId, String type,
			String description, long startEventId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long saveCompletedEvent(Long userId, Long accountId, String level,
			String type, String description, long startEventId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends SSHKeyPair> listSSHKeyPairs(ListSSHKeyPairsCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SSHKeyPair registerSSHKeyPair(RegisterSSHKeyPairCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SSHKeyPair createSSHKeyPair(CreateSSHKeyPairCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteSSHKeyPair(DeleteSSHKeyPairCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getVMPassword(GetVMPasswordCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public com.cloud.vm.VirtualMachine.Type findSystemVMTypeById(long instanceId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<List<? extends Host>, List<? extends Host>> listHostsForMigrationOfVM(
			Long vmId, Long startIndex, Long pageSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] listEventTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends HypervisorCapabilities> listHypervisorCapabilities(
			Long id, HypervisorType hypervisorType, String keyword,
			Long startIndex, Long pageSizeVal) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorCapabilities updateHypervisorCapabilities(Long id,
			Long maxGuestsLimit, Boolean securityGroupEnabled) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Capacity> listTopConsumedResources(ListCapacityCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getApiConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HostVO getHostBy(long hostId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<EventVO> getEvents(long userId, long accountId, Long domainId,
			String type, String level, Date startDate, Date endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsoleProxyInfo getConsoleProxyForVm(long dataCenterId,
			long userVmId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConsoleAccessUrlRoot(long vmId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GuestOSVO getGuestOs(Long guestOsId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<String, Integer> getVncPort(VirtualMachine vm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getMemoryOrCpuCapacityByHost(Long hostId, short capacityType) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<? extends StoragePoolVO> searchForStoragePools(Criteria c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHashKey() {
		// TODO Auto-generated method stub
		return null;
	}
}
