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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.AlreadyExistsFaultMsg;
import com.vmware.vim25.ClusterDasConfigInfo;
import com.vmware.vim25.ComputeResourceSummary;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostConfigManager;
import com.vmware.vim25.HostConnectInfo;
import com.vmware.vim25.HostFirewallInfo;
import com.vmware.vim25.HostFirewallRuleset;
import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostHyperThreadScheduleInfo;
import com.vmware.vim25.HostIpConfig;
import com.vmware.vim25.HostIpRouteEntry;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.HostNetworkInfo;
import com.vmware.vim25.HostNetworkPolicy;
import com.vmware.vim25.HostNetworkSecurityPolicy;
import com.vmware.vim25.HostNetworkTrafficShapingPolicy;
import com.vmware.vim25.HostOpaqueNetworkInfo;
import com.vmware.vim25.HostPortGroup;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.HostRuntimeInfo;
import com.vmware.vim25.HostSystemConnectionState;
import com.vmware.vim25.HostVirtualNic;
import com.vmware.vim25.HostVirtualSwitch;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualNicManagerNetConfig;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.utils.Pair;

public class HostMO extends BaseMO implements VmwareHypervisorHost {
    private static final Logger s_logger = Logger.getLogger(HostMO.class);
    Map<String, VirtualMachineMO> _vmCache = new HashMap<String, VirtualMachineMO>();

    //Map<String, String> _vmInternalNameMapCache = new HashMap<String, String>();

    public HostMO(VmwareContext context, ManagedObjectReference morHost) {
        super(context, morHost);
    }

    public HostMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public HostHardwareSummary getHostHardwareSummary() throws Exception {
        HostConnectInfo hostInfo = _context.getService().queryHostConnectionInfo(_mor);
        HostHardwareSummary hardwareSummary = hostInfo.getHost().getHardware();
        return hardwareSummary;
    }

    public HostConfigManager getHostConfigManager() throws Exception {
        return (HostConfigManager)_context.getVimClient().getDynamicProperty(_mor, "configManager");
    }

