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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class LibvirtVMDef {
    private static final Logger s_logger = Logger.getLogger(LibvirtVMDef.class);

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

        enum BootType {
            UEFI("UEFI"), BIOS("BIOS");

            String _type;

            BootType(String type) {
                _type = type;
            }

            @Override
            public String toString() {
                return _type;
            }
        }

        enum BootMode {
            LEGACY("LEGACY"), SECURE("SECURE");

            String _mode;

            BootMode(String mode) {
                _mode = mode;
            }

            @Override
            public String toString() {
                return _mode;
            }
        }

        private GuestType _type;
        private BootType _boottype;
        private BootMode _bootmode;
        private String _arch;
        private String _loader;
        private String _kernel;
        private String _initrd;
        private String _root;
        private String _cmdline;
        private String _uuid;
        private final List<BootOrder> _bootdevs = new ArrayList<BootOrder>();
        private String _machine;
        private String _nvram;
        private String _nvramTemplate;

        public static final String GUEST_LOADER_SECURE = "guest.loader.secure";
        public static final String GUEST_LOADER_LEGACY = "guest.loader.legacy";
        public static final String GUEST_NVRAM_PATH = "guest.nvram.path";
        public static final String GUEST_NVRAM_TEMPLATE_SECURE = "guest.nvram.template.secure";
        public static final String GUEST_NVRAM_TEMPLATE_LEGACY = "guest.nvram.template.legacy";

        public void setGuestType(GuestType type) {
            _type = type;
        }

        public GuestType getGuestType() {
            return _type;
        }

        public void setNvram(String nvram) { _nvram = nvram; }

        public void setNvramTemplate(String nvramTemplate) { _nvramTemplate = nvramTemplate; }

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

        public BootType getBootType() {
            return _boottype;
        }

        public void setBootType(BootType boottype) {
            this._boottype = boottype;
        }

        public BootMode getBootMode() {
            return _bootmode;
        }

        public void setBootMode(BootMode bootmode) {
            this._bootmode = bootmode;
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
                if (_arch != null && _arch.equals("aarch64")) {
                    guestDef.append("<loader readonly='yes' type='pflash'>/usr/share/AAVMF/AAVMF_CODE.fd</loader>\n");
                }
                if (_loader != null) {
                    if (_bootmode == BootMode.LEGACY) {
                        guestDef.append("<loader readonly='yes' secure='no' type='pflash'>" + _loader + "</loader>\n");
                    } else if (_bootmode == BootMode.SECURE) {
                        guestDef.append("<loader readonly='yes' secure='yes' type='pflash'>" + _loader + "</loader>\n");
                    }
                }
                if (_nvram != null) {
                    guestDef.append("<nvram ");
                    if (_nvramTemplate != null) {
                        guestDef.append("template='" + _nvramTemplate + "'>");
                    } else {
                        guestDef.append(">");
                    }

                    guestDef.append(_nvram);
                    guestDef.append(_uuid + ".fd</nvram>");
                }
                if (!_bootdevs.isEmpty()) {
                    for (BootOrder bo : _bootdevs) {
                        guestDef.append("<boot dev='" + bo + "'/>\n");
                    }
                }
                if (_arch == null || !_arch.equals("aarch64")) {
                    guestDef.append("<smbios mode='sysinfo'/>\n");
                }
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
        enum Enlight {
            RELAX("relaxed"),
            VAPIC("vapic"),
            SPIN("spinlocks");

            private final String featureName;
            Enlight(String featureName) { this.featureName = featureName; }
            String getFeatureName() { return featureName; }

            static boolean isValidFeature(String featureName) {
                Enlight[] enlights = Enlight.values();
                for(Enlight e : enlights) {
                    if(e.getFeatureName().equals(featureName))
                        return true;
                }
                return false;
            }
        }

        private final Map<String, String> features = new HashMap<String, String>();
        private int retries = 4096; // set to sane default

        public void setFeature(String feature, boolean on) {
            if(on && Enlight.isValidFeature(feature))
                setFeature(feature);
        }

        private void setFeature(String feature) {
            features.put(feature, "on");
        }

        public void setRetries(int retry) {
            if(retry>=retries)
                retries=retry;
        }

        public int getRetries() {
            return retries;
        }

        @Override
        public String toString() {
            StringBuilder feaBuilder = new StringBuilder();
            feaBuilder.append("<hyperv>\n");
            for (Map.Entry<String, String> e : features.entrySet()) {
                feaBuilder.append("<");
                feaBuilder.append(e.getKey());

                if(e.getKey().equals("spinlocks"))  feaBuilder.append(" state='" + e.getValue() + "' retries='" + getRetries() + "'");
                else                                feaBuilder.append(" state='" + e.getValue() + "'");

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
                if (feature.equalsIgnoreCase("smm")) {
                    feaBuilder.append("<" + feature + " state=\'on\' " + "/>\n");
                } else {
                    feaBuilder.append("<" + feature + "/>\n");
                }
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
                } else if (_timerName.equals("hypervclock")) {
                    clockBuilder.append("present='yes' />");
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
            FLOPPY("floppy"), DISK("disk"), CDROM("cdrom"), LUN("lun");
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
            IDE("ide"), SCSI("scsi"), VIRTIO("virtio"), XEN("xen"), USB("usb"), UML("uml"), FDC("fdc"), SATA("sata");
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

        public enum DiscardType {
            IGNORE("ignore"), UNMAP("unmap");
            String _discardType;
            DiscardType(String discardType) {
                _discardType = discardType;
            }

            @Override
            public String toString() {
                if (_discardType == null) {
                    return "ignore";
                }
                return _discardType;
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
        private Long _bytesReadRateMax;
        private Long _bytesReadRateMaxLength;
        private Long _bytesWriteRate;
        private Long _bytesWriteRateMax;
        private Long _bytesWriteRateMaxLength;
        private Long _iopsReadRate;
        private Long _iopsReadRateMax;
        private Long _iopsReadRateMaxLength;
        private Long _iopsWriteRate;
        private Long _iopsWriteRateMax;
        private Long _iopsWriteRateMaxLength;
        private DiskCacheMode _diskCacheMode;
        private String _serial;
        private boolean qemuDriver = true;
        private DiscardType _discard = DiscardType.IGNORE;

        public DiscardType getDiscard() {
            return _discard;
        }

        public void setDiscard(DiscardType discard) {
            this._discard = discard;
        }

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

        /* skip iso labels */
        private String getDevLabel(int devId, DiskBus bus, boolean forIso) {
            if (devId < 0) {
                return "";
            }

            if (bus == DiskBus.SCSI) {
                return "sd" + getDevLabelSuffix(devId);
            } else if (bus == DiskBus.VIRTIO) {
                return "vd" + getDevLabelSuffix(devId);
            } else if (bus == DiskBus.SATA){
                if (!forIso) {
                    return "sda";
                }
            }
            if (forIso) {
                devId --;
            } else if(devId >= 2) {
                devId += 2;
            }
            return (DiskBus.SATA == bus) ? "sdb" : "hd" + getDevLabelSuffix(devId);

        }

        private String getDevLabelSuffix(int deviceIndex) {
            if (deviceIndex < 0) {
                return "";
            }

            int base = 'z' - 'a' + 1;
            String labelSuffix = "";
            do {
                char suffix = (char)('a' + (deviceIndex % base));
                labelSuffix = suffix + labelSuffix;
                deviceIndex = (deviceIndex / base) - 1;
            } while (deviceIndex >= 0);

            return labelSuffix;
        }

        public void defFileBasedDisk(String filePath, int devId, DiskBus bus, DiskFmtType diskFmtType) {

            _diskType = DiskType.FILE;
            _deviceType = DeviceType.DISK;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = filePath;
            _diskLabel = getDevLabel(devId, bus, false);
            _diskFmtType = diskFmtType;
            _bus = bus;

        }

        public void defFileBasedDisk(String filePath, int devId, DiskFmtType diskFmtType,boolean isWindowsOS) {

            _diskType = DiskType.FILE;
            _deviceType = DeviceType.DISK;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = filePath;
            _diskFmtType = diskFmtType;

            if (isWindowsOS) {
                _diskLabel = getDevLabel(devId, DiskBus.SATA, false); // Windows Secure VM
                _bus = DiskBus.SATA;
            } else {
                _diskLabel = getDevLabel(devId, DiskBus.VIRTIO, false); // Linux Secure VM
                _bus = DiskBus.VIRTIO;
            }
        }

        public void defISODisk(String volPath) {
            _diskType = DiskType.FILE;
            _deviceType = DeviceType.CDROM;
            _sourcePath = volPath;
            _diskLabel = getDevLabel(3, DiskBus.IDE, true);
            _diskFmtType = DiskFmtType.RAW;
            _diskCacheMode = DiskCacheMode.NONE;
            _bus = DiskBus.IDE;
        }

        public void defISODisk(String volPath, Integer devId) {
            if (devId == null) {
                defISODisk(volPath);
            } else {
                _diskType = DiskType.FILE;
                _deviceType = DeviceType.CDROM;
                _sourcePath = volPath;
                _diskLabel = getDevLabel(devId, DiskBus.IDE, true);
                _diskFmtType = DiskFmtType.RAW;
                _diskCacheMode = DiskCacheMode.NONE;
                _bus = DiskBus.IDE;
            }
        }

        public void defISODisk(String volPath, Integer devId,boolean isSecure, boolean isWindowOs) {
            if (!isSecure) {
                defISODisk(volPath, devId);
            } else {
                _diskType = DiskType.FILE;
                _deviceType = DeviceType.CDROM;
                _sourcePath = volPath;
                if (isWindowOs) {
                    _diskLabel = getDevLabel(devId, DiskBus.SATA, true);
                    _bus = DiskBus.SATA;
                } else {
                    _diskLabel = getDevLabel(devId, DiskBus.SCSI, true);
                    _bus = DiskBus.SCSI;
                }
                _diskFmtType = DiskFmtType.RAW;
                _diskCacheMode = DiskCacheMode.NONE;

            }
        }

        public void defBlockBasedDisk(String diskName, int devId, DiskBus bus) {
            _diskType = DiskType.BLOCK;
            _deviceType = DeviceType.DISK;
            _diskFmtType = DiskFmtType.RAW;
            _diskCacheMode = DiskCacheMode.NONE;
            _sourcePath = diskName;
            _diskLabel = getDevLabel(devId, bus, false);
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
            _diskLabel = getDevLabel(devId, bus, false);
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

        public void setBusType(DiskBus busType) {
            _bus = busType;
        }

        public DiskFmtType getDiskFormatType() {
            return _diskFmtType;
        }

        public void setBytesReadRate(Long bytesReadRate) {
            _bytesReadRate = bytesReadRate;
        }

        public void setBytesReadRateMax(Long bytesReadRateMax) {
            _bytesReadRateMax = bytesReadRateMax;
        }

        public void  setBytesReadRateMaxLength(Long bytesReadRateLength) {
            _bytesReadRateMaxLength = bytesReadRateLength;
        }

        public void setBytesWriteRate(Long bytesWriteRate) {
            _bytesWriteRate = bytesWriteRate;
        }

        public void setBytesWriteRateMax(Long bytesWriteRateMax) {
            _bytesWriteRateMax = bytesWriteRateMax;
        }

        public void setBytesWriteRateMaxLength(Long bytesWriteRateMaxLength) {
            _bytesWriteRateMaxLength = bytesWriteRateMaxLength;
        }

        public void setIopsReadRate(Long iopsReadRate) {
            _iopsReadRate = iopsReadRate;
        }

        public void setIopsReadRateMax(Long iopsReadRateMax) {
            _iopsReadRateMax = iopsReadRateMax;
        }

        public void setIopsReadRateMaxLength(Long iopsReadRateMaxLength) {
            _iopsReadRateMaxLength = iopsReadRateMaxLength;
        }

        public void setIopsWriteRate(Long iopsWriteRate) {
            _iopsWriteRate = iopsWriteRate;
        }

        public void setIopsWriteRateMax(Long iopsWriteRateMax) {
            _iopsWriteRateMax = iopsWriteRateMax;
        }

        public void setIopsWriteRateMaxLength(Long iopsWriteRateMaxLength) {
            _iopsWriteRateMaxLength = iopsWriteRateMaxLength;
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

        public void setSerial(String serial) {
            this._serial = serial;
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
                diskBuilder.append("<driver name='qemu'" + " type='" + _diskFmtType + "' ");

                if (_deviceType != DeviceType.CDROM) {
                    diskBuilder.append("cache='" + _diskCacheMode + "' ");
                }

                if(_discard != null && _discard != DiscardType.IGNORE) {
                    diskBuilder.append("discard='" + _discard.toString() + "' ");
                }
                diskBuilder.append("/>\n");
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

            if (_serial != null && !_serial.isEmpty() && _deviceType != DeviceType.LUN) {
                diskBuilder.append("<serial>" + _serial + "</serial>");
            }

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
                if (s_qemuVersion >= 2004000) {
                    if (_bytesReadRateMax != null && _bytesReadRateMax > 0 ) {
                        diskBuilder.append("<read_bytes_sec_max>" + _bytesReadRateMax + "</read_bytes_sec_max>\n");
                    }
                    if (_bytesWriteRateMax != null && _bytesWriteRateMax > 0) {
                        diskBuilder.append("<write_bytes_sec_max>" + _bytesWriteRateMax + "</write_bytes_sec_max>\n");
                    }
                    if (_iopsReadRateMax != null && _iopsReadRateMax > 0)
                        diskBuilder.append("<read_iops_sec_max>" + _iopsReadRateMax + "</read_iops_sec_max>\n");
                    if (_iopsWriteRateMax != null && _iopsWriteRateMax > 0)
                        diskBuilder.append("<write_iops_sec_max>" + _iopsWriteRateMax + "</write_iops_sec_max>\n");
                }
                if (s_qemuVersion >= 2006000) {
                    if (_bytesReadRateMaxLength != null && _bytesReadRateMaxLength > 0) {
                        diskBuilder.append("<read_bytes_sec_max_length>" + _bytesReadRateMaxLength + "</read_bytes_sec_max_length>\n");
                    }
                    if (_bytesWriteRateMaxLength != null && _bytesWriteRateMaxLength > 0) {
                        diskBuilder.append("<write_bytes_sec_max_length>" + _bytesWriteRateMaxLength + "</write_bytes_sec_max_length>\n");
                    }
                    if (_iopsReadRateMaxLength != null && _iopsReadRateMaxLength > 0)
                        diskBuilder.append("<read_iops_sec_max_length>" + _iopsReadRateMaxLength + "</read_iops_sec_max_length>\n");
                    if (_iopsWriteRateMaxLength != null && _iopsWriteRateMaxLength > 0)
                        diskBuilder.append("<write_iops_sec_max_length>" + _iopsWriteRateMaxLength + "</write_iops_sec_max_length>\n");
                }

                diskBuilder.append("</iotune>\n");
            }

            diskBuilder.append("</disk>\n");
            return diskBuilder.toString();
        }
    }

    public static class InterfaceDef {
        public enum GuestNetType {
            BRIDGE("bridge"), DIRECT("direct"), NETWORK("network"), USER("user"), ETHERNET("ethernet"), INTERNAL("internal"), VHOSTUSER("vhostuser");
            String _type;

            GuestNetType(String type) {
                _type = type;
            }

            @Override
            public String toString() {
                return _type;
            }
        }

        public enum NicModel {
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
         * internal, vhostuser
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
        private boolean _linkStateUp = true;
        private Integer _slot;
        private String _dpdkSourcePath;
        private String _dpdkSourcePort;
        private String _dpdkExtraLines;
        private String _interfaceMode;

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

        public void defDpdkNet(String dpdkSourcePath, String dpdkPort, String macAddress, NicModel model,
                               Integer networkRateKBps, String extra, String interfaceMode) {
            _netType = GuestNetType.VHOSTUSER;
            _dpdkSourcePath = dpdkSourcePath;
            _dpdkSourcePort = dpdkPort;
            _macAddr = macAddress;
            _model = model;
            _networkRateKBps = networkRateKBps;
            _dpdkExtraLines = extra;
            _interfaceMode = interfaceMode;
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

        public void setDevName(String networkName) {
            _networkName = networkName;
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

        public void setSlot(Integer slot) {
            _slot = slot;
        }

        public Integer getSlot() {
            return _slot;
        }

        public void setLinkStateUp(boolean linkStateUp) {
            _linkStateUp = linkStateUp;
        }

        public boolean isLinkStateUp() {
            return _linkStateUp;
        }

        public String getDpdkSourcePort() {
            return _dpdkSourcePort;
        }
        public void setDpdkSourcePort(String port) {
            _dpdkSourcePort = port;
        }

        public String getDpdkOvsPath() {
            return _dpdkSourcePath;
        }

        public void setDpdkOvsPath(String path) {
            _dpdkSourcePath = path;
        }

        public String getInterfaceMode() {
            return _interfaceMode;
        }

        public void setInterfaceMode(String mode) {
            _interfaceMode = mode;
        }

        public String getContent() {
            StringBuilder netBuilder = new StringBuilder();
            if (_netType == GuestNetType.BRIDGE) {
                netBuilder.append("<source bridge='" + _sourceName + "'/>\n");
            } else if (_netType == GuestNetType.NETWORK) {
                netBuilder.append("<source network='" + _sourceName + "'/>\n");
            } else if (_netType == GuestNetType.DIRECT) {
                netBuilder.append("<source dev='" + _sourceName + "' mode='" + _netSourceMode + "'/>\n");
            } else if (_netType == GuestNetType.VHOSTUSER) {
                netBuilder.append("<source type='unix' path='"+ _dpdkSourcePath + _dpdkSourcePort +
                        "' mode='" + _interfaceMode + "'/>\n");
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

            if (StringUtils.isNotBlank(_dpdkExtraLines)) {
                netBuilder.append(_dpdkExtraLines);
            }

            if (_netType != GuestNetType.VHOSTUSER) {
                netBuilder.append("<link state='" + (_linkStateUp ? "up" : "down") +"'/>\n");
            }

            if (_slot  != null) {
                netBuilder.append(String.format("<address type='pci' domain='0x0000' bus='0x00' slot='0x%02x' function='0x0'/>\n", _slot));
            }
            return netBuilder.toString();
        }

        @Override
        public String toString() {
            StringBuilder netBuilder = new StringBuilder();
            netBuilder.append("<interface type='" + _netType + "'>\n");
            netBuilder.append(getContent());
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
        private int quota = 0;
        private int period = 0;
        static final int DEFAULT_PERIOD = 10000;
        static final int MIN_QUOTA = 1000;
        static final int MAX_PERIOD = 1000000;

        public void setShares(int shares) {
            _shares = shares;
        }

        public int getShares() {
            return _shares;
        }

        public int getQuota() {
            return quota;
        }

        public void setQuota(int quota) {
            this.quota = quota;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) {
            this.period = period;
        }

        @Override
        public String toString() {
            StringBuilder cpuTuneBuilder = new StringBuilder();
            cpuTuneBuilder.append("<cputune>\n");
            if (_shares > 0) {
                cpuTuneBuilder.append("<shares>" + _shares + "</shares>\n");
            }
            if (quota > 0) {
                cpuTuneBuilder.append("<quota>" + quota + "</quota>\n");
            }
            if (period > 0) {
                cpuTuneBuilder.append("<period>" + period + "</period>\n");
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
                    if (feature.startsWith("-")) {
                        modeBuilder.append("<feature policy='disable' name='" + feature.substring(1) + "'/>");
                    } else {
                        modeBuilder.append("<feature policy='require' name='" + feature + "'/>");
                    }
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

    public final static class ChannelDef {
        enum ChannelType {
            UNIX("unix"), SERIAL("serial");
            String type;

            ChannelType(String type) {
                this.type = type;
            }

            @Override
            public String toString() {
                return this.type;
            }
        }

        enum ChannelState {
            DISCONNECTED("disconnected"), CONNECTED("connected");
            String type;

            ChannelState(String type) {
                this.type = type;
            }

            @Override
            public String toString() {
                return type;
            }
        }

        private final String name;
        private File path = new File("");
        private final ChannelType type;
        private ChannelState state;

        public ChannelDef(String name, ChannelType type) {
            this.name = name;
            this.type = type;
        }

        public ChannelDef(String name, ChannelType type, File path) {
            this.name = name;
            this.path = path;
            this.type = type;
        }

        public ChannelDef(String name, ChannelType type, ChannelState state) {
            this.name = name;
            this.state = state;
            this.type = type;
        }

        public ChannelDef(String name, ChannelType type, ChannelState state, File path) {
            this.name = name;
            this.path = path;
            this.state = state;
            this.type = type;
        }

        public ChannelType getChannelType() {
            return type;
        }

        public ChannelState getChannelState() {
            return state;
        }

        public String getName() {
            return name;
        }

        public File getPath() {
            return path;
        }

        @Override
        public String toString() {
            StringBuilder virtioSerialBuilder = new StringBuilder();
            virtioSerialBuilder.append("<channel type='" + type.toString() + "'>\n");
            if (path == null) {
                virtioSerialBuilder.append("<source mode='bind'/>\n");
            } else {
                virtioSerialBuilder.append("<source mode='bind' path='" + path.toString() + "'/>\n");
            }
            virtioSerialBuilder.append("<address type='virtio-serial'/>\n");
            if (state == null) {
                virtioSerialBuilder.append("<target type='virtio' name='" + name + "'/>\n");
            } else {
                virtioSerialBuilder.append("<target type='virtio' name='" + name + "' state='" + state.toString() + "'/>\n");
            }
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

    public static class SCSIDef {
        private short index = 0;
        private int domain = 0;
        private int bus = 0;
        private int slot = 9;
        private int function = 0;
        private int queues = 0;

        public SCSIDef(short index, int domain, int bus, int slot, int function, int queues) {
            this.index = index;
            this.domain = domain;
            this.bus = bus;
            this.slot = slot;
            this.function = function;
            this.queues = queues;
        }

        public SCSIDef() {

        }

        @Override
        public String toString() {
            StringBuilder scsiBuilder = new StringBuilder();

            scsiBuilder.append(String.format("<controller type='scsi' index='%d' model='virtio-scsi'>\n", this.index));
            scsiBuilder.append(String.format("<address type='pci' domain='0x%04X' bus='0x%02X' slot='0x%02X' function='0x%01X'/>\n",
                    this.domain, this.bus, this.slot, this.function ) );
            if (this.queues > 0) {
                scsiBuilder.append(String.format("<driver queues='%d'/>\n", this.queues));
            }
            scsiBuilder.append("</controller>\n");
            return scsiBuilder.toString();
        }
    }

    public static class USBDef {
        private short index = 0;
        private int domain = 0;
        private int bus = 0;
        private int slot = 9;
        private int function = 0;

        public USBDef(short index, int domain, int bus, int slot, int function) {
            this.index = index;
            this.domain = domain;
            this.bus = bus;
            this.slot = slot;
            this.function = function;
        }

        public USBDef() {
        }

        @Override
        public String toString() {
            StringBuilder scsiBuilder = new StringBuilder();

            scsiBuilder.append(String.format("<controller type='usb' index='%d' model='qemu-xhci'>\n", this.index));
            scsiBuilder.append("<alias name='usb'/>");
            scsiBuilder.append(String.format("<address type='pci' domain='0x%04X' bus='0x%02X' slot='0x%02X' function='0x%01X'/>\n",
                    this.domain, this.bus, this.slot, this.function ) );
            scsiBuilder.append("</controller>\n");
            return scsiBuilder.toString();
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

    public static class MetadataDef {
        Map<String, Object> customNodes = new HashMap<>();

        public <T> T getMetadataNode(Class<T> fieldClass) {
            T field = (T) customNodes.get(fieldClass.getName());
            if (field == null) {
                try {
                    field = fieldClass.newInstance();
                    customNodes.put(field.getClass().getName(), field);
                } catch (InstantiationException | IllegalAccessException e) {
                    s_logger.debug("No default constructor available in class " + fieldClass.getName() + ", ignoring exception", e);
                }
            }
            return field;
        }

        @Override
        public String toString() {
            StringBuilder fsBuilder = new StringBuilder();
            fsBuilder.append("<metadata>\n");
            for (Object field : customNodes.values()) {
                fsBuilder.append(field.toString());
            }
            fsBuilder.append("</metadata>\n");
            return fsBuilder.toString();
        }
    }

    public static class RngDef {
        enum RngModel {
            VIRTIO("virtio");
            String model;

            RngModel(String model) {
                this.model = model;
            }

            @Override
            public String toString() {
                return model;
            }
        }

        enum RngBackendModel {
            RANDOM("random"), EGD("egd");
            String model;

            RngBackendModel(String model) {
                this.model = model;
            }

            @Override
            public String toString() {
                return model;
            }
        }

        private String path = "/dev/random";
        private RngModel rngModel = RngModel.VIRTIO;
        private RngBackendModel rngBackendModel = RngBackendModel.RANDOM;
        private int rngRateBytes = 2048;
        private int rngRatePeriod = 1000;

        public RngDef(String path) {
            this.path = path;
        }

        public RngDef(String path, int rngRateBytes, int rngRatePeriod) {
            this.path = path;
            this.rngRateBytes = rngRateBytes;
            this.rngRatePeriod = rngRatePeriod;
        }

        public RngDef(RngModel rngModel) {
            this.rngModel = rngModel;
        }

        public RngDef(RngBackendModel rngBackendModel) {
            this.rngBackendModel = rngBackendModel;
        }

        public RngDef(String path, RngBackendModel rngBackendModel) {
            this.path = path;
            this.rngBackendModel = rngBackendModel;
        }

        public RngDef(String path, RngBackendModel rngBackendModel, int rngRateBytes, int rngRatePeriod) {
            this.path = path;
            this.rngBackendModel = rngBackendModel;
            this.rngRateBytes = rngRateBytes;
            this.rngRatePeriod = rngRatePeriod;
        }

        public RngDef(String path, RngModel rngModel) {
            this.path = path;
            this.rngModel = rngModel;
        }

        public String getPath() {
           return path;
        }

        public RngBackendModel getRngBackendModel() {
            return rngBackendModel;
        }

        public RngModel getRngModel() {
            return rngModel;
        }

        public int getRngRateBytes() {
            return rngRateBytes;
        }

        public int getRngRatePeriod() {
            return rngRatePeriod;
        }

        @Override
        public String toString() {
            StringBuilder rngBuilder = new StringBuilder();
            rngBuilder.append("<rng model='" + rngModel + "'>\n");
            rngBuilder.append("<rate period='" + rngRatePeriod + "' bytes='" + rngRateBytes + "' />\n");
            rngBuilder.append("<backend model='" + rngBackendModel + "'>" + path + "</backend>");
            rngBuilder.append("</rng>\n");
            return rngBuilder.toString();
        }
    }

    public static class WatchDogDef {
        enum WatchDogModel {
            I6300ESB("i6300esb"), IB700("ib700"), DIAG288("diag288");
            String model;

            WatchDogModel(String model) {
                this.model = model;
            }

            @Override
            public String toString() {
                return model;
            }
        }

        enum WatchDogAction {
            RESET("reset"), SHUTDOWN("shutdown"), POWEROFF("poweroff"), PAUSE("pause"), NONE("none"), DUMP("dump");
            String action;

            WatchDogAction(String action) {
                this.action = action;
            }

            @Override
            public String toString() {
                return action;
            }
        }

        WatchDogModel model = WatchDogModel.I6300ESB;
        WatchDogAction action = WatchDogAction.NONE;

        public WatchDogDef(WatchDogAction action) {
            this.action = action;
        }

        public WatchDogDef(WatchDogModel model) {
            this.model = model;
        }

        public WatchDogDef(WatchDogAction action, WatchDogModel model) {
            this.action = action;
            this.model = model;
        }

        public WatchDogAction getAction() {
            return action;
        }

        public WatchDogModel getModel() {
            return model;
        }

        @Override
        public String toString() {
            StringBuilder wacthDogBuilder = new StringBuilder();
            wacthDogBuilder.append("<watchdog model='" + model + "' action='" + action + "'/>\n");
            return wacthDogBuilder.toString();
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

    public MetadataDef getMetaData() {
        MetadataDef o = (MetadataDef) components.get(MetadataDef.class.toString());
        if (o == null) {
            o = new MetadataDef();
            addComp(o);
        }
        return o;
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
