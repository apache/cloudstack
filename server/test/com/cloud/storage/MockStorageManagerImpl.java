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
package com.cloud.storage;

import java.math.BigDecimal;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CleanupSnapshotBackupCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DeleteVolumeCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.CancelPrimaryStorageMaintenanceCmd;
import com.cloud.api.commands.CreateStoragePoolCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeletePoolCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.api.commands.UpdateStoragePoolCmd;
import com.cloud.api.commands.UploadVolumeCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.Capacity;
import com.cloud.capacity.CapacityManager;
import com.cloud.capacity.CapacityState;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.cluster.CheckPointManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.NetworkManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Grouping;
import com.cloud.org.Grouping.AllocationState;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.server.ManagementServer;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.server.StatsCollector;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.allocator.StoragePoolAllocator;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.StoragePoolWorkDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VMTemplateSwiftDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeHostDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.listener.StoragePoolMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EnumUtils;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { StorageManager.class, StorageService.class })
public class MockStorageManagerImpl implements StorageManager, Manager, ClusterManagerListener {

	@Override
	public StoragePool createPool(CreateStoragePoolCmd cmd)
			throws ResourceInUseException, IllegalArgumentException,
			UnknownHostException, ResourceUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Volume allocVolume(CreateVolumeCmd cmd)
			throws ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Volume createVolume(CreateVolumeCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteVolume(long volumeId)
			throws ConcurrentOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deletePool(DeletePoolCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public StoragePool preparePrimaryStorageForMaintenance(Long primaryStorageId)
			throws ResourceUnavailableException, InsufficientCapacityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoragePool cancelPrimaryStorageForMaintenance(
			CancelPrimaryStorageMaintenanceCmd cmd)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoragePool updateStoragePool(UpdateStoragePoolCmd cmd)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoragePool getStoragePool(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Volume migrateVolume(Long volumeId, Long storagePoolId)
			throws ConcurrentOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Volume> searchForVolumes(ListVolumesCmd cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Volume uploadVolume(UploadVolumeCmd cmd)
			throws ResourceAllocationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList,
			long selfNodeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList,
			long selfNodeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onManagementNodeIsolated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canVmRestartOnAnotherServer(long vmId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Pair<String, String> getAbsoluteIsoPath(long templateId,
			long dataCenterId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSecondaryStorageURL(long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStoragePoolTags(long poolId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HostVO getSecondaryStorageHost(long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VMTemplateHostVO findVmTemplateHost(long templateId, StoragePool pool) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VolumeVO moveVolume(VolumeVO volume, long destPoolDcId,
			Long destPoolPodId, Long destPoolClusterId,
			HypervisorType dataDiskHyperType)
			throws ConcurrentOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VolumeVO createVolume(VolumeVO volume, VMInstanceVO vm,
			VMTemplateVO template, DataCenterVO dc, HostPodVO pod,
			Long clusterId, ServiceOfferingVO offering,
			DiskOfferingVO diskOffering, List<StoragePoolVO> avoids, long size,
			HypervisorType hyperType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean destroyVolume(VolumeVO volume)
			throws ConcurrentOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createCapacityEntry(StoragePoolVO storagePool) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean volumeOnSharedStoragePool(VolumeVO volume) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Answer sendToPool(long poolId, Command cmd)
			throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer sendToPool(StoragePool pool, Command cmd)
			throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer[] sendToPool(long poolId, Commands cmd)
			throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Answer[] sendToPool(StoragePool pool, Commands cmds)
			throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<Long, Answer[]> sendToPool(StoragePool pool,
			long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Commands cmds)
			throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<Long, Answer> sendToPool(StoragePool pool,
			long[] hostIdsToTryFirst, List<Long> hostIdsToAvoid, Command cmd)
			throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean volumeInactive(VolumeVO volume) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getVmNameOnVolume(VolumeVO volume) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLocalStorageActiveOnHost(Host host) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cleanupStorage(boolean recurring) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPrimaryStorageNameLabel(VolumeVO volume) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends VMInstanceVO> DiskProfile allocateRawVolume(Type type,
			String name, DiskOfferingVO offering, Long size, T vm, Account owner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends VMInstanceVO> DiskProfile allocateTemplatedVolume(
			Type type, String name, DiskOfferingVO offering,
			VMTemplateVO template, T vm, Account owner) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createCapacityEntry(StoragePoolVO storagePool,
			short capacityType, long allocated) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void prepare(VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest) throws StorageUnavailableException,
			InsufficientStorageCapacityException, ConcurrentOperationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void release(VirtualMachineProfile<? extends VMInstanceVO> profile) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cleanupVolumes(long vmId) throws ConcurrentOperationException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void prepareForMigration(
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Answer sendToPool(StoragePool pool, long[] hostIdsToTryFirst,
			Command cmd) throws StorageUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CapacityVO getSecondaryStorageUsedStats(Long hostId, Long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CapacityVO getStoragePoolUsedStats(Long poolId, Long clusterId,
			Long podId, Long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean createStoragePool(long hostId, StoragePoolVO pool) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean delPoolFromHost(long hostId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public HostVO getSecondaryStorageHost(long zoneId, long tmpltId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<HostVO> getSecondaryStorageHosts(long zoneId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<StoragePoolVO> ListByDataCenterHypervisor(long datacenterId,
			HypervisorType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<VMInstanceVO> listByStoragePool(long storagePoolId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StoragePoolVO findLocalStorageOnHost(long hostId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VMTemplateHostVO getTemplateHostRef(long zoneId, long tmpltId,
			boolean readyOnly) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean StorageMigration(
			VirtualMachineProfile<? extends VirtualMachine> vm,
			StoragePool destPool) throws ConcurrentOperationException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stateTransitTo(Volume vol, Event event)
			throws NoTransitionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VolumeVO allocateDuplicateVolume(VolumeVO oldVol, Long templateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Host updateSecondaryStorage(long secStorageId, String newUrl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Long> getUpHostsInPool(long poolId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cleanupSecondaryStorage(boolean recurring) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public VolumeVO copyVolumeFromSecToPrimary(VolumeVO volume,
			VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc,
			HostPodVO pod, Long clusterId, ServiceOfferingVO offering,
			DiskOfferingVO diskOffering, List<StoragePoolVO> avoids, long size,
			HypervisorType hyperType) throws NoTransitionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSupportedImageFormatForCluster(Long clusterId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorType getHypervisorTypeFromFormat(ImageFormat format) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean storagePoolHasEnoughSpace(List<Volume> volume,
			StoragePool pool) {
		// TODO Auto-generated method stub
		return false;
	}
}
