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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.mo.SnapshotDescriptor.SnapshotInfo;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.utils.ActionDelegate;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.script.Script;
import com.google.gson.Gson;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.CustomFieldStringValue;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestInfo;
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
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.SelectionSpec;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
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
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateDiskMoveOptions;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRelocateSpecDiskLocator;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VirtualPCIController;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;

import edu.emory.mathcs.backport.java.util.Arrays;

public class VirtualMachineMO extends BaseMO {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineMO.class);

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

	    List<ObjectContent> ocs = _context.getService().retrieveProperties(
	    	_context.getPropertyCollector(), pfSpecArr);


	    if(ocs != null) {
	    	for(ObjectContent oc : ocs) {
	    		DynamicProperty prop = oc.getPropSet().get(0);
	    		if(prop.getVal().toString().equals(dsName)) {
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

	public boolean isVMwareToolsRunning() throws Exception {
		GuestInfo guestInfo = getVmGuestInfo();
		if(guestInfo != null) {
			if("guestToolsRunning".equalsIgnoreCase(guestInfo.getToolsRunningStatus()))
				return true;
		}
		return false;
	}

	public boolean powerOn() throws Exception {
		if(getPowerState() == VirtualMachinePowerState.POWERED_ON)
			return true;

		ManagedObjectReference morTask = _context.getService().powerOnVMTask(_mor, null);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware powerOnVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}

		return false;
	}

	public boolean powerOff() throws Exception {
		if(getPowerState() == VirtualMachinePowerState.POWERED_OFF)
			return true;

		return powerOffNoCheck();
	}

	public boolean safePowerOff(int shutdownWaitMs) throws Exception {

		if(getPowerState() == VirtualMachinePowerState.POWERED_OFF)
			return true;

		if(isVMwareToolsRunning()) {
			try {
				String vmName = this.getName();

				s_logger.info("Try gracefully shut down VM " + vmName);
				shutdown();

				long startTick = System.currentTimeMillis();
				while(getPowerState() != VirtualMachinePowerState.POWERED_OFF && System.currentTimeMillis() - startTick < shutdownWaitMs) {
					try {
						Thread.sleep(1000);
					} catch(InterruptedException e) {
					}
				}

				if(getPowerState() != VirtualMachinePowerState.POWERED_OFF) {
					s_logger.info("can not gracefully shutdown VM within " + (shutdownWaitMs/1000) + " seconds, we will perform force power off on VM " + vmName);
					return powerOffNoCheck();
				}

				return true;
			} catch(Exception e) {
				s_logger.warn("Failed to do guest-os graceful shutdown due to " + VmwareHelper.getExceptionMessage(e));
			}
		}

		return powerOffNoCheck();
	}

	private boolean powerOffNoCheck() throws Exception {
		ManagedObjectReference morTask = _context.getService().powerOffVMTask(_mor);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);

			 // It seems that even if a power-off task is returned done, VM state may still not be marked,
			// wait up to 5 seconds to make sure to avoid race conditioning for immediate following on operations
			// that relies on a powered-off VM
			long startTick = System.currentTimeMillis();
			while(getPowerState() != VirtualMachinePowerState.POWERED_OFF && System.currentTimeMillis() - startTick < 5000) {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
				}
			}
			return true;
		} else {
			 if(getPowerState() == VirtualMachinePowerState.POWERED_OFF) {
				 // to help deal with possible race-condition
				 s_logger.info("Current power-off task failed. However, VM has been switched to the state we are expecting for");
				 return true;
			 }

        	s_logger.error("VMware powerOffVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}

		return false;
	}

	public VirtualMachinePowerState getPowerState() throws Exception {

	    VirtualMachinePowerState powerState = VirtualMachinePowerState.POWERED_OFF;

	    // This is really ugly, there is a case that when windows guest VM is doing sysprep, the temporary
	    // rebooting process may let us pick up a "poweredOff" state during VMsync process, this can trigger
	    // a series actions. Unfortunately, from VMware API we can not distinguish power state into such details.
	    // We hope by giving it 3 second to re-read the state can cover this as a short-term solution.
	    //
	    // In the future, VMsync should not kick off CloudStack action (this is not a HA case) based on VM
	    // state report, until then we can remove this hacking fix
	    for(int i = 0; i < 3; i++) {
	        powerState = (VirtualMachinePowerState)getContext().getVimClient().getDynamicProperty(_mor, "runtime.powerState");
	        if(powerState == VirtualMachinePowerState.POWERED_OFF) {
	            try {
	                Thread.sleep(1000);
	            } catch(InterruptedException e) {
	            }
	        } else {
	            break;
	        }
	    }

	    return powerState;
	}

	public boolean reset() throws Exception {
		ManagedObjectReference morTask = _context.getService().resetVMTask(_mor);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
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
		VirtualMachineConfigInfo configInfo = this.getConfigInfo();
		return configInfo.isTemplate();
	}

	public boolean migrate(ManagedObjectReference morRp, ManagedObjectReference morTargetHost) throws Exception {
		ManagedObjectReference morTask = _context.getService().migrateVMTask(_mor,
			morRp, morTargetHost, VirtualMachineMovePriority.DEFAULT_PRIORITY, null);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware migrateVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}

		return false;
	}

	public boolean relocate(ManagedObjectReference morTargetHost) throws Exception {
	    VirtualMachineRelocateSpec relocateSpec = new VirtualMachineRelocateSpec();
	    relocateSpec.setHost(morTargetHost);

        ManagedObjectReference morTask = _context.getService().relocateVMTask(_mor,
            relocateSpec, null);

        boolean result = _context.getVimClient().waitForTask(morTask);
        if(result) {
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

	public boolean createSnapshot(String snapshotName, String snapshotDescription,
		boolean dumpMemory, boolean quiesce) throws Exception {

		ManagedObjectReference morTask = _context.getService().createSnapshotTask(_mor, snapshotName,
			snapshotDescription, dumpMemory, quiesce);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);

			ManagedObjectReference morSnapshot = null;
			// We still need to wait until the object appear in vCenter
			long startTick = System.currentTimeMillis();
			while(System.currentTimeMillis() - startTick < 10000) {
				morSnapshot = getSnapshotMor(snapshotName);
				if(morSnapshot != null) {
					break;
				}

				try { Thread.sleep(1000); } catch(InterruptedException e) {}
			}

			if(morSnapshot == null)
				s_logger.error("We've been waiting for over 10 seconds for snapshot MOR to be appearing in vCenter after CreateSnapshot task is done, but it is still not there?!");

			return true;
		} else {
        	s_logger.error("VMware createSnapshot_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}

		return false;
	}

	public boolean removeSnapshot(String snapshotName, boolean removeChildren) throws Exception {
		ManagedObjectReference morSnapshot = getSnapshotMor(snapshotName);
		if(morSnapshot == null) {
			s_logger.warn("Unable to find snapshot: " + snapshotName);
			return false;
		}

		ManagedObjectReference morTask = _context.getService().removeSnapshotTask(morSnapshot, removeChildren, true);
		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
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
        ManagedObjectReference morTask = _context.getService()
                .revertToSnapshotTask(morSnapshot, _mor, null);
        boolean result = _context.getVimClient().waitForTask(morTask);
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            return true;
        } else {
            s_logger.error("VMware revert to snapshot failed due to "
                    + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return false;
    }

	public boolean removeAllSnapshots() throws Exception {
		VirtualMachineSnapshotInfo snapshotInfo = getSnapshotInfo();

		if(snapshotInfo != null && snapshotInfo.getRootSnapshotList() != null) {
			List<VirtualMachineSnapshotTree> tree = snapshotInfo.getRootSnapshotList();
			for(VirtualMachineSnapshotTree treeNode : tree) {
				ManagedObjectReference morTask = _context.getService().removeSnapshotTask(treeNode.getSnapshot(), true, true);
				boolean result = _context.getVimClient().waitForTask(morTask);
				if(result) {
					_context.waitForTaskProgressDone(morTask);
				} else {
		        	s_logger.error("VMware removeSnapshot_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		        	return false;
				}
			}
		}

		return true;
	}

	public String getSnapshotDiskFileDatastorePath(VirtualMachineFileInfo vmFileInfo,
		List<Pair<ManagedObjectReference, String>> datastoreMounts,
		String snapshotDiskFile) throws Exception {

		// if file path start with "/", need to search all datastore mounts on the host in order
		// to form fully qualified datastore path
		if(snapshotDiskFile.startsWith("/")) {
			for(Pair<ManagedObjectReference, String> mount: datastoreMounts) {
				if(snapshotDiskFile.startsWith(mount.second())) {
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
		assert(dsPath != null);
		String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), dsPath);
		byte[] content = getContext().getResourceContent(url);

		if(content == null || content.length < 1) {
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

	    List<ObjectContent> ocs = _context.getService().retrieveProperties(
	    	_context.getPropertyCollector(), pfSpecArr);
	    assert(ocs != null);

    	String vmName = null;
    	VirtualMachineFileInfo fileInfo = null;

    	assert(ocs.size() == 1);
    	for(ObjectContent oc : ocs) {
    		List<DynamicProperty> props = oc.getPropSet();
    		if(props != null) {
    			assert(props.size() == 2);

    			for(DynamicProperty prop : props) {
    				if(prop.getName().equals("name")) {
    					vmName = prop.getVal().toString();
    				} else {
    					fileInfo = (VirtualMachineFileInfo)prop.getVal();
    				}
    			}
    		}
    	}
    	assert(vmName != null);
    	assert(fileInfo != null);

    	// .vmsd file exists at the same directory of .vmx file
    	DatastoreFile vmxFile = new DatastoreFile(fileInfo.getVmPathName());
    	return vmxFile.getCompanionPath(vmName + ".vmsd");
	}

	public ManagedObjectReference getSnapshotMor(String snapshotName) throws Exception {
		VirtualMachineSnapshotInfo info = getSnapshotInfo();
		if(info != null) {
	         List<VirtualMachineSnapshotTree> snapTree = info.getRootSnapshotList();
	         return VmwareHelper.findSnapshotInTree(snapTree, snapshotName);
		}
		return null;
	}

	public boolean createFullClone(String cloneName, ManagedObjectReference morFolder, ManagedObjectReference morResourcePool,
		ManagedObjectReference morDs) throws Exception {

     	VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
     	VirtualMachineRelocateSpec relocSpec = new VirtualMachineRelocateSpec();
     	cloneSpec.setLocation(relocSpec);
     	cloneSpec.setPowerOn(false);
     	cloneSpec.setTemplate(false);

     	relocSpec.setDatastore(morDs);
     	relocSpec.setPool(morResourcePool);
        ManagedObjectReference morTask = _context.getService().cloneVMTask(_mor, morFolder, cloneName, cloneSpec);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware cloneVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}

		return false;
	}

	public boolean createLinkedClone(String cloneName, ManagedObjectReference morBaseSnapshot,
		ManagedObjectReference morFolder, ManagedObjectReference morResourcePool,
		ManagedObjectReference morDs) throws Exception {

		assert(morBaseSnapshot != null);
		assert(morFolder != null);
		assert(morResourcePool != null);
		assert(morDs != null);

		VirtualDisk[] independentDisks = getAllIndependentDiskDevice();
        VirtualMachineRelocateSpec rSpec = new VirtualMachineRelocateSpec();
        if(independentDisks.length > 0) {
            List<VirtualMachineRelocateSpecDiskLocator> diskLocator = new ArrayList<VirtualMachineRelocateSpecDiskLocator>(independentDisks.length);
            for(int i = 0; i < independentDisks.length; i++) {
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
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware cloneVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}

		return false;
	}

	public VirtualMachineRuntimeInfo getRuntimeInfo() throws Exception {
		return (VirtualMachineRuntimeInfo)_context.getVimClient().getDynamicProperty(
			_mor, "runtime");
	}

	public VirtualMachineConfigInfo getConfigInfo() throws Exception {
		return (VirtualMachineConfigInfo)_context.getVimClient().getDynamicProperty(
			_mor, "config");
	}

	public VirtualMachineConfigSummary getConfigSummary() throws Exception {
		return (VirtualMachineConfigSummary)_context.getVimClient().getDynamicProperty(
			_mor, "summary.config");
	}

	public VirtualMachineFileInfo getFileInfo() throws Exception {
		return (VirtualMachineFileInfo)_context.getVimClient().getDynamicProperty(
			_mor, "config.files");
	}

	public ManagedObjectReference getParentMor() throws Exception {
		return (ManagedObjectReference)_context.getVimClient().getDynamicProperty(
			_mor, "parent");
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

	    List<ObjectContent> ocs = _context.getService().retrieveProperties(
	    	_context.getPropertyCollector(), pfSpecArr);

	    List<String> networks = new ArrayList<String>();
	    if(ocs != null && ocs.size() > 0) {
	    	for(ObjectContent oc : ocs) {
	    		networks.add(oc.getPropSet().get(0).getVal().toString());
	    	}
	    }
	    return networks.toArray(new String[0]);
	}

	public List<NetworkDetails> getNetworksWithDetails() throws Exception {
		List<NetworkDetails> networks = new ArrayList<NetworkDetails>();

		int gcTagKey = getCustomFieldKey("Network", CustomFieldConstants.CLOUD_GC);

		if(gcTagKey == 0) {
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

	    List<ObjectContent> ocs = _context.getService().retrieveProperties(
	    	_context.getPropertyCollector(), pfSpecArr);


	    if(ocs != null && ocs.size() > 0) {
	    	for(ObjectContent oc : ocs) {
	    		ArrayOfManagedObjectReference morVms = null;
	    		String gcTagValue = null;
	    		String name = null;

	    		for(DynamicProperty prop : oc.getPropSet()) {
	    			if(prop.getName().equals("name"))
	    				name = prop.getVal().toString();
	    			else if(prop.getName().equals("vm"))
	    	    		morVms = (ArrayOfManagedObjectReference)prop.getVal();
	    			else if(prop.getName().startsWith("value[")) {
		    			CustomFieldStringValue val = (CustomFieldStringValue)prop.getVal();
		    			if(val != null)
		    				gcTagValue = val.getValue();
	    			}
	    		}

	    		NetworkDetails details = new NetworkDetails(name, oc.getObj(),
	    			(morVms != null ? morVms.getManagedObjectReference().toArray(new ManagedObjectReference[morVms.getManagedObjectReference().size()]) : null),
	    			gcTagValue);

	    		networks.add(details);
	    	}
	    	s_logger.debug("Retrieved " + networks.size() + " networks with key : " + gcTagKey);
	    }

		return networks;
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
		assert(vmxPathTokens.length == 4);
		pathInfo[1] = vmxPathTokens[1].trim();							// vSphere vm name
		pathInfo[2] = dcInfo.second();									// vSphere datacenter name
		pathInfo[3] = vmxPathTokens[0].trim();							// vSphere datastore name
		return pathInfo;
	}

	public String getVmxHttpAccessUrl() throws Exception {
		Pair<DatacenterMO, String> dcInfo = getOwnerDatacenter();

		VirtualMachineFileInfo fileInfo = getFileInfo();
		String vmxFilePath = fileInfo.getVmPathName();
		String vmxPathTokens[] = vmxFilePath.split("\\[|\\]|/");

		StringBuffer sb = new StringBuffer("https://" + _context.getServerAddress() + "/folder/");
		sb.append(URLEncoder.encode(vmxPathTokens[2].trim()));
		sb.append("/");
		sb.append(URLEncoder.encode(vmxPathTokens[3].trim()));
		sb.append("?dcPath=");
		sb.append(URLEncoder.encode(dcInfo.second()));
		sb.append("&dsName=");
		sb.append(URLEncoder.encode(vmxPathTokens[1].trim()));

		return sb.toString();
	}

	public boolean setVncConfigInfo(boolean enableVnc, String vncPassword, int vncPort, String keyboard) throws Exception {
		VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
		OptionValue[] vncOptions = VmwareHelper.composeVncOptions(null, enableVnc, vncPassword, vncPort, keyboard);
		vmConfigSpec.getExtraConfig().addAll(Arrays.asList(vncOptions));
    	ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, vmConfigSpec);

  		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
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
		if(result) {
			_context.waitForTaskProgressDone(morTask);
			return true;
		} else {
        	s_logger.error("VMware reconfigVM_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		}
		return false;
	}

	public boolean configureVm(Ternary<VirtualDevice, VirtualDeviceConfigSpecOperation,
		VirtualDeviceConfigSpecFileOperation>[] devices) throws Exception {

		assert(devices != null);

		VirtualMachineConfigSpec configSpec = new VirtualMachineConfigSpec();
	    VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[devices.length];
	    int i = 0;
	    for(Ternary<VirtualDevice, VirtualDeviceConfigSpecOperation, VirtualDeviceConfigSpecFileOperation> deviceTernary: devices) {
	    	VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
	    	deviceConfigSpec.setDevice(deviceTernary.first());
	    	deviceConfigSpec.setOperation(deviceTernary.second());
	    	deviceConfigSpec.setFileOperation(deviceTernary.third());
	    	deviceConfigSpecArray[i++] = deviceConfigSpec;
	    }
	    configSpec.getDeviceChange().addAll(Arrays.asList(deviceConfigSpecArray));

    	ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, configSpec);

  		boolean result = _context.getVimClient().waitForTask(morTask);
		if(result) {
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

		if(values != null) {
			for(OptionValue option : values) {
				if(option.getKey().equals("RemoteDisplay.vnc.port")) {
					String value = (String)option.getValue();
					if(value != null) {
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
	public void createDisk(String vmdkDatastorePath, VirtualDiskType diskType, VirtualDiskMode diskMode,
		String rdmDeviceName, int sizeInMb, ManagedObjectReference morDs, int controllerKey) throws Exception {

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - createDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: " + vmdkDatastorePath
				+ ", sizeInMb: " + sizeInMb + ", diskType: " + diskType + ", diskMode: " + diskMode + ", rdmDeviceName: " + rdmDeviceName
				+ ", datastore: " + morDs.getValue() + ", controllerKey: " + controllerKey);

		assert(vmdkDatastorePath != null);
		assert(morDs != null);

		if(controllerKey < 0) {
            controllerKey = getIDEDeviceControllerKey();
        }

		VirtualDisk newDisk = new VirtualDisk();
		if(diskType == VirtualDiskType.THIN || diskType == VirtualDiskType.PREALLOCATED
			|| diskType == VirtualDiskType.EAGER_ZEROED_THICK) {

			VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
	        backingInfo.setDiskMode(diskMode.PERSISTENT.value());
	        if(diskType == VirtualDiskType.THIN) {
                backingInfo.setThinProvisioned(true);
            } else {
                backingInfo.setThinProvisioned(false);
            }

	        if(diskType == VirtualDiskType.EAGER_ZEROED_THICK) {
                backingInfo.setEagerlyScrub(true);
            } else {
                backingInfo.setEagerlyScrub(false);
            }

	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        newDisk.setBacking(backingInfo);
		} else if(diskType == VirtualDiskType.RDM || diskType == VirtualDiskType.RDMP) {
			VirtualDiskRawDiskMappingVer1BackingInfo backingInfo =
				new VirtualDiskRawDiskMappingVer1BackingInfo();
			if(diskType == VirtualDiskType.RDM) {
				backingInfo.setCompatibilityMode("virtualMode");
	        } else {
	        	backingInfo.setCompatibilityMode("physicalMode");
	        }
	        backingInfo.setDeviceName(rdmDeviceName);
	        if(diskType == VirtualDiskType.RDM) {
	        	backingInfo.setDiskMode(diskMode.PERSISTENT.value());
	        }

	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        newDisk.setBacking(backingInfo);
		}

		int deviceNumber = getNextDeviceNumber(controllerKey);

		newDisk.setControllerKey(controllerKey);
	    newDisk.setKey(-deviceNumber);
	    newDisk.setUnitNumber(deviceNumber);
	    newDisk.setCapacityInKB(sizeInMb*1024);

	    VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
	    //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
	    VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

	    deviceConfigSpec.setDevice(newDisk);
	    deviceConfigSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.CREATE);
	    deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

	    //deviceConfigSpecArray[0] = deviceConfigSpec;
	    reConfigSpec.getDeviceChange().add(deviceConfigSpec);

	    ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
		boolean result = _context.getVimClient().waitForTask(morTask);

		if(!result) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - createDisk() done(failed)");
			throw new Exception("Unable to create disk " + vmdkDatastorePath + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

		_context.waitForTaskProgressDone(morTask);

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - createDisk() done(successfully)");
	}

	public void attachDisk(String[] vmdkDatastorePathChain, ManagedObjectReference morDs) throws Exception {

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - attachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: "
				+ new Gson().toJson(vmdkDatastorePathChain) + ", datastore: " + morDs.getValue());

		VirtualDevice newDisk = VmwareHelper.prepareDiskDevice(this, getScsiDeviceControllerKey(),
			vmdkDatastorePathChain, morDs, -1, 1);
	    VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
	    //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
	    VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

	    deviceConfigSpec.setDevice(newDisk);
	    deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

	    //deviceConfigSpecArray[0] = deviceConfigSpec;
	    reConfigSpec.getDeviceChange().add(deviceConfigSpec);

	    ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
		boolean result = _context.getVimClient().waitForTask(morTask);

		if(!result) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - attachDisk() done(failed)");
            throw new Exception("Failed to attach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

		_context.waitForTaskProgressDone(morTask);

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - attachDisk() done(successfully)");
	}

	public void attachDisk(Pair<String, ManagedObjectReference>[] vmdkDatastorePathChain, int controllerKey) throws Exception {

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - attachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: "
				+ new Gson().toJson(vmdkDatastorePathChain));

		VirtualDevice newDisk = VmwareHelper.prepareDiskDevice(this, controllerKey,
			vmdkDatastorePathChain, -1, 1);
	    VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
	    //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
	    VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

	    deviceConfigSpec.setDevice(newDisk);
	    deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

	    //deviceConfigSpecArray[0] = deviceConfigSpec;
	    reConfigSpec.getDeviceChange().add(deviceConfigSpec);

	    ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
		boolean result = _context.getVimClient().waitForTask(morTask);

		if(!result) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - attachDisk() done(failed)");
            throw new Exception("Failed to attach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

		_context.waitForTaskProgressDone(morTask);

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - attachDisk() done(successfully)");
	}

	// vmdkDatastorePath: [datastore name] vmdkFilePath
	public List<Pair<String, ManagedObjectReference>> detachDisk(String vmdkDatastorePath, boolean deleteBackingFile) throws Exception {

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachDisk(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: "
				+ vmdkDatastorePath + ", deleteBacking: " + deleteBackingFile);

		// Note: if VM has been taken snapshot, original backing file will be renamed, therefore, when we try to find the matching
		// VirtualDisk, we only perform prefix matching
		Pair<VirtualDisk, String> deviceInfo = getDiskDevice(vmdkDatastorePath, false);
		if(deviceInfo == null) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - detachDisk() done (failed)");
			throw new Exception("No such disk device: " + vmdkDatastorePath);
		}

		List<Pair<String, ManagedObjectReference>> chain = getDiskDatastorePathChain(deviceInfo.first(), true);

	    VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
	    //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
	    VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

	    deviceConfigSpec.setDevice(deviceInfo.first());
	    if(deleteBackingFile) {
            deviceConfigSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.DESTROY);
        }
	    deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

	    //deviceConfigSpecArray[0] = deviceConfigSpec;
	    reConfigSpec.getDeviceChange().add(deviceConfigSpec);

	    ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
		boolean result = _context.getVimClient().waitForTask(morTask);

		if(!result) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - detachDisk() done (failed)");

            throw new Exception("Failed to detach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
		_context.waitForTaskProgressDone(morTask);

		// VMware does not update snapshot references to the detached disk, we have to work around it
		SnapshotDescriptor snapshotDescriptor = null;
		try {
			snapshotDescriptor = getSnapshotDescriptor();
		} catch(Exception e) {
			s_logger.info("Unable to retrieve snapshot descriptor, will skip updating snapshot reference");
		}

		if(snapshotDescriptor != null) {
			for(Pair<String, ManagedObjectReference> pair: chain) {
				DatastoreFile dsFile = new DatastoreFile(pair.first());
				snapshotDescriptor.removeDiskReferenceFromSnapshot(dsFile.getFileName());
			}

			Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();
			String dsPath = getSnapshotDescriptorDatastorePath();
			assert(dsPath != null);
			String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), dsPath);
			getContext().uploadResourceContent(url, snapshotDescriptor.getVmsdContent());
		}

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachDisk() done (successfully)");
		return chain;
	}

	public void detachAllDisks() throws Exception {
		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachAllDisk(). target MOR: " + _mor.getValue());

		VirtualDisk[] disks = getAllDiskDevice();
		if(disks.length > 0) {
		    VirtualMachineConfigSpec reConfigSpec = new VirtualMachineConfigSpec();
		    VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[disks.length];

			for(int i = 0; i < disks.length; i++) {
				deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
				deviceConfigSpecArray[i].setDevice(disks[i]);
				deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
			}
		    reConfigSpec.getDeviceChange().addAll(Arrays.asList(deviceConfigSpecArray));

		    ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
			boolean result = _context.getVimClient().waitForTask(morTask);

			if(!result) {
				if(s_logger.isTraceEnabled())
					s_logger.trace("vCenter API trace - detachAllDisk() done(failed)");
				throw new Exception("Failed to detach disk due to " + TaskMO.getTaskFailureInfo(_context, morTask));
            }

			_context.waitForTaskProgressDone(morTask);
		}

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachAllDisk() done(successfully)");
	}

	// isoDatastorePath: [datastore name] isoFilePath
	public void attachIso(String isoDatastorePath, ManagedObjectReference morDs,
		boolean connect, boolean connectAtBoot) throws Exception {

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachIso(). target MOR: " + _mor.getValue() + ", isoDatastorePath: "
				+ isoDatastorePath + ", datastore: " + morDs.getValue() + ", connect: " + connect + ", connectAtBoot: " + connectAtBoot);

		assert(isoDatastorePath != null);
		assert(morDs != null);

		boolean newCdRom = false;
		VirtualCdrom cdRom = (VirtualCdrom )getIsoDevice();
		if(cdRom == null) {
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
        if(newCdRom) {
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
        } else {
            deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.EDIT);
        }

	    //deviceConfigSpecArray[0] = deviceConfigSpec;
	    reConfigSpec.getDeviceChange().add(deviceConfigSpec);

	    ManagedObjectReference morTask = _context.getService().reconfigVMTask(_mor, reConfigSpec);
		boolean result = _context.getVimClient().waitForTask(morTask);

		if(!result) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - detachIso() done(failed)");
			throw new Exception("Failed to attach ISO due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

		_context.waitForTaskProgressDone(morTask);

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachIso() done(successfully)");
	}

	public void detachIso(String isoDatastorePath) throws Exception {
		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachIso(). target MOR: " + _mor.getValue() + ", isoDatastorePath: "
				+ isoDatastorePath);

		VirtualDevice device = getIsoDevice();
		if(device == null) {
			if(s_logger.isTraceEnabled())
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
		boolean result = _context.getVimClient().waitForTask(morTask);

		if(!result) {
			if(s_logger.isTraceEnabled())
				s_logger.trace("vCenter API trace - detachIso() done(failed)");
            throw new Exception("Failed to detachIso due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }
		_context.waitForTaskProgressDone(morTask);

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - detachIso() done(successfully)");
	}

	public Pair<VmdkFileDescriptor, byte[]> getVmdkFileInfo(String vmdkDatastorePath) throws Exception {

		if(s_logger.isTraceEnabled())
			s_logger.trace("vCenter API trace - getVmdkFileInfo(). target MOR: " + _mor.getValue() + ", vmdkDatastorePath: "
				+ vmdkDatastorePath);

		Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();

		String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), vmdkDatastorePath);
		byte[] content = getContext().getResourceContent(url);
		VmdkFileDescriptor descriptor = new VmdkFileDescriptor();
		descriptor.parse(content);

		Pair<VmdkFileDescriptor, byte[]> result = new Pair<VmdkFileDescriptor, byte[]>(descriptor, content);
		if(s_logger.isTraceEnabled()) {
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

		if(runtimeInfo.getPowerState() != VirtualMachinePowerState.POWERED_OFF) {
			String msg = "Unable to export VM because it is not at powerdOff state. vmName: " + vmName + ", host: " + hostName;
			s_logger.error(msg);
			throw new Exception(msg);
		}

		ManagedObjectReference morLease = _context.getService().exportVm(getMor());
		if(morLease == null) {
			s_logger.error("exportVm() failed");
			throw new Exception("exportVm() failed");
		}

		HttpNfcLeaseMO leaseMo = new HttpNfcLeaseMO(_context, morLease);
		HttpNfcLeaseState state = leaseMo.waitState(new HttpNfcLeaseState[] { HttpNfcLeaseState.READY, HttpNfcLeaseState.ERROR });

		try {
			if(state == HttpNfcLeaseState.READY) {
				final HttpNfcLeaseMO.ProgressReporter progressReporter = leaseMo.createProgressReporter();

                boolean success = false;
                List<String> fileNames = new ArrayList<String>();
				try {
					HttpNfcLeaseInfo leaseInfo = leaseMo.getLeaseInfo();
					final long totalBytes = leaseInfo.getTotalDiskCapacityInKB() * 1024;
					long totalBytesDownloaded = 0;

					List<HttpNfcLeaseDeviceUrl> deviceUrls = leaseInfo.getDeviceUrl();
					if(deviceUrls != null) {
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

						 	if(s_logger.isInfoEnabled()) {
                                s_logger.info("Download VMDK file for export. url: " + deviceUrlStr);
                            }
						 	long lengthOfDiskFile = _context.downloadVmdkFile(diskUrlStr, diskLocalPath, totalBytesDownloaded,
						 		new ActionDelegate<Long> () {
						 			@Override
                                    public void action(Long param) {
						 				if(s_logger.isTraceEnabled()) {
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

						FileWriter out = new FileWriter(ovfPath);
						out.write(ovfCreateDescriptorResult.getOvfDescriptor());
						out.close();

						// tar files into OVA
						if(packToOva) {
						    // Important! we need to sync file system before we can safely use tar to work around a linux kernal bug(or feature)
                            s_logger.info("Sync file system before we package OVA...");

						    Script commandSync = new Script(true, "sync", 0, s_logger);
                            commandSync.execute();

					        Script command = new Script(false, "tar", 0, s_logger);
					        command.setWorkDir(exportDir);
					        command.add("-cf", exportName + ".ova");
					        command.add(exportName + ".ovf");		// OVF file should be the first file in OVA archive
					        for(String name: fileNames) {
                                command.add((new File(name).getName()));
                            }

                            s_logger.info("Package OVA with commmand: " + command.toString());
					        command.execute();

					        // to be safe, physically test existence of the target OVA file
					        if((new File(exportDir + File.separator + exportName + ".ova")).exists()) {
					            success = true;
					        } else {
					            s_logger.error(exportDir + File.separator + exportName + ".ova is not created as expected");
					        }
						}
					}
				} catch(Throwable e) {
					s_logger.error("Unexpected exception ", e);
				} finally {
					progressReporter.close();

					if(leaveOvaFileOnly) {
                        for(String name : fileNames) {
                            new File(name).delete();
                        }
					}

					if(!success)
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
			in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(vmxContent)));
			out = new BufferedWriter(new OutputStreamWriter(bos));
			String line;
			while((line = in.readLine()) != null) {
				if(line.startsWith("workingDir")) {
					replaced = true;
					out.write(String.format("workingDir=\"%s\"", snapshotDir));
					out.newLine();
				} else {
					out.write(line);
					out.newLine();
				}
			}

			if(!replaced) {
				out.newLine();
				out.write(String.format("workingDir=\"%s\"", snapshotDir));
				out.newLine();
			}
		} finally {
			if(in != null) {
                in.close();
            }
			if(out != null) {
                out.close();
            }
		}
		_context.uploadResourceContent(vmxUrl, bos.toByteArray());

		// It seems that I don't need to do re-registration. VMware has bug in writing the correct snapshot's VMDK path to
		// its disk backing info anyway.
		// redoRegistration();
	}

	// destName does not contain extension name
	public void backupCurrentSnapshot(String deviceName, ManagedObjectReference morDestDs,
		String destDsDirectory, String destName, boolean includeBase) throws Exception {

		SnapshotDescriptor descriptor = getSnapshotDescriptor();
		SnapshotInfo[] snapshotInfo = descriptor.getCurrentDiskChain();
		if(snapshotInfo.length == 0) {
			String msg = "No snapshot found in this VM";
			throw new Exception(msg);
		}

		HostMO hostMo = getRunningHost();
		DatacenterMO dcMo = getOwnerDatacenter().first();
		List<Pair<ManagedObjectReference, String>> mounts = hostMo.getDatastoreMountsOnHost();
		VirtualMachineFileInfo vmFileInfo = getFileInfo();

		List<Ternary<String, String, String>> backupInfo = new ArrayList<Ternary<String, String, String>>();

		for(int i = 0; i < snapshotInfo.length; i++) {
			if(!includeBase && i == snapshotInfo.length - 1) {
                break;
            }

			SnapshotDescriptor.DiskInfo[] disks = snapshotInfo[i].getDisks();
			if(disks != null) {
				String destBaseFileName;
				String destFileName;
				String destParentFileName;
				for(SnapshotDescriptor.DiskInfo disk : disks) {
					if(deviceName == null || deviceName.equals(disk.getDeviceName())) {
						String srcVmdkFullDsPath = getSnapshotDiskFileDatastorePath(vmFileInfo,
							mounts, disk.getDiskFileName());
						Pair<DatastoreMO, String> srcDsInfo = getOwnerDatastore(srcVmdkFullDsPath);

						Pair<VmdkFileDescriptor, byte[]> vmdkInfo = getVmdkFileInfo(srcVmdkFullDsPath);
						String srcVmdkBaseFilePath = DatastoreFile.getCompanionDatastorePath(
							srcVmdkFullDsPath, vmdkInfo.first().getBaseFileName());

						destFileName = destName + (snapshotInfo.length - i - 1)+ ".vmdk";
						if(vmdkInfo.first().getParentFileName() != null) {
							destBaseFileName = destName + (snapshotInfo.length - i - 1)+ "-delta.vmdk";
							destParentFileName = destName + (snapshotInfo.length - i - 2)+ ".vmdk";
						} else {
							destBaseFileName = destName + (snapshotInfo.length - i - 1) + "-flat.vmdk";
							destParentFileName = null;
						}

						s_logger.info("Copy VMDK base file " + srcVmdkBaseFilePath + " to " + destDsDirectory + "/" + destBaseFileName);
						srcDsInfo.first().copyDatastoreFile(srcVmdkBaseFilePath, dcMo.getMor(),
							morDestDs, destDsDirectory + "/" + destBaseFileName, dcMo.getMor(), true);

						byte[] newVmdkContent = VmdkFileDescriptor.changeVmdkContentBaseInfo(
							vmdkInfo.second(), destBaseFileName, destParentFileName);
						String vmdkUploadUrl = getContext().composeDatastoreBrowseUrl(dcMo.getName(),
							destDsDirectory + "/" + destFileName);

						s_logger.info("Upload VMDK content file to " + destDsDirectory + "/" + destFileName);
						getContext().uploadResourceContent(vmdkUploadUrl, newVmdkContent);

						backupInfo.add(new Ternary<String, String, String>(
							destFileName, destBaseFileName, destParentFileName)
						);
					}
				}
			}
		}

		byte[] vdiskInfo = VmwareHelper.composeDiskInfo(backupInfo, snapshotInfo.length, includeBase);
		String vdiskUploadUrl = getContext().composeDatastoreBrowseUrl(dcMo.getName(),
				destDsDirectory + "/" + destName + ".vdisk");
		getContext().uploadResourceContent(vdiskUploadUrl, vdiskInfo);
	}

	public String[] getCurrentSnapshotDiskChainDatastorePaths(String diskDevice) throws Exception {
		HostMO hostMo = getRunningHost();
		List<Pair<ManagedObjectReference, String>> mounts = hostMo.getDatastoreMountsOnHost();
		VirtualMachineFileInfo vmFileInfo = getFileInfo();

		SnapshotDescriptor descriptor = getSnapshotDescriptor();
		SnapshotInfo[] snapshotInfo = descriptor.getCurrentDiskChain();

		List<String> diskDsFullPaths = new ArrayList<String>();
		for(int i = 0; i < snapshotInfo.length; i++) {
			SnapshotDescriptor.DiskInfo[] disks = snapshotInfo[i].getDisks();
			if(disks != null) {
				for(SnapshotDescriptor.DiskInfo disk: disks) {
					String deviceNameInDisk = disk.getDeviceName();
					if(diskDevice == null || diskDevice.equalsIgnoreCase(deviceNameInDisk)) {
						String vmdkFullDsPath = getSnapshotDiskFileDatastorePath(vmFileInfo,
								mounts, disk.getDiskFileName());
						diskDsFullPaths.add(vmdkFullDsPath);
					}
				}
			}
		}
		return diskDsFullPaths.toArray(new String[0]);
	}

	public void cloneFromCurrentSnapshot(String clonedVmName, int cpuSpeedMHz, int memoryMb, String diskDevice,
		ManagedObjectReference morDs) throws Exception {
		assert(morDs != null);
		String[] disks = getCurrentSnapshotDiskChainDatastorePaths(diskDevice);
		cloneFromDiskChain(clonedVmName, cpuSpeedMHz, memoryMb, disks, morDs);
	}

	public void cloneFromDiskChain(String clonedVmName, int cpuSpeedMHz, int memoryMb,
		String[] disks, ManagedObjectReference morDs) throws Exception {
		assert(disks != null);
	    assert(disks.length >= 1);

		HostMO hostMo = getRunningHost();
		VirtualMachineConfigInfo vmConfigInfo = getConfigInfo();

		if(!hostMo.createBlankVm(clonedVmName, 1, cpuSpeedMHz, 0, false, memoryMb, 0, vmConfigInfo.getGuestId(), morDs, false))
		    throw new Exception("Unable to create a blank VM");

		VirtualMachineMO clonedVmMo = hostMo.findVmOnHyperHost(clonedVmName);
		if(clonedVmMo == null)
		    throw new Exception("Unable to find just-created blank VM");

		boolean bSuccess = false;
		try {
    		VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
    	    //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
    		VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();

    	    VirtualDevice device = VmwareHelper.prepareDiskDevice(clonedVmMo, -1, disks, morDs, -1, 1);

    	    deviceConfigSpec.setDevice(device);
    	    deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
    	    vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
    	    clonedVmMo.configureVm(vmConfigSpec);
    	    bSuccess = true;
		} finally {
		    if(!bSuccess) {
		        clonedVmMo.detachAllDisks();
		        clonedVmMo.destroy();
		    }
		}
	}

	public void plugDevice(VirtualDevice device) throws Exception {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(device);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

        vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
        if(!configureVm(vmConfigSpec)) {
            throw new Exception("Failed to add devices");
        }
	}

	public void tearDownDevice(VirtualDevice device) throws Exception {
        VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
        //VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[1];
        VirtualDeviceConfigSpec deviceConfigSpec = new VirtualDeviceConfigSpec();
        deviceConfigSpec.setDevice(device);
        deviceConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.REMOVE);

        vmConfigSpec.getDeviceChange().add(deviceConfigSpec);
        if(!configureVm(vmConfigSpec)) {
            throw new Exception("Failed to detach devices");
        }
	}

	public void tearDownDevices(Class<?>[] deviceClasses) throws Exception {
		VirtualDevice[] devices = getMatchedDevices(deviceClasses);
		if(devices.length > 0) {
    		VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
    	    VirtualDeviceConfigSpec[] deviceConfigSpecArray = new VirtualDeviceConfigSpec[devices.length];

    	    for(int i = 0; i < devices.length; i++) {
    	    	deviceConfigSpecArray[i] = new VirtualDeviceConfigSpec();
    	    	deviceConfigSpecArray[i].setDevice(devices[i]);
    	    	deviceConfigSpecArray[i].setOperation(VirtualDeviceConfigSpecOperation.REMOVE);
    	    }

    	    vmConfigSpec.getDeviceChange().addAll(Arrays.asList(deviceConfigSpecArray));
    		if(!configureVm(vmConfigSpec)) {
                throw new Exception("Failed to detach devices");
            }
		}
	}

	public void copyAllVmDiskFiles(DatastoreMO destDsMo, String destDsDir, boolean followDiskChain) throws Exception {
		VirtualDevice[] disks = getAllDiskDevice();
		DatacenterMO dcMo = getOwnerDatacenter().first();
		if(disks != null) {
			for(VirtualDevice disk : disks) {
				List<Pair<String, ManagedObjectReference>> vmdkFiles = this.getDiskDatastorePathChain((VirtualDisk)disk, followDiskChain);
				for(Pair<String, ManagedObjectReference> fileItem : vmdkFiles) {
					DatastoreMO srcDsMo = new DatastoreMO(_context, fileItem.second());

					DatastoreFile srcFile = new DatastoreFile(fileItem.first());
					DatastoreFile destFile = new DatastoreFile(destDsMo.getName(), destDsDir, srcFile.getFileName());

					Pair<VmdkFileDescriptor, byte[]> vmdkDescriptor = null;

					vmdkDescriptor = getVmdkFileInfo(fileItem.first());

					s_logger.info("Copy VM disk file " + srcFile.getPath() + " to " + destFile.getPath());
					srcDsMo.copyDatastoreFile(fileItem.first(), dcMo.getMor(), destDsMo.getMor(),
						destFile.getPath(), dcMo.getMor(), true);

					if(vmdkDescriptor != null) {
						String vmdkBaseFileName = vmdkDescriptor.first().getBaseFileName();
						String baseFilePath = srcFile.getCompanionPath(vmdkBaseFileName);
						destFile = new DatastoreFile(destDsMo.getName(), destDsDir, vmdkBaseFileName);

						s_logger.info("Copy VM disk file " + baseFilePath + " to " + destFile.getPath());
						srcDsMo.copyDatastoreFile(baseFilePath, dcMo.getMor(), destDsMo.getMor(),
							destFile.getPath(), dcMo.getMor(), true);
					}
				}
			}
		}
	}

	// this method relies on un-offical VMware API
	@Deprecated
	public void moveAllVmDiskFiles(DatastoreMO destDsMo, String destDsDir, boolean followDiskChain) throws Exception {
		VirtualDevice[] disks = getAllDiskDevice();
		DatacenterMO dcMo = getOwnerDatacenter().first();
		if(disks != null) {
			for(VirtualDevice disk : disks) {
				List<Pair<String, ManagedObjectReference>> vmdkFiles = this.getDiskDatastorePathChain((VirtualDisk)disk, followDiskChain);
				for(Pair<String, ManagedObjectReference> fileItem : vmdkFiles) {
					DatastoreMO srcDsMo = new DatastoreMO(_context, fileItem.second());

					DatastoreFile srcFile = new DatastoreFile(fileItem.first());
					DatastoreFile destFile = new DatastoreFile(destDsMo.getName(), destDsDir, srcFile.getFileName());

					Pair<VmdkFileDescriptor, byte[]> vmdkDescriptor = null;
					vmdkDescriptor = getVmdkFileInfo(fileItem.first());

					s_logger.info("Move VM disk file " + srcFile.getPath() + " to " + destFile.getPath());
					srcDsMo.moveDatastoreFile(fileItem.first(), dcMo.getMor(), destDsMo.getMor(),
						destFile.getPath(), dcMo.getMor(), true);

					if(vmdkDescriptor != null) {
						String vmdkBaseFileName = vmdkDescriptor.first().getBaseFileName();
						String baseFilePath = srcFile.getCompanionPath(vmdkBaseFileName);
						destFile = new DatastoreFile(destDsMo.getName(), destDsDir, vmdkBaseFileName);

						s_logger.info("Move VM disk file " + baseFilePath + " to " + destFile.getPath());
						srcDsMo.moveDatastoreFile(baseFilePath, dcMo.getMor(), destDsMo.getMor(),
							destFile.getPath(), dcMo.getMor(), true);
					}
				}
			}
		}
	}

	public int getNextScsiDiskDeviceNumber() throws Exception {
		int scsiControllerKey = getScsiDeviceControllerKey();
		return getNextDeviceNumber(scsiControllerKey);
	}

	public int getScsiDeviceControllerKey() throws Exception {
	    List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
	    	getDynamicProperty(_mor, "config.hardware.device");

	    if(devices != null && devices.size() > 0) {
	    	for(VirtualDevice device : devices) {
                if(device instanceof VirtualLsiLogicController) {
                    return device.getKey();
                }
            }
	    }

	    assert(false);
	    throw new Exception("SCSI Controller Not Found");
	}

	public int getScsiDeviceControllerKeyNoException() throws Exception {
	    List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
	    	getDynamicProperty(_mor, "config.hardware.device");

	    if(devices != null && devices.size() > 0) {
	    	for(VirtualDevice device : devices) {
                if(device instanceof VirtualLsiLogicController) {
                    return device.getKey();
                }
            }
	    }

	    return -1;
	}

	public void ensureScsiDeviceController() throws Exception {
		int scsiControllerKey = getScsiDeviceControllerKeyNoException();
		if(scsiControllerKey < 0) {
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
			if(configureVm(vmConfig)) {
				throw new Exception("Unable to add Scsi controller");
			}
		}
	}

	// return pair of VirtualDisk and disk device bus name(ide0:0, etc)
	public Pair<VirtualDisk, String> getDiskDevice(String vmdkDatastorePath, boolean matchExactly) throws Exception {
		List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");

		s_logger.info("Look for disk device info from volume : " + vmdkDatastorePath);
		DatastoreFile dsSrcFile = new DatastoreFile(vmdkDatastorePath);
		String srcBaseName = dsSrcFile.getFileBaseName();

		if(devices != null && devices.size() > 0) {
			for(VirtualDevice device : devices) {
				if(device instanceof VirtualDisk) {
					s_logger.info("Test against disk device, controller key: " + device.getControllerKey() + ", unit number: " + device.getUnitNumber());

					VirtualDeviceBackingInfo backingInfo = ((VirtualDisk)device).getBacking();
					if(backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
						VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;
						do {
							s_logger.info("Test against disk backing : " + diskBackingInfo.getFileName());

							DatastoreFile dsBackingFile = new DatastoreFile(diskBackingInfo.getFileName());
							String backingBaseName = dsBackingFile.getFileBaseName();
							if(matchExactly) {
								if(backingBaseName .equalsIgnoreCase(srcBaseName)) {
									String deviceNumbering = getDeviceBusName(devices, device);

									s_logger.info("Disk backing : " + diskBackingInfo.getFileName() + " matches ==> " + deviceNumbering);
									return new Pair<VirtualDisk, String>((VirtualDisk)device, deviceNumbering);
								}
							} else {
								if(backingBaseName.contains(srcBaseName)) {
									String deviceNumbering = getDeviceBusName(devices, device);

									s_logger.info("Disk backing : " + diskBackingInfo.getFileName() + " matches ==> " + deviceNumbering);
									return new Pair<VirtualDisk, String>((VirtualDisk)device, deviceNumbering);
								}
							}

							diskBackingInfo = diskBackingInfo.getParent();
						} while(diskBackingInfo != null);
					}
				}
			}
		}

		return null;
	}

	@Deprecated
	public List<Pair<String, ManagedObjectReference>> getDiskDatastorePathChain(VirtualDisk disk, boolean followChain) throws Exception {
		VirtualDeviceBackingInfo backingInfo = disk.getBacking();
		if(!(backingInfo instanceof VirtualDiskFlatVer2BackingInfo)) {
            throw new Exception("Unsupported VirtualDeviceBackingInfo");
        }

		List<Pair<String, ManagedObjectReference>> pathList = new ArrayList<Pair<String, ManagedObjectReference>>();
		VirtualDiskFlatVer2BackingInfo diskBackingInfo = (VirtualDiskFlatVer2BackingInfo)backingInfo;

		if(!followChain) {
			pathList.add(new Pair<String, ManagedObjectReference>(diskBackingInfo.getFileName(), diskBackingInfo.getDatastore()));
			return pathList;
		}

		Pair<DatacenterMO, String> dcPair = getOwnerDatacenter();
		VirtualMachineFileInfo vmFilesInfo = getFileInfo();
		DatastoreFile snapshotDirFile = new DatastoreFile(vmFilesInfo.getSnapshotDirectory());
		DatastoreFile vmxDirFile = new DatastoreFile(vmFilesInfo.getVmPathName());

		do {
			if(diskBackingInfo.getParent() != null) {
				pathList.add(new Pair<String, ManagedObjectReference>(diskBackingInfo.getFileName(), diskBackingInfo.getDatastore()));
				diskBackingInfo = diskBackingInfo.getParent();
			} else {
				// try getting parent info from VMDK file itself
				byte[] content = null;
				try {
					String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), diskBackingInfo.getFileName());
					content = getContext().getResourceContent(url);
					if(content == null || content.length == 0) {
                        break;
                    }

					pathList.add(new Pair<String, ManagedObjectReference>(diskBackingInfo.getFileName(), diskBackingInfo.getDatastore()));
				} catch(Exception e) {
					// if snapshot directory has been changed to place other than default. VMware has a bug
					// that its corresponding disk backing info is not updated correctly. therefore, we will try search
					// in snapshot directory one more time
					DatastoreFile currentFile = new DatastoreFile(diskBackingInfo.getFileName());
					String vmdkFullDsPath = snapshotDirFile.getCompanionPath(currentFile.getFileName());

					String url = getContext().composeDatastoreBrowseUrl(dcPair.second(), vmdkFullDsPath);
					content = getContext().getResourceContent(url);
					if(content == null || content.length == 0) {
                        break;
                    }

					pathList.add(new Pair<String, ManagedObjectReference>(vmdkFullDsPath, diskBackingInfo.getDatastore()));
				}

				VmdkFileDescriptor descriptor = new VmdkFileDescriptor();
				descriptor.parse(content);
				if(descriptor.getParentFileName() != null && !descriptor.getParentFileName().isEmpty()) {
					// create a fake one
					VirtualDiskFlatVer2BackingInfo parentDiskBackingInfo = new VirtualDiskFlatVer2BackingInfo();
					parentDiskBackingInfo.setDatastore(diskBackingInfo.getDatastore());

					String parentFileName = descriptor.getParentFileName();
					if(parentFileName.startsWith("/")) {
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
		} while(diskBackingInfo != null);

		return pathList;
	}

	private String getDeviceBusName(List<VirtualDevice> allDevices, VirtualDevice theDevice) throws Exception {
		for(VirtualDevice device : allDevices) {
			if(device.getKey() == theDevice.getControllerKey().intValue()) {
				if(device instanceof VirtualIDEController) {
					return String.format("ide%d:%d", ((VirtualIDEController)device).getBusNumber(), theDevice.getUnitNumber());
				} else if(device instanceof VirtualSCSIController) {
					return String.format("scsi%d:%d", ((VirtualSCSIController)device).getBusNumber(), theDevice.getUnitNumber());
				} else {
					throw new Exception("Device controller is not supported yet");
				}
			}
		}
		throw new Exception("Unable to find device controller");
	}

	public VirtualDisk[] getAllDiskDevice() throws Exception {
		List<VirtualDisk> deviceList = new ArrayList<VirtualDisk>();
		List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().getDynamicProperty(_mor, "config.hardware.device");
		if(devices != null && devices.size() > 0) {
			for(VirtualDevice device : devices) {
				if(device instanceof VirtualDisk) {
					deviceList.add((VirtualDisk)device);
				}
			}
		}

		return deviceList.toArray(new VirtualDisk[0]);
	}

	public VirtualDisk[] getAllIndependentDiskDevice() throws Exception {
		List<VirtualDisk> independentDisks = new ArrayList<VirtualDisk>();
		VirtualDisk[] allDisks = getAllDiskDevice();
		if(allDisks.length > 0) {
			for(VirtualDisk disk : allDisks) {
	            String diskMode = "";
	            if(disk.getBacking() instanceof VirtualDiskFlatVer1BackingInfo) {
	            	diskMode = ((VirtualDiskFlatVer1BackingInfo)disk.getBacking()).getDiskMode();
				} else if(disk.getBacking() instanceof VirtualDiskFlatVer2BackingInfo) {
					diskMode = ((VirtualDiskFlatVer2BackingInfo)disk.getBacking()).getDiskMode();
				} else if(disk.getBacking() instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
					diskMode = ((VirtualDiskRawDiskMappingVer1BackingInfo)disk.getBacking()).getDiskMode();
				} else if(disk.getBacking() instanceof VirtualDiskSparseVer1BackingInfo) {
					diskMode = ((VirtualDiskSparseVer1BackingInfo)disk.getBacking()).getDiskMode();
				} else if(disk.getBacking() instanceof VirtualDiskSparseVer2BackingInfo) {
					diskMode = ((VirtualDiskSparseVer2BackingInfo)disk.getBacking()).getDiskMode();
				}

				if(diskMode.indexOf("independent") != -1) {
					independentDisks.add(disk);
				}
			}
		}

		return independentDisks.toArray(new VirtualDisk[0]);
	}

	public int tryGetIDEDeviceControllerKey() throws Exception {
	    List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
    		getDynamicProperty(_mor, "config.hardware.device");

	    if(devices != null && devices.size() > 0) {
	    	for(VirtualDevice device : devices) {
	    		if(device instanceof VirtualIDEController) {
	    			return ((VirtualIDEController)device).getKey();
	    		}
	    	}
	    }

		return -1;
	}

	public int getIDEDeviceControllerKey() throws Exception {
	    List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
	    	getDynamicProperty(_mor, "config.hardware.device");

	    if(devices != null && devices.size() > 0) {
	    	for(VirtualDevice device : devices) {
	    		if(device instanceof VirtualIDEController) {
	    			return ((VirtualIDEController)device).getKey();
	    		}
	    	}
	    }

	    assert(false);
	    throw new Exception("IDE Controller Not Found");
	}

	public int getNextIDEDeviceNumber() throws Exception {
		int controllerKey = getIDEDeviceControllerKey();
		return getNextDeviceNumber(controllerKey);
	}

	public VirtualDevice getIsoDevice() throws Exception {
		List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
			getDynamicProperty(_mor, "config.hardware.device");
		if(devices != null && devices.size() > 0) {
			for(VirtualDevice device : devices) {
				if(device instanceof VirtualCdrom) {
					return device;
				}
			}
		}
		return null;
	}

	public int getPCIDeviceControllerKey() throws Exception {
	    List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
	        getDynamicProperty(_mor, "config.hardware.device");

	    if(devices != null && devices.size() > 0) {
	    	for(VirtualDevice device : devices) {
	    		if(device instanceof VirtualPCIController) {
	    			return ((VirtualPCIController)device).getKey();
	    		}
	    	}
	    }

	    assert(false);
	    throw new Exception("PCI Controller Not Found");
	}

	public int getNextPCIDeviceNumber() throws Exception {
		int controllerKey = getPCIDeviceControllerKey();
		return getNextDeviceNumber(controllerKey);
	}

	public int getNextDeviceNumber(int controllerKey) throws Exception {
		List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
			getDynamicProperty(_mor, "config.hardware.device");

		int deviceNumber = -1;
		if(devices != null && devices.size() > 0) {
			for(VirtualDevice device : devices) {
				if(device.getControllerKey() != null && device.getControllerKey().intValue() == controllerKey) {
					if(device.getUnitNumber() != null && device.getUnitNumber().intValue() > deviceNumber) {
                        deviceNumber = device.getUnitNumber().intValue();
                    }
				}
			}
		}
		return ++deviceNumber;
	}

	public VirtualDevice[] getNicDevices() throws Exception {
		List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
			getDynamicProperty(_mor, "config.hardware.device");

		List<VirtualDevice> nics = new ArrayList<VirtualDevice>();
		if(devices != null) {
			for(VirtualDevice device : devices) {
				if(device instanceof VirtualEthernetCard) {
                    nics.add(device);
                }
			}
		}

		return nics.toArray(new VirtualDevice[0]);
	}

	public Pair<Integer, VirtualDevice> getNicDeviceIndex(String networkNamePrefix) throws Exception {
        List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
        getDynamicProperty(_mor, "config.hardware.device");

        List<VirtualDevice> nics = new ArrayList<VirtualDevice>();
        if(devices != null) {
            for(VirtualDevice device : devices) {
                if(device instanceof VirtualEthernetCard) {
                    nics.add(device);
                }
            }
        }

        Collections.sort(nics, new Comparator<VirtualDevice>() {
            @Override
            public int compare(VirtualDevice arg0, VirtualDevice arg1) {
                int unitNumber0 = arg0.getUnitNumber() != null ? arg0.getUnitNumber().intValue() : -1;
                int unitNumber1 = arg1.getUnitNumber() != null ? arg1.getUnitNumber().intValue() : -1;
                if(unitNumber0 < unitNumber1)
                    return -1;
                else if(unitNumber0 > unitNumber1)
                    return 1;
                return 0;
            }
        });

        int index = 0;
        String attachedNetworkSummary;
        String dvPortGroupName;
        for (VirtualDevice nic : nics) {
            attachedNetworkSummary = ((VirtualEthernetCard) nic).getDeviceInfo().getSummary();
            if (attachedNetworkSummary.startsWith(networkNamePrefix)) {
                return new Pair<Integer, VirtualDevice>(new Integer(index), nic);
            } else if (attachedNetworkSummary.endsWith("DistributedVirtualPortBackingInfo.summary")) {
                dvPortGroupName = getDvPortGroupName((VirtualEthernetCard) nic);
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
        VirtualEthernetCardDistributedVirtualPortBackingInfo dvpBackingInfo =
                (VirtualEthernetCardDistributedVirtualPortBackingInfo) ((VirtualEthernetCard) nic).getBacking();
        DistributedVirtualSwitchPortConnection dvsPort = (DistributedVirtualSwitchPortConnection) dvpBackingInfo.getPort();
        String dvPortGroupKey = dvsPort.getPortgroupKey();
        ManagedObjectReference dvPortGroupMor = new ManagedObjectReference();
        dvPortGroupMor.setValue(dvPortGroupKey);
        dvPortGroupMor.setType("DistributedVirtualPortgroup");
        return (String) _context.getVimClient().getDynamicProperty(dvPortGroupMor, "name");
    }

	public VirtualDevice[] getMatchedDevices(Class<?>[] deviceClasses) throws Exception {
		assert(deviceClasses != null);

		List<VirtualDevice> returnList = new ArrayList<VirtualDevice>();

		List<VirtualDevice> devices = (List<VirtualDevice>)_context.getVimClient().
			getDynamicProperty(_mor, "config.hardware.device");

		if(devices != null) {
			for(VirtualDevice device : devices) {
				for(Class<?> clz : deviceClasses) {
					if(clz.isInstance(device)) {
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

	public void unmountToolsInstaller() throws Exception {
		_context.getService().unmountToolsInstaller(_mor);
	}

	public void redoRegistration(ManagedObjectReference morHost) throws Exception {
		String vmName = getVmName();
		VirtualMachineFileInfo vmFileInfo = getFileInfo();
		boolean isTemplate = isTemplate();

		HostMO hostMo;
		if(morHost != null)
			hostMo = new HostMO(getContext(), morHost);
		else
			hostMo = getRunningHost();

		ManagedObjectReference morFolder = getParentMor();
		ManagedObjectReference morPool = hostMo.getHyperHostOwnerResourcePool();

		_context.getService().unregisterVM(_mor);

		ManagedObjectReference morTask = _context.getService().registerVMTask(
    		 morFolder,
    		 vmFileInfo.getVmPathName(),
    		 vmName, false,
    		 morPool, hostMo.getMor());

		boolean result = _context.getVimClient().waitForTask(morTask);
		if (!result) {
			throw new Exception("Unable to register template due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		} else {
			_context.waitForTaskProgressDone(morTask);
			if(isTemplate) {
				VirtualMachineMO vmNewRegistration = hostMo.findVmOnHyperHost(vmName);
				assert(vmNewRegistration != null);
				vmNewRegistration.markAsTemplate();
			}
		}
	}
}

