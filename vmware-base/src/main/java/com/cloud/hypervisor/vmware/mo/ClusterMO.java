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
package com.cloud.hypervisor.vmware.mo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.vim25.ArrayOfHostIpRouteEntry;
import com.vmware.vim25.ArrayUpdateOperation;
import com.vmware.vim25.ClusterComputeResourceSummary;
import com.vmware.vim25.ClusterConfigInfoEx;
import com.vmware.vim25.ClusterDasConfigInfo;
import com.vmware.vim25.ClusterDasVmSettingsRestartPriority;
import com.vmware.vim25.ClusterHostRecommendation;
import com.vmware.vim25.ComputeResourceSummary;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DasVmPriority;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestOsDescriptor;
import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostIpRouteEntry;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostSystemConnectionState;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.ClusterDasVmConfigInfo;
import com.vmware.vim25.ClusterDasVmConfigSpec;
import com.vmware.vim25.ClusterDasVmSettings;
import com.vmware.vim25.ClusterConfigSpecEx;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

//
// interface. This has changed as ClusterMO no longer works as a special host anymore. Need to refactor accordingly
//
public class ClusterMO extends BaseMO implements VmwareHypervisorHost {
    private static final Logger s_logger = Logger.getLogger(ClusterMO.class);
    private ManagedObjectReference _environmentBrowser = null;

    public ClusterMO(VmwareContext context, ManagedObjectReference morCluster) {
        super(context, morCluster);
    }

    public ClusterMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    @Override
    public String getHyperHostName() throws Exception {
        return getName();
    }

    @Override
    public ClusterDasConfigInfo getDasConfig() throws Exception {
        ClusterConfigInfoEx configInfo = getClusterConfigInfo();
        if (configInfo != null) {
            // Note getDynamicProperty() with "configurationEx.dasConfig" does not work here because of that dasConfig is a property in subclass
            return configInfo.getDasConfig();
        }

        return null;
    }

    public ClusterConfigInfoEx getClusterConfigInfo() throws Exception {
        ClusterConfigInfoEx configInfo = (ClusterConfigInfoEx)_context.getVimClient().getDynamicProperty(_mor, "configurationEx");
        return configInfo;
    }

    @Override
    public boolean isHAEnabled() throws Exception {
        ClusterDasConfigInfo dasConfig = getDasConfig();
        if (dasConfig != null && dasConfig.isEnabled() != null && dasConfig.isEnabled().booleanValue()) {
            return true;
        }

        return false;
    }

    private String getRestartPriorityForVM(VirtualMachineMO vmMo) throws Exception {
        if (vmMo == null) {
            s_logger.debug("Failed to get restart priority for VM, invalid VM object reference");
            return null;
        }

        ManagedObjectReference vmMor = vmMo.getMor();
        if (vmMor == null || !vmMor.getType().equals("VirtualMachine")) {
            s_logger.debug("Failed to get restart priority for VM: " + vmMo.getName() + ", invalid VM object reference");
            return null;
        }

        ClusterConfigInfoEx configInfo = getClusterConfigInfo();
        if (configInfo == null) {
            s_logger.debug("Failed to get restart priority for VM: " + vmMo.getName() + ", no cluster config information");
            return null;
        }

        List<ClusterDasVmConfigInfo> dasVmConfig = configInfo.getDasVmConfig();
        for (int dasVmConfigIndex = 0; dasVmConfigIndex < dasVmConfig.size(); dasVmConfigIndex++) {
            ClusterDasVmConfigInfo dasVmConfigInfo = dasVmConfig.get(dasVmConfigIndex);
            if (dasVmConfigInfo != null && dasVmConfigInfo.getKey().getValue().equals(vmMor.getValue())) {
                DasVmPriority dasVmPriority = dasVmConfigInfo.getRestartPriority();
                if (dasVmPriority != null) {
                    return dasVmPriority.value();
                } else {
                    //VM uses cluster restart priority when DasVmPriority for the VM is null.
                    return ClusterDasVmSettingsRestartPriority.CLUSTER_RESTART_PRIORITY.value();
                }
            }
        }

        s_logger.debug("VM: " + vmMo.getName() + " uses default restart priority in the cluster: " + getName());
        return null;
    }

