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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ChoiceOption;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ElementDescription;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestOsDescriptor;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.OvfCreateDescriptorParams;
import com.vmware.vim25.OvfCreateDescriptorResult;
import com.vmware.vim25.OvfFile;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VirtualBusLogicController;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
import com.vmware.vim25.VirtualController;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecFileOperation;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualDiskType;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualHardwareOption;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualLsiLogicSASController;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigOption;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineFileLayoutEx;
import com.vmware.vim25.VirtualMachineMessage;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineQuestionInfo;
import com.vmware.vim25.VirtualMachineRelocateDiskMoveOptions;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRelocateSpecDiskLocator;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;

import com.cloud.hypervisor.vmware.mo.SnapshotDescriptor.SnapshotInfo;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.utils.ActionDelegate;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.script.Script;

public class VirtualMachineMO extends BaseMO {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineMO.class);
    private static final ExecutorService MonitorServiceExecutor = Executors.newCachedThreadPool(new NamedThreadFactory("VM-Question-Monitor"));

    public static final String ANSWER_YES = "0";
    public static final String ANSWER_NO = "1";

    private ManagedObjectReference _vmEnvironmentBrowser = null;

    public VirtualMachineMO(VmwareContext context, ManagedObjectReference morVm) {
        super(context, morVm);
    }

    public VirtualMachineMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public Pair<DatacenterMO, String> getOwnerDatacenter() throws Exception {
        return DatacenterMO.getOwnerDatacenter(getContext(), getMor());
    }

    public Pair<DatastoreMO, String> getOwnerDatastore(String dsFullPath) throws Exception {
        String dsName = DatastoreFile.getDatastoreNameFromPath(dsFullPath);

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Datastore");
        pSpec.getPathSet().add("name");

        TraversalSpec vmDatastoreTraversal = new TraversalSpec();
        vmDatastoreTraversal.setType("VirtualMachine");
        vmDatastoreTraversal.setPath("datastore");
        vmDatastoreTraversal.setName("vmDatastoreTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(vmDatastoreTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        if (ocs != null) {
            for (ObjectContent oc : ocs) {
                DynamicProperty prop = oc.getPropSet().get(0);
                if (prop.getVal().toString().equals(dsName)) {
                    return new Pair<DatastoreMO, String>(new DatastoreMO(_context, oc.getObj()), dsName);
                }
            }
        }

        return null;
    }

    public HostMO getRunningHost() throws Exception {
        VirtualMachineRuntimeInfo runtimeInfo = getRuntimeInfo();
        return new HostMO(_context, runtimeInfo.getHost());
    }

    public String getVmName() throws Exception {
        return (String)getContext().getVimClient().getDynamicProperty(_mor, "name");
    }

    public GuestInfo getVmGuestInfo() throws Exception {
        return (GuestInfo)getContext().getVimClient().getDynamicProperty(_mor, "guest");
    }

    public void answerVM(String questionId, String choice) throws Exception {
        getContext().getService().answerVM(_mor, questionId, choice);
    }

    public boolean isVMwareToolsRunning() throws Exception {
        GuestInfo guestInfo = getVmGuestInfo();
        if (guestInfo != null) {
            if ("guestToolsRunning".equalsIgnoreCase(guestInfo.getToolsRunningStatus()))
                return true;
        }
        return false;
    }

    public boolean powerOn() throws Exception {
        if (getResetSafePowerState() == VirtualMachinePowerState.POWERED_ON)
            return true;

        ManagedObjectReference morTask = _context.getService().powerOnVMTask(_mor, null);
        // Monitor VM questions
        final Boolean[] flags = {false};
        final VirtualMachineMO vmMo = this;
        Future<?> future = MonitorServiceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                s_logger.info("VM Question monitor started...");

                while (!flags[0]) {
                    try {
                        VirtualMachineRuntimeInfo runtimeInfo = vmMo.getRuntimeInfo();
                        VirtualMachineQuestionInfo question = runtimeInfo.getQuestion();
                        if (question != null) {
                            s_logger.info("Question id: " + question.getId());
                            s_logger.info("Question text: " + question.getText());
                            if (question.getMessage() != null) {
                                for (VirtualMachineMessage msg : question.getMessage()) {
                                    if (s_logger.isInfoEnabled()) {
                                        s_logger.info("msg id: " + msg.getId());
                                        s_logger.info("msg text: " + msg.getText());
                                    }
                                    if ("msg.uuid.altered".equalsIgnoreCase(msg.getId())) {
                                        s_logger.info("Found that VM has a pending question that we need to answer programmatically, question id: " + msg.getId()
                                                + ", we will automatically answer as 'moved it' to address out of band HA for the VM");
                                        vmMo.answerVM(question.getId(), "1");
                                        break;
                                    }
                                }
                            }

                            if (s_logger.isTraceEnabled())
                                s_logger.trace("These are the choices we can have just in case");
                            ChoiceOption choice = question.getChoice();
                            if (choice != null) {
                                for (ElementDescription info : choice.getChoiceInfo()) {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("Choice option key: " + info.getKey());
                                        s_logger.trace("Choice option label: " + info.getLabel());
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        s_logger.error("Unexpected exception: ", e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        s_logger.debug("[ignored] interupted while dealing with vm questions.");
                    }
                }
                s_logger.info("VM Question monitor stopped");
            }
        });

        try {
            boolean result = _context.getVimClient().waitForTask(morTask);
            if (result) {
                _context.waitForTaskProgressDone(morTask);
                return true;
            } else {
                s_logger.error("VMware powerOnVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }
        } finally {
            // make sure to let VM question monitor exit
            flags[0] = true;
        }

        return false;
    }

    public boolean powerOff() throws Exception {
        if (getResetSafePowerState() == VirtualMachinePowerState.POWERED_OFF)
            return true;

        return powerOffNoCheck();
    }

    public boolean safePowerOff(int shutdownWaitMs) throws Exception {

        if (getResetSafePowerState() == VirtualMachinePowerState.POWERED_OFF)
            return true;

        if (isVMwareToolsRunning()) {
            try {
                String vmName = getName();

                s_logger.info("Try gracefully shut down VM " + vmName);
                shutdown();

                long startTick = System.currentTimeMillis();
                while (getResetSafePowerState() != VirtualMachinePowerState.POWERED_OFF && System.currentTimeMillis() - startTick < shutdownWaitMs) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        s_logger.debug("[ignored] interupted while powering of vm.");
                    }
                }

                if (getResetSafePowerState() != VirtualMachinePowerState.POWERED_OFF) {
                    s_logger.info("can not gracefully shutdown VM within " + (shutdownWaitMs / 1000) + " seconds, we will perform force power off on VM " + vmName);
                    return powerOffNoCheck();
                }

                return true;
            } catch (Exception e) {
                s_logger.warn("Failed to do guest-os graceful shutdown due to " + VmwareHelper.getExceptionMessage(e));
            }
        }

        return powerOffNoCheck();
    }

    private boolean powerOffNoCheck() throws Exception {
        ManagedObjectReference morTask = _context.getService().powerOffVMTask(_mor);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);

            // It seems that even if a power-off task is returned done, VM state may still not be marked,
            // wait up to 5 seconds to make sure to avoid race conditioning for immediate following on operations
            // that relies on a powered-off VM
            long startTick = System.currentTimeMillis();
            while (getResetSafePowerState() != VirtualMachinePowerState.POWERED_OFF && System.currentTimeMillis() - startTick < 5000) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    s_logger.debug("[ignored] interupted while powering of vm unconditionaly.");
                }
            }
            return true;
        } else {
            if (getResetSafePowerState() == VirtualMachinePowerState.POWERED_OFF) {
                // to help deal with possible race-condition
                s_logger.info("Current power-off task failed. However, VM has been switched to the state we are expecting for");
                return true;
            }

            s_logger.error("VMware powerOffVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public VirtualMachinePowerState getResetSafePowerState() throws Exception {

        VirtualMachinePowerState powerState = VirtualMachinePowerState.POWERED_OFF;

        // This is really ugly, there is a case that when windows guest VM is doing sysprep, the temporary
        // rebooting process may let us pick up a "poweredOff" state during VMsync process, this can trigger
        // a series actions. Unfortunately, from VMware API we can not distinguish power state into such details.
        // We hope by giving it 3 second to re-read the state can cover this as a short-term solution.
        //
        // In the future, VMsync should not kick off CloudStack action (this is not a HA case) based on VM
        // state report, until then we can remove this hacking fix
        for (int i = 0; i < 3; i++) {
            powerState = (VirtualMachinePowerState)getContext().getVimClient().getDynamicProperty(_mor, "runtime.powerState");
            if (powerState == VirtualMachinePowerState.POWERED_OFF) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    s_logger.debug("[ignored] interupted while pausing after power off.");
                }
            } else {
                break;
            }
        }

        return powerState;
    }

    public VirtualMachinePowerState getPowerState() throws Exception {
        return (VirtualMachinePowerState)getContext().getVimClient().getDynamicProperty(_mor, "runtime.powerState");
    }

    public boolean reset() throws Exception {
        ManagedObjectReference morTask = _context.getService().resetVMTask(_mor);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware resetVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public void shutdown() throws Exception {
        _context.getService().shutdownGuest(_mor);
    }

    public void rebootGuest() throws Exception {
        _context.getService().rebootGuest(_mor);
    }

    public void markAsTemplate() throws Exception {
        _context.getService().markAsTemplate(_mor);
    }

    public boolean isTemplate() throws Exception {
        VirtualMachineConfigInfo configInfo = getConfigInfo();
        return configInfo.isTemplate();
    }

    public boolean migrate(ManagedObjectReference morRp, ManagedObjectReference morTargetHost) throws Exception {
        ManagedObjectReference morTask = _context.getService().migrateVMTask(_mor, morRp, morTargetHost, VirtualMachineMovePriority.DEFAULT_PRIORITY, null);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware migrateVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public boolean changeDatastore(VirtualMachineRelocateSpec relocateSpec) throws Exception {
        ManagedObjectReference morTask = _context.getVimClient().getService().relocateVMTask(_mor, relocateSpec, VirtualMachineMovePriority.DEFAULT_PRIORITY);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware RelocateVM_Task to change datastore failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public boolean changeHost(VirtualMachineRelocateSpec relocateSpec) throws Exception {
        ManagedObjectReference morTask = _context.getService().relocateVMTask(_mor, relocateSpec, VirtualMachineMovePriority.DEFAULT_PRIORITY);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware RelocateVM_Task to change host failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public boolean changeDatastore(ManagedObjectReference morDataStore) throws Exception {
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.setDatastore(morDataStore);

        ManagedObjectReference morTask = _context.getService().relocateVMTask(_mor, relocateSpec, null);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware change datastore relocateVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public boolean relocate(ManagedObjectReference morTargetHost) throws Exception {
        VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
        relocateSpec.setHost(morTargetHost);

        ManagedObjectReference morTask = _context.getService().relocateVMTask(_mor, relocateSpec, null);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware relocateVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public VirtualMachineSnapshotInfo getSnapshotInfo() throws Exception {
        return (VirtualMachineSnapshotInfo)_context.getVimClient().getDynamicProperty(_mor, "snapshot");
    }

    public boolean createSnapshot(String snapshotName, String snapshotDescription, boolean dumpMemory, boolean quiesce) throws Exception {
        return createSnapshotGetReference(snapshotName, snapshotDescription, dumpMemory, quiesce) != null;
    }

    public ManagedObjectReference createSnapshotGetReference(String snapshotName, String snapshotDescription, boolean dumpMemory, boolean quiesce) throws Exception {
        long apiTimeout = _context.getVimClient().getVcenterSessionTimeout();
        ManagedObjectReference morTask = _context.getService().createSnapshotTask(_mor, snapshotName, snapshotDescription, dumpMemory, quiesce);

        boolean result = _context.getVimClient().waitForTask(morTask);

        if (result) {
            _context.waitForTaskProgressDone(morTask);

            ManagedObjectReference morSnapshot = null;

            // We still need to wait until the object appear in vCenter
            long startTick = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTick < apiTimeout) {
                morSnapshot = getSnapshotMor(snapshotName);

                if (morSnapshot != null) {
                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    s_logger.debug("[ignored] interupted while waiting for snapshot to be done.");
                }
            }

            if (morSnapshot == null) {
                s_logger.error("We've been waiting for over " + apiTimeout + " milli seconds for snapshot MOR to be appearing in vCenter after CreateSnapshot task is done, " +
                        "but it is still not there?!");

                return null;
            }

            s_logger.debug("Waited for " + (System.currentTimeMillis() - startTick) + " seconds for snapshot object [" + snapshotName + "] to appear in vCenter.");

            return morSnapshot;
        } else {
            s_logger.error("VMware createSnapshot_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return null;
    }

    public boolean removeSnapshot(String snapshotName, boolean removeChildren) throws Exception {
        ManagedObjectReference morSnapshot = getSnapshotMor(snapshotName);
        if (morSnapshot == null) {
            s_logger.warn("Unable to find snapshot: " + snapshotName);
            return false;
        }

        ManagedObjectReference morTask = _context.getService().removeSnapshotTask(morSnapshot, removeChildren, true);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware removeSnapshot_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public boolean revertToSnapshot(String snapshotName) throws Exception {
        ManagedObjectReference morSnapshot = getSnapshotMor(snapshotName);
        if (morSnapshot == null) {
            s_logger.warn("Unable to find snapshot: " + snapshotName);
            return false;
        }
        ManagedObjectReference morTask = _context.getService().revertToSnapshotTask(morSnapshot, _mor, null);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware revert to snapshot failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    /**
     * Deletes all of the snapshots of a VM.
     */
    public void consolidateAllSnapshots() throws Exception {
        ManagedObjectReference task = _context.getService().removeAllSnapshotsTask(_mor, true);

        boolean result = _context.getVimClient().waitForTask(task);

        if (result) {
            _context.waitForTaskProgressDone(task);
        } else {
            throw new Exception("Unable to register VM due to the following issue: " + TaskMO.getTaskFailureInfo(_context, task));
        }
    }

    public boolean removeAllSnapshots() throws Exception {
        VirtualMachineSnapshotInfo snapshotInfo = getSnapshotInfo();

        if (snapshotInfo != null && snapshotInfo.getRootSnapshotList() != null) {
            List<VirtualMachineSnapshotTree> tree = snapshotInfo.getRootSnapshotList();
            for (VirtualMachineSnapshotTree treeNode : tree) {
                ManagedObjectReference morTask = _context.getService().removeSnapshotTask(treeNode.getSnapshot(), true, true);
                boolean result = _context.getVimClient().waitForTask(morTask);
                if (result) {
                    _context.waitForTaskProgressDone(morTask);
                } else {
                    s_logger.error("VMware removeSnapshot_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
                    return false;
                }
            }
        }

        return true;
    }

    public String
    getSnapshotDiskFileDatastorePath(VirtualMachineFileInfo vmFileInfo, List<Pair<ManagedObjectReference, String>> datastoreMounts, String snapshotDiskFile)
            throws Exception {

        // if file path start with "/", need to search all datastore mounts on the host in order
        // to form fully qualified datastore path
        if (snapshotDiskFile.startsWith("/")) {
            for (Pair<ManagedObjectReference, String> mount : datastoreMounts) {
                if (snapshotDiskFile.startsWith(mount.second())) {
                    DatastoreMO dsMo = new DatastoreMO(_context, mount.first());

                    String dsFullPath = String.format("[%s] %s", dsMo.getName(), snapshotDiskFile.substring(mount.second().length() + 1));
                    s_logger.info("Convert snapshot disk file name to datastore path. " + snapshotDiskFile + "->" + dsFullPath);
                    return dsFullPath;
                }
            }

            s_logger.info("Convert snapshot disk file name to datastore path. " + snapshotDiskFile + "->" + snapshotDiskFile);
            return snapshotDiskFile;
        } else {

            // snapshot directory string from VirtualMachineFileInfo ends with /
            String dsFullPath = vmFileInfo.getSnapshotDirectory() + snapshotDiskFile;
            s_logger.info("Convert snapshot disk file name to datastore path. " + snapshotDiskFile + "->" + dsFullPath);
            return dsFullPath;
        }
    }

    public SnapshotDescriptor getSnapshotDescriptor() throws Exception {

        Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();

        String dsPath = getSnapshotDescriptorDatastorePath();
        assert (dsPath != null);
        String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), dsPath);
        byte[] content = getContext().getResourceContent(url);

        if (content == null || content.length < 1) {
            s_logger.warn("Snapshot descriptor file (vsd) does not exist anymore?");
        }

        SnapshotDescriptor descriptor = new SnapshotDescriptor();
        descriptor.parse(content);
        return descriptor;
    }

    public String getSnapshotDescriptorDatastorePath() throws Exception {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("VirtualMachine");
        pSpec.getPathSet().add("name");
        pSpec.getPathSet().add("config.files");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.FALSE);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);
        assert (ocs != null);

        String vmName = null;
        VirtualMachineFileInfo fileInfo = null;

        assert (ocs.size() == 1);
        for (ObjectContent oc : ocs) {
            List<DynamicProperty> props = oc.getPropSet();
            if (props != null) {
                assert (props.size() == 2);

                for (DynamicProperty prop : props) {
                    if (prop.getName().equals("name")) {
                        vmName = prop.getVal().toString();
                    } else {
                        fileInfo = (VirtualMachineFileInfo)prop.getVal();
                    }
                }
            }
        }
        assert (vmName != null);
        assert (fileInfo != null);

        // .vmsd file exists at the same directory of .vmx file
        DatastoreFile vmxFile = new DatastoreFile(fileInfo.getVmPathName());
        return vmxFile.getCompanionPath(vmName + ".vmsd");
    }

    public ManagedObjectReference getSnapshotMor(String snapshotName) throws Exception {
        VirtualMachineSnapshotInfo info = getSnapshotInfo();
        if (info != null) {
            List<VirtualMachineSnapshotTree> snapTree = info.getRootSnapshotList();
            return VmwareHelper.findSnapshotInTree(snapTree, snapshotName);
        }
        return null;
    }

    public boolean hasSnapshot() throws Exception {
        VirtualMachineSnapshotInfo info = getSnapshotInfo();
        if (info != null) {
            ManagedObjectReference currentSnapshot = info.getCurrentSnapshot();
            if (currentSnapshot != null) {
                return true;
            }
            List<VirtualMachineSnapshotTree> rootSnapshotList = info.getRootSnapshotList();
            if (rootSnapshotList != null && rootSnapshotList.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean createFullClone(String cloneName, ManagedObjectReference morFolder, ManagedObjectReference morResourcePool, ManagedObjectReference morDs)
            throws Exception {

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        VirtualMachineRelocateSpec relocSpec = new VirtualMachineRelocateSpec();
        cloneSpec.setLocation(relocSpec);
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);

        relocSpec.setDatastore(morDs);
        relocSpec.setPool(morResourcePool);
        ManagedObjectReference morTask = _context.getService().cloneVMTask(_mor, morFolder, cloneName, cloneSpec);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware cloneVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public boolean createLinkedClone(String cloneName, ManagedObjectReference morBaseSnapshot, ManagedObjectReference morFolder, ManagedObjectReference morResourcePool,
            ManagedObjectReference morDs) throws Exception {

        assert (morBaseSnapshot != null);
        assert (morFolder != null);
        assert (morResourcePool != null);
        assert (morDs != null);

        VirtualDisk[] independentDisks = getAllIndependentDiskDevice();
        VirtualMachineRelocateSpec rSpec = new VirtualMachineRelocateSpec();
        if (independentDisks.length > 0) {
            List<VirtualMachineRelocateSpecDiskLocator> diskLocator = new ArrayList<VirtualMachineRelocateSpecDiskLocator>(independentDisks.length);
            for (int i = 0; i < independentDisks.length; i++) {
                VirtualMachineRelocateSpecDiskLocator loc = new VirtualMachineRelocateSpecDiskLocator();
                loc.setDatastore(morDs);
                loc.setDiskId(independentDisks[i].getKey());
                loc.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions.MOVE_ALL_DISK_BACKINGS_AND_DISALLOW_SHARING.value());
                diskLocator.add(loc);
            }

            rSpec.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions.CREATE_NEW_CHILD_DISK_BACKING.value());
            rSpec.getDisk().addAll(diskLocator);
        } else {
            rSpec.setDiskMoveType(VirtualMachineRelocateDiskMoveOptions.CREATE_NEW_CHILD_DISK_BACKING.value());
        }
        rSpec.setPool(morResourcePool);

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);
        cloneSpec.setLocation(rSpec);
        cloneSpec.setSnapshot(morBaseSnapshot);

        ManagedObjectReference morTask = _context.getService().cloneVMTask(_mor, morFolder, cloneName, cloneSpec);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware cloneVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

    public VirtualMachineRuntimeInfo getRuntimeInfo() throws Exception {
        return (VirtualMachineRuntimeInfo)_context.getVimClient().getDynamicProperty(_mor, "runtime");
    }

    public VirtualMachineConfigInfo getConfigInfo() throws Exception {
        return (VirtualMachineConfigInfo)_context.getVimClient().getDynamicProperty(_mor, "config");
    }

    public boolean isToolsInstallerMounted() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "runtime.toolsInstallerMounted");
    }
    public GuestInfo getGuestInfo() throws Exception {
        return (GuestInfo)_context.getVimClient().getDynamicProperty(_mor, "guest");
    }

    public VirtualMachineConfigSummary getConfigSummary() throws Exception {
        return (VirtualMachineConfigSummary)_context.getVimClient().getDynamicProperty(_mor, "summary.config");
    }

    public VirtualMachineFileInfo getFileInfo() throws Exception {
        return (VirtualMachineFileInfo)_context.getVimClient().getDynamicProperty(_mor, "config.files");
    }

    public VirtualMachineFileLayoutEx getFileLayout() throws Exception {
        VirtualMachineFileLayoutEx fileLayout = null;
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("VirtualMachine");
        pSpec.getPathSet().add("layoutEx");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.FALSE);

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
                        if (prop.getName().equals("layoutEx")) {
                            fileLayout = (VirtualMachineFileLayoutEx)prop.getVal();
                            break;
                        }
                    }
                }
            }
        }

        return fileLayout;
    }

    @Override
    public ManagedObjectReference getParentMor() throws Exception {
        return (ManagedObjectReference)_context.getVimClient().getDynamicProperty(_mor, "parent");
    }

    public String[] getNetworks() throws Exception {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Network");
        pSpec.getPathSet().add("name");

        TraversalSpec vm2NetworkTraversal = new TraversalSpec();
        vm2NetworkTraversal.setType("VirtualMachine");
        vm2NetworkTraversal.setPath("network");
        vm2NetworkTraversal.setName("vm2NetworkTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(vm2NetworkTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        List<String> networks = new ArrayList<String>();
        if (ocs != null && ocs.size() > 0) {
            for (ObjectContent oc : ocs) {
                networks.add(oc.getPropSet().get(0).getVal().toString());
            }
        }
        return networks.toArray(new String[0]);
    }

    public List<NetworkDetails> getNetworksWithDetails() throws Exception {
        List<NetworkDetails> networks = new ArrayList<NetworkDetails>();

        int gcTagKey = getCustomFieldKey("Network", CustomFieldConstants.CLOUD_GC);

        if (gcTagKey == 0) {
            gcTagKey = getCustomFieldKey("DistributedVirtualPortgroup", CustomFieldConstants.CLOUD_GC_DVP);
            s_logger.debug("The custom key for dvPortGroup is : " + gcTagKey);
        }

        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Network");
        pSpec.getPathSet().add("name");
        pSpec.getPathSet().add("vm");
        pSpec.getPathSet().add(String.format("value[%d]", gcTagKey));

        TraversalSpec vm2NetworkTraversal = new TraversalSpec();
        vm2NetworkTraversal.setType("VirtualMachine");
        vm2NetworkTraversal.setPath("network");
        vm2NetworkTraversal.setName("vm2NetworkTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(vm2NetworkTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        if (ocs != null && ocs.size() > 0) {
            for (ObjectContent oc : ocs) {
                ArrayOfManagedObjectReference morVms = null;
                String gcTagValue = null;
                String name = null;

                for (DynamicProperty prop : oc.getPropSet()) {
                    if (prop.getName().equals("name"))
                        name = prop.getVal().toString();
                    else if (prop.getName().equals("vm"))
                        morVms = (ArrayOfManagedObjectReference)prop.getVal();
                    else if (prop.getName().startsWith("value[")) {
                        CustomFieldStringValue val = (CustomFieldStringValue)prop.getVal();
                        if (val != null)
                            gcTagValue = val.getValue();
                    }
                }

                NetworkDetails details =
                        new NetworkDetails(name, oc.getObj(), (morVms != null ? morVms.getManagedObjectReference().toArray(
                                new ManagedObjectReference[morVms.getManagedObjectReference().size()]) : null), gcTagValue);

                networks.add(details);
            }
            s_logger.debug("Retrieved " + networks.size() + " networks with key : " + gcTagKey);
        }

        return networks;
    }

    public List<DatastoreMO> getAllDatastores() throws Exception {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType("Datastore");
        pSpec.getPathSet().add("name");

        TraversalSpec vmDatastoreTraversal = new TraversalSpec();
        vmDatastoreTraversal.setType("VirtualMachine");
        vmDatastoreTraversal.setPath("datastore");
        vmDatastoreTraversal.setName("vmDatastoreTraversal");

        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setObj(_mor);
        oSpec.setSkip(Boolean.TRUE);
        oSpec.getSelectSet().add(vmDatastoreTraversal);

        PropertyFilterSpec pfSpec = new PropertyFilterSpec();
        pfSpec.getPropSet().add(pSpec);
        pfSpec.getObjectSet().add(oSpec);
        List<PropertyFilterSpec> pfSpecArr = new ArrayList<PropertyFilterSpec>();
        pfSpecArr.add(pfSpec);

        List<ObjectContent> ocs = _context.getService().retrieveProperties(_context.getPropertyCollector(), pfSpecArr);

        List<DatastoreMO> datastores = new ArrayList<DatastoreMO>();
        if (CollectionUtils.isNotEmpty(ocs)) {
            for (ObjectContent oc : ocs) {
                datastores.add(new DatastoreMO(_context, oc.getObj()));
            }
        }
        return datastores;
    }

    /**
     * Retrieve path info to access VM files via vSphere web interface
     * @return [0] vm-name, [1] data-center-name, [2] datastore-name
     * @throws Exception
     */
    public String[] getHttpAccessPathInfo() throws Exception {
        String[] pathInfo = new String[3];

        Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();

        VirtualMachineFileInfo fileInfo = getFileInfo();
        String vmxFilePath = fileInfo.getVmPathName();
        String vmxPathTokens[] = vmxFilePath.split("\\[|\\]|/");
        assert (vmxPathTokens.length == 4);
        pathInfo[1] = vmxPathTokens[1].trim();                            // vSphere vm name
        pathInfo[2] = dcInfo.second();                                    // vSphere datacenter name
        pathInfo[3] = vmxPathTokens[0].trim();                            // vSphere datastore name
        return pathInfo;
    }

    public String getVmxHttpAccessUrl() throws Exception {
        Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();

        VirtualMachineFileInfo fileInfo = getFileInfo();
        String vmxFilePath = fileInfo.getVmPathName();
        String vmxPathTokens[] = vmxFilePath.split("\\[|\\]|/");

        StringBuffer sb = new StringBuffer("https://" + _context.getServerAddress() + "/folder/");
        sb.append(URLEncoder.encode(vmxPathTokens[2].trim(), "UTF-8"));
        sb.append("/");
        sb.append(URLEncoder.encode(vmxPathTokens[3].trim(), "UTF-8"));
        sb.append("?dcPath=");
        sb.append(URLEncoder.encode(dcInfo.second(), "UTF-8"));
        sb.append("&dsName=");
        sb.append(URLEncoder.encode(vmxPathTokens[1].trim(), "UTF-8"));

        return sb.toString();
    }

    public boolean setVncConfigInfo(boolean enableVnc, String vncPassword, int vncPort, String keyboard) throws Exception {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        OptionValue[] vncOptions = VmwareHelper.composeVncOptions(null, enableVnc, vncPassword, vncPort, keyboard);
        vmConfigSpec.getExtraConfig().addAll(Arrays.asList(vncOptions));
        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, vmConfigSpec);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware reconfigVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public boolean configureVm(VirtualMachineConfigSpec vmConfigSpec) throws Exception {
        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, vmConfigSpec);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware reconfigVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public boolean configureVm(Ternary<VirtualDevice, VirtualDeviceConfigSpecOperation, VirtualDeviceConfigSpecFileOperation>[] devices) throws Exception {

        assert (devices != null);

        VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[devices.length];
        int i = 0;
        for (Ternary<VirtualDevice, VirtualDeviceConfigSpecOperation, VirtualDeviceConfigSpecFileOperation> deviceTernary : devices) {
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
            deviceConfigSpec.setDevice(deviceTernary.first());
            deviceConfigSpec.setOperation(deviceTernary.second());
            deviceConfigSpec.setFileOperation(deviceTernary.third());
            deviceConfigSpecArray[i++] = deviceConfigSpec;
        }
        configSpec.getDeviceChange().addAll(Arrays.asList(deviceConfigSpecArray));

        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, configSpec);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware reconfigVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }

    public Pair<String, Integer> getVncPort(String hostNetworkName) throws Exception {
        HostMO hostMo = getRunningHost();
        VmwareHypervisorHostNetworkSummary summary = hostMo.getHyperHostNetworkSummary(hostNetworkName);

        VirtualMachineConfigInfo configInfo = getConfigInfo();
        List<OptionValue> values = configInfo.getExtraConfig();

        if (values != null) {
            for (OptionValue option : values) {
                if (option.getKey().equals("RemoteDisplay.vnc.port")) {
                    String value = (String)option.getValue();
                    if (value != null) {
                        return new Pair<String, Integer>(summary.getHostIp(), Integer.parseInt(value));
                    }
                }
            }
        }
        return new Pair<String, Integer>(summary.getHostIp(), 0);
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath
    public void createDisk(String vmdkDatastorePath, int sizeInMb, ManagedObjectReference morDs, int controllerKey) throws Exception {
        createDisk(vmdkDatastorePath, VirtualDiskType.THIN, VirtualDiskMode.PERSISTENT, null, sizeInMb, morDs, controllerKey);
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath
    public void createDisk(String vmdkDatastorePath, VirtualDiskType diskType, VirtualDiskMode diskMode, String rdmDeviceName, int sizeInMb,
            ManagedObjectReference morDs, int controllerKey) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: " + vmdkDatastorePath + ", sizeInMb: " + sizeInMb +
                    ", diskType: " + diskType + ", diskMode: " + diskMode + ", rdmDeviceName: " + rdmDeviceName + ", datastore: " + morDs.getValue() + ", controllerKey: " +
                    controllerKey);

        assert (vmdkDatastorePath != null);
        assert (morDs != null);

        int ideControllerKey = getIDEDeviceControllerKey();
        if (controllerKey < 0) {
            controllerKey = ideControllerKey;
        }

        VirtualDisk newDisk = new VirtualDisk();
        if (diskType == VirtualDiskType.THIN || diskType == VirtualDiskType.PREALLOCATED || diskType == VirtualDiskType.EAGER_ZEROED_THICK) {

            VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
            backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
            if (diskType == VirtualDiskType.THIN) {
                backingInfo.setThinProvisioned(true);
            } else {
                backingInfo.setThinProvisioned(false);
            }

            if (diskType == VirtualDiskType.EAGER_ZEROED_THICK) {
                backingInfo.setEagerlyScrub(true);
            } else {
                backingInfo.setEagerlyScrub(false);
            }

            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            newDisk.setBacking(backingInfo);
        } else if (diskType == VirtualDiskType.RDM || diskType == VirtualDiskType.RDMP) {
            VirtualDiskRawDiskMappingVer1BackingInfo backingInfo = new VirtualDiskRawDiskMappingVer1BackingInfo();
            if (diskType == VirtualDiskType.RDM) {
                backingInfo.setCompatibilityMode("virtualMode");
            } else {
                backingInfo.setCompatibilityMode("physicalMode");
            }
            backingInfo.setDeviceName(rdmDeviceName);
            if (diskType == VirtualDiskType.RDM) {
                backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
            }

            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            newDisk.setBacking(backingInfo);
        }

        int deviceNumber = getNextDeviceNumber(controllerKey);

        newDisk.setControllerKey(controllerKey);
        newDisk.setKey(-deviceNumber);
        newDisk.setUnitNumber(deviceNumber);
        newDisk.setCapacityInKB(sizeInMb * 1024);

        VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

        deviceConfigSpec.setDevice(newDisk);
        deviceConfigSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        reConfigSpec.getDeviceChange().add(deviceConfigSpec);

        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
        boolean result = _context.getVimClient().waitForTask(morTask);

        if (!result) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - createDisk() done(failed)");
            throw new Exception("Unable to create disk " + vmdkDatastorePath + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        _context.waitForTaskProgressDone(morTask);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - createDisk() done(successfully)");
    }

    public void updateVmdkAdapter(String vmdkFileName, String newAdapterType) throws Exception {
        Pair<VmdkFileDescriptor, byte[]> vmdkInfo = getVmdkFileInfo(vmdkFileName);
        VmdkFileDescriptor vmdkFileDescriptor = vmdkInfo.first();
        boolean isVmfsSparseFile = vmdkFileDescriptor.isVmfsSparseFile();
        if (!isVmfsSparseFile) {
            String currentAdapterType = vmdkFileDescriptor.getAdapterType();
            if (!currentAdapterType.equalsIgnoreCase(newAdapterType)) {
                s_logger.info("Updating adapter type to " + newAdapterType + " for VMDK file " + vmdkFileName);
                Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();
                byte[] newVmdkContent = vmdkFileDescriptor.changeVmdkAdapterType(vmdkInfo.second(), newAdapterType);
                String vmdkUploadUrl = getContext().composeDatastoreBrowseUrl(dcInfo.first().getName(), vmdkFileName);
                getContext().uploadResourceContent(vmdkUploadUrl, newVmdkContent);
                s_logger.info("Updated VMDK file " + vmdkFileName);
            }
        }
    }

    public void updateAdapterTypeIfRequired(String vmdkFileName) throws Exception {
        // Validate existing adapter type of VMDK file. Update it with a supported adapter type if validation fails.
        Pair<VmdkFileDescriptor, byte[]> vmdkInfo = getVmdkFileInfo(vmdkFileName);
        VmdkFileDescriptor vmdkFileDescriptor = vmdkInfo.first();

        boolean isVmfsSparseFile = vmdkFileDescriptor.isVmfsSparseFile();
        if (!isVmfsSparseFile) {
            String currentAdapterTypeStr = vmdkFileDescriptor.getAdapterType();
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Detected adapter type  " + currentAdapterTypeStr + " for VMDK file " + vmdkFileName);
            }
            VmdkAdapterType currentAdapterType = VmdkAdapterType.getType(currentAdapterTypeStr);
            if (currentAdapterType == VmdkAdapterType.none) {
                // Value of currentAdapterType can be VmdkAdapterType.none only if adapter type of vmdk is set to either
                // lsisas1068 (SAS controller) or pvscsi (Vmware Paravirtual) only. Valid adapter type for those controllers is lsilogic.
                // Hence use adapter type lsilogic. Other adapter types ide, lsilogic, buslogic are valid and does not need to be modified.
                VmdkAdapterType newAdapterType = VmdkAdapterType.lsilogic;
                s_logger.debug("Updating adapter type to " + newAdapterType + " from " + currentAdapterTypeStr + " for VMDK file " + vmdkFileName);
                Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();
                byte[] newVmdkContent = vmdkFileDescriptor.changeVmdkAdapterType(vmdkInfo.second(), newAdapterType.toString());
                String vmdkUploadUrl = getContext().composeDatastoreBrowseUrl(dcInfo.first().getName(), vmdkFileName);

                getContext().uploadResourceContent(vmdkUploadUrl, newVmdkContent);
                s_logger.debug("Updated VMDK file " + vmdkFileName);
            }
        }
    }

    public void attachDisk(String[] vmdkDatastorePathChain, ManagedObjectReference morDs, String diskController) throws Exception {

        if(s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: "
                            + new Gson().toJson(vmdkDatastorePathChain) + ", datastore: " + morDs.getValue());
        int controllerKey = 0;
        int unitNumber = 0;

        if (DiskControllerType.getType(diskController) == DiskControllerType.ide) {
            // IDE virtual disk cannot be added if VM is running
            if (getPowerState() == VirtualMachinePowerState.POWERED_ON) {
                throw new Exception("Adding a virtual disk over IDE controller is not supported while VM is running in VMware hypervisor. Please re-try when VM is not running.");
            }
            // Get next available unit number and controller key
            int ideDeviceCount = getNumberOfIDEDevices();
            if (ideDeviceCount >= VmwareHelper.MAX_IDE_CONTROLLER_COUNT * VmwareHelper.MAX_ALLOWED_DEVICES_IDE_CONTROLLER) {
                throw new Exception("Maximum limit of  devices supported on IDE controllers [" + VmwareHelper.MAX_IDE_CONTROLLER_COUNT
                        * VmwareHelper.MAX_ALLOWED_DEVICES_IDE_CONTROLLER + "] is reached.");
            }
            controllerKey = getIDEControllerKey(ideDeviceCount);
            unitNumber = getFreeUnitNumberOnIDEController(controllerKey);
        } else {
            controllerKey = getScsiDiskControllerKey(diskController);
            unitNumber = -1;
        }
        synchronized (_mor.getValue().intern()) {
            VirtualDevice newDisk = VmwareHelper.prepareDiskDevice(this, null, controllerKey, vmdkDatastorePathChain, morDs, unitNumber, 1);
            controllerKey = newDisk.getControllerKey();
            unitNumber = newDisk.getUnitNumber();
            VirtualDiskFlatVer2BackingInfo backingInfo = (VirtualDiskFlatVer2BackingInfo)newDisk.getBacking();
            String vmdkFileName = backingInfo.getFileName();
            DiskControllerType diskControllerType = DiskControllerType.getType(diskController);
            VmdkAdapterType vmdkAdapterType = VmdkAdapterType.getAdapterType(diskControllerType);
            if (vmdkAdapterType == VmdkAdapterType.none) {
                String message = "Failed to attach disk due to invalid vmdk adapter type for vmdk file [" +
                    vmdkFileName + "] with controller : " + diskControllerType;
                s_logger.debug(message);
                throw new Exception(message);
            }
            updateVmdkAdapter(vmdkFileName, vmdkAdapterType.toString());
            VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

            deviceConfigSpec.setDevice(newDisk);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            reConfigSpec.getDeviceChange().add(deviceConfigSpec);

            ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
            boolean result = _context.getVimClient().waitForTask(morTask);

            if (!result) {
                if (s_logger.isTraceEnabled())
                    s_logger.trace("vCenter API trace - attachDisk() done(failed)");
                throw new Exception("Failed to attach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }

            _context.waitForTaskProgressDone(morTask);
        }

        if(s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachDisk() done(successfully)");
    }

    private int getControllerBusNumber(int controllerKey) throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualController && device.getKey() == controllerKey) {
                    return ((VirtualController)device).getBusNumber();
                }
            }
        }
        throw new Exception("SCSI Controller with key " + controllerKey + " is Not Found");

    }

    public void attachDisk(String[] vmdkDatastorePathChain, ManagedObjectReference morDs) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: " + new Gson().toJson(vmdkDatastorePathChain) +
                    ", datastore: " + morDs.getValue());

        synchronized (_mor.getValue().intern()) {
            VirtualDevice newDisk = VmwareHelper.prepareDiskDevice(this, null, getScsiDeviceControllerKey(), vmdkDatastorePathChain, morDs, -1, 1);
            VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

            deviceConfigSpec.setDevice(newDisk);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            reConfigSpec.getDeviceChange().add(deviceConfigSpec);

            ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
            boolean result = _context.getVimClient().waitForTask(morTask);

            if (!result) {
                if (s_logger.isTraceEnabled())
                    s_logger.trace("vCenter API trace - attachDisk() done(failed)");
                throw new Exception("Failed to attach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }

            _context.waitForTaskProgressDone(morTask);
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachDisk() done(successfully)");
    }

    public void attachDisk(Pair<String, ManagedObjectReference>[] vmdkDatastorePathChain, int controllerKey) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: " + new Gson().toJson(vmdkDatastorePathChain));

        synchronized (_mor.getValue().intern()) {
            VirtualDevice newDisk = VmwareHelper.prepareDiskDevice(this, controllerKey, vmdkDatastorePathChain, -1, 1);
            VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

            deviceConfigSpec.setDevice(newDisk);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            reConfigSpec.getDeviceChange().add(deviceConfigSpec);

            ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
            boolean result = _context.getVimClient().waitForTask(morTask);

            if (!result) {
                if (s_logger.isTraceEnabled())
                    s_logger.trace("vCenter API trace - attachDisk() done(failed)");
                throw new Exception("Failed to attach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }

            _context.waitForTaskProgressDone(morTask);
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachDisk() done(successfully)");
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath
    public List<Pair<String, ManagedObjectReference>> detachDisk(String vmdkDatastorePath, boolean deleteBackingFile) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - detachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: " + vmdkDatastorePath + ", deleteBacking: " +
                    deleteBackingFile);

        // Note: if VM has been taken snapshot, original backing file will be renamed, therefore, when we try to find the matching
        // VirtualDisk, we only perform prefix matching
        Pair<VirtualDisk, String> deviceInfo = getDiskDevice(vmdkDatastorePath);
        if (deviceInfo == null) {
            s_logger.warn("vCenter API trace - detachDisk() done (failed)");
            throw new Exception("No such disk device: " + vmdkDatastorePath);
        }

        // IDE virtual disk cannot be detached if VM is running
        if (deviceInfo.second() != null && deviceInfo.second().contains("ide")) {
            if (getPowerState() == VirtualMachinePowerState.POWERED_ON) {
                throw new Exception("Removing a virtual disk over IDE controller is not supported while VM is running in VMware hypervisor. " +
                        "Please re-try when VM is not running.");
            }
        }

        List<Pair<String, ManagedObjectReference>> chain = getDiskDatastorePathChain(deviceInfo.first(), true);

        VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

        deviceConfigSpec.setDevice(deviceInfo.first());
        if (deleteBackingFile) {
            deviceConfigSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.DESTROY);
        }
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

        reConfigSpec.getDeviceChange().add(deviceConfigSpec);

        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
        boolean result = _context.getVimClient().waitForTask(morTask);

        if (!result) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - detachDisk() done (failed)");

            throw new Exception("Failed to detach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        _context.waitForTaskProgressDone(morTask);

        // VMware does not update snapshot references to the detached disk, we have to work around it
        SnapshotDescriptor snapshotDescriptor = null;
        try {
            snapshotDescriptor = getSnapshotDescriptor();
        } catch (Exception e) {
            s_logger.info("Unable to retrieve snapshot descriptor, will skip updating snapshot reference");
        }

        if (snapshotDescriptor != null) {
            for (Pair<String, ManagedObjectReference> pair : chain) {
                DatastoreFile dsFile = new DatastoreFile(pair.first());
                snapshotDescriptor.removeDiskReferenceFromSnapshot(dsFile.getFileName());
            }

            Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();
            String dsPath = getSnapshotDescriptorDatastorePath();
            assert (dsPath != null);
            String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), dsPath);
            getContext().uploadResourceContent(url, snapshotDescriptor.getVmsdContent());
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - detachDisk() done (successfully)");
        return chain;
    }

    public void detachAllDisks() throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - detachAllDisk(). target MOR: " + _mor.getValue());

        VirtualDisk[] disks = getAllDiskDevice();
        if (disks.length > 0) {
            VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[disks.length];

            for (int i = 0; i < disks.length; i++) {
                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                deviceConfigSpecArray[i].setDevice(disks[i]);
                deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
            }
            reConfigSpec.getDeviceChange().addAll(Arrays.asList(deviceConfigSpecArray));

            ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
            boolean result = _context.getVimClient().waitForTask(morTask);

            if (!result) {
                if (s_logger.isTraceEnabled())
                    s_logger.trace("vCenter API trace - detachAllDisk() done(failed)");
                throw new Exception("Failed to detach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }

            _context.waitForTaskProgressDone(morTask);
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - detachAllDisk() done(successfully)");
    }

    // isoDatastorePath: [datastore name] isoFilePath
    public void attachIso(String isoDatastorePath, ManagedObjectReference morDs, boolean connect, boolean connectAtBoot) throws Exception {
        attachIso(isoDatastorePath, morDs, connect, connectAtBoot, null);
    }

    // isoDatastorePath: [datastore name] isoFilePath
    public void attachIso(String isoDatastorePath, ManagedObjectReference morDs,
    boolean connect, boolean connectAtBoot, Integer key) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachIso(). target MOR: " + _mor.getValue() + ", isoDatastorePath: " + isoDatastorePath + ", datastore: " +
                    morDs.getValue() + ", connect: " + connect + ", connectAtBoot: " + connectAtBoot);

        assert (isoDatastorePath != null);
        assert (morDs != null);

        boolean newCdRom = false;
        VirtualCdrom cdRom;
        if (key == null) {
            cdRom = (VirtualCdrom) getIsoDevice();
        } else {
            cdRom = (VirtualCdrom) getIsoDevice(key);
        }
        if (cdRom == null) {
            newCdRom = true;
            cdRom = new VirtualCdrom();
            cdRom.setControllerKey(getIDEDeviceControllerKey());

            int deviceNumber = getNextIDEDeviceNumber();
            cdRom.setUnitNumber(deviceNumber);
            cdRom.setKey(-deviceNumber);
        }

        VirtualDeviceConnectInfo cInfo = new VirtualDeviceConnectInfo();
        cInfo.setConnected(connect);
        cInfo.setStartConnected(connectAtBoot);
        cdRom.setConnectable(cInfo);

        VirtualCdromIsoBackingInfo backingInfo = new VirtualCdromIsoBackingInfo();
        backingInfo.setFileName(isoDatastorePath);
        backingInfo.setDatastore(morDs);
        cdRom.setBacking(backingInfo);

        VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
        //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

        deviceConfigSpec.setDevice(cdRom);
        if (newCdRom) {
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
        } else {
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);
        }

        //deviceConfigSpecArray[0] = deviceConfigSpec;
        reConfigSpec.getDeviceChange().add(deviceConfigSpec);

        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
        boolean result = _context.getVimClient().waitForTask(morTask);

        if (!result) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - attachIso() done(failed)");
            throw new Exception("Failed to attach ISO due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        _context.waitForTaskProgressDone(morTask);

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - attachIso() done(successfully)");
    }

    public int detachIso(String isoDatastorePath) throws Exception {
        return detachIso(isoDatastorePath, false);
    }

    public int detachIso(String isoDatastorePath, final boolean force) throws Exception {
        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - detachIso(). target MOR: " + _mor.getValue() + ", isoDatastorePath: " + isoDatastorePath);

        VirtualDevice device = getIsoDevice(isoDatastorePath);
        if (device == null) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("vCenter API trace - detachIso() done(failed)");
            throw new Exception("Unable to find a CDROM device");
        }

        VirtualCdromRemotePassthroughBackingInfo backingInfo = new VirtualCdromRemotePassthroughBackingInfo();
        backingInfo.setDeviceName("");
        device.setBacking(backingInfo);

        VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
        //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

        deviceConfigSpec.setDevice(device);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);

        //deviceConfigSpecArray[0] = deviceConfigSpec;
        reConfigSpec.getDeviceChange().add(deviceConfigSpec);

        ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);

        // Monitor VM questions
        final Boolean[] flags = {false};
        final VirtualMachineMO vmMo = this;
        Future<?> future = MonitorServiceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                s_logger.info("VM Question monitor started...");

                while (!flags[0]) {
                    try {
                        VirtualMachineRuntimeInfo runtimeInfo = vmMo.getRuntimeInfo();
                        VirtualMachineQuestionInfo question = runtimeInfo.getQuestion();
                        if (question != null) {
                            if (s_logger.isTraceEnabled()) {
                                s_logger.trace("Question id: " + question.getId());
                                s_logger.trace("Question text: " + question.getText());
                            }
                            if (question.getMessage() != null) {
                                for (VirtualMachineMessage msg : question.getMessage()) {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("msg id: " + msg.getId());
                                        s_logger.trace("msg text: " + msg.getText());
                                    }
                                    if ("msg.cdromdisconnect.locked".equalsIgnoreCase(msg.getId())) {
                                        s_logger.info("Found that VM has a pending question that we need to answer programmatically, question id: " + msg.getId() +
                                                ", for safe operation we will automatically decline it");
                                        vmMo.answerVM(question.getId(), force ? ANSWER_YES : ANSWER_NO);
                                        break;
                                    }
                                }
                            } else if (question.getText() != null) {
                                String text = question.getText();
                                String msgId;
                                String msgText;
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("question text : " + text);
                                }
                                String[] tokens = text.split(":");
                                msgId = tokens[0];
                                msgText = tokens[1];
                                if ("msg.cdromdisconnect.locked".equalsIgnoreCase(msgId)) {
                                    s_logger.info("Found that VM has a pending question that we need to answer programmatically, question id: " + question.getId() +
                                            ". Message id : " + msgId + ". Message text : " + msgText + ", for safe operation we will automatically decline it.");
                                    vmMo.answerVM(question.getId(), force ? ANSWER_YES : ANSWER_NO);
                                }
                            }

                            ChoiceOption choice = question.getChoice();
                            if (choice != null) {
                                for (ElementDescription info : choice.getChoiceInfo()) {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("Choice option key: " + info.getKey());
                                        s_logger.trace("Choice option label: " + info.getLabel());
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        s_logger.error("Unexpected exception: ", e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        s_logger.debug("[ignored] interupted while handling vm question about iso detach.");
                    }
                }
                s_logger.info("VM Question monitor stopped");
            }
        });
        try {
            boolean result = _context.getVimClient().waitForTask(morTask);
            if (!result) {
                if (s_logger.isDebugEnabled())
                    s_logger.trace("vCenter API trace - detachIso() done(failed)");
                throw new Exception("Failed to detachIso due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }
            _context.waitForTaskProgressDone(morTask);
            s_logger.trace("vCenter API trace - detachIso() done(successfully)");
        } finally {
            flags[0] = true;
            future.cancel(true);
        }
        return device.getKey();
    }

    public Pair<VmdkFileDescriptor, byte[]> getVmdkFileInfo(String vmdkDatastorePath) throws Exception {

        if (s_logger.isTraceEnabled())
            s_logger.trace("vCenter API trace - getVmdkFileInfo(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: " + vmdkDatastorePath);

        Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();

        String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), vmdkDatastorePath);
        byte[] content = getContext().getResourceContent(url);
        VmdkFileDescriptor descriptor = new VmdkFileDescriptor();
        descriptor.parse(content);

        Pair<VmdkFileDescriptor, byte[]> result = new Pair<VmdkFileDescriptor, byte[]>(descriptor, content);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("vCenter API trace - getVmdkFileInfo() done");
            s_logger.trace("VMDK file descriptor: " + new Gson().toJson(result.first()));
        }
        return result;
    }

    public void exportVm(String exportDir, String exportName, boolean packToOva, boolean leaveOvaFileOnly) throws Exception {
        ManagedObjectReference morOvf = _context.getServiceContent().getOvfManager();

        VirtualMachineRuntimeInfo runtimeInfo = getRuntimeInfo();
        HostMO hostMo = new HostMO(_context, runtimeInfo.getHost());
        String hostName = hostMo.getHostName();
        String vmName = getVmName();

        DatacenterMO dcMo = new DatacenterMO(_context, hostMo.getHyperHostDatacenter());

        if (runtimeInfo.getPowerState() != VirtualMachinePowerState.POWERED_OFF) {
            String msg = "Unable to export VM because it is not at powerdOff state. vmName: " + vmName + ", host: " + hostName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        ManagedObjectReference morLease = _context.getService().exportVm(getMor());
        if (morLease == null) {
            s_logger.error("exportVm() failed");
            throw new Exception("exportVm() failed");
        }

        HttpNfcLeaseMO leaseMo = new HttpNfcLeaseMO(_context, morLease);
        HttpNfcLeaseState state = leaseMo.waitState(new HttpNfcLeaseState[] {HttpNfcLeaseState.READY, HttpNfcLeaseState.ERROR});

        try {
            if (state == HttpNfcLeaseState.READY) {
                final HttpNfcLeaseMO.ProgressReporter progressReporter = leaseMo.createProgressReporter();

                boolean success = false;
                List<String> fileNames = new ArrayList<String>();
                try {
                    HttpNfcLeaseInfo leaseInfo = leaseMo.getLeaseInfo();
                    final long totalBytes = leaseInfo.getTotalDiskCapacityInKB() * 1024;
                    long totalBytesDownloaded = 0;

                    List<HttpNfcLeaseDeviceUrl> deviceUrls = leaseInfo.getDeviceUrl();
                    s_logger.info("volss: copy vmdk and ovf file starts " + System.currentTimeMillis());
                    if (deviceUrls != null) {
                        OvfFile[] ovfFiles = new OvfFile[deviceUrls.size()];
                        for (int i = 0; i < deviceUrls.size(); i++) {
                            String deviceId = deviceUrls.get(i).getKey();
                            String deviceUrlStr = deviceUrls.get(i).getUrl();
                            String orgDiskFileName = deviceUrlStr.substring(deviceUrlStr.lastIndexOf("/") + 1);
                            String diskFileName = String.format("%s-disk%d%s", exportName, i, VmwareHelper.getFileExtension(orgDiskFileName, ".vmdk"));
                            String diskUrlStr = deviceUrlStr.replace("*", hostName);
                            diskUrlStr = HypervisorHostHelper.resolveHostNameInUrl(dcMo, diskUrlStr);
                            String diskLocalPath = exportDir + File.separator + diskFileName;
                            fileNames.add(diskLocalPath);

                            if (s_logger.isInfoEnabled()) {
                                s_logger.info("Download VMDK file for export. url: " + deviceUrlStr);
                            }
                            long lengthOfDiskFile = _context.downloadVmdkFile(diskUrlStr, diskLocalPath, totalBytesDownloaded, new ActionDelegate<Long>() {
                                @Override
                                public void action(Long param) {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("Download progress " + param + "/" + totalBytes);
                                    }
                                    progressReporter.reportProgress((int)(param * 100 / totalBytes));
                                }
                            });
                            totalBytesDownloaded += lengthOfDiskFile;

                            OvfFile ovfFile = new OvfFile();
                            ovfFile.setPath(diskFileName);
                            ovfFile.setDeviceId(deviceId);
                            ovfFile.setSize(lengthOfDiskFile);
                            ovfFiles[i] = ovfFile;
                        }

                        // write OVF descriptor file
                        OvfCreateDescriptorParams ovfDescParams = new OvfCreateDescriptorParams();
                        ovfDescParams.getOvfFiles().addAll(Arrays.asList(ovfFiles));
                        OvfCreateDescriptorResult ovfCreateDescriptorResult = _context.getService().createDescriptor(morOvf, getMor(), ovfDescParams);
                        String ovfPath = exportDir + File.separator + exportName + ".ovf";
                        fileNames.add(ovfPath);

                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(ovfPath),"UTF-8");
                        out.write(ovfCreateDescriptorResult.getOvfDescriptor());
                        out.close();

                        // tar files into OVA
                        if (packToOva) {
                            // Important! we need to sync file system before we can safely use tar to work around a linux kernal bug(or feature)
                            s_logger.info("Sync file system before we package OVA...");

                            Script commandSync = new Script(true, "sync", 0, s_logger);
                            commandSync.execute();

                            Script command = new Script(false, "tar", 0, s_logger);
                            command.setWorkDir(exportDir);
                            command.add("-cf", exportName + ".ova");
                            command.add(exportName + ".ovf");        // OVF file should be the first file in OVA archive
                            for (String name : fileNames) {
                                command.add((new File(name).getName()));
                            }

                            s_logger.info("Package OVA with commmand: " + command.toString());
                            command.execute();

                            // to be safe, physically test existence of the target OVA file
                            if ((new File(exportDir + File.separator + exportName + ".ova")).exists()) {
                                success = true;
                            } else {
                                s_logger.error(exportDir + File.separator + exportName + ".ova is not created as expected");
                            }
                        } else {
                            success = true;
                        }
                    }
                    s_logger.info("volss: copy vmdk and ovf file finishes " + System.currentTimeMillis());
                } catch (Throwable e) {
                    s_logger.error("Unexpected exception ", e);
                } finally {
                    progressReporter.close();

                    if (leaveOvaFileOnly) {
                        for (String name : fileNames) {
                            new File(name).delete();
                        }
                    }

                    if (!success)
                        throw new Exception("Unable to finish the whole process to package as a OVA file");
                }
            }
        } finally {
            leaseMo.updateLeaseProgress(100);
            leaseMo.completeLease();
        }
    }

    // snapshot directory in format of: /vmfs/volumes/<datastore name>/<path>
    @Deprecated
    public void setSnapshotDirectory(String snapshotDir) throws Exception {
        VirtualMachineFileInfo fileInfo = getFileInfo();
        Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();
        String vmxUrl = _context.composeDatastoreBrowseUrl(dcInfo.second(), fileInfo.getVmPathName());
        byte[] vmxContent = _context.getResourceContent(vmxUrl);

        BufferedReader in = null;
        BufferedWriter out = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        boolean replaced = false;
        try {
            in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmxContent),"UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(bos,"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("workingDir")) {
                    replaced = true;
                    out.write(String.format("workingDir=\"%s\"", snapshotDir));
                    out.newLine();
                } else {
                    out.write(line);
                    out.newLine();
                }
            }

            if (!replaced) {
                out.newLine();
                out.write(String.format("workingDir=\"%s\"", snapshotDir));
                out.newLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        _context.uploadResourceContent(vmxUrl, bos.toByteArray());

        // It seems that I don't need to do re-registration. VMware has bug in writing the correct snapshot's VMDK path to
        // its disk backing info anyway.
        // redoRegistration();
    }

    // destName does not contain extension name
    public void backupCurrentSnapshot(String deviceName, ManagedObjectReference morDestDs, String destDsDirectory, String destName, boolean includeBase) throws Exception {

        SnapshotDescriptor descriptor = getSnapshotDescriptor();
        SnapshotInfo[] snapshotInfo = descriptor.getCurrentDiskChain();
        if (snapshotInfo.length == 0) {
            String msg = "No snapshot found in this VM";
            throw new Exception(msg);
        }

        HostMO hostMo = getRunningHost();
        DatacenterMO dcMo = getOwnerDatacenter().first();
        List<Pair<ManagedObjectReference, String>> mounts = hostMo.getDatastoreMountsOnHost();
        VirtualMachineFileInfo vmFileInfo = getFileInfo();

        List<Ternary<String, String, String>> backupInfo = new ArrayList<Ternary<String, String, String>>();

        for (int i = 0; i < snapshotInfo.length; i++) {
            if (!includeBase && i == snapshotInfo.length - 1) {
                break;
            }

            SnapshotDescriptor.DiskInfo[] disks = snapshotInfo[i].getDisks();
            if (disks != null) {
                String destBaseFileName;
                String destFileName;
                String destParentFileName;
                for (SnapshotDescriptor.DiskInfo disk : disks) {
                    if (deviceName == null || deviceName.equals(disk.getDeviceName())) {
                        String srcVmdkFullDsPath = getSnapshotDiskFileDatastorePath(vmFileInfo, mounts, disk.getDiskFileName());
                        Pair<DatastoreMO, String> srcDsInfo = getOwnerDatastore(srcVmdkFullDsPath);

                        Pair<VmdkFileDescriptor, byte[]> vmdkInfo = getVmdkFileInfo(srcVmdkFullDsPath);
                        String srcVmdkBaseFilePath = DatastoreFile.getCompanionDatastorePath(srcVmdkFullDsPath, vmdkInfo.first().getBaseFileName());

                        destFileName = destName + (snapshotInfo.length - i - 1) + ".vmdk";
                        if (vmdkInfo.first().getParentFileName() != null) {
                            destBaseFileName = destName + (snapshotInfo.length - i - 1) + "-delta.vmdk";
                            destParentFileName = destName + (snapshotInfo.length - i - 2) + ".vmdk";
                        } else {
                            destBaseFileName = destName + (snapshotInfo.length - i - 1) + "-flat.vmdk";
                            destParentFileName = null;
                        }

                        s_logger.info("Copy VMDK base file " + srcVmdkBaseFilePath + " to " + destDsDirectory + "/" + destBaseFileName);
                        srcDsInfo.first().copyDatastoreFile(srcVmdkBaseFilePath, dcMo.getMor(), morDestDs, destDsDirectory + "/" + destBaseFileName, dcMo.getMor(), true);

                        byte[] newVmdkContent = VmdkFileDescriptor.changeVmdkContentBaseInfo(vmdkInfo.second(), destBaseFileName, destParentFileName);
                        String vmdkUploadUrl = getContext().composeDatastoreBrowseUrl(dcMo.getName(), destDsDirectory + "/" + destFileName);

                        s_logger.info("Upload VMDK content file to " + destDsDirectory + "/" + destFileName);
                        getContext().uploadResourceContent(vmdkUploadUrl, newVmdkContent);

                        backupInfo.add(new Ternary<String, String, String>(destFileName, destBaseFileName, destParentFileName));
                    }
                }
            }
        }

        byte[] vdiskInfo = VmwareHelper.composeDiskInfo(backupInfo, snapshotInfo.length, includeBase);
        String vdiskUploadUrl = getContext().composeDatastoreBrowseUrl(dcMo.getName(), destDsDirectory + "/" + destName + ".vdisk");
        getContext().uploadResourceContent(vdiskUploadUrl, vdiskInfo);
    }

    public String[] getCurrentSnapshotDiskChainDatastorePaths(String diskDevice) throws Exception {
        HostMO hostMo = getRunningHost();
        List<Pair<ManagedObjectReference, String>> mounts = hostMo.getDatastoreMountsOnHost();
        VirtualMachineFileInfo vmFileInfo = getFileInfo();

        SnapshotDescriptor descriptor = getSnapshotDescriptor();
        SnapshotInfo[] snapshotInfo = descriptor.getCurrentDiskChain();

        List<String> diskDsFullPaths = new ArrayList<String>();
        for (int i = 0; i < snapshotInfo.length; i++) {
            SnapshotDescriptor.DiskInfo[] disks = snapshotInfo[i].getDisks();
            if (disks != null) {
                for (SnapshotDescriptor.DiskInfo disk : disks) {
                    String deviceNameInDisk = disk.getDeviceName();
                    if (diskDevice == null || diskDevice.equalsIgnoreCase(deviceNameInDisk)) {
                        String vmdkFullDsPath = getSnapshotDiskFileDatastorePath(vmFileInfo, mounts, disk.getDiskFileName());
                        diskDsFullPaths.add(vmdkFullDsPath);
                    }
                }
            }
        }
        return diskDsFullPaths.toArray(new String[0]);
    }

    // return the disk chain (VMDK datastore paths) for cloned snapshot
    public Pair<VirtualMachineMO, String[]> cloneFromCurrentSnapshot(String clonedVmName, int cpuSpeedMHz, int memoryMb, String diskDevice, ManagedObjectReference morDs)
            throws Exception {
        assert (morDs != null);
        String[] disks = getCurrentSnapshotDiskChainDatastorePaths(diskDevice);
        VirtualMachineMO clonedVm = cloneFromDiskChain(clonedVmName, cpuSpeedMHz, memoryMb, disks, morDs);
        return new Pair<VirtualMachineMO, String[]>(clonedVm, disks);
    }

    public VirtualMachineMO cloneFromDiskChain(String clonedVmName, int cpuSpeedMHz, int memoryMb, String[] disks, ManagedObjectReference morDs) throws Exception {
        assert (disks != null);
        assert (disks.length >= 1);

        HostMO hostMo = getRunningHost();

        VirtualMachineMO clonedVmMo = HypervisorHostHelper.createWorkerVM(hostMo, new DatastoreMO(hostMo.getContext(), morDs), clonedVmName);
        if (clonedVmMo == null)
            throw new Exception("Unable to find just-created blank VM");

        boolean bSuccess = false;
        try {
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

            VirtualDevice device = VmwareHelper.prepareDiskDevice(clonedVmMo, null, -1, disks, morDs, -1, 1);

            deviceConfigSpec.setDevice(device);
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
            vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
            clonedVmMo.configureVm(vmConfigSpec);
            bSuccess = true;
            return clonedVmMo;
        } finally {
            if (!bSuccess) {
                clonedVmMo.detachAllDisks();
                clonedVmMo.destroy();
            }
        }
    }

    public GuestOsDescriptor getGuestOsDescriptor(String guestOsId) throws Exception {
        GuestOsDescriptor guestOsDescriptor = null;
        String guestId = guestOsId;
        if (guestId == null) {
            guestId = getGuestId();
        }
        ManagedObjectReference vmEnvironmentBrowser = _context.getVimClient().getMoRefProp(_mor, "environmentBrowser");
        VirtualMachineConfigOption vmConfigOption = _context.getService().queryConfigOption(vmEnvironmentBrowser, null, null);
        List<GuestOsDescriptor> guestDescriptors = vmConfigOption.getGuestOSDescriptor();
        for (GuestOsDescriptor descriptor : guestDescriptors) {
            if (guestId != null && guestId.equalsIgnoreCase(descriptor.getId())) {
                guestOsDescriptor = descriptor;
                break;
            }
        }
        return guestOsDescriptor;
    }

    public void plugDevice(VirtualDevice device) throws Exception {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(device);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
        if (!configureVm(vmConfigSpec)) {
            throw new Exception("Failed to add devices");
        }
    }

    public void tearDownDevice(VirtualDevice device) throws Exception {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(device);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

        vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
        if (!configureVm(vmConfigSpec)) {
            throw new Exception("Failed to detach devices");
        }
    }

    public void tearDownDevices(Class<?>[] deviceClasses) throws Exception {
        VirtualDevice[] devices = getMatchedDevices(deviceClasses);

        if (devices.length > 0) {
            VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
            VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[devices.length];

            for (int i = 0; i < devices.length; i++) {
                deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
                deviceConfigSpecArray[i].setDevice(devices[i]);
                deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
                vmConfigSpec.getDeviceChange().add(deviceConfigSpecArray[i]);
            }

            if (!configureVm(vmConfigSpec)) {
                throw new Exception("Failed to detach devices");
            }
        }
    }

    public void copyAllVmDiskFiles(DatastoreMO destDsMo, String destDsDir, boolean followDiskChain) throws Exception {
        VirtualDevice[] disks = getAllDiskDevice();
        DatacenterMO dcMo = getOwnerDatacenter().first();
        if (disks != null) {
            for (VirtualDevice disk : disks) {
                List<Pair<String, ManagedObjectReference>> vmdkFiles = getDiskDatastorePathChain((VirtualDisk)disk, followDiskChain);
                for (Pair<String, ManagedObjectReference> fileItem : vmdkFiles) {
                    DatastoreMO srcDsMo = new DatastoreMO(_context, fileItem.second());

                    DatastoreFile srcFile = new DatastoreFile(fileItem.first());
                    DatastoreFile destFile = new DatastoreFile(destDsMo.getName(), destDsDir, srcFile.getFileName());

                    Pair<VmdkFileDescriptor, byte[]> vmdkDescriptor = null;

                    vmdkDescriptor = getVmdkFileInfo(fileItem.first());

                    s_logger.info("Copy VM disk file " + srcFile.getPath() + " to " + destFile.getPath());
                    srcDsMo.copyDatastoreFile(fileItem.first(), dcMo.getMor(), destDsMo.getMor(), destFile.getPath(), dcMo.getMor(), true);

                    if (vmdkDescriptor != null) {
                        String vmdkBaseFileName = vmdkDescriptor.first().getBaseFileName();
                        String baseFilePath = srcFile.getCompanionPath(vmdkBaseFileName);
                        destFile = new DatastoreFile(destDsMo.getName(), destDsDir, vmdkBaseFileName);

                        s_logger.info("Copy VM disk file " + baseFilePath + " to " + destFile.getPath());
                        srcDsMo.copyDatastoreFile(baseFilePath, dcMo.getMor(), destDsMo.getMor(), destFile.getPath(), dcMo.getMor(), true);
                    }
                }
            }
        }
    }

    public List<String> getVmdkFileBaseNames() throws Exception {
        List<String> vmdkFileBaseNames = new ArrayList<String>();
        VirtualDevice[] devices = getAllDiskDevice();
        for(VirtualDevice device : devices) {
            if(device instanceof VirtualDisk) {
                vmdkFileBaseNames.add(getVmdkFileBaseName((VirtualDisk)device));
            }
        }
        return vmdkFileBaseNames;
    }

    public String getVmdkFileBaseName(VirtualDisk disk) throws Exception {
        String vmdkFileBaseName = null;
        VirtualDeviceBackingInfo backingInfo = disk.getBacking();
        if(backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
            VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;
            DatastoreFile dsBackingFile = new DatastoreFile(diskBackingInfo.getFileName());
            vmdkFileBaseName = dsBackingFile.getFileBaseName();
        }
        return vmdkFileBaseName;
    }

    // this method relies on un-offical VMware API
    @Deprecated
    public void moveAllVmDiskFiles(DatastoreMO destDsMo, String destDsDir, boolean followDiskChain) throws Exception {
        VirtualDevice[] disks = getAllDiskDevice();
        DatacenterMO dcMo = getOwnerDatacenter().first();
        if (disks != null) {
            for (VirtualDevice disk : disks) {
                List<Pair<String, ManagedObjectReference>> vmdkFiles = getDiskDatastorePathChain((VirtualDisk)disk, followDiskChain);
                for (Pair<String, ManagedObjectReference> fileItem : vmdkFiles) {
                    DatastoreMO srcDsMo = new DatastoreMO(_context, fileItem.second());

                    DatastoreFile srcFile = new DatastoreFile(fileItem.first());
                    DatastoreFile destFile = new DatastoreFile(destDsMo.getName(), destDsDir, srcFile.getFileName());

                    Pair<VmdkFileDescriptor, byte[]> vmdkDescriptor = null;
                    vmdkDescriptor = getVmdkFileInfo(fileItem.first());

                    s_logger.info("Move VM disk file " + srcFile.getPath() + " to " + destFile.getPath());
                    srcDsMo.moveDatastoreFile(fileItem.first(), dcMo.getMor(), destDsMo.getMor(), destFile.getPath(), dcMo.getMor(), true);

                    if (vmdkDescriptor != null) {
                        String vmdkBaseFileName = vmdkDescriptor.first().getBaseFileName();
                        String baseFilePath = srcFile.getCompanionPath(vmdkBaseFileName);
                        destFile = new DatastoreFile(destDsMo.getName(), destDsDir, vmdkBaseFileName);

                        s_logger.info("Move VM disk file " + baseFilePath + " to " + destFile.getPath());
                        srcDsMo.moveDatastoreFile(baseFilePath, dcMo.getMor(), destDsMo.getMor(), destFile.getPath(), dcMo.getMor(), true);
                    }
                }
            }
        }
    }

    public int getPvScsiDeviceControllerKeyNoException() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof ParaVirtualSCSIController) {
                    return device.getKey();
                }
            }
        }

        return -1;
    }

    public int getPvScsiDeviceControllerKey() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof ParaVirtualSCSIController) {
                    return device.getKey();
                }
            }
        }

        assert (false);
        throw new Exception("VMware Paravirtual SCSI Controller Not Found");
    }

    public void ensurePvScsiDeviceController(int requiredNumScsiControllers, int availableBusNum) throws Exception {
        VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();

        int busNum = availableBusNum;
        while (busNum < requiredNumScsiControllers) {
            ParaVirtualSCSIController scsiController = new ParaVirtualSCSIController();

            scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
            scsiController.setBusNumber(busNum);
            scsiController.setKey(busNum - VmwareHelper.MAX_SCSI_CONTROLLER_COUNT);
            VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
            scsiControllerSpec.setDevice(scsiController);
            scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            vmConfig.getDeviceChange().add(scsiControllerSpec);
            busNum++;
        }

        if (configureVm(vmConfig)) {
            throw new Exception("Unable to add Scsi controllers to the VM " + getName());
        } else {
            s_logger.info("Successfully added " + requiredNumScsiControllers + " SCSI controllers.");
        }
    }

    public String getRecommendedDiskController(String guestOsId) throws Exception {
        String recommendedController;
        GuestOsDescriptor guestOsDescriptor = getGuestOsDescriptor(guestOsId);
        recommendedController = VmwareHelper.getRecommendedDiskControllerFromDescriptor(guestOsDescriptor);
        return recommendedController;
    }

    public boolean isPvScsiSupported() throws Exception {
        int virtualHardwareVersion;

        virtualHardwareVersion = getVirtualHardwareVersion();

        // Check if virtual machine is using hardware version 7 or later.
        if (virtualHardwareVersion < 7) {
            s_logger.error("The virtual hardware version of the VM is " + virtualHardwareVersion
                    + ", which doesn't support PV SCSI controller type for virtual harddisks. Please upgrade this VM's virtual hardware version to 7 or later.");
            return false;
        }
        return true;
    }

    // Would be useful if there exists multiple sub types of SCSI controllers per VM are supported in CloudStack f
    public int getScsiDiskControllerKey(String diskController) throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if ((DiskControllerType.getType(diskController) == DiskControllerType.lsilogic || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof VirtualLsiLogicController) {
                    return ((VirtualLsiLogicController)device).getKey();
                } else if ((DiskControllerType.getType(diskController) == DiskControllerType.lsisas1068 || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof VirtualLsiLogicSASController) {
                    return ((VirtualLsiLogicSASController)device).getKey();
                } else if ((DiskControllerType.getType(diskController) == DiskControllerType.pvscsi || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof ParaVirtualSCSIController) {
                    return ((ParaVirtualSCSIController)device).getKey();
                } else if ((DiskControllerType.getType(diskController) == DiskControllerType.buslogic || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof VirtualBusLogicController) {
                    return ((VirtualBusLogicController)device).getKey();
                }
            }
        }

        assert (false);
        throw new IllegalStateException("Scsi disk controller of type " + diskController + " not found among configured devices.");
    }

    public int getScsiDiskControllerKeyNoException(String diskController) throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if ((DiskControllerType.getType(diskController) == DiskControllerType.lsilogic || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof VirtualLsiLogicController) {
                    return ((VirtualLsiLogicController)device).getKey();
                } else if ((DiskControllerType.getType(diskController) == DiskControllerType.lsisas1068 || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof VirtualLsiLogicSASController) {
                    return ((VirtualLsiLogicSASController)device).getKey();
                } else if ((DiskControllerType.getType(diskController) == DiskControllerType.pvscsi || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof ParaVirtualSCSIController) {
                    return ((ParaVirtualSCSIController)device).getKey();
                } else if ((DiskControllerType.getType(diskController) == DiskControllerType.buslogic || DiskControllerType.getType(diskController) == DiskControllerType.scsi)
                        && device instanceof VirtualBusLogicController) {
                    return ((VirtualBusLogicController)device).getKey();
                }
            }
        }
        return -1;
    }

    public int getNextScsiDiskDeviceNumber() throws Exception {
        int scsiControllerKey = getScsiDeviceControllerKey();
        int deviceNumber = getNextDeviceNumber(scsiControllerKey);

        return deviceNumber;
    }

    public int getScsiDeviceControllerKey() throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualSCSIController) {
                    return device.getKey();
                }
            }
        }

        assert (false);
        throw new Exception("SCSI Controller Not Found");
    }

    public int getGenericScsiDeviceControllerKeyNoException() throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualSCSIController) {
                    return device.getKey();
                }
            }
        }

        return -1;
    }

    public int getScsiDeviceControllerKeyNoException() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
            getDynamicProperty(_mor, "config.hardware.device");

        if(devices != null && devices.size() > 0) {
            for(VirtualDevice device : devices) {
                if(device instanceof VirtualSCSIController) {
                    return device.getKey();
                }
            }
        }

        return -1;
    }

    public void ensureLsiLogicDeviceControllers(int count, int availableBusNum) throws Exception {
        int scsiControllerKey = getLsiLogicDeviceControllerKeyNoException();
        if (scsiControllerKey < 0) {
            VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();

            int busNum = availableBusNum;
            while (busNum < count) {
                VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
                scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
                scsiController.setBusNumber(busNum);
                scsiController.setKey(busNum - VmwareHelper.MAX_SCSI_CONTROLLER_COUNT);
                VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
                scsiControllerSpec.setDevice(scsiController);
                scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

                vmConfig.getDeviceChange().add(scsiControllerSpec);
                busNum++;
            }
            if (configureVm(vmConfig)) {
                throw new Exception("Unable to add Lsi Logic controllers to the VM " + getName());
            } else {
                s_logger.info("Successfully added " + count + " LsiLogic Parallel SCSI controllers.");
            }
        }
    }

    private int getLsiLogicDeviceControllerKeyNoException() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualLsiLogicController) {
                    return device.getKey();
                }
            }
        }

        return -1;
    }

    public void ensureScsiDeviceController() throws Exception {
        int scsiControllerKey = getScsiDeviceControllerKeyNoException();
        if (scsiControllerKey < 0) {
            VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();

            // Scsi controller
            VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
            scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
            scsiController.setBusNumber(0);
            scsiController.setKey(1);
            VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
            scsiControllerSpec.setDevice(scsiController);
            scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            vmConfig.getDeviceChange().add(scsiControllerSpec);
            if (configureVm(vmConfig)) {
                throw new Exception("Unable to add Scsi controller");
            }
        }
    }

    public void ensureScsiDeviceControllers(int count, int availableBusNum) throws Exception {
        int scsiControllerKey = getScsiDeviceControllerKeyNoException();
        if (scsiControllerKey < 0) {
            VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();

            int busNum = availableBusNum;
            while (busNum < count) {
            VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
            scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
                scsiController.setBusNumber(busNum);
                scsiController.setKey(busNum - VmwareHelper.MAX_SCSI_CONTROLLER_COUNT);
            VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
            scsiControllerSpec.setDevice(scsiController);
            scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

            vmConfig.getDeviceChange().add(scsiControllerSpec);
                busNum++;
            }
            if (configureVm(vmConfig)) {
                throw new Exception("Unable to add Scsi controllers to the VM " + getName());
            } else {
                s_logger.info("Successfully added " + count + " SCSI controllers.");
            }
        }
    }

    // return pair of VirtualDisk and disk device bus name(ide0:0, etc)
    public Pair<VirtualDisk, String> getDiskDevice(String vmdkDatastorePath) throws Exception {
        final String zeroLengthString = "";

        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
        ArrayList<Pair<VirtualDisk, String>> partialMatchingDiskDevices = new ArrayList<>();

        DatastoreFile dsSrcFile = new DatastoreFile(vmdkDatastorePath);

        String srcBaseName = dsSrcFile.getFileBaseName();
        String trimmedSrcBaseName = VmwareHelper.trimSnapshotDeltaPostfix(srcBaseName);
        String srcDatastoreName = dsSrcFile.getDatastoreName() != null ? dsSrcFile.getDatastoreName() : zeroLengthString;

        s_logger.info("Look for disk device info for volume : " + vmdkDatastorePath + " with base name: " + srcBaseName);

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    s_logger.info("Test against disk device, controller key: " + device.getControllerKey() + ", unit number: " + device.getUnitNumber());

                    VirtualDeviceBackingInfo backingInfo = device.getBacking();

                    if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;

                        do {
                            s_logger.info("Test against disk backing : " + diskBackingInfo.getFileName());

                            DatastoreFile dsBackingFile = new DatastoreFile(diskBackingInfo.getFileName());

                            String backingDatastoreName = dsBackingFile.getDatastoreName() != null ? dsBackingFile.getDatastoreName() : zeroLengthString;

                            if (srcDatastoreName.equals(zeroLengthString)) {
                                backingDatastoreName = zeroLengthString;
                            }

                            if (srcDatastoreName.equalsIgnoreCase(backingDatastoreName)) {
                                String backingBaseName = dsBackingFile.getFileBaseName();

                                if (backingBaseName.equalsIgnoreCase(srcBaseName)) {
                                    String deviceNumbering = getDeviceBusName(devices, device);

                                    s_logger.info("Disk backing : " + diskBackingInfo.getFileName() + " matches ==> " + deviceNumbering);

                                    return new Pair<>((VirtualDisk)device, deviceNumbering);
                                }

                                if (backingBaseName.contains(trimmedSrcBaseName)) {
                                    String deviceNumbering = getDeviceBusName(devices, device);

                                    partialMatchingDiskDevices.add(new Pair<>((VirtualDisk)device, deviceNumbering));
                                }
                            }

                            diskBackingInfo = diskBackingInfo.getParent();
                        } while (diskBackingInfo != null);
                    }
                }
            }
        }

        // No disk device was found with an exact match for the volume path, hence look for disk device that matches the trimmed name.
        s_logger.info("No disk device with an exact match found for volume : " + vmdkDatastorePath + ". Look for disk device info against trimmed base name: " + srcBaseName);

        if (partialMatchingDiskDevices != null) {
            if (partialMatchingDiskDevices.size() == 1) {
                VirtualDiskFlatVer2BackingInfo matchingDiskBackingInfo = (VirtualDiskFlatVer2BackingInfo)partialMatchingDiskDevices.get(0).first().getBacking();

                s_logger.info("Disk backing : " + matchingDiskBackingInfo.getFileName() + " matches ==> " + partialMatchingDiskDevices.get(0).second());

                return partialMatchingDiskDevices.get(0);
            } else if (partialMatchingDiskDevices.size() > 1) {
                s_logger.warn("Disk device info lookup for volume: " + vmdkDatastorePath + " failed as multiple disk devices were found to match" +
                        " volume's trimmed base name: " + trimmedSrcBaseName);

                return null;
            }
        }

        s_logger.warn("Disk device info lookup for volume: " + vmdkDatastorePath + " failed as no matching disk device found");

        return null;
    }

    // return pair of VirtualDisk and disk device bus name(ide0:0, etc)
    public Pair<VirtualDisk, String> getDiskDevice(String vmdkDatastorePath, boolean matchExactly) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        DatastoreFile dsSrcFile = new DatastoreFile(vmdkDatastorePath);
        String srcBaseName = dsSrcFile.getFileBaseName();
        String trimmedSrcBaseName = VmwareHelper.trimSnapshotDeltaPostfix(srcBaseName);

        if (matchExactly) {
            s_logger.info("Look for disk device info from volume : " + vmdkDatastorePath + " with base name: " + srcBaseName);
        } else {
            s_logger.info("Look for disk device info from volume : " + vmdkDatastorePath + " with trimmed base name: " + trimmedSrcBaseName);
        }

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    s_logger.info("Test against disk device, controller key: " + device.getControllerKey() + ", unit number: " + device.getUnitNumber());

                    VirtualDeviceBackingInfo backingInfo = ((VirtualDisk)device).getBacking();
                    if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;
                        do {
                            s_logger.info("Test against disk backing : " + diskBackingInfo.getFileName());

                            DatastoreFile dsBackingFile = new DatastoreFile(diskBackingInfo.getFileName());
                            String backingBaseName = dsBackingFile.getFileBaseName();
                            if (matchExactly) {
                                if (backingBaseName.equalsIgnoreCase(srcBaseName)) {
                                    String deviceNumbering = getDeviceBusName(devices, device);

                                    s_logger.info("Disk backing : " + diskBackingInfo.getFileName() + " matches ==> " + deviceNumbering);
                                    return new Pair<VirtualDisk, String>((VirtualDisk)device, deviceNumbering);
                                }
                            } else {
                                if (backingBaseName.contains(trimmedSrcBaseName)) {
                                    String deviceNumbering = getDeviceBusName(devices, device);

                                    s_logger.info("Disk backing : " + diskBackingInfo.getFileName() + " matches ==> " + deviceNumbering);
                                    return new Pair<VirtualDisk, String>((VirtualDisk)device, deviceNumbering);
                                }
                            }

                            diskBackingInfo = diskBackingInfo.getParent();
                        } while (diskBackingInfo != null);
                    }
                }
            }
        }

        return null;
    }

    public String getDiskCurrentTopBackingFileInChain(String deviceBusName) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    s_logger.info("Test against disk device, controller key: " + device.getControllerKey() + ", unit number: " + device.getUnitNumber());

                    VirtualDeviceBackingInfo backingInfo = ((VirtualDisk)device).getBacking();
                    if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;

                        String deviceNumbering = getDeviceBusName(devices, device);
                        if (deviceNumbering.equals(deviceBusName))
                            return diskBackingInfo.getFileName();
                    }
                }
            }
        }

        return null;
    }

    public VirtualDisk getDiskDeviceByDeviceBusName(String deviceBusName) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    String deviceNumbering = getDeviceBusName(devices, device);
                    if (deviceNumbering.equals(deviceBusName))
                        return (VirtualDisk)device;
                }
            }
        }

        return null;
    }

    public VirtualMachineDiskInfoBuilder getDiskInfoBuilder() throws Exception {
        VirtualMachineDiskInfoBuilder builder = new VirtualMachineDiskInfoBuilder();

        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    VirtualDeviceBackingInfo backingInfo = ((VirtualDisk)device).getBacking();
                    if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;

                        while (diskBackingInfo != null) {
                            String deviceBusName = getDeviceBusName(devices, device);
                            builder.addDisk(deviceBusName, diskBackingInfo.getFileName());
                            diskBackingInfo = diskBackingInfo.getParent();
                        }
                    }
                }
            }
        }

        return builder;
    }

    public List<Pair<Integer, ManagedObjectReference>> getAllDiskDatastores() throws Exception {
        List<Pair<Integer, ManagedObjectReference>> disks = new ArrayList<Pair<Integer, ManagedObjectReference>>();

        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    VirtualDeviceBackingInfo backingInfo = ((VirtualDisk)device).getBacking();
                    if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
                        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;
                        disks.add(new Pair<Integer, ManagedObjectReference>(new Integer(device.getKey()), diskBackingInfo.getDatastore()));
                    }
                }
            }
        }

        return disks;
    }


    @Deprecated
    public List<Pair<String, ManagedObjectReference>> getDiskDatastorePathChain(VirtualDisk disk, boolean followChain) throws Exception {
        VirtualDeviceBackingInfo backingInfo = disk.getBacking();
        if (!(backingInfo instanceof VirtualDiskFlatVer2BackingInfo)) {
            throw new Exception("Unsupported VirtualDeviceBackingInfo");
        }

        List<Pair<String, ManagedObjectReference>> pathList = new ArrayList<Pair<String, ManagedObjectReference>>();
        VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;

        if (!followChain) {
            pathList.add(new Pair<String, ManagedObjectReference>(diskBackingInfo.getFileName(), diskBackingInfo.getDatastore()));
            return pathList;
        }

        Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();
        VirtualMachineFileInfo vmFilesInfo = getFileInfo();
        DatastoreFile snapshotDirFile = new DatastoreFile(vmFilesInfo.getSnapshotDirectory());
        DatastoreFile vmxDirFile = new DatastoreFile(vmFilesInfo.getVmPathName());

        do {
            if (diskBackingInfo.getParent() != null) {
                pathList.add(new Pair<String, ManagedObjectReference>(diskBackingInfo.getFileName(), diskBackingInfo.getDatastore()));
                diskBackingInfo = diskBackingInfo.getParent();
            } else {
                // try getting parent info from VMDK file itself
                byte[] content = null;
                try {
                    String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), diskBackingInfo.getFileName());
                    content = getContext().getResourceContent(url);
                    if (content == null || content.length == 0) {
                        break;
                    }

                    pathList.add(new Pair<String, ManagedObjectReference>(diskBackingInfo.getFileName(), diskBackingInfo.getDatastore()));
                } catch (Exception e) {
                    // if snapshot directory has been changed to place other than default. VMware has a bug
                    // that its corresponding disk backing info is not updated correctly. therefore, we will try search
                    // in snapshot directory one more time
                    DatastoreFile currentFile = new DatastoreFile(diskBackingInfo.getFileName());
                    String vmdkFullDsPath = snapshotDirFile.getCompanionPath(currentFile.getFileName());

                    String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), vmdkFullDsPath);
                    content = getContext().getResourceContent(url);
                    if (content == null || content.length == 0) {
                        break;
                    }

                    pathList.add(new Pair<String, ManagedObjectReference>(vmdkFullDsPath, diskBackingInfo.getDatastore()));
                }

                VmdkFileDescriptor descriptor = new VmdkFileDescriptor();
                descriptor.parse(content);
                if (descriptor.getParentFileName() != null && !descriptor.getParentFileName().isEmpty()) {
                    // create a fake one
                    VirtualDiskFlatVer2BackingInfo parentDiskBackingInfo = new VirtualDiskFlatVer2BackingInfo();
                    parentDiskBackingInfo.setDatastore(diskBackingInfo.getDatastore());

                    String parentFileName = descriptor.getParentFileName();
                    if (parentFileName.startsWith("/")) {
                        int fileNameStartPos = parentFileName.lastIndexOf("/");
                        parentFileName = parentFileName.substring(fileNameStartPos + 1);
                        parentDiskBackingInfo.setFileName(vmxDirFile.getCompanionPath(parentFileName));
                    } else {
                        parentDiskBackingInfo.setFileName(snapshotDirFile.getCompanionPath(parentFileName));
                    }
                    diskBackingInfo = parentDiskBackingInfo;
                } else {
                    break;
                }
            }
        } while (diskBackingInfo != null);

        return pathList;
    }

    public String getDeviceBusName(List<VirtualDevice> allDevices, VirtualDevice theDevice) throws Exception {
        for (VirtualDevice device : allDevices) {
            if (device.getKey() == theDevice.getControllerKey().intValue()) {
                if (device instanceof VirtualIDEController) {
                    return String.format("ide%d:%d", ((VirtualIDEController)device).getBusNumber(), theDevice.getUnitNumber());
                } else if (device instanceof VirtualSCSIController) {
                    return String.format("scsi%d:%d", ((VirtualSCSIController)device).getBusNumber(), theDevice.getUnitNumber());
                } else {
                    throw new Exception("Device controller is not supported yet");
                }
            }
        }
        throw new Exception("Unable to find device controller");
    }

    public List<VirtualDisk> getVirtualDisks() throws Exception {
        List<VirtualDisk> virtualDisks = new ArrayList<VirtualDisk>();

        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        for (VirtualDevice device : devices) {
            if (device instanceof VirtualDisk) {
                virtualDisks.add((VirtualDisk)device);
            }
        }

        return virtualDisks;
    }

    public List<String> detachAllDisksExcept(String vmdkBaseName, String deviceBusName) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
        List<String> detachedDiskFiles = new ArrayList<String>();

        for (VirtualDevice device : devices) {
            if (device instanceof VirtualDisk) {
                VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

                VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)device.getBacking();

                DatastoreFile dsBackingFile = new DatastoreFile(diskBackingInfo.getFileName());
                String backingBaseName = dsBackingFile.getFileBaseName();
                String deviceNumbering = getDeviceBusName(devices, device);
                if (backingBaseName.equalsIgnoreCase(vmdkBaseName) || (deviceBusName != null && deviceBusName.equals(deviceNumbering))) {
                    continue;
                } else {
                    s_logger.info("Detach " + diskBackingInfo.getFileName() + " from " + getName());

                    detachedDiskFiles.add(diskBackingInfo.getFileName());

                    deviceConfigSpec.setDevice(device);
                    deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

                    reConfigSpec.getDeviceChange().add(deviceConfigSpec);
                }
            }
        }

        if (detachedDiskFiles.size() > 0) {
            ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
            boolean result = _context.getVimClient().waitForTask(morTask);
            if (result) {
                _context.waitForTaskProgressDone(morTask);
            } else {
                s_logger.warn("Unable to reconfigure the VM to detach disks");
                throw new Exception("Unable to reconfigure the VM to detach disks");
            }
        }

        return detachedDiskFiles;
    }

    public List<VirtualDevice> getAllDeviceList() throws Exception {
        return _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
    }

    public VirtualDisk[] getAllDiskDevice() throws Exception {
        List<VirtualDisk> deviceList = new ArrayList<VirtualDisk>();
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    deviceList.add((VirtualDisk)device);
                }
            }
        }

        return deviceList.toArray(new VirtualDisk[0]);
    }

    public VirtualDisk getDiskDeviceByBusName(List<VirtualDevice> allDevices, String busName) throws Exception {
        for (VirtualDevice device : allDevices) {
            if (device instanceof VirtualDisk) {
                VirtualDisk disk = (VirtualDisk)device;
                String diskBusName = getDeviceBusName(allDevices, disk);
                if (busName.equalsIgnoreCase(diskBusName))
                    return disk;
            }
        }

        return null;
    }

    public VirtualDisk[] getAllIndependentDiskDevice() throws Exception {
        List<VirtualDisk> independentDisks = new ArrayList<VirtualDisk>();
        VirtualDisk[] allDisks = getAllDiskDevice();
        if (allDisks.length > 0) {
            for (VirtualDisk disk : allDisks) {
                String diskMode = "";
                if (disk.getBacking() instanceof VirtualDiskFlatVer1BackingInfo) {
                    diskMode = ((VirtualDiskFlatVer1BackingInfo)disk.getBacking()).getDiskMode();
                } else if (disk.getBacking() instanceof VirtualDiskFlatVer2BackingInfo) {
                    diskMode = ((VirtualDiskFlatVer2BackingInfo)disk.getBacking()).getDiskMode();
                } else if (disk.getBacking() instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
                    diskMode = ((VirtualDiskRawDiskMappingVer1BackingInfo)disk.getBacking()).getDiskMode();
                } else if (disk.getBacking() instanceof VirtualDiskSparseVer1BackingInfo) {
                    diskMode = ((VirtualDiskSparseVer1BackingInfo)disk.getBacking()).getDiskMode();
                } else if (disk.getBacking() instanceof VirtualDiskSparseVer2BackingInfo) {
                    diskMode = ((VirtualDiskSparseVer2BackingInfo)disk.getBacking()).getDiskMode();
                }

                if (diskMode.indexOf("independent") != -1) {
                    independentDisks.add(disk);
                }
            }
        }

        return independentDisks.toArray(new VirtualDisk[0]);
    }

    public int tryGetIDEDeviceControllerKey() throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualIDEController) {
                    return ((VirtualIDEController)device).getKey();
                }
            }
        }

        return -1;
    }

    public int getIDEDeviceControllerKey() throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualIDEController) {
                    return ((VirtualIDEController)device).getKey();
                }
            }
        }

        assert (false);
        throw new Exception("IDE Controller Not Found");
    }

    public int getIDEControllerKey(int ideUnitNumber) throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
            getDynamicProperty(_mor, "config.hardware.device");

        int requiredIdeController = ideUnitNumber / VmwareHelper.MAX_IDE_CONTROLLER_COUNT;

        int ideControllerCount = 0;
        if(devices != null && devices.size() > 0) {
            for(VirtualDevice device : devices) {
                if(device instanceof VirtualIDEController) {
                    if (ideControllerCount == requiredIdeController) {
                        return ((VirtualIDEController)device).getKey();
                    }
                    ideControllerCount++;
                }
            }
        }

        assert(false);
        throw new Exception("IDE Controller Not Found");
    }

    public int getNumberOfIDEDevices() throws Exception {
        int ideDeviceCount = 0;
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualIDEController) {
                    ideDeviceCount += ((VirtualIDEController)device).getDevice().size();
                }
            }
        }
        return ideDeviceCount;
    }

    public int getFreeUnitNumberOnIDEController(int controllerKey) throws Exception {
        int freeUnitNumber = 0;
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        int deviceCount = 0;
        int ideDeviceUnitNumber = -1;
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk && (controllerKey == device.getControllerKey())) {
                    deviceCount++;
                    ideDeviceUnitNumber = device.getUnitNumber();
                }
            }
        }
        if (deviceCount == 1) {
            if (ideDeviceUnitNumber == 0) {
                freeUnitNumber = 1;
            } // else freeUnitNumber is already initialized to 0
        } else if (deviceCount == 2) {
            throw new Exception("IDE controller with key [" + controllerKey + "] already has 2 device attached. Cannot attach more than the limit of 2.");
        }
        return freeUnitNumber;
    }
    public int getNextIDEDeviceNumber() throws Exception {
        int controllerKey = getIDEDeviceControllerKey();
        return getNextDeviceNumber(controllerKey);
    }

    public VirtualDevice getIsoDevice() throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualCdrom) {
                    return device;
                }
            }
        }
        return null;
    }

    public VirtualDevice getIsoDevice(int key) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualCdrom && device.getKey() == key) {
                    return device;
                }
            }
        }
        return null;
    }

    public VirtualDevice getIsoDevice(String filename) throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");
        if(devices != null && devices.size() > 0) {
            long isoDevices = devices.stream()
                    .filter(x -> x instanceof VirtualCdrom && x.getBacking() instanceof VirtualCdromIsoBackingInfo)
                    .count();
            for(VirtualDevice device : devices) {
                if(device instanceof VirtualCdrom && device.getBacking() instanceof VirtualCdromIsoBackingInfo) {
                    if (((VirtualCdromIsoBackingInfo)device.getBacking()).getFileName().equals(filename)) {
                        return device;
                    } else if (isoDevices == 1L){
                        s_logger.warn(String.format("VM ISO filename %s differs from the expected filename %s",
                                ((VirtualCdromIsoBackingInfo)device.getBacking()).getFileName(), filename));
                        return device;
                    }
                }
            }
        }
        return null;
    }

    public int getNextDeviceNumber(int controllerKey) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        List<Integer> existingUnitNumbers = new ArrayList<Integer>();
        int deviceNumber = 0;
        int scsiControllerKey = getGenericScsiDeviceControllerKeyNoException();
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device.getControllerKey() != null && device.getControllerKey().intValue() == controllerKey) {
                    existingUnitNumbers.add(device.getUnitNumber());
                }
            }
        }
        while (true) {
            // Next device number should be the lowest device number on the key that is not in use and is not reserved.
            if (!existingUnitNumbers.contains(Integer.valueOf(deviceNumber))) {
                if (controllerKey != scsiControllerKey || !VmwareHelper.isReservedScsiDeviceNumber(deviceNumber))
                    break;
            }
            ++deviceNumber;
        }
        return deviceNumber;
    }

    private List<VirtualDevice> getNicDevices(boolean sorted) throws Exception {
        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        List<VirtualDevice> nics = new ArrayList<VirtualDevice>();
        if (devices != null) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualEthernetCard) {
                    nics.add(device);
                }
            }
        }

        if (sorted) {
            Collections.sort(nics, new Comparator<VirtualDevice>() {
                @Override
                public int compare(VirtualDevice arg0, VirtualDevice arg1) {
                    int unitNumber0 = arg0.getUnitNumber() != null ? arg0.getUnitNumber().intValue() : -1;
                    int unitNumber1 = arg1.getUnitNumber() != null ? arg1.getUnitNumber().intValue() : -1;
                    if (unitNumber0 < unitNumber1)
                        return -1;
                    else if (unitNumber0 > unitNumber1)
                        return 1;
                    return 0;
                }
            });
        }

        return nics;
    }

    public VirtualDevice[] getNicDevices() throws Exception {
        return getNicDevices(false).toArray(new VirtualDevice[0]);
    }

    public VirtualDevice getNicDeviceByIndex(int index) throws Exception {
        List<VirtualDevice> nics = getNicDevices(true);
        try {
            return nics.get(index);
        } catch (IndexOutOfBoundsException e) {
            // Not found
            return null;
        }
    }

    public Pair<Integer, VirtualDevice> getNicDeviceIndex(String networkNamePrefix) throws Exception {
        List<VirtualDevice> nics = getNicDevices(true);

        int index = 0;
        String attachedNetworkSummary;
        String dvPortGroupName;
        for (VirtualDevice nic : nics) {
            attachedNetworkSummary = ((VirtualEthernetCard)nic).getDeviceInfo().getSummary();
            if (attachedNetworkSummary.startsWith(networkNamePrefix)) {
                return new Pair<Integer, VirtualDevice>(new Integer(index), nic);
            } else if (attachedNetworkSummary.endsWith("DistributedVirtualPortBackingInfo.summary") || attachedNetworkSummary.startsWith("DVSwitch")) {
                dvPortGroupName = getDvPortGroupName((VirtualEthernetCard)nic);
                if (dvPortGroupName != null && dvPortGroupName.startsWith(networkNamePrefix)) {
                    s_logger.debug("Found a dvPortGroup already associated with public NIC.");
                    return new Pair<Integer, VirtualDevice>(new Integer(index), nic);
                }
            }
            index++;
        }
        return new Pair<Integer, VirtualDevice>(new Integer(-1), null);
    }

    public String getDvPortGroupName(VirtualEthernetCard nic) throws Exception {
        VirtualEthernetCardDistributedVirtualPortBackingInfo dvpBackingInfo = (VirtualEthernetCardDistributedVirtualPortBackingInfo)nic.getBacking();
        DistributedVirtualSwitchPortConnection dvsPort = dvpBackingInfo.getPort();
        String dvPortGroupKey = dvsPort.getPortgroupKey();
        ManagedObjectReference dvPortGroupMor = new ManagedObjectReference();
        dvPortGroupMor.setValue(dvPortGroupKey);
        dvPortGroupMor.setType("DistributedVirtualPortgroup");
        return (String)_context.getVimClient().getDynamicProperty(dvPortGroupMor, "name");
    }

    public VirtualDevice[] getMatchedDevices(Class<?>[] deviceClasses) throws Exception {
        assert (deviceClasses != null);

        List<VirtualDevice> returnList = new ArrayList<VirtualDevice>();

        List<VirtualDevice> devices = _context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null) {
            for (VirtualDevice device : devices) {
                for (Class<?> clz : deviceClasses) {
                    if (clz.isInstance(device)) {
                        returnList.add(device);
                        break;
                    }
                }
            }
        }

        return returnList.toArray(new VirtualDevice[0]);
    }

    public void mountToolsInstaller() throws Exception {
        _context.getService().mountToolsInstaller(_mor);
    }

    public boolean unmountToolsInstaller() throws Exception {
        // Monitor VM questions
        final Boolean[] flags = {false};
        final VirtualMachineMO vmMo = this;
        final boolean[] encounterQuestion = new boolean[1];
        encounterQuestion[0] = false;
        Future<?> future = MonitorServiceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                s_logger.info("VM Question monitor started...");

                while (!flags[0]) {
                    try {
                        VirtualMachineRuntimeInfo runtimeInfo = vmMo.getRuntimeInfo();
                        VirtualMachineQuestionInfo question = runtimeInfo.getQuestion();
                        if (question != null) {
                            encounterQuestion[0] = true;
                            if (s_logger.isTraceEnabled()) {
                                s_logger.trace("Question id: " + question.getId());
                                s_logger.trace("Question text: " + question.getText());
                            }

                            if (question.getMessage() != null) {
                                for (VirtualMachineMessage msg : question.getMessage()) {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("msg id: " + msg.getId());
                                        s_logger.trace("msg text: " + msg.getText());
                                    }
                                    if ("msg.cdromdisconnect.locked".equalsIgnoreCase(msg.getId())) {
                                        s_logger.info("Found that VM has a pending question that we need to answer programmatically, question id: " + msg.getId() +
                                                ", for safe operation we will automatically decline it");
                                        vmMo.answerVM(question.getId(), ANSWER_NO);
                                        break;
                                    }
                                }
                            } else if (question.getText() != null) {
                                String text = question.getText();
                                String msgId;
                                String msgText;
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("question text : " + text);
                                }
                                String[] tokens = text.split(":");
                                msgId = tokens[0];
                                msgText = tokens[1];
                                if ("msg.cdromdisconnect.locked".equalsIgnoreCase(msgId)) {
                                    s_logger.info("Found that VM has a pending question that we need to answer programmatically, question id: " + question.getId() +
                                            ". Message id : " + msgId + ". Message text : " + msgText + ", for safe operation we will automatically decline it.");
                                    vmMo.answerVM(question.getId(), ANSWER_NO);
                                }
                            }

                            ChoiceOption choice = question.getChoice();
                            if (choice != null) {
                                for (ElementDescription info : choice.getChoiceInfo()) {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("Choice option key: " + info.getKey());
                                        s_logger.trace("Choice option label: " + info.getLabel());
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        s_logger.error("Unexpected exception: ", e);
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        s_logger.debug("[ignored] interupted while handling vm question about umount tools install.");
                    }
                }

                s_logger.info("VM Question monitor stopped");
            }
        });

        try {
            _context.getService().unmountToolsInstaller(_mor);
        } finally {
            flags[0] = true;
            future.cancel(true);
        }
        if (encounterQuestion[0]) {
            s_logger.warn("cdrom is locked by VM. Failed to detach the ISO.");
            return false;
        } else {
            s_logger.info("Successfully unmounted tools installer from VM.");
            return true;
        }
    }

    public void redoRegistration(ManagedObjectReference morHost) throws Exception {
        String vmName = getVmName();
        VirtualMachineFileInfo vmFileInfo = getFileInfo();
        boolean isTemplate = isTemplate();

        HostMO hostMo;
        if (morHost != null)
            hostMo = new HostMO(getContext(), morHost);
        else
            hostMo = getRunningHost();

        ManagedObjectReference morFolder = getParentMor();
        ManagedObjectReference morPool = hostMo.getHyperHostOwnerResourcePool();

        _context.getService().unregisterVM(_mor);

        ManagedObjectReference morTask = _context.getService().registerVMTask(morFolder, vmFileInfo.getVmPathName(), vmName, false, morPool, hostMo.getMor());

        boolean result = _context.getVimClient().waitForTask(morTask);
        if (!result) {
            throw new Exception("Unable to register template due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        } else {
            _context.waitForTaskProgressDone(morTask);
            if (isTemplate) {
                VirtualMachineMO vmNewRegistration = hostMo.findVmOnHyperHost(vmName);
                assert (vmNewRegistration != null);
                vmNewRegistration.markAsTemplate();
            }
        }
    }

    public long getHotAddMemoryIncrementSizeInMb() throws Exception {
        return (Long)_context.getVimClient().getDynamicProperty(_mor, "config.hotPlugMemoryIncrementSize");
    }

    public long getHotAddMemoryLimitInMb() throws Exception {
        return (Long)_context.getVimClient().getDynamicProperty(_mor, "config.hotPlugMemoryLimit");
    }
    public String getGuestId() throws Exception {
        return (String)_context.getVimClient().getDynamicProperty(_mor, "config.guestId");
    }

    public int getCoresPerSocket() throws Exception {
        // number of cores per socket is 1 in case of ESXi. It's not defined explicitly and the property is support since vSphere API 5.0.
        String apiVersion = HypervisorHostHelper.getVcenterApiVersion(_context);
        if (apiVersion.compareTo("5.0") < 0) {
            return 1;
        }
        Integer coresPerSocket = (Integer)_context.getVimClient().getDynamicProperty(_mor, "config.hardware.numCoresPerSocket");
        return coresPerSocket != null ? coresPerSocket : 1;
    }

    public int getVirtualHardwareVersion() throws Exception {
        VirtualHardwareOption vhOption = getVirtualHardwareOption();
        return vhOption.getHwVersion();
    }

    public VirtualHardwareOption getVirtualHardwareOption() throws Exception {
        VirtualMachineConfigOption vmConfigOption = _context.getService().queryConfigOption(getEnvironmentBrowser(), null, null);
        return vmConfigOption.getHardwareOptions();
    }

    private ManagedObjectReference getEnvironmentBrowser() throws Exception {
        if (_vmEnvironmentBrowser == null) {
            _vmEnvironmentBrowser = _context.getVimClient().getMoRefProp(_mor, "environmentBrowser");
        }
        return _vmEnvironmentBrowser;
    }

    public boolean isCpuHotAddSupported(String guestOsId) throws Exception {
        boolean guestOsSupportsCpuHotAdd = false;
        boolean virtualHardwareSupportsCpuHotAdd = false;
        GuestOsDescriptor guestOsDescriptor;
        int virtualHardwareVersion;
        int numCoresPerSocket;

        guestOsDescriptor = getGuestOsDescriptor(guestOsId);
        virtualHardwareVersion = getVirtualHardwareVersion();

        // Check if guest operating system supports cpu hotadd
        if (guestOsDescriptor.isSupportsCpuHotAdd()) {
            guestOsSupportsCpuHotAdd = true;
        }

        // Check if virtual machine is using hardware version 8 or later.
        // If hardware version is 7, then only 1 core per socket is supported. Hot adding multi-core vcpus is not allowed if hardware version is 7.
        if (virtualHardwareVersion >= 8) {
            virtualHardwareSupportsCpuHotAdd = true;
        } else if (virtualHardwareVersion == 7) {
            // Check if virtual machine has only 1 core per socket.
            numCoresPerSocket = getCoresPerSocket();
            if (numCoresPerSocket == 1) {
                virtualHardwareSupportsCpuHotAdd = true;
            }
        }
        return guestOsSupportsCpuHotAdd && virtualHardwareSupportsCpuHotAdd;
    }

    public boolean isMemoryHotAddSupported(String guestOsId) throws Exception {
        boolean guestOsSupportsMemoryHotAdd = false;
        boolean virtualHardwareSupportsMemoryHotAdd = false;
        GuestOsDescriptor guestOsDescriptor;
        int virtualHardwareVersion;

        guestOsDescriptor = getGuestOsDescriptor(guestOsId);
        virtualHardwareVersion = getVirtualHardwareVersion();

        // Check if guest operating system supports memory hotadd
        if (guestOsDescriptor != null && guestOsDescriptor.isSupportsMemoryHotAdd()) {
            guestOsSupportsMemoryHotAdd = true;
        }
        // Check if virtual machine is using hardware version 7 or later.
        if (virtualHardwareVersion >= 7) {
            virtualHardwareSupportsMemoryHotAdd = true;
        }
        return guestOsSupportsMemoryHotAdd && virtualHardwareSupportsMemoryHotAdd;
    }
    public void ensureLsiLogicSasDeviceControllers(int count, int availableBusNum) throws Exception {
        int scsiControllerKey = getLsiLogicSasDeviceControllerKeyNoException();
        if (scsiControllerKey < 0) {
            VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();

            int busNum = availableBusNum;
            while (busNum < count) {
                VirtualLsiLogicSASController scsiController = new VirtualLsiLogicSASController();
                scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
                scsiController.setBusNumber(busNum);
                scsiController.setKey(busNum - VmwareHelper.MAX_SCSI_CONTROLLER_COUNT);
                VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
                scsiControllerSpec.setDevice(scsiController);
                scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

                vmConfig.getDeviceChange().add(scsiControllerSpec);
                busNum++;
            }
            if (configureVm(vmConfig)) {
                throw new Exception("Unable to add Scsi controller of type LsiLogic SAS.");
            }
        }

    }

    private int getLsiLogicSasDeviceControllerKeyNoException() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualLsiLogicSASController) {
                    return device.getKey();
                }
            }
        }

        return -1;
    }

    public void ensureBusLogicDeviceControllers(int count, int availableBusNum) throws Exception {
        int scsiControllerKey = getBusLogicDeviceControllerKeyNoException();
        if (scsiControllerKey < 0) {
            VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();

            int busNum = availableBusNum;
            while (busNum < count) {
                VirtualBusLogicController scsiController = new VirtualBusLogicController();

                scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
                scsiController.setBusNumber(busNum);
                scsiController.setKey(busNum - VmwareHelper.MAX_SCSI_CONTROLLER_COUNT);
                VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
                scsiControllerSpec.setDevice(scsiController);
                scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

                vmConfig.getDeviceChange().add(scsiControllerSpec);
                busNum++;
            }

            if (configureVm(vmConfig)) {
                throw new Exception("Unable to add Scsi BusLogic controllers to the VM " + getName());
            } else {
                s_logger.info("Successfully added " + count + " SCSI BusLogic controllers.");
            }
        }
    }

    private int getBusLogicDeviceControllerKeyNoException() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualBusLogicController) {
                    return device.getKey();
                }
            }
        }

        return -1;
    }

    public Ternary<Integer, Integer, DiskControllerType> getScsiControllerInfo() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
                getDynamicProperty(_mor, "config.hardware.device");

        int scsiControllerCount = 0;
        int busNum = -1;
        DiskControllerType controllerType = DiskControllerType.lsilogic;
        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualSCSIController) {
                    scsiControllerCount++;
                    int deviceBus = ((VirtualSCSIController)device).getBusNumber();
                    if (busNum < deviceBus) {
                        busNum = deviceBus;
                    }
                    if (device instanceof VirtualLsiLogicController) {
                        controllerType = DiskControllerType.lsilogic;
                    } else if (device instanceof VirtualLsiLogicSASController) {
                        controllerType = DiskControllerType.lsisas1068;
                    } else if (device instanceof VirtualBusLogicController) {
                        controllerType = DiskControllerType.buslogic;
                    } else if (device instanceof ParaVirtualSCSIController) {
                        controllerType = DiskControllerType.pvscsi;
                    }
                }
            }
        }

        return new Ternary<Integer, Integer, DiskControllerType>(scsiControllerCount, busNum, controllerType);
    }

    public int getNumberOfVirtualDisks() throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

        s_logger.info("Counting disk devices attached to VM " + getVmName());
        int count = 0;

        if (devices != null && devices.size() > 0) {
            for (VirtualDevice device : devices) {
                if (device instanceof VirtualDisk) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean consolidateVmDisks() throws Exception {
        ManagedObjectReference morTask = _context.getService().consolidateVMDisksTask(_mor);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware ConsolidateVMDisks_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
        return false;
    }
}
