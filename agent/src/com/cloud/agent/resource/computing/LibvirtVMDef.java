/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.agent.resource.computing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LibvirtVMDef {
	private String _hvsType;
	private String _domName;
	private String _domUUID;
	private String _desc;
	private final List<Object> components = new ArrayList<Object>();
	
	public static class guestDef {
		enum guestType {
			KVM,
			XEN,
			EXE
		}
		enum bootOrder {
			HARDISK("hd"),
			CDROM("cdrom"),
			FLOOPY("fd"),
			NETWORK("network");
			String _order;
			bootOrder(String order) {
				_order = order;
			}
			@Override
            public String toString() {
				return _order;
			}
		}
		private guestType _type;
		private String _arch;
		private String _loader;
		private String _kernel;
		private String _initrd;
		private String _root;
		private String _cmdline;
		private List<bootOrder> _bootdevs = new ArrayList<bootOrder>();
		private String _machine;
		public void setGuestType (guestType type) {
			_type = type;
		}
		public void setGuestArch (String arch) {
			_arch = arch;
		}
		public void setMachineType (String machine) {
			_machine = machine;
		}
		public void setLoader (String loader) {
			_loader = loader;
		}
		public void setBootKernel(String kernel, String initrd, String rootdev, String cmdline) {
			_kernel = kernel;
			_initrd = initrd;
			_root = rootdev;
			_cmdline = cmdline;
		}
		public void setBootOrder(bootOrder order) {
			_bootdevs.add(order);
		}
		@Override
        public String toString () {
			if (_type == guestType.KVM) {
				StringBuilder guestDef = new StringBuilder();
				guestDef.append("<os>\n");
				guestDef.append("<type ");
				if (_arch != null) {
					guestDef.append(" arch='" + _arch + "'");
				}
				if (_machine != null) {
					guestDef.append(" machine='" + _machine + "'");
				}
				guestDef.append(">hvm</type>\n");
				if (!_bootdevs.isEmpty()) {
					for (bootOrder bo : _bootdevs) {
						guestDef.append("<boot dev='" + bo + "'/>\n");
					}
				}
				guestDef.append("</os>\n");
				return guestDef.toString();
			} else
				return null;
		}
	}
	
	public static class guestResourceDef {
		private int _mem;
		private int _currentMem = -1;
		private String _memBacking;
		private int _vcpu = -1;
		public void setMemorySize(int mem) {
			_mem = mem;
		}
		public void setCurrentMem(int currMem) {
			_currentMem = currMem;
		}
		public void setMemBacking(String memBacking) {
			_memBacking = memBacking;
		}
		public void setVcpuNum(int vcpu) {
			_vcpu = vcpu;
		}
		@Override
        public String toString(){
			StringBuilder resBuidler = new StringBuilder();
			resBuidler.append("<memory>" + _mem + "</memory>\n");
			if (_currentMem != -1) {
				resBuidler.append("<currentMemory>" + _currentMem + "</currentMemory>\n");
			}
			if (_memBacking != null) {
				resBuidler.append("<memoryBacking>" + "<" + _memBacking + "/>" + "</memoryBacking>\n");
			}
			if (_vcpu != -1) {
				resBuidler.append("<vcpu>" + _vcpu + "</vcpu>\n");
			}
			return resBuidler.toString();
		}
	}
	
	public static class featuresDef {
		private final List<String> _features = new ArrayList<String>();
		public void addFeatures(String feature) {
			_features.add(feature);
		}
		@Override
        public String toString() {
			StringBuilder feaBuilder = new StringBuilder();
			feaBuilder.append("<features>\n");
			for (String feature : _features) {
				feaBuilder.append("<" + feature + "/>\n");
			}
			feaBuilder.append("</features>\n");
			return feaBuilder.toString();
		}
	}
	public static class termPolicy {
		private String _reboot;
		private String _powerOff;
		private String _crash;
		public termPolicy() {
			_reboot = _powerOff = _crash = "destroy";
		}
		public void setRebootPolicy(String rbPolicy) {
			_reboot = rbPolicy;
		}
		public void setPowerOffPolicy(String poPolicy) {
			_powerOff = poPolicy;
		}
		public void setCrashPolicy(String crashPolicy) {
			_crash = crashPolicy;
		}
		@Override
        public String toString() {
			StringBuilder term = new StringBuilder();
			term.append("<on_reboot>" + _reboot + "</on_reboot>\n");
			term.append("<on_poweroff>" + _powerOff + "</on_poweroff>\n");
			term.append("<on_crash>" + _powerOff + "</on_crash>\n");
			return term.toString();
		}
	}
	
	public static class devicesDef {
		private String _emulator;
		private final List<Object> devices = new ArrayList<Object>();
		public boolean addDevice(Object device) {
			return devices.add(device);
		}
		public void setEmulatorPath(String emulator) {
			_emulator = emulator;
		}
		@Override
        public String toString() {
			StringBuilder devicesBuilder = new StringBuilder();
			devicesBuilder.append("<devices>\n");
			if (_emulator != null) {
				devicesBuilder.append("<emulator>" + _emulator + "</emulator>\n");
			}
			for (Object o : devices) {
				devicesBuilder.append(o.toString());
			}
			devicesBuilder.append("</devices>\n");
			return devicesBuilder.toString();
		}
		
	}
	public static class diskDef {
		enum deviceType {
			FLOOPY("floopy"),
			DISK("disk"),
			CDROM("cdrom");
			String _type;
			deviceType(String type) {
				_type = type;
			}
			@Override
            public String toString() {
				return _type;
			}
		}
		enum diskType {
			FILE("file"),
			BLOCK("block"),
			DIRECTROY("dir");
			String _diskType;
			diskType(String type) {
				_diskType = type;
			}
			@Override
            public String toString() {
				return _diskType;
			}
		}
		enum diskBus {
			IDE("ide"),
			SCSI("scsi"),
			VIRTIO("virtio"),
			XEN("xen"),
			USB("usb"),
			UML("uml"),
			FDC("fdc");
			String _bus;
			diskBus(String bus) {
				_bus = bus;
			}
			@Override
            public String toString() {
				return _bus;
			}
		}
		
		private deviceType _deviceType; /*floppy, disk, cdrom*/
		private diskType _diskType;
		private String _sourcePath;
		private String _diskLabel;
		private diskBus _bus;
		private boolean _readonly = false;
		private boolean _shareable = false;
		private boolean _deferAttach = false;
		public void setDeviceType(deviceType deviceType) {
			_deviceType = deviceType;
		}
		public void defFileBasedDisk(String filePath, String diskLabel, diskBus bus) {
			_diskType = diskType.FILE;
			_deviceType = deviceType.DISK;
			_sourcePath = filePath;
			_diskLabel = diskLabel;
			_bus = bus;

		}
		public void defBlockBasedDisk(String diskName, String diskLabel, diskBus bus) {
			_diskType = diskType.BLOCK;
			_deviceType = deviceType.DISK;
			_sourcePath = diskName;
			_diskLabel = diskLabel;
			_bus = bus;
		}
		public void setReadonly() {
			_readonly = true;
		}
		public void setSharable() {
			_shareable = true;
		}
		public void setAttachDeferred(boolean deferAttach) {
			_deferAttach = deferAttach;
		}
		public boolean isAttachDeferred() {
			return _deferAttach;
		}
		public String getDiskPath() {
			return _sourcePath;
		}
		public String getDiskLabel() {
			return _diskLabel;
		}
		@Override
        public String toString() {
			StringBuilder diskBuilder = new StringBuilder();
			diskBuilder.append("<disk ");
			if (_deviceType != null) {
				diskBuilder.append(" device='" + _deviceType + "'");
			}
			diskBuilder.append(" type='" + _diskType + "'");
			diskBuilder.append(">\n");
			if (_diskType == diskType.FILE) {
				diskBuilder.append("<source ");
				if (_sourcePath != null) {
					diskBuilder.append("file='" + _sourcePath + "'");
				} else if (_deviceType == deviceType.CDROM) {
					diskBuilder.append("file=''");
				}
				diskBuilder.append("/>\n");
			} else if (_diskType == diskType.BLOCK) {
				diskBuilder.append("<source");
				if (_sourcePath != null) {
					diskBuilder.append(" dev='" + _sourcePath + "'");
				}
				diskBuilder.append("/>\n");
			}
			diskBuilder.append("<target dev='" + _diskLabel + "'");
			if (_bus != null) {
				diskBuilder.append(" bus='" + _bus + "'");
			}
			diskBuilder.append("/>\n");
			diskBuilder.append("</disk>\n");
			return diskBuilder.toString();
		}
	}
	
	public static class interfaceDef {
		enum guestNetType {
			BRIDGE("bridge"),
			NETWORK("network"),
			USER("user"),
			ETHERNET("ethernet"),
			INTERNAL("internal");
			String _type;
			guestNetType(String type) {
				_type = type;
			}
			@Override
            public String toString() {
				return _type;
			}
		}
		enum nicModel {
			E1000("e1000"),
			VIRTIO("virtio"),
			RTL8139("rtl8139"),
			NE2KPCI("ne2k_pci");
			String _model;
			nicModel(String model) {
				_model = model;
			}
			@Override
            public String toString() {
				return _model;
			}
		}
		enum hostNicType {
			DIRECT_ATTACHED_WITHOUT_DHCP,
			DIRECT_ATTACHED_WITH_DHCP,
			VNET,
			VLAN;
		}
		private guestNetType _netType; /*bridge, ethernet, network, user, internal*/
		private hostNicType _hostNetType; /*Only used by agent java code*/
		private String _sourceName;
		private String _networkName;
		private String _macAddr;
		private String _ipAddr;
		private String _scriptPath;
		private nicModel _model;
		public void defBridgeNet(String brName, String targetBrName, String macAddr, nicModel model) {
			_netType = guestNetType.BRIDGE;
			_sourceName = brName;
			_networkName = targetBrName;
			_macAddr = macAddr;
			_model = model;
		}
		public void defPrivateNet(String networkName, String targetName, String macAddr, nicModel model) {
			_netType = guestNetType.NETWORK;
			_sourceName = networkName;
			_networkName = targetName;
			_macAddr = macAddr;
			_model = model;
		}
		
		public void setHostNetType(hostNicType hostNetType) {
			_hostNetType = hostNetType;
		}
		
		public hostNicType getHostNetType() {
			return _hostNetType;
		}
		
		public String getBrName() {
			return _sourceName;
		}
		public guestNetType getNetType() {
			return _netType;
		}
		@Override
        public String toString() {
			StringBuilder netBuilder = new StringBuilder();
			netBuilder.append("<interface type='" + _netType +"'>\n");
			if (_netType == guestNetType.BRIDGE) {
				netBuilder.append("<source bridge='" + _sourceName +"'/>\n");
			} else if (_netType == guestNetType.NETWORK) {
				netBuilder.append("<source network='" + _sourceName +"'/>\n");
			}
			if (_networkName !=null) {
				netBuilder.append("<target dev='" + _networkName + "'/>\n");
			}
			if (_macAddr !=null) {
				netBuilder.append("<mac address='" + _macAddr + "'/>\n");
			}
			if (_model !=null) {
				netBuilder.append("<model type='" + _model + "'/>\n");
			}
			netBuilder.append("</interface>\n");
			return netBuilder.toString();
		}
	}
	public static class consoleDef {
		private final String _ttyPath;
		private final String _type;
		private final String _source;
		private short _port = -1;
		public consoleDef(String type, String path, String source, short port) {
			_type = type;
			_ttyPath = path;
			_source = source;
			_port = port;
		}
		@Override
        public String toString() {
			StringBuilder consoleBuilder = new StringBuilder();
			consoleBuilder.append("<console ");
			consoleBuilder.append("type='" + _type + "'");
			if (_ttyPath != null) {
				consoleBuilder.append("tty='" + _ttyPath + "'");
			}
			consoleBuilder.append(">\n");
			if (_source != null) {
				consoleBuilder.append("<source path='" + _source + "'/>\n");
			}
			if (_port != -1) {
				consoleBuilder.append("<target port='" + _port + "'/>\n");
			}
			consoleBuilder.append("</console>\n");
			return consoleBuilder.toString();
		}
	}
	public static class serialDef {
		private final String _type;
		private final String _source;
		private short _port = -1;
		public serialDef(String type, String source, short port) {
			_type = type;
			_source = source;
			_port = port;
		}
		@Override
        public String toString() {
			StringBuilder serialBuidler = new StringBuilder();
			serialBuidler.append("<serial type='" + _type + "'>\n");
			if (_source != null) {
				serialBuidler.append("<source path='" + _source + "'/>\n");
			}
			if (_port != -1) {
				serialBuidler.append("<target port='" + _port + "'/>\n");
			}
			serialBuidler.append("</serial>\n");
			return serialBuidler.toString();
		}
	}
	public  static class graphicDef {
		private final String _type;
		private short _port = -2;
		private boolean _autoPort = false;
		private final String _listenAddr;
		private final String _passwd;
		private final String _keyMap;
		public graphicDef(String type, short port, boolean auotPort, String listenAddr, String passwd, String keyMap) {
			_type = type;
			_port = port;
			_autoPort = auotPort;
			_listenAddr = listenAddr;
			_passwd = passwd;
			_keyMap = keyMap;
		}
		@Override
        public String toString() {
			StringBuilder graphicBuilder = new StringBuilder();
			graphicBuilder.append("<graphics type='" + _type + "'");
			if (_autoPort) {
				graphicBuilder.append(" autoport='yes'");
			} else if (_port != -2){
				graphicBuilder.append(" port='" + _port + "'");
			}
			if (_listenAddr != null) {
				graphicBuilder.append(" listen='" + _listenAddr + "'");
			} else {
				graphicBuilder.append(" listen='' ");
			}
			if (_passwd != null) {
				graphicBuilder.append(" passwd='" + _passwd + "'");
			} else if (_keyMap != null) {
				graphicBuilder.append(" _keymap='" + _keyMap + "'");
			}
			graphicBuilder.append("/>\n");
			return graphicBuilder.toString();
		}
	}
	public  static class inputDef {
		private final String _type; /*tablet, mouse*/
		private final String _bus; /*ps2, usb, xen*/
		public inputDef(String type, String bus) {
			_type = type;
			_bus = bus;
		}
		@Override
        public String toString() {
			StringBuilder inputBuilder = new StringBuilder();
			inputBuilder.append("<input type='" + _type + "'");
			if (_bus != null) {
				inputBuilder.append(" bus='" + _bus + "'");
			}
			inputBuilder.append("/>\n");
			return inputBuilder.toString();
		}
	}
	public void setHvsType(String hvs) {
		_hvsType = hvs;
	}
	public void setDomainName(String domainName) {
		_domName = domainName;
	}
	public void setDomUUID(String uuid) {
		_domUUID = uuid;
	}
	public void setDomDescription(String desc) {
		_desc = desc;
	}
	public boolean addComp(Object comp) {
		return components.add(comp);
	}
	@Override
    public String toString() {
		StringBuilder vmBuilder = new StringBuilder();
		vmBuilder.append("<domain type='" + _hvsType + "'>\n");
		vmBuilder.append("<name>" + _domName + "</name>\n");
		if (_domUUID != null) {
			vmBuilder.append("<uuid>" + _domUUID + "</uuid>\n");
		}
		if (_desc != null ) {
			vmBuilder.append("<description>" + _desc + "</description>\n");
		}
		for (Object o : components) {
			vmBuilder.append(o.toString());
		}
		vmBuilder.append("</domain>\n");
		return vmBuilder.toString();
	}
	
	public static void main(String [] args){
		System.out.println("testing");
		LibvirtVMDef vm = new LibvirtVMDef();
		vm.setHvsType("kvm");
		vm.setDomainName("testing");
		vm.setDomUUID(UUID.randomUUID().toString());
		
		guestDef guest = new guestDef();
		guest.setGuestType(guestDef.guestType.KVM);
		guest.setGuestArch("x86_64");
		guest.setMachineType("pc-0.11");
		guest.setBootOrder(guestDef.bootOrder.HARDISK);
		vm.addComp(guest);
		
		guestResourceDef grd = new guestResourceDef();
		grd.setMemorySize(512*1024);
		grd.setVcpuNum(1);
		vm.addComp(grd);
		
		featuresDef features = new featuresDef();
		features.addFeatures("pae");
		features.addFeatures("apic");
		features.addFeatures("acpi");
		vm.addComp(features);
		
		termPolicy term = new termPolicy();
		term.setCrashPolicy("destroy");
		term.setPowerOffPolicy("destroy");
		term.setRebootPolicy("destroy");
		vm.addComp(term);
		
		devicesDef devices = new devicesDef();
		devices.setEmulatorPath("/usr/bin/qemu-kvm");
		
		diskDef hda = new diskDef();
		hda.defFileBasedDisk("/path/to/hda1", "hda", diskDef.diskBus.IDE);
		devices.addDevice(hda);
		
		diskDef hdb = new diskDef();
		hdb.defFileBasedDisk("/path/to/hda2", "hdb",  diskDef.diskBus.IDE);
		devices.addDevice(hdb);
		
		interfaceDef pubNic = new interfaceDef();
		pubNic.defBridgeNet("cloudbr0", "vnet1", "00:16:3e:77:e2:a1", interfaceDef.nicModel.VIRTIO);
		devices.addDevice(pubNic);
		
		interfaceDef privNic = new interfaceDef();
		privNic.defPrivateNet("cloud-private", null, "00:16:3e:77:e2:a2", interfaceDef.nicModel.VIRTIO);
		devices.addDevice(privNic);
		
		interfaceDef vlanNic = new interfaceDef();
		vlanNic.defBridgeNet("vnbr1000", "tap1", "00:16:3e:77:e2:a2", interfaceDef.nicModel.VIRTIO);
		devices.addDevice(vlanNic);
		
		serialDef serial = new serialDef("pty", null, (short)0);
		devices.addDevice(serial);
		
		consoleDef console = new consoleDef("pty", null, null, (short)0);
		devices.addDevice(console);
		
		graphicDef grap = new graphicDef("vnc", (short)0, true, null, null, null);
		devices.addDevice(grap);
		
		inputDef input = new inputDef("tablet", "usb");
		devices.addDevice(input);
		
		vm.addComp(devices);
		
		System.out.println(vm.toString());
	}

}