    public List<VirtualNicManagerNetConfig> getHostVirtualNicManagerNetConfig() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "config.virtualNicManagerInfo.netConfig");
    }

    public List<HostIpRouteEntry> getHostIpRouteEntries() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "config.network.routeTableInfo.ipRoute");
    }

    public HostListSummaryQuickStats getHostQuickStats() throws Exception {
        return (HostListSummaryQuickStats)_context.getVimClient().getDynamicProperty(_mor, "summary.quickStats");
    }

    public HostHyperThreadScheduleInfo getHostHyperThreadInfo() throws Exception {
        return (HostHyperThreadScheduleInfo)_context.getVimClient().getDynamicProperty(_mor, "config.hyperThread");
    }

    public HostNetworkInfo getHostNetworkInfo() throws Exception {
        return (HostNetworkInfo)_context.getVimClient().getDynamicProperty(_mor, "config.network");
    }

    public HostPortGroupSpec getHostPortGroupSpec(String portGroupName) throws Exception {

        HostNetworkInfo hostNetInfo = getHostNetworkInfo();

        List<HostPortGroup> portGroups = hostNetInfo.getPortgroup();
        if (portGroups != null) {
            for (HostPortGroup portGroup : portGroups) {
                HostPortGroupSpec spec = portGroup.getSpec();
                if (spec.getName().equals(portGroupName))
                    return spec;
            }
        }

        return null;
    }

    @Override
    public String getHyperHostName() throws Exception {
        return getName();
    }

    @Override
    public ClusterDasConfigInfo getDasConfig() throws Exception {
        ManagedObjectReference morParent = getParentMor();
        if (morParent.getType().equals("ClusterComputeResource")) {
            ClusterMO clusterMo = new ClusterMO(_context, morParent);
            return clusterMo.getDasConfig();
        }

        return null;
    }

    @Override
    public boolean isHAEnabled() throws Exception {
        ManagedObjectReference morParent = getParentMor();
        if (morParent.getType().equals("ClusterComputeResource")) {
            ClusterMO clusterMo = new ClusterMO(_context, morParent);
            return clusterMo.isHAEnabled();
        }

        return false;
    }

    @Override
    public void setRestartPriorityForVM(VirtualMachineMO vmMo, String priority) throws Exception {
        ManagedObjectReference morParent = getParentMor();
        if (morParent.getType().equals("ClusterComputeResource")) {
            ClusterMO clusterMo = new ClusterMO(_context, morParent);
            clusterMo.setRestartPriorityForVM(vmMo, priority);
        }
    }

    @Override
    public String getHyperHostDefaultGateway() throws Exception {
        List<HostIpRouteEntry> entries = getHostIpRouteEntries();
        for (HostIpRouteEntry entry : entries) {
            if (entry.getNetwork().equalsIgnoreCase("0.0.0.0"))
                return entry.getGateway();
        }

        throw new Exception("Could not find host default gateway, host is not properly configured?");
    }

    public HostStorageSystemMO getHostStorageSystemMO() throws Exception {
        return new HostStorageSystemMO(_context, (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "configManager.storageSystem"));
    }

    public HostDatastoreSystemMO getHostDatastoreSystemMO() throws Exception {
        return new HostDatastoreSystemMO(_context, (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "configManager.datastoreSystem"));
    }

    public HostDatastoreBrowserMO getHostDatastoreBrowserMO() throws Exception {
        return new HostDatastoreBrowserMO(_context, (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "datastoreBrowser"));
    }

    private DatastoreMO getHostDatastoreMO(String datastoreName) throws Exception {
        ObjectContent[] ocs = getDatastorePropertiesOnHyperHost(new String[] {"name"});
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {
                    for (DynamicProperty objProp : objProps) {
                        if (objProp.getVal().toString().equals(datastoreName))
                            return new DatastoreMO(_context, oc.getObj());
                    }
                }
            }
        }
        return null;
    }

    public HostNetworkSystemMO getHostNetworkSystemMO() throws Exception {
        HostConfigManager configMgr = getHostConfigManager();
        return new HostNetworkSystemMO(_context, configMgr.getNetworkSystem());
    }

    public HostFirewallSystemMO getHostFirewallSystemMO() throws Exception {
        HostConfigManager configMgr = getHostConfigManager();
        ManagedObjectReference morFirewall = configMgr.getFirewallSystem();

        // only ESX hosts have firewall manager
        if (morFirewall != null)
            return new HostFirewallSystemMO(_context, morFirewall);
        return null;
    }

    @Override
    public ManagedObjectReference getHyperHostDatacenter() throws Exception {
        Pair<DatacenterMO, String> dcPair = DatacenterMO.getOwnerDatacenter(getContext(), getMor());
        assert (dcPair != null);
        return dcPair.first().getMor();
    }

    @Override
    public ManagedObjectReference getHyperHostOwnerResourcePool() throws Exception {
        ManagedObjectReference morComputerResource = (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "parent");
        return (ManagedObjectReference)_context.getVimClient().getDynamicProperty(morComputerResource, "resourcePool");
    }

    @Override
    public ManagedObjectReference getHyperHostCluster() throws Exception {
        ManagedObjectReference morParent = (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "parent");

        if (morParent.getType().equalsIgnoreCase("ClusterComputeResource")) {
            return morParent;
        }

        assert (false);
        throw new Exception("Standalone host is not supported");
    }

    public ManagedObjectReference[] getHostLocalDatastore() throws Exception {
        List<ManagedObjectReference> datastores = _context.getVimClient().getDynamicProperty(_mor, "datastore");
        List<ManagedObjectReference> l = new ArrayList<ManagedObjectReference>();
        if (datastores != null) {
            for (ManagedObjectReference mor : datastores) {
                DatastoreSummary summary = (DatastoreSummary)_context.getVimClient().getDynamicProperty(mor, "summary");
                if (summary.getType().equalsIgnoreCase("VMFS") && !summary.isMultipleHostAccess())
                    l.add(mor);
            }
        }
        return l.toArray(new ManagedObjectReference[1]);
    }

    public HostVirtualSwitch getHostVirtualSwitchByName(String name) throws Exception {
        List<HostVirtualSwitch> switches = _context.getVimClient().getDynamicProperty(_mor, "config.network.vswitch");

        if (switches != null) {
            for (HostVirtualSwitch vswitch : switches) {
                if (vswitch.getName().equals(name))
                    return vswitch;
            }
        }
        return null;
    }

    public List<HostVirtualSwitch> getHostVirtualSwitch() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "config.network.vswitch");
    }

    public AboutInfo getHostAboutInfo() throws Exception {
        return (AboutInfo)_context.getVimClient().getDynamicProperty(_mor, "config.product");
    }

    public VmwareHostType getHostType() throws Exception {
        AboutInfo aboutInfo = getHostAboutInfo();
        if ("VMware ESXi".equals(aboutInfo.getName()))
            return VmwareHostType.ESXi;
        else if ("VMware ESX".equals(aboutInfo.getName()))
            return VmwareHostType.ESX;

        throw new Exception("Unrecognized VMware host type " + aboutInfo.getName());
    }

    // default virtual switch is which management network residents on
    public HostVirtualSwitch getHostDefaultVirtualSwitch() throws Exception {
        String managementPortGroup = getPortGroupNameByNicType(HostVirtualNicType.management);
        if (managementPortGroup != null)
            return getPortGroupVirtualSwitch(managementPortGroup);

        return null;
    }

    public HostVirtualSwitch getPortGroupVirtualSwitch(String portGroupName) throws Exception {
        String vSwitchName = getPortGroupVirtualSwitchName(portGroupName);
        if (vSwitchName != null)
            return getVirtualSwitchByName(vSwitchName);

        return null;
    }

    public HostVirtualSwitch getVirtualSwitchByName(String vSwitchName) throws Exception {

        List<HostVirtualSwitch> vSwitchs = getHostVirtualSwitch();
        if (vSwitchs != null) {
            for (HostVirtualSwitch vSwitch : vSwitchs) {
                if (vSwitch.getName().equals(vSwitchName))
                    return vSwitch;
            }
        }

        return null;
    }

    public String getPortGroupVirtualSwitchName(String portGroupName) throws Exception {
        HostNetworkInfo hostNetInfo = getHostNetworkInfo();
        List<HostPortGroup> portGroups = hostNetInfo.getPortgroup();
        if (portGroups != null) {
            for (HostPortGroup portGroup : portGroups) {
                HostPortGroupSpec spec = portGroup.getSpec();
                if (spec.getName().equals(portGroupName))
                    return spec.getVswitchName();
            }
        }

        return null;
    }

    public HostPortGroupSpec getPortGroupSpec(String portGroupName) throws Exception {
        HostNetworkInfo hostNetInfo = getHostNetworkInfo();
        List<HostPortGroup> portGroups = hostNetInfo.getPortgroup();
        if (portGroups != null) {
            for (HostPortGroup portGroup : portGroups) {
                HostPortGroupSpec spec = portGroup.getSpec();
                if (spec.getName().equals(portGroupName))
                    return spec;
            }
        }

        return null;
    }

    public String getPortGroupNameByNicType(HostVirtualNicType nicType) throws Exception {
        assert (nicType != null);

        List<VirtualNicManagerNetConfig> netConfigs =
                _context.getVimClient().getDynamicProperty(_mor, "config.virtualNicManagerInfo.netConfig");

        if (netConfigs != null) {
            for (VirtualNicManagerNetConfig netConfig : netConfigs) {
                if (netConfig.getNicType().equals(nicType.toString())) {
                    List<HostVirtualNic> nics = netConfig.getCandidateVnic();
                    if (nics != null) {
                        for (HostVirtualNic nic : nics) {
                            return nic.getPortgroup();
                        }
                    }
                }
            }
        }

        if (nicType == HostVirtualNicType.management) {
            // ESX management network is configured in service console
            HostNetworkInfo netInfo = getHostNetworkInfo();
            assert (netInfo != null);
            List<HostVirtualNic> nics = netInfo.getConsoleVnic();
            if (nics != null) {
                for (HostVirtualNic nic : nics) {
                    return nic.getPortgroup();
                }
            }
        }

        return null;
    }

    public boolean hasOpaqueNSXNetwork() throws Exception{
        HostNetworkInfo netInfo = getHostNetworkInfo();
        List<HostOpaqueNetworkInfo> opaqueNetworks = netInfo.getOpaqueNetwork();
        if (opaqueNetworks != null){
            for (HostOpaqueNetworkInfo opaqueNetwork : opaqueNetworks){
                if (opaqueNetwork.getOpaqueNetworkId() != null && opaqueNetwork.getOpaqueNetworkId().equals("br-int")
                        && opaqueNetwork.getOpaqueNetworkType() != null && opaqueNetwork.getOpaqueNetworkType().equals("nsx.network")){
                    return true;
                }
            }
            throw new Exception("NSX API VERSION >= 4.2 BUT br-int (nsx.network) NOT FOUND");
        }
        else {
            throw new Exception("NSX API VERSION >= 4.2 BUT br-int (nsx.network) NOT FOUND");
        }
    }

    public boolean hasPortGroup(HostVirtualSwitch vSwitch, String portGroupName) throws Exception {
        ManagedObjectReference morNetwork = getNetworkMor(portGroupName);
        if (morNetwork != null)
            return true;
        return false;
    }

    public void createPortGroup(HostVirtualSwitch vSwitch, String portGroupName, Integer vlanId, HostNetworkSecurityPolicy secPolicy,
            HostNetworkTrafficShapingPolicy shapingPolicy) throws Exception {
        assert (portGroupName != null);
        HostNetworkSystemMO hostNetMo = getHostNetworkSystemMO();
        assert (hostNetMo != null);

        HostPortGroupSpec spec = new HostPortGroupSpec();

        spec.setName(portGroupName);
        if (vlanId != null)
            spec.setVlanId(vlanId.intValue());
        HostNetworkPolicy policy = new HostNetworkPolicy();
        if (secPolicy != null)
            policy.setSecurity(secPolicy);
        policy.setShapingPolicy(shapingPolicy);
        spec.setPolicy(policy);
        spec.setVswitchName(vSwitch.getName());
        hostNetMo.addPortGroup(spec);
    }

    public void updatePortGroup(HostVirtualSwitch vSwitch, String portGroupName, Integer vlanId, HostNetworkSecurityPolicy secPolicy,
            HostNetworkTrafficShapingPolicy shapingPolicy) throws Exception {
        assert (portGroupName != null);
        HostNetworkSystemMO hostNetMo = getHostNetworkSystemMO();
        assert (hostNetMo != null);

        HostPortGroupSpec spec = new HostPortGroupSpec();

        spec.setName(portGroupName);
        if (vlanId != null)
            spec.setVlanId(vlanId.intValue());
        HostNetworkPolicy policy = new HostNetworkPolicy();
        if (secPolicy != null)
            policy.setSecurity(secPolicy);
        policy.setShapingPolicy(shapingPolicy);
        spec.setPolicy(policy);
        spec.setVswitchName(vSwitch.getName());
        hostNetMo.updatePortGroup(portGroupName, spec);
    }

    public void deletePortGroup(String portGroupName) throws Exception {
        assert (portGroupName != null);
        HostNetworkSystemMO hostNetMo = getHostNetworkSystemMO();
        assert (hostNetMo != null);
        hostNetMo.removePortGroup(portGroupName);
    }

    public ManagedObjectReference getNetworkMor(String portGroupName) throws Exception {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Network");
        pSpec.getPathSet().add("summary.name");

        TraversalSpec host2NetworkTraversal = new TraversalSpec();
        host2NetworkTraversal.setType("HostSystem");
        host2NetworkTraversal.setPath("network");
        host2NetworkTraversal.setName("host2NetworkTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(host2NetworkTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> props = oc.getPropSet();
                if (props != null) {
                    for (DynamicProperty prop : props) {
                        if (prop.getVal().equals(portGroupName))
                            return oc.getObj();
                    }
                }
            }
        }
        return null;
    }

    public List<ManagedObjectReference> getVmMorsOnNetwork(String portGroupName) throws Exception {
        ManagedObjectReference morNetwork = getNetworkMor(portGroupName);
        if (morNetwork != null)
            return _context.getVimClient().getDynamicProperty(morNetwork, "vm");
        return null;
    }

    public String getHostName() throws Exception {
        return (String)_context.getVimClient().getDynamicProperty(_mor, "name");
    }

    @Override
    public synchronized VirtualMachineMO findVmOnHyperHost(String vmName) throws Exception {
        if (s_logger.isDebugEnabled())
            s_logger.debug("find VM " + vmName + " on host");

        VirtualMachineMO vmMo = _vmCache.get(vmName);
        if (vmMo != null) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("VM " + vmName + " found in host cache");
            return vmMo;
        }

        s_logger.info("VM " + vmName + " not found in host cache");
        loadVmCache();

        return _vmCache.get(vmName);
    }

    private boolean isUserVMInternalCSName(String vmInternalCSName) {
        // CS generated internal names for user VMs are always of the format i-x-y.

        String internalCSUserVMNamingPattern = "^[i][-][0-9]+[-][0-9]+[-]";
        Pattern p = Pattern.compile(internalCSUserVMNamingPattern);
        java.util.regex.Matcher m = p.matcher(vmInternalCSName);
        if (m.find()) {
            return true;
        } else {
            return false;
        }
    }

    private void loadVmCache() throws Exception {
        if (s_logger.isDebugEnabled())
            s_logger.debug("load VM cache on host");

        _vmCache.clear();

        int key = getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }

        // name is the name of the VM as it appears in vCenter. The CLOUD_VM_INTERNAL_NAME custom
        // field value contains the name of the VM as it is maintained internally by cloudstack (i-x-y).
        ObjectContent[] ocs = getVmPropertiesOnHyperHost(new String[] {"name", "value[" + key + "]"});
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> props = oc.getPropSet();
                if (props != null) {
                    String vmVcenterName = null;
                    String vmInternalCSName = null;
                    for (DynamicProperty prop : props) {
                        if (prop.getName().equals("name")) {
                            vmVcenterName = prop.getVal().toString();
                        } else if (prop.getName().startsWith("value[")) {
                            if (prop.getVal() != null)
                                vmInternalCSName = ((CustomFieldStringValue)prop.getVal()).getValue();
                        }
                    }
                    String vmName = null;
                    if (vmInternalCSName != null && isUserVMInternalCSName(vmInternalCSName)) {
                        vmName = vmInternalCSName;
                    } else {
                        vmName = vmVcenterName;
                    }

                    if (s_logger.isTraceEnabled())
                        s_logger.trace("put " + vmName + " into host cache");

                    _vmCache.put(vmName, new VirtualMachineMO(_context, oc.getObj()));
                }
            }
        }
    }

    @Override
    public VirtualMachineMO findVmOnPeerHyperHost(String name) throws Exception {
        ManagedObjectReference morParent = getParentMor();

        if (morParent.getType().equals("ClusterComputeResource")) {
            ClusterMO clusterMo = new ClusterMO(_context, morParent);
            return clusterMo.findVmOnHyperHost(name);
        } else {
            // we don't support standalone host, all hosts have to be managed by
            // a cluster within vCenter
            assert (false);
            return null;
        }
    }

    @Override
    public boolean createVm(VirtualMachineConfigSpec vmSpec) throws Exception {
        assert (vmSpec != null);
        DatacenterMO dcMo = new DatacenterMO(_context, getHyperHostDatacenter());
        ManagedObjectReference morPool = getHyperHostOwnerResourcePool();

        ManagedObjectReference morTask = _context.getService().createVMTask(dcMo.getVmFolder(), vmSpec, morPool, _mor);
        boolean result = _context.getVimClient().waitForTask(morTask);

        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware createVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public HashMap<String, Integer> getVmVncPortsOnHost() throws Exception {

        int key = getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }

        ObjectContent[] ocs = getVmPropertiesOnHyperHost(new String[] {"name", "config.extraConfig[\"RemoteDisplay.vnc.port\"]", "value[" + key + "]"});

        HashMap<String, Integer> portInfo = new HashMap<String, Integer>();
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {
                    String vmName = null;
                    String value = null;
                    String vmInternalCSName = null;
                    for (DynamicProperty objProp : objProps) {
                        if (objProp.getName().equals("name")) {
                            vmName = (String)objProp.getVal();
                        } else if (objProp.getName().startsWith("value[")) {
                            if (objProp.getVal() != null)
                                vmInternalCSName = ((CustomFieldStringValue)objProp.getVal()).getValue();
                        } else {
                            OptionValue optValue = (OptionValue)objProp.getVal();
                            value = (String)optValue.getValue();
                        }
                    }

                    if (vmInternalCSName != null && isUserVMInternalCSName(vmInternalCSName))
                        vmName = vmInternalCSName;

                    if (vmName != null && value != null) {
                        portInfo.put(vmName, Integer.parseInt(value));
                    }
                }
            }
        }

        return portInfo;
    }

    @Override
    public ObjectContent[] getVmPropertiesOnHyperHost(String[] propertyPaths) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - retrieveProperties() for VM properties. target MOR: " + _mor.getValue() + ", properties: " +
                    new Gson().toJson(propertyPaths));

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("VirtualMachine");
        pSpec.getPathSet().addAll(Arrays.asList(propertyPaths));

        TraversalSpec host2VmTraversal = new TraversalSpec();
        host2VmTraversal.setType("HostSystem");
        host2VmTraversal.setPath("vm");
        host2VmTraversal.setName("host2VmTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(host2VmTraversal);

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
            s_logger.trace("vCenter API trace - retrieveProperties() on Datastore properties. target MOR: " + _mor.getValue() + ", properties: " +
                    new Gson().toJson(propertyPaths));

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Datastore");
        pSpec.getPathSet().addAll(Arrays.asList(propertyPaths));

        TraversalSpec host2DatastoreTraversal = new TraversalSpec();
        host2DatastoreTraversal.setType("HostSystem");
        host2DatastoreTraversal.setPath("datastore");
        host2DatastoreTraversal.setName("host2DatastoreTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(host2DatastoreTraversal);

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

    public List<Pair<ManagedObjectReference, String>> getDatastoreMountsOnHost() throws Exception {
        List<Pair<ManagedObjectReference, String>> mounts = new ArrayList<Pair<ManagedObjectReference, String>>();

        ObjectContent[] ocs = getDatastorePropertiesOnHyperHost(new String[] {String.format("host[\"%s\"].mountInfo.path", _mor.getValue())});
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                Pair<ManagedObjectReference, String> mount = new Pair<ManagedObjectReference, String>(oc.getObj(), oc.getPropSet().get(0).getVal().toString());
                mounts.add(mount);
            }
        }
        return mounts;
    }

    public List<Pair<ManagedObjectReference, String>> getLocalDatastoreOnHost() throws Exception {
        List<Pair<ManagedObjectReference, String>> dsList = new ArrayList<Pair<ManagedObjectReference, String>>();

        ObjectContent[] ocs = getDatastorePropertiesOnHyperHost(new String[] {"name", "summary"});
        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                DatastoreSummary dsSummary = (DatastoreSummary)VmwareHelper.getPropValue(oc, "summary");
                if (dsSummary.isMultipleHostAccess() == false && dsSummary.isAccessible() && dsSummary.getType().equalsIgnoreCase("vmfs")) {
                    ManagedObjectReference morDs = oc.getObj();
                    String name = (String)VmwareHelper.getPropValue(oc, "name");

                    if (!name.startsWith("-iqn.") && !name.startsWith("_iqn.")) {
                        dsList.add(new Pair<ManagedObjectReference, String>(morDs, name));
                    }
                }
            }
        }
        return dsList;
    }

    public void importVmFromOVF(String ovfFilePath, String vmName, String datastoreName, String diskOption) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - importVmFromOVF(). target MOR: " + _mor.getValue() + ", ovfFilePath: " + ovfFilePath + ", vmName: " + vmName +
                    ",datastoreName: " + datastoreName + ", diskOption: " + diskOption);

        DatastoreMO dsMo = getHostDatastoreMO(datastoreName);
        if (dsMo == null)
            throw new Exception("Invalid datastore name: " + datastoreName);

        importVmFromOVF(ovfFilePath, vmName, dsMo, diskOption);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - importVmFromOVF() done");
    }

    @Override
    public void importVmFromOVF(String ovfFilePath, String vmName, DatastoreMO dsMo, String diskOption) throws Exception {

        ManagedObjectReference morRp = getHyperHostOwnerResourcePool();
        assert (morRp != null);

        HypervisorHostHelper.importVmFromOVF(this, ovfFilePath, vmName, dsMo, diskOption, morRp, _mor);
    }

    @Override
    public boolean createBlankVm(String vmName, String vmInternalCSName, int cpuCount, int cpuSpeedMHz, int cpuReservedMHz, boolean limitCpuUse, int memoryMB,
                                 int memoryReserveMB, String guestOsIdentifier, ManagedObjectReference morDs, boolean snapshotDirToParent, Pair<String, String> controllerInfo, Boolean systemVm) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createBlankVm(). target MOR: " + _mor.getValue() + ", vmName: " + vmName + ", cpuCount: " + cpuCount + ", cpuSpeedMhz: " +
                    cpuSpeedMHz + ", cpuReservedMHz: " + cpuReservedMHz + ", limitCpu: " + limitCpuUse + ", memoryMB: " + memoryMB + ", guestOS: " + guestOsIdentifier +
                    ", datastore: " + morDs.getValue() + ", snapshotDirToParent: " + snapshotDirToParent +
                    ", controllerInfo:[" + controllerInfo.first() + "," + controllerInfo.second() + "], systemvm: " + systemVm);

        boolean result =
                HypervisorHostHelper.createBlankVm(this, vmName, vmInternalCSName, cpuCount, cpuSpeedMHz, cpuReservedMHz, limitCpuUse, memoryMB, memoryReserveMB,
                        guestOsIdentifier, morDs, snapshotDirToParent, controllerInfo, systemVm);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createBlankVm() done");
        return result;
    }

    public ManagedObjectReference getExistingDataStoreOnHost(boolean vmfsDatastore, String hostAddress, int hostPort, String path, String uuid,
            HostDatastoreSystemMO hostDatastoreSystemMo) {
        // First retrieve the list of Datastores on the host.
        List<ManagedObjectReference> morArray;
        try {
            morArray = hostDatastoreSystemMo.getDatastores();
        } catch (Exception e) {
            s_logger.info("Failed to retrieve list of Managed Object References");
            return null;
        }
        // Next, get all the NAS datastores from this array of datastores.
        if (morArray.size() > 0) {
            int i;
            for (i = 0; i < morArray.size(); i++) {
                NasDatastoreInfo nasDS;
                try {
                    nasDS = hostDatastoreSystemMo.getNasDatastoreInfo(morArray.get(i));
                    if (nasDS != null) {
                        //DatastoreInfo info = (DatastoreInfo)_context.getServiceUtil().getDynamicProperty(morDatastore, "info");
                        if (nasDS.getNas().getRemoteHost().equalsIgnoreCase(hostAddress) && nasDS.getNas().getRemotePath().equalsIgnoreCase(path)) {
                            return morArray.get(i);
                        }
                    }
                } catch (Exception e) {
                    s_logger.info("Encountered exception when retrieving nas datastore info");
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public ManagedObjectReference mountDatastore(boolean vmfsDatastore, String poolHostAddress, int poolHostPort, String poolPath, String poolUuid) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - mountDatastore(). target MOR: " + _mor.getValue() + ", vmfs: " + vmfsDatastore + ", poolHost: " + poolHostAddress +
                    ", poolHostPort: " + poolHostPort + ", poolPath: " + poolPath + ", poolUuid: " + poolUuid);

        HostDatastoreSystemMO hostDatastoreSystemMo = getHostDatastoreSystemMO();
        ManagedObjectReference morDatastore = hostDatastoreSystemMo.findDatastore(poolUuid);
        if (morDatastore == null) {
            if (!vmfsDatastore) {
                try {
                    morDatastore = hostDatastoreSystemMo.createNfsDatastore(poolHostAddress, poolHostPort, poolPath, poolUuid);
                } catch (AlreadyExistsFaultMsg e) {
                    s_logger.info("Creation of NFS datastore on vCenter failed since datastore already exists." +
                            " Details: vCenter API trace - mountDatastore(). target MOR: " + _mor.getValue() + ", vmfs: " + vmfsDatastore + ", poolHost: " + poolHostAddress +
                            ", poolHostPort: " + poolHostPort + ", poolPath: " + poolPath + ", poolUuid: " + poolUuid);
                    // Retrieve the morDatastore and return it.
                    return (getExistingDataStoreOnHost(vmfsDatastore, poolHostAddress, poolHostPort, poolPath, poolUuid, hostDatastoreSystemMo));
                } catch (Exception e) {
                    s_logger.info("Creation of NFS datastore on vCenter failed. " + " Details: vCenter API trace - mountDatastore(). target MOR: " + _mor.getValue() +
                            ", vmfs: " + vmfsDatastore + ", poolHost: " + poolHostAddress + ", poolHostPort: " + poolHostPort + ", poolPath: " + poolPath + ", poolUuid: " +
                            poolUuid + ". Exception mesg: " + e.getMessage());
                    throw new Exception("Creation of NFS datastore on vCenter failed.");
                }
                if (morDatastore == null) {
                    String msg = "Unable to create NFS datastore. host: " + poolHostAddress + ", port: " + poolHostPort + ", path: " + poolPath + ", uuid: " + poolUuid;
                    s_logger.error(msg);

                    if (s_logger.isTraceEnabled())
                        s_logger.trace("vCenter API trace - mountDatastore() done(failed)");
                    throw new Exception(msg);
                }
            } else {
                morDatastore = _context.getDatastoreMorByPath(poolPath);
                if (morDatastore == null) {
                    String msg = "Unable to create VMFS datastore. host: " + poolHostAddress + ", port: " + poolHostPort + ", path: " + poolPath + ", uuid: " + poolUuid;
                    s_logger.error(msg);

                    if (s_logger.isTraceEnabled())
                        s_logger.trace("vCenter API trace - mountDatastore() done(failed)");
                    throw new Exception(msg);
                }

                DatastoreMO dsMo = new DatastoreMO(_context, morDatastore);
                dsMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, poolUuid);
            }
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - mountDatastore() done(successfully)");

        return morDatastore;
    }

    @Override
    public void unmountDatastore(String uuid) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - unmountDatastore(). target MOR: " + _mor.getValue() + ", uuid: " + uuid);

        HostDatastoreSystemMO hostDatastoreSystemMo = getHostDatastoreSystemMO();
        if (!hostDatastoreSystemMo.deleteDatastore(uuid)) {
            String msg = "Unable to unmount datastore. uuid: " + uuid;
            s_logger.error(msg);

            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - unmountDatastore() done(failed)");
            throw new Exception(msg);
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - unmountDatastore() done");
    }

    @Override
    public ManagedObjectReference findDatastore(String poolUuid) throws Exception {
        HostDatastoreSystemMO hostDsMo = getHostDatastoreSystemMO();
        return hostDsMo.findDatastore(poolUuid);
    }

    @Override
    public ManagedObjectReference findDatastoreByExportPath(String exportPath) throws Exception {
        HostDatastoreSystemMO datastoreSystemMo = getHostDatastoreSystemMO();
        return datastoreSystemMo.findDatastoreByExportPath(exportPath);
    }

    @Override
    public ManagedObjectReference findDatastoreByName(String datastoreName) throws Exception {
        HostDatastoreSystemMO hostDsMo = getHostDatastoreSystemMO();
        return hostDsMo.findDatastoreByName(datastoreName);
    }

    @Override
    public ManagedObjectReference findMigrationTarget(VirtualMachineMO vmMo) throws Exception {
        return _mor;
    }

    @Override
    public VmwareHypervisorHostResourceSummary getHyperHostResourceSummary() throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostResourceSummary(). target MOR: " + _mor.getValue());

        VmwareHypervisorHostResourceSummary summary = new VmwareHypervisorHostResourceSummary();

        HostHardwareSummary hardwareSummary = getHostHardwareSummary();
        // TODO: not sure how hyper-thread is counted in VMware resource pool
        summary.setCpuCount(hardwareSummary.getNumCpuThreads());
        summary.setMemoryBytes(hardwareSummary.getMemorySize());
        summary.setCpuSpeed(hardwareSummary.getCpuMhz());
        summary.setCpuSockets((int)hardwareSummary.getNumCpuPkgs());

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostResourceSummary() done");
        return summary;
    }

    @Override
    public VmwareHypervisorHostNetworkSummary getHyperHostNetworkSummary(String managementPortGroup) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostNetworkSummary(). target MOR: " + _mor.getValue() + ", mgmtPortgroup: " + managementPortGroup);

        VmwareHypervisorHostNetworkSummary summary = new VmwareHypervisorHostNetworkSummary();

        if (getHostType() == VmwareHostType.ESXi) {
            List<VirtualNicManagerNetConfig> netConfigs =
                    _context.getVimClient().getDynamicProperty(_mor, "config.virtualNicManagerInfo.netConfig");
            assert (netConfigs != null);

            String dvPortGroupKey;
            String portGroup;
            for (VirtualNicManagerNetConfig netConfig : netConfigs) {
                if (netConfig.getNicType().equals("management")) {
                    for (HostVirtualNic nic : netConfig.getCandidateVnic()) {
                        portGroup = nic.getPortgroup();
                        if (portGroup == null || portGroup.isEmpty()) {
                            dvPortGroupKey = nic.getSpec().getDistributedVirtualPort().getPortgroupKey();
                            portGroup = getNetworkName(dvPortGroupKey);
                        }
                        if (portGroup.equalsIgnoreCase(managementPortGroup)) {
                            summary.setHostIp(nic.getSpec().getIp().getIpAddress());
                            summary.setHostNetmask(nic.getSpec().getIp().getSubnetMask());
                            summary.setHostMacAddress(nic.getSpec().getMac());

                            if (s_logger.isTraceEnabled())
                                s_logger.trace("vCenter API trace - getHyperHostNetworkSummary() done(successfully)");
                            return summary;
                        }
                    }
                }
            }
        } else {
            // try with ESX path
            List<HostVirtualNic> hostVNics = _context.getVimClient().getDynamicProperty(_mor, "config.network.consoleVnic");

            if (hostVNics != null) {
                for (HostVirtualNic vnic : hostVNics) {
                    if (vnic.getPortgroup().equals(managementPortGroup)) {
                        summary.setHostIp(vnic.getSpec().getIp().getIpAddress());
                        summary.setHostNetmask(vnic.getSpec().getIp().getSubnetMask());
                        summary.setHostMacAddress(vnic.getSpec().getMac());

                        if (s_logger.isTraceEnabled())
                            s_logger.trace("vCenter API trace - getHyperHostNetworkSummary() done(successfully)");
                        return summary;
                    }
                }
            }
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostNetworkSummary() done(failed)");
        throw new Exception("Unable to find management port group " + managementPortGroup);
    }

    @Override
    public ComputeResourceSummary getHyperHostHardwareSummary() throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostHardwareSummary(). target MOR: " + _mor.getValue());

        //
        // This is to adopt the model when using Cluster as a big host while ComputeResourceSummary is used
        // directly from VMware resource pool
        //
        // When we break cluster hosts into individual hosts used in our resource allocator,
        // we will have to populate ComputeResourceSummary by ourselves here
        //
        HostHardwareSummary hardwareSummary = getHostHardwareSummary();

        ComputeResourceSummary resourceSummary = new ComputeResourceSummary();

        // TODO: not sure how hyper-threading is counted in VMware
        resourceSummary.setNumCpuCores(hardwareSummary.getNumCpuCores());

        // Note: memory here is in Byte unit
        resourceSummary.setTotalMemory(hardwareSummary.getMemorySize());

        // Total CPU is based on (# of cores) x Mhz
        int totalCpu = hardwareSummary.getCpuMhz() * hardwareSummary.getNumCpuCores();
        resourceSummary.setTotalCpu(totalCpu);

        HostListSummaryQuickStats stats = getHostQuickStats();
        if (stats.getOverallCpuUsage() == null || stats.getOverallMemoryUsage() == null)
            throw new Exception("Unable to get valid overal CPU/Memory usage data, host may be disconnected");

        resourceSummary.setEffectiveCpu(totalCpu - stats.getOverallCpuUsage());

        // Note effective memory is in MB unit
        resourceSummary.setEffectiveMemory(hardwareSummary.getMemorySize() / (1024 * 1024) - stats.getOverallMemoryUsage());

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getHyperHostHardwareSummary() done");

        return resourceSummary;
    }

    @Override
    public boolean isHyperHostConnected() throws Exception {
        HostRuntimeInfo runtimeInfo = (HostRuntimeInfo)_context.getVimClient().getDynamicProperty(_mor, "runtime");
        return runtimeInfo != null && runtimeInfo.getConnectionState() == HostSystemConnectionState.CONNECTED;
    }

    public boolean revertToSnapshot(ManagedObjectReference morSnapshot) throws Exception {
        ManagedObjectReference morTask = _context.getService().revertToSnapshotTask(morSnapshot, _mor, false);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware revert to snapshot failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    @Override
    public LicenseAssignmentManagerMO getLicenseAssignmentManager() throws Exception {
        ManagedObjectReference licenseMgr;
        ManagedObjectReference licenseAssignmentManager;
        LicenseManagerMO licenseMgrMo;

        licenseMgr = _context.getServiceContent().getLicenseManager();
        licenseMgrMo = new LicenseManagerMO(_context, licenseMgr);
        licenseAssignmentManager = licenseMgrMo.getLicenseAssignmentManager();

        return new LicenseAssignmentManagerMO(_context, licenseAssignmentManager);
    }

    public void enableVncOnHostFirewall() throws Exception {
        HostFirewallSystemMO firewallMo = getHostFirewallSystemMO();
        boolean bRefresh = false;
        if (firewallMo != null) {
            HostFirewallInfo firewallInfo = firewallMo.getFirewallInfo();
            if (firewallInfo != null && firewallInfo.getRuleset() != null) {
                for (HostFirewallRuleset rule : firewallInfo.getRuleset()) {
                    if ("vncServer".equalsIgnoreCase(rule.getKey())) {
                        bRefresh = true;
                        firewallMo.enableRuleset("vncServer");
                    } else if ("gdbserver".equalsIgnoreCase(rule.getKey())) {
                        bRefresh = true;
                        firewallMo.enableRuleset("gdbserver");
                    }
                }
            }

            if (bRefresh)
                firewallMo.refreshFirewall();
        }
    }

    @Override
    public String getRecommendedDiskController(String guestOsId) throws Exception {
        ManagedObjectReference morParent = getParentMor();
        if (morParent.getType().equals("ClusterComputeResource")) {
            ClusterMO clusterMo = new ClusterMO(_context, morParent);
            return clusterMo.getRecommendedDiskController(guestOsId);
        }
        return null;
    }

    public String getHostManagementIp(String managementPortGroup) throws Exception {
        HostNetworkInfo netInfo = getHostNetworkInfo();

        List<HostVirtualNic> nics = netInfo.getVnic();
        for (HostVirtualNic nic : nics) {
            if (nic.getPortgroup().equals(managementPortGroup)) {
                HostIpConfig ipConfig = nic.getSpec().getIp();

                return ipConfig.getIpAddress();
            }
        }

        return null;
    }

    public List<ManagedObjectReference> getHostNetworks() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "network");
    }

    public String getNetworkName(String netMorVal) throws Exception {
        String networkName = "";
        List<ManagedObjectReference> hostNetworks = getHostNetworks();
        for (ManagedObjectReference hostNetwork : hostNetworks) {
            if (hostNetwork.getValue().equals(netMorVal)) {
                networkName = _context.getVimClient().getDynamicProperty(hostNetwork, "name");
                break;
            }
        }
        return networkName;
    }

    public void createPortGroup(HostVirtualSwitch vSwitch, String portGroupName, Integer vlanId,
            HostNetworkSecurityPolicy secPolicy, HostNetworkTrafficShapingPolicy shapingPolicy, long timeOutMs)
            throws Exception {
        assert (portGroupName != null);

        // Prepare lock to avoid simultaneous execution of the synchronized block for
        // duplicate port groups on the ESXi host it's being created on.
        String hostPortGroup = _mor.getValue() + "-" + portGroupName;
        synchronized (hostPortGroup.intern()) {
            // Check if port group exists already
            if (hasPortGroup(vSwitch, portGroupName)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found port group " + portGroupName + " in vSwitch " + vSwitch.getName()
                        + ". Not attempting to create port group as it already exists.");
                }
                return;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Port group " + portGroupName + " doesn't exist in vSwitch " + vSwitch.getName()
                        + ". Attempting to create port group in this vSwitch.");
                }
            }
            // Create port group if not exists already
            createPortGroup(vSwitch, portGroupName, vlanId, secPolicy, shapingPolicy);

            // Wait for port group to turn up ready on vCenter upto timeout of timeOutMs milli seconds
            waitForPortGroup(portGroupName, timeOutMs);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully created port group " + portGroupName + " in vSwitch " + vSwitch.getName()
                + " on host " + getHostName());
        }
    }

    public ManagedObjectReference waitForPortGroup(String networkName, long timeOutMs) throws Exception {
        ManagedObjectReference morNetwork = null;
        // if portGroup is just created, getNetwork may fail to retrieve it, we
        // need to retry
        long startTick = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTick <= timeOutMs) {
            morNetwork = getNetworkMor(networkName);
            if (morNetwork != null) {
                break;
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Waiting for network " + networkName + " to be ready");
            }
            Thread.sleep(1000);
        }
        return morNetwork;
    }
}