    @Override
    public void setRestartPriorityForVM(VirtualMachineMO vmMo, String priority) throws Exception {
        if (vmMo == null || StringUtils.isBlank(priority)) {
            return;
        }

        if (!isHAEnabled()) {
            s_logger.debug("Couldn't set restart priority for VM: " + vmMo.getName() + ", HA disabled in the cluster");
            return;
        }

        ManagedObjectReference vmMor = vmMo.getMor();
        if (vmMor == null || !vmMor.getType().equals("VirtualMachine")) {
            s_logger.debug("Failed to set restart priority for VM: " + vmMo.getName() + ", invalid VM object reference");
            return;
        }

        String currentVmRestartPriority = getRestartPriorityForVM(vmMo);
        if (StringUtils.isNotBlank(currentVmRestartPriority) && currentVmRestartPriority.equalsIgnoreCase(priority)) {
            return;
        }

        ClusterDasVmSettings clusterDasVmSettings = new ClusterDasVmSettings();
        clusterDasVmSettings.setRestartPriority(priority);

        ClusterDasVmConfigInfo clusterDasVmConfigInfo = new ClusterDasVmConfigInfo();
        clusterDasVmConfigInfo.setKey(vmMor);
        clusterDasVmConfigInfo.setDasSettings(clusterDasVmSettings);

        ClusterDasVmConfigSpec clusterDasVmConfigSpec = new ClusterDasVmConfigSpec();
        clusterDasVmConfigSpec.setOperation((StringUtils.isNotBlank(currentVmRestartPriority)) ? ArrayUpdateOperation.EDIT : ArrayUpdateOperation.ADD);
        clusterDasVmConfigSpec.setInfo(clusterDasVmConfigInfo);

        ClusterConfigSpecEx clusterConfigSpecEx = new ClusterConfigSpecEx();
        ClusterDasConfigInfo clusterDasConfigInfo = new ClusterDasConfigInfo();
        clusterConfigSpecEx.setDasConfig(clusterDasConfigInfo);
        clusterConfigSpecEx.getDasVmConfigSpec().add(clusterDasVmConfigSpec);

        ManagedObjectReference morTask = _context.getService().reconfigureComputeResourceTask(_mor, clusterConfigSpecEx, true);

        boolean result = _context.getVimClient().waitForTask(morTask);

        if (result) {
            _context.waitForTaskProgressDone(morTask);

            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - setRestartPriority done(successfully)");
        } else {
            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - setRestartPriority done(failed)");
            s_logger.error("Set restart priority failed for VM: " + vmMo.getName() + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
    }

    @Override
    public ManagedObjectReference getHyperHostDatacenter() throws Exception {
        Pair<DatacenterMO, String> dcPair = DatacenterMO.getOwnerDatacenter(getContext(), getMor());
        assert (dcPair != null);
        return dcPair.first().getMor();
    }

    @Override
    public ManagedObjectReference getHyperHostOwnerResourcePool() throws Exception {
        return (ManagedObjectReference)_context.getVimClient().getDynamicProperty(getMor(), "resourcePool");
    }

    @Override
    public ManagedObjectReference getHyperHostCluster() throws Exception {
        return _mor;
    }

    @Override
    public synchronized List<VirtualMachineMO> listVmsOnHyperHostWithHypervisorName(String vmName) throws Exception {
        List<VirtualMachineMO> vms = new ArrayList<>();
        List<ManagedObjectReference> hosts = _context.getVimClient().getDynamicProperty(_mor, "host");
        if (CollectionUtils.isNotEmpty(hosts)) {
            for (ManagedObjectReference morHost : hosts) {
                HostMO hostMo = new HostMO(_context, morHost);
                vms.addAll(hostMo.listVmsOnHyperHostWithHypervisorName(vmName));
            }
        }
        return vms;
    }

    @Override
    public VirtualMachineMO findVmOnHyperHost(String name) throws Exception {

        int key = getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }

        String instanceNameCustomField = "value[" + key + "]";
        ObjectContent[] ocs = getVmPropertiesOnHyperHost(new String[] {"name", instanceNameCustomField});
        return HypervisorHostHelper.findVmFromObjectContent(_context, ocs, name, instanceNameCustomField);
    }

    @Override
    public VirtualMachineMO findVmOnPeerHyperHost(String name) throws Exception {
        int key = getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }

        String instanceNameCustomField = "value[" + key + "]";

        ObjectContent[] ocs = getVmPropertiesOnHyperHost(new String[] {"name", instanceNameCustomField});
        return HypervisorHostHelper.findVmFromObjectContent(_context, ocs, name, instanceNameCustomField);
    }

