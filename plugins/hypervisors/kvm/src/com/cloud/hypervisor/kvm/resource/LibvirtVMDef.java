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
package com.cloud.hypervisor.kvm.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LibvirtVMDef {
    private String _hvsType;
    private String _domName;
    private String _domUUID;
    private String _desc;
    private final Map<String, Object> components = new HashMap<String, Object>();

    public static class GuestDef {
        enum guestType {
            KVM, XEN, EXE
        }

        enum bootOrder {
            HARDISK("hd"), CDROM("cdrom"), FLOPPY("fd"), NETWORK("network");
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

        public void setGuestType(guestType type) {
            _type = type;
        }

        public void setGuestArch(String arch) {
            _arch = arch;
        }

        public void setMachineType(String machine) {
            _machine = machine;
        }

        public void setLoader(String loader) {
            _loader = loader;
        }

        public void setBootKernel(String kernel, String initrd, String rootdev,
                String cmdline) {
            _kernel = kernel;
            _initrd = initrd;
            _root = rootdev;
            _cmdline = cmdline;
        }

        public void setBootOrder(bootOrder order) {
            _bootdevs.add(order);
        }

        @Override
        public String toString() {
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

    public static class GuestResourceDef {
        private long _mem;
        private int _currentMem = -1;
        private String _memBacking;
        private int _vcpu = -1;
        private boolean _memBalloning= false;

        public void setMemorySize(long mem) {
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

        public void setMemBalloning(boolean turnon){
              _memBalloning = turnon;
        }

        @Override
        public String toString() {
            StringBuilder resBuidler = new StringBuilder();
            resBuidler.append("<memory>" + _mem + "</memory>\n");
            if (_currentMem != -1) {
                resBuidler.append("<currentMemory>" + _currentMem
                        + "</currentMemory>\n");
            }
            if (_memBacking != null) {
                resBuidler.append("<memoryBacking>" + "<" + _memBacking + "/>"
                        + "</memoryBacking>\n");
            }
            if (_memBalloning){
                resBuidler.append("<devices>\n" + "<memballoon model='virtio'/>\n" + "</devices>\n");
            }
            if (_vcpu != -1) {
                resBuidler.append("<vcpu>" + _vcpu + "</vcpu>\n");
            }
            return resBuidler.toString();
        }
    }

    public static class FeaturesDef {
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

    public static class TermPolicy {
        private String _reboot;
        private String _powerOff;
        private String _crash;

        public TermPolicy() {
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

    public static class ClockDef {
        public enum ClockOffset {
            UTC("utc"), LOCALTIME("localtime"), TIMEZONE("timezone"), VARIABLE(
                    "variable");

            private String _offset;

            private ClockOffset(String offset) {
                _offset = offset;
            }

            @Override
            public String toString() {
                return _offset;
            }
        }

        private ClockOffset _offset;
        private String _timerName;
        private String _tickPolicy;
        private String _track;

        public ClockDef() {
            _offset = ClockOffset.UTC;
        }

        public void setClockOffset(ClockOffset offset) {
            _offset = offset;
        }

        public void setTimer(String timerName, String tickPolicy, String track) {
            _timerName = timerName;
            _tickPolicy = tickPolicy;
            _track = track;
        }

        @Override
        public String toString() {
            StringBuilder clockBuilder = new StringBuilder();
            clockBuilder.append("<clock offset='");
            clockBuilder.append(_offset.toString());
            clockBuilder.append("'>\n");
            if (_timerName != null) {
                clockBuilder.append("<timer name='");
                clockBuilder.append(_timerName);
                clockBuilder.append("' ");

                if (_tickPolicy != null) {
                    clockBuilder.append("tickpolicy='");
                    clockBuilder.append(_tickPolicy);
                    clockBuilder.append("' ");
                }

                if (_track != null) {
                    clockBuilder.append("track='");
                    clockBuilder.append(_track);
                    clockBuilder.append("' ");
                }

                clockBuilder.append(">\n");
                clockBuilder.append("</timer>\n");
            }
            clockBuilder.append("</clock>\n");
            return clockBuilder.toString();
        }
    }

    public static class DevicesDef {
        private String _emulator;
        private final Map<String, List<?>> devices = new HashMap<String, List<?>>();

        public boolean addDevice(Object device) {
            Object dev = devices.get(device.getClass().toString());
            if (dev == null) {
                List<Object> devs = new ArrayList<Object>();
                devs.add(device);
                devices.put(device.getClass().toString(), devs);
            } else {
                List<Object> devs = (List<Object>) dev;
                devs.add(device);
            }
            return true;
        }

        public void setEmulatorPath(String emulator) {
            _emulator = emulator;
        }

        @Override
        public String toString() {
            StringBuilder devicesBuilder = new StringBuilder();
            devicesBuilder.append("<devices>\n");
            if (_emulator != null) {
                devicesBuilder.append("<emulator>" + _emulator
                        + "</emulator>\n");
            }

            for (List<?> devs : devices.values()) {
                for (Object dev : devs) {
                    devicesBuilder.append(dev.toString());
                }
            }
            devicesBuilder.append("</devices>\n");
            return devicesBuilder.toString();
        }

        @SuppressWarnings("unchecked")
        public List<DiskDef> getDisks() {
            return (List<DiskDef>) devices.get(DiskDef.class.toString());
        }

        @SuppressWarnings("unchecked")
        public List<InterfaceDef> getInterfaces() {
            return (List<InterfaceDef>) devices.get(InterfaceDef.class
                    .toString());
        }

    }

    public static class DiskDef {
        enum deviceType {
            FLOPPY("floppy"), DISK("disk"), CDROM("cdrom");
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
            FILE("file"), BLOCK("block"), DIRECTROY("dir"), NETWORK("network");
            String _diskType;

            diskType(String type) {
                _diskType = type;
            }

            @Override
            public String toString() {
                return _diskType;
            }
        }

        enum diskProtocol {
            RBD("rbd"), SHEEPDOG("sheepdog");
            String _diskProtocol;

            diskProtocol(String protocol) {
                _diskProtocol = protocol;
            }

            @Override
            public String toString() {
                return _diskProtocol;
            }
        }

        enum diskBus {
            IDE("ide"), SCSI("scsi"), VIRTIO("virtio"), XEN("xen"), USB("usb"), UML(
                    "uml"), FDC("fdc");
            String _bus;

            diskBus(String bus) {
                _bus = bus;
            }

            @Override
            public String toString() {
                return _bus;
            }
        }

        enum diskFmtType {
            RAW("raw"), QCOW2("qcow2");
            String _fmtType;

            diskFmtType(String fmt) {
                _fmtType = fmt;
            }

            @Override
            public String toString() {
                return _fmtType;
            }
        }

        private deviceType _deviceType; /* floppy, disk, cdrom */
        private diskType _diskType;
        private diskProtocol _diskProtocol;
        private String _sourcePath;
        private String _sourceHost;
        private int _sourcePort;
        private String _authUserName;
        private String _authSecretUUID;
        private String _diskLabel;
        private diskBus _bus;
        private diskFmtType _diskFmtType; /* qcow2, raw etc. */
        private boolean _readonly = false;
        private boolean _shareable = false;
        private boolean _deferAttach = false;

        public void setDeviceType(deviceType deviceType) {
            _deviceType = deviceType;
        }

        public void defFileBasedDisk(String filePath, String diskLabel,
                diskBus bus, diskFmtType diskFmtType) {
            _diskType = diskType.FILE;
            _deviceType = deviceType.DISK;
            _sourcePath = filePath;
            _diskLabel = diskLabel;
            _diskFmtType = diskFmtType;
            _bus = bus;

        }

        /* skip iso label */
        private String getDevLabel(int devId, diskBus bus) {
            if (devId == 2) {
                devId++;
            }

            char suffix = (char) ('a' + devId);
            if (bus == diskBus.SCSI) {
                return "sd" + suffix;
            } else if (bus == diskBus.VIRTIO) {
                return "vd" + suffix;
            }
            return "hd" + suffix;

        }

        public void defFileBasedDisk(String filePath, int devId, diskBus bus,
                diskFmtType diskFmtType) {

            _diskType = diskType.FILE;
            _deviceType = deviceType.DISK;
            _sourcePath = filePath;
            _diskLabel = getDevLabel(devId, bus);
            _diskFmtType = diskFmtType;
            _bus = bus;

        }

        public void defISODisk(String volPath) {
            _diskType = diskType.FILE;
            _deviceType = deviceType.CDROM;
            _sourcePath = volPath;
            _diskLabel = "hdc";
            _diskFmtType = diskFmtType.RAW;
            _bus = diskBus.IDE;
        }

        public void defBlockBasedDisk(String diskName, int devId, diskBus bus) {
            _diskType = diskType.BLOCK;
            _deviceType = deviceType.DISK;
            _diskFmtType = diskFmtType.RAW;
            _sourcePath = diskName;
            _diskLabel = getDevLabel(devId, bus);
            _bus = bus;
        }

        public void defBlockBasedDisk(String diskName, String diskLabel,
                diskBus bus) {
            _diskType = diskType.BLOCK;
            _deviceType = deviceType.DISK;
            _diskFmtType = diskFmtType.RAW;
            _sourcePath = diskName;
            _diskLabel = diskLabel;
            _bus = bus;
        }

        public void defNetworkBasedDisk(String diskName, String sourceHost, int sourcePort,
                                        String authUserName, String authSecretUUID,
                                        int devId, diskBus bus, diskProtocol protocol) {
            _diskType = diskType.NETWORK;
            _deviceType = deviceType.DISK;
            _diskFmtType = diskFmtType.RAW;
            _sourcePath = diskName;
            _sourceHost = sourceHost;
            _sourcePort = sourcePort;
            _authUserName = authUserName;
            _authSecretUUID = authSecretUUID;
            _diskLabel = getDevLabel(devId, bus);
            _bus = bus;
            _diskProtocol = protocol;
        }

        public void defNetworkBasedDisk(String diskName, String sourceHost, int sourcePort,
                                        String authUserName, String authSecretUUID,
                                        String diskLabel, diskBus bus, diskProtocol protocol) {
            _diskType = diskType.NETWORK;
            _deviceType = deviceType.DISK;
            _diskFmtType = diskFmtType.RAW;
            _sourcePath = diskName;
            _sourceHost = sourceHost;
            _sourcePort = sourcePort;
            _authUserName = authUserName;
            _authSecretUUID = authSecretUUID;
            _diskLabel = diskLabel;
            _bus = bus;
            _diskProtocol = protocol;
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

        public deviceType getDeviceType() {
            return _deviceType;
        }

        public void setDiskPath(String volPath) {
            this._sourcePath = volPath;
        }

        public diskBus getBusType() {
            return _bus;
        }

        public int getDiskSeq() {
            char suffix = this._diskLabel.charAt(this._diskLabel.length() - 1);
            return suffix - 'a';
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
            diskBuilder.append("<driver name='qemu'" + " type='" + _diskFmtType
                    + "' cache='none' " + "/>\n");
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
            } else if (_diskType == diskType.NETWORK) {
                diskBuilder.append("<source ");
                diskBuilder.append(" protocol='" + _diskProtocol + "'");
                diskBuilder.append(" name='" + _sourcePath + "'");
                diskBuilder.append(">\n");
                diskBuilder.append("<host name='" + _sourceHost + "' port='" + _sourcePort + "'/>\n");
                diskBuilder.append("</source>\n");
                if (_authUserName != null) {
                    diskBuilder.append("<auth username='" + _authUserName + "'>\n");
                    diskBuilder.append("<secret type='ceph' uuid='" + _authSecretUUID + "'/>\n");
                    diskBuilder.append("</auth>\n");
                }
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

    public static class InterfaceDef {
        enum guestNetType {
            BRIDGE("bridge"), NETWORK("network"), USER("user"), ETHERNET(
                    "ethernet"), INTERNAL("internal");
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
            E1000("e1000"), VIRTIO("virtio"), RTL8139("rtl8139"), NE2KPCI(
                    "ne2k_pci");
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
            DIRECT_ATTACHED_WITHOUT_DHCP, DIRECT_ATTACHED_WITH_DHCP, VNET, VLAN;
        }

        private guestNetType _netType; /*
                                         * bridge, ethernet, network, user,
                                         * internal
                                         */
        private hostNicType _hostNetType; /* Only used by agent java code */
        private String _sourceName;
        private String _networkName;
        private String _macAddr;
        private String _ipAddr;
        private String _scriptPath;
        private nicModel _model;
        private String _virtualPortType;
        private String _virtualPortInterfaceId;
        private int _vlanTag = -1;

        public void defBridgeNet(String brName, String targetBrName,
                String macAddr, nicModel model) {
            _netType = guestNetType.BRIDGE;
            _sourceName = brName;
            _networkName = targetBrName;
            _macAddr = macAddr;
            _model = model;
        }

        public void defPrivateNet(String networkName, String targetName,
                String macAddr, nicModel model) {
            _netType = guestNetType.NETWORK;
            _sourceName = networkName;
            _networkName = targetName;
            _macAddr = macAddr;
            _model = model;
        }

        public void defEthernet(String targetName, String macAddr,  nicModel model) {
            _netType = guestNetType.ETHERNET;
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

        public String getDevName() {
            return _networkName;
        }

        public String getMacAddress() {
            return _macAddr;
        }
        
        public void setVirtualPortType(String virtualPortType) {
            _virtualPortType = virtualPortType;
        }

        public String getVirtualPortType() {
            return _virtualPortType;
        }

        public void setVirtualPortInterfaceId(String virtualPortInterfaceId) {
            _virtualPortInterfaceId = virtualPortInterfaceId;
        }

        public String getVirtualPortInterfaceId() {
            return _virtualPortInterfaceId;
        }

        public void setVlanTag(int vlanTag) {
            _vlanTag = vlanTag;
        }

        public int getVlanTag() {
            return _vlanTag;
        }
        
        @Override
        public String toString() {
            StringBuilder netBuilder = new StringBuilder();
            netBuilder.append("<interface type='" + _netType + "'>\n");
            if (_netType == guestNetType.BRIDGE) {
                netBuilder.append("<source bridge='" + _sourceName + "'/>\n");
            } else if (_netType == guestNetType.NETWORK) {
                netBuilder.append("<source network='" + _sourceName + "'/>\n");
            }
            if (_networkName != null) {
                netBuilder.append("<target dev='" + _networkName + "'/>\n");
            }
            if (_macAddr != null) {
                netBuilder.append("<mac address='" + _macAddr + "'/>\n");
            }
            if (_model != null) {
                netBuilder.append("<model type='" + _model + "'/>\n");
            }
            if (_virtualPortType != null) {
                netBuilder.append("<virtualport type='" + _virtualPortType + "'>\n");
                if (_virtualPortInterfaceId != null) {
                    netBuilder.append("<parameters interfaceid='" + _virtualPortInterfaceId + "'/>\n");
                }
                netBuilder.append("</virtualport>\n");
            }
            if (_vlanTag > 0 && _vlanTag < 4095) {
                netBuilder.append("<vlan trunk='no'>\n<tag id='" + _vlanTag + "'/>\n</vlan>");
            }
            netBuilder.append("</interface>\n");
            return netBuilder.toString();
        }
    }

    public static class ConsoleDef {
        private final String _ttyPath;
        private final String _type;
        private final String _source;
        private short _port = -1;

        public ConsoleDef(String type, String path, String source, short port) {
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

    public static class CpuTuneDef {
        private int _shares = 0;

        public void setShares(int shares) {
            _shares = shares;
        }

        public int getShares() {
            return _shares;
        }

        @Override
        public String toString() {
            StringBuilder cpuTuneBuilder = new StringBuilder();
            cpuTuneBuilder.append("<cputune>\n");
            if (_shares > 0) {
                cpuTuneBuilder.append("<shares>" + _shares + "</shares>\n");
            }
            cpuTuneBuilder.append("</cputune>\n");
            return cpuTuneBuilder.toString();
        }
    }

    public static class SerialDef {
        private final String _type;
        private final String _source;
        private short _port = -1;

        public SerialDef(String type, String source, short port) {
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

    public static class VirtioSerialDef {
        private final String _name;
        private String _path;

        public VirtioSerialDef(String name, String path) {
            _name = name;
            _path = path;
        }

        @Override
        public String toString() {
            StringBuilder virtioSerialBuilder = new StringBuilder();
            if(_path == null) {
                _path = "/var/lib/libvirt/qemu";
            }
            virtioSerialBuilder.append("<channel type='unix'>\n");
            virtioSerialBuilder.append("<source mode='bind' path='" + _path
                                        + "/" + _name + ".agent'/>\n");
            virtioSerialBuilder.append("<target type='virtio' name='" + _name + ".vport'/>\n");
            virtioSerialBuilder.append("<address type='virtio-serial'/>\n");
            virtioSerialBuilder.append("</channel>\n");
            return virtioSerialBuilder.toString();
        }
    }

    public static class GraphicDef {
        private final String _type;
        private short _port = -2;
        private boolean _autoPort = false;
        private final String _listenAddr;
        private final String _passwd;
        private final String _keyMap;

        public GraphicDef(String type, short port, boolean autoPort,
                String listenAddr, String passwd, String keyMap) {
            _type = type;
            _port = port;
            _autoPort = autoPort;
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
            } else if (_port != -2) {
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

    public static class InputDef {
        private final String _type; /* tablet, mouse */
        private final String _bus; /* ps2, usb, xen */

        public InputDef(String type, String bus) {
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

    public String getGuestOSType() {
        return _desc;
    }

    public void addComp(Object comp) {
        components.put(comp.getClass().toString(), comp);
    }

    public DevicesDef getDevices() {
        Object o = components.get(DevicesDef.class.toString());
        if (o != null) {
            return (DevicesDef) o;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder vmBuilder = new StringBuilder();
        vmBuilder.append("<domain type='" + _hvsType + "'>\n");
        vmBuilder.append("<name>" + _domName + "</name>\n");
        if (_domUUID != null) {
            vmBuilder.append("<uuid>" + _domUUID + "</uuid>\n");
        }
        if (_desc != null) {
            vmBuilder.append("<description>" + _desc + "</description>\n");
        }
        for (Object o : components.values()) {
            vmBuilder.append(o.toString());
        }
        vmBuilder.append("</domain>\n");
        return vmBuilder.toString();
    }
}
