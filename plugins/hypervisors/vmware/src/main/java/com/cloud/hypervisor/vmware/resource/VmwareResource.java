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
package com.cloud.hypervisor.vmware.resource;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import com.cloud.agent.api.PatchSystemVmAnswer;
import com.cloud.agent.api.PatchSystemVmCommand;
import com.cloud.resource.ServerResourceBase;
import com.cloud.utils.FileUtil;
import com.cloud.utils.LogUtils;
import com.cloud.utils.validation.ChecksumUtil;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.configdrive.ConfigDrive;
import org.apache.cloudstack.storage.resource.NfsSecondaryStorageResource;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.joda.time.Duration;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoAnswer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStoragePoolCapabilitiesAnswer;
import com.cloud.agent.api.GetStoragePoolCapabilitiesCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.agent.api.GetVmDiskStatsAnswer;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmIpAddressCommand;
import com.cloud.agent.api.GetVmNetworkStatsAnswer;
import com.cloud.agent.api.GetVmNetworkStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVmVncTicketAnswer;
import com.cloud.agent.api.GetVmVncTicketCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateVmToPoolAnswer;
import com.cloud.agent.api.MigrateVmToPoolCommand;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.ModifyTargetsAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.PrepareUnmanageVMInstanceAnswer;
import com.cloud.agent.api.PrepareUnmanageVMInstanceCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.ReplugNicAnswer;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.SetupPersistentNetworkAnswer;
import com.cloud.agent.api.SetupPersistentNetworkCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UnregisterNicCommand;
import com.cloud.agent.api.UnregisterVMCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.ValidateSnapshotAnswer;
import com.cloud.agent.api.ValidateSnapshotCommand;
import com.cloud.agent.api.ValidateVcenterDetailsCommand;
import com.cloud.agent.api.VmDiskStatsEntry;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeAnswer;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DeployAsIsInfoTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.api.to.deployasis.OVFPropertyTO;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.Vlan;
import com.cloud.exception.CloudException;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.guru.VMwareGuru;
import com.cloud.hypervisor.vmware.manager.VmwareHostService;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.manager.VmwareStorageMount;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.CustomFieldsManagerMO;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreFile;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.DistributedVirtualSwitchMO;
import com.cloud.hypervisor.vmware.mo.FeatureKeyConstants;
import com.cloud.hypervisor.vmware.mo.HostDatastoreSystemMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.HostStorageSystemMO;
import com.cloud.hypervisor.vmware.mo.HypervisorHostHelper;
import com.cloud.hypervisor.vmware.mo.NetworkDetails;
import com.cloud.hypervisor.vmware.mo.NetworkMO;
import com.cloud.hypervisor.vmware.mo.PbmProfileManagerMO;
import com.cloud.hypervisor.vmware.mo.StoragepodMO;
import com.cloud.hypervisor.vmware.mo.TaskMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VirtualMachineDiskInfoBuilder;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VirtualSwitchType;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHostNetworkSummary;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHostResourceSummary;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareContextPool;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.VmwareTrafficLabel;
import com.cloud.network.router.VirtualRouterAutoScale;
import com.cloud.resource.ServerResource;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume;
import com.cloud.storage.resource.StoragePoolResource;
import com.cloud.storage.resource.StorageSubsystemCommandHandler;
import com.cloud.storage.resource.VmwareStorageLayoutHelper;
import com.cloud.storage.resource.VmwareStorageProcessor;
import com.cloud.storage.resource.VmwareStorageProcessor.VmwareStorageProcessorConfigurableFields;
import com.cloud.storage.resource.VmwareStorageSubsystemCommandHandler;
import com.cloud.storage.template.TemplateProp;
import com.cloud.template.TemplateManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.mgmt.PropertyMapDynamicBean;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.nicira.nvp.plugin.NiciraNvpApiVersion;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VmDetailConstants;
import com.google.gson.Gson;
import com.vmware.vim25.AboutInfo;
import com.vmware.vim25.ArrayUpdateOperation;
import com.vmware.vim25.BoolPolicy;
import com.vmware.vim25.ComputeResourceSummary;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DVPortConfigInfo;
import com.vmware.vim25.DVPortConfigSpec;
import com.vmware.vim25.DasVmPriority;
import com.vmware.vim25.DatastoreInfo;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.DistributedVirtualPort;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DistributedVirtualSwitchPortCriteria;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.HostCapability;
import com.vmware.vim25.HostConfigInfo;
import com.vmware.vim25.HostFileSystemMountInfo;
import com.vmware.vim25.HostHostBusAdapter;
import com.vmware.vim25.HostInternetScsiHba;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NasDatastoreInfo;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.StoragePodSummary;
import com.vmware.vim25.ToolsUnavailableFaultMsg;
import com.vmware.vim25.VAppOvfSectionInfo;
import com.vmware.vim25.VAppOvfSectionSpec;
import com.vmware.vim25.VAppProductInfo;
import com.vmware.vim25.VAppProductSpec;
import com.vmware.vim25.VAppPropertyInfo;
import com.vmware.vim25.VAppPropertySpec;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualEthernetCardOpaqueNetworkBackingInfo;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualMachineBootOptions;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineDefinedProfileSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineFileLayoutEx;
import com.vmware.vim25.VirtualMachineFileLayoutExFileInfo;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRelocateSpecDiskLocator;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineToolsStatus;
import com.vmware.vim25.VirtualMachineVideoCard;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualUSBController;
import com.vmware.vim25.VirtualVmxnet2;
import com.vmware.vim25.VirtualVmxnet3;
import com.vmware.vim25.VmConfigInfo;
import com.vmware.vim25.VmConfigSpec;
import com.vmware.vim25.VmwareDistributedVirtualSwitchPvlanSpec;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanIdSpec;

public class VmwareResource extends ServerResourceBase implements StoragePoolResource, ServerResource, VmwareHostService, VirtualRouterDeployer {
    private static final Logger s_logger = Logger.getLogger(VmwareResource.class);
    public static final String VMDK_EXTENSION = ".vmdk";
    private static final String EXECUTING_RESOURCE_COMMAND = "Executing resource command %s: [%s].";
    public static final String BASEPATH = "/usr/share/cloudstack-common/vms/";

    private static final Random RANDOM = new Random(System.nanoTime());

    protected String _name;

    protected final long _opsTimeout = 900000;   // 15 minutes time out to time

    protected final int _shutdownWaitMs = 300000;  // wait up to 5 minutes for shutdown

    // out an operation
    protected final int _retry = 24;
    protected final int _sleep = 10000;
    protected final int DefaultDomRSshPort = 3922;
    protected final int MazCmdMBean = 100;

    protected String _url;
    protected String _dcId;
    protected String _pod;
    protected String _cluster;
    protected String _username;
    protected String _password;
    protected String _guid;
    protected String _vCenterAddress;
    protected String storageNfsVersion;

    protected String _privateNetworkVSwitchName;
    protected VmwareTrafficLabel _guestTrafficInfo = new VmwareTrafficLabel(TrafficType.Guest);
    protected VmwareTrafficLabel _publicTrafficInfo = new VmwareTrafficLabel(TrafficType.Public);
    protected Map<String, String> _vsmCredentials = null;
    protected int _portsPerDvPortGroup;
    protected boolean _fullCloneFlag = false;
    protected boolean _instanceNameFlag = false;

    protected boolean _recycleHungWorker = false;
    protected DiskControllerType _rootDiskController = DiskControllerType.ide;

    protected ManagedObjectReference _morHyperHost;
    protected final static ThreadLocal<VmwareContext> s_serviceContext = new ThreadLocal<VmwareContext>();
    protected String _hostName;

    protected List<PropertyMapDynamicBean> _cmdMBeans = new ArrayList<PropertyMapDynamicBean>();

    protected Gson _gson;

    protected volatile long _cmdSequence = 1;

    protected StorageSubsystemCommandHandler storageHandler;
    private VmwareStorageProcessor _storageProcessor;

    protected VirtualRoutingResource _vrResource;

    protected final static HashMap<VirtualMachinePowerState, PowerState> s_powerStatesTable = new HashMap<VirtualMachinePowerState, PowerState>();

    static {
        s_powerStatesTable.put(VirtualMachinePowerState.POWERED_ON, PowerState.PowerOn);
        s_powerStatesTable.put(VirtualMachinePowerState.POWERED_OFF, PowerState.PowerOff);
        s_powerStatesTable.put(VirtualMachinePowerState.SUSPENDED, PowerState.PowerOn);
    }

    protected static File s_systemVmKeyFile = null;
    private static final Object s_syncLockObjectFetchKeyFile = new Object();
    protected static final String s_relativePathSystemVmKeyFileInstallDir = "scripts/vm/systemvm/id_rsa.cloud";
    protected static final String s_defaultPathSystemVmKeyFile = "/usr/share/cloudstack-common/scripts/vm/systemvm/id_rsa.cloud";

    public Gson getGson() {
        return _gson;
    }

    public VmwareResource() {
        _gson = GsonHelper.getGsonLogger();
    }

    private String getCommandLogTitle(Command cmd) {
        StringBuffer sb = new StringBuffer();
        if (_hostName != null) {
            sb.append(_hostName);
        }

        if (cmd.getContextParam("job") != null) {
            sb.append(", ").append(cmd.getContextParam("job"));
        }
        sb.append(", cmd: ").append(cmd.getClass().getSimpleName());

        return sb.toString();
    }

    @Override
    public Answer executeRequest(Command cmd) {
        logCommand(cmd);
        Answer answer = null;
        NDC.push(getCommandLogTitle(cmd));
        try {
            long cmdSequence = _cmdSequence++;
            Date startTime = DateUtil.currentGMTTime();
            PropertyMapDynamicBean mbean = new PropertyMapDynamicBean();
            mbean.addProp("StartTime", DateUtil.getDateDisplayString(TimeZone.getDefault(), startTime));
            mbean.addProp("Command", _gson.toJson(cmd));
            mbean.addProp("Sequence", String.valueOf(cmdSequence));
            mbean.addProp("Name", cmd.getClass().getSimpleName());

            Class<? extends Command> clz = cmd.getClass();
            if (clz == PatchSystemVmCommand.class) {
                answer = execute((PatchSystemVmCommand) cmd);
            } else if (cmd instanceof NetworkElementCommand) {
                return _vrResource.executeRequest((NetworkElementCommand) cmd);
            } else if (clz == ReadyCommand.class) {
                answer = execute((ReadyCommand) cmd);
            } else if (clz == GetHostStatsCommand.class) {
                answer = execute((GetHostStatsCommand) cmd);
            } else if (clz == GetVmStatsCommand.class) {
                answer = execute((GetVmStatsCommand) cmd);
            } else if (clz == GetVmNetworkStatsCommand.class) {
                answer = execute((GetVmNetworkStatsCommand) cmd);
            } else if (clz == GetVmDiskStatsCommand.class) {
                answer = execute((GetVmDiskStatsCommand) cmd);
            } else if (cmd instanceof GetVolumeStatsCommand) {
                return execute((GetVolumeStatsCommand) cmd);
            } else if (clz == CheckHealthCommand.class) {
                answer = execute((CheckHealthCommand) cmd);
            } else if (clz == StopCommand.class) {
                answer = execute((StopCommand) cmd);
            } else if (clz == RebootRouterCommand.class) {
                answer = execute((RebootRouterCommand) cmd);
            } else if (clz == RebootCommand.class) {
                answer = execute((RebootCommand) cmd);
            } else if (clz == CheckVirtualMachineCommand.class) {
                answer = execute((CheckVirtualMachineCommand) cmd);
            } else if (clz == PrepareForMigrationCommand.class) {
                answer = execute((PrepareForMigrationCommand) cmd);
            } else if (clz == MigrateCommand.class) {
                answer = execute((MigrateCommand) cmd);
            } else if (clz == MigrateVmToPoolCommand.class) {
                answer = execute((MigrateVmToPoolCommand) cmd);
            } else if (clz == MigrateWithStorageCommand.class) {
                answer = execute((MigrateWithStorageCommand) cmd);
            } else if (clz == MigrateVolumeCommand.class) {
                answer = execute((MigrateVolumeCommand) cmd);
            } else if (clz == DestroyCommand.class) {
                answer = execute((DestroyCommand) cmd);
            } else if (clz == CreateStoragePoolCommand.class) {
                return execute((CreateStoragePoolCommand) cmd);
            } else if (clz == ModifyTargetsCommand.class) {
                answer = execute((ModifyTargetsCommand) cmd);
            } else if (clz == ModifyStoragePoolCommand.class) {
                answer = execute((ModifyStoragePoolCommand) cmd);
            } else if (clz == GetStoragePoolCapabilitiesCommand.class) {
                answer = execute((GetStoragePoolCapabilitiesCommand) cmd);
            } else if (clz == DeleteStoragePoolCommand.class) {
                answer = execute((DeleteStoragePoolCommand) cmd);
            } else if (clz == CopyVolumeCommand.class) {
                answer = execute((CopyVolumeCommand) cmd);
            } else if (clz == AttachIsoCommand.class) {
                answer = execute((AttachIsoCommand) cmd);
            } else if (clz == ValidateSnapshotCommand.class) {
                answer = execute((ValidateSnapshotCommand) cmd);
            } else if (clz == ManageSnapshotCommand.class) {
                answer = execute((ManageSnapshotCommand) cmd);
            } else if (clz == BackupSnapshotCommand.class) {
                answer = execute((BackupSnapshotCommand) cmd);
            } else if (clz == CreateVolumeFromSnapshotCommand.class) {
                answer = execute((CreateVolumeFromSnapshotCommand) cmd);
            } else if (clz == CreatePrivateTemplateFromVolumeCommand.class) {
                answer = execute((CreatePrivateTemplateFromVolumeCommand) cmd);
            } else if (clz == CreatePrivateTemplateFromSnapshotCommand.class) {
                answer = execute((CreatePrivateTemplateFromSnapshotCommand) cmd);
            } else if (clz == UpgradeSnapshotCommand.class) {
                answer = execute((UpgradeSnapshotCommand) cmd);
            } else if (clz == GetStorageStatsCommand.class) {
                answer = execute((GetStorageStatsCommand) cmd);
            } else if (clz == PrimaryStorageDownloadCommand.class) {
                answer = execute((PrimaryStorageDownloadCommand) cmd);
            } else if (clz == GetVncPortCommand.class) {
                answer = execute((GetVncPortCommand) cmd);
            } else if (clz == SetupCommand.class) {
                answer = execute((SetupCommand) cmd);
            } else if (clz == MaintainCommand.class) {
                answer = execute((MaintainCommand) cmd);
            } else if (clz == PingTestCommand.class) {
                answer = execute((PingTestCommand) cmd);
            } else if (clz == CheckOnHostCommand.class) {
                answer = execute((CheckOnHostCommand) cmd);
            } else if (clz == ModifySshKeysCommand.class) {
                answer = execute((ModifySshKeysCommand) cmd);
            } else if (clz == NetworkUsageCommand.class) {
                answer = execute((NetworkUsageCommand) cmd);
            } else if (clz == StartCommand.class) {
                answer = execute((StartCommand) cmd);
            } else if (clz == CheckSshCommand.class) {
                answer = execute((CheckSshCommand) cmd);
            } else if (clz == CheckNetworkCommand.class) {
                answer = execute((CheckNetworkCommand) cmd);
            } else if (clz == PlugNicCommand.class) {
                answer = execute((PlugNicCommand) cmd);
            } else if (clz == ReplugNicCommand.class) {
                answer = execute((ReplugNicCommand) cmd);
            } else if (clz == UnPlugNicCommand.class) {
                answer = execute((UnPlugNicCommand) cmd);
            } else if (cmd instanceof CreateVMSnapshotCommand) {
                return execute((CreateVMSnapshotCommand) cmd);
            } else if (cmd instanceof DeleteVMSnapshotCommand) {
                return execute((DeleteVMSnapshotCommand) cmd);
            } else if (cmd instanceof RevertToVMSnapshotCommand) {
                return execute((RevertToVMSnapshotCommand) cmd);
            } else if (clz == ResizeVolumeCommand.class) {
                return execute((ResizeVolumeCommand) cmd);
            } else if (clz == UnregisterVMCommand.class) {
                return execute((UnregisterVMCommand) cmd);
            } else if (cmd instanceof StorageSubSystemCommand) {
                checkStorageProcessorAndHandlerNfsVersionAttribute((StorageSubSystemCommand) cmd);
                return storageHandler.handleStorageCommands((StorageSubSystemCommand) cmd);
            } else if (clz == ScaleVmCommand.class) {
                return execute((ScaleVmCommand) cmd);
            } else if (clz == PvlanSetupCommand.class) {
                return execute((PvlanSetupCommand) cmd);
            } else if (clz == GetVmIpAddressCommand.class) {
                return execute((GetVmIpAddressCommand) cmd);
            } else if (clz == UnregisterNicCommand.class) {
                answer = execute((UnregisterNicCommand) cmd);
            } else if (clz == GetUnmanagedInstancesCommand.class) {
                answer = execute((GetUnmanagedInstancesCommand) cmd);
            } else if (clz == PrepareUnmanageVMInstanceCommand.class) {
                answer = execute((PrepareUnmanageVMInstanceCommand) cmd);
            } else if (clz == ValidateVcenterDetailsCommand.class) {
                answer = execute((ValidateVcenterDetailsCommand) cmd);
            } else if (clz == SetupPersistentNetworkCommand.class) {
                answer = execute((SetupPersistentNetworkCommand) cmd);
            } else if (clz == GetVmVncTicketCommand.class) {
                answer = execute((GetVmVncTicketCommand) cmd);
            } else if (clz == GetAutoScaleMetricsCommand.class) {
                answer = execute((GetAutoScaleMetricsCommand) cmd);
            } else {
                answer = Answer.createUnsupportedCommandAnswer(cmd);
            }

            if (cmd.getContextParam("checkpoint") != null) {
                answer.setContextParam("checkpoint", cmd.getContextParam("checkpoint"));
            }

            Date doneTime = DateUtil.currentGMTTime();
            mbean.addProp("DoneTime", DateUtil.getDateDisplayString(TimeZone.getDefault(), doneTime));
            mbean.addProp("Answer", _gson.toJson(answer));

            synchronized (this) {
                try {
                    JmxUtil.registerMBean("VMware " + _morHyperHost.getValue(), "Command " + cmdSequence + "-" + cmd.getClass().getSimpleName(), mbean);
                    _cmdMBeans.add(mbean);

                    if (_cmdMBeans.size() >= MazCmdMBean) {
                        PropertyMapDynamicBean mbeanToRemove = _cmdMBeans.get(0);
                        _cmdMBeans.remove(0);

                        JmxUtil.unregisterMBean("VMware " + _morHyperHost.getValue(), "Command " + mbeanToRemove.getProp("Sequence") + "-" + mbeanToRemove.getProp("Name"));
                    }
                } catch (Exception e) {
                    if (s_logger.isTraceEnabled())
                        s_logger.trace("Unable to register JMX monitoring due to exception " + ExceptionUtil.toString(e));
                }
            }

        } finally {
            recycleServiceContext();
            NDC.pop();
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("End executeRequest(), cmd: " + cmd.getClass().getSimpleName());

        return answer;
    }

    private ExecutionResult getSystemVmVersionAndChecksum(String controlIp) {
        ExecutionResult result;
        try {
            result = executeInVR(controlIp, VRScripts.VERSION, null);
            if (!result.isSuccess()) {
                String errMsg = String.format("GetSystemVMVersionCmd on %s failed, message %s", controlIp, result.getDetails());
                s_logger.error(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
        } catch (final Exception e) {
            final String msg = "GetSystemVMVersionCmd failed due to " + e;
            s_logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
        return result;
    }

    private Answer execute(PatchSystemVmCommand cmd) {
        String controlIp = cmd.getAccessDetail((NetworkElementCommand.ROUTER_IP));
        String sysVMName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String homeDir = System.getProperty("user.home");
        File pemFile = new File(homeDir + "/.ssh/id_rsa");
        ExecutionResult result;
        try {
            result = getSystemVmVersionAndChecksum(controlIp);
            FileUtil.scpPatchFiles(controlIp, VRScripts.CONFIG_CACHE_LOCATION, DefaultDomRSshPort, pemFile, systemVmPatchFiles, BASEPATH);
        } catch (CloudRuntimeException e) {
            return new PatchSystemVmAnswer(cmd, e.getMessage());
        }

        final String[] lines = result.getDetails().split("&");
        // TODO: do we fail, or patch anyway??
        if (lines.length != 2) {
            return new PatchSystemVmAnswer(cmd, result.getDetails());
        }

        String scriptChecksum = lines[1].trim();
        String checksum = ChecksumUtil.calculateCurrentChecksum(sysVMName, "vms/cloud-scripts.tgz").trim();

        if (!org.apache.commons.lang3.StringUtils.isEmpty(checksum) && checksum.equals(scriptChecksum) && !cmd.isForced()) {
            String msg = String.format("No change in the scripts checksum, not patching systemVM %s", sysVMName);
            s_logger.info(msg);
            return new PatchSystemVmAnswer(cmd, msg, lines[0], lines[1]);
        }

        Pair<Boolean, String> patchResult = null;
        try {
            patchResult = SshHelper.sshExecute(controlIp, DefaultDomRSshPort, "root",
                    pemFile, null, "/var/cache/cloud/patch-sysvms.sh", 10000, 10000, 600000);
        } catch (Exception e) {
            return new PatchSystemVmAnswer(cmd, e.getMessage());
        }

        String scriptVersion = lines[1];
        if (StringUtils.isNotEmpty(patchResult.second())) {
            String res = patchResult.second().replace("\n", " ");
            String[] output = res.split(":");
            if (output.length != 2) {
                s_logger.warn("Failed to get the latest script version");
            } else {
                scriptVersion = output[1].split(" ")[0];
            }

        }
        if (patchResult.first()) {
            return new PatchSystemVmAnswer(cmd, String.format("Successfully patched systemVM %s ", sysVMName), lines[0], scriptVersion);
        }
        return new PatchSystemVmAnswer(cmd, patchResult.second());

    }

    private Answer execute(SetupPersistentNetworkCommand cmd) {
        VmwareHypervisorHost host = getHyperHost(getServiceContext());
        String hostname = null;
        VmwareContext context = getServiceContext();
        HostMO hostMO = new HostMO(context, host.getMor());

        try {
            prepareNetworkFromNicInfo(hostMO, cmd.getNic(), false, null);
            hostname =  host.getHyperHostName();
        } catch (Exception e) {
            return new SetupPersistentNetworkAnswer(cmd, false, "failed to setup port-group due to: "+ e.getLocalizedMessage());
        }
        return new SetupPersistentNetworkAnswer(cmd, true, hostname);
    }

    /**
     * Check if storage NFS version is already set or needs to be reconfigured.<br>
     * If _storageNfsVersion is not null -> nothing to do, version already set.<br>
     * If _storageNfsVersion is null -> examine StorageSubSystemCommand to get NFS version and set it
     * to the storage processor and storage handler.
     *
     * @param cmd command to execute
     */
    protected void checkStorageProcessorAndHandlerNfsVersionAttribute(StorageSubSystemCommand cmd) {
        if (storageNfsVersion != null)
            return;
        if (cmd instanceof CopyCommand) {
            EnumMap<VmwareStorageProcessorConfigurableFields, Object> params = new EnumMap<VmwareStorageProcessorConfigurableFields, Object>(
                    VmwareStorageProcessorConfigurableFields.class);
            examineStorageSubSystemCommandNfsVersion((CopyCommand) cmd, params);
            params = examineStorageSubSystemCommandFullCloneFlagForVmware((CopyCommand) cmd, params);
            reconfigureProcessorByHandler(params);
        }
    }

    /**
     * Reconfigure processor by handler
     *
     * @param params params
     */
    protected void reconfigureProcessorByHandler(EnumMap<VmwareStorageProcessorConfigurableFields, Object> params) {
        VmwareStorageSubsystemCommandHandler handler = (VmwareStorageSubsystemCommandHandler) storageHandler;
        boolean success = handler.reconfigureStorageProcessor(params);
        if (success) {
            s_logger.info("VmwareStorageProcessor and VmwareStorageSubsystemCommandHandler successfully reconfigured");
        } else {
            s_logger.error("Error while reconfiguring VmwareStorageProcessor and VmwareStorageSubsystemCommandHandler, params=" + _gson.toJson(params));
        }
    }

    /**
     * Examine StorageSubSystem command to get full clone flag, if provided
     *
     * @param cmd    command to execute
     * @param params params
     * @return copy of params including new values, if suitable
     */
    protected EnumMap<VmwareStorageProcessorConfigurableFields, Object> examineStorageSubSystemCommandFullCloneFlagForVmware(CopyCommand cmd,
            EnumMap<VmwareStorageProcessorConfigurableFields, Object> params) {
        EnumMap<VmwareStorageProcessorConfigurableFields, Object> paramsCopy = new EnumMap<VmwareStorageProcessorConfigurableFields, Object>(params);
        HypervisorType hypervisor = cmd.getDestTO().getHypervisorType();
        if (hypervisor != null && hypervisor.equals(HypervisorType.VMware)) {
            DataStoreTO destDataStore = cmd.getDestTO().getDataStore();
            if (destDataStore instanceof PrimaryDataStoreTO) {
                PrimaryDataStoreTO dest = (PrimaryDataStoreTO) destDataStore;
                if (dest.isFullCloneFlag() != null) {
                    paramsCopy.put(VmwareStorageProcessorConfigurableFields.FULL_CLONE_FLAG, dest.isFullCloneFlag().booleanValue());
                }
                if (dest.getDiskProvisioningStrictnessFlag() != null) {
                    paramsCopy.put(VmwareStorageProcessorConfigurableFields.DISK_PROVISIONING_STRICTNESS, dest.getDiskProvisioningStrictnessFlag().booleanValue());
                }
            }
        }
        return paramsCopy;
    }

    /**
     * Examine StorageSubSystem command to get storage NFS version, if provided
     *
     * @param cmd    command to execute
     * @param params params
     */
    protected void examineStorageSubSystemCommandNfsVersion(CopyCommand cmd, EnumMap<VmwareStorageProcessorConfigurableFields, Object> params) {
        DataStoreTO srcDataStore = cmd.getSrcTO().getDataStore();
        boolean nfsVersionFound = false;

        if (srcDataStore instanceof NfsTO) {
            nfsVersionFound = getStorageNfsVersionFromNfsTO((NfsTO) srcDataStore);
        }

        if (nfsVersionFound) {
            params.put(VmwareStorageProcessorConfigurableFields.NFS_VERSION, storageNfsVersion);
        }
    }

    /**
     * Get storage NFS version from NfsTO
     *
     * @param nfsTO nfsTO
     * @return true if NFS version was found and not null, false in other case
     */
    protected boolean getStorageNfsVersionFromNfsTO(NfsTO nfsTO) {
        if (nfsTO != null && nfsTO.getNfsVersion() != null) {
            storageNfsVersion = nfsTO.getNfsVersion();
            return true;
        }
        return false;
    }

    /**
     * Registers the vm to the inventory given the vmx file.
     */
    private void registerVm(String vmName, DatastoreMO dsMo) throws Exception {

        //1st param
        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
        ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
        DatacenterMO dataCenterMo = new DatacenterMO(getServiceContext(), dcMor);
        ManagedObjectReference vmFolderMor = dataCenterMo.getVmFolder();

        //2nd param
        String vmxFilePath = dsMo.searchFileInSubFolders(vmName + ".vmx", false, VmwareManager.s_vmwareSearchExcludeFolder.value());

        // 5th param
        ManagedObjectReference morPool = hyperHost.getHyperHostOwnerResourcePool();

        ManagedObjectReference morTask = getServiceContext().getService().registerVMTask(vmFolderMor, vmxFilePath, vmName, false, morPool, hyperHost.getMor());
        boolean result = getServiceContext().getVimClient().waitForTask(morTask);
        if (!result) {
            throw new Exception("Unable to register vm due to " + TaskMO.getTaskFailureInfo(getServiceContext(), morTask));
        } else {
            getServiceContext().waitForTaskProgressDone(morTask);
        }

    }

    private Answer execute(ResizeVolumeCommand cmd) {
        String path = cmd.getPath();
        String vmName = cmd.getInstanceName();
        long newSize = cmd.getNewSize() / ResourceType.bytesToKiB;
        long oldSize = cmd.getCurrentSize() / ResourceType.bytesToKiB;
        boolean managed = cmd.isManaged();
        String poolUUID = cmd.getPoolUuid();
        String chainInfo = cmd.getChainInfo();
        boolean useWorkerVm = false;

        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        VirtualMachineMO vmMo = null;

        String vmdkDataStorePath = null;

        try {
            if (newSize < oldSize) {
                String errorMsg = String.format("VMware doesn't support shrinking volume from larger size [%s] GB to a smaller size [%s] GB. Can't resize volume of VM [name: %s].",
                        oldSize / Float.valueOf(ResourceType.bytesToMiB), newSize / Float.valueOf(ResourceType.bytesToMiB), vmName);
                s_logger.error(errorMsg);
                throw new Exception(errorMsg);
            } else if (newSize == oldSize) {
                return new ResizeVolumeAnswer(cmd, true, "success", newSize * ResourceType.bytesToKiB);
            }

            if (vmName.equalsIgnoreCase("none")) {
                // OfflineVmwareMigration: we need to refactor the worker vm creation out for use in migration methods as well as here
                // OfflineVmwareMigration: this method is 100 lines and needs refactorring anyway
                // we need to spawn a worker VM to attach the volume to and resize the volume.
                useWorkerVm = true;

                String poolId = cmd.getPoolUuid();

                // OfflineVmwareMigration: refactor for re-use
                // OfflineVmwareMigration: 1. find data(store)
                ManagedObjectReference morDS = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolId);
                DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), morDS);
                vmName = getWorkerName(getServiceContext(), cmd, 0, dsMo);

                s_logger.info("Create worker VM " + vmName);

                // OfflineVmwareMigration: 2. create the worker with access to the data(store)
                vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, dsMo, vmName, null);

                if (vmMo == null) {
                    // OfflineVmwareMigration: don't throw a general Exception but think of a specific one
                    throw new Exception("Unable to create a worker VM for volume resize");
                }

                synchronized (this) {
                    // OfflineVmwareMigration: 3. attach the disk to the worker
                    vmdkDataStorePath = VmwareStorageLayoutHelper.getLegacyDatastorePathFromVmdkFileName(dsMo, path + VMDK_EXTENSION);

                    vmMo.attachDisk(new String[]{vmdkDataStorePath}, morDS);
                }
            }

            // OfflineVmwareMigration: 4. find the (worker-) VM
            // find VM through datacenter (VM is not at the target host yet)
            vmMo = hyperHost.findVmOnPeerHyperHost(vmName);

            if (vmMo == null) {
                String errorMsg = String.format("VM [name: %s] does not exist in VMware datacenter.", vmName);
                s_logger.error(errorMsg);
                throw new Exception(errorMsg);
            }


            if (managed) {
                ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
                ClusterMO clusterMO = new ClusterMO(context, morCluster);

                List<Pair<ManagedObjectReference, String>> lstHosts = clusterMO.getClusterHosts();

                Collections.shuffle(lstHosts, RANDOM);

                Pair<ManagedObjectReference, String> host = lstHosts.get(0);

                HostMO hostMO = new HostMO(context, host.first());
                HostDatastoreSystemMO hostDatastoreSystem = hostMO.getHostDatastoreSystemMO();

                String iScsiName = cmd.get_iScsiName();

                ManagedObjectReference morDS = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, VmwareResource.getDatastoreName(iScsiName));
                DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), morDS);

                _storageProcessor.expandDatastore(hostDatastoreSystem, dsMo);
            }

            boolean volumePathChangeObserved = false;
            boolean datastoreChangeObserved = false;

            Pair<String, String> pathAndChainInfo = getNewPathAndChainInfoInDatastoreCluster(vmMo, path, chainInfo, managed, cmd.get_iScsiName(), poolUUID, cmd.getContextParam(DiskTO.PROTOCOL_TYPE));
            Pair<String, String> poolUUIDandChainInfo = getNewPoolUUIDAndChainInfoInDatastoreCluster(vmMo, path, chainInfo, managed, cmd.get_iScsiName(), poolUUID, cmd.getContextParam(DiskTO.PROTOCOL_TYPE));

            if (pathAndChainInfo != null) {
                volumePathChangeObserved = true;
                path = pathAndChainInfo.first();
                chainInfo = pathAndChainInfo.second();
            }

            if (poolUUIDandChainInfo != null) {
                datastoreChangeObserved = true;
                poolUUID = poolUUIDandChainInfo.first();
                chainInfo = poolUUIDandChainInfo.second();
            }

            // OfflineVmwareMigration: 5. ignore/replace the rest of the try-block; It is the functional bit
            VirtualDisk disk = getDiskAfterResizeDiskValidations(vmMo, path);
            String vmdkAbsFile = getAbsoluteVmdkFile(disk);

            if (vmdkAbsFile != null && !vmdkAbsFile.isEmpty()) {
                vmMo.updateAdapterTypeIfRequired(vmdkAbsFile);
            }

            disk.setCapacityInKB(newSize);

            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

            deviceConfigSpec.setDevice(disk);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();

            vmConfigSpec.getDeviceChange().add(deviceConfigSpec);

            if (!vmMo.configureVm(vmConfigSpec)) {
                throw new Exception(String.format("Failed to configure VM [name: %s] to resize disk.", vmName));
            }

            ResizeVolumeAnswer answer = new ResizeVolumeAnswer(cmd, true, "success", newSize * 1024);
            if (datastoreChangeObserved) {
                answer.setContextParam("datastoreUUID", poolUUID);
                answer.setContextParam("chainInfo", chainInfo);
            }

            if (volumePathChangeObserved) {
                answer.setContextParam("volumePath", path);
                answer.setContextParam("chainInfo", chainInfo);
            }
            return answer;
        } catch (Exception e) {
            String errorMsg = String.format("Failed to resize volume of VM [name: %s] due to: [%s].", vmName, e.getMessage());
            s_logger.error(errorMsg, e);
            return new ResizeVolumeAnswer(cmd, false, errorMsg);
        } finally {
            // OfflineVmwareMigration: 6. check if a worker was used and destroy it if needed
            try {
                if (useWorkerVm) {
                    s_logger.info("Destroy worker VM after volume resize");

                    vmMo.detachDisk(vmdkDataStorePath, false);
                    vmMo.destroy();
                }
            } catch (Throwable e) {
                s_logger.error(String.format("Failed to destroy worker VM [name: %s] due to: [%s].", vmName, e.getMessage()), e);
            }
        }
    }

    private VirtualDisk getDiskAfterResizeDiskValidations(VirtualMachineMO vmMo, String volumePath) throws Exception {
        Pair<VirtualDisk, String> vdisk = vmMo.getDiskDevice(volumePath);
        if (vdisk == null) {
            String errorMsg = String.format("Resize volume of VM [name: %s] failed because disk device [path: %s] doesn't exist.", vmMo.getVmName(), volumePath);
            s_logger.error(errorMsg);
            throw new Exception(errorMsg);
        }

        // IDE virtual disk cannot be re-sized if VM is running
        if (vdisk.second() != null && vdisk.second().toLowerCase().contains("ide")) {
            String errorMsg = String.format("Re-sizing a virtual disk over an IDE controller is not supported in the VMware hypervisor. "
                    + "Please re-try when virtual disk is attached to VM [name: %s] using a SCSI controller.", vmMo.getVmName());
            s_logger.error(errorMsg);
            throw new Exception(errorMsg);
        }

        VirtualDisk disk = vdisk.first();
        if ((VirtualDiskFlatVer2BackingInfo) disk.getBacking() != null && ((VirtualDiskFlatVer2BackingInfo) disk.getBacking()).getParent() != null) {
            String errorMsg = String.format("Resize of volume in VM [name: %s] is not supported because Disk device [path: %s] has Parents: [%s].",
                    vmMo.getVmName(), volumePath, ((VirtualDiskFlatVer2BackingInfo) disk.getBacking()).getParent().getUuid());
            s_logger.error(errorMsg);
            throw new Exception(errorMsg);
        }
        return disk;
    }

    private Pair<String, String> getNewPathAndChainInfoInDatastoreCluster(VirtualMachineMO vmMo, String path, String chainInfo, boolean managed, String iscsiName, String poolUUID, String poolType) throws Exception {
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        if (poolType != null && poolType.equalsIgnoreCase(Storage.StoragePoolType.DatastoreCluster.toString())) {
            VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
            VirtualMachineDiskInfo matchingExistingDisk = getMatchingExistingDiskWithVolumeDetails(diskInfoBuilder, path, chainInfo, managed, iscsiName, poolUUID, hyperHost, context);
            if (diskInfoBuilder != null && matchingExistingDisk != null) {
                String[] diskChain = matchingExistingDisk.getDiskChain();
                DatastoreFile file = new DatastoreFile(diskChain[0]);
                if (!file.getFileBaseName().equalsIgnoreCase(path)) {
                    if (s_logger.isInfoEnabled())
                        s_logger.info("Detected disk-chain top file change on volume: " + path + " -> " + file.getFileBaseName());
                    path = file.getFileBaseName();
                    chainInfo = _gson.toJson(matchingExistingDisk);
                    return new Pair<>(path, chainInfo);
                }
            }
        }
        return null;
    }

    private Pair<String, String> getNewPoolUUIDAndChainInfoInDatastoreCluster(VirtualMachineMO vmMo, String path, String chainInfo, boolean managed, String iscsiName, String poolUUID, String poolType) throws Exception {
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        if (poolType != null && poolType.equalsIgnoreCase(Storage.StoragePoolType.DatastoreCluster.toString())) {
            VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
            VirtualMachineDiskInfo matchingExistingDisk = getMatchingExistingDiskWithVolumeDetails(diskInfoBuilder, path, chainInfo, managed, iscsiName, poolUUID, hyperHost, context);
            if (diskInfoBuilder != null && matchingExistingDisk != null) {
                String[] diskChain = matchingExistingDisk.getDiskChain();
                DatastoreFile file = new DatastoreFile(diskChain[0]);
                DatacenterMO dcMo = new DatacenterMO(hyperHost.getContext(), hyperHost.getHyperHostDatacenter());
                DatastoreMO diskDatastoreMofromVM = new DatastoreMO(context, dcMo.findDatastore(file.getDatastoreName()));
                if (diskDatastoreMofromVM != null) {
                    String actualPoolUuid = diskDatastoreMofromVM.getCustomFieldValue(CustomFieldConstants.CLOUD_UUID);
                    if (!actualPoolUuid.equalsIgnoreCase(poolUUID)) {
                        s_logger.warn(String.format("Volume %s found to be in a different storage pool %s", path, actualPoolUuid));
                        poolUUID = actualPoolUuid;
                        chainInfo = _gson.toJson(matchingExistingDisk);
                        return new Pair<>(poolUUID, chainInfo);
                    }
                }
            }
        }
        return null;
    }

    protected Answer execute(CheckNetworkCommand cmd) {
        // TODO setup portgroup for private network needs to be done here now
        return new CheckNetworkAnswer(cmd, true, "Network Setup check by names is done");
    }

    protected Answer execute(NetworkUsageCommand cmd) {
        if (cmd.isForVpc()) {
            return VPCNetworkUsage(cmd);
        }

        if (cmd.getOption() != null && cmd.getOption().equals("create")) {
            String result = networkUsage(cmd.getPrivateIP(), "create", null);
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
            return answer;
        }
        long[] stats = getNetworkStats(cmd.getPrivateIP(), null);

        NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
        return answer;
    }

    protected NetworkUsageAnswer VPCNetworkUsage(NetworkUsageCommand cmd) {
        String privateIp = cmd.getPrivateIP();
        String option = cmd.getOption();
        String publicIp = cmd.getGatewayIP();
        String vpcCIDR = cmd.getVpcCIDR();

        final long[] stats = getVPCNetworkStats(privateIp, publicIp, option, vpcCIDR);
        return new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
    }

    protected long[] getVPCNetworkStats(String privateIp, String publicIp, String option, String vpcCIDR) {
        String args = "-l " + publicIp + " ";
        if (option.equals("get")) {
            args += "-g";
        } else if (option.equals("create")) {
            args += "-c";
            args += " -v " + vpcCIDR;
        } else if (option.equals("reset")) {
            args += "-r";
        } else if (option.equals("vpn")) {
            args += "-n";
        } else if (option.equals("remove")) {
            args += "-d";
        } else {
            return new long[2];
        }

        ExecutionResult callResult = executeInVR(privateIp, "vpc_netusage.sh", args);

        if (!callResult.isSuccess()) {
            s_logger.error("Unable to execute NetworkUsage command on DomR (" + privateIp + "), domR may not be ready yet. failure due to " + callResult.getDetails());
        }

        if (option.equals("get") || option.equals("vpn")) {
            String result = callResult.getDetails();
            if (result == null || result.isEmpty()) {
                s_logger.error(" vpc network usage get returns empty ");
            }
            long[] stats = new long[2];
            if (result != null) {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += Long.parseLong(splitResult[i++]);
                    stats[1] += Long.parseLong(splitResult[i++]);
                }
                return stats;
            }
        }
        return new long[2];
    }

    protected long[] getNetworkLbStats(String privateIp, String publicIp, Integer port) {
        String args = publicIp + " " + port;
        ExecutionResult callResult = executeInVR(privateIp, "get_haproxy_stats.sh", args);

        String result = callResult.getDetails();
        if (!Boolean.TRUE.equals(callResult.isSuccess())) {
            s_logger.error(String.format("Unable to get network loadbalancer stats on DomR (%s), domR may not be ready yet. failure due to %s", privateIp, callResult.getDetails()));
            result = null;
        } else if (result == null || result.isEmpty()) {
            s_logger.error("Get network loadbalancer stats returns empty result");
        }
        long[] stats = new long[1];
        if (result != null) {
            final String[] splitResult = result.split(",");
            stats[0] += Long.parseLong(splitResult[0]);
        }
        return stats;
    }

    protected Answer execute(GetAutoScaleMetricsCommand cmd) {
        Long bytesSent;
        Long bytesReceived;
        if (cmd.isForVpc()) {
            long[] stats = getVPCNetworkStats(cmd.getPrivateIP(), cmd.getPublicIP(), "get", "");
            bytesSent = stats[0];
            bytesReceived = stats[1];
        } else {
            long [] stats = getNetworkStats(cmd.getPrivateIP(), cmd.getPublicIP());
            bytesSent = stats[0];
            bytesReceived = stats[1];
        }

        long [] lbStats = getNetworkLbStats(cmd.getPrivateIP(), cmd.getPublicIP(), cmd.getPort());
        long lbConnections = lbStats[0];

        List<VirtualRouterAutoScale.AutoScaleMetricsValue> values = new ArrayList<>();

        for (VirtualRouterAutoScale.AutoScaleMetrics metrics : cmd.getMetrics()) {
            switch (metrics.getCounter()) {
                case NETWORK_RECEIVED_AVERAGE_MBPS:
                    values.add(new VirtualRouterAutoScale.AutoScaleMetricsValue(metrics, VirtualRouterAutoScale.AutoScaleValueType.AGGREGATED_VM_GROUP,
                            Double.valueOf(bytesReceived) / VirtualRouterAutoScale.MBITS_TO_BYTES));
                    break;
                case NETWORK_TRANSMIT_AVERAGE_MBPS:
                    values.add(new VirtualRouterAutoScale.AutoScaleMetricsValue(metrics, VirtualRouterAutoScale.AutoScaleValueType.AGGREGATED_VM_GROUP,
                            Double.valueOf(bytesSent) / VirtualRouterAutoScale.MBITS_TO_BYTES));
                    break;
                case LB_AVERAGE_CONNECTIONS:
                    values.add(new VirtualRouterAutoScale.AutoScaleMetricsValue(metrics, VirtualRouterAutoScale.AutoScaleValueType.INSTANT_VM, Double.valueOf(lbConnections)));
                    break;
            }
        }

        return new GetAutoScaleMetricsAnswer(cmd, true, values);
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String filePath, String fileName, String content) {
        File keyFile = getSystemVmKeyFile();
        try {
            SshHelper.scpTo(routerIp, 3922, "root", keyFile, null, filePath, content.getBytes("UTF-8"), fileName, null);
        } catch (Exception e) {
            s_logger.warn("Fail to create file " + filePath + fileName + " in VR " + routerIp, e);
            return new ExecutionResult(false, e.getMessage());
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
        //Update IP used to access router
        cmd.setRouterAccessIp(getRouterSshControlIp(cmd));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand) cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand) cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand) cmd);
        } else if (cmd instanceof SetNetworkACLCommand) {
            return prepareNetworkElementCommand((SetNetworkACLCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        if (cmd instanceof IpAssocCommand && !(cmd instanceof IpAssocVpcCommand)) {
            return cleanupNetworkElementCommand((IpAssocCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

    //
    // list IP with eth devices
    //  ifconfig ethx |grep -B1 "inet addr" | awk '{ if ( $1 == "inet" ) { print $2 } else if ( $2 == "Link" ) { printf "%s:" ,$1 } }'
    //     | awk -F: '{ print $1 ": " $3 }'
    //
    // returns
    //      eth0:xx.xx.xx.xx
    //
    //
    private int findRouterEthDeviceIndex(String domrName, String routerIp, String mac) throws Exception {
        File keyFile = getSystemVmKeyFile();
        s_logger.info("findRouterEthDeviceIndex. mac: " + mac);
        ArrayList<String> skipInterfaces = new ArrayList<String>(Arrays.asList("all", "default", "lo"));

        // when we dynamically plug in a new NIC into virtual router, it may take time to show up in guest OS
        // we use a waiting loop here as a workaround to synchronize activities in systems
        long startTick = System.currentTimeMillis();
        long waitTimeoutMillis = VmwareManager.s_vmwareNicHotplugWaitTimeout.value();
        while (System.currentTimeMillis() - startTick < waitTimeoutMillis) {

            // TODO : this is a temporary very inefficient solution, will refactor it later
            Pair<Boolean, String> result = SshHelper.sshExecute(routerIp, DefaultDomRSshPort, "root", keyFile, null, "ls /proc/sys/net/ipv4/conf");
            if (result.first()) {
                String[] tokens = result.second().split("\\s+");
                for (String token : tokens) {
                    if (!(skipInterfaces.contains(token))) {
                        String cmd = String.format("ip address show %s | grep link/ether | sed -e 's/^[ \t]*//' | cut -d' ' -f2", token);

                        if (s_logger.isDebugEnabled())
                            s_logger.debug("Run domr script " + cmd);
                        Pair<Boolean, String> result2 = SshHelper.sshExecute(routerIp, DefaultDomRSshPort, "root", keyFile, null,
                                // TODO need to find the dev index inside router based on IP address
                                cmd);
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("result: " + result2.first() + ", output: " + result2.second());

                        if (result2.first() && result2.second().trim().equalsIgnoreCase(mac.trim())) {
                            return Integer.parseInt(token.substring(3));
                        } else {
                            skipInterfaces.add(token);
                        }
                    }
                }
            }

            s_logger.warn("can not find intereface associated with mac: " + mac + ", guest OS may still at loading state, retry...");

            try {
                Thread.currentThread();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                s_logger.debug("[ignored] interrupted while trying to get mac.");
            }
        }

        return -1;
    }

    private VirtualDevice findVirtualNicDevice(VirtualMachineMO vmMo, String mac) throws Exception {

        VirtualDevice[] nics = vmMo.getNicDevices();
        for (VirtualDevice nic : nics) {
            if (nic instanceof VirtualEthernetCard) {
                if (((VirtualEthernetCard) nic).getMacAddress().equals(mac))
                    return nic;
            }
        }
        return null;
    }

    protected ExecutionResult prepareNetworkElementCommand(SetupGuestNetworkCommand cmd) {
        NicTO nic = cmd.getNic();
        String routerIp = getRouterSshControlIp(cmd);
        String domrName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

        try {
            int ethDeviceNum = findRouterEthDeviceIndex(domrName, routerIp, nic.getMac());
            nic.setDeviceId(ethDeviceNum);
        } catch (Exception e) {
            String msg = "Prepare SetupGuestNetwork failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = getRouterSshControlIp(cmd);

        try {
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {

                int ethDeviceNum = findRouterEthDeviceIndex(routerName, routerIp, ip.getVifMacAddress());
                if (ethDeviceNum < 0) {
                    if (ip.isAdd()) {
                        throw new InternalErrorException("Failed to find DomR VIF to associate/disassociate IP with.");
                    } else {
                        s_logger.debug("VIF to deassociate IP with does not exist, return success");
                        continue;
                    }
                }

                ip.setNicDevId(ethDeviceNum);
            }
        } catch (Exception e) {
            s_logger.error("Prepare Ip Assoc failure on applying one ip due to exception:  ", e);
            return new ExecutionResult(false, e.toString());
        }

        return new ExecutionResult(true, null);
    }

    protected ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = getRouterSshControlIp(cmd);
        IpAddressTO pubIp = cmd.getIpAddress();

        try {
            int ethDeviceNum = findRouterEthDeviceIndex(routerName, routerIp, pubIp.getVifMacAddress());
            pubIp.setNicDevId(ethDeviceNum);
        } catch (Exception e) {
            String msg = "Prepare Ip SNAT failure due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, e.toString());
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(SetNetworkACLCommand cmd) {
        NicTO nic = cmd.getNic();
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = getRouterSshControlIp(cmd);

        try {
            int ethDeviceNum = findRouterEthDeviceIndex(routerName, routerIp, nic.getMac());
            nic.setDeviceId(ethDeviceNum);
        } catch (Exception e) {
            String msg = "Prepare SetNetworkACL failed due to " + e.toString();
            s_logger.error(msg, e);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }

    private PlugNicAnswer execute(PlugNicCommand cmd) {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Executing resource PlugNicCommand " + _gson.toJson(cmd));
        }

        try {
            VirtualEthernetCardType nicDeviceType = null;
            if (cmd.getDetails() != null) {
                nicDeviceType = VirtualEthernetCardType.valueOf(cmd.getDetails().get("nicAdapter"));
            }
            plugNicCommandInternal(cmd.getVmName(), nicDeviceType, cmd.getNic(), cmd.getVMType());
            return new PlugNicAnswer(cmd, true, "success");
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
            return new PlugNicAnswer(cmd, false, "Unable to execute PlugNicCommand due to " + e.toString());
        }
    }

    private void plugNicCommandInternal(String vmName, VirtualEthernetCardType nicDeviceType, NicTO nicTo, VirtualMachine.Type vmType) throws Exception {
        getServiceContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);

        VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);

        if (vmMo == null) {
            if (hyperHost instanceof HostMO) {
                ClusterMO clusterMo = new ClusterMO(hyperHost.getContext(), ((HostMO) hyperHost).getParentMor());
                vmMo = clusterMo.findVmOnHyperHost(vmName);
            }
        }

        if (vmMo == null) {
            String msg = "Router " + vmName + " no longer exists to execute PlugNic command";
            s_logger.error(msg);
            throw new Exception(msg);
        }

            /*
            if(!isVMWareToolsInstalled(vmMo)){
                String errMsg = "vmware tools is not installed or not running, cannot add nic to vm " + vmName;
                s_logger.debug(errMsg);
                return new PlugNicAnswer(cmd, false, "Unable to execute PlugNicCommand due to " + errMsg);
            }
             */
        // Fallback to E1000 if no specific nicAdapter is passed
        if (nicDeviceType == null) {
            nicDeviceType = VirtualEthernetCardType.E1000;
        }

        // find a usable device number in VMware environment
        VirtualDevice[] nicDevices = vmMo.getSortedNicDevices();
        int deviceNumber = -1;
        for (VirtualDevice device : nicDevices) {
            if (device.getUnitNumber() > deviceNumber)
                deviceNumber = device.getUnitNumber();
        }
        deviceNumber++;

        VirtualDevice nic;
        Pair<ManagedObjectReference, String> networkInfo = prepareNetworkFromNicInfo(vmMo.getRunningHost(), nicTo, false, vmType);
        String dvSwitchUuid = null;
        if (VmwareHelper.isDvPortGroup(networkInfo.first())) {
            ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
            DatacenterMO dataCenterMo = new DatacenterMO(context, dcMor);
            ManagedObjectReference dvsMor = dataCenterMo.getDvSwitchMor(networkInfo.first());
            dvSwitchUuid = dataCenterMo.getDvSwitchUuid(dvsMor);
            s_logger.info("Preparing NIC device on dvSwitch : " + dvSwitchUuid);
            nic = VmwareHelper.prepareDvNicDevice(vmMo, networkInfo.first(), nicDeviceType, networkInfo.second(), dvSwitchUuid,
                    nicTo.getMac(), deviceNumber + 1, true, true);
        } else {
            s_logger.info("Preparing NIC device on network " + networkInfo.second());
            nic = VmwareHelper.prepareNicDevice(vmMo, networkInfo.first(), nicDeviceType, networkInfo.second(),
                    nicTo.getMac(), deviceNumber + 1, true, true);
        }

        configureNicDevice(vmMo, nic, VirtualDeviceConfigSpecOperation.ADD, "PlugNicCommand");
    }

    private ReplugNicAnswer execute(ReplugNicCommand cmd) {
        getServiceContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
        VmwareContext context = getServiceContext();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);

            String vmName = cmd.getVmName();
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);

            if (vmMo == null) {
                if (hyperHost instanceof HostMO) {
                    ClusterMO clusterMo = new ClusterMO(hyperHost.getContext(), ((HostMO) hyperHost).getParentMor());
                    vmMo = clusterMo.findVmOnHyperHost(vmName);
                }
            }

            if (vmMo == null) {
                String msg = "Router " + vmName + " no longer exists to execute ReplugNic command";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            /*
            if(!isVMWareToolsInstalled(vmMo)){
                String errMsg = "vmware tools is not installed or not running, cannot add nic to vm " + vmName;
                s_logger.debug(errMsg);
                return new PlugNicAnswer(cmd, false, "Unable to execute PlugNicCommand due to " + errMsg);
            }
             */
            // Fallback to E1000 if no specific nicAdapter is passed
            VirtualEthernetCardType nicDeviceType = VirtualEthernetCardType.E1000;
            Map<String, String> details = cmd.getDetails();
            if (details != null) {
                nicDeviceType = VirtualEthernetCardType.valueOf((String) details.get("nicAdapter"));
            }

            NicTO nicTo = cmd.getNic();

            VirtualDevice nic = findVirtualNicDevice(vmMo, nicTo.getMac());
            if (nic == null) {
                return new ReplugNicAnswer(cmd, false, "Nic to replug not found");
            }

            Pair<ManagedObjectReference, String> networkInfo = prepareNetworkFromNicInfo(vmMo.getRunningHost(), nicTo, false, cmd.getVMType());
            String dvSwitchUuid = null;
            if (VmwareHelper.isDvPortGroup(networkInfo.first())) {
                ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                DatacenterMO dataCenterMo = new DatacenterMO(context, dcMor);
                ManagedObjectReference dvsMor = dataCenterMo.getDvSwitchMor(networkInfo.first());
                dvSwitchUuid = dataCenterMo.getDvSwitchUuid(dvsMor);
                s_logger.info("Preparing NIC device on dvSwitch : " + dvSwitchUuid);
                VmwareHelper.updateDvNicDevice(nic, networkInfo.first(), dvSwitchUuid);
            } else {
                s_logger.info("Preparing NIC device on network " + networkInfo.second());

                VmwareHelper.updateNicDevice(nic, networkInfo.first(), networkInfo.second());
            }

            configureNicDevice(vmMo, nic, VirtualDeviceConfigSpecOperation.EDIT, "ReplugNicCommand");

            return new ReplugNicAnswer(cmd, true, "success");
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
            return new ReplugNicAnswer(cmd, false, "Unable to execute ReplugNicCommand due to " + e.toString());
        }
    }

    private UnPlugNicAnswer execute(UnPlugNicCommand cmd) {
        VmwareContext context = getServiceContext();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);

            String vmName = cmd.getVmName();
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);

            if (vmMo == null) {
                if (hyperHost instanceof HostMO) {
                    ClusterMO clusterMo = new ClusterMO(hyperHost.getContext(), ((HostMO) hyperHost).getParentMor());
                    vmMo = clusterMo.findVmOnHyperHost(vmName);
                }
            }

            if (vmMo == null) {
                String msg = "VM " + vmName + " no longer exists to execute UnPlugNic command";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            /*
            if(!isVMWareToolsInstalled(vmMo)){
                String errMsg = "vmware tools not installed or not running, cannot remove nic from vm " + vmName;
                s_logger.debug(errMsg);
                return new UnPlugNicAnswer(cmd, false, "Unable to execute unPlugNicCommand due to " + errMsg);
            }
             */
            VirtualDevice nic = findVirtualNicDevice(vmMo, cmd.getNic().getMac());
            if (nic == null) {
                return new UnPlugNicAnswer(cmd, true, "success");
            }
            configureNicDevice(vmMo, nic, VirtualDeviceConfigSpecOperation.REMOVE, "unplugNicCommand");

            return new UnPlugNicAnswer(cmd, true, "success");
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
            return new UnPlugNicAnswer(cmd, false, "Unable to execute unPlugNicCommand due to " + e.toString());
        }
    }

    private void plugPublicNic(VirtualMachineMO vmMo, final String vlanId, final IpAddressTO ipAddressTO) throws Exception {
        // TODO : probably need to set traffic shaping
        Pair<ManagedObjectReference, String> networkInfo = null;
        VirtualSwitchType vSwitchType = VirtualSwitchType.StandardVirtualSwitch;
        if (_publicTrafficInfo != null) {
            vSwitchType = _publicTrafficInfo.getVirtualSwitchType();
        }
        /** FIXME We have no clue which network this nic is on and that means that we can't figure out the BroadcastDomainType
         *  so we assume that it's VLAN for now
         */
        if (VirtualSwitchType.StandardVirtualSwitch == vSwitchType) {
            networkInfo = HypervisorHostHelper.prepareNetwork(_publicTrafficInfo.getVirtualSwitchName(),
                    "cloud.public", vmMo.getRunningHost(), vlanId, ipAddressTO.getNetworkRate(), null,
                    _opsTimeout, true, BroadcastDomainType.Vlan, null, null);
        } else {
            networkInfo =
                    HypervisorHostHelper.prepareNetwork(_publicTrafficInfo.getVirtualSwitchName(), "cloud.public", vmMo.getRunningHost(), vlanId, null, ipAddressTO.getNetworkRate(), null,
                            _opsTimeout, vSwitchType, _portsPerDvPortGroup, null, false, BroadcastDomainType.Vlan, _vsmCredentials, null);
        }

        int nicIndex = allocPublicNicIndex(vmMo);

        try {
            VirtualDevice[] nicDevices = vmMo.getSortedNicDevices();

            VirtualEthernetCard device = (VirtualEthernetCard) nicDevices[nicIndex];

            if (VirtualSwitchType.StandardVirtualSwitch == vSwitchType) {
                VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
                nicBacking.setDeviceName(networkInfo.second());
                nicBacking.setNetwork(networkInfo.first());
                device.setBacking(nicBacking);
            } else {
                HostMO hostMo = vmMo.getRunningHost();
                DatacenterMO dataCenterMo = new DatacenterMO(hostMo.getContext(), hostMo.getHyperHostDatacenter());
                device.setBacking(dataCenterMo.getDvPortBackingInfo(networkInfo));
            }

            configureNicDevice(vmMo, device, VirtualDeviceConfigSpecOperation.EDIT, "plugPublicNic");
        } catch (Exception e) {

            // restore allocation mask in case of exceptions
            String nicMasksStr = vmMo.getCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK);
            int nicMasks = Integer.parseInt(nicMasksStr);
            nicMasks &= ~(1 << nicIndex);
            vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK, String.valueOf(nicMasks));

            throw e;
        }
    }

    private int allocPublicNicIndex(VirtualMachineMO vmMo) throws Exception {
        String nicMasksStr = vmMo.getCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK);
        if (nicMasksStr == null || nicMasksStr.isEmpty()) {
            throw new Exception("Could not find NIC allocation info");
        }

        int nicMasks = Integer.parseInt(nicMasksStr);
        VirtualDevice[] nicDevices = vmMo.getNicDevices();
        for (int i = 3; i < nicDevices.length; i++) {
            if ((nicMasks & (1 << i)) == 0) {
                nicMasks |= (1 << i);
                vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK, String.valueOf(nicMasks));
                return i;
            }
        }

        throw new Exception("Could not allocate a free public NIC");
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        VmwareContext context = getServiceContext();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);

            IpAddressTO[] ips = cmd.getIpAddresses();
            String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
            String controlIp = VmwareResource.getRouterSshControlIp(cmd);

            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(routerName);

            // command may sometimes be redirect to a wrong host, we relax
            // the check and will try to find it within cluster
            if (vmMo == null) {
                if (hyperHost instanceof HostMO) {
                    ClusterMO clusterMo = new ClusterMO(hyperHost.getContext(), ((HostMO) hyperHost).getParentMor());
                    vmMo = clusterMo.findVmOnHyperHost(routerName);
                }
            }

            if (vmMo == null) {
                String msg = "Router " + routerName + " no longer exists to execute IPAssoc command";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            for (IpAddressTO ip : ips) {
                /**
                 * TODO support other networks
                 */
                URI broadcastUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
                if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
                    throw new InternalErrorException("Unable to assign a public IP to a VIF on network " + ip.getBroadcastUri());
                }
                String vlanId = BroadcastDomainType.getValue(broadcastUri);

                String publicNetworkName = HypervisorHostHelper.getPublicNetworkNamePrefix(vlanId);
                Pair<Integer, VirtualDevice> publicNicInfo = vmMo.getNicDeviceIndex(publicNetworkName);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Find public NIC index, public network name: " + publicNetworkName + ", index: " + publicNicInfo.first());
                }

                boolean addVif = false;
                if (ip.isAdd() && publicNicInfo.first() == -1) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Plug new NIC to associate" + controlIp + " to " + ip.getPublicIp());
                    }
                    addVif = true;
                }

                if (addVif) {
                    NicTO nicTO = ip.getNicTO();
                    VirtualEthernetCardType nicDeviceType = null;
                    if (ip.getDetails() != null) {
                        nicDeviceType = VirtualEthernetCardType.valueOf(ip.getDetails().get("nicAdapter"));
                    }
                    plugNicCommandInternal(routerName, nicDeviceType, nicTO, VirtualMachine.Type.DomainRouter);
                    publicNicInfo = vmMo.getNicDeviceIndex(publicNetworkName);
                    if (publicNicInfo.first() >= 0) {
                        networkUsage(controlIp, "addVif", "eth" + publicNicInfo.first());
                    }
                }

                if (publicNicInfo.first() < 0) {
                    String msg = "Failed to find DomR VIF to associate/disassociate IP with.";
                    s_logger.error(msg);
                    throw new InternalErrorException(msg);
                }
                ip.setNicDevId(publicNicInfo.first());
                ip.setNewNic(addVif);
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: " + e.toString() + " will shortcut rest of IPAssoc commands", e);
            return new ExecutionResult(false, e.toString());
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult cleanupNetworkElementCommand(IpAssocCommand cmd) {
        VmwareContext context = getServiceContext();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            IpAddressTO[] ips = cmd.getIpAddresses();
            String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);

            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(routerName);
            // command may sometimes be redirected to a wrong host, we relax
            // the check and will try to find it within datacenter
            if (vmMo == null) {
                if (hyperHost instanceof HostMO) {
                    final DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
                    vmMo = dcMo.findVm(routerName);
                }
            }

            if (vmMo == null) {
                String msg = String.format("Router %s no longer exists to execute IPAssoc command ", routerName);
                s_logger.error(msg);
                throw new Exception(msg);
            }
            final String lastIp = cmd.getAccessDetail(NetworkElementCommand.NETWORK_PUB_LAST_IP);
            for (IpAddressTO ip : ips) {
                if (ip.isAdd() || lastIp.equalsIgnoreCase("false")) {
                    continue;
                }
                Pair<VirtualDevice, Integer> nicInfo = getVirtualDevice(vmMo, ip);

                if (nicInfo.second() == 2) {
                    return new ExecutionResult(true, "Not removing eth2 in network VR because it is the public NIC of source NAT");
                }
                if (nicInfo.first() == null) {
                    return new ExecutionResult(false, "Couldn't find NIC");
                }
                configureNicDevice(vmMo, nicInfo.first(), VirtualDeviceConfigSpecOperation.REMOVE, "unplugNicCommand");
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception: " + e.toString() + " will shortcut rest of IPAssoc commands", e);
            return new ExecutionResult(false, e.toString());
        }
        return new ExecutionResult(true, null);
    }

    private Pair<VirtualDevice, Integer> getVirtualDevice(VirtualMachineMO vmMo, IpAddressTO ip) throws Exception {
        NicTO nicTO = ip.getNicTO();
        URI broadcastUri = BroadcastDomainType.fromString(ip.getBroadcastUri());
        if (BroadcastDomainType.getSchemeValue(broadcastUri) != BroadcastDomainType.Vlan) {
            throw new InternalErrorException(String.format("Unable to assign a public IP to a VIF on network %s", ip.getBroadcastUri()));
        }
        String vlanId = BroadcastDomainType.getValue(broadcastUri);

        String publicNetworkName = HypervisorHostHelper.getPublicNetworkNamePrefix(vlanId);
        Pair<Integer, VirtualDevice> publicNicInfo = vmMo.getNicDeviceIndex(publicNetworkName);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Find public NIC index, public network name: %s , index: %s", publicNetworkName, publicNicInfo.first()));
        }

        return new Pair<>(findVirtualNicDevice(vmMo, nicTO.getMac()), publicNicInfo.first());
    }

    private void configureNicDevice(VirtualMachineMO vmMo, VirtualDevice nic, VirtualDeviceConfigSpecOperation operation, String commandName) throws Exception {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(nic);
        deviceConfigSpec.setOperation(operation);


        vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
        if (!vmMo.configureVm(vmConfigSpec)) {
            throw new Exception(String.format("Failed to configure devices when running %s", commandName));
        }
    }

    @Override
    public ExecutionResult executeInVR(String routerIP, String script, String args) {
        return executeInVR(routerIP, script, args, VRScripts.VR_SCRIPT_EXEC_TIMEOUT);
    }

    @Override
    public ExecutionResult executeInVR(String routerIP, String script, String args, Duration timeout) {
        Pair<Boolean, String> result;

        //TODO: Password should be masked, cannot output to log directly
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Run command on VR: " + routerIP + ", script: " + script + " with args: " + args);
        }

        try {
            result = SshHelper.sshExecute(routerIP, DefaultDomRSshPort, "root", getSystemVmKeyFile(), null, "/opt/cloud/bin/" + script + " " + args,
                    VRScripts.CONNECTION_TIMEOUT, VRScripts.CONNECTION_TIMEOUT, timeout);
        } catch (Exception e) {
            String msg = "Command failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg);
            result = new Pair<Boolean, String>(false, msg);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(script + " execution result: " + result.first().toString());
        }
        return new ExecutionResult(result.first(), result.second());
    }

    protected CheckSshAnswer execute(CheckSshCommand cmd) {
        String vmName = cmd.getName();
        String privateIp = cmd.getIp();
        int cmdPort = cmd.getPort();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port, " + privateIp + ":" + cmdPort);
        }

        String errorMessage = "Can not ping System VM [%s], due to: [%s].";
        try {
            String result = connect(cmd.getName(), privateIp, cmdPort);
            if (result != null) {
                s_logger.error(String.format(errorMessage, vmName, result));
                return new CheckSshAnswer(cmd, String.format(errorMessage, vmName, result));
            }
        } catch (Exception e) {
            s_logger.error(String.format(errorMessage, vmName, e.getMessage()), e);
            return new CheckSshAnswer(cmd, e);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping command port succeeded for vm " + vmName);
        }

        if (VirtualMachineName.isValidRouterName(vmName)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Execute network usage setup command on " + vmName);
            }
            networkUsage(privateIp, "create", null);
        }

        return new CheckSshAnswer(cmd);
    }

    private DiskTO[] validateDisks(DiskTO[] disks) {
        List<DiskTO> validatedDisks = new ArrayList<DiskTO>();

        for (DiskTO vol : disks) {
            if (vol.getType() != Volume.Type.ISO) {
                VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
                DataStoreTO primaryStore = volumeTO.getDataStore();
                if (primaryStore.getUuid() != null && !primaryStore.getUuid().isEmpty()) {
                    validatedDisks.add(vol);
                }
            } else if (vol.getType() == Volume.Type.ISO) {
                TemplateObjectTO templateTO = (TemplateObjectTO) vol.getData();
                if (templateTO.getPath() != null && !templateTO.getPath().isEmpty()) {
                    validatedDisks.add(vol);
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Drop invalid disk option, volumeTO: " + _gson.toJson(vol));
                }
            }
        }
        Collections.sort(validatedDisks, (d1, d2) -> d1.getDiskSeq().compareTo(d2.getDiskSeq()));
        return validatedDisks.toArray(new DiskTO[0]);
    }

    private static DiskTO getIsoDiskTO(DiskTO[] disks) {
        for (DiskTO vol : disks) {
            if (vol.getType() == Volume.Type.ISO) {
                return vol;
            }
        }
        return null;
    }

    protected ScaleVmAnswer execute(ScaleVmCommand cmd) {
        VmwareContext context = getServiceContext();
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            int ramMb = getReservedMemoryMb(vmSpec);
            long hotaddIncrementSizeInMb;
            long hotaddMemoryLimitInMb;
            long requestedMaxMemoryInMb = vmSpec.getMaxRam() / (1024 * 1024);

            // Check if VM is really running on hypervisor host
            if (getVmPowerState(vmMo) != PowerState.PowerOn) {
                throw new CloudRuntimeException("Found that the VM " + vmMo.getVmName() + " is not running. Unable to scale-up this VM");
            }

            // Check max hot add limit
            hotaddIncrementSizeInMb = vmMo.getHotAddMemoryIncrementSizeInMb();
            hotaddMemoryLimitInMb = vmMo.getHotAddMemoryLimitInMb();
            if (requestedMaxMemoryInMb > hotaddMemoryLimitInMb) {
                throw new CloudRuntimeException("Memory of VM " + vmMo.getVmName() + " cannot be scaled to " + requestedMaxMemoryInMb + "MB."
                        + " Requested memory limit is beyond the hotadd memory limit for this VM at the moment is " + hotaddMemoryLimitInMb + "MB.");
            }

            // Check increment is multiple of increment size
            long reminder = hotaddIncrementSizeInMb > 0 ? requestedMaxMemoryInMb % hotaddIncrementSizeInMb : 0;
            if (reminder != 0) {
                requestedMaxMemoryInMb = requestedMaxMemoryInMb + hotaddIncrementSizeInMb - reminder;
            }

            // Check if license supports the feature
            VmwareHelper.isFeatureLicensed(hyperHost, FeatureKeyConstants.HOTPLUG);
            VmwareHelper.setVmScaleUpConfig(vmConfigSpec, vmSpec.getCpus(), vmSpec.getMaxSpeed(), getReservedCpuMHZ(vmSpec), (int) requestedMaxMemoryInMb, ramMb,
                    vmSpec.getLimitCpuUse());

            if (!vmMo.configureVm(vmConfigSpec)) {
                throw new Exception("Unable to execute ScaleVmCommand");
            }
        } catch (Exception e) {
            s_logger.error(String.format("ScaleVmCommand failed due to: [%s].", VmwareHelper.getExceptionMessage(e)), e);
            return new ScaleVmAnswer(cmd, false, String.format("Unable to execute ScaleVmCommand due to: [%s].", e.toString()));
        }
        return new ScaleVmAnswer(cmd, true, null);
    }

    protected void ensureDiskControllers(VirtualMachineMO vmMo, Pair<String, String> controllerInfo) throws Exception {
        if (vmMo == null) {
            return;
        }

        String msg;
        String rootDiskController = controllerInfo.first();
        String dataDiskController = controllerInfo.second();
        String scsiDiskController;
        String recommendedDiskController = null;

        if (VmwareHelper.isControllerOsRecommended(dataDiskController) || VmwareHelper.isControllerOsRecommended(rootDiskController)) {
            recommendedDiskController = vmMo.getRecommendedDiskController(null);
        }
        scsiDiskController = HypervisorHostHelper.getScsiController(new Pair<String, String>(rootDiskController, dataDiskController), recommendedDiskController);
        if (scsiDiskController == null) {
            return;
        }

        vmMo.getScsiDeviceControllerKeyNoException();
        // This VM needs SCSI controllers.
        // Get count of existing scsi controllers. Helps not to attempt to create more than the maximum allowed 4
        // Get maximum among the bus numbers in use by scsi controllers. Safe to pick maximum, because we always go sequential allocating bus numbers.
        Ternary<Integer, Integer, DiskControllerType> scsiControllerInfo = vmMo.getScsiControllerInfo();
        int requiredNumScsiControllers = VmwareHelper.MAX_SCSI_CONTROLLER_COUNT - scsiControllerInfo.first();
        int availableBusNum = scsiControllerInfo.second() + 1; // method returned current max. bus number

        if (DiskControllerType.getType(scsiDiskController) != scsiControllerInfo.third()) {
            s_logger.debug(String.format("Change controller type from: %s to: %s", scsiControllerInfo.third().toString(),
                    scsiDiskController));
            vmMo.tearDownDevices(new Class<?>[]{VirtualSCSIController.class});
            vmMo.addScsiDeviceControllers(DiskControllerType.getType(scsiDiskController));
            return;
        }

        if (requiredNumScsiControllers == 0) {
            return;
        }
        if (scsiControllerInfo.first() > 0) {
            // For VMs which already have a SCSI controller, do NOT attempt to add any more SCSI controllers & return the sub type.
            // For Legacy VMs would have only 1 LsiLogic Parallel SCSI controller, and doesn't require more.
            // For VMs created post device ordering support, 4 SCSI subtype controllers are ensured during deployment itself. No need to add more.
            // For fresh VM deployment only, all required controllers should be ensured.
            return;
        }
        ensureScsiDiskControllers(vmMo, scsiDiskController, requiredNumScsiControllers, availableBusNum);
    }

    private void ensureScsiDiskControllers(VirtualMachineMO vmMo, String scsiDiskController, int requiredNumScsiControllers, int availableBusNum) throws Exception {
        // Pick the sub type of scsi
        if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.pvscsi) {
            if (!vmMo.isPvScsiSupported()) {
                String msg = "This VM doesn't support Vmware Paravirtual SCSI controller for virtual disks, because the virtual hardware version is less than 7.";
                throw new Exception(msg);
            }
            vmMo.ensurePvScsiDeviceController(requiredNumScsiControllers, availableBusNum);
        } else if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.lsisas1068) {
            vmMo.ensureLsiLogicSasDeviceControllers(requiredNumScsiControllers, availableBusNum);
        } else if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.buslogic) {
            vmMo.ensureBusLogicDeviceControllers(requiredNumScsiControllers, availableBusNum);
        } else if (DiskControllerType.getType(scsiDiskController) == DiskControllerType.lsilogic) {
            vmMo.ensureLsiLogicDeviceControllers(requiredNumScsiControllers, availableBusNum);
        }
    }

    protected StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        boolean vmAlreadyExistsInVcenter = false;

        String existingVmName = null;
        VirtualMachineFileInfo existingVmFileInfo = null;
        VirtualMachineFileLayoutEx existingVmFileLayout = null;
        List<DatastoreMO> existingDatastores = new ArrayList<DatastoreMO>();
        String diskStoragePolicyId = null;
        String vmStoragePolicyId = null;
        VirtualMachineDefinedProfileSpec diskProfileSpec = null;
        VirtualMachineDefinedProfileSpec vmProfileSpec = null;


        DeployAsIsInfoTO deployAsIsInfo = vmSpec.getDeployAsIsInfo();
        boolean deployAsIs = deployAsIsInfo != null;

        Pair<String, String> names = composeVmNames(vmSpec);
        String vmInternalCSName = names.first();
        String vmNameOnVcenter = names.second();
        DiskTO rootDiskTO = null;
        String bootMode = getBootModeFromVmSpec(vmSpec, deployAsIs);
        Pair<String, String> controllerInfo = getControllerInfoFromVmSpec(vmSpec);

        Boolean systemVm = vmSpec.getType().isUsedBySystem();
        // Thus, vmInternalCSName always holds i-x-y, the cloudstack generated internal VM name.
        VmwareContext context = getServiceContext();
        DatacenterMO dcMo = null;
        try {
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            VmwareHypervisorHost hyperHost = getHyperHost(context);
            dcMo = new DatacenterMO(hyperHost.getContext(), hyperHost.getHyperHostDatacenter());

            // Validate VM name is unique in Datacenter
            VirtualMachineMO vmInVcenter = dcMo.checkIfVmAlreadyExistsInVcenter(vmNameOnVcenter, vmInternalCSName);
            if (vmInVcenter != null) {
                vmAlreadyExistsInVcenter = true;
                String msg = "VM with name: " + vmNameOnVcenter + " already exists in vCenter.";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DiskTO[] specDisks = vmSpec.getDisks();
            String guestOsId = getGuestOsIdFromVmSpec(vmSpec, deployAsIs);
            DiskTO[] disks = validateDisks(vmSpec.getDisks());
            assert (disks.length > 0);
            NicTO[] nics = vmSpec.getNics();

            HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> dataStoresDetails = inferDatastoreDetailsFromDiskInfo(hyperHost, context, disks, cmd);
            if ((dataStoresDetails == null) || (dataStoresDetails.isEmpty())) {
                String msg = "Unable to locate datastore details of the volumes to be attached";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            VirtualMachineDiskInfoBuilder diskInfoBuilder = null;
            VirtualDevice[] nicDevices = null;
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
            DiskControllerType systemVmScsiControllerType = DiskControllerType.lsilogic;
            int firstScsiControllerBusNum = 0;
            int numScsiControllerForSystemVm = 1;
            boolean hasSnapshot = false;

            List<Pair<Integer, ManagedObjectReference>> diskDatastores = null;
            if (vmMo != null) {
                s_logger.info("VM " + vmInternalCSName + " already exists, tear down devices for reconfiguration");
                if (getVmPowerState(vmMo) != PowerState.PowerOff)
                    vmMo.safePowerOff(_shutdownWaitMs);

                // retrieve disk information before we tear down
                diskDatastores = vmMo.getAllDiskDatastores();
                diskInfoBuilder = vmMo.getDiskInfoBuilder();
                hasSnapshot = vmMo.hasSnapshot();
                nicDevices = vmMo.getNicDevices();

                tearDownVmDevices(vmMo, hasSnapshot, deployAsIs);
                ensureDiskControllersInternal(vmMo, systemVm, controllerInfo, systemVmScsiControllerType,
                        numScsiControllerForSystemVm, firstScsiControllerBusNum, deployAsIs);
            } else {
                ManagedObjectReference morDc = hyperHost.getHyperHostDatacenter();
                assert (morDc != null);

                vmMo = hyperHost.findVmOnPeerHyperHost(vmInternalCSName);
                if (vmMo != null) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Found vm " + vmInternalCSName + " at other host, relocate to " + hyperHost.getHyperHostName());
                    }

                    takeVmFromOtherHyperHost(hyperHost, vmInternalCSName);

                    if (getVmPowerState(vmMo) != PowerState.PowerOff)
                        vmMo.safePowerOff(_shutdownWaitMs);

                    diskInfoBuilder = vmMo.getDiskInfoBuilder();
                    hasSnapshot = vmMo.hasSnapshot();
                    diskDatastores = vmMo.getAllDiskDatastores();

                    tearDownVmDevices(vmMo, hasSnapshot, deployAsIs);
                    ensureDiskControllersInternal(vmMo, systemVm, controllerInfo, systemVmScsiControllerType,
                            numScsiControllerForSystemVm, firstScsiControllerBusNum, deployAsIs);
                } else {
                    // If a VM with the same name is found in a different cluster in the DC, unregister the old VM and configure a new VM (cold-migration).
                    VirtualMachineMO existingVmInDc = dcMo.findVm(vmInternalCSName);
                    if (existingVmInDc != null) {
                        s_logger.debug("Found VM: " + vmInternalCSName + " on a host in a different cluster. Unregistering the exisitng VM.");
                        existingVmName = existingVmInDc.getName();
                        existingVmFileInfo = existingVmInDc.getFileInfo();
                        existingVmFileLayout = existingVmInDc.getFileLayout();
                        existingDatastores = existingVmInDc.getAllDatastores();
                        existingVmInDc.unregisterVm();
                    }

                    if (deployAsIs) {
                        vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
                        if (vmMo == null) {
                            s_logger.info("Cloned deploy-as-is VM " + vmInternalCSName + " is not in this host, relocating it");
                            vmMo = takeVmFromOtherHyperHost(hyperHost, vmInternalCSName);
                        }
                    } else {
                        DiskTO rootDisk = null;
                        for (DiskTO vol : disks) {
                            if (vol.getType() == Volume.Type.ROOT) {
                                rootDisk = vol;
                            }
                        }
                        Pair<ManagedObjectReference, DatastoreMO> rootDiskDataStoreDetails = getDatastoreThatDiskIsOn(dataStoresDetails, rootDisk);
                        assert (vmSpec.getMinSpeed() != null) && (rootDiskDataStoreDetails != null);
                        DatastoreMO dsRootVolumeIsOn = rootDiskDataStoreDetails.second();
                        if (dsRootVolumeIsOn == null) {
                                String msg = "Unable to locate datastore details of root volume";
                                s_logger.error(msg);
                                throw new Exception(msg);
                            }
                        if (rootDisk.getDetails().get(DiskTO.PROTOCOL_TYPE) != null && rootDisk.getDetails().get(DiskTO.PROTOCOL_TYPE).equalsIgnoreCase(Storage.StoragePoolType.DatastoreCluster.toString())) {
                            if (diskInfoBuilder != null) {
                                DatastoreMO diskDatastoreMofromVM = getDataStoreWhereDiskExists(hyperHost, context, diskInfoBuilder, rootDisk, diskDatastores);
                                if (diskDatastoreMofromVM != null) {
                                    String actualPoolUuid = diskDatastoreMofromVM.getCustomFieldValue(CustomFieldConstants.CLOUD_UUID);
                                    if (!actualPoolUuid.equalsIgnoreCase(rootDisk.getData().getDataStore().getUuid())) {
                                        dsRootVolumeIsOn = diskDatastoreMofromVM;
                                    }
                                }
                            }
                        }

                        boolean vmFolderExists = dsRootVolumeIsOn.folderExists(String.format("[%s]", dsRootVolumeIsOn.getName()), vmNameOnVcenter);
                        String vmxFileFullPath = dsRootVolumeIsOn.searchFileInSubFolders(vmNameOnVcenter + ".vmx", false, VmwareManager.s_vmwareSearchExcludeFolder.value());
                        if (vmFolderExists && vmxFileFullPath != null) { // VM can be registered only if .vmx is present.
                            registerVm(vmNameOnVcenter, dsRootVolumeIsOn);
                            vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
                            if (vmMo != null) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Found registered vm " + vmInternalCSName + " at host " + hyperHost.getHyperHostName());
                                }
                            }
                            tearDownVm(vmMo);
                        } else if (!hyperHost.createBlankVm(vmNameOnVcenter, vmInternalCSName, vmSpec.getCpus(), vmSpec.getMaxSpeed().intValue(), getReservedCpuMHZ(vmSpec),
                                vmSpec.getLimitCpuUse(), (int) (vmSpec.getMaxRam() / ResourceType.bytesToMiB), getReservedMemoryMb(vmSpec), guestOsId, rootDiskDataStoreDetails.first(), false,
                                controllerInfo, systemVm)) {
                            throw new Exception("Failed to create VM. vmName: " + vmInternalCSName);
                        }
                    }
                }

                vmMo = hyperHost.findVmOnHyperHost(vmInternalCSName);
                if (vmMo == null) {
                    throw new Exception("Failed to find the newly create or relocated VM. vmName: " + vmInternalCSName);
                }
            }
            if (deployAsIs) {
                s_logger.info("Mapping VM disks to spec disks and tearing down datadisks (if any)");
                mapSpecDisksToClonedDisksAndTearDownDatadisks(vmMo, vmInternalCSName, specDisks);
            }

            int disksChanges = getDisksChangesNumberFromDisksSpec(disks, deployAsIs);
            int totalChangeDevices = disksChanges + nics.length;
            if (deployAsIsInfo != null && deployAsIsInfo.getProperties() != null) {
                totalChangeDevices++;
            }

            DiskTO volIso = null;
            if (vmSpec.getType() != VirtualMachine.Type.User) {
                // system VM needs a patch ISO
                totalChangeDevices++;
            } else {
                volIso = getIsoDiskTO(disks);
                if (volIso == null && !deployAsIs) {
                    totalChangeDevices++;
                }
            }

            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();

            int i = 0;
            int ideUnitNumber = !deployAsIs ? 0 : vmMo.getNextIDEDeviceNumber();
            int scsiUnitNumber = !deployAsIs ? 0 : vmMo.getNextScsiDiskDeviceNumber();
            int ideControllerKey = vmMo.getIDEDeviceControllerKey();
            int scsiControllerKey = vmMo.getScsiDeviceControllerKeyNoException();
            VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[totalChangeDevices];
            DiskTO[] sortedDisks = sortVolumesByDeviceId(disks);
            VmwareHelper.setBasicVmConfig(vmConfigSpec, vmSpec.getCpus(), vmSpec.getMaxSpeed(), getReservedCpuMHZ(vmSpec), (int) (vmSpec.getMaxRam() / (1024 * 1024)),
                    getReservedMemoryMb(vmSpec), guestOsId, vmSpec.getLimitCpuUse(), deployAsIs);

            // Check for multi-cores per socket settings
            int numCoresPerSocket = 1;
            String coresPerSocket = vmSpec.getDetails().get(VmDetailConstants.CPU_CORE_PER_SOCKET);
            if (coresPerSocket != null) {
                String apiVersion = HypervisorHostHelper.getVcenterApiVersion(vmMo.getContext());
                // Property 'numCoresPerSocket' is supported since vSphere API 5.0
                if (apiVersion.compareTo("5.0") >= 0) {
                    numCoresPerSocket = NumbersUtil.parseInt(coresPerSocket, 1);
                    vmConfigSpec.setNumCoresPerSocket(numCoresPerSocket);
                }
            }

            // Check for hotadd settings
            vmConfigSpec.setMemoryHotAddEnabled(vmMo.isMemoryHotAddSupported(guestOsId) && vmSpec.isEnableDynamicallyScaleVm());
            String hostApiVersion = ((HostMO) hyperHost).getHostAboutInfo().getApiVersion();
            if (numCoresPerSocket > 1 && hostApiVersion.compareTo("5.0") < 0) {
                s_logger.warn("Dynamic scaling of CPU is not supported for Virtual Machines with multi-core vCPUs in case of ESXi hosts 4.1 and prior. Hence CpuHotAdd will not be"
                        + " enabled for Virtual Machine: " + vmInternalCSName);
                vmConfigSpec.setCpuHotAddEnabled(false);
            } else {
                vmConfigSpec.setCpuHotAddEnabled(vmMo.isCpuHotAddSupported(guestOsId) && vmSpec.isEnableDynamicallyScaleVm());
            }

            if(!vmMo.isMemoryHotAddSupported(guestOsId) && vmSpec.isEnableDynamicallyScaleVm()){
                s_logger.warn("hotadd of memory is not supported, dynamic scaling feature can not be applied to vm: " + vmInternalCSName);
            }

            if(!vmMo.isCpuHotAddSupported(guestOsId) && vmSpec.isEnableDynamicallyScaleVm()){
                s_logger.warn("hotadd of cpu is not supported, dynamic scaling feature can not be applied to vm: " + vmInternalCSName);
            }

            configNestedHVSupport(vmMo, vmSpec, vmConfigSpec);

            //
            // Setup ISO device
            //

            // prepare systemvm patch ISO
            if (vmSpec.getType() != VirtualMachine.Type.User) {
                // attach ISO (for patching of system VM)
                Pair<String, Long> secStoreUrlAndId = mgr.getSecondaryStorageStoreUrlAndId(Long.parseLong(_dcId));
                String secStoreUrl = secStoreUrlAndId.first();
                Long secStoreId = secStoreUrlAndId.second();
                if (secStoreUrl == null) {
                    String msg = "secondary storage for dc " + _dcId + " is not ready yet?";
                    throw new Exception(msg);
                }

                ManagedObjectReference morSecDs = prepareSecondaryDatastoreOnHost(secStoreUrl);
                if (morSecDs == null) {
                    String msg = "Failed to prepare secondary storage on host, secondary store url: " + secStoreUrl;
                    throw new Exception(msg);
                }
                DatastoreMO secDsMo = new DatastoreMO(hyperHost.getContext(), morSecDs);

                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                Pair<VirtualDevice, Boolean> isoInfo = VmwareHelper.prepareIsoDevice(vmMo,
                        null, secDsMo.getMor(), true, true, ideUnitNumber++, i + 1);
                deviceConfigSpecArray[i].setDevice(isoInfo.first());
                if (isoInfo.second()) {
                    if (s_logger.isDebugEnabled())
                        s_logger.debug("Prepare ISO volume at new device " + _gson.toJson(isoInfo.first()));
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
                } else {
                    if (s_logger.isDebugEnabled())
                        s_logger.debug("Prepare ISO volume at existing device " + _gson.toJson(isoInfo.first()));
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                }
                i++;
            } else if (!deployAsIs) {
                // Note: we will always plug a CDROM device
                if (volIso != null) {
                    for (DiskTO vol : disks) {
                        if (vol.getType() == Volume.Type.ISO) {
                            configureIso(hyperHost, vmMo, vol, deviceConfigSpecArray, ideUnitNumber++, i);
                            i++;
                        }
                    }
                } else {
                    deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                    Pair<VirtualDevice, Boolean> isoInfo = VmwareHelper.prepareIsoDevice(vmMo, null, null, true, true, ideUnitNumber++, i + 1);
                    deviceConfigSpecArray[i].setDevice(isoInfo.first());
                    if (isoInfo.second()) {
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("Prepare ISO volume at existing device " + _gson.toJson(isoInfo.first()));

                        deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
                    } else {
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("Prepare ISO volume at existing device " + _gson.toJson(isoInfo.first()));

                        deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
                    }
                    i++;
                }
            }

            int controllerKey;

            //
            // Setup ROOT/DATA disk devices
            //
            if (multipleIsosAtached(sortedDisks) && deployAsIs) {
                sortedDisks = getDisks(sortedDisks);
            }

            for (DiskTO vol : sortedDisks) {
                if (vol.getType() == Volume.Type.ISO) {
                    if (deployAsIs) {
                        configureIso(hyperHost, vmMo, vol, deviceConfigSpecArray, ideUnitNumber++, i);
                        i++;
                    }
                    continue;
                }

                if (deployAsIs && vol.getType() == Volume.Type.ROOT) {
                    rootDiskTO = vol;
                    resizeRootDiskOnVMStart(vmMo, rootDiskTO, hyperHost, context);
                    continue;
                }

                VirtualMachineDiskInfo matchingExistingDisk = getMatchingExistingDisk(diskInfoBuilder, vol, hyperHost, context);
                String diskController = getDiskController(vmMo, matchingExistingDisk, vol, controllerInfo, deployAsIs);
                if (DiskControllerType.getType(diskController) == DiskControllerType.osdefault) {
                    diskController = vmMo.getRecommendedDiskController(null);
                }
                if (DiskControllerType.getType(diskController) == DiskControllerType.ide) {
                    controllerKey = vmMo.getIDEControllerKey(ideUnitNumber);
                    if (vol.getType() == Volume.Type.DATADISK) {
                        // Could be result of flip due to user configured setting or "osdefault" for data disks
                        // Ensure maximum of 2 data volumes over IDE controller, 3 includeing root volume
                        if (vmMo.getNumberOfVirtualDisks() > 3) {
                            throw new CloudRuntimeException("Found more than 3 virtual disks attached to this VM [" + vmMo.getVmName() + "]. Unable to implement the disks over "
                                    + diskController + " controller, as maximum number of devices supported over IDE controller is 4 includeing CDROM device.");
                        }
                    }
                } else {
                    if (VmwareHelper.isReservedScsiDeviceNumber(scsiUnitNumber)) {
                        scsiUnitNumber++;
                    }

                    controllerKey = vmMo.getScsiDiskControllerKeyNoException(diskController, scsiUnitNumber);
                    if (controllerKey == -1) {
                        // This may happen for ROOT legacy VMs which doesn't have recommended disk controller when global configuration parameter 'vmware.root.disk.controller' is set to "osdefault"
                        // Retrieve existing controller and use.
                        Ternary<Integer, Integer, DiskControllerType> vmScsiControllerInfo = vmMo.getScsiControllerInfo();
                        DiskControllerType existingControllerType = vmScsiControllerInfo.third();
                        controllerKey = vmMo.getScsiDiskControllerKeyNoException(existingControllerType.toString(), scsiUnitNumber);
                    }
                }
                if (!hasSnapshot) {
                    deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();

                    VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
                    DataStoreTO primaryStore = volumeTO.getDataStore();
                    Map<String, String> details = vol.getDetails();
                    boolean managed = false;
                    String iScsiName = null;

                    if (details != null) {
                        managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
                        iScsiName = details.get(DiskTO.IQN);
                    }

                    String primaryStoreUuid = primaryStore.getUuid();
                    // if the storage is managed, iScsiName should not be null
                    String datastoreName = managed ? VmwareResource.getDatastoreName(iScsiName) : primaryStoreUuid;
                    Pair<ManagedObjectReference, DatastoreMO> volumeDsDetails = dataStoresDetails.get(datastoreName);

                    assert (volumeDsDetails != null);
                    if (volumeDsDetails == null) {
                        throw new Exception("Primary datastore " + primaryStore.getUuid() + " is not mounted on host.");
                    }

                    if (vol.getDetails().get(DiskTO.PROTOCOL_TYPE) != null && vol.getDetails().get(DiskTO.PROTOCOL_TYPE).equalsIgnoreCase("DatastoreCluster")) {
                        if (diskInfoBuilder != null && matchingExistingDisk != null) {
                            String[] diskChain = matchingExistingDisk.getDiskChain();
                            if (diskChain != null && diskChain.length > 0) {
                                DatastoreFile file = new DatastoreFile(diskChain[0]);
                                if (!file.getFileBaseName().equalsIgnoreCase(volumeTO.getPath())) {
                                    if (s_logger.isInfoEnabled())
                                        s_logger.info("Detected disk-chain top file change on volume: " + volumeTO.getId() + " " + volumeTO.getPath() + " -> " + file.getFileBaseName());
                                    volumeTO.setPath(file.getFileBaseName());
                                }
                            }
                            DatastoreMO diskDatastoreMofromVM = getDataStoreWhereDiskExists(hyperHost, context, diskInfoBuilder, vol, diskDatastores);
                            if (diskDatastoreMofromVM != null) {
                                String actualPoolUuid = diskDatastoreMofromVM.getCustomFieldValue(CustomFieldConstants.CLOUD_UUID);
                                if (actualPoolUuid != null && !actualPoolUuid.equalsIgnoreCase(primaryStore.getUuid())) {
                                    volumeDsDetails = new Pair<>(diskDatastoreMofromVM.getMor(), diskDatastoreMofromVM);
                                    if (s_logger.isInfoEnabled())
                                        s_logger.info("Detected datastore uuid change on volume: " + volumeTO.getId() + " " + primaryStore.getUuid() + " -> " + actualPoolUuid);
                                    ((PrimaryDataStoreTO)primaryStore).setUuid(actualPoolUuid);
                                }
                            }
                        }
                    }

                    String[] diskChain = syncDiskChain(dcMo, vmMo, vol, matchingExistingDisk, volumeDsDetails.second());

                    int deviceNumber = -1;
                    if (controllerKey == vmMo.getIDEControllerKey(ideUnitNumber)) {
                        deviceNumber = ideUnitNumber % VmwareHelper.MAX_ALLOWED_DEVICES_IDE_CONTROLLER;
                        ideUnitNumber++;
                    } else {
                        deviceNumber = scsiUnitNumber % VmwareHelper.MAX_ALLOWED_DEVICES_SCSI_CONTROLLER;
                        scsiUnitNumber++;
                    }

                    Long maxIops = volumeTO.getIopsWriteRate() + volumeTO.getIopsReadRate();
                    VirtualDevice device = VmwareHelper.prepareDiskDevice(vmMo, null, controllerKey, diskChain, volumeDsDetails.first(), deviceNumber, i + 1, maxIops);
                    s_logger.debug(LogUtils.logGsonWithoutException("The following definitions will be used to start the VM: virtual device [%s], volume [%s].", device, volumeTO));

                    diskStoragePolicyId = volumeTO.getvSphereStoragePolicyId();
                    if (StringUtils.isNotEmpty(diskStoragePolicyId)) {
                        PbmProfileManagerMO profMgrMo = new PbmProfileManagerMO(context);
                        diskProfileSpec = profMgrMo.getProfileSpec(diskStoragePolicyId);
                        deviceConfigSpecArray[i].getProfile().add(diskProfileSpec);
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug(String.format("Adding vSphere storage profile: %s to virtual disk [%s]", diskStoragePolicyId, _gson.toJson(device)));
                        }
                    }
                    if (vol.getType() == Volume.Type.ROOT) {
                        rootDiskTO = vol;
                        vmStoragePolicyId = diskStoragePolicyId;
                        vmProfileSpec = diskProfileSpec;
                    }
                    deviceConfigSpecArray[i].setDevice(device);
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);

                    if (s_logger.isDebugEnabled())
                        s_logger.debug("Prepare volume at new device " + _gson.toJson(device));

                    i++;
                } else {
                    if (controllerKey == vmMo.getIDEControllerKey(ideUnitNumber))
                        ideUnitNumber++;
                    else
                        scsiUnitNumber++;
                }
            }

            //
            // Setup USB devices
            //
            if (StringUtils.isNotBlank(guestOsId) && guestOsId.startsWith("darwin")) { //Mac OS
                VirtualDevice[] devices = vmMo.getMatchedDevices(new Class<?>[]{VirtualUSBController.class});
                if (devices.length == 0) {
                    s_logger.debug("No USB Controller device on VM Start. Add USB Controller device for Mac OS VM " + vmInternalCSName);

                    //For Mac OS X systems, the EHCI+UHCI controller is enabled by default and is required for USB mouse and keyboard access.
                    VirtualDevice usbControllerDevice = VmwareHelper.prepareUSBControllerDevice();
                    deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                    deviceConfigSpecArray[i].setDevice(usbControllerDevice);
                    deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);

                    if (s_logger.isDebugEnabled())
                        s_logger.debug("Prepare USB controller at new device " + _gson.toJson(deviceConfigSpecArray[i]));

                    i++;
                } else {
                    s_logger.debug("USB Controller device exists on VM Start for Mac OS VM " + vmInternalCSName);
                }
            }

            //
            // Setup NIC devices
            //
            VirtualDevice nic;
            int nicMask = 0;
            int nicCount = 0;

            VirtualEthernetCardType nicDeviceType;

            NiciraNvpApiVersion.logNiciraApiVersion();

            Map<String, String> nicUuidToDvSwitchUuid = new HashMap<String, String>();
            for (NicTO nicTo : sortNicsByDeviceId(nics)) {
                s_logger.info("Prepare NIC device based on NicTO: " + _gson.toJson(nicTo));

                String adapterTypeStr = deployAsIs ?
                        mapAdapterType(deployAsIsInfo.getNicAdapterMap().get(nicTo.getDeviceId())) :
                        vmSpec.getDetails().get(VmDetailConstants.NIC_ADAPTER);
                nicDeviceType = VirtualEthernetCardType.valueOf(adapterTypeStr);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("VM " + vmInternalCSName + " will be started with NIC device type: " + nicDeviceType + " on NIC device " + nicTo.getDeviceId());
                }
                boolean configureVServiceInNexus = (nicTo.getType() == TrafficType.Guest) && (vmSpec.getDetails().containsKey("ConfigureVServiceInNexus"));
                VirtualMachine.Type vmType = cmd.getVirtualMachine().getType();
                Pair<ManagedObjectReference, String> networkInfo = prepareNetworkFromNicInfo(vmMo.getRunningHost(), nicTo, configureVServiceInNexus, vmType);
                if ((nicTo.getBroadcastType() != BroadcastDomainType.Lswitch)
                        || (nicTo.getBroadcastType() == BroadcastDomainType.Lswitch && NiciraNvpApiVersion.isApiVersionLowerThan("4.2"))) {
                    if (VmwareHelper.isDvPortGroup(networkInfo.first())) {
                        String dvSwitchUuid;
                        ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                        DatacenterMO dataCenterMo = new DatacenterMO(context, dcMor);
                        ManagedObjectReference dvsMor = dataCenterMo.getDvSwitchMor(networkInfo.first());
                        dvSwitchUuid = dataCenterMo.getDvSwitchUuid(dvsMor);
                        s_logger.info("Preparing NIC device on dvSwitch : " + dvSwitchUuid);
                        nic = VmwareHelper.prepareDvNicDevice(vmMo, networkInfo.first(), nicDeviceType, networkInfo.second(), dvSwitchUuid,
                                nicTo.getMac(), i + 1, true, true);
                        if (nicTo.getUuid() != null) {
                            nicUuidToDvSwitchUuid.put(nicTo.getUuid(), dvSwitchUuid);
                        }
                    } else {
                        s_logger.info("Preparing NIC device on network " + networkInfo.second());
                        nic = VmwareHelper.prepareNicDevice(vmMo, networkInfo.first(), nicDeviceType, networkInfo.second(),
                                nicTo.getMac(), i + 1, true, true);
                    }
                } else {
                    //if NSX API VERSION >= 4.2, connect to br-int (nsx.network), do not create portgroup else previous behaviour
                    nic = VmwareHelper.prepareNicOpaque(vmMo, nicDeviceType, networkInfo.second(),
                            nicTo.getMac(), i + 1, true, true);
                }

                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                deviceConfigSpecArray[i].setDevice(nic);
                deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);

                if (s_logger.isDebugEnabled())
                    s_logger.debug("Prepare NIC at new device " + _gson.toJson(deviceConfigSpecArray[i]));

                // this is really a hacking for DomR, upon DomR startup, we will reset all the NIC allocation after eth3
                if (nicCount < 3)
                    nicMask |= (1 << nicCount);

                i++;
                nicCount++;
            }

            for (int j = 0; j < i; j++)
                vmConfigSpec.getDeviceChange().add(deviceConfigSpecArray[j]);

            //
            // Setup VM options
            //

            // pass boot arguments through machine.id & perform customized options to VMX
            ArrayList<OptionValue> extraOptions = new ArrayList<OptionValue>();
            configBasicExtraOption(extraOptions, vmSpec);

            if (deployAsIs) {
                setDeployAsIsProperties(vmMo, deployAsIsInfo, vmConfigSpec, hyperHost);
            }

            configNvpExtraOption(extraOptions, vmSpec, nicUuidToDvSwitchUuid);
            configCustomExtraOption(extraOptions, vmSpec);

            // config for NCC
            VirtualMachine.Type vmType = cmd.getVirtualMachine().getType();
            if (vmType.equals(VirtualMachine.Type.NetScalerVm)) {
                NicTO mgmtNic = vmSpec.getNics()[0];
                OptionValue option = new OptionValue();
                option.setKey("machine.id");
                option.setValue("ip=" + mgmtNic.getIp() + "&netmask=" + mgmtNic.getNetmask() + "&gateway=" + mgmtNic.getGateway());
                extraOptions.add(option);
            }

            configureVNC(vmSpec, extraOptions, vmConfigSpec, hyperHost, vmInternalCSName);

            // config video card
            configureVideoCard(vmMo, vmSpec, vmConfigSpec);

            setBootOptions(vmSpec, bootMode, vmConfigSpec);

            if (StringUtils.isNotEmpty(vmStoragePolicyId)) {
                vmConfigSpec.getVmProfile().add(vmProfileSpec);
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace(String.format("Configuring the VM %s with storage policy: %s", vmInternalCSName, vmStoragePolicyId));
                }
            }
            //
            // Configure VM
            //
            if (!vmMo.configureVm(vmConfigSpec)) {
                throw new Exception("Failed to configure VM before start. vmName: " + vmInternalCSName);
            }

            if (vmSpec.getType() == VirtualMachine.Type.DomainRouter) {
                hyperHost.setRestartPriorityForVM(vmMo, DasVmPriority.HIGH.value());
            }

            // Resizing root disk only when explicit requested by user
            final Map<String, String> vmDetails = cmd.getVirtualMachine().getDetails();
            if (!deployAsIs && rootDiskTO != null && !hasSnapshot && (vmDetails != null && vmDetails.containsKey(ApiConstants.ROOT_DISK_SIZE))) {
                resizeRootDiskOnVMStart(vmMo, rootDiskTO, hyperHost, context);
            }

            //
            // Post Configuration
            //

            vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK, String.valueOf(nicMask));
            postNvpConfigBeforeStart(vmMo, vmSpec);

            Map<String, Map<String, String>> iqnToData = new HashMap<>();

            postDiskConfigBeforeStart(vmMo, vmSpec, sortedDisks, ideControllerKey, scsiControllerKey, iqnToData, hyperHost, context);

            //
            // Power-on VM
            //
            if (!vmMo.powerOn()) {
                throw new Exception("Failed to start VM. vmName: " + vmInternalCSName + " with hostname " + vmNameOnVcenter);
            }

            StartAnswer startAnswer = new StartAnswer(cmd);

            startAnswer.setIqnToData(iqnToData);

            if (vmSpec.getType() != VirtualMachine.Type.User) {
                String controlIp = getControlIp(nics);
                // check if the router is up?
                for (int count = 0; count < 60; count++) {
                    final boolean result = _vrResource.connect(controlIp, 1, 5000);
                    if (result) {
                        break;
                    }
                }

                try {
                    String homeDir = System.getProperty("user.home");
                    File pemFile = new File(homeDir + "/.ssh/id_rsa");
                    FileUtil.scpPatchFiles(controlIp, VRScripts.CONFIG_CACHE_LOCATION, DefaultDomRSshPort, pemFile, systemVmPatchFiles, BASEPATH);
                    if (!_vrResource.isSystemVMSetup(vmInternalCSName, controlIp)) {
                        String errMsg = "Failed to patch systemVM";
                        s_logger.error(errMsg);
                        return new StartAnswer(cmd, errMsg);
                    }
                } catch (Exception e) {
                    String errMsg = "Failed to scp files to system VM. Patching of systemVM failed";
                    s_logger.error(errMsg, e);
                    return new StartAnswer(cmd, String.format("%s due to: %s", errMsg, e.getMessage()));
                }
            }

            // Since VM was successfully powered-on, if there was an existing VM in a different cluster that was unregistered, delete all the files associated with it.
            if (existingVmName != null && existingVmFileLayout != null) {
                List<String> vmDatastoreNames = new ArrayList<String>();
                for (DatastoreMO vmDatastore : vmMo.getAllDatastores()) {
                    vmDatastoreNames.add(vmDatastore.getName());
                }
                // Don't delete files that are in a datastore that is being used by the new VM as well (zone-wide datastore).
                List<String> skipDatastores = new ArrayList<String>();
                for (DatastoreMO existingDatastore : existingDatastores) {
                    if (vmDatastoreNames.contains(existingDatastore.getName())) {
                        skipDatastores.add(existingDatastore.getName());
                    }
                }
                deleteUnregisteredVmFiles(existingVmFileLayout, dcMo, true, skipDatastores);
            }

            return startAnswer;
        } catch (Throwable e) {
            StartAnswer startAnswer = new StartAnswer(cmd, createLogMessageException(e, cmd));
            if (vmAlreadyExistsInVcenter) {
                startAnswer.setContextParam("stopRetry", "true");
            }

            if (existingVmName != null && existingVmFileInfo != null) {
                s_logger.debug(String.format("Since VM start failed, registering back an existing VM: [%s] that was unregistered.", existingVmName));
                try {
                    DatastoreFile fileInDatastore = new DatastoreFile(existingVmFileInfo.getVmPathName());
                    DatastoreMO existingVmDsMo = new DatastoreMO(dcMo.getContext(), dcMo.findDatastore(fileInDatastore.getDatastoreName()));
                    registerVm(existingVmName, existingVmDsMo);
                } catch (Exception ex) {
                    String message = String.format("Failed to register an existing VM: [%s] due to [%s].", existingVmName, VmwareHelper.getExceptionMessage(ex));
                    s_logger.error(message, ex);
                }
            }
            return startAnswer;
        }
    }

    private boolean multipleIsosAtached(DiskTO[] sortedDisks) {
        return Arrays.stream(sortedDisks).filter(disk -> disk.getType() == Volume.Type.ISO).count() > 1;
    }

    private DiskTO[] getDisks(DiskTO[] sortedDisks) {
       return Arrays.stream(sortedDisks).filter(vol -> ((vol.getPath() != null &&
                vol.getPath().contains("configdrive"))) || (vol.getType() != Volume.Type.ISO)).toArray(DiskTO[]::new);
    }
    private void configureIso(VmwareHypervisorHost hyperHost, VirtualMachineMO vmMo, DiskTO vol,
                              VirtualDeviceConfigSpec[] deviceConfigSpecArray, int ideUnitNumber, int i) throws Exception {
        TemplateObjectTO iso = (TemplateObjectTO) vol.getData();

        if (iso.getPath() != null && !iso.getPath().isEmpty()) {
            DataStoreTO imageStore = iso.getDataStore();
            if (!(imageStore instanceof NfsTO)) {
                s_logger.debug("unsupported protocol");
                throw new Exception("unsupported protocol");
            }
            NfsTO nfsImageStore = (NfsTO) imageStore;
            String isoPath = nfsImageStore.getUrl() + File.separator + iso.getPath();
            Pair<String, ManagedObjectReference> isoDatastoreInfo = getIsoDatastoreInfo(hyperHost, isoPath);
            assert (isoDatastoreInfo != null);
            assert (isoDatastoreInfo.second() != null);

            deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
            Pair<VirtualDevice, Boolean> isoInfo =
                    VmwareHelper.prepareIsoDevice(vmMo, isoDatastoreInfo.first(), isoDatastoreInfo.second(), true, true, ideUnitNumber, i + 1);
            deviceConfigSpecArray[i].setDevice(isoInfo.first());
            if (isoInfo.second()) {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("Prepare ISO volume at new device " + _gson.toJson(isoInfo.first()));
                deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.ADD);
            } else {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("Prepare ISO volume at existing device " + _gson.toJson(isoInfo.first()));
                deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.EDIT);
            }
        }
    }

    private String mapAdapterType(String adapterStringFromOVF) {
        if (StringUtils.isBlank(adapterStringFromOVF) || adapterStringFromOVF.equalsIgnoreCase(VirtualEthernetCardType.E1000.toString())) {
            return VirtualEthernetCardType.E1000.toString();
        } else if (adapterStringFromOVF.equalsIgnoreCase(VirtualEthernetCardType.PCNet32.toString())) {
            return VirtualEthernetCardType.PCNet32.toString();
        } else if (adapterStringFromOVF.equalsIgnoreCase(VirtualEthernetCardType.Vmxnet2.toString())) {
            return VirtualEthernetCardType.Vmxnet2.toString();
        } else if (adapterStringFromOVF.equalsIgnoreCase(VirtualEthernetCardType.Vmxnet3.toString())) {
            return VirtualEthernetCardType.Vmxnet3.toString();
        }
        return VirtualEthernetCardType.E1000.toString();
    }

    private int getDisksChangesNumberFromDisksSpec(DiskTO[] disks, boolean deployAsIs) {
        if (!deployAsIs) {
            return disks.length;
        } else {
            int datadisksNumber = 0;
            if (ArrayUtils.isNotEmpty(disks)) {
                List<DiskTO> datadisks = Arrays.stream(disks).filter(x -> x.getType() == Volume.Type.DATADISK).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(datadisks)) {
                    datadisksNumber = datadisks.size();
                }
            }
            return datadisksNumber;
        }
    }

    /**
     * Configure VNC
     */
    private void configureVNC(VirtualMachineTO vmSpec, ArrayList<OptionValue> extraOptions, VirtualMachineConfigSpec vmConfigSpec, VmwareHypervisorHost hyperHost, String vmInternalCSName) throws Exception {
        String keyboardLayout = null;
        if (vmSpec.getDetails() != null)
            keyboardLayout = vmSpec.getDetails().get(VmDetailConstants.KEYBOARD);
        vmConfigSpec.getExtraConfig()
                .addAll(Arrays.asList(configureVnc(extraOptions.toArray(new OptionValue[0]), hyperHost, vmInternalCSName, vmSpec.getVncPassword(), keyboardLayout)));

    }

    private void ensureDiskControllersInternal(VirtualMachineMO vmMo, Boolean systemVm,
                                               Pair<String, String> controllerInfo,
                                               DiskControllerType systemVmScsiControllerType,
                                               int numScsiControllerForSystemVm,
                                               int firstScsiControllerBusNum, boolean deployAsIs) throws Exception {
        if (systemVm) {
            ensureScsiDiskControllers(vmMo, systemVmScsiControllerType.toString(), numScsiControllerForSystemVm, firstScsiControllerBusNum);
        } else if (!deployAsIs) {
            ensureDiskControllers(vmMo, controllerInfo);
        }
    }

    private void tearDownVmDevices(VirtualMachineMO vmMo, boolean hasSnapshot, boolean deployAsIs) throws Exception {
        if (deployAsIs) {
            vmMo.tearDownDevices(new Class<?>[]{VirtualEthernetCard.class});
        } else if (!hasSnapshot) {
            vmMo.tearDownDevices(new Class<?>[]{VirtualDisk.class, VirtualEthernetCard.class});
        } else {
            vmMo.tearDownDevices(new Class<?>[]{VirtualEthernetCard.class});
        }
    }

    private void tearDownVMDisks(VirtualMachineMO vmMo, List<VirtualDisk> disks) throws Exception {
        for (VirtualDisk disk : disks) {
            vmMo.tearDownDevice(disk);
        }
    }

    private String getGuestOsIdFromVmSpec(VirtualMachineTO vmSpec, boolean deployAsIs) {
        return translateGuestOsIdentifier(vmSpec.getArch(), vmSpec.getOs(), vmSpec.getPlatformEmulator()).value();
    }

    private Pair<String, String> getControllerInfoFromVmSpec(VirtualMachineTO vmSpec) throws CloudRuntimeException {
        String dataDiskController = vmSpec.getDetails().get(VmDetailConstants.DATA_DISK_CONTROLLER);
        String rootDiskController = vmSpec.getDetails().get(VmDetailConstants.ROOT_DISK_CONTROLLER);

        // If root disk controller is scsi, then data disk controller would also be scsi instead of using 'osdefault'
        // This helps avoid mix of different scsi subtype controllers in instance.
        if (DiskControllerType.osdefault == DiskControllerType.getType(dataDiskController) && DiskControllerType.lsilogic == DiskControllerType.getType(rootDiskController)) {
            dataDiskController = DiskControllerType.scsi.toString();
        }

        // Validate the controller types
        dataDiskController = DiskControllerType.getType(dataDiskController).toString();
        rootDiskController = DiskControllerType.getType(rootDiskController).toString();

        if (DiskControllerType.getType(rootDiskController) == DiskControllerType.none) {
            throw new CloudRuntimeException("Invalid root disk controller detected : " + rootDiskController);
        }
        if (DiskControllerType.getType(dataDiskController) == DiskControllerType.none) {
            throw new CloudRuntimeException("Invalid data disk controller detected : " + dataDiskController);
        }

        return new Pair<>(rootDiskController, dataDiskController);
    }

    private String getBootModeFromVmSpec(VirtualMachineTO vmSpec, boolean deployAsIs) {
        String bootMode = null;
        if (vmSpec.getDetails().containsKey(VmDetailConstants.BOOT_MODE)) {
            bootMode = vmSpec.getDetails().get(VmDetailConstants.BOOT_MODE);
        }
        if (bootMode == null) {
            bootMode = ApiConstants.BootType.BIOS.toString();
        }
        return bootMode;
    }

    /**
     * Set OVF properties (if available)
     */
    private void setDeployAsIsProperties(VirtualMachineMO vmMo, DeployAsIsInfoTO deployAsIsInfo,
                                         VirtualMachineConfigSpec vmConfigSpec, VmwareHypervisorHost hyperHost) throws Exception {
        if (deployAsIsInfo != null && MapUtils.isNotEmpty(deployAsIsInfo.getProperties())) {
            Map<String, String> properties = deployAsIsInfo.getProperties();
            VmConfigInfo vAppConfig = vmMo.getConfigInfo().getVAppConfig();
            s_logger.info("Copying OVF properties to the values the user provided");
            setVAppPropertiesToConfigSpec(vAppConfig, properties, vmConfigSpec, hyperHost);
        }
    }

    /**
     * Modify the specDisks information to match the cloned VM's disks (from vmMo VM)
     */
    private void mapSpecDisksToClonedDisksAndTearDownDatadisks(VirtualMachineMO vmMo, String vmInternalCSName, DiskTO[] specDisks) {
        try {
            s_logger.debug("Mapping spec disks information to cloned VM disks for VM " + vmInternalCSName);
            if (vmMo != null && ArrayUtils.isNotEmpty(specDisks)) {
                List<VirtualDisk> vmDisks = vmMo.getVirtualDisksOrderedByKey();

                List<VirtualDisk> rootDisks = new ArrayList<>();
                List<DiskTO> sortedRootDisksFromSpec = Arrays.asList(sortVolumesByDeviceId(specDisks))
                        .stream()
                        .filter(x -> x.getType() == Volume.Type.ROOT)
                        .collect(Collectors.toList());
                for (int i = 0; i < sortedRootDisksFromSpec.size(); i++) {
                    DiskTO specDisk = sortedRootDisksFromSpec.get(i);
                    VirtualDisk vmDisk = vmDisks.get(i);
                    DataTO dataVolume = specDisk.getData();
                    if (dataVolume instanceof VolumeObjectTO) {
                        VolumeObjectTO volumeObjectTO = (VolumeObjectTO) dataVolume;
                        if (!volumeObjectTO.getSize().equals(vmDisk.getCapacityInBytes())) {
                            s_logger.info("Mapped disk size is not the same as the cloned VM disk size: " +
                                    volumeObjectTO.getSize() + " - " + vmDisk.getCapacityInBytes());
                        }
                        VirtualDeviceBackingInfo backingInfo = vmDisk.getBacking();
                        if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                            VirtualDiskFlatVer2BackingInfo backing = (VirtualDiskFlatVer2BackingInfo) backingInfo;
                            String fileName = backing.getFileName();
                            if (StringUtils.isNotBlank(fileName)) {
                                String[] fileNameParts = fileName.split(" ");
                                String datastoreUuid = fileNameParts[0].replace("[", "").replace("]", "");
                                String relativePath = fileNameParts[1].split("/")[1].replace(".vmdk", "");
                                String vmSpecDatastoreUuid = volumeObjectTO.getDataStore().getUuid().replaceAll("-", "");
                                if (!datastoreUuid.equals(vmSpecDatastoreUuid)) {
                                    s_logger.info("Mapped disk datastore UUID is not the same as the cloned VM datastore UUID: " +
                                            datastoreUuid + " - " + vmSpecDatastoreUuid);
                                }
                                volumeObjectTO.setPath(relativePath);
                                specDisk.setPath(relativePath);
                                rootDisks.add(vmDisk);
                            } else {
                                s_logger.error("Empty backing filename for volume " + volumeObjectTO.getName());
                            }
                        } else {
                            s_logger.error("Could not get volume backing info for volume " + volumeObjectTO.getName());
                        }
                    }
                }
                vmDisks.removeAll(rootDisks);
                if (CollectionUtils.isNotEmpty(vmDisks)) {
                    s_logger.info("Tearing down datadisks for deploy-as-is VM");
                    tearDownVMDisks(vmMo, vmDisks);
                }
            }
        } catch (Exception e) {
            String msg = "Error mapping deploy-as-is VM disks from cloned VM " + vmInternalCSName;
            s_logger.error(msg, e);
            throw new CloudRuntimeException(e);
        }
    }

    private void setBootOptions(VirtualMachineTO vmSpec, String bootMode, VirtualMachineConfigSpec vmConfigSpec) {
        VirtualMachineBootOptions bootOptions = null;
        if (StringUtils.isNotBlank(bootMode) && !bootMode.equalsIgnoreCase("bios")) {
            vmConfigSpec.setFirmware("efi");
            if (vmSpec.getDetails().containsKey(ApiConstants.BootType.UEFI.toString()) && "secure".equalsIgnoreCase(vmSpec.getDetails().get(ApiConstants.BootType.UEFI.toString()))) {
                if (bootOptions == null) {
                    bootOptions = new VirtualMachineBootOptions();
                }
                bootOptions.setEfiSecureBootEnabled(true);
            }
        }
        if (vmSpec.isEnterHardwareSetup()) {
            if (bootOptions == null) {
                bootOptions = new VirtualMachineBootOptions();
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("configuring VM '%s' to enter hardware setup",vmSpec.getName()));
            }
            bootOptions.setEnterBIOSSetup(vmSpec.isEnterHardwareSetup());
        }
        if (bootOptions != null) {
            vmConfigSpec.setBootOptions(bootOptions);
        }
    }

    /**
     * Set the ovf section spec from existing vApp configuration
     */
    protected List<VAppOvfSectionSpec> copyVAppConfigOvfSectionFromOVF(VmConfigInfo vAppConfig, boolean useEdit) {
        List<VAppOvfSectionInfo> ovfSection = vAppConfig.getOvfSection();
        List<VAppOvfSectionSpec> specs = new ArrayList<>();
        for (VAppOvfSectionInfo info : ovfSection) {
            VAppOvfSectionSpec spec = new VAppOvfSectionSpec();
            spec.setInfo(info);
            spec.setOperation(useEdit ? ArrayUpdateOperation.EDIT : ArrayUpdateOperation.ADD);
            specs.add(spec);
        }
        return specs;
    }

    private Map<String, Pair<String, Boolean>> getOVFMap(List<OVFPropertyTO> props) {
        Map<String, Pair<String, Boolean>> map = new HashMap<>();
        for (OVFPropertyTO prop : props) {
            String value = getPropertyValue(prop);
            Pair<String, Boolean> pair = new Pair<>(value, prop.isPassword());
            map.put(prop.getKey(), pair);
        }
        return map;
    }

    private String getPropertyValue(OVFPropertyTO prop) {
        String type = prop.getType();
        String value = prop.getValue();
        if ("boolean".equalsIgnoreCase(type)) {
            value = Boolean.parseBoolean(value) ? "True" : "False";
        }
        return value;
    }

    /**
     * Set the properties section from existing vApp configuration and values set on ovfProperties
     */
    protected List<VAppPropertySpec> copyVAppConfigPropertySectionFromOVF(VmConfigInfo vAppConfig, Map<String, String> ovfProperties,
                                                                          boolean useEdit) {
        List<VAppPropertyInfo> productFromOvf = vAppConfig.getProperty();
        List<VAppPropertySpec> specs = new ArrayList<>();
        for (VAppPropertyInfo info : productFromOvf) {
            VAppPropertySpec spec = new VAppPropertySpec();
            if (ovfProperties.containsKey(info.getId())) {
                String value = ovfProperties.get(info.getId());
                info.setValue(value);
                s_logger.info("Setting OVF property ID = " + info.getId() + " VALUE = " + value);
            }
            spec.setInfo(info);
            spec.setOperation(useEdit ? ArrayUpdateOperation.EDIT : ArrayUpdateOperation.ADD);
            specs.add(spec);
        }
        return specs;
    }

    /**
     * Set the product section spec from existing vApp configuration
     */
    protected List<VAppProductSpec> copyVAppConfigProductSectionFromOVF(VmConfigInfo vAppConfig, boolean useEdit) {
        List<VAppProductInfo> productFromOvf = vAppConfig.getProduct();
        List<VAppProductSpec> specs = new ArrayList<>();
        for (VAppProductInfo info : productFromOvf) {
            VAppProductSpec spec = new VAppProductSpec();
            spec.setInfo(info);
            s_logger.info("Procuct info KEY " + info.getKey());
            spec.setOperation(useEdit ? ArrayUpdateOperation.EDIT : ArrayUpdateOperation.ADD);
            specs.add(spec);
        }
        return specs;
    }

    /**
     * Set the vApp configuration to vmConfig spec, copying existing configuration from vAppConfig
     * and seting properties values from ovfProperties
     */
    protected void setVAppPropertiesToConfigSpec(VmConfigInfo vAppConfig,
                                                 Map<String, String> ovfProperties,
                                                 VirtualMachineConfigSpec vmConfig, VmwareHypervisorHost hyperHost) throws Exception {
        VmConfigSpec vmConfigSpec = new VmConfigSpec();
        vmConfigSpec.getEula().addAll(vAppConfig.getEula());
        vmConfigSpec.setInstallBootStopDelay(vAppConfig.getInstallBootStopDelay());
        vmConfigSpec.setInstallBootRequired(vAppConfig.isInstallBootRequired());
        vmConfigSpec.setIpAssignment(vAppConfig.getIpAssignment());
        vmConfigSpec.getOvfEnvironmentTransport().addAll(vAppConfig.getOvfEnvironmentTransport());

        // For backward compatibility, prior to Vmware 6.5 use EDIT operation instead of ADD
        boolean useEditOperation = hyperHost.getContext().getServiceContent().getAbout().getApiVersion().compareTo("6.5") < 1;
        vmConfigSpec.getProduct().addAll(copyVAppConfigProductSectionFromOVF(vAppConfig, useEditOperation));
        vmConfigSpec.getProperty().addAll(copyVAppConfigPropertySectionFromOVF(vAppConfig, ovfProperties, useEditOperation));
        vmConfigSpec.getOvfSection().addAll(copyVAppConfigOvfSectionFromOVF(vAppConfig, useEditOperation));
        vmConfig.setVAppConfig(vmConfigSpec);
    }

    private String appendFileType(String path, String fileType) {
        if (path.toLowerCase().endsWith(fileType.toLowerCase())) {
            return path;
        }

        return path + fileType;
    }

    private void resizeRootDiskOnVMStart(VirtualMachineMO vmMo, DiskTO rootDiskTO, VmwareHypervisorHost hyperHost, VmwareContext context) throws Exception {
        final Pair<VirtualDisk, String> vdisk = getVirtualDiskInfo(vmMo, appendFileType(rootDiskTO.getPath(), VMDK_EXTENSION));
        assert (vdisk != null);

        Long reqSize = 0L;
        final VolumeObjectTO volumeTO = ((VolumeObjectTO) rootDiskTO.getData());
        if (volumeTO != null) {
            reqSize = volumeTO.getSize() / 1024;
        }
        final VirtualDisk disk = vdisk.first();
        if (reqSize > disk.getCapacityInKB()) {
            final VirtualMachineDiskInfo diskInfo = getMatchingExistingDisk(vmMo.getDiskInfoBuilder(), rootDiskTO, hyperHost, context);
            assert (diskInfo != null);
            final String[] diskChain = diskInfo.getDiskChain();

            if (diskChain != null && diskChain.length > 1) {
                s_logger.warn("Disk chain length for the VM is greater than one, this is not supported");
                throw new CloudRuntimeException("Unsupported VM disk chain length: " + diskChain.length);
            }

            boolean resizingSupported = false;
            String deviceBusName = diskInfo.getDiskDeviceBusName();
            if (deviceBusName != null && (deviceBusName.toLowerCase().contains("scsi") || deviceBusName.toLowerCase().contains("lsi"))) {
                resizingSupported = true;
            }
            if (!resizingSupported) {
                s_logger.warn("Resizing of root disk is only support for scsi device/bus, the provide VM's disk device bus name is " + diskInfo.getDiskDeviceBusName());
                throw new CloudRuntimeException("Unsupported VM root disk device bus: " + diskInfo.getDiskDeviceBusName());
            }

            disk.setCapacityInKB(reqSize);
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
            deviceConfigSpec.setDevice(disk);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);
            vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
            if (!vmMo.configureVm(vmConfigSpec)) {
                throw new Exception("Failed to configure VM for given root disk size. vmName: " + vmMo.getName());
            }
        }
    }


    /**
     * Generate the mac sequence from the nics.
     */
    protected String generateMacSequence(NicTO[] nics) {
        if (nics.length == 0) {
            return "";
        }

        StringBuffer sbMacSequence = new StringBuffer();
        for (NicTO nicTo : sortNicsByDeviceId(nics)) {
            sbMacSequence.append(nicTo.getMac()).append("|");
        }
        if (!sbMacSequence.toString().isEmpty()) {
            sbMacSequence.deleteCharAt(sbMacSequence.length() - 1); //Remove extra '|' char appended at the end
        }

        return sbMacSequence.toString();
    }

    /**
     * Update boot args with the new nic mac addresses.
     */
    protected String replaceNicsMacSequenceInBootArgs(String oldMacSequence, String newMacSequence, VirtualMachineTO vmSpec) {
        String bootArgs = vmSpec.getBootArgs();
        if (StringUtils.isNoneBlank(bootArgs, oldMacSequence, newMacSequence)) {
            return bootArgs.replace(oldMacSequence, newMacSequence);
        }
        return "";
    }

    /**
     * Sets video card memory to the one provided in detail svga.vramSize (if provided) on {@code vmConfigSpec}.
     * 64MB was always set before.
     * Size must be in KB.
     *
     * @param vmMo         virtual machine mo
     * @param vmSpec       virtual machine specs
     * @param vmConfigSpec virtual machine config spec
     * @throws Exception exception
     */
    protected void configureVideoCard(VirtualMachineMO vmMo, VirtualMachineTO vmSpec, VirtualMachineConfigSpec vmConfigSpec) throws Exception {
        if (vmSpec.getDetails().containsKey(VmDetailConstants.SVGA_VRAM_SIZE)) {
            String value = vmSpec.getDetails().get(VmDetailConstants.SVGA_VRAM_SIZE);
            try {
                long svgaVmramSize = Long.parseLong(value);
                setNewVRamSizeVmVideoCard(vmMo, svgaVmramSize, vmConfigSpec);
            } catch (NumberFormatException e) {
                s_logger.error("Unexpected value, cannot parse " + value + " to long due to: " + e.getMessage());
            }
        }
    }

    /**
     * Search for vm video card iterating through vm device list
     *
     * @param vmMo          virtual machine mo
     * @param svgaVmramSize new svga vram size (in KB)
     * @param vmConfigSpec  virtual machine config spec
     */
    protected void setNewVRamSizeVmVideoCard(VirtualMachineMO vmMo, long svgaVmramSize, VirtualMachineConfigSpec vmConfigSpec) throws Exception {
        for (VirtualDevice device : vmMo.getAllDeviceList()) {
            if (device instanceof VirtualMachineVideoCard) {
                VirtualMachineVideoCard videoCard = (VirtualMachineVideoCard) device;
                modifyVmVideoCardVRamSize(videoCard, vmMo, svgaVmramSize, vmConfigSpec);
            }
        }
    }

    /**
     * Modifies vm vram size if it was set to a different size to the one provided in svga.vramSize (user_vm_details or template_vm_details) on {@code vmConfigSpec}
     *
     * @param videoCard     vm's video card device
     * @param vmMo          virtual machine mo
     * @param svgaVmramSize new svga vram size (in KB)
     * @param vmConfigSpec  virtual machine config spec
     */
    protected void modifyVmVideoCardVRamSize(VirtualMachineVideoCard videoCard, VirtualMachineMO vmMo, long svgaVmramSize, VirtualMachineConfigSpec vmConfigSpec) {
        if (videoCard.getVideoRamSizeInKB().longValue() != svgaVmramSize) {
           s_logger.info("Video card memory was set " + toHumanReadableSize(videoCard.getVideoRamSizeInKB().longValue()) + " instead of " + toHumanReadableSize(svgaVmramSize));
            configureSpecVideoCardNewVRamSize(videoCard, svgaVmramSize, vmConfigSpec);
        }
    }

    /**
     * Add edit spec on {@code vmConfigSpec} to modify svga vram size
     *
     * @param videoCard     video card device to edit providing the svga vram size
     * @param svgaVmramSize new svga vram size (in KB)
     * @param vmConfigSpec  virtual machine spec
     */
    protected void configureSpecVideoCardNewVRamSize(VirtualMachineVideoCard videoCard, long svgaVmramSize, VirtualMachineConfigSpec vmConfigSpec) {
        videoCard.setVideoRamSizeInKB(svgaVmramSize);
        videoCard.setUseAutoDetect(false);

        VirtualDeviceConfigSpec arrayVideoCardConfigSpecs = new VirtualDeviceConfigSpec();
        arrayVideoCardConfigSpecs.setDevice(videoCard);
        arrayVideoCardConfigSpecs.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

        vmConfigSpec.getDeviceChange().add(arrayVideoCardConfigSpecs);
    }

    private void tearDownVm(VirtualMachineMO vmMo) throws Exception {

        if (vmMo == null)
            return;

        boolean hasSnapshot = false;
        hasSnapshot = vmMo.hasSnapshot();
        if (!hasSnapshot)
            vmMo.tearDownDevices(new Class<?>[]{VirtualDisk.class, VirtualEthernetCard.class});
        else
            vmMo.tearDownDevices(new Class<?>[]{VirtualEthernetCard.class});
        vmMo.ensureScsiDeviceController();
    }

    int getReservedMemoryMb(VirtualMachineTO vmSpec) {
        if (vmSpec.getDetails().get(VMwareGuru.VmwareReserveMemory.key()).equalsIgnoreCase("true")) {
            if(vmSpec.getDetails().get(VmDetailConstants.RAM_RESERVATION) != null){
                float reservedMemory = (vmSpec.getMaxRam() * Float.parseFloat(vmSpec.getDetails().get(VmDetailConstants.RAM_RESERVATION)));
                return (int) (reservedMemory / ResourceType.bytesToMiB);
            }
            return (int) (vmSpec.getMinRam() / ResourceType.bytesToMiB);
        }
        return 0;
    }

    int getReservedCpuMHZ(VirtualMachineTO vmSpec) {
        if (vmSpec.getDetails().get(VMwareGuru.VmwareReserveCpu.key()).equalsIgnoreCase("true")) {
            return vmSpec.getMinSpeed() * vmSpec.getCpus();
        }
        return 0;
    }

    // return the finalized disk chain for startup, from top to bottom
    private String[] syncDiskChain(DatacenterMO dcMo, VirtualMachineMO vmMo, DiskTO vol, VirtualMachineDiskInfo diskInfo,
                                   DatastoreMO dsMo) throws Exception {

        VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
        Map<String, String> details = vol.getDetails();
        boolean isManaged = false;

        if (details != null) {
            isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
        }

        String datastoreDiskPath;
        if (dsMo.getDatastoreType().equalsIgnoreCase("VVOL")) {
            datastoreDiskPath = VmwareStorageLayoutHelper.getLegacyDatastorePathFromVmdkFileName(dsMo, volumeTO.getPath() + ".vmdk");
            if (!dsMo.fileExists(datastoreDiskPath)) {
                datastoreDiskPath = VmwareStorageLayoutHelper.getVmwareDatastorePathFromVmdkFileName(dsMo, vmMo.getName(), volumeTO.getPath() + ".vmdk");
            }
            if (!dsMo.fileExists(datastoreDiskPath)) {
                datastoreDiskPath = dsMo.searchFileInSubFolders(volumeTO.getPath() + ".vmdk", true, null);
            }
        } else {
            // we will honor vCenter's meta if it exists
            if (diskInfo != null) {
                // to deal with run-time upgrade to maintain the new datastore folder structure
                String disks[] = diskInfo.getDiskChain();
                for (int i = 0; i < disks.length; i++) {
                    DatastoreFile file = new DatastoreFile(disks[i]);
                    if (!isManaged && file.getDir() != null && file.getDir().isEmpty()) {
                        s_logger.info("Perform run-time datastore folder upgrade. sync " + disks[i] + " to VM folder");
                        disks[i] = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmMo.getName(), dsMo, file.getFileBaseName(), VmwareManager.s_vmwareSearchExcludeFolder.value());
                    }
                }
                return disks;
            }

            if (isManaged) {
                String vmdkPath = new DatastoreFile(volumeTO.getPath()).getFileBaseName();

                if (volumeTO.getVolumeType() == Volume.Type.ROOT) {
                    if (vmdkPath == null) {
                        vmdkPath = volumeTO.getName();
                    }

                    datastoreDiskPath = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmMo.getName(), dsMo, vmdkPath);
                } else {
                    if (vmdkPath == null) {
                        vmdkPath = dsMo.getName();
                    }

                    datastoreDiskPath = dsMo.getDatastorePath(vmdkPath + VMDK_EXTENSION);
                }
            } else {
                datastoreDiskPath = VmwareStorageLayoutHelper.syncVolumeToVmDefaultFolder(dcMo, vmMo.getName(), dsMo, volumeTO.getPath(), VmwareManager.s_vmwareSearchExcludeFolder.value());
            }
        }
        if (!dsMo.fileExists(datastoreDiskPath)) {
            s_logger.warn("Volume " + volumeTO.getId() + " does not seem to exist on datastore, out of sync? path: " + datastoreDiskPath);
        }

        return new String[]{datastoreDiskPath};
    }

    // Pair<internal CS name, vCenter display name>
    private Pair<String, String> composeVmNames(VirtualMachineTO vmSpec) {
        String vmInternalCSName = vmSpec.getName();
        String vmNameOnVcenter = vmSpec.getName();
        String hostNameInDetails = null;
        if (_instanceNameFlag && MapUtils.isNotEmpty(vmSpec.getDetails()) && vmSpec.getDetails().containsKey(VmDetailConstants.NAME_ON_HYPERVISOR)) {
            hostNameInDetails = vmSpec.getDetails().get(VmDetailConstants.NAME_ON_HYPERVISOR);
        }
        if (StringUtils.isNotBlank(hostNameInDetails)) {
            vmNameOnVcenter = hostNameInDetails;
        } else if (_instanceNameFlag && vmSpec.getHostName() != null) {
            vmNameOnVcenter = vmSpec.getHostName();
        }
        return new Pair<String, String>(vmInternalCSName, vmNameOnVcenter);
    }

    protected void configNestedHVSupport(VirtualMachineMO vmMo, VirtualMachineTO vmSpec, VirtualMachineConfigSpec vmConfigSpec) throws Exception {

        VmwareContext context = vmMo.getContext();
        if ("true".equals(vmSpec.getDetails().get(VmDetailConstants.NESTED_VIRTUALIZATION_FLAG))) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Nested Virtualization enabled in configuration, checking hypervisor capability");

            ManagedObjectReference hostMor = vmMo.getRunningHost().getMor();
            ManagedObjectReference computeMor = context.getVimClient().getMoRefProp(hostMor, "parent");
            ManagedObjectReference environmentBrowser = context.getVimClient().getMoRefProp(computeMor, "environmentBrowser");
            HostCapability hostCapability = context.getService().queryTargetCapabilities(environmentBrowser, hostMor);
            Boolean nestedHvSupported = hostCapability.isNestedHVSupported();
            if (nestedHvSupported == null) {
                // nestedHvEnabled property is supported only since VMware 5.1. It's not defined for earlier versions.
                s_logger.warn("Hypervisor doesn't support nested virtualization, unable to set config for VM " + vmSpec.getName());
            } else if (nestedHvSupported.booleanValue()) {
                s_logger.debug("Hypervisor supports nested virtualization, enabling for VM " + vmSpec.getName());
                vmConfigSpec.setNestedHVEnabled(true);
            } else {
                s_logger.warn("Hypervisor doesn't support nested virtualization, unable to set config for VM " + vmSpec.getName());
                vmConfigSpec.setNestedHVEnabled(false);
            }
        }
    }

    private static void configBasicExtraOption(List<OptionValue> extraOptions, VirtualMachineTO vmSpec) {
        OptionValue newVal = new OptionValue();
        newVal.setKey("machine.id");
        newVal.setValue(vmSpec.getBootArgs());
        extraOptions.add(newVal);

        newVal = new OptionValue();
        newVal.setKey("devices.hotplug");
        newVal.setValue("true");
        extraOptions.add(newVal);
    }

    private static void configNvpExtraOption(List<OptionValue> extraOptions, VirtualMachineTO vmSpec, Map<String, String> nicUuidToDvSwitchUuid) {
        /**
         * Extra Config : nvp.vm-uuid = uuid
         *  - Required for Nicira NVP integration
         */
        OptionValue newVal = new OptionValue();
        newVal.setKey("nvp.vm-uuid");
        newVal.setValue(vmSpec.getUuid());
        extraOptions.add(newVal);

        /**
         * Extra Config : nvp.iface-id.<num> = uuid
         *  - Required for Nicira NVP integration
         */
        int nicNum = 0;
        for (NicTO nicTo : sortNicsByDeviceId(vmSpec.getNics())) {
            if (nicTo.getUuid() != null) {
                newVal = new OptionValue();
                newVal.setKey("nvp.iface-id." + nicNum);
                newVal.setValue(nicTo.getUuid());
                extraOptions.add(newVal);
            }
            nicNum++;
        }
    }

    private static void configCustomExtraOption(List<OptionValue> extraOptions, VirtualMachineTO vmSpec) {
        // we no longer to validation anymore
        for (Map.Entry<String, String> entry : vmSpec.getDetails().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(VmDetailConstants.BOOT_MODE)) {
                continue;
            }
            OptionValue newVal = new OptionValue();
            newVal.setKey(entry.getKey());
            newVal.setValue(entry.getValue());
            extraOptions.add(newVal);
        }
    }

    private static void postNvpConfigBeforeStart(VirtualMachineMO vmMo, VirtualMachineTO vmSpec) throws Exception {
        /**
         * We need to configure the port on the DV switch after the host is
         * connected. So make this happen between the configure and start of
         * the VM
         */
        int nicIndex = 0;
        for (NicTO nicTo : sortNicsByDeviceId(vmSpec.getNics())) {
            if (nicTo.getBroadcastType() == BroadcastDomainType.Lswitch) {
                // We need to create a port with a unique vlan and pass the key to the nic device
                s_logger.trace("Nic " + nicTo.toString() + " is connected to an NVP logicalswitch");
                VirtualDevice nicVirtualDevice = vmMo.getNicDeviceByIndex(nicIndex);
                if (nicVirtualDevice == null) {
                    throw new Exception("Failed to find a VirtualDevice for nic " + nicIndex); //FIXME Generic exceptions are bad
                }
                VirtualDeviceBackingInfo backing = nicVirtualDevice.getBacking();
                if (backing instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                    // This NIC is connected to a Distributed Virtual Switch
                    VirtualEthernetCardDistributedVirtualPortBackingInfo portInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo) backing;
                    DistributedVirtualSwitchPortConnection port = portInfo.getPort();
                    String portKey = port.getPortKey();
                    String portGroupKey = port.getPortgroupKey();
                    String dvSwitchUuid = port.getSwitchUuid();

                    s_logger.debug("NIC " + nicTo.toString() + " is connected to dvSwitch " + dvSwitchUuid + " pg " + portGroupKey + " port " + portKey);

                    ManagedObjectReference dvSwitchManager = vmMo.getContext().getVimClient().getServiceContent().getDvSwitchManager();
                    ManagedObjectReference dvSwitch = vmMo.getContext().getVimClient().getService().queryDvsByUuid(dvSwitchManager, dvSwitchUuid);

                    // Get all ports
                    DistributedVirtualSwitchPortCriteria criteria = new DistributedVirtualSwitchPortCriteria();
                    criteria.setInside(true);
                    criteria.getPortgroupKey().add(portGroupKey);
                    List<DistributedVirtualPort> dvPorts = vmMo.getContext().getVimClient().getService().fetchDVPorts(dvSwitch, criteria);

                    DistributedVirtualPort vmDvPort = null;
                    List<Integer> usedVlans = new ArrayList<Integer>();
                    for (DistributedVirtualPort dvPort : dvPorts) {
                        // Find the port for this NIC by portkey
                        if (portKey.equals(dvPort.getKey())) {
                            vmDvPort = dvPort;
                        }
                        VMwareDVSPortSetting settings = (VMwareDVSPortSetting) dvPort.getConfig().getSetting();
                        VmwareDistributedVirtualSwitchVlanIdSpec vlanId = (VmwareDistributedVirtualSwitchVlanIdSpec) settings.getVlan();
                        s_logger.trace("Found port " + dvPort.getKey() + " with vlan " + vlanId.getVlanId());
                        if (vlanId.getVlanId() > 0 && vlanId.getVlanId() < 4095) {
                            usedVlans.add(vlanId.getVlanId());
                        }
                    }

                    if (vmDvPort == null) {
                        throw new Exception("Empty port list from dvSwitch for nic " + nicTo.toString());
                    }

                    DVPortConfigInfo dvPortConfigInfo = vmDvPort.getConfig();
                    VMwareDVSPortSetting settings = (VMwareDVSPortSetting) dvPortConfigInfo.getSetting();

                    VmwareDistributedVirtualSwitchVlanIdSpec vlanId = (VmwareDistributedVirtualSwitchVlanIdSpec) settings.getVlan();
                    BoolPolicy blocked = settings.getBlocked();
                    if (blocked.isValue() == Boolean.TRUE) {
                        s_logger.trace("Port is blocked, set a vlanid and unblock");
                        DVPortConfigSpec dvPortConfigSpec = new DVPortConfigSpec();
                        VMwareDVSPortSetting edittedSettings = new VMwareDVSPortSetting();
                        // Unblock
                        blocked.setValue(Boolean.FALSE);
                        blocked.setInherited(Boolean.FALSE);
                        edittedSettings.setBlocked(blocked);
                        // Set vlan
                        int i;
                        for (i = 1; i < 4095; i++) {
                            if (!usedVlans.contains(i))
                                break;
                        }
                        vlanId.setVlanId(i); // FIXME should be a determined
                        // based on usage
                        vlanId.setInherited(false);
                        edittedSettings.setVlan(vlanId);

                        dvPortConfigSpec.setSetting(edittedSettings);
                        dvPortConfigSpec.setOperation("edit");
                        dvPortConfigSpec.setKey(portKey);
                        List<DVPortConfigSpec> dvPortConfigSpecs = new ArrayList<DVPortConfigSpec>();
                        dvPortConfigSpecs.add(dvPortConfigSpec);
                        ManagedObjectReference task = vmMo.getContext().getVimClient().getService().reconfigureDVPortTask(dvSwitch, dvPortConfigSpecs);
                        if (!vmMo.getContext().getVimClient().waitForTask(task)) {
                            throw new Exception("Failed to configure the dvSwitch port for nic " + nicTo.toString());
                        }
                        s_logger.debug("NIC " + nicTo.toString() + " connected to vlan " + i);
                    } else {
                        s_logger.trace("Port already configured and set to vlan " + vlanId.getVlanId());
                    }
                } else if (backing instanceof VirtualEthernetCardNetworkBackingInfo) {
                    // This NIC is connected to a Virtual Switch
                    // Nothing to do
                } else if (backing instanceof VirtualEthernetCardOpaqueNetworkBackingInfo) {
                    //if NSX API VERSION >= 4.2, connect to br-int (nsx.network), do not create portgroup else previous behaviour
                    //OK, connected to OpaqueNetwork
                } else {
                    s_logger.error("nic device backing is of type " + backing.getClass().getName());
                    throw new Exception("Incompatible backing for a VirtualDevice for nic " + nicIndex); //FIXME Generic exceptions are bad
                }
            }
            nicIndex++;
        }
    }
    private VirtualMachineDiskInfo getMatchingExistingDiskWithVolumeDetails(VirtualMachineDiskInfoBuilder diskInfoBuilder, String volumePath,
                                                                             String chainInfo, boolean isManaged, String iScsiName, String datastoreUUID,
                                                                             VmwareHypervisorHost hyperHost, VmwareContext context) throws Exception {

        Pair<String, String> dsNameAndFileName = getVMDiskInfo(volumePath, isManaged, iScsiName, datastoreUUID, hyperHost, context);
        String dsName = dsNameAndFileName.first();
        String diskBackingFileBaseName = dsNameAndFileName.second();

        VirtualMachineDiskInfo diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(diskBackingFileBaseName, dsName);
        if (diskInfo != null) {
            s_logger.info("Found existing disk info from volume path: " + volumePath);
            return diskInfo;
        } else {
            if (chainInfo != null) {
                VirtualMachineDiskInfo infoInChain = _gson.fromJson(chainInfo, VirtualMachineDiskInfo.class);
                if (infoInChain != null) {
                    String[] disks = infoInChain.getDiskChain();
                    if (disks.length > 0) {
                        for (String diskPath : disks) {
                            DatastoreFile file = new DatastoreFile(diskPath);
                            diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(file.getFileBaseName(), dsName);
                            if (diskInfo != null) {
                                s_logger.info("Found existing disk from chain info: " + diskPath);
                                return diskInfo;
                            }
                        }
                    }

                    if (diskInfo == null) {
                        diskInfo = diskInfoBuilder.getDiskInfoByDeviceBusName(infoInChain.getDiskDeviceBusName());
                        if (diskInfo != null) {
                            s_logger.info("Found existing disk from from chain device bus information: " + infoInChain.getDiskDeviceBusName());
                            return diskInfo;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Pair<String, String> getVMDiskInfo(String volumePath, boolean isManaged, String iScsiName, String datastoreUUID,
                                               VmwareHypervisorHost hyperHost, VmwareContext context) throws Exception {
        String dsName = null;
        String diskBackingFileBaseName = null;

        if (isManaged) {
            // if the storage is managed, iScsiName should not be null
            dsName = VmwareResource.getDatastoreName(iScsiName);
            diskBackingFileBaseName = new DatastoreFile(volumePath).getFileBaseName();
        } else {
            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, datastoreUUID);
            DatastoreMO dsMo = new DatastoreMO(context, morDs);
            dsName = dsMo.getName();
            diskBackingFileBaseName = volumePath;
        }

        return new Pair<>(dsName, diskBackingFileBaseName);
    }

    private VirtualMachineDiskInfo getMatchingExistingDisk(VirtualMachineDiskInfoBuilder diskInfoBuilder, DiskTO vol, VmwareHypervisorHost hyperHost, VmwareContext context)
            throws Exception {
        if (diskInfoBuilder != null) {
            VolumeObjectTO volume = (VolumeObjectTO) vol.getData();
            String chainInfo = volume.getChainInfo();
            Map<String, String> details = vol.getDetails();
            boolean isManaged = details != null && Boolean.parseBoolean(details.get(DiskTO.MANAGED));
            String iScsiName = details.get(DiskTO.IQN);
            String datastoreUUID = volume.getDataStore().getUuid();

            return getMatchingExistingDiskWithVolumeDetails(diskInfoBuilder, volume.getPath(), chainInfo, isManaged, iScsiName, datastoreUUID, hyperHost, context);
        } else {
            return null;
        }
    }

    private String getDiskController(VirtualMachineMO vmMo, VirtualMachineDiskInfo matchingExistingDisk, DiskTO vol, Pair<String, String> controllerInfo, boolean deployAsIs) throws Exception {
        DiskControllerType controllerType = DiskControllerType.none;
        if (deployAsIs && matchingExistingDisk != null) {
            String currentBusName = matchingExistingDisk.getDiskDeviceBusName();
            if (currentBusName != null) {
                s_logger.info("Chose disk controller based on existing information: " + currentBusName);
                if (currentBusName.startsWith("ide")) {
                    controllerType = DiskControllerType.ide;
                } else if (currentBusName.startsWith("scsi")) {
                    controllerType = DiskControllerType.scsi;
                }
            }
            if (controllerType == DiskControllerType.scsi || controllerType == DiskControllerType.none) {
                Ternary<Integer, Integer, DiskControllerType> vmScsiControllerInfo = vmMo.getScsiControllerInfo();
                controllerType = vmScsiControllerInfo.third();
            }
            return controllerType.toString();
        }

        if (vol.getType() == Volume.Type.ROOT) {
            s_logger.info("Chose disk controller for vol " + vol.getType() + " -> " + controllerInfo.first()
                    + ", based on root disk controller settings at global configuration setting.");
            return controllerInfo.first();
        } else {
            s_logger.info("Chose disk controller for vol " + vol.getType() + " -> " + controllerInfo.second()
                    + ", based on default data disk controller setting i.e. Operating system recommended."); // Need to bring in global configuration setting & template level setting.
            return controllerInfo.second();
        }
    }

    private void postDiskConfigBeforeStart(VirtualMachineMO vmMo, VirtualMachineTO vmSpec, DiskTO[] sortedDisks, int ideControllerKey,
                                           int scsiControllerKey, Map<String, Map<String, String>> iqnToData, VmwareHypervisorHost hyperHost, VmwareContext context) throws Exception {
        VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();

        for (DiskTO vol : sortedDisks) {
            if (vol.getType() == Volume.Type.ISO)
                continue;

            VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();

            VirtualMachineDiskInfo diskInfo = getMatchingExistingDisk(diskInfoBuilder, vol, hyperHost, context);
            assert (diskInfo != null);

            String[] diskChain = diskInfo.getDiskChain();
            assert (diskChain.length > 0);

            Map<String, String> details = vol.getDetails();
            boolean managed = false;

            if (details != null) {
                managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
            }

            DatastoreFile file = new DatastoreFile(diskChain[0]);

            if (managed) {
                DatastoreFile originalFile = new DatastoreFile(volumeTO.getPath());

                if (!file.getFileBaseName().equalsIgnoreCase(originalFile.getFileBaseName())) {
                    if (s_logger.isInfoEnabled())
                        s_logger.info("Detected disk-chain top file change on volume: " + volumeTO.getId() + " " + volumeTO.getPath() + " -> " + diskChain[0]);
                }
            } else {
                if (!file.getFileBaseName().equalsIgnoreCase(volumeTO.getPath())) {
                    if (s_logger.isInfoEnabled())
                        s_logger.info("Detected disk-chain top file change on volume: " + volumeTO.getId() + " " + volumeTO.getPath() + " -> " + file.getFileBaseName());
                }
            }

            VolumeObjectTO volInSpec = getVolumeInSpec(vmSpec, volumeTO);

            if (volInSpec != null) {
                if (managed) {
                    Map<String, String> data = new HashMap<>();

                    String datastoreVolumePath = diskChain[0];

                    data.put(StartAnswer.PATH, datastoreVolumePath);
                    data.put(StartAnswer.IMAGE_FORMAT, Storage.ImageFormat.OVA.toString());

                    iqnToData.put(details.get(DiskTO.IQN), data);

                    vol.setPath(datastoreVolumePath);
                    volumeTO.setPath(datastoreVolumePath);
                    volInSpec.setPath(datastoreVolumePath);
                } else {
                    volInSpec.setPath(file.getFileBaseName());
                    if (vol.getDetails().get(DiskTO.PROTOCOL_TYPE) != null && vol.getDetails().get(DiskTO.PROTOCOL_TYPE).equalsIgnoreCase("DatastoreCluster")) {
                        volInSpec.setUpdatedDataStoreUUID(volumeTO.getDataStore().getUuid());
                    }
                }
                volInSpec.setChainInfo(_gson.toJson(diskInfo));
            }
        }
    }

    private void checkAndDeleteDatastoreFile(String filePath, List<String> skipDatastores, DatastoreMO dsMo, DatacenterMO dcMo) throws Exception {
        if (dsMo != null && dcMo != null && (skipDatastores == null || !skipDatastores.contains(dsMo.getName()))) {
            s_logger.debug("Deleting file: " + filePath);
            dsMo.deleteFile(filePath, dcMo.getMor(), true);
        }
    }

    private void deleteUnregisteredVmFiles(VirtualMachineFileLayoutEx vmFileLayout, DatacenterMO dcMo, boolean deleteDisks, List<String> skipDatastores) throws Exception {
        s_logger.debug("Deleting files associated with an existing VM that was unregistered");
        DatastoreFile vmFolder = null;
        try {
            List<VirtualMachineFileLayoutExFileInfo> fileInfo = vmFileLayout.getFile();
            for (VirtualMachineFileLayoutExFileInfo file : fileInfo) {
                DatastoreFile fileInDatastore = new DatastoreFile(file.getName());
                // In case of linked clones, VM file layout includes the base disk so don't delete all disk files.
                if (file.getType().startsWith("disk") || file.getType().startsWith("digest"))
                    continue;
                else if (file.getType().equals("config"))
                    vmFolder = new DatastoreFile(fileInDatastore.getDatastoreName(), fileInDatastore.getDir());
                DatastoreMO dsMo = new DatastoreMO(dcMo.getContext(), dcMo.findDatastore(fileInDatastore.getDatastoreName()));
                checkAndDeleteDatastoreFile(file.getName(), skipDatastores, dsMo, dcMo);
            }
            // Delete files that are present in the VM folder - this will take care of the VM disks as well.
            DatastoreMO vmFolderDsMo = new DatastoreMO(dcMo.getContext(), dcMo.findDatastore(vmFolder.getDatastoreName()));
            String[] files = vmFolderDsMo.listDirContent(vmFolder.getPath());
            if (deleteDisks) {
                for (String file : files) {
                    String vmDiskFileFullPath = String.format("%s/%s", vmFolder.getPath(), file);
                    checkAndDeleteDatastoreFile(vmDiskFileFullPath, skipDatastores, vmFolderDsMo, dcMo);
                }
            }
            // Delete VM folder
            if (deleteDisks || files.length == 0) {
                checkAndDeleteDatastoreFile(vmFolder.getPath(), skipDatastores, vmFolderDsMo, dcMo);
            }
        } catch (Exception e) {
            String message = "Failed to delete files associated with an existing VM that was unregistered due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.warn(message, e);
        }
    }

    private static VolumeObjectTO getVolumeInSpec(VirtualMachineTO vmSpec, VolumeObjectTO srcVol) {
        for (DiskTO disk : vmSpec.getDisks()) {
            if (disk.getData() instanceof VolumeObjectTO) {
                VolumeObjectTO vol = (VolumeObjectTO) disk.getData();
                if (vol.getId() == srcVol.getId())
                    return vol;
            }
        }

        return null;
    }

    private static NicTO[] sortNicsByDeviceId(NicTO[] nics) {

        List<NicTO> listForSort = new ArrayList<NicTO>();
        for (NicTO nic : nics) {
            listForSort.add(nic);
        }
        Collections.sort(listForSort, new Comparator<NicTO>() {

            @Override
            public int compare(NicTO arg0, NicTO arg1) {
                if (arg0.getDeviceId() < arg1.getDeviceId()) {
                    return -1;
                } else if (arg0.getDeviceId() == arg1.getDeviceId()) {
                    return 0;
                }

                return 1;
            }
        });

        return listForSort.toArray(new NicTO[0]);
    }

    private static DiskTO[] sortVolumesByDeviceId(DiskTO[] volumes) {

        List<DiskTO> listForSort = new ArrayList<DiskTO>();
        for (DiskTO vol : volumes) {
            listForSort.add(vol);
        }
        Collections.sort(listForSort, new Comparator<DiskTO>() {

            @Override
            public int compare(DiskTO arg0, DiskTO arg1) {
                if (arg0.getDiskSeq() < arg1.getDiskSeq()) {
                    return -1;
                } else if (arg0.getDiskSeq().equals(arg1.getDiskSeq())) {
                    return 0;
                }

                return 1;
            }
        });

        return listForSort.toArray(new DiskTO[0]);
    }

    /**
     * Only call this for managed storage.
     * Ex. "[-iqn.2010-01.com.solidfire:4nhe.vol-1.27-0] i-2-18-VM/ROOT-18.vmdk" should return "i-2-18-VM/ROOT-18"
     */
    public String getVmdkPath(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }

        final String search = "]";

        int startIndex = path.indexOf(search);

        if (startIndex == -1) {
            return null;
        }

        path = path.substring(startIndex + search.length());

        final String search2 = VMDK_EXTENSION;

        int endIndex = path.indexOf(search2);

        if (endIndex == -1) {
            return null;
        }

        return path.substring(0, endIndex).trim();
    }

    private DatastoreMO getDataStoreWhereDiskExists(VmwareHypervisorHost hyperHost, VmwareContext context,
                                               VirtualMachineDiskInfoBuilder diskInfoBuilder, DiskTO disk, List<Pair<Integer, ManagedObjectReference>> diskDatastores) throws Exception {
        VolumeObjectTO volume = (VolumeObjectTO) disk.getData();
        String diskBackingFileBaseName = volume.getPath();
        for (Pair<Integer, ManagedObjectReference> diskDatastore : diskDatastores) {
            DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), diskDatastore.second());
            String dsName = dsMo.getName();

            VirtualMachineDiskInfo diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(diskBackingFileBaseName, dsName);
            if (diskInfo != null) {
                s_logger.info("Found existing disk info from volume path: " + volume.getPath());
                return dsMo;
            } else {
                String chainInfo = volume.getChainInfo();
                if (chainInfo != null) {
                    VirtualMachineDiskInfo infoInChain = _gson.fromJson(chainInfo, VirtualMachineDiskInfo.class);
                    if (infoInChain != null) {
                        String[] disks = infoInChain.getDiskChain();
                        if (disks.length > 0) {
                            for (String diskPath : disks) {
                                DatastoreFile file = new DatastoreFile(diskPath);
                                diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(file.getFileBaseName(), dsName);
                                if (diskInfo != null) {
                                    s_logger.info("Found existing disk from chain info: " + diskPath);
                                    return dsMo;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> inferDatastoreDetailsFromDiskInfo(VmwareHypervisorHost hyperHost, VmwareContext context,
                                                                                                         DiskTO[] disks, Command cmd) throws Exception {
        HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> mapIdToMors = new HashMap<>();

        assert (hyperHost != null) && (context != null);

        for (DiskTO vol : disks) {
            if (vol.getType() != Volume.Type.ISO) {
                VolumeObjectTO volumeTO = (VolumeObjectTO) vol.getData();
                DataStoreTO primaryStore = volumeTO.getDataStore();
                String poolUuid = primaryStore.getUuid();

                if (mapIdToMors.get(poolUuid) == null) {
                    boolean isManaged = false;
                    Map<String, String> details = vol.getDetails();

                    if (details != null) {
                        isManaged = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
                    }

                    if (isManaged) {
                        String iScsiName = details.get(DiskTO.IQN); // details should not be null for managed storage (it may or may not be null for non-managed storage)
                        String datastoreName = VmwareResource.getDatastoreName(iScsiName);
                        ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, datastoreName);

                        // if the datastore is not present, we need to discover the iSCSI device that will support it,
                        // create the datastore, and create a VMDK file in the datastore
                        if (morDatastore == null) {
                            final String vmdkPath = getVmdkPath(volumeTO.getPath());

                            morDatastore = _storageProcessor.prepareManagedStorage(context, hyperHost, null, iScsiName,
                                    details.get(DiskTO.STORAGE_HOST), Integer.parseInt(details.get(DiskTO.STORAGE_PORT)),
                                    vmdkPath,
                                    details.get(DiskTO.CHAP_INITIATOR_USERNAME), details.get(DiskTO.CHAP_INITIATOR_SECRET),
                                    details.get(DiskTO.CHAP_TARGET_USERNAME), details.get(DiskTO.CHAP_TARGET_SECRET),
                                    Long.parseLong(details.get(DiskTO.VOLUME_SIZE)), cmd);

                            DatastoreMO dsMo = new DatastoreMO(getServiceContext(), morDatastore);

                            final String datastoreVolumePath;

                            if (vmdkPath != null) {
                                datastoreVolumePath = dsMo.getDatastorePath(vmdkPath + VMDK_EXTENSION);
                            } else {
                                datastoreVolumePath = dsMo.getDatastorePath(dsMo.getName() + VMDK_EXTENSION);
                            }

                            volumeTO.setPath(datastoreVolumePath);
                            vol.setPath(datastoreVolumePath);
                        }

                        mapIdToMors.put(datastoreName, new Pair<>(morDatastore, new DatastoreMO(context, morDatastore)));
                    } else {
                        ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, poolUuid);

                        if (morDatastore == null) {
                            String msg = "Failed to get the mounted datastore for the volume's pool " + poolUuid;

                            s_logger.error(msg);

                            throw new Exception(msg);
                        }

                        mapIdToMors.put(poolUuid, new Pair<>(morDatastore, new DatastoreMO(context, morDatastore)));
                    }
                }
            }
        }

        return mapIdToMors;
    }

    private Pair<ManagedObjectReference, DatastoreMO> getDatastoreThatDiskIsOn(HashMap<String, Pair<ManagedObjectReference, DatastoreMO>> dataStoresDetails, DiskTO vol) {
        Pair<ManagedObjectReference, DatastoreMO> rootDiskDataStoreDetails = null;

        Map<String, String> details = vol.getDetails();
        boolean managed = false;

        if (details != null) {
            managed = Boolean.parseBoolean(details.get(DiskTO.MANAGED));
        }

        if (managed) {
            String datastoreName = VmwareResource.getDatastoreName(details.get(DiskTO.IQN));
            rootDiskDataStoreDetails = dataStoresDetails.get(datastoreName);
        } else {
            DataStoreTO primaryStore = vol.getData().getDataStore();
            rootDiskDataStoreDetails = dataStoresDetails.get(primaryStore.getUuid());
        }

        return rootDiskDataStoreDetails;
    }

    private String getPvlanInfo(NicTO nicTo) {
        if (nicTo.getBroadcastType() == BroadcastDomainType.Pvlan) {
            return NetUtils.getIsolatedPvlanFromUri(nicTo.getBroadcastUri());
        }
        return null;
    }

    private String getVlanInfo(NicTO nicTo, String defaultVlan) {
        if (nicTo.getBroadcastType() == BroadcastDomainType.Native) {
            return defaultVlan;
        } else if (nicTo.getBroadcastType() == BroadcastDomainType.Vlan || nicTo.getBroadcastType() == BroadcastDomainType.Pvlan) {
            if (nicTo.getBroadcastUri() != null) {
                if (nicTo.getBroadcastType() == BroadcastDomainType.Vlan)
                    // For vlan, the broadcast uri is of the form vlan://<vlanid>
                    // BroadcastDomainType recogniizes and handles this.
                    return BroadcastDomainType.getValue(nicTo.getBroadcastUri());
                else
                    // for pvlan, the broadcast uri will be of the form pvlan://<vlanid>-i<pvlanid>
                    // TODO consider the spread of functionality between BroadcastDomainType and NetUtils
                    return NetUtils.getPrimaryPvlanFromUri(nicTo.getBroadcastUri());
            } else {
                s_logger.warn("BroadcastType is not claimed as VLAN or PVLAN, but without vlan info in broadcast URI. Use vlan info from labeling: " + defaultVlan);
                return defaultVlan;
            }
        } else if (nicTo.getBroadcastType() == BroadcastDomainType.Lswitch) {
            // We don't need to set any VLAN id for an NVP logical switch
            return null;
        } else if (nicTo.getBroadcastType() == BroadcastDomainType.Storage) {
            URI broadcastUri = nicTo.getBroadcastUri();
            if (broadcastUri != null) {
                String vlanId = BroadcastDomainType.getValue(broadcastUri);
                s_logger.debug("Using VLAN [" + vlanId + "] from broadcast uri [" + broadcastUri + "]");
                return vlanId;
            }
        }

        s_logger.warn("Unrecognized broadcast type in VmwareResource, type: " + nicTo.getBroadcastType().toString() + ". Use vlan info from labeling: " + defaultVlan);
        return defaultVlan;
    }

    private Pair<ManagedObjectReference, String> prepareNetworkFromNicInfo(HostMO hostMo, NicTO nicTo, boolean configureVServiceInNexus, VirtualMachine.Type vmType)
            throws Exception {

        Ternary<String, String, String> switchDetails = getTargetSwitch(nicTo);
        VirtualSwitchType switchType = VirtualSwitchType.getType(switchDetails.second());
        String switchName = switchDetails.first();
        String vlanToken = switchDetails.third();

        String namePrefix = getNetworkNamePrefix(nicTo);
        Pair<ManagedObjectReference, String> networkInfo = null;

        s_logger.info("Prepare network on " + switchType + " " + switchName + " with name prefix: " + namePrefix);

        if (VirtualSwitchType.StandardVirtualSwitch == switchType) {
            networkInfo = HypervisorHostHelper.prepareNetwork(switchName, namePrefix, hostMo,
                    getVlanInfo(nicTo, vlanToken), nicTo.getNetworkRateMbps(), nicTo.getNetworkRateMulticastMbps(),
                    _opsTimeout, true, nicTo.getBroadcastType(), nicTo.getUuid(), nicTo.getDetails());
        } else {
            String vlanId = getVlanInfo(nicTo, vlanToken);
            String svlanId = null;
            boolean pvlannetwork = (getPvlanInfo(nicTo) == null) ? false : true;
            if (vmType != null && vmType.equals(VirtualMachine.Type.DomainRouter) && pvlannetwork) {
                // plumb this network to the promiscuous vlan.
                svlanId = vlanId;
            } else {
                // plumb this network to the isolated vlan.
                svlanId = getPvlanInfo(nicTo);
            }
            networkInfo = HypervisorHostHelper.prepareNetwork(switchName, namePrefix, hostMo, vlanId, svlanId,
                    nicTo.getNetworkRateMbps(), nicTo.getNetworkRateMulticastMbps(), _opsTimeout, switchType,
                    _portsPerDvPortGroup, nicTo.getGateway(), configureVServiceInNexus, nicTo.getBroadcastType(), _vsmCredentials, nicTo.getDetails());
        }

        return networkInfo;
    }

    // return Ternary <switch name, switch type, vlan tagging>
    private Ternary<String, String, String> getTargetSwitch(NicTO nicTo) throws CloudException {
        TrafficType[] supportedTrafficTypes = new TrafficType[]{TrafficType.Guest, TrafficType.Public, TrafficType.Control, TrafficType.Management, TrafficType.Storage};

        TrafficType trafficType = nicTo.getType();
        if (!Arrays.asList(supportedTrafficTypes).contains(trafficType)) {
            throw new CloudException("Traffic type " + trafficType.toString() + " for nic " + nicTo.toString() + " is not supported.");
        }

        String switchName = null;
        VirtualSwitchType switchType = VirtualSwitchType.StandardVirtualSwitch;
        String vlanId = Vlan.UNTAGGED;

        if (StringUtils.isNotBlank(nicTo.getName())) {
            // Format of network traffic label is <VSWITCH>,<VLANID>,<VSWITCHTYPE>
            // If all 3 fields are mentioned then number of tokens would be 3.
            // If only <VSWITCH>,<VLANID> are mentioned then number of tokens would be 2.
            // Get switch details from the nicTO object
            String networkName = nicTo.getName();
            VmwareTrafficLabel mgmtTrafficLabelObj = new VmwareTrafficLabel(networkName, trafficType);
            switchName = mgmtTrafficLabelObj.getVirtualSwitchName();
            vlanId = mgmtTrafficLabelObj.getVlanId();
            switchType = mgmtTrafficLabelObj.getVirtualSwitchType();
        } else {
            if (trafficType == TrafficType.Guest && _guestTrafficInfo != null) {
                switchType = _guestTrafficInfo.getVirtualSwitchType();
                switchName = _guestTrafficInfo.getVirtualSwitchName();
            } else if (trafficType == TrafficType.Public && _publicTrafficInfo != null) {
                switchType = _publicTrafficInfo.getVirtualSwitchType();
                switchName = _publicTrafficInfo.getVirtualSwitchName();
            }
        }

        if (switchName == null
                && (nicTo.getType() == Networks.TrafficType.Control || nicTo.getType() == Networks.TrafficType.Management || nicTo.getType() == Networks.TrafficType.Storage)) {
            switchName = _privateNetworkVSwitchName;
        }

        if (switchType == VirtualSwitchType.NexusDistributedVirtualSwitch) {
            if (trafficType == TrafficType.Management || trafficType == TrafficType.Storage) {
                throw new CloudException(
                        "Unable to configure NIC " + nicTo.toString() + " as traffic type " + trafficType.toString() + " is not supported over virtual switch type " + switchType
                                + ". Please specify only supported type of virtual switches i.e. {vmwaresvs, vmwaredvs} in physical network traffic label.");
            }
        }

        return new Ternary<String, String, String>(switchName, switchType.toString(), vlanId);
    }

    private String getNetworkNamePrefix(NicTO nicTo) throws Exception {
        if (nicTo.getType() == Networks.TrafficType.Guest) {
            return "cloud.guest";
        } else if (nicTo.getType() == Networks.TrafficType.Control || nicTo.getType() == Networks.TrafficType.Management) {
            return "cloud.private";
        } else if (nicTo.getType() == Networks.TrafficType.Public) {
            return "cloud.public";
        } else if (nicTo.getType() == Networks.TrafficType.Storage) {
            return "cloud.storage";
        } else if (nicTo.getType() == Networks.TrafficType.Vpn) {
            throw new Exception("Unsupported traffic type: " + nicTo.getType().toString());
        } else {
            throw new Exception("Unsupported traffic type: " + nicTo.getType().toString());
        }
    }

    private String getControlIp(NicTO[] nicTOs) {
        String controlIpAddress = null;
        for (NicTO nic : nicTOs) {
            if ((TrafficType.Management == nic.getType() || TrafficType.Control == nic.getType()) && nic.getIp() != null) {
                controlIpAddress = nic.getIp();
                break;
            }
        }
        return controlIpAddress;
    }

    private VirtualMachineMO takeVmFromOtherHyperHost(VmwareHypervisorHost hyperHost, String vmName) throws Exception {

        VirtualMachineMO vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
        if (vmMo != null) {
            ManagedObjectReference morTargetPhysicalHost = hyperHost.findMigrationTarget(vmMo);
            if (morTargetPhysicalHost == null) {
                String msg = "VM " + vmName + " is on other host and we have no resource available to migrate and start it here";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            if (!vmMo.relocate(morTargetPhysicalHost)) {
                String msg = "VM " + vmName + " is on other host and we failed to relocate it here";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            return vmMo;
        }
        return null;
    }

    // isoUrl sample content :
    // nfs://192.168.10.231/export/home/kelven/vmware-test/secondary/template/tmpl/2/200//200-2-80f7ee58-6eff-3a2d-bcb0-59663edf6d26.iso
    private Pair<String, ManagedObjectReference> getIsoDatastoreInfo(VmwareHypervisorHost hyperHost, String isoUrl) throws Exception {

        assert (isoUrl != null);
        int isoFileNameStartPos = isoUrl.lastIndexOf("/");
        if (isoFileNameStartPos < 0) {
            throw new Exception("Invalid ISO path info");
        }

        String isoFileName = isoUrl.substring(isoFileNameStartPos);

        int templateRootPos = isoUrl.indexOf("template/tmpl");
        templateRootPos = (templateRootPos < 0 ? isoUrl.indexOf(ConfigDrive.CONFIGDRIVEDIR) : templateRootPos);
        if (templateRootPos < 0) {
            throw new Exception("Invalid ISO path info");
        }

        String storeUrl = isoUrl.substring(0, templateRootPos - 1);
        String isoPath = isoUrl.substring(templateRootPos, isoFileNameStartPos);

        ManagedObjectReference morDs = prepareSecondaryDatastoreOnHost(storeUrl);
        DatastoreMO dsMo = new DatastoreMO(getServiceContext(), morDs);

        return new Pair<String, ManagedObjectReference>(String.format("[%s] %s%s", dsMo.getName(), isoPath, isoFileName), morDs);
    }

    protected Answer execute(ReadyCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost hyperHost = getHyperHost(context);

            Map<String, String> hostDetails = new HashMap<String, String>();
            ManagedObjectReference morHost = hyperHost.getMor();
            HostMO hostMo = new HostMO(context, morHost);
            boolean uefiLegacySupported = hostMo.isUefiLegacySupported();
            if (uefiLegacySupported) {
                hostDetails.put(Host.HOST_UEFI_ENABLE, Boolean.TRUE.toString());
            }

            if (hyperHost.isHyperHostConnected()) {
                return new ReadyAnswer(cmd, hostDetails);
            } else {
                return new ReadyAnswer(cmd, "Host is not in connect state");
            }
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
            return new ReadyAnswer(cmd, VmwareHelper.getExceptionMessage(e));
        }
    }

    protected Answer execute(GetHostStatsCommand cmd) {
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);

        HostStatsEntry hostStats = new HostStatsEntry(cmd.getHostId(), 0, 0, 0, "host", 0, 0, 0, 0);
        Answer answer = new GetHostStatsAnswer(cmd, hostStats);
        try {
            HostStatsEntry entry = getHyperHostStats(hyperHost);
            if (entry != null) {
                s_logger.debug(String.format("Host stats response from hypervisor is: [%s].", _gson.toJson(entry)));
                entry.setHostId(cmd.getHostId());
                answer = new GetHostStatsAnswer(cmd, entry);
            }
        } catch (Exception e) {
            s_logger.error(createLogMessageException(e, cmd), e);
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("GetHostStats Answer: " + _gson.toJson(answer));
        }

        return answer;
    }

    protected Answer execute(GetVmStatsCommand cmd) {
        HashMap<String, VmStatsEntry> vmStatsMap = null;

        try {
            HashMap<String, PowerState> vmPowerStates = getVmStates();

            // getVmNames should return all i-x-y values.
            List<String> requestedVmNames = cmd.getVmNames();
            List<String> vmNames = new ArrayList<String>();

            if (requestedVmNames != null) {
                for (String vmName : requestedVmNames) {
                    if (vmPowerStates.get(vmName) != null) {
                        vmNames.add(vmName);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(vmNames)) {
                vmStatsMap = getVmStats(vmNames);
            }
        } catch (Throwable e) {
            createLogMessageException(e, cmd);
        }

        s_logger.debug(String.format("VM Stats Map is: [%s].", _gson.toJson(vmStatsMap)));
        return new GetVmStatsAnswer(cmd, vmStatsMap);
    }

    protected Answer execute(GetVmDiskStatsCommand cmd) {
        try {
            final VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            final ManagedObjectReference perfMgr = getServiceContext().getServiceContent().getPerfManager();
            VimPortType service = getServiceContext().getService();

            Integer windowInterval = getVmwareWindowTimeInterval();
            final XMLGregorianCalendar startTime = VmwareHelper.getXMLGregorianCalendar(new Date(), windowInterval);
            final XMLGregorianCalendar endTime = VmwareHelper.getXMLGregorianCalendar(new Date(), 0);

            PerfCounterInfo diskReadIOPerfCounterInfo = null;
            PerfCounterInfo diskWriteIOPerfCounterInfo = null;
            PerfCounterInfo diskReadKbsPerfCounterInfo = null;
            PerfCounterInfo diskWriteKbsPerfCounterInfo = null;

            // https://pubs.vmware.com/vsphere-5-5/topic/com.vmware.wssdk.apiref.doc/virtual_disk_counters.html
            List<PerfCounterInfo> cInfo = getServiceContext().getVimClient().getDynamicProperty(perfMgr, "perfCounter");
            for (PerfCounterInfo info : cInfo) {
                if ("virtualdisk".equalsIgnoreCase(info.getGroupInfo().getKey()) && "average".equalsIgnoreCase(info.getRollupType().value())) {
                    if ("numberReadAveraged".equalsIgnoreCase(info.getNameInfo().getKey())) {
                        diskReadIOPerfCounterInfo = info;
                    }
                    if ("numberWriteAveraged".equalsIgnoreCase(info.getNameInfo().getKey())) {
                        diskWriteIOPerfCounterInfo = info;
                    }
                    if ("read".equalsIgnoreCase(info.getNameInfo().getKey())) {
                        diskReadKbsPerfCounterInfo = info;
                    }
                    if ("write".equalsIgnoreCase(info.getNameInfo().getKey())) {
                        diskWriteKbsPerfCounterInfo = info;
                    }
                }
            }

            final ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
            final DatacenterMO dcMo = new DatacenterMO(getServiceContext(), dcMor);

            final HashMap<String, List<VmDiskStatsEntry>> vmStatsMap = new HashMap<>();
            for (final String vmName : cmd.getVmNames()) {
                final VirtualMachineMO vmMo = dcMo.findVm(vmName);
                final List<VmDiskStatsEntry> diskStats = new ArrayList<>();
                for (final VirtualDisk disk : vmMo.getAllDiskDevice()) {
                    final String diskBusName = vmMo.getDeviceBusName(vmMo.getAllDeviceList(), disk);
                    long readReq = 0;
                    long readBytes = 0;
                    long writeReq = 0;
                    long writeBytes = 0;

                    final ArrayList<PerfMetricId> perfMetricsIds = new ArrayList<PerfMetricId>();
                    if (diskReadIOPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskReadIOPerfCounterInfo, diskBusName));
                    }
                    if (diskWriteIOPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskWriteIOPerfCounterInfo, diskBusName));
                    }
                    if (diskReadKbsPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskReadKbsPerfCounterInfo, diskBusName));
                    }
                    if (diskWriteKbsPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskWriteKbsPerfCounterInfo, diskBusName));
                    }

                    if (perfMetricsIds.size() > 0) {
                        try {
                            final PerfQuerySpec qSpec = new PerfQuerySpec();
                            qSpec.setEntity(vmMo.getMor());
                            qSpec.setFormat("normal");
                            qSpec.setIntervalId(windowInterval);
                            qSpec.setStartTime(startTime);
                            qSpec.setEndTime(endTime);
                            qSpec.getMetricId().addAll(perfMetricsIds);

                            for (final PerfEntityMetricBase perfValue: service.queryPerf(perfMgr, Collections.singletonList(qSpec))) {
                                if (!(perfValue instanceof PerfEntityMetric)) {
                                    continue;
                                }
                                final List<PerfMetricSeries> values = ((PerfEntityMetric) perfValue).getValue();
                                if (values == null || values.isEmpty()) {
                                    continue;
                                }
                                for (final PerfMetricSeries value : values) {
                                    if (!(value instanceof PerfMetricIntSeries) || !value.getId().getInstance().equals(diskBusName)) {
                                        continue;
                                    }
                                    final List<Long> perfStats = ((PerfMetricIntSeries) value).getValue();
                                    if (perfStats.size() > 0) {
                                        long sum = 0;
                                        for (long val : perfStats) {
                                            sum += val;
                                        }
                                        long avg = sum / perfStats.size();
                                        if (value.getId().getCounterId() == diskReadIOPerfCounterInfo.getKey()) {
                                            readReq = avg;
                                        } else if (value.getId().getCounterId() == diskWriteIOPerfCounterInfo.getKey()) {
                                            writeReq = avg;
                                        } else if (value.getId().getCounterId() == diskReadKbsPerfCounterInfo.getKey()) {
                                            readBytes = avg * 1024;
                                        } else if (value.getId().getCounterId() == diskWriteKbsPerfCounterInfo.getKey()) {
                                            writeBytes = avg * 1024;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            s_logger.error(String.format("Unable to execute PerfQuerySpec due to: [%s]. The window interval is enabled in vCenter?", VmwareHelper.getExceptionMessage(e)), e);
                        }

                    }
                    diskStats.add(new VmDiskStatsEntry(vmName, VmwareHelper.getDiskDeviceFileName(disk), writeReq, readReq, writeBytes, readBytes));
                }
                if (CollectionUtils.isNotEmpty(diskStats)) {
                    vmStatsMap.put(vmName, diskStats);
                }
            }

            s_logger.debug(String.format("VM Disks Maps is: [%s].", _gson.toJson(vmStatsMap)));
            if (MapUtils.isNotEmpty(vmStatsMap)) {
                return new GetVmDiskStatsAnswer(cmd, "", cmd.getHostName(), vmStatsMap);
            }
        } catch (Exception e) {
            s_logger.error(String.format("Unable to execute GetVmDiskStatsCommand due to [%s].", VmwareHelper.getExceptionMessage(e)), e);
        }
        return new GetVmDiskStatsAnswer(cmd, null, null, null);
    }

    protected Answer execute(GetVmNetworkStatsCommand cmd) {
        return new GetVmNetworkStatsAnswer(cmd, null, null, null);
    }

    protected GetVolumeStatsAnswer execute(GetVolumeStatsCommand cmd) {
        try {
            VmwareHypervisorHost srcHyperHost = getHyperHost(getServiceContext());
            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(srcHyperHost, cmd.getPoolUuid());
            assert (morDs != null);
            DatastoreMO primaryStorageDatastoreMo = new DatastoreMO(getServiceContext(), morDs);
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
            DatacenterMO dcMo = new DatacenterMO(getServiceContext(), dcMor);
            HashMap<String, VolumeStatsEntry> statEntry = new HashMap<String, VolumeStatsEntry>();

            for (String chainInfo : cmd.getVolumeUuids()) {
                if (chainInfo != null) {
                    VirtualMachineDiskInfo infoInChain = _gson.fromJson(chainInfo, VirtualMachineDiskInfo.class);
                    if (infoInChain != null) {
                        String[] disks = infoInChain.getDiskChain();
                        if (disks.length > 0) {
                            for (String diskPath : disks) {
                                DatastoreFile file = new DatastoreFile(diskPath);
                                VirtualMachineMO vmMo = dcMo.findVm(file.getDir());
                                Pair<VirtualDisk, String> vds = vmMo.getDiskDevice(file.getFileName(), true);
                                long virtualsize = vds.first().getCapacityInKB() * 1024;
                                long physicalsize = primaryStorageDatastoreMo.fileDiskSize(file.getPath());
                                if (statEntry.containsKey(chainInfo)) {
                                    VolumeStatsEntry vse = statEntry.get(chainInfo);
                                    if (vse != null) {
                                        vse.setPhysicalSize(vse.getPhysicalSize() + physicalsize);
                                    }
                                } else {
                                    VolumeStatsEntry vse = new VolumeStatsEntry(chainInfo, physicalsize, virtualsize);
                                    statEntry.put(chainInfo, vse);
                                }
                            }
                        }
                    }
                }
            }
            s_logger.debug(String.format("Volume Stats Entry is: [%s].", _gson.toJson(statEntry)));
            return new GetVolumeStatsAnswer(cmd, "", statEntry);
        } catch (Exception e) {
            s_logger.error(String.format("VOLSTAT GetVolumeStatsCommand failed due to [%s].", VmwareHelper.getExceptionMessage(e)), e);
        }

        return new GetVolumeStatsAnswer(cmd, "", null);
    }

    protected Answer execute(CheckHealthCommand cmd) {
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            if (hyperHost.isHyperHostConnected()) {
                return new CheckHealthAnswer(cmd, true);
            }
        } catch (Throwable e) {
            createLogMessageException(e, cmd);
        }
        return new CheckHealthAnswer(cmd, false);
    }

    protected Answer execute(StopCommand cmd) {
        // In the stop command, we're passed in the name of the VM as seen by cloudstack,
        // i.e., i-x-y. This is the internal VM name.
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        try {
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
            if (vmMo != null) {
                if (cmd.checkBeforeCleanup()) {
                    if (getVmPowerState(vmMo) != PowerState.PowerOff) {
                        String msg = "StopCommand is sent for cleanup and VM " + cmd.getVmName() + " is current running. ignore it.";
                        s_logger.warn(msg);
                        return new StopAnswer(cmd, msg, false);
                    } else {
                        String msg = "StopCommand is sent for cleanup and VM " + cmd.getVmName() + " is indeed stopped already.";
                        s_logger.info(msg);
                        return new StopAnswer(cmd, msg, true);
                    }
                }

                try {
                    vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_NIC_MASK, "0");
                    vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_VM_INTERNAL_NAME, cmd.getVmName());

                    if (getVmPowerState(vmMo) != PowerState.PowerOff) {
                        String msg = "Stop VM " + cmd.getVmName() + " Succeed";
                        boolean success = false;
                        if (cmd.isForceStop()) {
                            success = vmMo.powerOff();
                        } else {
                            success = vmMo.safePowerOff(_shutdownWaitMs);
                        }
                        if (!success) {
                            msg = "Have problem in powering off VM " + cmd.getVmName() + ", let the process continue";
                            s_logger.warn(msg);
                        }
                        return new StopAnswer(cmd, msg, true);
                    }

                    String msg = "VM " + cmd.getVmName() + " is already in stopped state";
                    s_logger.info(msg);
                    return new StopAnswer(cmd, msg, true);
                } finally {
                }
            } else {
                String msg = "VM " + cmd.getVmName() + " is no longer on the expected host in vSphere";
                s_logger.info(msg);
                return new StopAnswer(cmd, msg, true);
            }
        } catch (Exception e) {
            return new StopAnswer(cmd, createLogMessageException(e, cmd), false);
        }
    }

    protected Answer execute(RebootRouterCommand cmd) {
        RebootAnswer answer = (RebootAnswer) execute((RebootCommand) cmd);

        if (answer.getResult()) {
            String connectResult = connect(cmd.getVmName(), cmd.getPrivateIpAddress());
            networkUsage(cmd.getPrivateIpAddress(), "create", null);
            if (connectResult == null) {
                return answer;
            } else {
                return new Answer(cmd, false, connectResult);
            }
        }
        return answer;
    }

    protected Answer execute(RebootCommand cmd) {
        boolean toolsInstallerMounted = false;
        VirtualMachineMO vmMo = null;
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        try {
            vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
            if (vmMo != null) {
                if (vmMo.isToolsInstallerMounted()) {
                    toolsInstallerMounted = true;
                    s_logger.trace("Detected mounted vmware tools installer for :[" + cmd.getVmName() + "]");
                }
                try {
                    if (canSetEnableSetupConfig(vmMo,cmd.getVirtualMachine())) {
                        vmMo.rebootGuest();
                        return new RebootAnswer(cmd, "reboot succeeded", true);
                    } else {
                        return new RebootAnswer(cmd, "Failed to configure VM to boot into hardware setup menu: " + vmMo.getName(), false);
                    }
                } catch (ToolsUnavailableFaultMsg e) {
                    s_logger.warn("VMware tools is not installed at guest OS, we will perform hard reset for reboot");
                } catch (Exception e) {
                    s_logger.warn("We are not able to perform gracefull guest reboot due to " + VmwareHelper.getExceptionMessage(e));
                }

                // continue to try with hard-reset
                if (vmMo.reset()) {
                    return new RebootAnswer(cmd, "reboot succeeded", true);
                }

                String msg = "Reboot failed in vSphere. vm: " + cmd.getVmName();
                s_logger.warn(msg);
                return new RebootAnswer(cmd, msg, false);
            } else {
                String msg = "Unable to find the VM in vSphere to reboot. vm: " + cmd.getVmName();
                s_logger.warn(msg);
                return new RebootAnswer(cmd, msg, false);
            }
        } catch (Exception e) {
            return new RebootAnswer(cmd, createLogMessageException(e, cmd), false);
        } finally {
            if (toolsInstallerMounted) {
                try {
                    vmMo.mountToolsInstaller();
                    s_logger.debug(String.format("Successfully re-mounted vmware tools installer for :[%s].", cmd.getVmName()));
                } catch (Exception e) {
                    s_logger.error(String.format("Unabled to re-mount vmware tools installer for: [%s].", cmd.getVmName()), e);
                }
            }
        }
    }

    /**
     * set the boot into setup option if possible
     * @param vmMo vmware view on the vm
     * @param virtualMachine orchestration spec for the vm
     * @return true unless reboot into setup is requested and vmware is unable to comply
     */
    private boolean canSetEnableSetupConfig(VirtualMachineMO vmMo, VirtualMachineTO virtualMachine) {
        if (virtualMachine.isEnterHardwareSetup()) {
            VirtualMachineBootOptions bootOptions = new VirtualMachineBootOptions();
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("configuring VM '%s' to reboot into hardware setup menu.",virtualMachine.getName()));
            }
            bootOptions.setEnterBIOSSetup(virtualMachine.isEnterHardwareSetup());
            vmConfigSpec.setBootOptions(bootOptions);
            try {
                if (!vmMo.configureVm(vmConfigSpec)) {
                    return false;
                }
            } catch (Exception e) {
                s_logger.error(String.format("failed to reconfigure VM '%s' to boot into hardware setup menu",virtualMachine.getName()),e);
                return false;
            }
        }
        return true;
    }

    protected Answer execute(CheckVirtualMachineCommand cmd) {
        final String vmName = cmd.getVmName();
        PowerState powerState = PowerState.PowerUnknown;
        Integer vncPort = null;

        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);

        try {
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo != null) {
                powerState = getVmPowerState(vmMo);
                return new CheckVirtualMachineAnswer(cmd, powerState, vncPort);
            } else {
                s_logger.warn("Can not find vm " + vmName + " to execute CheckVirtualMachineCommand");
                return new CheckVirtualMachineAnswer(cmd, powerState, vncPort);
            }

        } catch (Throwable e) {
            createLogMessageException(e, cmd);
            return new CheckVirtualMachineAnswer(cmd, powerState, vncPort);
        }
    }

    protected Answer execute(PrepareForMigrationCommand cmd) {
        VirtualMachineTO vm = cmd.getVirtualMachine();
        final String vmName = vm.getName();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            VmwareManager mgr = hyperHost.getContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            // find VM through datacenter (VM is not at the target host yet)
            VirtualMachineMO vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
            if (vmMo == null) {
                s_logger.info("VM " + vmName + " was not found in the cluster of host " + hyperHost.getHyperHostName() + ". Looking for the VM in datacenter.");
                ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                DatacenterMO dcMo = new DatacenterMO(hyperHost.getContext(), dcMor);
                vmMo = dcMo.findVm(vmName);
                if (vmMo == null) {
                    String msg = "VM " + vmName + " does not exist in VMware datacenter";
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
            }

            NicTO[] nics = vm.getNics();
            for (NicTO nic : nics) {
                // prepare network on the host
                prepareNetworkFromNicInfo(new HostMO(getServiceContext(), _morHyperHost), nic, false, cmd.getVirtualMachine().getType());
            }

            List<Pair<String, Long>> secStoreUrlAndIdList = mgr.getSecondaryStorageStoresUrlAndIdList(Long.parseLong(_dcId));
            for (Pair<String, Long> secStoreUrlAndId : secStoreUrlAndIdList) {
                String secStoreUrl = secStoreUrlAndId.first();
                Long secStoreId = secStoreUrlAndId.second();
                if (secStoreUrl == null) {
                    String msg = String.format("Secondary storage for dc %s is not ready yet?", _dcId);
                    throw new Exception(msg);
                }

                ManagedObjectReference morSecDs = prepareSecondaryDatastoreOnHost(secStoreUrl);
                if (morSecDs == null) {
                    String msg = "Failed to prepare secondary storage on host, secondary store url: " + secStoreUrl;
                    throw new Exception(msg);
                }
            }
            return new PrepareForMigrationAnswer(cmd);
        } catch (Throwable e) {
            return new PrepareForMigrationAnswer(cmd, createLogMessageException(e, cmd));
        }
    }

    protected Answer execute(MigrateVmToPoolCommand cmd) {
        final String vmName = cmd.getVmName();

        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
        try {
            VirtualMachineMO vmMo = getVirtualMachineMO(vmName, hyperHost);
            if (vmMo == null) {
                s_logger.info("VM " + vmName + " was not found in the cluster of host " + hyperHost.getHyperHostName() + ". Looking for the VM in datacenter.");
                ManagedObjectReference dcMor = hyperHost.getHyperHostDatacenter();
                DatacenterMO dcMo = new DatacenterMO(hyperHost.getContext(), dcMor);
                vmMo = dcMo.findVm(vmName);
                if (vmMo == null) {
                    String msg = "VM " + vmName + " does not exist in VMware datacenter";
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
            }
            return migrateAndAnswer(vmMo, null, hyperHost, cmd);
        } catch (Throwable e) { // hopefully only CloudRuntimeException :/
            if (e instanceof Exception) {
                return new Answer(cmd, (Exception) e);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("problem", e);
            }
            s_logger.error(e.getLocalizedMessage());
            return new Answer(cmd, false, "unknown problem: " + e.getLocalizedMessage());
        }
    }

    private Answer migrateAndAnswer(VirtualMachineMO vmMo, String poolUuid, VmwareHypervisorHost hyperHost, Command cmd) throws Exception {
        String hostNameInTargetCluster = null;
        List<Pair<VolumeTO, StorageFilerTO>> volToFiler = new ArrayList<>();
        if (cmd instanceof MigrateVmToPoolCommand) {
            MigrateVmToPoolCommand mcmd = (MigrateVmToPoolCommand)cmd;
            hostNameInTargetCluster = mcmd.getHostGuidInTargetCluster();
            volToFiler = mcmd.getVolumeToFilerAsList();
        } else if (cmd instanceof MigrateVolumeCommand) {
            hostNameInTargetCluster = ((MigrateVolumeCommand)cmd).getHostGuidInTargetCluster();
        }
        VmwareHypervisorHost hostInTargetCluster = VmwareHelper.getHostMOFromHostName(getServiceContext(),
                hostNameInTargetCluster);

        try {
            // OfflineVmwareMigration: getVolumesFromCommand(cmd);
            Map<Integer, Long> volumeDeviceKey = new HashMap<>();
            if (cmd instanceof MigrateVolumeCommand) { // Else device keys will be found in relocateVirtualMachine
                MigrateVolumeCommand mcmd = (MigrateVolumeCommand) cmd;
                addVolumeDiskmapping(vmMo, volumeDeviceKey, mcmd.getVolumePath(), mcmd.getVolumeId());
                if (s_logger.isTraceEnabled()) {
                    for (Integer diskId: volumeDeviceKey.keySet()) {
                        s_logger.trace(String.format("Disk to migrate has disk id %d and volumeId %d", diskId, volumeDeviceKey.get(diskId)));
                    }
                }
            }
            List<VolumeObjectTO> volumeToList = relocateVirtualMachine(hyperHost, vmMo.getName(), null, null, hostInTargetCluster, poolUuid, volToFiler);
            return createAnswerForCmd(vmMo, volumeToList, cmd, volumeDeviceKey);
        } catch (Exception e) {
            String msg = "Change data store for VM " + vmMo.getVmName() + " failed";
            s_logger.error(msg + ": " + e.getLocalizedMessage());
            throw new CloudRuntimeException(msg, e);
        }
    }

    Answer createAnswerForCmd(VirtualMachineMO vmMo, List<VolumeObjectTO> volumeObjectToList, Command cmd, Map<Integer, Long> volumeDeviceKey) throws Exception {
        List<VolumeObjectTO> volumeToList = new ArrayList<>();
        VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
        VirtualDisk[] disks = vmMo.getAllDiskDevice();
        Answer answer;
        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("creating answer for %s", cmd.getClass().getSimpleName()));
        }
        if (cmd instanceof MigrateVolumeCommand) {
            if (disks.length == 1) {
                String volumePath = vmMo.getVmdkFileBaseName(disks[0]);
                return new MigrateVolumeAnswer(cmd, true, null, volumePath);
            }
            throw new CloudRuntimeException("not expecting more then  one disk after migrate volume command");
        } else if (cmd instanceof MigrateVmToPoolCommand) {
            volumeToList = volumeObjectToList;
            return new MigrateVmToPoolAnswer((MigrateVmToPoolCommand)cmd, volumeToList);
        }
        return new Answer(cmd, false, null);
    }

    private void addVolumeDiskmapping(VirtualMachineMO vmMo, Map<Integer, Long> volumeDeviceKey, String volumePath, long volumeId) throws Exception {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("locating disk for volume (%d) using path %s", volumeId, volumePath));
        }
        Pair<VirtualDisk, String> diskInfo = getVirtualDiskInfo(vmMo, volumePath + VMDK_EXTENSION);
        String vmdkAbsFile = getAbsoluteVmdkFile(diskInfo.first());
        if (vmdkAbsFile != null && !vmdkAbsFile.isEmpty()) {
            vmMo.updateAdapterTypeIfRequired(vmdkAbsFile);
        }
        int diskId = diskInfo.first().getKey();
        volumeDeviceKey.put(diskId, volumeId);
    }

    private ManagedObjectReference getTargetDatastoreMOReference(String destinationPool,
                                                                 VmwareHypervisorHost hyperHost) {
        ManagedObjectReference morDs;
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("finding datastore %s", destinationPool));
            }
            morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, destinationPool);
        } catch (Exception e) {
            String msg = "exception while finding data store  " + destinationPool;
            s_logger.error(msg);
            throw new CloudRuntimeException(msg + ": " + e.getLocalizedMessage());
        }
        return morDs;
    }

    private ManagedObjectReference getDataCenterMOReference(String vmName, VmwareHypervisorHost hyperHost) {
        ManagedObjectReference morDc;
        try {
            morDc = hyperHost.getHyperHostDatacenter();
        } catch (Exception e) {
            String msg = "exception while finding VMware datacenter to search for VM " + vmName;
            s_logger.error(msg);
            throw new CloudRuntimeException(msg + ": " + e.getLocalizedMessage());
        }
        return morDc;
    }

    private VirtualMachineMO getVirtualMachineMO(String vmName, VmwareHypervisorHost hyperHost) {
        VirtualMachineMO vmMo = null;
        try {
            // find VM through datacenter (VM is not at the target host yet)
            vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
        } catch (Exception e) {
            String msg = "exception while searching for VM " + vmName + " in VMware datacenter";
            s_logger.error(msg);
            throw new CloudRuntimeException(msg + ": " + e.getLocalizedMessage());
        }
        return vmMo;
    }

    protected Answer execute(MigrateCommand cmd) {
        final String vmName = cmd.getVmName();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            ManagedObjectReference morDc = hyperHost.getHyperHostDatacenter();

            // find VM through datacenter (VM is not at the target host yet)
            VirtualMachineMO vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
            if (vmMo == null) {
                String msg = "VM " + vmName + " does not exist in VMware datacenter";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            VmwareHypervisorHost destHyperHost = getTargetHyperHost(new DatacenterMO(hyperHost.getContext(), morDc), cmd.getDestinationIp());

            ManagedObjectReference morTargetPhysicalHost = destHyperHost.findMigrationTarget(vmMo);
            if (morTargetPhysicalHost == null) {
                throw new Exception("Unable to find a target capable physical host");
            }

            if (!vmMo.migrate(destHyperHost.getHyperHostOwnerResourcePool(), morTargetPhysicalHost)) {
                throw new Exception("Migration failed");
            }

            return new MigrateAnswer(cmd, true, "migration succeeded", null);
        } catch (Throwable e) {
            return new MigrateAnswer(cmd, false, createLogMessageException(e, cmd), null);
        }
    }

    protected Answer execute(MigrateWithStorageCommand cmd) {
        final VirtualMachineTO vmTo = cmd.getVirtualMachine();
        final List<Pair<VolumeTO, StorageFilerTO>> volToFiler = cmd.getVolumeToFilerAsList();
        final String targetHost = cmd.getTargetHost();

        try {
            List<VolumeObjectTO> volumeToList =  relocateVirtualMachine(null, null, vmTo, targetHost, null, null, volToFiler);
            return new MigrateWithStorageAnswer(cmd, volumeToList);
        } catch (Throwable e) {
            String msg = "MigrateWithStorageCommand failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.warn(msg, e);
            return new MigrateWithStorageAnswer(cmd, (Exception)e);
        }
    }

    private Answer migrateVolume(MigrateVolumeCommand cmd) {
        Answer answer = null;
        String path = cmd.getVolumePath();

        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
        VirtualMachineMO vmMo = null;
        DatastoreMO sourceDsMo = null;
        DatastoreMO destinationDsMo = null;
        ManagedObjectReference morSourceDS = null;
        ManagedObjectReference morDestinationDS = null;
        String vmdkDataStorePath = null;
        boolean isvVolsInvolved = false;

        String vmName = null;
        try {
            // OfflineVmwareMigration: we need to refactor the worker vm creation out for use in migration methods as well as here
            // OfflineVmwareMigration: this method is 100 lines and needs refactorring anyway
            // we need to spawn a worker VM to attach the volume to and move it
            morSourceDS = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, cmd.getSourcePool().getUuid());
            sourceDsMo = new DatastoreMO(hyperHost.getContext(), morSourceDS);
            VmwareHypervisorHost hyperHostInTargetCluster = VmwareHelper.getHostMOFromHostName(getServiceContext(),
                    cmd.getHostGuidInTargetCluster());
            VmwareHypervisorHost dsHost = hyperHostInTargetCluster == null ? hyperHost : hyperHostInTargetCluster;
            String targetDsName = cmd.getTargetPool().getUuid();
            morDestinationDS = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(dsHost, targetDsName);
            if(morDestinationDS == null) {
                String msg = "Unable to find the target datastore: " + targetDsName + " on host: " + dsHost.getHyperHostName();
                s_logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            destinationDsMo = new DatastoreMO(hyperHost.getContext(), morDestinationDS);

            vmName = getWorkerName(getServiceContext(), cmd, 0, sourceDsMo);
            if (destinationDsMo.getDatastoreType().equalsIgnoreCase("VVOL")) {
                isvVolsInvolved = true;
                vmName = getWorkerName(getServiceContext(), cmd, 0, destinationDsMo);
            }

            // OfflineVmwareMigration: refactor for re-use
            // OfflineVmwareMigration: 1. find data(store)
            // OfflineVmwareMigration: more robust would be to find the store given the volume as it might have been moved out of band or due to error
            // example: DatastoreMO existingVmDsMo = new DatastoreMO(dcMo.getContext(), dcMo.findDatastore(fileInDatastore.getDatastoreName()));

            s_logger.info("Create worker VM " + vmName);
            // OfflineVmwareMigration: 2. create the worker with access to the data(store)
            vmMo = HypervisorHostHelper.createWorkerVM(hyperHost, sourceDsMo, vmName,
                    HypervisorHostHelper.getMinimumHostHardwareVersion(hyperHost, hyperHostInTargetCluster));
            if (vmMo == null) {
                // OfflineVmwareMigration: don't throw a general Exception but think of a specific one
                throw new CloudRuntimeException("Unable to create a worker VM for volume operation");
            }

            synchronized (this) {
                // OfflineVmwareMigration: 3. attach the disk to the worker
                String vmdkFileName = path + VMDK_EXTENSION;
                vmdkDataStorePath = VmwareStorageLayoutHelper.getLegacyDatastorePathFromVmdkFileName(sourceDsMo, vmdkFileName);
                if (!sourceDsMo.fileExists(vmdkDataStorePath)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(String.format("path not found (%s), trying under '%s'", vmdkFileName, path));
                    }
                    vmdkDataStorePath = VmwareStorageLayoutHelper.getVmwareDatastorePathFromVmdkFileName(sourceDsMo, path, vmdkFileName);
                }
                if (!sourceDsMo.folderExists(String.format("[%s]", sourceDsMo.getName()), path) || !sourceDsMo.fileExists(vmdkDataStorePath)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(String.format("path not found (%s), trying under '%s'", vmdkFileName, vmName));
                    }
                    vmdkDataStorePath = VmwareStorageLayoutHelper.getVmwareDatastorePathFromVmdkFileName(sourceDsMo, vmName, vmdkFileName);
                }
                if (!sourceDsMo.folderExists(String.format("[%s]", sourceDsMo.getName()), vmName) || !sourceDsMo.fileExists(vmdkDataStorePath)) {
                    vmdkDataStorePath = sourceDsMo.searchFileInSubFolders(vmdkFileName, true, null);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(String.format("attaching %s to %s for migration", vmdkDataStorePath, vmMo.getVmName()));
                }
                vmMo.attachDisk(new String[]{vmdkDataStorePath}, morSourceDS);
            }

            // OfflineVmwareMigration: 4. find the (worker-) VM
            // find VM through datacenter (VM is not at the target host yet)
            vmMo = hyperHost.findVmOnPeerHyperHost(vmName);
            if (vmMo == null) {
                String msg = "VM " + vmName + " does not exist in VMware datacenter";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            if (s_logger.isTraceEnabled()) {
                VirtualDisk[] disks = vmMo.getAllDiskDevice();
                String format = "disk %d is attached as %s";
                for (VirtualDisk disk : disks) {
                    s_logger.trace(String.format(format, disk.getKey(), vmMo.getVmdkFileBaseName(disk)));
                }
            }

            // OfflineVmwareMigration: 5. create a relocate spec and perform
            Pair<VirtualDisk, String> vdisk = vmMo.getDiskDevice(path);
            if (vdisk == null) {
                if (s_logger.isTraceEnabled())
                    s_logger.trace("migrate volume done (failed)");
                throw new CloudRuntimeException("No such disk device: " + path);
            }

            VirtualDisk disk = vdisk.first();
            String vmdkAbsFile = getAbsoluteVmdkFile(disk);
            if (vmdkAbsFile != null && !vmdkAbsFile.isEmpty()) {
                vmMo.updateAdapterTypeIfRequired(vmdkAbsFile);
            }

            // OfflineVmwareMigration: this may have to be disected and executed in separate steps
            answer = migrateAndAnswer(vmMo, cmd.getTargetPool().getUuid(), hyperHost, cmd);
        } catch (Exception e) {
            String msg = String.format("Migration of volume '%s' failed due to %s", cmd.getVolumePath(), e.getLocalizedMessage());
            s_logger.error(msg, e);
            answer = new Answer(cmd, false, msg);
        } finally {
            try {
                // OfflineVmwareMigration: worker *may* have been renamed
                vmName = vmMo.getVmName();
                s_logger.info("Dettaching disks before destroying worker VM '" + vmName + "' after volume migration");
                VirtualDisk[] disks = vmMo.getAllDiskDevice();
                String format = "disk %d was migrated to %s";
                for (VirtualDisk disk : disks) {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace(String.format(format, disk.getKey(), vmMo.getVmdkFileBaseName(disk)));
                    }
                    vmdkDataStorePath = VmwareStorageLayoutHelper.getLegacyDatastorePathFromVmdkFileName(destinationDsMo, vmMo.getVmdkFileBaseName(disk) + VMDK_EXTENSION);
                    vmMo.detachDisk(vmdkDataStorePath, false);
                }
                s_logger.info("Destroy worker VM '" + vmName + "' after volume migration");
                vmMo.destroy();
            } catch (Throwable e) {
                s_logger.info("Failed to destroy worker VM: " + vmName);
            }
        }
        if (answer instanceof MigrateVolumeAnswer) {
            if (!isvVolsInvolved) {
                String newPath = ((MigrateVolumeAnswer) answer).getVolumePath();
                String vmdkFileName = newPath + VMDK_EXTENSION;
                try {
                    VmwareStorageLayoutHelper.syncVolumeToRootFolder(destinationDsMo.getOwnerDatacenter().first(), destinationDsMo, newPath, vmName);
                    vmdkDataStorePath = VmwareStorageLayoutHelper.getLegacyDatastorePathFromVmdkFileName(destinationDsMo, vmdkFileName);

                    if (!destinationDsMo.fileExists(vmdkDataStorePath)) {
                        String msg = String.format("Migration of volume '%s' failed; file (%s) not found as path '%s'", cmd.getVolumePath(), vmdkFileName, vmdkDataStorePath);
                        s_logger.error(msg);
                        answer = new Answer(cmd, false, msg);
                    }
                } catch (Exception e) {
                    String msg = String.format("Migration of volume '%s' failed due to %s", cmd.getVolumePath(), e.getLocalizedMessage());
                    s_logger.error(msg, e);
                    answer = new Answer(cmd, false, msg);
                }
            }
        }
        return answer;
    }

    // OfflineVmwareMigration: refactor to be able to handle a detached volume
    private Answer execute(MigrateVolumeCommand cmd) {
        String volumePath = cmd.getVolumePath();
        String chainInfo = cmd.getChainInfo();
        StorageFilerTO poolTo = cmd.getPool();
        VolumeObjectTO volumeObjectTO = (VolumeObjectTO)cmd.getSrcData();

        String vmName = cmd.getAttachedVmName();

        VirtualMachineMO vmMo = null;
        VmwareHypervisorHost srcHyperHost = null;

        // OfflineVmwareMigration: ifhost is null ???
        if (StringUtils.isBlank(cmd.getAttachedVmName())) {
            return migrateVolume(cmd);
        }
        ManagedObjectReference morDs = null;
        ManagedObjectReference morDc = null;
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        List<VirtualMachineRelocateSpecDiskLocator> diskLocators = new ArrayList<VirtualMachineRelocateSpecDiskLocator>();
        VirtualMachineRelocateSpecDiskLocator diskLocator = null;

        String tgtDsName = "";

        try {
            srcHyperHost = getHyperHost(getServiceContext());
            morDc = srcHyperHost.getHyperHostDatacenter();
            tgtDsName = poolTo.getUuid();

            // find VM in this datacenter not just in this cluster.
            DatacenterMO dcMo = new DatacenterMO(getServiceContext(), morDc);
            vmMo = dcMo.findVm(vmName);

            if (vmMo == null) {
                String msg = "VM " + vmName + " does not exist in VMware datacenter " + morDc.getValue();
                s_logger.error(msg);
                throw new CloudRuntimeException(msg);
            }
            vmName = vmMo.getName();
            morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(srcHyperHost, tgtDsName);
            if (morDs == null) {
                String msg = "Unable to find the mounted datastore with name: " + tgtDsName + " on source host: " + srcHyperHost.getHyperHostName()
                        + " to execute MigrateVolumeCommand";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            DatastoreMO targetDsMo = new DatastoreMO(srcHyperHost.getContext(), morDs);
            if (cmd.getContextParam(DiskTO.PROTOCOL_TYPE) != null && cmd.getContextParam(DiskTO.PROTOCOL_TYPE).equalsIgnoreCase("DatastoreCluster")) {
                VmwareContext context = getServiceContext();
                VmwareHypervisorHost hyperHost = getHyperHost(context);
                VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
                VirtualMachineDiskInfo matchingExistingDisk = getMatchingExistingDiskWithVolumeDetails(diskInfoBuilder, volumePath, chainInfo, false, null, poolTo.getUuid(), hyperHost, context);
                if (diskInfoBuilder != null && matchingExistingDisk != null) {
                    String[] diskChain = matchingExistingDisk.getDiskChain();
                    DatastoreFile file = new DatastoreFile(diskChain[0]);
                    if (!file.getFileBaseName().equalsIgnoreCase(volumePath)) {
                        if (s_logger.isInfoEnabled())
                            s_logger.info("Detected disk-chain top file change on volume: " + volumePath + " -> " + file.getFileBaseName());
                        volumePath = file.getFileBaseName();
                    }
                }
            }

            String fullVolumePath = VmwareStorageLayoutHelper.getVmwareDatastorePathFromVmdkFileName(targetDsMo, vmName, volumePath + VMDK_EXTENSION);
            Pair<VirtualDisk, String> diskInfo = getVirtualDiskInfo(vmMo, appendFileType(volumePath, VMDK_EXTENSION));
            String vmdkAbsFile = getAbsoluteVmdkFile(diskInfo.first());
            if (vmdkAbsFile != null && !vmdkAbsFile.isEmpty()) {
                vmMo.updateAdapterTypeIfRequired(vmdkAbsFile);
            }
            int diskId = diskInfo.first().getKey();

            diskLocator = new VirtualMachineRelocateSpecDiskLocator();
            diskLocator.setDatastore(morDs);
            diskLocator.setDiskId(diskId);
            diskLocators.add(diskLocator);
            if (cmd.getVolumeType() == Volume.Type.ROOT) {
                relocateSpec.setDatastore(morDs);
                // If a target datastore is provided for the VM, then by default all volumes associated with the VM will be migrated to that target datastore.
                // Hence set the existing datastore as target datastore for volumes that are not to be migrated.
                List<Pair<Integer, ManagedObjectReference>> diskDatastores = vmMo.getAllDiskDatastores();
                for (Pair<Integer, ManagedObjectReference> diskDatastore : diskDatastores) {
                    if (diskDatastore.first().intValue() != diskId) {
                        diskLocator = new VirtualMachineRelocateSpecDiskLocator();
                        diskLocator.setDiskId(diskDatastore.first().intValue());
                        diskLocator.setDatastore(diskDatastore.second());
                        diskLocators.add(diskLocator);
                    }
                }
            }

            relocateSpec.getDisk().addAll(diskLocators);

            // Change datastore
            if (!vmMo.changeDatastore(relocateSpec)) {
                throw new Exception("Change datastore operation failed during volume migration");
            } else {
                s_logger.debug("Successfully migrated volume " + volumePath + " to target datastore " + tgtDsName);
            }

            // Consolidate VM disks.
            // In case of a linked clone VM, if VM's disks are not consolidated,
            // further volume operations on the ROOT volume such as volume snapshot etc. will result in DB inconsistencies.
            if (!vmMo.consolidateVmDisks()) {
                s_logger.warn("VM disk consolidation failed after storage migration.");
            } else {
                s_logger.debug("Successfully consolidated disks of VM " + vmName + ".");
            }

            // Update and return volume path and chain info because that could have changed after migration
            if (!targetDsMo.fileExists(fullVolumePath)) {
                VirtualDisk[] disks = vmMo.getAllDiskDevice();
                for (VirtualDisk disk : disks)
                    if (disk.getKey() == diskId) {
                        volumePath = vmMo.getVmdkFileBaseName(disk);
                    }
            }
            VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
            chainInfo = _gson.toJson(diskInfoBuilder.getDiskInfoByBackingFileBaseName(volumePath, targetDsMo.getName()));
            MigrateVolumeAnswer answer = new MigrateVolumeAnswer(cmd, true, null, volumePath);
            answer.setVolumeChainInfo(chainInfo);
            return answer;
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
            s_logger.error(msg, e);
            return new MigrateVolumeAnswer(cmd, false, msg, null);
        }
    }

    private Pair<VirtualDisk, String> getVirtualDiskInfo(VirtualMachineMO vmMo, String srcDiskName) throws Exception {
        Pair<VirtualDisk, String> deviceInfo = vmMo.getDiskDevice(srcDiskName);
        if (deviceInfo == null) {
            throw new Exception("No such disk device: " + srcDiskName);
        }
        return deviceInfo;
    }

    private VmwareHypervisorHost getTargetHyperHost(DatacenterMO dcMo, String destIp) throws Exception {

        VmwareManager mgr = dcMo.getContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

        List<ObjectContent> ocs = dcMo.getHostPropertiesOnDatacenterHostFolder(new String[]{"name", "parent"});
        if (ocs != null && ocs.size() > 0) {
            for (ObjectContent oc : ocs) {
                HostMO hostMo = new HostMO(dcMo.getContext(), oc.getObj());
                VmwareHypervisorHostNetworkSummary netSummary = hostMo.getHyperHostNetworkSummary(mgr.getManagementPortGroupByHost(hostMo));
                if (destIp.equalsIgnoreCase(netSummary.getHostIp())) {
                    return new HostMO(dcMo.getContext(), oc.getObj());
                }
            }
        }

        throw new Exception("Unable to locate dest host by " + destIp);
    }

    protected Answer execute(CreateStoragePoolCommand cmd) {
        if (cmd.getCreateDatastore()) {
            try {
                VmwareContext context = getServiceContext();

                _storageProcessor.prepareManagedDatastore(context, getHyperHost(context), cmd.getDetails().get(CreateStoragePoolCommand.DATASTORE_NAME),
                        cmd.getDetails().get(CreateStoragePoolCommand.IQN), cmd.getDetails().get(CreateStoragePoolCommand.STORAGE_HOST),
                        Integer.parseInt(cmd.getDetails().get(CreateStoragePoolCommand.STORAGE_PORT)));
            } catch (Exception ex) {
                return new Answer(cmd, false, "Issue creating datastore");
            }
        }

        return new Answer(cmd, true, "success");
    }

    protected Answer execute(ModifyTargetsCommand cmd) {
        VmwareContext context = getServiceContext(cmd);
        VmwareHypervisorHost hyperHost = getHyperHost(context);

        List<HostMO> hostMOs = new ArrayList<>();

        if (cmd.getApplyToAllHostsInCluster()) {
            try {
                ManagedObjectReference morCluster = hyperHost.getHyperHostCluster();
                ClusterMO clusterMO = new ClusterMO(context, morCluster);

                List<Pair<ManagedObjectReference, String>> hosts = clusterMO.getClusterHosts();

                for (Pair<ManagedObjectReference, String> host : hosts) {
                    HostMO hostMO = new HostMO(context, host.first());

                    hostMOs.add(hostMO);
                }
            } catch (Exception ex) {
                s_logger.error(ex.getMessage(), ex);

                throw new CloudRuntimeException(ex.getMessage(), ex);
            }
        } else {
            hostMOs.add((HostMO) hyperHost);
        }

        handleTargets(cmd.getAdd(), cmd.getTargetTypeToRemove(), cmd.isRemoveAsync(), cmd.getTargets(), hostMOs);

        return new ModifyTargetsAnswer();
    }

    protected Answer execute(ModifyStoragePoolCommand cmd) {
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            StorageFilerTO pool = cmd.getPool();

            if (pool.getType() != StoragePoolType.NetworkFilesystem && pool.getType() != StoragePoolType.VMFS && pool.getType() != StoragePoolType.PreSetup && pool.getType() != StoragePoolType.DatastoreCluster) {
                throw new Exception("Unsupported storage pool type " + pool.getType());
            }

            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, pool.getUuid());

            if (morDatastore == null) {
                morDatastore = hyperHost.mountDatastore((pool.getType() == StoragePoolType.VMFS || pool.getType() == StoragePoolType.PreSetup || pool.getType() == StoragePoolType.DatastoreCluster), pool.getHost(), pool.getPort(), pool.getPath(), pool.getUuid().replace("-", ""), true);
            }

            assert (morDatastore != null);

            DatastoreMO dsMo = new DatastoreMO(getServiceContext(), morDatastore);
            HypervisorHostHelper.createBaseFolder(dsMo, hyperHost, pool.getType());

            long capacity = 0;
            long available = 0;
            List<ModifyStoragePoolAnswer> childDatastoresModifyStoragePoolAnswers = new ArrayList<>();
            if (pool.getType() == StoragePoolType.DatastoreCluster) {
                StoragepodMO datastoreClusterMo = new StoragepodMO(getServiceContext(), morDatastore);
                StoragePodSummary dsClusterSummary = datastoreClusterMo.getDatastoreClusterSummary();
                capacity = dsClusterSummary.getCapacity();
                available = dsClusterSummary.getFreeSpace();

                List<ManagedObjectReference> childDatastoreMors = datastoreClusterMo.getDatastoresInDatastoreCluster();
                for (ManagedObjectReference childDsMor : childDatastoreMors) {
                    DatastoreMO childDsMo = new DatastoreMO(getServiceContext(), childDsMor);

                    Map<String, TemplateProp> tInfo = new HashMap<>();
                    DatastoreSummary summary = childDsMo.getDatastoreSummary();;
                    ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, summary.getCapacity(), summary.getFreeSpace(), tInfo);
                    StoragePoolInfo poolInfo = answer.getPoolInfo();
                    poolInfo.setName(summary.getName());
                    String datastoreClusterPath = pool.getPath();
                    int pathstartPosition = datastoreClusterPath.lastIndexOf('/');
                    String datacenterName = datastoreClusterPath.substring(0, pathstartPosition+1);
                    String childPath = datacenterName + summary.getName();
                    poolInfo.setHostPath(childPath);
                    String uuid = childDsMo.getCustomFieldValue(CustomFieldConstants.CLOUD_UUID);
                    if (uuid == null) {
                        uuid = UUID.nameUUIDFromBytes(((pool.getHost() + childPath)).getBytes()).toString();
                    }
                    poolInfo.setUuid(uuid);
                    poolInfo.setLocalPath(cmd.LOCAL_PATH_PREFIX + File.separator + uuid);
                    answer.setPoolInfo(poolInfo);
                    answer.setPoolType(summary.getType());
                    answer.setLocalDatastoreName(morDatastore.getValue());

                    childDsMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, uuid);
                    HypervisorHostHelper.createBaseFolderInDatastore(childDsMo, hyperHost.getHyperHostDatacenter());

                    childDatastoresModifyStoragePoolAnswers.add(answer);
                }
            } else {
                HypervisorHostHelper.createBaseFolderInDatastore(dsMo, hyperHost.getHyperHostDatacenter());

                DatastoreSummary summary = dsMo.getDatastoreSummary();
                capacity = summary.getCapacity();
                available = summary.getFreeSpace();
            }

            Map<String, TemplateProp> tInfo = new HashMap<>();
            ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, capacity, available, tInfo);
            answer.setDatastoreClusterChildren(childDatastoresModifyStoragePoolAnswers);

            if (cmd.getAdd() && (pool.getType() == StoragePoolType.VMFS || pool.getType() == StoragePoolType.PreSetup) && pool.getType() != StoragePoolType.DatastoreCluster) {
                answer.setPoolType(dsMo.getDatastoreType());
                answer.setLocalDatastoreName(morDatastore.getValue());
            }

            return answer;
        } catch (Throwable e) {
            return new Answer(cmd, false, createLogMessageException(e, cmd));
        }
    }

    protected Answer execute(GetStoragePoolCapabilitiesCommand cmd) {
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());

            HostMO host = (HostMO) hyperHost;

            StorageFilerTO pool = cmd.getPool();

            ManagedObjectReference morDatastore = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, pool.getUuid());

            if (morDatastore == null) {
                morDatastore = hyperHost.mountDatastore((pool.getType() == StoragePoolType.VMFS || pool.getType() == StoragePoolType.PreSetup || pool.getType() == StoragePoolType.DatastoreCluster), pool.getHost(), pool.getPort(), pool.getPath(), pool.getUuid().replace("-", ""), true);
            }

            assert (morDatastore != null);

            DatastoreMO dsMo = new DatastoreMO(getServiceContext(), morDatastore);

            GetStoragePoolCapabilitiesAnswer answer = new GetStoragePoolCapabilitiesAnswer(cmd);

            boolean hardwareAccelerationSupportForDataStore = getHardwareAccelerationSupportForDataStore(host.getMor(), dsMo.getName());
            Map<String, String> poolDetails = answer.getPoolDetails();
            poolDetails.put(Storage.Capability.HARDWARE_ACCELERATION.toString(), String.valueOf(hardwareAccelerationSupportForDataStore));
            answer.setPoolDetails(poolDetails);
            answer.setResult(true);

            return answer;
        } catch (Throwable e) {
            GetStoragePoolCapabilitiesAnswer answer = new GetStoragePoolCapabilitiesAnswer(cmd);
            answer.setResult(false);
            answer.setDetails(createLogMessageException(e, cmd));
            return answer;
        }
    }

    private boolean getHardwareAccelerationSupportForDataStore(ManagedObjectReference host, String dataStoreName) throws Exception {
        HostConfigInfo config = getServiceContext().getVimClient().getDynamicProperty(host, "config");
        List<HostFileSystemMountInfo> mountInfoList = config.getFileSystemVolume().getMountInfo();
        for (HostFileSystemMountInfo hostFileSystemMountInfo: mountInfoList) {
            if ( hostFileSystemMountInfo.getVolume().getName().equals(dataStoreName) ) {
                return hostFileSystemMountInfo.getVStorageSupport().equals("vStorageSupported");
            }
        }
        return false;
    }

    private void handleTargets(boolean add, ModifyTargetsCommand.TargetTypeToRemove targetTypeToRemove, boolean isRemoveAsync,
                               List<Map<String, String>> targets, List<HostMO> hosts) {
        if (targets != null && targets.size() > 0) {
            try {
                _storageProcessor.handleTargets(add, targetTypeToRemove, isRemoveAsync, targets, hosts);
            } catch (Exception ex) {
                s_logger.warn(ex.getMessage());
            }
        }
    }

    protected Answer execute(DeleteStoragePoolCommand cmd) {
        try {
            if (cmd.getRemoveDatastore()) {
                _storageProcessor.handleDatastoreAndVmdkDetach(cmd, cmd.getDetails().get(DeleteStoragePoolCommand.DATASTORE_NAME),
                        cmd.getDetails().get(DeleteStoragePoolCommand.IQN), cmd.getDetails().get(DeleteStoragePoolCommand.STORAGE_HOST),
                        Integer.parseInt(cmd.getDetails().get(DeleteStoragePoolCommand.STORAGE_PORT)));

                return new Answer(cmd, true, "success");
            } else {
                // We will leave datastore cleanup management to vCenter. Since for cluster VMFS datastore, it will always
                // be mounted by vCenter.

                // VmwareHypervisorHost hyperHost = this.getHyperHost(getServiceContext());
                // hyperHost.unmountDatastore(pool.getUuid());

                return new Answer(cmd, true, "success");
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");

                invalidateServiceContext();
            }

            StorageFilerTO pool = cmd.getPool();
            String msg = String.format("DeleteStoragePoolCommand (pool: [%s], path: [%s]) failed due to [%s].", pool.getHost(), pool.getPath(), VmwareHelper.getExceptionMessage(e));
            s_logger.error(msg, e);

            return new Answer(cmd, false, msg);
        }
    }

    public static String getDatastoreName(String str) {
        return str.replace('/', '-');
    }

    public static String createDatastoreNameFromIqn(String iqn) {
        return "-" + iqn + "-0";
    }

    protected AttachIsoAnswer execute(AttachIsoCommand cmd) {
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
            VirtualMachineMO vmMo = HypervisorHostHelper.findVmOnHypervisorHostOrPeer(hyperHost, cmd.getVmName());
            if (vmMo == null) {
                String msg = "Unable to find VM in vSphere to execute AttachIsoCommand, vmName: " + cmd.getVmName();
                s_logger.error(msg);
                throw new Exception(msg);
            }

            String storeUrl = cmd.getStoreUrl();
            if (storeUrl == null) {
                if (!cmd.getIsoPath().equalsIgnoreCase(TemplateManager.VMWARE_TOOLS_ISO)) {
                    String msg = "ISO store root url is not found in AttachIsoCommand";
                    s_logger.error(msg);
                    throw new Exception(msg);
                } else {
                    if (cmd.isAttach()) {
                        vmMo.mountToolsInstaller();
                    } else {
                        try {
                            if (!vmMo.unmountToolsInstaller()) {
                                return new AttachIsoAnswer(cmd, false,
                                        "Failed to unmount vmware-tools installer ISO as the corresponding CDROM device is locked by VM. Please unmount the CDROM device inside the VM and ret-try.");
                            }
                        } catch (Throwable e) {
                            vmMo.detachIso(null, cmd.isForce());
                        }
                    }

                    return new AttachIsoAnswer(cmd);
                }
            }

            ManagedObjectReference morSecondaryDs = prepareSecondaryDatastoreOnHost(storeUrl);
            String isoPath = cmd.getIsoPath();
            if (!isoPath.startsWith(storeUrl)) {
                assert (false);
                String msg = "ISO path does not start with the secondary storage root";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            int isoNameStartPos = isoPath.lastIndexOf('/');
            String isoFileName = isoPath.substring(isoNameStartPos + 1);
            String isoStorePathFromRoot = isoPath.substring(storeUrl.length() + 1, isoNameStartPos + 1);


            // TODO, check if iso is already attached, or if there is a previous
            // attachment
            DatastoreMO secondaryDsMo = new DatastoreMO(getServiceContext(), morSecondaryDs);
            String storeName = secondaryDsMo.getName();
            String isoDatastorePath = String.format("[%s] %s%s", storeName, isoStorePathFromRoot, isoFileName);

            if (cmd.isAttach()) {
                vmMo.attachIso(isoDatastorePath, morSecondaryDs, true, false, cmd.getDeviceKey(), cmd.isForce());
                return new AttachIsoAnswer(cmd);
            } else {
                int key = vmMo.detachIso(isoDatastorePath, cmd.isForce());
                return new AttachIsoAnswer(cmd, key);
            }

        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                invalidateServiceContext();
            }

            String message = String.format("AttachIsoCommand(%s) failed due to [%s].", cmd.isAttach()? "attach" : "detach", VmwareHelper.getExceptionMessage(e));
            s_logger.error(message, e);
            return new AttachIsoAnswer(cmd, false, message);
        }
    }

    public synchronized ManagedObjectReference prepareSecondaryDatastoreOnHost(String storeUrl) throws Exception {
        String storeName = getSecondaryDatastoreUUID(storeUrl);
        URI uri = new URI(storeUrl);

        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
        ManagedObjectReference morDatastore = hyperHost.mountDatastore(false, uri.getHost(), 0, uri.getPath(), storeName.replace("-", ""), false);

        if (morDatastore == null)
            throw new Exception("Unable to mount secondary storage on host. storeUrl: " + storeUrl);

        return morDatastore;
    }

    public synchronized ManagedObjectReference prepareSecondaryDatastoreOnSpecificHost(String storeUrl, VmwareHypervisorHost hyperHost) throws Exception {
        String storeName = getSecondaryDatastoreUUID(storeUrl);
        URI uri = new URI(storeUrl);

        ManagedObjectReference morDatastore = hyperHost.mountDatastore(false, uri.getHost(), 0, uri.getPath(), storeName.replace("-", ""), false);

        if (morDatastore == null)
            throw new Exception("Unable to mount secondary storage on host. storeUrl: " + storeUrl);

        return morDatastore;
    }

    private static String getSecondaryDatastoreUUID(String storeUrl) {
        String uuid = null;
        try {
            uuid = UUID.nameUUIDFromBytes(storeUrl.getBytes("UTF-8")).toString();
        } catch (UnsupportedEncodingException e) {
            s_logger.warn("Failed to create UUID from string " + storeUrl + ". Bad storeUrl or UTF-8 encoding error.");
        }
        return uuid;
    }

    protected Answer execute(ValidateSnapshotCommand cmd) {
        // the command is no longer available
        String expectedSnapshotBackupUuid = null;
        String actualSnapshotBackupUuid = null;
        String actualSnapshotUuid = null;
        return new ValidateSnapshotAnswer(cmd, false, "ValidateSnapshotCommand is not supported for vmware yet", expectedSnapshotBackupUuid, actualSnapshotBackupUuid,
                actualSnapshotUuid);
    }

    protected Answer execute(ManageSnapshotCommand cmd) {
        long snapshotId = cmd.getSnapshotId();

        /*
         * "ManageSnapshotCommand",
         * "{\"_commandSwitch\":\"-c\",\"_volumePath\":\"i-2-3-KY-ROOT\",\"_snapshotName\":\"i-2-3-KY_i-2-3-KY-ROOT_20101102203827\",\"_snapshotId\":1,\"_vmName\":\"i-2-3-KY\"}"
         */
        boolean success = false;
        String cmdSwitch = cmd.getCommandSwitch();
        String snapshotOp = "Unsupported snapshot command." + cmdSwitch;
        if (cmdSwitch.equals(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
            snapshotOp = "create";
        } else if (cmdSwitch.equals(ManageSnapshotCommand.DESTROY_SNAPSHOT)) {
            snapshotOp = "destroy";
        }

        String details = "ManageSnapshotCommand operation: " + snapshotOp + " Failed for snapshotId: " + snapshotId;
        String snapshotUUID = null;

        // snapshot operation (create or destroy) is handled inside BackupSnapshotCommand(), we just fake
        // a success return here
        snapshotUUID = UUID.randomUUID().toString();
        success = true;
        details = null;

        return new ManageSnapshotAnswer(cmd, snapshotId, snapshotUUID, success, details);
    }

    protected Answer execute(BackupSnapshotCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            return mgr.getStorageManager().execute(this, cmd);
        } catch (Throwable e) {
            return new BackupSnapshotAnswer(cmd, false, createLogMessageException(e, cmd), null, true);
        }
    }

    protected Answer execute(CreateVMSnapshotCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            return mgr.getStorageManager().execute(this, cmd);
        } catch (Exception e) {
            createLogMessageException(e, cmd);
            return new CreateVMSnapshotAnswer(cmd, false, "");
        }
    }

    protected Answer execute(DeleteVMSnapshotCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            return mgr.getStorageManager().execute(this, cmd);
        } catch (Exception e) {
            createLogMessageException(e, cmd);
            return new DeleteVMSnapshotAnswer(cmd, false, "");
        }
    }

    protected Answer execute(RevertToVMSnapshotCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
            return mgr.getStorageManager().execute(this, cmd);
        } catch (Exception e) {
            createLogMessageException(e, cmd);
            return new RevertToVMSnapshotAnswer(cmd, false, "");
        }
    }

    protected Answer execute(CreateVolumeFromSnapshotCommand cmd) {
        String details = null;
        boolean success = false;
        String newVolumeName = UUID.randomUUID().toString();

        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
            return mgr.getStorageManager().execute(this, cmd);
        } catch (Throwable e) {
            details = createLogMessageException(e, cmd);
        }

        return new CreateVolumeFromSnapshotAnswer(cmd, success, details, newVolumeName);
    }

    protected Answer execute(CreatePrivateTemplateFromVolumeCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            return mgr.getStorageManager().execute(this, cmd);

        } catch (Throwable e) {
            return new CreatePrivateTemplateAnswer(cmd, false, createLogMessageException(e, cmd));
        }
    }

    protected Answer execute(final UpgradeSnapshotCommand cmd) {
        return new Answer(cmd, true, "success");
    }

    protected Answer execute(CreatePrivateTemplateFromSnapshotCommand cmd) {
        try {
            VmwareManager mgr = getServiceContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
            return mgr.getStorageManager().execute(this, cmd);

        } catch (Throwable e) {
            return new CreatePrivateTemplateAnswer(cmd, false, createLogMessageException(e, cmd));
        }
    }

    protected Answer execute(GetStorageStatsCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(hyperHost, cmd.getStorageId());

            if (morDs != null) {
                long capacity = 0;
                long free = 0;
                if (cmd.getPooltype() == StoragePoolType.DatastoreCluster) {
                    StoragepodMO datastoreClusterMo = new StoragepodMO(getServiceContext(), morDs);
                    StoragePodSummary summary = datastoreClusterMo.getDatastoreClusterSummary();
                    capacity = summary.getCapacity();
                    free = summary.getFreeSpace();
                } else {
                    DatastoreMO datastoreMo = new DatastoreMO(context, morDs);
                    DatastoreSummary summary = datastoreMo.getDatastoreSummary();
                    capacity = summary.getCapacity();
                    free = summary.getFreeSpace();
                }

                long used = capacity - free;

                s_logger.debug(String.format("Datastore summary info: [storageId: %s, ], localPath: %s, poolType: %s, capacity: %s, free: %s, used: %s].", cmd.getStorageId(),
                        cmd.getLocalPath(), cmd.getPooltype(), toHumanReadableSize(capacity), toHumanReadableSize(free), toHumanReadableSize(used)));

                if (capacity <= 0) {
                    s_logger.warn("Something is wrong with vSphere NFS datastore, rebooting ESX(ESXi) host should help");
                }

                return new GetStorageStatsAnswer(cmd, capacity, used);
            } else {
                String msg = String.format("Could not find datastore for GetStorageStatsCommand: [storageId: %s, localPath: %s, poolType: %s].",
                        cmd.getStorageId(), cmd.getLocalPath(), cmd.getPooltype());

                s_logger.error(msg);
                return new GetStorageStatsAnswer(cmd, msg);
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                invalidateServiceContext();
            }

            String msg = String.format("Unable to execute GetStorageStatsCommand(storageId : [%s], localPath: [%s], poolType: [%s]) due to [%s]", cmd.getStorageId(), cmd.getLocalPath(), cmd.getPooltype(), VmwareHelper.getExceptionMessage(e));
            s_logger.error(msg, e);
            return new GetStorageStatsAnswer(cmd, msg);
        }
    }

    protected Answer execute(GetVncPortCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            assert (hyperHost instanceof HostMO);
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(cmd.getName());
            if (vmMo == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find the owner VM for GetVncPortCommand on host " + hyperHost.getHyperHostName() + ", try within datacenter");
                }

                vmMo = hyperHost.findVmOnPeerHyperHost(cmd.getName());

                if (vmMo == null) {
                    throw new Exception("Unable to find VM in vSphere, vm: " + cmd.getName());
                }
            }

            Pair<String, Integer> portInfo = vmMo.getVncPort(mgr.getManagementPortGroupByHost((HostMO) hyperHost));

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Found vnc port info. vm: " + cmd.getName() + " host: " + portInfo.first() + ", vnc port: " + portInfo.second());
            }
            return new GetVncPortAnswer(cmd, portInfo.first(), portInfo.second());
        } catch (Throwable e) {
            return new GetVncPortAnswer(cmd, createLogMessageException(e, cmd));
        }
    }

    protected Answer execute(SetupCommand cmd) {
        return new SetupAnswer(cmd, false);
    }

    protected Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd, "Put host in maintaince");
    }

    protected Answer execute(PingTestCommand cmd) {
        String controlIp = cmd.getRouterIp();
        if (controlIp != null) {
            String args = " -c 1 -n -q " + cmd.getPrivateIp();
            try {
                Pair<Boolean, String> result = SshHelper.sshExecute(controlIp, DefaultDomRSshPort, "root", getSystemVmKeyFile(), null, "/bin/ping" + args);
                if (result.first())
                    return new Answer(cmd);
            } catch (Exception e) {
                s_logger.error("Unable to execute ping command on DomR (" + controlIp + "), domR may not be ready yet. failure due to " + VmwareHelper.getExceptionMessage(e), e);
            }
            return new Answer(cmd, false, "PingTestCommand failed");
        } else {
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost hyperHost = getHyperHost(context);

            try {
                HostMO hostMo = (HostMO) hyperHost;
                ClusterMO clusterMo = new ClusterMO(context, hostMo.getHyperHostCluster());
                VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

                List<Pair<ManagedObjectReference, String>> hosts = clusterMo.getClusterHosts();
                for (Pair<ManagedObjectReference, String> entry : hosts) {
                    HostMO hostInCluster = new HostMO(context, entry.first());
                    String hostIp = hostInCluster.getHostManagementIp(mgr.getManagementPortGroupName());
                    if (hostIp != null && hostIp.equals(cmd.getComputingHostIp())) {
                        if (hostInCluster.isHyperHostConnected())
                            return new Answer(cmd);
                        else
                            return new Answer(cmd, false, "PingTestCommand failed");
                    }
                }
            } catch (Exception e) {
                s_logger.error("Unable to execute ping command on host (" + cmd.getComputingHostIp() + "). failure due to " + VmwareHelper.getExceptionMessage(e), e);
            }

            return new Answer(cmd, false, "PingTestCommand failed");
        }
    }

    protected Answer execute(CheckOnHostCommand cmd) {
        return new CheckOnHostAnswer(cmd, null, "Not Implmeneted");
    }

    protected Answer execute(ModifySshKeysCommand cmd) {
        s_logger.debug(String.format("Executing resource command %s.", cmd.getClass().getSimpleName()));
        return new Answer(cmd);
    }

    protected Answer execute(GetVmIpAddressCommand cmd) {
        String details = "Unable to find IP Address of VM. ";
        String vmName = cmd.getVmName();
        boolean result = false;
        String ip = null;
        Answer answer = null;

        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);

        if (vmName == null || vmName.isEmpty()) {
            details += "Name of instance provided is NULL or empty.";
            return new Answer(cmd, result, details);
        }

        try {
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo != null) {
                GuestInfo guestInfo = vmMo.getGuestInfo();
                VirtualMachineToolsStatus toolsStatus = guestInfo.getToolsStatus();
                if (toolsStatus == VirtualMachineToolsStatus.TOOLS_NOT_INSTALLED) {
                    details += "Vmware tools not installed.";
                } else {
                    ip = guestInfo.getIpAddress();
                    if (ip != null) {
                        result = true;
                    }
                    details = ip;
                }
            } else {
                details += "VM " + vmName + " no longer exists on vSphere host: " + hyperHost.getHyperHostName();
                s_logger.info(details);
            }
        } catch (Throwable e) {
            createLogMessageException(e, cmd);
            details = String.format("%s. Encountered exception: [%s].", details,  VmwareHelper.getExceptionMessage(e));
            s_logger.error(details);
        }

        answer = new Answer(cmd, result, details);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Returning GetVmIpAddressAnswer: " + _gson.toJson(answer));
        }
        return answer;
    }

    @Override
    public PrimaryStorageDownloadAnswer execute(PrimaryStorageDownloadCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
            return (PrimaryStorageDownloadAnswer) mgr.getStorageManager().execute(this, cmd);
        } catch (Throwable e) {
            return new PrimaryStorageDownloadAnswer(createLogMessageException(e, cmd));
        }
    }

    protected Answer execute(PvlanSetupCommand cmd) {
        // Pvlan related operations are performed in the start/stop command paths
        // for vmware. This function is implemented to support mgmt layer code
        // that issue this command. Note that pvlan operations are supported only
        // in Distributed Virtual Switch environments for vmware deployments.
        return new Answer(cmd, true, "success");
    }

    protected Answer execute(UnregisterVMCommand cmd) {
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        try {
            DatacenterMO dataCenterMo = new DatacenterMO(getServiceContext(), hyperHost.getHyperHostDatacenter());
            VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
            if (vmMo != null) {
                try {
                    VirtualMachineFileLayoutEx vmFileLayout = vmMo.getFileLayout();
                    context.getService().unregisterVM(vmMo.getMor());
                    if (cmd.getCleanupVmFiles()) {
                        deleteUnregisteredVmFiles(vmFileLayout, dataCenterMo, false, null);
                    }
                    return new Answer(cmd, true, "unregister succeeded");
                } catch (Exception e) {
                    s_logger.warn("We are not able to unregister VM " + VmwareHelper.getExceptionMessage(e));
                }

                String msg = "Expunge failed in vSphere. vm: " + cmd.getVmName();
                s_logger.warn(msg);
                return new Answer(cmd, false, msg);
            } else {
                String msg = "Unable to find the VM in vSphere to unregister, assume it is already removed. VM: " + cmd.getVmName();
                s_logger.warn(msg);
                return new Answer(cmd, true, msg);
            }
        } catch (Exception e) {
            return new Answer(cmd, false, createLogMessageException(e, cmd));
        }
    }

    /**
     * UnregisterNicCommand is used to remove a portgroup created for this
     * specific nic. The portgroup will have the name set to the UUID of the
     * nic. Introduced to cleanup the portgroups created for each nic that is
     * plugged into an lswitch (Nicira NVP plugin)
     *
     * @param cmd
     * @return
     */
    protected Answer execute(UnregisterNicCommand cmd) {
        if (_guestTrafficInfo == null) {
            return new Answer(cmd, false, "No Guest Traffic Info found, unable to determine where to clean up");
        }

        try {
            if (_guestTrafficInfo.getVirtualSwitchType() != VirtualSwitchType.StandardVirtualSwitch) {
                // For now we only need to cleanup the nvp specific portgroups
                // on the standard switches
                return new Answer(cmd, true, "Nothing to do");
            }

            s_logger.debug("Cleaning up portgroup " + cmd.getNicUuid() + " on switch " + _guestTrafficInfo.getVirtualSwitchName());
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost host = getHyperHost(context);
            ManagedObjectReference clusterMO = host.getHyperHostCluster();

            // Get a list of all the hosts in this cluster
            @SuppressWarnings("unchecked")
            List<ManagedObjectReference> hosts = (List<ManagedObjectReference>) context.getVimClient().getDynamicProperty(clusterMO, "host");
            if (hosts == null) {
                return new Answer(cmd, false, "No hosts in cluster, which is pretty weird");
            }

            for (ManagedObjectReference hostMOR : hosts) {
                HostMO hostMo = new HostMO(context, hostMOR);
                hostMo.deletePortGroup(cmd.getNicUuid().toString());
                s_logger.debug("Removed portgroup " + cmd.getNicUuid() + " from host " + hostMo.getHostName());
            }
            return new Answer(cmd, true, "Unregistered resources for NIC " + cmd.getNicUuid());
        } catch (Exception e) {
            return new Answer(cmd, false, createLogMessageException(e, cmd));
        }
    }

    public void cleanupNetwork(DatacenterMO dcMO, NetworkDetails netDetails) {
        if (!VmwareManager.s_vmwareCleanupPortGroups.value()){
            return;
        }

        try {
            synchronized(this) {
                if (!areVMsOnNetwork(dcMO, netDetails)) {
                    cleanupPortGroup(dcMO, netDetails.getName());
                }
            }
        } catch(Throwable e) {
            s_logger.warn("Unable to cleanup network due to exception: " + e.getMessage(), e);
        }
    }

    private void cleanupPortGroup(DatacenterMO dcMO, String portGroupName) throws Exception {
        if (StringUtils.isBlank(portGroupName)) {
            s_logger.debug("Unspecified network port group, couldn't cleanup");
            return;
        }

        List<HostMO> hosts = dcMO.getAllHostsOnDatacenter();
        if (!CollectionUtils.isEmpty(hosts)) {
            for (HostMO host : hosts) {
                host.deletePortGroup(portGroupName);
            }
        }
    }

    private boolean areVMsOnNetwork(DatacenterMO dcMO, NetworkDetails netDetails) throws Exception {
        if (netDetails == null || netDetails.getName() == null) {
            throw new CloudRuntimeException("Unspecified network details / port group, couldn't check VMs on network port group");
        }

        List<HostMO> hosts = dcMO.getAllHostsOnDatacenter();
        if (!CollectionUtils.isEmpty(hosts)) {
            for (HostMO host : hosts) {
                NetworkMO networkMo = new NetworkMO(host.getContext(), netDetails.getNetworkMor());
                List<ManagedObjectReference> vms = networkMo.getVMsOnNetwork();
                if (!CollectionUtils.isEmpty(vms)) {
                    s_logger.debug("Network port group: " + netDetails.getName() + " is in use");
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public CopyVolumeAnswer execute(CopyVolumeCommand cmd) {
        try {
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
            return (CopyVolumeAnswer) mgr.getStorageManager().execute(this, cmd);
        } catch (Throwable e) {
            return new CopyVolumeAnswer(cmd, false, createLogMessageException(e, cmd), null, null);
        }
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            gcAndKillHungWorkerVMs();
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            try {
                if (!hyperHost.isHyperHostConnected()) {
                    return null;
                }
            } catch (Exception e) {
                s_logger.error("Unexpected exception", e);
                return null;
            }
            return new PingRoutingCommand(getType(), id, syncHostVmStates());
        } finally {
            recycleServiceContext();
        }
    }

    private void gcAndKillHungWorkerVMs() {
        try {
            // take the chance to do left-over dummy VM cleanup from previous run
            VmwareContext context = getServiceContext();
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            VmwareManager mgr = hyperHost.getContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            if (hyperHost.isHyperHostConnected()) {
                mgr.gcLeftOverVMs(context);

                s_logger.info("Scan hung worker VM to recycle");

                int workerKey = ((HostMO) hyperHost).getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_WORKER);
                int workerTagKey = ((HostMO) hyperHost).getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_WORKER_TAG);
                String workerPropName = String.format("value[%d]", workerKey);
                String workerTagPropName = String.format("value[%d]", workerTagKey);

                // GC worker that has been running for too long
                ObjectContent[] ocs = hyperHost.getVmPropertiesOnHyperHost(new String[]{"name", "config.template", workerPropName, workerTagPropName,});
                if (ocs != null) {
                    for (ObjectContent oc : ocs) {
                        List<DynamicProperty> props = oc.getPropSet();
                        if (props != null) {
                            boolean template = false;
                            boolean isWorker = false;
                            String workerTag = null;

                            for (DynamicProperty prop : props) {
                                if (prop.getName().equals("config.template")) {
                                    template = (Boolean) prop.getVal();
                                } else if (prop.getName().equals(workerPropName)) {
                                    CustomFieldStringValue val = (CustomFieldStringValue) prop.getVal();
                                    if (val != null && val.getValue() != null && val.getValue().equalsIgnoreCase("true"))
                                        isWorker = true;
                                } else if (prop.getName().equals(workerTagPropName)) {
                                    CustomFieldStringValue val = (CustomFieldStringValue) prop.getVal();
                                    workerTag = val.getValue();
                                }
                            }

                            VirtualMachineMO vmMo = new VirtualMachineMO(hyperHost.getContext(), oc.getObj());
                            if (!template && isWorker) {
                                boolean recycle = false;
                                recycle = mgr.needRecycle(workerTag);

                                if (recycle) {
                                    s_logger.info("Recycle pending worker VM: " + vmMo.getName());

                                    vmMo.cancelPendingTasks();
                                    vmMo.powerOff();
                                    vmMo.detachAllDisksAndDestroy();
                                }
                            }
                        }
                    }
                }
            } else {
                s_logger.error("Host is no longer connected.");
            }

        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context");
                invalidateServiceContext();
            }
        }
    }

    @Override
    public Type getType() {
        return com.cloud.host.Host.Type.Routing;
    }

    @Override
    public StartupCommand[] initialize() {
        try {
            String hostApiVersion = "4.1";
            VmwareContext context = getServiceContext();
            try {
                VmwareHypervisorHost hyperHost = getHyperHost(context);
                assert (hyperHost instanceof HostMO);
                if (!((HostMO) hyperHost).isHyperHostConnected()) {
                    s_logger.info("Host " + hyperHost.getHyperHostName() + " is not in connected state");
                    return null;
                }

                ((HostMO) hyperHost).enableVncOnHostFirewall();

                AboutInfo aboutInfo = ((HostMO) hyperHost).getHostAboutInfo();
                hostApiVersion = aboutInfo.getApiVersion();

            } catch (Exception e) {
                String msg = "VmwareResource intialize() failed due to : " + VmwareHelper.getExceptionMessage(e);
                s_logger.error(msg);
                invalidateServiceContext();
                return null;
            }

            StartupRoutingCommand cmd = new StartupRoutingCommand();
            fillHostInfo(cmd);
            cmd.setHypervisorType(HypervisorType.VMware);
            cmd.setCluster(_cluster);
            cmd.setHypervisorVersion(hostApiVersion);

            List<StartupStorageCommand> storageCmds = initializeLocalStorage();
            StartupCommand[] answerCmds = new StartupCommand[1 + storageCmds.size()];
            answerCmds[0] = cmd;
            for (int i = 0; i < storageCmds.size(); i++) {
                answerCmds[i + 1] = storageCmds.get(i);
            }

            return answerCmds;
        } finally {
            recycleServiceContext();
        }
    }

    private List<StartupStorageCommand> initializeLocalStorage() {
        List<StartupStorageCommand> storageCmds = new ArrayList<StartupStorageCommand>();
        VmwareContext context = getServiceContext();

        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);
            if (hyperHost instanceof HostMO) {
                HostMO hostMo = (HostMO) hyperHost;

                List<Pair<ManagedObjectReference, String>> dsList = hostMo.getLocalDatastoreOnHost();
                for (Pair<ManagedObjectReference, String> dsPair : dsList) {
                    DatastoreMO dsMo = new DatastoreMO(context, dsPair.first());

                    String poolUuid = dsMo.getCustomFieldValue(CustomFieldConstants.CLOUD_UUID);
                    if (poolUuid == null || poolUuid.isEmpty()) {
                        poolUuid = UUID.randomUUID().toString();
                        dsMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, poolUuid);
                    }

                    HypervisorHostHelper.createBaseFolder(dsMo, hyperHost, StoragePoolType.VMFS);

                    DatastoreSummary dsSummary = dsMo.getDatastoreSummary();
                    String address = hostMo.getHostName();
                    StoragePoolInfo pInfo = new StoragePoolInfo(poolUuid, address, dsMo.getMor().getValue(), "", StoragePoolType.VMFS, dsSummary.getCapacity(),
                            dsSummary.getFreeSpace());
                    StartupStorageCommand cmd = new StartupStorageCommand();
                    cmd.setName(poolUuid);
                    cmd.setPoolInfo(pInfo);
                    cmd.setGuid(poolUuid); // give storage host the same UUID as the local storage pool itself
                    cmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
                    cmd.setDataCenter(_dcId);
                    cmd.setPod(_pod);
                    cmd.setCluster(_cluster);

                    s_logger.info("Add local storage startup command: " + _gson.toJson(cmd));
                    storageCmds.add(cmd);
                }

            } else {
                s_logger.info("Cluster host does not support local storage, skip it");
            }
        } catch (Exception e) {
            String msg = "initializing local storage failed due to : " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg);
            invalidateServiceContext();
            throw new CloudRuntimeException(msg);
        }

        return storageCmds;
    }

    protected void fillHostInfo(StartupRoutingCommand cmd) {
        VmwareContext serviceContext = getServiceContext();
        Map<String, String> details = cmd.getHostDetails();
        if (details == null) {
            details = new HashMap<String, String>();
        }

        try {
            fillHostHardwareInfo(serviceContext, cmd);
            fillHostNetworkInfo(serviceContext, cmd);
            fillHostDetailsInfo(serviceContext, details);
        } catch (RuntimeFaultFaultMsg e) {
            s_logger.error("RuntimeFault while retrieving host info: " + e.toString(), e);
            throw new CloudRuntimeException("RuntimeFault while retrieving host info");
        } catch (RemoteException e) {
            s_logger.error("RemoteException while retrieving host info: " + e.toString(), e);
            invalidateServiceContext();
            throw new CloudRuntimeException("RemoteException while retrieving host info");
        } catch (Exception e) {
            s_logger.error("Exception while retrieving host info: " + e.toString(), e);
            invalidateServiceContext();
            throw new CloudRuntimeException("Exception while retrieving host info: " + e.toString());
        }

        cmd.setHostDetails(details);
        cmd.setName(_url);
        cmd.setGuid(_guid);
        cmd.setDataCenter(_dcId);
        cmd.setIqn(getIqn());
        cmd.setPod(_pod);
        cmd.setCluster(_cluster);
        cmd.setVersion(VmwareResource.class.getPackage().getImplementationVersion());
    }

    private String getIqn() {
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());

            if (hyperHost instanceof HostMO) {
                HostMO host = (HostMO) hyperHost;
                HostStorageSystemMO hostStorageSystem = host.getHostStorageSystemMO();

                for (HostHostBusAdapter hba : hostStorageSystem.getStorageDeviceInfo().getHostBusAdapter()) {
                    if (hba instanceof HostInternetScsiHba) {
                        HostInternetScsiHba hostInternetScsiHba = (HostInternetScsiHba) hba;

                        if (hostInternetScsiHba.isIsSoftwareBased()) {
                            return ((HostInternetScsiHba) hba).getIScsiName();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            s_logger.info("Could not locate an IQN for this host.");
        }

        return null;
    }

    private void fillHostHardwareInfo(VmwareContext serviceContext, StartupRoutingCommand cmd) throws RuntimeFaultFaultMsg, RemoteException, Exception {

        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
        VmwareHypervisorHostResourceSummary summary = hyperHost.getHyperHostResourceSummary();

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Startup report on host hardware info. " + _gson.toJson(summary));
        }

        cmd.setCaps("hvm");
        cmd.setDom0MinMemory(0);
        cmd.setSpeed(summary.getCpuSpeed());
        cmd.setCpuSockets(summary.getCpuSockets());
        cmd.setCpus((int) summary.getCpuCount());
        cmd.setMemory(summary.getMemoryBytes());
    }

    private void fillHostNetworkInfo(VmwareContext serviceContext, StartupRoutingCommand cmd) throws RuntimeFaultFaultMsg, RemoteException {

        try {
            VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());

            assert (hyperHost instanceof HostMO);
            VmwareManager mgr = hyperHost.getContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);

            VmwareHypervisorHostNetworkSummary summary = hyperHost.getHyperHostNetworkSummary(mgr.getManagementPortGroupByHost((HostMO) hyperHost));
            if (summary == null) {
                throw new Exception("No ESX(i) host found");
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Startup report on host network info. " + _gson.toJson(summary));
            }

            cmd.setPrivateIpAddress(summary.getHostIp());
            cmd.setPrivateNetmask(summary.getHostNetmask());
            cmd.setPrivateMacAddress(summary.getHostMacAddress());

            cmd.setStorageIpAddress(summary.getHostIp());
            cmd.setStorageNetmask(summary.getHostNetmask());
            cmd.setStorageMacAddress(summary.getHostMacAddress());

        } catch (Throwable e) {
            String msg = "querying host network info failed due to " + VmwareHelper.getExceptionMessage(e);
            s_logger.error(msg, e);
            throw new CloudRuntimeException(msg);
        }
    }

    private void fillHostDetailsInfo(VmwareContext serviceContext, Map<String, String> details) throws Exception {
        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());

        if (hyperHost.isHAEnabled()) {
            details.put("NativeHA", "true");
        }
    }

    protected HashMap<String, HostVmStateReportEntry> syncHostVmStates() {
        try {
            return getHostVmStateReport();
        } catch (Exception e) {
            return new HashMap<String, HostVmStateReportEntry>();
        }
    }

    protected OptionValue[] configureVnc(OptionValue[] optionsToMerge, VmwareHypervisorHost hyperHost, String vmName, String vncPassword, String keyboardLayout) throws Exception {

        VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);

        VmwareManager mgr = hyperHost.getContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
        if (!mgr.beginExclusiveOperation(600))
            throw new Exception("Unable to begin exclusive operation, lock time out");

        try {
            int maxVncPorts = 64;
            int vncPort = 0;
            Random random = new Random();

            HostMO vmOwnerHost = vmMo.getRunningHost();

            ManagedObjectReference morParent = vmOwnerHost.getParentMor();
            HashMap<String, Integer> portInfo;
            if (morParent.getType().equalsIgnoreCase("ClusterComputeResource")) {
                ClusterMO clusterMo = new ClusterMO(vmOwnerHost.getContext(), morParent);
                portInfo = clusterMo.getVmVncPortsOnCluster();
            } else {
                portInfo = vmOwnerHost.getVmVncPortsOnHost();
            }

            // allocate first at 5900 - 5964 range
            Collection<Integer> existingPorts = portInfo.values();
            int val = random.nextInt(maxVncPorts);
            int startVal = val;
            do {
                if (!existingPorts.contains(5900 + val)) {
                    vncPort = 5900 + val;
                    break;
                }

                val = (++val) % maxVncPorts;
            } while (val != startVal);

            if (vncPort == 0) {
                s_logger.info("we've run out of range for ports between 5900-5964 for the cluster, we will try port range at 59000-60000");

                Pair<Integer, Integer> additionalRange = mgr.getAddiionalVncPortRange();
                maxVncPorts = additionalRange.second();
                val = random.nextInt(maxVncPorts);
                startVal = val;
                do {
                    if (!existingPorts.contains(additionalRange.first() + val)) {
                        vncPort = additionalRange.first() + val;
                        break;
                    }

                    val = (++val) % maxVncPorts;
                } while (val != startVal);
            }

            if (vncPort == 0) {
                throw new Exception("Unable to find an available VNC port on host");
            }

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Configure VNC port for VM " + vmName + ", port: " + vncPort + ", host: " + vmOwnerHost.getHyperHostName());
            }

            return VmwareHelper.composeVncOptions(optionsToMerge, true, vncPassword, vncPort, keyboardLayout);
        } finally {
            try {
                mgr.endExclusiveOperation();
            } catch (Throwable e) {
                assert (false);
                s_logger.error("Unexpected exception ", e);
            }
        }
    }

    private VirtualMachineGuestOsIdentifier translateGuestOsIdentifier(String cpuArchitecture, String guestOs, String cloudGuestOs) {
        if (cpuArchitecture == null) {
            s_logger.warn("CPU arch is not set, default to i386. guest os: " + guestOs);
            cpuArchitecture = "i386";
        }

        if (cloudGuestOs == null) {
            s_logger.warn("Guest OS mapping name is not set for guest os: " + guestOs);
        }

        VirtualMachineGuestOsIdentifier identifier = null;
        try {
            if (cloudGuestOs != null) {
                identifier = VirtualMachineGuestOsIdentifier.fromValue(cloudGuestOs);
                s_logger.debug("Using mapping name : " + identifier.toString());
            }
        } catch (IllegalArgumentException e) {
            s_logger.warn("Unable to find Guest OS Identifier in VMware for mapping name: " + cloudGuestOs + ". Continuing with defaults.");
        }
        if (identifier != null) {
            return identifier;
        }

        if (cpuArchitecture.equalsIgnoreCase("x86_64")) {
            return VirtualMachineGuestOsIdentifier.OTHER_GUEST_64;
        }
        return VirtualMachineGuestOsIdentifier.OTHER_GUEST;
    }

    private HashMap<String, HostVmStateReportEntry> getHostVmStateReport() throws Exception {
        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());

        int key = ((HostMO) hyperHost).getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }
        String instanceNameCustomField = "value[" + key + "]";

        // CLOUD_VM_INTERNAL_NAME stores the internal CS generated vm name. This was earlier stored in name. Now, name can be either the hostname or
        // the internal CS name, but the custom field CLOUD_VM_INTERNAL_NAME always stores the internal CS name.
        ObjectContent[] ocs = hyperHost.getVmPropertiesOnHyperHost(new String[]{"name", "runtime.powerState", "config.template", instanceNameCustomField});

        HashMap<String, HostVmStateReportEntry> newStates = new HashMap<String, HostVmStateReportEntry>();
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {

                    boolean isTemplate = false;
                    String name = null;
                    String VMInternalCSName = null;
                    VirtualMachinePowerState powerState = VirtualMachinePowerState.POWERED_OFF;
                    for (DynamicProperty objProp : objProps) {
                        if (objProp.getName().equals("config.template")) {
                            if (objProp.getVal().toString().equalsIgnoreCase("true")) {
                                isTemplate = true;
                            }
                        } else if (objProp.getName().equals("runtime.powerState")) {
                            powerState = (VirtualMachinePowerState) objProp.getVal();
                        } else if (objProp.getName().equals("name")) {
                            name = (String) objProp.getVal();
                        } else if (objProp.getName().contains(instanceNameCustomField)) {
                            if (objProp.getVal() != null)
                                VMInternalCSName = ((CustomFieldStringValue) objProp.getVal()).getValue();
                        } else {
                            assert (false);
                        }
                    }

                    if (VMInternalCSName != null)
                        name = VMInternalCSName;

                    if (!isTemplate) {
                        newStates.put(name, new HostVmStateReportEntry(convertPowerState(powerState), hyperHost.getHyperHostName()));
                    }
                }
            }
        }
        return newStates;
    }

    private HashMap<String, PowerState> getVmStates() throws Exception {
        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());

        int key = ((HostMO) hyperHost).getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }
        String instanceNameCustomField = "value[" + key + "]";

        // CLOUD_VM_INTERNAL_NAME stores the internal CS generated vm name. This was earlier stored in name. Now, name can be either the hostname or
        // the internal CS name, but the custom field CLOUD_VM_INTERNAL_NAME always stores the internal CS name.
        ObjectContent[] ocs = hyperHost.getVmPropertiesOnHyperHost(new String[]{"name", "runtime.powerState", "config.template", instanceNameCustomField});

        HashMap<String, PowerState> newStates = new HashMap<String, PowerState>();
        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {

                    boolean isTemplate = false;
                    String name = null;
                    String VMInternalCSName = null;
                    VirtualMachinePowerState powerState = VirtualMachinePowerState.POWERED_OFF;
                    for (DynamicProperty objProp : objProps) {
                        if (objProp.getName().equals("config.template")) {
                            if (objProp.getVal().toString().equalsIgnoreCase("true")) {
                                isTemplate = true;
                            }
                        } else if (objProp.getName().equals("runtime.powerState")) {
                            powerState = (VirtualMachinePowerState) objProp.getVal();
                        } else if (objProp.getName().equals("name")) {
                            name = (String) objProp.getVal();
                        } else if (objProp.getName().contains(instanceNameCustomField)) {
                            if (objProp.getVal() != null)
                                VMInternalCSName = ((CustomFieldStringValue) objProp.getVal()).getValue();
                        } else {
                            assert (false);
                        }
                    }

                    if (VMInternalCSName != null)
                        name = VMInternalCSName;

                    if (!isTemplate) {
                        newStates.put(name, convertPowerState(powerState));
                    }
                }
            }
        }
        return newStates;
    }

    private HashMap<String, VmStatsEntry> getVmStats(List<String> vmNames) throws Exception {
        VmwareHypervisorHost hyperHost = getHyperHost(getServiceContext());
        HashMap<String, VmStatsEntry> vmResponseMap = new HashMap<String, VmStatsEntry>();
        ManagedObjectReference perfMgr = getServiceContext().getServiceContent().getPerfManager();
        VimPortType service = getServiceContext().getService();

        PerfCounterInfo rxPerfCounterInfo = null;
        PerfCounterInfo txPerfCounterInfo = null;
        PerfCounterInfo diskReadIOPerfCounterInfo = null;
        PerfCounterInfo diskWriteIOPerfCounterInfo = null;
        PerfCounterInfo diskReadKbsPerfCounterInfo = null;
        PerfCounterInfo diskWriteKbsPerfCounterInfo = null;

        Integer windowInterval = getVmwareWindowTimeInterval();
        final XMLGregorianCalendar startTime = VmwareHelper.getXMLGregorianCalendar(new Date(), windowInterval);
        final XMLGregorianCalendar endTime = VmwareHelper.getXMLGregorianCalendar(new Date(), 0);

        List<PerfCounterInfo> cInfo = getServiceContext().getVimClient().getDynamicProperty(perfMgr, "perfCounter");
        for (PerfCounterInfo info : cInfo) {
            if ("net".equalsIgnoreCase(info.getGroupInfo().getKey()) && "average".equalsIgnoreCase(info.getRollupType().value())) {
                if ("transmitted".equalsIgnoreCase(info.getNameInfo().getKey())) {
                    txPerfCounterInfo = info;
                }
                if ("received".equalsIgnoreCase(info.getNameInfo().getKey())) {
                    rxPerfCounterInfo = info;
                }
            }
            if ("virtualdisk".equalsIgnoreCase(info.getGroupInfo().getKey())) {
                if ("numberReadAveraged".equalsIgnoreCase(info.getNameInfo().getKey())) {
                    diskReadIOPerfCounterInfo = info;
                }
                if ("numberWriteAveraged".equalsIgnoreCase(info.getNameInfo().getKey())) {
                    diskWriteIOPerfCounterInfo = info;
                }
                if ("read".equalsIgnoreCase(info.getNameInfo().getKey())) {
                    diskReadKbsPerfCounterInfo = info;
                }
                if ("write".equalsIgnoreCase(info.getNameInfo().getKey())) {
                    diskWriteKbsPerfCounterInfo = info;
                }
            }
        }

        int key = ((HostMO) hyperHost).getCustomFieldKey("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
        if (key == 0) {
            s_logger.warn("Custom field " + CustomFieldConstants.CLOUD_VM_INTERNAL_NAME + " is not registered ?!");
        }
        String instanceNameCustomField = "value[" + key + "]";

        final String numCpuStr = "summary.config.numCpu";
        final String cpuUseStr = "summary.quickStats.overallCpuUsage";
        final String guestMemUseStr = "summary.quickStats.guestMemoryUsage";
        final String memLimitStr = "resourceConfig.memoryAllocation.limit";
        final String memMbStr = "config.hardware.memoryMB";
        final String allocatedCpuStr = "summary.runtime.maxCpuUsage";

        ObjectContent[] ocs = hyperHost.getVmPropertiesOnHyperHost(new String[]{
                "name", numCpuStr, cpuUseStr, guestMemUseStr, memLimitStr, memMbStr, allocatedCpuStr, instanceNameCustomField
        });

        if (ocs != null && ocs.length > 0) {
            for (ObjectContent oc : ocs) {
                List<DynamicProperty> objProps = oc.getPropSet();
                if (objProps != null) {
                    String name = null;
                    String numberCPUs = null;
                    double maxCpuUsage = 0;
                    String memlimit = null;
                    String memkb = null;
                    String guestMemusage = null;
                    String vmNameOnVcenter = null;
                    String vmInternalCSName = null;
                    double allocatedCpu = 0;
                    for (DynamicProperty objProp : objProps) {
                        if (objProp.getName().equals("name")) {
                            vmNameOnVcenter = objProp.getVal().toString();
                        } else if (objProp.getName().contains(instanceNameCustomField)) {
                            if (objProp.getVal() != null)
                                vmInternalCSName = ((CustomFieldStringValue) objProp.getVal()).getValue();
                        } else if (objProp.getName().equals(guestMemUseStr)) {
                            guestMemusage = objProp.getVal().toString();
                        } else if (objProp.getName().equals(numCpuStr)) {
                            numberCPUs = objProp.getVal().toString();
                        } else if (objProp.getName().equals(cpuUseStr)) {
                            maxCpuUsage = NumberUtils.toDouble(objProp.getVal().toString());
                        } else if (objProp.getName().equals(memLimitStr)) {
                            memlimit = objProp.getVal().toString();
                        } else if (objProp.getName().equals(memMbStr)) {
                            memkb = objProp.getVal().toString();
                        } else if (objProp.getName().equals(allocatedCpuStr)) {
                            allocatedCpu = NumberUtils.toDouble(objProp.getVal().toString());
                        }
                    }

                    maxCpuUsage = (maxCpuUsage / allocatedCpu) * 100;
                    if (vmInternalCSName != null) {
                        name = vmInternalCSName;
                    } else {
                        name = vmNameOnVcenter;
                    }

                    if (!vmNames.contains(name)) {
                        continue;
                    }

                    ManagedObjectReference vmMor = hyperHost.findVmOnHyperHost(name).getMor();
                    assert (vmMor != null);

                    double networkReadKBs = 0;
                    double networkWriteKBs = 0;
                    double diskReadIops = 0;
                    double diskWriteIops = 0;
                    double diskReadKbs = 0;
                    double diskWriteKbs = 0;

                    final ArrayList<PerfMetricId> perfMetricsIds = new ArrayList<PerfMetricId>();
                    if (rxPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(rxPerfCounterInfo, ""));
                    }
                    if (txPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(txPerfCounterInfo, ""));
                    }
                    if (diskReadIOPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskReadIOPerfCounterInfo, "*"));
                    }
                    if (diskWriteIOPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskWriteIOPerfCounterInfo, "*"));
                    }
                    if (diskReadKbsPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskReadKbsPerfCounterInfo, ""));
                    }
                    if (diskWriteKbsPerfCounterInfo != null) {
                        perfMetricsIds.add(VmwareHelper.createPerfMetricId(diskWriteKbsPerfCounterInfo, ""));
                    }

                    if (perfMetricsIds.size() > 0) {
                        try {
                            final PerfQuerySpec qSpec = new PerfQuerySpec();
                            qSpec.setEntity(vmMor);
                            qSpec.setFormat("normal");
                            qSpec.setIntervalId(windowInterval);
                            qSpec.setStartTime(startTime);
                            qSpec.setEndTime(endTime);
                            qSpec.getMetricId().addAll(perfMetricsIds);
                            final List<PerfEntityMetricBase> perfValues = service.queryPerf(perfMgr, Collections.singletonList(qSpec));
                            for (final PerfEntityMetricBase perfValue : perfValues) {
                                if (!(perfValue instanceof PerfEntityMetric)) {
                                    continue;
                                }
                                final List<PerfMetricSeries> seriesList = ((PerfEntityMetric) perfValue).getValue();
                                for (final PerfMetricSeries series : seriesList) {
                                    if (!(series instanceof PerfMetricIntSeries)) {
                                        continue;
                                    }
                                    final List<Long> values = ((PerfMetricIntSeries) series).getValue();
                                    double sum = 0;
                                    for (final Long value : values) {
                                        sum += value;
                                    }
                                    double avg = sum / (values.size() * 1f);
                                    if (series.getId().getCounterId() == rxPerfCounterInfo.getKey()) {
                                        networkReadKBs = avg;
                                    }
                                    if (series.getId().getCounterId() == txPerfCounterInfo.getKey()) {
                                        networkWriteKBs = avg;
                                    }
                                    if (series.getId().getCounterId() == diskReadIOPerfCounterInfo.getKey()) {
                                        diskReadIops += avg;
                                    }
                                    if (series.getId().getCounterId() == diskWriteIOPerfCounterInfo.getKey()) {
                                        diskWriteIops += avg;
                                    }
                                    if (series.getId().getCounterId() == diskReadKbsPerfCounterInfo.getKey()) {
                                        diskReadKbs = avg;
                                    }
                                    if (series.getId().getCounterId() == diskWriteKbsPerfCounterInfo.getKey()) {
                                        diskWriteKbs = avg;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            s_logger.error(String.format("Unable to execute PerfQuerySpec due to: [%s]. The window interval is enabled in vCenter?", VmwareHelper.getExceptionMessage(e)), e);
                        }
                    }

                    double doubleMemKb = NumberUtils.toDouble(memkb);
                    double guestFreeMem =  doubleMemKb - NumberUtils.toDouble(guestMemusage);

                    final VmStatsEntry vmStats = new VmStatsEntry(0, doubleMemKb * 1024, guestFreeMem * 1024, NumberUtils.toDouble(memlimit) * 1024, maxCpuUsage, networkReadKBs,
                            networkWriteKBs, NumberUtils.toInt(numberCPUs), diskReadKbs, diskWriteKbs, diskReadIops, diskWriteIops, "vm");
                    vmResponseMap.put(name, vmStats);

                }
            }
        }
        return vmResponseMap;
    }

    protected String networkUsage(final String privateIpAddress, final String option, final String ethName) {
        return networkUsage(privateIpAddress, option, ethName, null);
    }

    protected String networkUsage(final String privateIpAddress, final String option, final String ethName, final String publicIp) {
        String args = null;
        if (option.equals("get")) {
            args = "-g";
            if (StringUtils.isNotEmpty(publicIp)) {
                args += " -l " + publicIp;
            }
        } else if (option.equals("create")) {
            args = "-c";
        } else if (option.equals("reset")) {
            args = "-r";
        } else if (option.equals("addVif")) {
            args = "-a";
            args += ethName;
        } else if (option.equals("deleteVif")) {
            args = "-d";
            args += ethName;
        }

        ExecutionResult result = executeInVR(privateIpAddress, "netusage.sh", args);

        if (!result.isSuccess()) {
            return null;
        }

        return result.getDetails();
    }

    protected long[] getNetworkStats(String privateIP, String publicIp) {
        String result = networkUsage(privateIP, "get", null, publicIp);
        long[] stats = new long[2];
        if (result != null) {
            try {
                String[] splitResult = result.split(":");
                int i = 0;
                while (i < splitResult.length - 1) {
                    stats[0] += Long.parseLong(splitResult[i++]);
                    stats[1] += Long.parseLong(splitResult[i++]);
                }
            } catch (Throwable e) {
                s_logger.warn("Unable to parse return from script return of network usage command: " + e.toString(), e);
            }
        }
        return stats;
    }

    protected String connect(final String vmName, final String ipAddress, final int port) {
        long startTick = System.currentTimeMillis();

        // wait until we have at least been waiting for _ops_timeout time or
        // at least have tried _retry times, this is to coordinate with system
        // VM patching/rebooting time that may need
        int retry = _retry;
        while (System.currentTimeMillis() - startTick <= _opsTimeout || --retry > 0) {
            s_logger.info("Trying to connect to " + ipAddress);
            try (SocketChannel sch = SocketChannel.open();) {
                sch.configureBlocking(true);
                sch.socket().setSoTimeout(5000);

                InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
                sch.connect(addr);
                return null;
            } catch (IOException e) {
                s_logger.info("Could not connect to " + ipAddress + " due to " + e.toString());
                if (e instanceof ConnectException) {
                    // if connection is refused because of VM is being started,
                    // we give it more sleep time
                    // to avoid running out of retry quota too quickly
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        s_logger.debug("[ignored] interrupted while waiting to retry connect after failure.", e);
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                s_logger.debug("[ignored] interrupted while waiting to retry connect.");
            }
        }

        s_logger.info("Unable to logon to " + ipAddress);

        return "Unable to connect";
    }

    protected String connect(final String vmname, final String ipAddress) {
        return connect(vmname, ipAddress, 3922);
    }

    public static PowerState getVmState(VirtualMachineMO vmMo) throws Exception {
        VirtualMachineRuntimeInfo runtimeInfo = vmMo.getRuntimeInfo();
        return convertPowerState(runtimeInfo.getPowerState());
    }

    private static PowerState convertPowerState(VirtualMachinePowerState powerState) {
        return s_powerStatesTable.get(powerState);
    }

    public static PowerState getVmPowerState(VirtualMachineMO vmMo) throws Exception {
        VirtualMachineRuntimeInfo runtimeInfo = vmMo.getRuntimeInfo();
        return convertPowerState(runtimeInfo.getPowerState());
    }

    private static HostStatsEntry getHyperHostStats(VmwareHypervisorHost hyperHost) throws Exception {
        ComputeResourceSummary hardwareSummary = hyperHost.getHyperHostHardwareSummary();
        if (hardwareSummary == null)
            return null;

        HostStatsEntry entry = new HostStatsEntry();

        entry.setEntityType("host");
        double cpuUtilization = ((double) (hardwareSummary.getTotalCpu() - hardwareSummary.getEffectiveCpu()) / (double) hardwareSummary.getTotalCpu() * 100);
        entry.setCpuUtilization(cpuUtilization);
        entry.setTotalMemoryKBs(hardwareSummary.getTotalMemory() / 1024);
        entry.setFreeMemoryKBs(hardwareSummary.getEffectiveMemory() * 1024);

        return entry;
    }

    private static String getRouterSshControlIp(NetworkElementCommand cmd) {
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (s_logger.isDebugEnabled())
            s_logger.debug("Use router's private IP for SSH control. IP : " + routerIp);
        return routerIp;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            _name = name;

            _url = (String) params.get("url");
            _username = (String) params.get("username");
            _password = (String) params.get("password");
            _dcId = (String) params.get("zone");
            _pod = (String) params.get("pod");
            _cluster = (String) params.get("cluster");

            _guid = (String) params.get("guid");
            String[] tokens = _guid.split("@");
            _vCenterAddress = tokens[1];
            _morHyperHost = new ManagedObjectReference();
            String[] hostTokens = tokens[0].split(":");
            _morHyperHost.setType(hostTokens[0]);
            _morHyperHost.setValue(hostTokens[1]);

            _guestTrafficInfo = (VmwareTrafficLabel) params.get("guestTrafficInfo");
            _publicTrafficInfo = (VmwareTrafficLabel) params.get("publicTrafficInfo");
            VmwareContext context = getServiceContext();
            VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
            if (mgr == null) {
                throw new ConfigurationException("Invalid vmwareContext:  vmwareMgr stock object is not set or cleared.");
            }
            mgr.setupResourceStartupParams(params);

            CustomFieldsManagerMO cfmMo = new CustomFieldsManagerMO(context, context.getServiceContent().getCustomFieldsManager());
            cfmMo.ensureCustomFieldDef("Datastore", CustomFieldConstants.CLOUD_UUID);
            cfmMo.ensureCustomFieldDef("StoragePod", CustomFieldConstants.CLOUD_UUID);

            if (_publicTrafficInfo != null && _publicTrafficInfo.getVirtualSwitchType() != VirtualSwitchType.StandardVirtualSwitch
                    || _guestTrafficInfo != null && _guestTrafficInfo.getVirtualSwitchType() != VirtualSwitchType.StandardVirtualSwitch) {
                cfmMo.ensureCustomFieldDef("DistributedVirtualPortgroup", CustomFieldConstants.CLOUD_GC_DVP);
            }
            cfmMo.ensureCustomFieldDef("Network", CustomFieldConstants.CLOUD_GC);
            cfmMo.ensureCustomFieldDef("VirtualMachine", CustomFieldConstants.CLOUD_UUID);
            cfmMo.ensureCustomFieldDef("VirtualMachine", CustomFieldConstants.CLOUD_NIC_MASK);
            cfmMo.ensureCustomFieldDef("VirtualMachine", CustomFieldConstants.CLOUD_VM_INTERNAL_NAME);
            cfmMo.ensureCustomFieldDef("VirtualMachine", CustomFieldConstants.CLOUD_WORKER);
            cfmMo.ensureCustomFieldDef("VirtualMachine", CustomFieldConstants.CLOUD_WORKER_TAG);

            VmwareHypervisorHost hostMo = this.getHyperHost(context);
            _hostName = hostMo.getHyperHostName();

            if (_guestTrafficInfo.getVirtualSwitchType() == VirtualSwitchType.NexusDistributedVirtualSwitch
                    || _publicTrafficInfo.getVirtualSwitchType() == VirtualSwitchType.NexusDistributedVirtualSwitch) {
                _privateNetworkVSwitchName = mgr.getPrivateVSwitchName(Long.parseLong(_dcId), HypervisorType.VMware);
                _vsmCredentials = mgr.getNexusVSMCredentialsByClusterId(Long.parseLong(_cluster));
            }

            if (_privateNetworkVSwitchName == null) {
                _privateNetworkVSwitchName = (String) params.get("private.network.vswitch.name");
            }

            String value = (String) params.get("vmware.recycle.hung.wokervm");
            if (value != null && value.equalsIgnoreCase("true"))
                _recycleHungWorker = true;

            value = (String) params.get("vmware.root.disk.controller");
            if (value != null && value.equalsIgnoreCase("scsi"))
                _rootDiskController = DiskControllerType.scsi;
            else if (value != null && value.equalsIgnoreCase("ide"))
                _rootDiskController = DiskControllerType.ide;
            else
                _rootDiskController = DiskControllerType.osdefault;

            Integer intObj = (Integer) params.get("ports.per.dvportgroup");
            if (intObj != null)
                _portsPerDvPortGroup = intObj.intValue();

            s_logger.info("VmwareResource network configuration info." + " private traffic over vSwitch: " + _privateNetworkVSwitchName + ", public traffic over "
                    + _publicTrafficInfo.getVirtualSwitchType() + " : " + _publicTrafficInfo.getVirtualSwitchName() + ", guest traffic over "
                    + _guestTrafficInfo.getVirtualSwitchType() + " : " + _guestTrafficInfo.getVirtualSwitchName());

            Boolean boolObj = (Boolean) params.get("vmware.create.full.clone");
            if (boolObj != null && boolObj.booleanValue()) {
                _fullCloneFlag = true;
            } else {
                _fullCloneFlag = false;
            }

            boolObj = (Boolean) params.get("vm.instancename.flag");
            if (boolObj != null && boolObj.booleanValue()) {
                _instanceNameFlag = true;
            } else {
                _instanceNameFlag = false;
            }

            value = (String) params.get("scripts.timeout");
            int timeout = NumbersUtil.parseInt(value, 1440) * 1000;

            storageNfsVersion = NfsSecondaryStorageResource.retrieveNfsVersionFromParams(params);
            _storageProcessor = new VmwareStorageProcessor((VmwareHostService) this, _fullCloneFlag, (VmwareStorageMount) mgr, timeout, this, _shutdownWaitMs, null,
                    storageNfsVersion);
            storageHandler = new VmwareStorageSubsystemCommandHandler(_storageProcessor, storageNfsVersion);

            _vrResource = new VirtualRoutingResource(this);
            if (!_vrResource.configure(name, params)) {
                throw new ConfigurationException("Unable to configure VirtualRoutingResource");
            }

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Successfully configured VmwareResource.");
            }
            return true;
        } catch (Exception e) {
            s_logger.error("Unexpected Exception ", e);
            throw new ConfigurationException("Failed to configure VmwareResource due to unexpect exception.");
        } finally {
            recycleServiceContext();
        }
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    protected String getDefaultScriptsDir() {
        return null;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public VmwareContext getServiceContext() {
        return getServiceContext(null);
    }

    public void invalidateServiceContext() {
        invalidateServiceContext(null);
    }

    public VmwareHypervisorHost getHyperHost(VmwareContext context) {
        return getHyperHost(context, null);
    }

    @Override
    public VmwareContext getServiceContext(Command cmd) {
        VmwareContext context = null;
        if (s_serviceContext.get() != null) {
            context = s_serviceContext.get();
            String poolKey = VmwareContextPool.composePoolKey(_vCenterAddress, _username);
            // Before re-using the thread local context, ensure it corresponds to the right vCenter API session and that it is valid to make calls.
            if (context.getPoolKey().equals(poolKey)) {
                if (context.validate()) {
                    if (s_logger.isTraceEnabled()) {
                        s_logger.trace("ThreadLocal context is still valid, just reuse");
                    }
                    return context;
                } else {
                    s_logger.info("Validation of the context failed, dispose and use a new one");
                    invalidateServiceContext(context);
                }
            } else {
                // Exisitng ThreadLocal context corresponds to a different vCenter API session. Why has it not been recycled?
                s_logger.warn("ThreadLocal VMware context: " + poolKey + " doesn't correspond to the right vCenter. Expected VMware context: " + context.getPoolKey());
            }
        }
        try {
            context = VmwareContextFactory.getContext(_vCenterAddress, _username, _password);
            s_serviceContext.set(context);
        } catch (Exception e) {
            s_logger.error("Unable to connect to vSphere server: " + _vCenterAddress, e);
            throw new CloudRuntimeException("Unable to connect to vSphere server: " + _vCenterAddress);
        }
        return context;
    }

    @Override
    public void invalidateServiceContext(VmwareContext context) {
        assert (s_serviceContext.get() == context);

        s_serviceContext.set(null);
        if (context != null)
            context.close();
    }

    private static void recycleServiceContext() {
        VmwareContext context = s_serviceContext.get();
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Reset threadlocal context to null");
        }
        s_serviceContext.set(null);

        if (context != null) {
            assert (context.getPool() != null);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Recycling threadlocal context to pool");
            }
            context.getPool().registerContext(context);
        }
    }

    @Override
    public VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd) {
        if (_morHyperHost.getType().equalsIgnoreCase("HostSystem")) {
            return new HostMO(context, _morHyperHost);
        }
        return new ClusterMO(context, _morHyperHost);
    }

    @Override
    @DB
    public String getWorkerName(VmwareContext context, Command cmd, int workerSequence, DatastoreMO sourceDsMo) throws Exception {
        VmwareManager mgr = context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
        String vmName = mgr.composeWorkerName();
        if (sourceDsMo!= null && sourceDsMo.getDatastoreType().equalsIgnoreCase("VVOL")) {
            vmName = CustomFieldConstants.CLOUD_UUID + "-" + vmName;
        }

        assert (cmd != null);
        context.getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
        // TODO: Fix this? long checkPointId = vmwareMgr.pushCleanupCheckpoint(this._guid, vmName);
        // TODO: Fix this? cmd.setContextParam("checkpoint", String.valueOf(checkPointId));
        return vmName;
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub
    }

    @Override
    public Answer execute(DestroyCommand cmd) {
        try {
            VmwareContext context = getServiceContext(null);
            VmwareHypervisorHost hyperHost = getHyperHost(context, null);
            VolumeTO vol = cmd.getVolume();

            VirtualMachineMO vmMo = findVmOnDatacenter(context, hyperHost, vol);

            if (vmMo != null) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Destroy template volume " + vol.getPath());
                }
                if (vmMo.isTemplate()) {
                    vmMo.markAsVirtualMachine(hyperHost.getHyperHostOwnerResourcePool(), hyperHost.getMor());
                }
                vmMo.destroy();
            } else {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Template volume " + vol.getPath() + " is not found, no need to delete.");
                }
            }
            return new Answer(cmd, true, "Success");

        } catch (Throwable e) {
            return new Answer(cmd, false, createLogMessageException(e, cmd));
        }
    }

    /**
     * Use data center to look for vm, instead of randomly picking up a cluster<br/>
     * (in multiple cluster environments vm could not be found if wrong cluster was chosen)
     *
     * @param context   vmware context
     * @param hyperHost vmware hv host
     * @param vol       volume
     * @return a virtualmachinemo if could be found on datacenter.
     * @throws Exception             if there is an error while finding vm
     * @throws CloudRuntimeException if datacenter cannot be found
     */
    protected VirtualMachineMO findVmOnDatacenter(VmwareContext context, VmwareHypervisorHost hyperHost, VolumeTO vol) throws Exception {
        DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
        if (dcMo.getMor() == null) {
            String msg = "Unable to find VMware DC";
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }
        return dcMo.findVm(vol.getPath());
    }

    public String getAbsoluteVmdkFile(VirtualDisk disk) {
        String vmdkAbsFile = null;
        VirtualDeviceBackingInfo backingInfo = disk.getBacking();
        if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
            VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo) backingInfo;
            vmdkAbsFile = diskBackingInfo.getFileName();
        }
        return vmdkAbsFile;
    }

    protected File getSystemVmKeyFile() {
        if (s_systemVmKeyFile == null) {
            syncFetchSystemVmKeyFile();
        }
        return s_systemVmKeyFile;
    }

    private static void syncFetchSystemVmKeyFile() {
        synchronized (s_syncLockObjectFetchKeyFile) {
            if (s_systemVmKeyFile == null) {
                s_systemVmKeyFile = fetchSystemVmKeyFile();
            }
        }
    }

    private static File fetchSystemVmKeyFile() {
        String filePath = s_relativePathSystemVmKeyFileInstallDir;
        s_logger.debug("Looking for file [" + filePath + "] in the classpath.");
        URL url = Script.class.getClassLoader().getResource(filePath);
        File keyFile = null;
        if (url != null) {
            keyFile = new File(url.getPath());
        }
        if (keyFile == null || !keyFile.exists()) {
            filePath = s_defaultPathSystemVmKeyFile;
            keyFile = new File(filePath);
            s_logger.debug("Looking for file [" + filePath + "] in the classpath.");
        }
        if (!keyFile.exists()) {
            s_logger.error("Unable to locate id_rsa.cloud in your setup at " + keyFile.toString());
        }
        return keyFile;
    }

    private List<UnmanagedInstanceTO.Disk> getUnmanageInstanceDisks(VirtualMachineMO vmMo) {
        List<UnmanagedInstanceTO.Disk> instanceDisks = new ArrayList<>();
        VirtualDisk[] disks = null;
        try {
            disks = vmMo.getAllDiskDevice();
        } catch (Exception e) {
            s_logger.info("Unable to retrieve unmanaged instance disks. " + e.getMessage());
        }
        if (disks != null) {
            for (VirtualDevice diskDevice : disks) {
                try {
                    if (diskDevice instanceof VirtualDisk) {
                        UnmanagedInstanceTO.Disk instanceDisk = new UnmanagedInstanceTO.Disk();
                        VirtualDisk disk = (VirtualDisk) diskDevice;
                        instanceDisk.setDiskId(disk.getDiskObjectId());
                        instanceDisk.setLabel(disk.getDeviceInfo() != null ? disk.getDeviceInfo().getLabel() : "");
                        instanceDisk.setFileBaseName(vmMo.getVmdkFileBaseName(disk));
                        instanceDisk.setImagePath(getAbsoluteVmdkFile(disk));
                        instanceDisk.setCapacity(disk.getCapacityInBytes());
                        instanceDisk.setPosition(diskDevice.getUnitNumber());
                        DatastoreFile file = new DatastoreFile(getAbsoluteVmdkFile(disk));
                        if (StringUtils.isNoneEmpty(file.getFileBaseName(), file.getDatastoreName())) {
                            VirtualMachineDiskInfo diskInfo = vmMo.getDiskInfoBuilder().getDiskInfoByBackingFileBaseName(file.getFileBaseName(), file.getDatastoreName());
                            instanceDisk.setChainInfo(getGson().toJson(diskInfo));
                        }
                        for (VirtualDevice device : vmMo.getAllDeviceList()) {
                            if (diskDevice.getControllerKey() == device.getKey()) {
                                if (device instanceof VirtualIDEController) {
                                    instanceDisk.setController(DiskControllerType.getType(device.getClass().getSimpleName()).toString());
                                    instanceDisk.setControllerUnit(((VirtualIDEController) device).getBusNumber());
                                } else if (device instanceof VirtualSCSIController) {
                                    instanceDisk.setController(DiskControllerType.getType(device.getClass().getSimpleName()).toString());
                                    instanceDisk.setControllerUnit(((VirtualSCSIController) device).getBusNumber());
                                } else {
                                    instanceDisk.setController(DiskControllerType.none.toString());
                                }
                                break;
                            }
                        }
                        if (disk.getBacking() instanceof VirtualDeviceFileBackingInfo) {
                            VirtualDeviceFileBackingInfo diskBacking = (VirtualDeviceFileBackingInfo) disk.getBacking();
                            ManagedObjectReference morDs = diskBacking.getDatastore();
                            DatastoreInfo info = (DatastoreInfo)vmMo.getContext().getVimClient().getDynamicProperty(diskBacking.getDatastore(), "info");
                            if (info instanceof NasDatastoreInfo) {
                                NasDatastoreInfo dsInfo = (NasDatastoreInfo) info;
                                instanceDisk.setDatastoreName(dsInfo.getName());
                                if (dsInfo.getNas() != null) {
                                    instanceDisk.setDatastoreHost(dsInfo.getNas().getRemoteHost());
                                    instanceDisk.setDatastorePath(dsInfo.getNas().getRemotePath());
                                    instanceDisk.setDatastoreType(dsInfo.getNas().getType());
                                }
                            } else {
                                instanceDisk.setDatastoreName(info.getName());
                            }
                        }
                        s_logger.info(vmMo.getName() + " " + disk.getDeviceInfo().getLabel() + " " + disk.getDeviceInfo().getSummary() + " " + disk.getDiskObjectId() + " " + disk.getCapacityInKB() + " " + instanceDisk.getController());
                        instanceDisks.add(instanceDisk);
                    }
                } catch (Exception e) {
                    s_logger.info("Unable to retrieve unmanaged instance disk info. " + e.getMessage());
                }
            }
            Collections.sort(instanceDisks, new Comparator<UnmanagedInstanceTO.Disk>() {
                @Override
                public int compare(final UnmanagedInstanceTO.Disk disk1, final UnmanagedInstanceTO.Disk disk2) {
                    return extractInt(disk1) - extractInt(disk2);
                }

                int extractInt(UnmanagedInstanceTO.Disk disk) {
                    String num = disk.getLabel().replaceAll("\\D", "");
                    // return 0 if no digits found
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
        }
        return instanceDisks;
    }

    private List<UnmanagedInstanceTO.Nic> getUnmanageInstanceNics(VmwareHypervisorHost hyperHost, VirtualMachineMO vmMo) {
        List<UnmanagedInstanceTO.Nic> instanceNics = new ArrayList<>();

        HashMap<String, List<String>> guestNicMacIPAddressMap = new HashMap<>();
        try {
            GuestInfo guestInfo = vmMo.getGuestInfo();
            if (guestInfo.getToolsStatus() == VirtualMachineToolsStatus.TOOLS_OK) {
                for (GuestNicInfo nicInfo: guestInfo.getNet()) {
                    if (CollectionUtils.isNotEmpty(nicInfo.getIpAddress())) {
                        List<String> ipAddresses = new ArrayList<>();
                        for (String ipAddress : nicInfo.getIpAddress()) {
                            if (NetUtils.isValidIp4(ipAddress)) {
                                ipAddresses.add(ipAddress);
                            }
                        }
                        guestNicMacIPAddressMap.put(nicInfo.getMacAddress(), ipAddresses);
                    }
                }
            } else {
                s_logger.info(String.format("Unable to retrieve guest nics for instance: %s from VMware tools as tools status: %s", vmMo.getName(), guestInfo.getToolsStatus().toString()));
            }
        } catch (Exception e) {
            s_logger.info("Unable to retrieve guest nics for instance from VMware tools. " + e.getMessage());
        }
        VirtualDevice[] nics = null;
        try {
            nics = vmMo.getNicDevices();
        } catch (Exception e) {
            s_logger.info("Unable to retrieve unmanaged instance nics. " + e.getMessage());
        }
        if (nics != null) {
            for (VirtualDevice nic : nics) {
                try {
                    VirtualEthernetCard ethCardDevice = (VirtualEthernetCard) nic;
                    s_logger.error(nic.getClass().getCanonicalName() + " " + nic.getBacking().getClass().getCanonicalName() + " " + ethCardDevice.getMacAddress());
                    UnmanagedInstanceTO.Nic instanceNic = new UnmanagedInstanceTO.Nic();
                    instanceNic.setNicId(ethCardDevice.getDeviceInfo().getLabel());
                    if (ethCardDevice instanceof VirtualPCNet32) {
                        instanceNic.setAdapterType(VirtualEthernetCardType.PCNet32.toString());
                    } else if (ethCardDevice instanceof VirtualVmxnet2) {
                        instanceNic.setAdapterType(VirtualEthernetCardType.Vmxnet2.toString());
                    } else if (ethCardDevice instanceof VirtualVmxnet3) {
                        instanceNic.setAdapterType(VirtualEthernetCardType.Vmxnet3.toString());
                    } else {
                        instanceNic.setAdapterType(VirtualEthernetCardType.E1000.toString());
                    }
                    instanceNic.setMacAddress(ethCardDevice.getMacAddress());
                    if (guestNicMacIPAddressMap.containsKey(instanceNic.getMacAddress())) {
                        instanceNic.setIpAddress(guestNicMacIPAddressMap.get(instanceNic.getMacAddress()));
                    }
                    if (ethCardDevice.getSlotInfo() != null) {
                        instanceNic.setPciSlot(ethCardDevice.getSlotInfo().toString());
                    }
                    VirtualDeviceBackingInfo backing = ethCardDevice.getBacking();
                    if (backing instanceof VirtualEthernetCardDistributedVirtualPortBackingInfo) {
                        VirtualEthernetCardDistributedVirtualPortBackingInfo backingInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo) backing;
                        DistributedVirtualSwitchPortConnection port = backingInfo.getPort();
                        String portKey = port.getPortKey();
                        String portGroupKey = port.getPortgroupKey();
                        String dvSwitchUuid = port.getSwitchUuid();

                        s_logger.debug("NIC " + nic.toString() + " is connected to dvSwitch " + dvSwitchUuid + " pg " + portGroupKey + " port " + portKey);

                        ManagedObjectReference dvSwitchManager = vmMo.getContext().getVimClient().getServiceContent().getDvSwitchManager();
                        ManagedObjectReference dvSwitch = vmMo.getContext().getVimClient().getService().queryDvsByUuid(dvSwitchManager, dvSwitchUuid);

                        // Get all ports
                        DistributedVirtualSwitchPortCriteria criteria = new DistributedVirtualSwitchPortCriteria();
                        criteria.setInside(true);
                        criteria.getPortgroupKey().add(portGroupKey);
                        List<DistributedVirtualPort> dvPorts = vmMo.getContext().getVimClient().getService().fetchDVPorts(dvSwitch, criteria);

                        for (DistributedVirtualPort dvPort : dvPorts) {
                            // Find the port for this NIC by portkey
                            if (portKey.equals(dvPort.getKey())) {
                                VMwareDVSPortSetting settings = (VMwareDVSPortSetting) dvPort.getConfig().getSetting();
                                if (settings.getVlan() instanceof VmwareDistributedVirtualSwitchVlanIdSpec) {
                                    VmwareDistributedVirtualSwitchVlanIdSpec vlanId = (VmwareDistributedVirtualSwitchVlanIdSpec) settings.getVlan();
                                    s_logger.trace("Found port " + dvPort.getKey() + " with vlan " + vlanId.getVlanId());
                                    if (vlanId.getVlanId() > 0 && vlanId.getVlanId() < 4095) {
                                        instanceNic.setVlan(vlanId.getVlanId());
                                    }
                                } else if (settings.getVlan() instanceof VmwareDistributedVirtualSwitchPvlanSpec) {
                                    VmwareDistributedVirtualSwitchPvlanSpec pvlanSpec = (VmwareDistributedVirtualSwitchPvlanSpec) settings.getVlan();
                                    s_logger.trace("Found port " + dvPort.getKey() + " with pvlan " + pvlanSpec.getPvlanId());
                                    if (pvlanSpec.getPvlanId() > 0 && pvlanSpec.getPvlanId() < 4095) {
                                        DistributedVirtualSwitchMO dvSwitchMo = new DistributedVirtualSwitchMO(vmMo.getContext(), dvSwitch);
                                        Pair<Integer, HypervisorHostHelper.PvlanType> vlanDetails = dvSwitchMo.retrieveVlanFromPvlan(pvlanSpec.getPvlanId(), dvSwitch);
                                        if (vlanDetails != null && vlanDetails.first() != null && vlanDetails.second() != null) {
                                            instanceNic.setVlan(vlanDetails.first());
                                            instanceNic.setPvlan(pvlanSpec.getPvlanId());
                                            instanceNic.setPvlanType(vlanDetails.second().toString());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    } else if (backing instanceof VirtualEthernetCardNetworkBackingInfo) {
                        VirtualEthernetCardNetworkBackingInfo backingInfo = (VirtualEthernetCardNetworkBackingInfo) backing;
                        instanceNic.setNetwork(backingInfo.getDeviceName());
                        if (hyperHost instanceof HostMO) {
                            HostMO hostMo = (HostMO) hyperHost;
                            HostPortGroupSpec portGroupSpec = hostMo.getHostPortGroupSpec(backingInfo.getDeviceName());
                            instanceNic.setVlan(portGroupSpec.getVlanId());
                        }
                    }
                    instanceNics.add(instanceNic);
                } catch (Exception e) {
                    s_logger.info("Unable to retrieve unmanaged instance nic info. " + e.getMessage());
                }
            }
            Collections.sort(instanceNics, new Comparator<UnmanagedInstanceTO.Nic>() {
                @Override
                public int compare(final UnmanagedInstanceTO.Nic nic1, final UnmanagedInstanceTO.Nic nic2) {
                    return extractInt(nic1) - extractInt(nic2);
                }

                int extractInt(UnmanagedInstanceTO.Nic nic) {
                    String num = nic.getNicId().replaceAll("\\D", "");
                    // return 0 if no digits found
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
        }
        return  instanceNics;
    }

    private UnmanagedInstanceTO getUnmanagedInstance(VmwareHypervisorHost hyperHost, VirtualMachineMO vmMo) {
        UnmanagedInstanceTO instance = null;
        try {
            instance = new UnmanagedInstanceTO();
            instance.setName(vmMo.getVmName());
            instance.setInternalCSName(vmMo.getInternalCSName());
            instance.setCpuCores(vmMo.getConfigSummary().getNumCpu());
            instance.setCpuCoresPerSocket(vmMo.getCoresPerSocket());
            instance.setCpuSpeed(vmMo.getConfigSummary().getCpuReservation());
            instance.setMemory(vmMo.getConfigSummary().getMemorySizeMB());
            instance.setOperatingSystemId(vmMo.getVmGuestInfo().getGuestId());
            if (StringUtils.isEmpty(instance.getOperatingSystemId())) {
                instance.setOperatingSystemId(vmMo.getConfigSummary().getGuestId());
            }
            VirtualMachineGuestOsIdentifier osIdentifier = VirtualMachineGuestOsIdentifier.OTHER_GUEST;
            try {
                osIdentifier = VirtualMachineGuestOsIdentifier.fromValue(instance.getOperatingSystemId());
            } catch (IllegalArgumentException iae) {
                if (StringUtils.isNotEmpty(instance.getOperatingSystemId()) && instance.getOperatingSystemId().contains("64")) {
                    osIdentifier = VirtualMachineGuestOsIdentifier.OTHER_GUEST_64;
                }
            }
            instance.setOperatingSystem(vmMo.getGuestInfo().getGuestFullName());
            if (StringUtils.isEmpty(instance.getOperatingSystem())) {
                instance.setOperatingSystem(vmMo.getConfigSummary().getGuestFullName());
            }
            UnmanagedInstanceTO.PowerState powerState = UnmanagedInstanceTO.PowerState.PowerUnknown;
            if (vmMo.getPowerState().toString().equalsIgnoreCase("POWERED_ON")) {
                powerState = UnmanagedInstanceTO.PowerState.PowerOn;
            }
            if (vmMo.getPowerState().toString().equalsIgnoreCase("POWERED_OFF")) {
                powerState = UnmanagedInstanceTO.PowerState.PowerOff;
            }
            instance.setPowerState(powerState);
            instance.setDisks(getUnmanageInstanceDisks(vmMo));
            instance.setNics(getUnmanageInstanceNics(hyperHost, vmMo));
        } catch (Exception e) {
            s_logger.info("Unable to retrieve unmanaged instance info. " + e.getMessage());
        }

        return  instance;
    }

    private Answer execute(GetUnmanagedInstancesCommand cmd) {
        VmwareContext context = getServiceContext();
        HashMap<String, UnmanagedInstanceTO> unmanagedInstances = new HashMap<>();
        try {
            VmwareHypervisorHost hyperHost = getHyperHost(context);

            String vmName = cmd.getInstanceName();
            List<VirtualMachineMO> vmMos = hyperHost.listVmsOnHyperHostWithHypervisorName(vmName);

            for (VirtualMachineMO vmMo : vmMos) {
                if (vmMo == null) {
                    continue;
                }
                if (vmMo.isTemplate()) {
                    continue;
                }
                // Filter managed instances
                if (cmd.hasManagedInstance(vmMo.getName())) {
                    continue;
                }
                // Filter instance if answer is requested for a particular instance name
                if (StringUtils.isNotEmpty(cmd.getInstanceName()) &&
                        !cmd.getInstanceName().equals(vmMo.getVmName())) {
                    continue;
                }
                UnmanagedInstanceTO instance = getUnmanagedInstance(hyperHost, vmMo);
                if (instance != null) {
                    unmanagedInstances.put(instance.getName(), instance);
                }
            }
        } catch (Exception e) {
            s_logger.info("GetUnmanagedInstancesCommand failed due to " + VmwareHelper.getExceptionMessage(e));
        }
        return new GetUnmanagedInstancesAnswer(cmd, "", unmanagedInstances);
    }

    private Answer execute(PrepareUnmanageVMInstanceCommand cmd) {
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        String instanceName = cmd.getInstanceName();

        try {
            s_logger.debug(String.format("Verify if VMware instance: [%s] is available before unmanaging VM.", cmd.getInstanceName()));

            ManagedObjectReference  dcMor = hyperHost.getHyperHostDatacenter();
            DatacenterMO dataCenterMo = new DatacenterMO(getServiceContext(), dcMor);
            VirtualMachineMO vm = dataCenterMo.findVm(instanceName);
            if (vm == null) {
                return new PrepareUnmanageVMInstanceAnswer(cmd, false, String.format("Cannot find VM with name [%s] in datacenter [%s].", instanceName, dataCenterMo.getName()));
            }
        } catch (Exception e) {
            s_logger.error("Error trying to verify if VM to unmanage exists", e);
            return new PrepareUnmanageVMInstanceAnswer(cmd, false, "Error: " + e.getMessage());
        }

        return new PrepareUnmanageVMInstanceAnswer(cmd, true, "OK");
    }

    /*
     * Method to relocate a virtual machine. This migrates VM and its volumes to given host, datastores.
     * It is used for MigrateVolumeCommand (detached volume case), MigrateVmToPoolCommand and MigrateVmWithStorageCommand.
     */

    private List<VolumeObjectTO> relocateVirtualMachine(final VmwareHypervisorHost hypervisorHost,
                                                        final String name, final VirtualMachineTO vmTo,
                                                        final String targetHost, final VmwareHypervisorHost hostInTargetCluster,
                                                        final String poolUuid, final List<Pair<VolumeTO, StorageFilerTO>> volToFiler) throws Exception {
        String vmName = name;
        if (vmName == null && vmTo != null) {
            vmName = vmTo.getName();
        }
        VmwareHypervisorHost sourceHyperHost = hypervisorHost;
        VmwareHypervisorHost targetHyperHost = hostInTargetCluster;
        VirtualMachineMO vmMo = null;
        ManagedObjectReference morSourceHostDc = null;
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        List<VirtualMachineRelocateSpecDiskLocator> diskLocators = new ArrayList<VirtualMachineRelocateSpecDiskLocator>();
        Set<String> mountedDatastoresAtSource = new HashSet<String>();
        List<VolumeObjectTO> volumeToList =  new ArrayList<>();
        Map<Long, Integer> volumeDeviceKey = new HashMap<Long, Integer>();

        try {
            if (sourceHyperHost == null) {
                sourceHyperHost = getHyperHost(getServiceContext());
            }
            if (targetHyperHost == null && StringUtils.isNotBlank(targetHost)) {
                targetHyperHost = VmwareHelper.getHostMOFromHostName(getServiceContext(), targetHost);
            }
            morSourceHostDc = sourceHyperHost.getHyperHostDatacenter();
            DatacenterMO dcMo = new DatacenterMO(sourceHyperHost.getContext(), morSourceHostDc);
            if (targetHyperHost != null) {
                ManagedObjectReference morTargetHostDc = targetHyperHost.getHyperHostDatacenter();
                if (!morSourceHostDc.getValue().equalsIgnoreCase(morTargetHostDc.getValue())) {
                    String msg = String.format("VM: %s cannot be migrated between different datacenter", vmName);
                    throw new CloudRuntimeException(msg);
                }
            }

            // find VM through source host (VM is not at the target host yet)
            vmMo = sourceHyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                String msg = String.format("VM: %s does not exist on host: %s", vmName, sourceHyperHost.getHyperHostName());
                s_logger.warn(msg);
                // find VM through source host (VM is not at the target host yet)
                vmMo = dcMo.findVm(vmName);
                if (vmMo == null) {
                    msg = String.format("VM: %s does not exist on datacenter: %s", vmName, dcMo.getName());
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
                // VM host has changed
                sourceHyperHost = vmMo.getRunningHost();
            }

            vmName = vmMo.getName();
            String srcHostApiVersion = ((HostMO)sourceHyperHost).getHostAboutInfo().getApiVersion();

            if (StringUtils.isNotBlank(poolUuid)) {
                VmwareHypervisorHost dsHost = targetHyperHost == null ? sourceHyperHost : targetHyperHost;
                ManagedObjectReference morDatastore = null;
                morDatastore = getTargetDatastoreMOReference(poolUuid, dsHost);
                if (morDatastore == null) {
                    String msg = String.format("Unable to find the target datastore: %s on host: %s to execute migration", poolUuid, dsHost.getHyperHostName());
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
                relocateSpec.setDatastore(morDatastore);
            } else if (CollectionUtils.isNotEmpty(volToFiler)) {
                // Specify destination datastore location for each volume
                VmwareHypervisorHost dsHost = targetHyperHost == null ? sourceHyperHost : targetHyperHost;
                for (Pair<VolumeTO, StorageFilerTO> entry : volToFiler) {
                    VolumeTO volume = entry.first();
                    StorageFilerTO filerTo = entry.second();
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(String.format("Preparing spec for volume: %s to migrate it to datastore: %s", volume.getName(), filerTo.getUuid()));
                    }
                    ManagedObjectReference morVolumeDatastore = getTargetDatastoreMOReference(filerTo.getUuid(), dsHost);
                    if (morVolumeDatastore == null) {
                        String msg = String.format("Unable to find the target datastore: %s in datacenter: %s to execute migration", filerTo.getUuid(), dcMo.getName());
                        s_logger.error(msg);
                        throw new CloudRuntimeException(msg);
                    }

                    String mountedDs = getMountedDatastoreName(sourceHyperHost, srcHostApiVersion, filerTo);
                    if (mountedDs != null) {
                        mountedDatastoresAtSource.add(mountedDs);
                    }

                    if (volume.getType() == Volume.Type.ROOT) {
                        relocateSpec.setDatastore(morVolumeDatastore);
                    }
                    VirtualMachineRelocateSpecDiskLocator diskLocator = new VirtualMachineRelocateSpecDiskLocator();
                    diskLocator.setDatastore(morVolumeDatastore);
                    Pair<VirtualDisk, String> diskInfo = getVirtualDiskInfo(vmMo, volume.getPath() + VMDK_EXTENSION);
                    String vmdkAbsFile = getAbsoluteVmdkFile(diskInfo.first());
                    if (vmdkAbsFile != null && !vmdkAbsFile.isEmpty()) {
                        vmMo.updateAdapterTypeIfRequired(vmdkAbsFile);
                    }
                    int diskId = diskInfo.first().getKey();
                    diskLocator.setDiskId(diskId);

                    diskLocators.add(diskLocator);
                    volumeDeviceKey.put(volume.getId(), diskId);
                }
                // If a target datastore is provided for the VM, then by default all volumes associated with the VM will be migrated to that target datastore.
                // Hence set the existing datastore as target datastore for volumes that are not to be migrated.
                List<Pair<Integer, ManagedObjectReference>> diskDatastores = vmMo.getAllDiskDatastores();
                for (Pair<Integer, ManagedObjectReference> diskDatastore : diskDatastores) {
                    if (!volumeDeviceKey.containsValue(diskDatastore.first().intValue())) {
                        VirtualMachineRelocateSpecDiskLocator diskLocator = new VirtualMachineRelocateSpecDiskLocator();
                        diskLocator.setDiskId(diskDatastore.first().intValue());
                        diskLocator.setDatastore(diskDatastore.second());
                        diskLocators.add(diskLocator);
                    }
                }

                relocateSpec.getDisk().addAll(diskLocators);
            }

            // Specific section for MigrateVmWithStorageCommand
            if (vmTo != null) {
                // Prepare network at target before migration
                NicTO[] nics = vmTo.getNics();
                for (NicTO nic : nics) {
                    // prepare network on the host
                    prepareNetworkFromNicInfo((HostMO)targetHyperHost, nic, false, vmTo.getType());
                }

                if (targetHyperHost == null) {
                    throw new CloudRuntimeException(String.format("Trying to relocate VM [%s], but target hyper host is null.", vmTo.getUuid()));
                }

                // Ensure secondary storage mounted on target host
                VmwareManager mgr = targetHyperHost.getContext().getStockObject(VmwareManager.CONTEXT_STOCK_NAME);
                Pair<String, Long> secStoreUrlAndId = mgr.getSecondaryStorageStoreUrlAndId(Long.parseLong(_dcId));
                String secStoreUrl = secStoreUrlAndId.first();
                Long secStoreId = secStoreUrlAndId.second();
                if (secStoreUrl == null) {
                    String msg = "secondary storage for dc " + _dcId + " is not ready yet?";
                    throw new Exception(msg);
                }
                ManagedObjectReference morSecDs = prepareSecondaryDatastoreOnSpecificHost(secStoreUrl, targetHyperHost);
                if (morSecDs == null) {
                    throw new Exception(String.format("Failed to prepare secondary storage on host, secondary store url: %s", secStoreUrl));
                }
            }

            if (srcHostApiVersion.compareTo("5.1") < 0) {
                // Migrate VM's volumes to target datastore(s).
                if (!vmMo.changeDatastore(relocateSpec)) {
                    throw new Exception("Change datastore operation failed during storage migration");
                } else {
                    s_logger.debug(String.format("Successfully migrated storage of VM: %s to target datastore(s)", vmName));
                }
                // Migrate VM to target host.
                if (targetHyperHost != null) {
                    ManagedObjectReference morPool = targetHyperHost.getHyperHostOwnerResourcePool();
                    if (!vmMo.migrate(morPool, targetHyperHost.getMor())) {
                        throw new Exception("VM migration to target host failed during storage migration");
                    } else {
                        s_logger.debug(String.format("Successfully migrated VM: %s from host %s to %s", vmName , sourceHyperHost.getHyperHostName(), targetHyperHost.getHyperHostName()));
                    }
                }
            } else {
                // Add target host to relocate spec
                if (targetHyperHost != null) {
                    relocateSpec.setHost(targetHyperHost.getMor());
                    relocateSpec.setPool(targetHyperHost.getHyperHostOwnerResourcePool());
                }
                if (!vmMo.changeDatastore(relocateSpec)) {
                    throw new Exception("Change datastore operation failed during storage migration");
                } else {
                    String msg = String.format("Successfully migrated VM: %s with its storage to target datastore(s)", vmName);
                    if (targetHyperHost != null) {
                        msg = String.format("%s from host %s to %s", msg, sourceHyperHost.getHyperHostName(), targetHyperHost.getHyperHostName());
                    }
                    s_logger.debug(msg);
                }
            }

            // Consolidate VM disks.
            // In case of a linked clone VM, if VM's disks are not consolidated, further VM operations such as volume snapshot, VM snapshot etc. will result in DB inconsistencies.
            if (!vmMo.consolidateVmDisks()) {
                s_logger.warn("VM disk consolidation failed after storage migration. Yet proceeding with VM migration.");
            } else {
                s_logger.debug(String.format("Successfully consolidated disks of VM: %s", vmName));
            }

            if (MapUtils.isNotEmpty(volumeDeviceKey)) {
                // Update and return volume path and chain info for every disk because that could have changed after migration
                VirtualMachineDiskInfoBuilder diskInfoBuilder = vmMo.getDiskInfoBuilder();
                for (Pair<VolumeTO, StorageFilerTO> entry : volToFiler) {
                    final VolumeTO volume = entry.first();
                    final long volumeId = volume.getId();
                    VirtualDisk[] disks = vmMo.getAllDiskDevice();
                    for (VirtualDisk disk : disks) {
                        if (volumeDeviceKey.get(volumeId) == disk.getKey()) {
                            VolumeObjectTO newVol = new VolumeObjectTO();
                            newVol.setDataStoreUuid(entry.second().getUuid());
                            String newPath = vmMo.getVmdkFileBaseName(disk);
                            ManagedObjectReference morDs = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(targetHyperHost != null ? targetHyperHost : sourceHyperHost, entry.second().getUuid());
                            DatastoreMO dsMo = new DatastoreMO(getServiceContext(), morDs);
                            VirtualMachineDiskInfo diskInfo = diskInfoBuilder.getDiskInfoByBackingFileBaseName(newPath, dsMo.getName());
                            newVol.setId(volumeId);
                            newVol.setPath(newPath);
                            newVol.setChainInfo(_gson.toJson(diskInfo));
                            volumeToList.add(newVol);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (e instanceof RemoteException) {
                s_logger.warn("Encountered remote exception at vCenter, invalidating VMware session context");
                invalidateServiceContext();
            }
            throw e;
        } finally {
            // Cleanup datastores mounted on source host
            for (String mountedDatastore : mountedDatastoresAtSource) {
                s_logger.debug("Attempting to unmount datastore " + mountedDatastore + " at " + sourceHyperHost.getHyperHostName());
                try {
                    sourceHyperHost.unmountDatastore(mountedDatastore);
                } catch (Exception unmountEx) {
                    s_logger.warn("Failed to unmount datastore " + mountedDatastore + " at " + sourceHyperHost.getHyperHostName() + ". Seems the datastore is still being used by " + sourceHyperHost.getHyperHostName() +
                            ". Please unmount manually to cleanup.");
                }
                s_logger.debug("Successfully unmounted datastore " + mountedDatastore + " at " + sourceHyperHost.getHyperHostName());
            }
        }

        // Only when volToFiler is not empty a filled list of VolumeObjectTO is returned else it will be empty
        return volumeToList;
    }

    private String getMountedDatastoreName(VmwareHypervisorHost sourceHyperHost, String sourceHostApiVersion, StorageFilerTO filerTo) throws Exception {
        String mountedDatastoreName = null;
        // If host version is below 5.1 then simultaneous change of VM's datastore and host is not supported.
        // So since only the datastore will be changed first, ensure the target datastore is mounted on source host.
        if (sourceHostApiVersion.compareTo("5.1") < 0) {
            s_logger.debug(String.format("Host: %s version is %s, vMotion without shared storage cannot be done. Check source host has target datastore mounted or can be mounted", sourceHyperHost.getHyperHostName(), sourceHostApiVersion));
            ManagedObjectReference morVolumeDatastoreAtSource = HypervisorHostHelper.findDatastoreWithBackwardsCompatibility(sourceHyperHost, filerTo.getUuid());
            String volumeDatastoreName = filerTo.getUuid().replace("-", "");
            String volumeDatastoreHost = filerTo.getHost();
            String volumeDatastorePath = filerTo.getPath();
            int volumeDatastorePort = filerTo.getPort();

            // If datastore is NFS and target datastore is not already mounted on source host then mount the datastore.
            if (filerTo.getType().equals(StoragePoolType.NetworkFilesystem)) {
                if (morVolumeDatastoreAtSource == null) {
                    morVolumeDatastoreAtSource = sourceHyperHost.mountDatastore(false, volumeDatastoreHost, volumeDatastorePort, volumeDatastorePath, volumeDatastoreName, false);
                    if (morVolumeDatastoreAtSource == null) {
                        throw new Exception("Unable to mount NFS datastore " + volumeDatastoreHost + ":/" + volumeDatastorePath + " on host: " + sourceHyperHost.getHyperHostName());
                    }
                    mountedDatastoreName = volumeDatastoreName;
                    s_logger.debug("Mounted NFS datastore " + volumeDatastoreHost + ":/" + volumeDatastorePath + " on host: " + sourceHyperHost.getHyperHostName());
                }
            }

            // If datastore is VMFS and target datastore is not mounted or accessible to source host then fail migration.
            if (filerTo.getType().equals(StoragePoolType.VMFS)) {
                if (morVolumeDatastoreAtSource == null) {
                    s_logger.warn("Host: " + sourceHyperHost.getHyperHostName() + " version is below 5.1, target VMFS datastore(s) need to be manually mounted on host for successful storage migration.");
                    throw new Exception("Target VMFS datastore: " + volumeDatastorePath + " is not mounted on host: " + sourceHyperHost.getHyperHostName());
                }
                DatastoreMO dsAtSourceMo = new DatastoreMO(getServiceContext(), morVolumeDatastoreAtSource);
                String srcHostValue = sourceHyperHost.getMor().getValue();
                if (!dsAtSourceMo.isAccessibleToHost(srcHostValue)) {
                    s_logger.warn("Host " + sourceHyperHost.getHyperHostName() + " version is below 5.1, target VMFS datastore(s) need to be accessible to host for a successful storage migration.");
                    throw new Exception("Target VMFS datastore: " + volumeDatastorePath + " is not accessible on host: " + sourceHyperHost.getHyperHostName());
                }
            }
        }
        return mountedDatastoreName;
    }

    private Answer execute(ValidateVcenterDetailsCommand cmd) {
        String vCenterServerAddress = cmd.getvCenterServerAddress();
        VmwareContext context = getServiceContext();

        if (vCenterServerAddress.equals(context.getServerAddress())) {
            return new Answer(cmd, true, "success");
        } else {
            return new Answer(cmd, false, "Provided vCenter server address is invalid");
        }
    }

    public String acquireVirtualMachineVncTicket(String vmInternalCSName) throws Exception {
        VmwareContext context = getServiceContext();
        VmwareHypervisorHost hyperHost = getHyperHost(context);
        DatacenterMO dcMo = new DatacenterMO(hyperHost.getContext(), hyperHost.getHyperHostDatacenter());
        VirtualMachineMO vmMo = dcMo.findVm(vmInternalCSName);
        return vmMo.acquireVncTicket();
    }

    private GetVmVncTicketAnswer execute(GetVmVncTicketCommand cmd) {
        String vmInternalName = cmd.getVmInternalName();
        s_logger.info("Getting VNC ticket for VM " + vmInternalName);
        try {
            String ticket = acquireVirtualMachineVncTicket(vmInternalName);
            boolean result = StringUtils.isNotBlank(ticket);
            return new GetVmVncTicketAnswer(ticket, result, result ? "" : "Empty ticket obtained");
        } catch (Exception e) {
            s_logger.error("Error getting VNC ticket for VM " + vmInternalName, e);
            return new GetVmVncTicketAnswer(null, false, e.getLocalizedMessage());
        }
    }

    private Integer getVmwareWindowTimeInterval() {
        Integer windowInterval = VmwareManager.VMWARE_STATS_TIME_WINDOW.value();
        if (windowInterval == null || windowInterval < 20) {
            s_logger.error(String.format("The window interval can't be [%s]. Therefore we will use the default value of [%s] seconds.", windowInterval, VmwareManager.VMWARE_STATS_TIME_WINDOW.defaultValue()));
            windowInterval = Integer.valueOf(VmwareManager.VMWARE_STATS_TIME_WINDOW.defaultValue());
        }
        return windowInterval;
    }

    @Override
    public String createLogMessageException(Throwable e, Command command) {
        if (e instanceof RemoteException) {
            s_logger.warn("Encounter remote exception to vCenter, invalidate VMware session context.");
            invalidateServiceContext();
        }

        String message = String.format("%s failed due to [%s].", command.getClass().getSimpleName(), VmwareHelper.getExceptionMessage(e));
        s_logger.error(message, e);

        return message;
    }

    private void logCommand(Command cmd) {
        try {
            s_logger.debug(String.format(EXECUTING_RESOURCE_COMMAND, cmd.getClass().getSimpleName(), _gson.toJson(cmd)));
        } catch (Exception e) {
            s_logger.error(String.format("Failed to log command %s due to: [%s].", cmd.getClass().getSimpleName(), e.getMessage()), e);
        }
    }

    @Override
    public VmwareStorageProcessor getStorageProcessor() {
        return _storageProcessor;
    }
}
