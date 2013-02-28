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

package com.cloud.hypervisor.vmware.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.ExceptionUtil;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualE1000;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualVmxnet2;
import com.vmware.vim25.VirtualVmxnet3;

public class VmwareHelper {
    private static final Logger s_logger = Logger.getLogger(VmwareHelper.class);

	public static VirtualDevice prepareNicDevice(VirtualMachineMO vmMo, ManagedObjectReference morNetwork, VirtualEthernetCardType deviceType,
		String portGroupName, String macAddress, int deviceNumber, int contextNumber, boolean conntected, boolean connectOnStart) throws Exception {

		VirtualEthernetCard nic;
		switch(deviceType) {
		case E1000 :
			nic = new VirtualE1000();
			break;

		case PCNet32 :
			nic = new VirtualPCNet32();
			break;

		case Vmxnet2 :
			nic = new VirtualVmxnet2();
			break;

		case Vmxnet3 :
			nic = new VirtualVmxnet3();
			break;

		default :
			assert(false);
			nic = new VirtualE1000();
		}

		VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
		nicBacking.setDeviceName(portGroupName);
		nicBacking.setNetwork(morNetwork);
		nic.setBacking(nicBacking);

		VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
		connectInfo.setAllowGuestControl(true);
		connectInfo.setConnected(conntected);
		connectInfo.setStartConnected(connectOnStart);
		nic.setAddressType("Manual");
		nic.setConnectable(connectInfo);
		nic.setMacAddress(macAddress);
		nic.setUnitNumber(deviceNumber);
		nic.setKey(-contextNumber);
		return nic;
	}

    public static VirtualDevice prepareDvNicDevice(VirtualMachineMO vmMo, ManagedObjectReference morNetwork, VirtualEthernetCardType deviceType,
            String dvPortGroupName, String dvSwitchUuid, String macAddress, int deviceNumber, int contextNumber, boolean conntected, boolean connectOnStart) throws Exception {

        VirtualEthernetCard nic;
        switch (deviceType) {
        case E1000:
            nic = new VirtualE1000();
            break;

        case PCNet32:
            nic = new VirtualPCNet32();
            break;

        case Vmxnet2:
            nic = new VirtualVmxnet2();
            break;

        case Vmxnet3:
            nic = new VirtualVmxnet3();
            break;

        default:
            assert (false);
            nic = new VirtualE1000();
        }

        final VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
        final DistributedVirtualSwitchPortConnection dvPortConnection = new DistributedVirtualSwitchPortConnection();
        final VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();

        dvPortConnection.setSwitchUuid(dvSwitchUuid);
        dvPortConnection.setPortgroupKey(morNetwork.getValue());
        dvPortBacking.setPort(dvPortConnection);
        nic.setBacking(dvPortBacking);
        nic.setKey(30);

        connectInfo.setAllowGuestControl(true);
        connectInfo.setConnected(conntected);
        connectInfo.setStartConnected(connectOnStart);
        nic.setAddressType("Manual");
        nic.setConnectable(connectInfo);
        nic.setMacAddress(macAddress);

        nic.setUnitNumber(deviceNumber);
        nic.setKey(-contextNumber);
        return nic;
    }