    @Override
    public ObjectContent[] getVmPropertiesOnHyperHost(String[] propertyPaths) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() for VM properties. target MOR: " + _mor.getValue() + ", properties: " + new Gson().toJson(propertyPaths));

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("VirtualMachine");
        pSpec.getPathSet().addAll(Arrays.asList(propertyPaths));

        TraversalSpec host2VmFolderTraversal = new TraversalSpec();
        host2VmFolderTraversal.setType("HostSystem");
        host2VmFolderTraversal.setPath("vm");
        host2VmFolderTraversal.setName("host2VmFolderTraversal");

        TraversalSpec cluster2HostFolderTraversal = new TraversalSpec();
        cluster2HostFolderTraversal.setType("ClusterComputeResource");
        cluster2HostFolderTraversal.setPath("host");
        cluster2HostFolderTraversal.setName("cluster2HostFolderTraversal");
        cluster2HostFolderTraversal.getSelectSet().add(host2VmFolderTraversal);

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(getMor());
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(cluster2HostFolderTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> properties = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() done");
        return properties.toArray(new ObjectContent[properties.size()]);
    }

    @Override
    public ObjectContent[] getDatastorePropertiesOnHyperHost(String[] propertyPaths) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() on Datastore properties. target MOR: " + _mor.getValue() + ", properties: " + new Gson().toJson(propertyPaths));

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Datastore");
        pSpec.getPathSet().addAll(Arrays.asList(propertyPaths));

