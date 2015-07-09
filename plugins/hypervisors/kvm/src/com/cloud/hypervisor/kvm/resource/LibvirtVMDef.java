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

import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibvirtVMDef {
    private String _hvsType;
    private static long s_libvirtVersion;
    private static long s_qemuVersion;
    private String _domName;
    private String _domUUID;
    private String _desc;
    private String _platformEmulator;
    private final Map<String, Object> components = new HashMap<String, Object>();

    public static class GuestDef {
        enum GuestType {
            KVM, XEN, EXE, LXC
        }

        enum BootOrder {
            HARDISK("hd"), CDROM("cdrom"), FLOPPY("fd"), NETWORK("network");
            String _order;

            BootOrder(String order) {
                _order = order;
            }

            @Override
            public String toString() {
                return _order;
            }
        }

        private GuestType _type;
        private String _arch;
        private String _loader;
        private String _kernel;
        private String _initrd;
        private String _root;
        private String _cmdline;
        private String _uuid;
        private final List<BootOrder> _bootdevs = new ArrayList<BootOrder>();
        private String _machine;

        public void setGuestType(GuestType type) {
            _type = type;
        }

        public GuestType getGuestType() {
            return _type;
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

        public void setBootKernel(String kernel, String initrd, String rootdev, String cmdline) {
            _kernel = kernel;
            _initrd = initrd;
            _root = rootdev;
            _cmdline = cmdline;
        }

        public void setBootOrder(BootOrder order) {
            _bootdevs.add(order);
        }

        public void setUuid(String uuid) {
            _uuid = uuid;
        }

        @Override
        public String toString() {
            if (_type == GuestType.KVM) {
                StringBuilder guestDef = new StringBuilder();

                guestDef.append("<sysinfo type='smbios'>\n");
                guestDef.append("<system>\n");
                guestDef.append("<entry name='manufacturer'>Apache Software Foundation</entry>\n");
                guestDef.append("<entry name='product'>CloudStack " + _type.toString() + " Hypervisor</entry>\n");
                guestDef.append("<entry name='uuid'>" + _uuid + "</entry>\n");
                guestDef.append("</system>\n");
                guestDef.append("</sysinfo>\n");

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
                    for (BootOrder bo : _bootdevs) {
                        guestDef.append("<boot dev='" + bo + "'/>\n");
                    }
                }
                guestDef.append("<smbios mode='sysinfo'/>\n");
                guestDef.append("</os>\n");
                return guestDef.toString();
            } else if (_type == GuestType.LXC) {
                StringBuilder guestDef = new StringBuilder();
                guestDef.append("<os>\n");
                guestDef.append("<type>exe</type>\n");
                guestDef.append("<init>/sbin/init</init>\n");
                guestDef.append("</os>\n");
                return guestDef.toString();
            } else {
                return null;
            }
        }
    }

    public static class GuestResourceDef {
        private long _mem;
        private long _currentMem = -1;
        private String _memBacking;
        private int _vcpu = -1;
        private boolean _memBalloning = false;

        public void setMemorySize(long mem) {
            _mem = mem;
        }

        public void setCurrentMem(long currMem) {
            _currentMem = currMem;
        }

        public void setMemBacking(String memBacking) {
            _memBacking = memBacking;
        }

        public void setVcpuNum(int vcpu) {
            _vcpu = vcpu;
        }

        public void setMemBalloning(boolean turnon) {
            _memBalloning = turnon;
        }

        @Override
        public String toString() {
            StringBuilder resBuidler = new StringBuilder();
            resBuidler.append("<memory>" + _mem + "</memory>\n");
            if (_currentMem != -1) {
                resBuidler.append("<currentMemory>" + _currentMem + "</currentMemory>\n");
            }
            if (_memBacking != null) {
                resBuidler.append("<memoryBacking>" + "<" + _memBacking + "/>" + "</memoryBacking>\n");
            }
            if (_memBalloning) {
                resBuidler.append("<devices>\n" + "<memballoon model='virtio'/>\n" + "</devices>\n");
            } else {
                resBuidler.append("<devices>\n" + "<memballoon model='none'/>\n" + "</devices>\n");
            }
            if (_vcpu != -1) {
                resBuidler.append("<vcpu>" + _vcpu + "</vcpu>\n");
            }
            return resBuidler.toString();
        }
    }

    public static class HyperVEnlightenmentFeatureDef {
        private final Map<String, String> features = new HashMap<String,String>();
        public void setRelaxed(boolean on) {
            String state = on ? "On":"Off";
            features.put("relaxed", state);
        }
        @Override
        public String toString() {
            if (features.isEmpty()) {
                return "";
            }
            StringBuilder feaBuilder = new StringBuilder();
            feaBuilder.append("<hyperv>\n");
            for (Map.Entry<String, String> e : features.entrySet()) {
                feaBuilder.append("<");
                feaBuilder.append(e.getKey());
                feaBuilder.append(" state='" + e.getValue() + "'");
                feaBuilder.append("/>\n");
            }
            feaBuilder.append("</hyperv>\n");
            return feaBuilder.toString();
        }
    }

    public static class FeaturesDef {
        private final List<String> _features = new ArrayList<String>();

        private HyperVEnlightenmentFeatureDef hyperVEnlightenmentFeatureDef = null;
        public void addFeatures(String feature) {
            _features.add(feature);
        }

        public void addHyperVFeature(HyperVEnlightenmentFeatureDef hyperVEnlightenmentFeatureDef) {
            this.hyperVEnlightenmentFeatureDef = hyperVEnlightenmentFeatureDef;
        }

        @Override
        public String toString() {
            StringBuilder feaBuilder = new StringBuilder();
            feaBuilder.append("<features>\n");
            for (String feature : _features) {
                feaBuilder.append("<" + feature + "/>\n");
            }
            if (hyperVEnlightenmentFeatureDef != null) {
                String hpervF = hyperVEnlightenmentFeatureDef.toString();
                if (!hpervF.isEmpty()) {
                    feaBuilder.append(hpervF);
                }
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
            UTC("utc"), LOCALTIME("localtime"), TIMEZONE("timezone"), VARIABLE("variable");

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
        private boolean _noKvmClock = false;

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

        public void setTimer(String timerName, String tickPolicy, String track, boolean noKvmClock) {
            _noKvmClock = noKvmClock;
            setTimer(timerName, tickPolicy, track);
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

                if (_timerName.equals("kvmclock") && _noKvmClock) {
                    clockBuilder.append("present='no' />");
                } else {
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
            }
            clockBuilder.append("</clock>\n");
            return clockBuilder.toString();
        }
    }

    public static class DevicesDef {
        private String _emulator;
        private GuestDef.GuestType _guestType;
        private final Map<String, List<?>> devices = new HashMap<String, List<?>>();

        public boolean addDevice(Object device) {
            Object dev = devices.get(device.getClass().toString());
            if (dev == null) {
                List<Object> devs = new ArrayList<Object>();
                devs.add(device);
                devices.put(device.getClass().toString(), devs);
            } else {
                List<Object> devs = (List<Object>)dev;
                devs.add(device);
            }
            return true;
        }

        public void setEmulatorPath(String emulator) {
            _emulator = emulator;
        }

        public void setGuestType(GuestDef.GuestType guestType) {
            _guestType = guestType;
        }

        @Override
        public String toString() {
            StringBuilder devicesBuilder = new StringBuilder();
            devicesBuilder.append("<devices>\n");
            if (_emulator != null) {
                devicesBuilder.append("<emulator>" + _emulator + "</emulator>\n");
            }

            for (List<?> devs : devices.values()) {
                for (Object dev : devs) {
                    if (_guestType == GuestDef.GuestType.LXC) {
                        if (dev instanceof GraphicDef || dev instanceof InputDef) {
                            continue;
                        }
                        if(dev instanceof DiskDef){
                            DiskDef disk = (DiskDef)dev;
                            if(!disk.getDiskType().toString().equals("block")){
                                continue;
                            }
                        }
                    }
                    devicesBuilder.append(dev.toString());
                }
            }
            devicesBuilder.append("</devices>\n");
            return devicesBuilder.toString();
        }

        @SuppressWarnings("unchecked")
        public List<DiskDef> getDisks() {
            return (List<DiskDef>)devices.get(DiskDef.class.toString());
        }

        @SuppressWarnings("unchecked")
        public List<InterfaceDef> getInterfaces() {
            return (List<InterfaceDef>)devices.get(InterfaceDef.class.toString());
        }

    }

    public static class DiskDef {
        public enum DeviceType {
            FLOPPY("floppy"), DISK("disk"), CDROM("cdrom");
            String _type;

            DeviceType(String type) {
                _type = type;
            }

            @Override
            public String toString() {
                return _type;
            }
        }

        enum DiskType {
            FILE("file"), BLOCK("block"), DIRECTROY("dir"), NETWORK("network");
            String _diskType;

            DiskType(String type) {
                _diskType = type;
            }

            @Override
            public String toString() {
                return _diskType;
            }
        }

        public enum DiskProtocol {
            RBD("rbd"), SHEEPDOG("sheepdog"), GLUSTER("gluster");
            String _diskProtocol;

            DiskProtocol(String protocol) {
                _diskProtocol = protocol;
            }

            @Override
            public String toString() {
                return _diskProtocol;
            }
        }

        public enum DiskBus {
            IDE("ide"), SCSI("scsi"), VIRTIO("virtio"), XEN("xen"), USB("usb"), UML("uml"), FDC("fdc");
            String _bus;

            DiskBus(String bus) {
                _bus = bus;
            }

            @Override
            public String toString() {
                return _bus;
            }
        }

        public enum DiskFmtType {
            RAW("raw"), QCOW2("qcow2");
            String _fmtType;

            DiskFmtType(String fmt) {
                _fmtType = fmt;
            }

            @Override
            public String toString() {
                return _fmtType;
            }
        }

        public enum DiskCacheMode {
            NONE("none"), WRITEBACK("writeback"), WRITETHROUGH("writethrough");
            String _diskCacheMode;

            DiskCacheMode(String cacheMode) {
                _diskCacheMode = cacheMode;
            }

            @Override
            public String toString() {
                if (_diskCacheMode == null) {
                    return "NONE";
                }
                return _diskCacheMode;
            }
        }

        private DeviceType _deviceType; /* floppy, disk, cdrom */
        private DiskType _diskType;
        private DiskProtocol _diskProtocol;
        private String _sourcePath;
        private String _sourceHost;
        private int _sourcePort;
        private String _authUserName;
        private String _authSecretUUID;
        private String _diskLabel;
        private DiskBus _bus;
        private DiskFmtType _diskFmtType; /* qcow2, raw etc. */
        private boolean _readonly = false;
        private boolean _shareable = false;
        private boolean _deferAttach = false;
        private Long _bytesReadRate;
        private Long _bytesWriteRate;
        private Long _iopsReadRate;
        private Long _iopsWriteRate;
        private DiskCacheMode _diskCacheMode;
        private boolean qemuDriver = true;

        public void setDeviceType(DeviceType deviceType) {
            _deviceType = deviceType;
        }

        public void defFileBasedDisk(String filePath, String diskLabel, DiskBus bus, DiskFmtType diskFmtType) {
            _diskType = DiskType.FILE;
            _deviceType = DeviceType.DISK;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = filePath;
            _diskLabel = diskLabel;
            _diskFmtType = diskFmtType;
            _bus = bus;

        }

        /* skip iso label */
        private String getDevLabel(int devId, DiskBus bus) {
            if (devId == 2) {
                devId++;
            }

            char suffix = (char)('a' + devId);
            if (bus == DiskBus.SCSI) {
                return "sd" + suffix;
            } else if (bus == DiskBus.VIRTIO) {
                return "vd" + suffix;
            }
            return "hd" + suffix;

        }

        public void defFileBasedDisk(String filePath, int devId, DiskBus bus, DiskFmtType diskFmtType) {

            _diskType = DiskType.FILE;
            _deviceType = DeviceType.DISK;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = filePath;
            _diskLabel = getDevLabel(devId, bus);
            _diskFmtType = diskFmtType;
            _bus = bus;

        }

        public void defISODisk(String volPath) {
            _diskType = DiskType.FILE;
            _deviceType = DeviceType.CDROM;
            _sourcePath = volPath;
            _diskLabel = "hdc";
            _diskFmtType = DiskFmtType.RAW;
            _diskCacheMode = DiskCacheMode.NONE;
            _bus = DiskBus.IDE;
        }

        public void defBlockBasedDisk(String diskName, int devId, DiskBus bus) {
            _diskType = DiskType.BLOCK;
            _deviceType = DeviceType.DISK;
            _diskFmtType = DiskFmtType.RAW;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = diskName;
            _diskLabel = getDevLabel(devId, bus);
            _bus = bus;
        }

        public void defBlockBasedDisk(String diskName, String diskLabel, DiskBus bus) {
            _diskType = DiskType.BLOCK;
            _deviceType = DeviceType.DISK;
            _diskFmtType = DiskFmtType.RAW;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = diskName;
            _diskLabel = diskLabel;
            _bus = bus;
        }

        public void defNetworkBasedDisk(String diskName, String sourceHost, int sourcePort, String authUserName, String authSecretUUID, int devId, DiskBus bus,
                DiskProtocol protocol, DiskFmtType diskFmtType) {
            _diskType = DiskType.NETWORK;
            _deviceType = DeviceType.DISK;
            _diskFmtType = diskFmtType;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = diskName;
            _sourceHost = sourceHost;
            _sourcePort = sourcePort;
            _authUserName = authUserName;
            _authSecretUUID = authSecretUUID;
            _diskLabel = getDevLabel(devId, bus);
            _bus = bus;
            _diskProtocol = protocol;
        }

        public void defNetworkBasedDisk(String diskName, String sourceHost, int sourcePort, String authUserName, String authSecretUUID, String diskLabel, DiskBus bus,
                DiskProtocol protocol, DiskFmtType diskFmtType) {
            _diskType = DiskType.NETWORK;
            _deviceType = DeviceType.DISK;
            _diskFmtType = diskFmtType;
            _diskCacheMode = DiskCacheMode.NONE;
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

        public DiskType getDiskType() {
            return _diskType;
        }

        public DeviceType getDeviceType() {
            return _deviceType;
        }

        public void setDiskPath(String volPath) {
            _sourcePath = volPath;
        }

        public DiskBus getBusType() {
            return _bus;
        }

        public DiskFmtType getDiskFormatType() {
            return _diskFmtType;
        }

        public int getDiskSeq() {
            char suffix = _diskLabel.charAt(_diskLabel.length() - 1);
            return suffix - 'a';
        }

        public void setBytesReadRate(Long bytesReadRate) {
            _bytesReadRate = bytesReadRate;
        }

        public void setBytesWriteRate(Long bytesWriteRate) {
            _bytesWriteRate = bytesWriteRate;
        }

        public void setIopsReadRate(Long iopsReadRate) {
            _iopsReadRate = iopsReadRate;
        }

        public void setIopsWriteRate(Long iopsWriteRate) {
            _iopsWriteRate = iopsWriteRate;
        }

        public void setCacheMode(DiskCacheMode cacheMode) {
            _diskCacheMode = cacheMode;
        }

        public DiskCacheMode getCacheMode() {
            return _diskCacheMode;
        }

        public void setQemuDriver(boolean qemuDriver){
            this.qemuDriver = qemuDriver;
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
            if(qemuDriver) {
                diskBuilder.append("<driver name='qemu'" + " type='" + _diskFmtType
                        + "' cache='" + _diskCacheMode + "' " + "/>\n");
            }

            if (_diskType == DiskType.FILE) {
                diskBuilder.append("<source ");
                if (_sourcePath != null) {
                    diskBuilder.append("file='" + _sourcePath + "'");
                } else if (_deviceType == DeviceType.CDROM) {
                    diskBuilder.append("file=''");
                }
                diskBuilder.append("/>\n");
            } else if (_diskType == DiskType.BLOCK) {
                diskBuilder.append("<source");
                if (_sourcePath != null) {
                    diskBuilder.append(" dev='" + _sourcePath + "'");
                }
                diskBuilder.append("/>\n");
            } else if (_diskType == DiskType.NETWORK) {
                diskBuilder.append("<source ");
                diskBuilder.append(" protocol='" + _diskProtocol + "'");
                diskBuilder.append(" name='" + _sourcePath + "'");
                diskBuilder.append(">\n");
                diskBuilder.append("<host name='");
                diskBuilder.append(_sourceHost);
                if (_sourcePort != 0) {
                    diskBuilder.append("' port='");
                    diskBuilder.append(_sourcePort);
                }
                diskBuilder.append("'/>\n");
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

            if ((_deviceType != DeviceType.CDROM) &&
                    (s_libvirtVersion >= 9008) &&
                    (s_qemuVersion >= 1001000) &&
                    (((_bytesReadRate != null) && (_bytesReadRate > 0)) || ((_bytesWriteRate != null) && (_bytesWriteRate > 0)) ||
                            ((_iopsReadRate != null) && (_iopsReadRate > 0)) || ((_iopsWriteRate != null) && (_iopsWriteRate > 0)))) { // not CDROM, from libvirt 0.9.8 and QEMU 1.1.0
                diskBuilder.append("<iotune>\n");
                if ((_bytesReadRate != null) && (_bytesReadRate > 0))
                    diskBuilder.append("<read_bytes_sec>" + _bytesReadRate + "</read_bytes_sec>\n");
                if ((_bytesWriteRate != null) && (_bytesWriteRate > 0))
                    diskBuilder.append("<write_bytes_sec>" + _bytesWriteRate + "</write_bytes_sec>\n");
                if ((_iopsReadRate != null) && (_iopsReadRate > 0))
                    diskBuilder.append("<read_iops_sec>" + _iopsReadRate + "</read_iops_sec>\n");
                if ((_iopsWriteRate != null) && (_iopsWriteRate > 0))
                    diskBuilder.append("<write_iops_sec>" + _iopsWriteRate + "</write_iops_sec>\n");
                diskBuilder.append("</iotune>\n");
            }

            diskBuilder.append("</disk>\n");
            return diskBuilder.toString();
        }
    }

    public static class InterfaceDef {
        enum GuestNetType {
            BRIDGE("bridge"), DIRECT("direct"), NETWORK("network"), USER("user"), ETHERNET("ethernet"), INTERNAL("internal");
            String _type;

            GuestNetType(String type) {
                _type = type;
            }

            @Override
            public String toString() {
                return _type;
            }
        }

        enum NicModel {
            E1000("e1000"), VIRTIO("virtio"), RTL8139("rtl8139"), NE2KPCI("ne2k_pci"), VMXNET3("vmxnet3");
            String _model;

            NicModel(String model) {
                _model = model;
            }

            @Override
            public String toString() {
                return _model;
            }
        }

        enum HostNicType {
            DIRECT_ATTACHED_WITHOUT_DHCP, DIRECT_ATTACHED_WITH_DHCP, VNET, VLAN;
        }

        private GuestNetType _netType; /*
         * bridge, ethernet, network, user,
         * internal
         */
        private HostNicType _hostNetType; /* Only used by agent java code */
        private String _netSourceMode;
        private String _sourceName;
        private String _networkName;
        private String _macAddr;
        private String _ipAddr;
        private String _scriptPath;
        private NicModel _model;
        private Integer _networkRateKBps;
        private String _virtualPortType;
        private String _virtualPortInterfaceId;
        private int _vlanTag = -1;
        private boolean _pxeDisable = false;

        public void defBridgeNet(String brName, String targetBrName, String macAddr, NicModel model) {
            defBridgeNet(brName, targetBrName, macAddr, model, 0);
        }

        public void defBridgeNet(String brName, String targetBrName, String macAddr, NicModel model, Integer networkRateKBps) {
            _netType = GuestNetType.BRIDGE;
            _sourceName = brName;
            _networkName = targetBrName;
            _macAddr = macAddr;
            _model = model;
            _networkRateKBps = networkRateKBps;
        }

        public void defDirectNet(String sourceName, String targetName, String macAddr, NicModel model, String sourceMode) {
            defDirectNet(sourceName, targetName, macAddr, model, sourceMode, 0);
        }

        public void defDirectNet(String sourceName, String targetName, String macAddr, NicModel model, String sourceMode, Integer networkRateKBps) {
            _netType = GuestNetType.DIRECT;
            _netSourceMode = sourceMode;
            _sourceName = sourceName;
            _networkName = targetName;
            _macAddr = macAddr;
            _model = model;
            _networkRateKBps = networkRateKBps;
        }

        public void defPrivateNet(String networkName, String targetName, String macAddr, NicModel model) {
            defPrivateNet(networkName, targetName, macAddr, model, 0);
        }

        public void defPrivateNet(String networkName, String targetName, String macAddr, NicModel model, Integer networkRateKBps) {
            _netType = GuestNetType.NETWORK;
            _sourceName = networkName;
            _networkName = targetName;
            _macAddr = macAddr;
            _model = model;
            _networkRateKBps = networkRateKBps;
        }

        public void defEthernet(String targetName, String macAddr, NicModel model, String scriptPath) {
            defEthernet(targetName, macAddr, model, scriptPath, 0);
        }

        public void defEthernet(String targetName, String macAddr, NicModel model, String scriptPath, Integer networkRateKBps) {
            _netType = GuestNetType.ETHERNET;
            _networkName = targetName;
            _sourceName = targetName;
            _macAddr = macAddr;
            _model = model;
            _scriptPath = scriptPath;
            _networkRateKBps = networkRateKBps;
        }

        public void defEthernet(String targetName, String macAddr, NicModel model) {
            defEthernet(targetName, macAddr, model, null);
        }

        public void setHostNetType(HostNicType hostNetType) {
            _hostNetType = hostNetType;
        }

        public HostNicType getHostNetType() {
            return _hostNetType;
        }

        public void setPxeDisable(boolean pxeDisable) {
            _pxeDisable = pxeDisable;
        }

        public String getBrName() {
            return _sourceName;
        }

        public GuestNetType getNetType() {
            return _netType;
        }

        public String getNetSourceMode() {
            return _netSourceMode;
        }

        public String getDevName() {
            return _networkName;
        }

        public String getMacAddress() {
            return _macAddr;
        }

        public NicModel getModel() {
            return _model;
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
            if (_netType == GuestNetType.BRIDGE) {
                netBuilder.append("<source bridge='" + _sourceName + "'/>\n");
            } else if (_netType == GuestNetType.NETWORK) {
                netBuilder.append("<source network='" + _sourceName + "'/>\n");
            } else if (_netType == GuestNetType.DIRECT) {
                netBuilder.append("<source dev='" + _sourceName + "' mode='" + _netSourceMode + "'/>\n");
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
            if ((s_libvirtVersion >= 9004) && (_networkRateKBps > 0)) { // supported from libvirt 0.9.4
                netBuilder.append("<bandwidth>\n");
                netBuilder.append("<inbound average='" + _networkRateKBps + "' peak='" + _networkRateKBps + "'/>\n");
                netBuilder.append("<outbound average='" + _networkRateKBps + "' peak='" + _networkRateKBps + "'/>\n");
                netBuilder.append("</bandwidth>\n");
            }
            if (_scriptPath != null) {
                netBuilder.append("<script path='" + _scriptPath + "'/>\n");
            }
            if (_pxeDisable) {
                netBuilder.append("<rom bar='off' file=''/>");
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

    public static class CpuModeDef {
        private String _mode;
        private String _model;
        private List<String> _features;
        private int _coresPerSocket = -1;
        private int _sockets = -1;

        public void setMode(String mode) {
            _mode = mode;
        }

        public void setFeatures(List<String> features) {
            if (features != null) {
                _features = features;
            }
        }

        public void setModel(String model) {
            _model = model;
        }

        public void setTopology(int coresPerSocket, int sockets) {
            _coresPerSocket = coresPerSocket;
            _sockets = sockets;
        }

        @Override
        public String toString() {
            StringBuilder modeBuilder = new StringBuilder();

            // start cpu def, adding mode, model
            if ("custom".equalsIgnoreCase(_mode) && _model != null){
                modeBuilder.append("<cpu mode='custom' match='exact'><model fallback='allow'>" + _model + "</model>");
            } else if ("host-model".equals(_mode)) {
                modeBuilder.append("<cpu mode='host-model'><model fallback='allow'></model>");
            } else if ("host-passthrough".equals(_mode)) {
                modeBuilder.append("<cpu mode='host-passthrough'>");
            } else {
                modeBuilder.append("<cpu>");
            }

            if (_features != null) {
                for (String feature : _features) {
                    modeBuilder.append("<feature policy='require' name='" + feature + "'/>");
                }
            }

            // add topology
            if (_sockets > 0 && _coresPerSocket > 0) {
                modeBuilder.append("<topology sockets='" + _sockets + "' cores='" + _coresPerSocket + "' threads='1' />");
            }

            // close cpu def
            modeBuilder.append("</cpu>");
            return modeBuilder.toString();
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

    public static class VideoDef {
        private String _videoModel;
        private int _videoRam;

        public VideoDef(String videoModel, int videoRam) {
            _videoModel = videoModel;
            _videoRam = videoRam;
        }

        @Override
        public String toString() {
            StringBuilder videoBuilder = new StringBuilder();
            if (_videoModel != null && !_videoModel.isEmpty() && _videoRam != 0){
                videoBuilder.append("<video>\n");
                videoBuilder.append("<model type='" + _videoModel + "' vram='" + _videoRam + "'/>\n");
                videoBuilder.append("</video>\n");
                return videoBuilder.toString();
            }
            return "";
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
            if (_path == null) {
                _path = "/var/lib/libvirt/qemu";
            }
            virtioSerialBuilder.append("<channel type='unix'>\n");
            virtioSerialBuilder.append("<source mode='bind' path='" + _path + "/" + _name + ".agent'/>\n");
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

        public GraphicDef(String type, short port, boolean autoPort, String listenAddr, String passwd, String keyMap) {
            _type = type;
            _port = port;
            _autoPort = autoPort;
            _listenAddr = listenAddr;
            _passwd = StringEscapeUtils.escapeXml(passwd);
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
                graphicBuilder.append(" listen=''");
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

    public static class FilesystemDef {
        private final String _sourcePath;
        private final String _targetPath;

        public FilesystemDef(String sourcePath, String targetPath) {
            _sourcePath = sourcePath;
            _targetPath = targetPath;
        }

        @Override
        public String toString() {
            StringBuilder fsBuilder = new StringBuilder();
            fsBuilder.append("<filesystem type='mount'>\n");
            fsBuilder.append("  <source dir='" + _sourcePath + "'/>\n");
            fsBuilder.append("  <target dir='" + _targetPath + "'/>\n");
            fsBuilder.append("</filesystem>\n");
            return fsBuilder.toString();
        }
    }

    public void setHvsType(String hvs) {
        _hvsType = hvs;
    }

    public String getHvsType() {
        return _hvsType;
    }

    public static void setGlobalLibvirtVersion(long libvirtVersion) {
        s_libvirtVersion = libvirtVersion;
    }

    public void setLibvirtVersion(long libvirtVersion) {
        setGlobalLibvirtVersion(libvirtVersion);
    }

    public static void setGlobalQemuVersion(long qemuVersion) {
        s_qemuVersion = qemuVersion;
    }

    public void setQemuVersion(long qemuVersion) {
        setGlobalQemuVersion(qemuVersion);
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

    public void setPlatformEmulator(String platformEmulator) {
        _platformEmulator = platformEmulator;
    }

    public String getPlatformEmulator() {
        return _platformEmulator;
    }

    public void addComp(Object comp) {
        components.put(comp.getClass().toString(), comp);
    }

    public DevicesDef getDevices() {
        Object o = components.get(DevicesDef.class.toString());
        if (o != null) {
            return (DevicesDef)o;
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