	// vmdkDatastorePath: [datastore name] vmdkFilePath
	public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, int controllerKey, String vmdkDatastorePath,
		int sizeInMb, ManagedObjectReference morDs, int deviceNumber, int contextNumber) throws Exception {

		VirtualDisk disk = new VirtualDisk();

		VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
        backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
    	backingInfo.setThinProvisioned(true);
    	backingInfo.setEagerlyScrub(false);
        backingInfo.setDatastore(morDs);
        backingInfo.setFileName(vmdkDatastorePath);
        disk.setBacking(backingInfo);

		if(controllerKey < 0)
			controllerKey = vmMo.getIDEDeviceControllerKey();
        if(deviceNumber < 0)
        	deviceNumber = vmMo.getNextDeviceNumber(controllerKey);
		disk.setControllerKey(controllerKey);

	    disk.setKey(-contextNumber);
	    disk.setUnitNumber(deviceNumber);
	    disk.setCapacityInKB(sizeInMb*1024);

	    VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
	    connectInfo.setConnected(true);
	    connectInfo.setStartConnected(true);
	    disk.setConnectable(connectInfo);

	    return disk;
	}

	// vmdkDatastorePath: [datastore name] vmdkFilePath, create delta disk based on disk from template
	public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, int controllerKey, String vmdkDatastorePath,
		int sizeInMb, ManagedObjectReference morDs, VirtualDisk templateDisk, int deviceNumber, int contextNumber) throws Exception {

		assert(templateDisk != null);
		VirtualDeviceBackingInfo parentBacking = templateDisk.getBacking();
		assert(parentBacking != null);

		// TODO Not sure if we need to check if the disk in template and the new disk needs to share the
		// same datastore
		VirtualDisk disk = new VirtualDisk();
		if(parentBacking instanceof VirtualDiskFlatVer1BackingInfo) {
			VirtualDiskFlatVer1BackingInfo backingInfo = new VirtualDiskFlatVer1BackingInfo();
	        backingInfo.setDiskMode(((VirtualDiskFlatVer1BackingInfo)parentBacking).getDiskMode());
	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        backingInfo.setParent((VirtualDiskFlatVer1BackingInfo)parentBacking);
	        disk.setBacking(backingInfo);
		} else if(parentBacking instanceof VirtualDiskFlatVer2BackingInfo) {
			VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
	        backingInfo.setDiskMode(((VirtualDiskFlatVer2BackingInfo)parentBacking).getDiskMode());
	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        backingInfo.setParent((VirtualDiskFlatVer2BackingInfo)parentBacking);
	        disk.setBacking(backingInfo);
		} else if(parentBacking instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
			VirtualDiskRawDiskMappingVer1BackingInfo backingInfo = new VirtualDiskRawDiskMappingVer1BackingInfo();
	        backingInfo.setDiskMode(((VirtualDiskRawDiskMappingVer1BackingInfo)parentBacking).getDiskMode());
	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        backingInfo.setParent((VirtualDiskRawDiskMappingVer1BackingInfo)parentBacking);
	        disk.setBacking(backingInfo);
		} else if(parentBacking instanceof VirtualDiskSparseVer1BackingInfo) {
			VirtualDiskSparseVer1BackingInfo backingInfo = new VirtualDiskSparseVer1BackingInfo();
	        backingInfo.setDiskMode(((VirtualDiskSparseVer1BackingInfo)parentBacking).getDiskMode());
	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        backingInfo.setParent((VirtualDiskSparseVer1BackingInfo)parentBacking);
	        disk.setBacking(backingInfo);
		} else if(parentBacking instanceof VirtualDiskSparseVer2BackingInfo) {
			VirtualDiskSparseVer2BackingInfo backingInfo = new VirtualDiskSparseVer2BackingInfo();
	        backingInfo.setDiskMode(((VirtualDiskSparseVer2BackingInfo)parentBacking).getDiskMode());
	        backingInfo.setDatastore(morDs);
	        backingInfo.setFileName(vmdkDatastorePath);
	        backingInfo.setParent((VirtualDiskSparseVer2BackingInfo)parentBacking);
	        disk.setBacking(backingInfo);
		} else {
			throw new Exception("Unsupported disk backing: " + parentBacking.getClass().getCanonicalName());
		}

		if(controllerKey < 0)
			controllerKey = vmMo.getIDEDeviceControllerKey();
		disk.setControllerKey(controllerKey);
		if(deviceNumber < 0)
			deviceNumber = vmMo.getNextDeviceNumber(controllerKey);

	    disk.setKey(-contextNumber);
	    disk.setUnitNumber(deviceNumber);
	    disk.setCapacityInKB(sizeInMb*1024);

	    VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
	    connectInfo.setConnected(true);
	    connectInfo.setStartConnected(true);
	    disk.setConnectable(connectInfo);
	    return disk;
	}

	// vmdkDatastorePath: [datastore name] vmdkFilePath
	public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, int controllerKey, String vmdkDatastorePathChain[],
		ManagedObjectReference morDs, int deviceNumber, int contextNumber) throws Exception {

		assert(vmdkDatastorePathChain != null);
		assert(vmdkDatastorePathChain.length >= 1);

		VirtualDisk disk = new VirtualDisk();

		VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
        backingInfo.setDatastore(morDs);
        backingInfo.setFileName(vmdkDatastorePathChain[0]);
        backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
        if(vmdkDatastorePathChain.length > 1) {
        	String[] parentDisks = new String[vmdkDatastorePathChain.length - 1];
        	for(int i = 0; i < vmdkDatastorePathChain.length - 1; i++)
        		parentDisks[i] = vmdkDatastorePathChain[i + 1];

        	setParentBackingInfo(backingInfo, morDs, parentDisks);
        }

        disk.setBacking(backingInfo);

		if(controllerKey < 0)
			controllerKey = vmMo.getIDEDeviceControllerKey();
        if(deviceNumber < 0)
        	deviceNumber = vmMo.getNextDeviceNumber(controllerKey);

		disk.setControllerKey(controllerKey);
	    disk.setKey(-contextNumber);
	    disk.setUnitNumber(deviceNumber);

	    VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
	    connectInfo.setConnected(true);
	    connectInfo.setStartConnected(true);
	    disk.setConnectable(connectInfo);

	    return disk;
	}

	public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, int controllerKey,
		Pair<String, ManagedObjectReference>[] vmdkDatastorePathChain,
		int deviceNumber, int contextNumber) throws Exception {

		assert(vmdkDatastorePathChain != null);
		assert(vmdkDatastorePathChain.length >= 1);

		VirtualDisk disk = new VirtualDisk();

		VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
        backingInfo.setDatastore(vmdkDatastorePathChain[0].second());
        backingInfo.setFileName(vmdkDatastorePathChain[0].first());
        backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
        if(vmdkDatastorePathChain.length > 1) {
        	Pair<String, ManagedObjectReference>[] parentDisks = new Pair[vmdkDatastorePathChain.length - 1];
        	for(int i = 0; i < vmdkDatastorePathChain.length - 1; i++)
        		parentDisks[i] = vmdkDatastorePathChain[i + 1];

        	setParentBackingInfo(backingInfo, parentDisks);
        }

        disk.setBacking(backingInfo);

		if(controllerKey < 0)
			controllerKey = vmMo.getIDEDeviceControllerKey();
        if(deviceNumber < 0)
        	deviceNumber = vmMo.getNextDeviceNumber(controllerKey);

		disk.setControllerKey(controllerKey);
	    disk.setKey(-contextNumber);
	    disk.setUnitNumber(deviceNumber);

	    VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
	    connectInfo.setConnected(true);
	    connectInfo.setStartConnected(true);
	    disk.setConnectable(connectInfo);

	    return disk;
	}

	private static void setParentBackingInfo(VirtualDiskFlatVer2BackingInfo backingInfo,
		ManagedObjectReference morDs, String[] parentDatastorePathList) {

		VirtualDiskFlatVer2BackingInfo parentBacking = new VirtualDiskFlatVer2BackingInfo();
		parentBacking.setDatastore(morDs);
		parentBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());

		if(parentDatastorePathList.length > 1) {
			String[] nextDatastorePathList = new String[parentDatastorePathList.length -1];
			for(int i = 0; i < parentDatastorePathList.length -1; i++)
				nextDatastorePathList[i] = parentDatastorePathList[i + 1];
			setParentBackingInfo(parentBacking, morDs, nextDatastorePathList);
		}
		parentBacking.setFileName(parentDatastorePathList[0]);

		backingInfo.setParent(parentBacking);
	}

	private static void setParentBackingInfo(VirtualDiskFlatVer2BackingInfo backingInfo,
		Pair<String, ManagedObjectReference>[] parentDatastorePathList) {

		VirtualDiskFlatVer2BackingInfo parentBacking = new VirtualDiskFlatVer2BackingInfo();
		parentBacking.setDatastore(parentDatastorePathList[0].second());
		parentBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());

		if(parentDatastorePathList.length > 1) {
			Pair<String, ManagedObjectReference>[] nextDatastorePathList = new Pair[parentDatastorePathList.length -1];
			for(int i = 0; i < parentDatastorePathList.length -1; i++)
				nextDatastorePathList[i] = parentDatastorePathList[i + 1];
			setParentBackingInfo(parentBacking, nextDatastorePathList);
		}
		parentBacking.setFileName(parentDatastorePathList[0].first());

		backingInfo.setParent(parentBacking);
	}

	public static Pair<VirtualDevice, Boolean> prepareIsoDevice(VirtualMachineMO vmMo, String isoDatastorePath, ManagedObjectReference morDs,
		boolean connect, boolean connectAtBoot, int deviceNumber, int contextNumber) throws Exception {

		boolean newCdRom = false;
		VirtualCdrom cdRom = (VirtualCdrom )vmMo.getIsoDevice();
		if(cdRom == null) {
			newCdRom = true;
			cdRom = new VirtualCdrom();

			assert(vmMo.getIDEDeviceControllerKey() >= 0);
			cdRom.setControllerKey(vmMo.getIDEDeviceControllerKey());
			if(deviceNumber < 0)
				deviceNumber = vmMo.getNextIDEDeviceNumber();

			cdRom.setUnitNumber(deviceNumber);
			cdRom.setKey(-contextNumber);
		}

	    VirtualDeviceConnectInfo cInfo = new VirtualDeviceConnectInfo();
	    cInfo.setConnected(connect);
	    cInfo.setStartConnected(connectAtBoot);
	    cdRom.setConnectable(cInfo);

        if(isoDatastorePath != null) {
            VirtualCdromIsoBackingInfo backingInfo = new VirtualCdromIsoBackingInfo();
	        backingInfo.setFileName(isoDatastorePath);
	        backingInfo.setDatastore(morDs);
	        cdRom.setBacking(backingInfo);
        } else {
    		VirtualCdromRemotePassthroughBackingInfo backingInfo = new VirtualCdromRemotePassthroughBackingInfo();
    		backingInfo.setDeviceName("");
    		cdRom.setBacking(backingInfo);
        }

		return new Pair<VirtualDevice, Boolean>(cdRom, newCdRom);
	}

	public static VirtualDisk getRootDisk(VirtualDisk[] disks) {
		if(disks.length == 1)
			return disks[0];

		// TODO : for now, always return the first disk as root disk
		return disks[0];
	}

	public static ManagedObjectReference findSnapshotInTree(List<VirtualMachineSnapshotTree> snapTree, String findName) {
		assert(findName != null);

		ManagedObjectReference snapMor = null;
		if (snapTree == null)
			return snapMor;

		for (int i = 0; i < snapTree.size() && snapMor == null; i++) {
			VirtualMachineSnapshotTree node = snapTree.get(i);

			if (node.getName().equals(findName)) {
				snapMor = node.getSnapshot();
			} else {
				List<VirtualMachineSnapshotTree> childTree = node.getChildSnapshotList();
				snapMor = findSnapshotInTree(childTree, findName);
			}
		}
		return snapMor;
	}

	public static byte[] composeDiskInfo(List<Ternary<String, String, String>> diskInfo, int disksInChain, boolean includeBase) throws IOException {

		BufferedWriter out = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			out = new BufferedWriter(new OutputStreamWriter(bos));

			out.write("disksInChain=" + disksInChain);
			out.newLine();

			out.write("disksInBackup=" + diskInfo.size());
			out.newLine();

			out.write("baseDiskIncluded=" + includeBase);
			out.newLine();

			int seq = disksInChain - 1;
			for(Ternary<String, String, String> item : diskInfo) {
				out.write(String.format("disk%d.fileName=%s", seq, item.first()));
				out.newLine();

				out.write(String.format("disk%d.baseFileName=%s", seq, item.second()));
				out.newLine();

				if(item.third() != null) {
					out.write(String.format("disk%d.parentFileName=%s", seq, item.third()));
					out.newLine();
				}
				seq--;
			}

			out.newLine();
		} finally {
			if(out != null)
				out.close();
		}

		return bos.toByteArray();
	}

	public static OptionValue[] composeVncOptions(OptionValue[] optionsToMerge,
		boolean enableVnc, String vncPassword, int vncPort, String keyboardLayout) {

		int numOptions = 3;
		boolean needKeyboardSetup = false;
		if(keyboardLayout != null && !keyboardLayout.isEmpty()) {
			numOptions++;
			needKeyboardSetup = true;
		}

		if(optionsToMerge != null)
			numOptions += optionsToMerge.length;

		OptionValue[] options = new OptionValue[numOptions];
		int i = 0;
		if(optionsToMerge != null) {
			for(int j = 0; j < optionsToMerge.length; j++)
				options[i++] = optionsToMerge[j];
		}

		options[i] = new OptionValue();
		options[i].setKey("RemoteDisplay.vnc.enabled");
		options[i++].setValue(enableVnc ? "true" : "false");

		options[i] = new OptionValue();
		options[i].setKey("RemoteDisplay.vnc.password");
		options[i++].setValue(vncPassword);

		options[i] = new OptionValue();
		options[i].setKey("RemoteDisplay.vnc.port");
		options[i++].setValue("" + vncPort);

		if(needKeyboardSetup) {
			options[i] = new OptionValue();
			options[i].setKey("RemoteDisplay.vnc.keymap");
			options[i++].setValue(keyboardLayout);
		}

		return options;
	}

	public static void setBasicVmConfig(VirtualMachineConfigSpec vmConfig, int cpuCount, int cpuSpeedMHz, int cpuReservedMhz,
		int memoryMB, int memoryReserveMB, String guestOsIdentifier, boolean limitCpuUse) {

		// VM config basics
		vmConfig.setMemoryMB((long)memoryMB);
		vmConfig.setNumCPUs(cpuCount);

		ResourceAllocationInfo cpuInfo = new ResourceAllocationInfo();
		if (limitCpuUse) {
			cpuInfo.setLimit((long)(cpuSpeedMHz * cpuCount));
		} else {
			cpuInfo.setLimit(-1L);
		}

		cpuInfo.setReservation((long)cpuReservedMhz);
		vmConfig.setCpuAllocation(cpuInfo);
		 if (cpuSpeedMHz != cpuReservedMhz){
             vmConfig.setCpuHotAddEnabled(true);
         }
        if (memoryMB != memoryReserveMB){
            vmConfig.setMemoryHotAddEnabled(true);
        }
		ResourceAllocationInfo memInfo = new ResourceAllocationInfo();
		memInfo.setLimit((long)memoryMB);
		memInfo.setReservation((long)memoryReserveMB);
		vmConfig.setMemoryAllocation(memInfo);

		vmConfig.setGuestId(guestOsIdentifier);
	}

	public static ManagedObjectReference getDiskDeviceDatastore(VirtualDisk diskDevice) throws Exception {
		VirtualDeviceBackingInfo backingInfo = diskDevice.getBacking();
		assert(backingInfo instanceof VirtualDiskFlatVer2BackingInfo);
		return ((VirtualDiskFlatVer2BackingInfo)backingInfo).getDatastore();
	}

	public static Object getPropValue(ObjectContent oc, String name) {
		List<DynamicProperty> props = oc.getPropSet();

		for(DynamicProperty prop : props) {
			if(prop.getName().equalsIgnoreCase(name))
				return prop.getVal();
		}

		return null;
	}

	public static String getFileExtension(String fileName, String defaultExtension) {
		int pos = fileName.lastIndexOf('.');
		if(pos < 0)
			return defaultExtension;

		return fileName.substring(pos);
	}

	public static boolean isSameHost(String ipAddress, String destName) {
		// TODO : may need to do DNS lookup to compare IP address exactly
		return ipAddress.equals(destName);
	}

	public static void deleteVolumeVmdkFiles(DatastoreMO dsMo, String volumeName, DatacenterMO dcMo) throws Exception {
        String volumeDatastorePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumeName);
        dsMo.deleteFile(volumeDatastorePath, dcMo.getMor(), true);

        volumeDatastorePath = String.format("[%s] %s-flat.vmdk", dsMo.getName(), volumeName);
        dsMo.deleteFile(volumeDatastorePath, dcMo.getMor(), true);

        volumeDatastorePath = String.format("[%s] %s-delta.vmdk", dsMo.getName(), volumeName);
        dsMo.deleteFile(volumeDatastorePath, dcMo.getMor(), true);
	}

	public static String getExceptionMessage(Throwable e) {
		return getExceptionMessage(e, false);
	}

	public static String getExceptionMessage(Throwable e, boolean printStack) {
	    //TODO: in vim 5.1, exceptions do not have a base exception class, MethodFault becomes a FaultInfo that we can only get
	    // from individual exception through getFaultInfo, so we have to use reflection here to get MethodFault information.
	    try{
	        Class cls = e.getClass();
	        Method mth = cls.getDeclaredMethod("getFaultInfo", null);
	        if ( mth != null ){
	            Object fault = mth.invoke(e, null);
	            if (fault instanceof MethodFault) {
	                final StringWriter writer = new StringWriter();
	                writer.append("Exception: " + fault.getClass().getName() + "\n");
	                writer.append("message: " + ((MethodFault)fault).getFaultMessage() + "\n");

	                if(printStack) {
	                    writer.append("stack: ");
	                    e.printStackTrace(new PrintWriter(writer));
	                }
	                return writer.toString();
	            }
	        }
	    }
	    catch (Exception ex){

	    }

		return ExceptionUtil.toString(e, printStack);
	}

	public static VirtualMachineMO pickOneVmOnRunningHost(List<VirtualMachineMO> vmList, boolean bFirstFit) throws Exception {
		List<VirtualMachineMO> candidates = new ArrayList<VirtualMachineMO>();

    	for(VirtualMachineMO vmMo : vmList) {
    		HostMO hostMo = vmMo.getRunningHost();
    		if(hostMo.isHyperHostConnected())
    			candidates.add(vmMo);
    	}

    	if(candidates.size() == 0)
    		return null;

    	if(bFirstFit)
    		return candidates.get(0);

    	Random random = new Random();
    	return candidates.get(random.nextInt(candidates.size()));
	}

    public static boolean isDvPortGroup(ManagedObjectReference networkMor) {
         return "DistributedVirtualPortgroup".equalsIgnoreCase(networkMor.getType());
    }
}