        TraversalSpec cluster2DatastoreTraversal = new TraversalSpec();
        cluster2DatastoreTraversal.setType("ClusterComputeResource");
        cluster2DatastoreTraversal.setPath("datastore");
        cluster2DatastoreTraversal.setName("cluster2DatastoreTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(cluster2DatastoreTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> properties = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() done");
        return properties.toArray(new ObjectContent[properties.size()]);
    }

    public ObjectContent[] getHostPropertiesOnCluster(String[] propertyPaths) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() on Host properties. target MOR: " + _mor.getValue() + ", properties: " + new Gson().toJson(propertyPaths));

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("HostSystem");
        pSpec.getPathSet().addAll(Arrays.asList(propertyPaths));

        TraversalSpec cluster2HostTraversal = new TraversalSpec();
        cluster2HostTraversal.setType("ClusterComputeResource");
        cluster2HostTraversal.setPath("host");
        cluster2HostTraversal.setName("cluster2HostTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(cluster2HostTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);

        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> properties = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() done");
        return properties.toArray(new ObjectContent[properties.size()]);
    }

    @Override
    public boolean createVm(VirtualMachineConfigSpec vmSpec) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createVM_Task(). target MOR: " + _mor.getValue() + ", VirtualMachineConfigSpec: " + new Gson().toJson(vmSpec));

        assert (vmSpec != null);
        DatacenterMO dcMo = new DatacenterMO(_context, getHyperHostDatacenter());
        ManagedObjectReference morPool = getHyperHostOwnerResourcePool();

        ManagedObjectReference morTask = _context.getService().createVMTask(dcMo.getVmFolder(), vmSpec, morPool, null);
        boolean result = _context.getVimClient().waitForTask(morTask);

        if (result) {
            _context.waitForTaskProgressDone(morTask);

            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - createVM_Task() done(successfully)");
            return true;
        } else {
            s_logger.error("VMware createVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createVM_Task() done(failed)");
        return false;
    }

    @Override
    public void importVmFromOVF(String ovfFilePath, String vmName, DatastoreMO dsMo, String diskOption, String configurationId) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - importVmFromOVF(). target MOR: " + _mor.getValue() + ", ovfFilePath: " + ovfFilePath + ", vmName: " + vmName +
                    ", datastore: " + dsMo.getMor().getValue() + ", diskOption: " + diskOption);

        ManagedObjectReference morRp = getHyperHostOwnerResourcePool();
        assert (morRp != null);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - importVmFromOVF(). resource pool: " + morRp.getValue());

        HypervisorHostHelper.importVmFromOVF(this, ovfFilePath, vmName, dsMo, diskOption, morRp, null, configurationId);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - importVmFromOVF() done");
    }

    @Override
    public boolean createBlankVm(String vmName, String vmInternalCSName, int cpuCount, int cpuSpeedMHz, int cpuReservedMHz, boolean limitCpuUse, int memoryMB,
                                 int memoryReserveMB, String guestOsIdentifier, ManagedObjectReference morDs, boolean snapshotDirToParent, Pair<String, String> controllerInfo, Boolean systemVm) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createBlankVm(). target MOR: " + _mor.getValue() + ", vmName: " + vmName + ", cpuCount: " + cpuCount + ", cpuSpeedMhz: " +
                    cpuSpeedMHz + ", cpuReservedMHz: " + cpuReservedMHz + ", limitCpu: " + limitCpuUse + ", memoryMB: " + memoryMB + ", guestOS: " + guestOsIdentifier +
                    ", datastore: " + morDs.getValue() + ", snapshotDirToParent: " + snapshotDirToParent);

        boolean result =
                HypervisorHostHelper.createBlankVm(this, vmName, vmInternalCSName, cpuCount, cpuSpeedMHz, cpuReservedMHz, limitCpuUse, memoryMB, memoryReserveMB,
                        guestOsIdentifier, morDs, snapshotDirToParent, controllerInfo, systemVm);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createBlankVm() done");

        return result;
    }

    @Override
    public ManagedObjectReference mountDatastore(boolean vmfsDatastore, String poolHostAddress, int poolHostPort, String poolPath, String poolUuid, boolean createBaseFolder) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - mountDatastore(). target MOR: " + _mor.getValue() + ", vmfs: " + vmfsDatastore + ", poolHost: " + poolHostAddress +
                    ", poolHostPort: " + poolHostPort + ", poolPath: " + poolPath + ", poolUuid: " + poolUuid);

        ManagedObjectReference morDs = null;
        ManagedObjectReference morDsFirst = null;
        List<ManagedObjectReference> hosts = _context.getVimClient().getDynamicProperty(_mor, "host");
        if (hosts != null && hosts.size() > 0) {
            for (ManagedObjectReference morHost : hosts) {
                HostMO hostMo = new HostMO(_context, morHost);
                morDs = hostMo.mountDatastore(vmfsDatastore, poolHostAddress, poolHostPort, poolPath, poolUuid, true);
                if (morDsFirst == null)
                    morDsFirst = morDs;

                // assume datastore is in scope of datacenter
                assert (morDsFirst.getValue().equals(morDs.getValue()));
            }
        }

        if (morDs == null) {
            String msg = "Failed to mount datastore in all hosts within the cluster";
            s_logger.error(msg);

            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - mountDatastore() done(failed)");
            throw new Exception(msg);
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - mountDatastore() done(successfully)");

        return morDs;
    }

    @Override
    public void unmountDatastore(String poolUuid) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - unmountDatastore(). target MOR: " + _mor.getValue() + ", poolUuid: " + poolUuid);

        List<ManagedObjectReference> hosts = _context.getVimClient().getDynamicProperty(_mor, "host");
        if (hosts != null && hosts.size() > 0) {
            for (ManagedObjectReference morHost : hosts) {
                HostMO hostMo = new HostMO(_context, morHost);
                hostMo.unmountDatastore(poolUuid);
            }
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - unmountDatastore() done");
    }

    @Override
    public ManagedObjectReference findDatastore(String poolUuid) throws Exception {
        s_logger.trace(String.format("Searching datastore in target MOR [%s] with poolUuid [%s].", _mor.getValue(), poolUuid));

        CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(_context, _context.getServiceContent().getCustomFieldsManager());
        int key = cfmMo.getCustomFieldKey("Datastore", CustomFieldConstants.CLOUD_UUID);
        assert (key != 0);

        ObjectContent[] ocs = getDatastorePropertiesOnHyperHost(new String[] {"name", String.format("value[%d]", key)});
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                if (oc.getPropSet().get(0).getVal().equals(poolUuid)) {
                    s_logger.trace(String.format("Found datastore [%s] in target MOR [%s].", oc.getObj(), _mor.getValue()));
                    return oc.getObj();
                }
                if (oc.getPropSet().size() > 1) {
                    DynamicProperty prop = oc.getPropSet().get(1);
                    if (prop != null && prop.getVal() != null) {
                        if (prop.getVal() instanceof CustomFieldStringValue) {
                            String val = ((CustomFieldStringValue)prop.getVal()).getValue();
                            if (val.equalsIgnoreCase(poolUuid)) {
                                s_logger.trace(String.format("Found datastore [%s] in target MOR [%s].", oc.getObj(), _mor.getValue()));
                                return oc.getObj();
                            }
                        }
                    }
                }
            }
        }

        s_logger.trace(String.format("Failed to find a datastore with UUID [%s].", poolUuid));
        return null;
    }

    @Override
    public ManagedObjectReference findDatastoreByName(String datastoreName) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public ManagedObjectReference findDatastoreByExportPath(String exportPath) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - findDatastoreByExportPath(). target MOR: " + _mor.getValue() + ", exportPath: " + exportPath);

        ObjectContent[] ocs = getDatastorePropertiesOnHyperHost(new String[] {"info"});
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                DatastoreInfo dsInfo = (DatastoreInfo)oc.getPropSet().get(0).getVal();
                if (dsInfo != null && dsInfo instanceof NasDatastoreInfo) {
                    NasDatastoreInfo info = (NasDatastoreInfo)dsInfo;
                    if (info != null) {
                        String vmwareUrl = info.getUrl();
                        if (vmwareUrl.charAt(vmwareUrl.length() - 1) == '/')
                            vmwareUrl = vmwareUrl.substring(0, vmwareUrl.length() - 1);

                        URI uri = new URI(vmwareUrl);
                        if (uri.getPath().equals("/" + exportPath)) {

                            if (s_logger.isTraceEnabled())
                                s_logger.trace("vCenter API trace - findDatastoreByExportPath() done(successfully)");
                            return oc.getObj();
                        }
                    }
                }
            }
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - findDatastoreByExportPath() done(failed)");
        return null;
    }

    @Override
    public ManagedObjectReference findMigrationTarget(VirtualMachineMO vmMo) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - findMigrationTarget(). target MOR: " + _mor.getValue() + ", vm: " + vmMo.getName());

        List<ClusterHostRecommendation> candidates = recommendHostsForVm(vmMo);
        if (candidates != null && candidates.size() > 0) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - findMigrationTarget() done(successfully)");
            return candidates.get(0).getHost();
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - findMigrationTarget() done(failed)");
        return null;
    }

    @Override
    public boolean isHyperHostConnected() throws Exception {
        ObjectContent[] ocs = getHostPropertiesOnCluster(new String[] {"runtime"});
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                HostRuntimeInfo runtimeInfo = (HostRuntimeInfo)oc.getPropSet().get(0).getVal();
                // as long as we have one host connected, we assume the cluster is up
                if (runtimeInfo.getConnectionState() == HostSystemConnectionState.CONNECTED)
                    return true;
            }
        }
        return false;
    }

    @Override
    public String getHyperHostDefaultGateway() throws Exception {
        ObjectContent[] ocs = getHostPropertiesOnCluster(new String[] {"config.network.routeTableInfo.ipRoute"});
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                ArrayOfHostIpRouteEntry entries = (ArrayOfHostIpRouteEntry)oc.getPropSet().get(0).getVal();
                if (entries != null) {
                    for (HostIpRouteEntry entry : entries.getHostIpRouteEntry()) {
                        if (entry.getNetwork().equalsIgnoreCase("0.0.0.0"))
                            return entry.getGateway();
                    }
                }
            }
        }

        throw new Exception("Could not find host default gateway, host is not properly configured?");
    }

    @Override
    public VmwareHypervisorHostResourceSummary getHyperHostResourceSummary() throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostResourceSummary(). target MOR: " + _mor.getValue());

        VmwareHypervisorHostResourceSummary summary = new VmwareHypervisorHostResourceSummary();

        ComputeResourceSummary vmwareSummary = (ComputeResourceSummary)_context.getVimClient().getDynamicProperty(_mor, "summary");

        // TODO, need to use traversal to optimize retrieve of
        int cpuNumInCpuThreads = 1;
        List<ManagedObjectReference> hosts = _context.getVimClient().getDynamicProperty(_mor, "host");
        if (hosts != null && hosts.size() > 0) {
            for (ManagedObjectReference morHost : hosts) {
                HostMO hostMo = new HostMO(_context, morHost);
                HostHardwareSummary hardwareSummary = hostMo.getHostHardwareSummary();

                if (hardwareSummary.getNumCpuCores() * hardwareSummary.getNumCpuThreads() > cpuNumInCpuThreads)
                    cpuNumInCpuThreads = hardwareSummary.getNumCpuCores() * hardwareSummary.getNumCpuThreads();
            }
        }
        summary.setCpuCount(cpuNumInCpuThreads);
        summary.setCpuSpeed(vmwareSummary.getTotalCpu());
        summary.setMemoryBytes(vmwareSummary.getTotalMemory());

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostResourceSummary() done");
        return summary;
    }

    @Override
    public VmwareHypervisorHostNetworkSummary getHyperHostNetworkSummary(String esxServiceConsolePort) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostNetworkSummary(). target MOR: " + _mor.getValue() + ", mgmtPortgroup: " + esxServiceConsolePort);

        List<ManagedObjectReference> hosts = _context.getVimClient().getDynamicProperty(_mor, "host");
        if (hosts != null && hosts.size() > 0) {
            VmwareHypervisorHostNetworkSummary summary = new HostMO(_context, hosts.get(0)).getHyperHostNetworkSummary(esxServiceConsolePort);

            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - getHyperHostResourceSummary() done(successfully)");
            return summary;
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostResourceSummary() done(failed)");
        return null;
    }

    @Override
    public ComputeResourceSummary getHyperHostHardwareSummary() throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostHardwareSummary(). target MOR: " + _mor.getValue());

        ClusterComputeResourceSummary hardwareSummary = (ClusterComputeResourceSummary)_context.getVimClient().getDynamicProperty(_mor, "summary");

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostHardwareSummary() done");
        return hardwareSummary;
    }

    public List<ClusterHostRecommendation> recommendHostsForVm(VirtualMachineMO vmMo) throws Exception {
        return _context.getService().recommendHostsForVm(_mor, vmMo.getMor(), getHyperHostOwnerResourcePool());
    }

    public List<Pair<ManagedObjectReference, String>> getClusterHosts() throws Exception {
        List<Pair<ManagedObjectReference, String>> hosts = new ArrayList<Pair<ManagedObjectReference, String>>();

        ObjectContent[] ocs = getHostPropertiesOnCluster(new String[] {"name"});
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                ManagedObjectReference morHost = oc.getObj();
                String name = (String)oc.getPropSet().get(0).getVal();

                hosts.add(new Pair<ManagedObjectReference, String>(morHost, name));
            }
        }
        return hosts;
    }

    public HashMap<String, Integer> getVmVncPortsOnCluster() throws Exception {
        ObjectContent[] ocs = getVmPropertiesOnHyperHost(new String[] {"name", "config.extraConfig[\"RemoteDisplay.vnc.port\"]"});

        HashMap<String, Integer> portInfo = new HashMap<String, Integer>();
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {
                    String name = null;
                    String value = null;
                    for (DynamicProperty objProp : objProps) {
                        if (objProp.getName().equals("name")) {
                            name = (String)objProp.getVal();
                        } else {
                            OptionValue optValue = (OptionValue)objProp.getVal();
                            value = (String)optValue.getValue();
                        }
                    }

                    if (name != null && value != null) {
                        portInfo.put(name, Integer.parseInt(value));
                    }
                }
            }
        }

        return portInfo;
    }

    @Override
    public LicenseAssignmentManagerMO getLicenseAssignmentManager() throws Exception {
        // LicenseAssignmentManager deals with only host/vcenter licenses only. Has nothing todo with cluster
        throw new CloudRuntimeException("Unable to get LicenseAssignmentManager at cluster level");
    }
    private ManagedObjectReference getEnvironmentBrowser() throws Exception {
        if (_environmentBrowser == null) {
            _environmentBrowser = _context.getVimClient().getMoRefProp(_mor, "environmentBrowser");
        }
        return _environmentBrowser;
    }
    @Override
    public String getRecommendedDiskController(String guestOsId) throws Exception {
        VirtualMachineConfigOption vmConfigOption = _context.getService().queryConfigOption(getEnvironmentBrowser(), null, null);
        GuestOsDescriptor guestOsDescriptor = null;
        String diskController = null;
        List<GuestOsDescriptor> guestDescriptors = vmConfigOption.getGuestOSDescriptor();
        for (GuestOsDescriptor descriptor : guestDescriptors) {
            if (guestOsId != null && guestOsId.equalsIgnoreCase(descriptor.getId())) {
                guestOsDescriptor = descriptor;
                break;
            }
        }
        if (guestOsDescriptor != null) {
            diskController = VmwareHelper.getRecommendedDiskControllerFromDescriptor(guestOsDescriptor);
            s_logger.debug("Retrieved recommended disk controller for guest OS : " + guestOsId + " in cluster " + getHyperHostName() + " : " + diskController);
            return diskController;
        } else {
            String msg = "Unable to retrieve recommended disk controller for guest OS : " + guestOsId + " in cluster " + getHyperHostName();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
    }
}
