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

package com.cloud.hypervisor.ovm3.objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class Linux extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(Linux.class);
    private static final String DEVICE = "Device";
    private static final String REMOTEDIR = "Remote_Dir";
    private static final String MOUNTPOINT = "Mount_Point";
    private Integer initMaps = 1;

    /**
     * use capabilities to match things later, perhaps also hardware discovery ?
     * wrap getters and setters.... for Mapps...
     */
    private Map<String, String> ovmCapabilities = new HashMap<String, String>();
    /**
     * MAX_CONCURRENT_MIGRATION_IN=1, ALL_VM_CPU_OVERSUBSCRIBE=True,
     * HIGH_AVAILABILITY=True, LOCAL_STORAGE_ELEMENT=True, NFS=True,
     * MTU_CONFIGURATION=True, CONCURRENT_MIGRATION=False,
     * VM_MEMORY_ALIGNMENT=1048576, CLUSTERS=True, VM_SUSPEND=True,
     * BOND_MODE_LINK_AGGREGATION=True, YUM_PACKAGE_MANAGEMENT=True,
     * VM_VNC_CONSOLE=True, BOND_MODE_ACTIVE_BACKUP=True,
     * MAX_CONCURRENT_MIGRATION_OUT=1, MIGRATION_SETUP=False,
     * PER_VM_CPU_OVERSUBSCRIBE=True, POWER_ON_WOL=True, FIBRE_CHANNEL=True,
     * ISCSI=True, HVM_MAX_VNICS=8}
     */
    private Map<String, String> ovmHypervisorDetails = new HashMap<String, String>();
    private Map<String, String> ovmHypervisor = new HashMap<String, String>();
    private Map<String, String> ovmNTP = new HashMap<String, String>();
    private Map<String, String> ovmDateTime = new HashMap<String, String>();
    private Map<String, String> ovmGeneric = new HashMap<String, String>();
    /**
     * {OS_Major_Version=5, Statistic=20, Membership_State=Unowned,
     * OVM_Version=3.2.1-517, OS_Type=Linux, Hypervisor_Name=Xen,
     * CPU_Type=x86_64, Manager_Core_API_Version=3.2.1.516,
     * Is_Primary=false, OS_Name=Oracle VM Server,
     * Server_Roles=xen,utility, Pool_Unique_Id=none,
     * Host_Kernel_Release=2.6.39-300.22.2.el5uek, OS_Minor_Version=7,
     * Agent_Version=3.2.1-183, Boot_Time=1392366638, RPM_Version=3.2.1-183,
     * Exports=, Hypervisor_Type=xen, Host_Kernel_Version=#1 SMP Fri Jan 4
     * 12:40:29 PST 2013,
     * Unique_Id=1d:d5:e8:91:d9:d0:ed:bd:81:c2:a6:9a:b3:d1:b7:ea,
     * Manager_Unique_Id=none, Cluster_State=Offline, Hostname=ovm-1}
     */
    private Map<String, String> hwPhysicalInfo = new HashMap<String, String>();
    private Map<String, String> hwSystemInfo = new HashMap<String, String>();
    private int localTime;
    private int lastBootTime;
    private String timeZ;
    private String timeUTC;
    private List<String> mounts = null;
    private Map<String, FileSystem> fsMap = null;

    public Linux(Connection c) {
        setClient(c);
    }

    /*
     * discover_server, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Boolean discoverServer() throws Ovm3ResourceException {
        Object result = callWrapper("discover_server");
        if (result == null) {
            return false;
        }
        Document xmlDocument = prepParse((String) result);
        /* could be more subtle */
        String path = "//Discover_Server_Result/Server";
        ovmCapabilities = xmlToMap(path + "/Capabilities", xmlDocument);
        ovmHypervisorDetails = xmlToMap(path + "/VMM/Version", xmlDocument);
        ovmHypervisor = xmlToMap(path + "/VMM", xmlDocument);
        ovmNTP = xmlToMap(path + "/NTP", xmlDocument);
        ovmDateTime = xmlToMap(path + "/Date_Time", xmlDocument);
        ovmGeneric = xmlToMap(path, xmlDocument);
        return true;
    }

    public String getAgentVersion() throws Ovm3ResourceException {
        return get("Agent_Version");
    }

    public String getHostKernelRelease() throws Ovm3ResourceException {
        return get("Host_Kernel_Release");
    }

    public String getHostOs() throws Ovm3ResourceException {
        return get("OS_Name");
    }

    public String getHostOsVersion() throws Ovm3ResourceException {
        return get("OS_Major_Version") + "."
                + get("OS_Minor_Version");
    }

    public String getHypervisorName() throws Ovm3ResourceException {
        return get("Hypervisor_Name");
    }

    public String getHypervisorVersion() throws Ovm3ResourceException {
        return getHypervisorMajor() + "."
                + getHypervisorMinor() + "." + getHypervisorExtra();
    }

    public String getCapabilities() throws Ovm3ResourceException {
        return get("Capabilities");
    }

    public String getHypervisorMajor() throws Ovm3ResourceException {
        return get("Major");
    }

    public String getHypervisorMinor() throws Ovm3ResourceException{
        return get("Minor");
    }

    public String getHypervisorExtra() throws Ovm3ResourceException {
        return get("Extra").replace(".", "");
    }

    public String getManagerUuid() throws Ovm3ResourceException {
        return get("Manager_Unique_Id");
    }

    public String getMembershipState() throws Ovm3ResourceException {
        return get("Membership_State");
    }

    public String getServerRoles() throws Ovm3ResourceException{
        return get("Server_Roles");
    }

    public boolean getIsPrimary() throws Ovm3ResourceException {
        return Boolean.parseBoolean(get("Is_Primary"));
    }

    public String getOvmVersion() throws Ovm3ResourceException {
        return get("OVM_Version");
    }

    public String getHostName() throws Ovm3ResourceException {
        return get("Hostname");
    }

    public Integer getCpuKhz() throws Ovm3ResourceException {
        return Integer.valueOf(get("CPUKHz"));
    }

    public Integer getCpuSockets() throws Ovm3ResourceException {
        return Integer.valueOf(get("SocketsPerNode"));
    }

    public Integer getCpuThreads() throws Ovm3ResourceException {
        return Integer.valueOf(get("ThreadsPerCore"));
    }

    public Integer getCpuCores() throws Ovm3ResourceException {
        return Integer.valueOf(get("CoresPerSocket"));
    }

    public Integer getTotalThreads() throws Ovm3ResourceException {
        return getCpuSockets() * getCpuCores() * getCpuThreads();
    }

    public Double getMemory() throws Ovm3ResourceException {
        return Double.valueOf(get("TotalPages")) * 4096;
    }

    public Double getFreeMemory() throws Ovm3ResourceException {
        return Double.valueOf(get("FreePages")) * 4096;
    }

    public String getUuid() throws Ovm3ResourceException {
        return get("Unique_Id");
    }

    private void initMaps() throws Ovm3ResourceException {
        if (initMaps == 1) {
            discoverHardware();
            discoverServer();
            initMaps = 0;
        }
    }

    public String get(String element) throws Ovm3ResourceException {
        try {
            initMaps();
        } catch (Ovm3ResourceException e) {
            LOGGER.info("Unable to discover host: " + e.getMessage(), e);
            throw e;
        }
        if (ovmGeneric.containsKey(element)) {
            return ovmGeneric.get(element);
        } else if (ovmHypervisor.containsKey(element)) {
            return ovmHypervisor.get(element);
        } else if (ovmHypervisorDetails.containsKey(element)) {
            return ovmHypervisorDetails.get(element);
        } else if (hwPhysicalInfo.containsKey(element)) {
            return hwPhysicalInfo.get(element);
        } else if (hwSystemInfo.containsKey(element)) {
            return hwSystemInfo.get(element);
        } else if (ovmCapabilities.containsKey(element)) {
            return ovmCapabilities.get(element);
        }
        return "";
    }

    /*
     * get_last_boot_time, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Integer getLastBootTime() throws Ovm3ResourceException {
        Map<String, Long> result = callMap("get_last_boot_time");
        if (result == null) {
            return null;
        }
        lastBootTime = result.get("last_boot_time").intValue();
        localTime = result.get("local_time").intValue();
        return lastBootTime;
    }

    /*
     * get_support_files, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */

    public Boolean copyFile(String src, String dst) throws Ovm3ResourceException {
        /* sparse is set to true by default ? */
        Object x = callWrapper("copy_file", src, dst, true);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean copyFile(String src, String dst, Boolean sparse) throws Ovm3ResourceException {
        Object x = callWrapper("copy_file", src, dst, sparse);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Map<String, FileSystem> getFileSystemMap(String type) throws Ovm3ResourceException {
        if (fsMap == null) {
            discoverMountedFs(type);
        }
        return fsMap;
    }
    public FileSystem getFileSystem(String mountpoint, String type) throws Ovm3ResourceException {
        getFileSystemMap(type);
        if (getFileSystemMap(type).containsKey(mountpoint)) {
            return getFileSystemMap(type).get(mountpoint);
        }
        return null;
    }
    public FileSystem getFileSystemByUuid(String uuid, String type) throws Ovm3ResourceException {
        getFileSystemMap(type);
        for (final Map.Entry<String, FileSystem> fs : fsMap.entrySet()) {
            if (fs.getValue().getUuid().matches(uuid)) {
                return fs.getValue();
            }
        }
        return null;
    }
    public void setFileSystemMap(Map<String, FileSystem> map) {
        fsMap = map;
    }
    public List<String> getFileSystemList() {
        return mounts;
    }

    public static class FileSystem {
        private Map<String, Object> fileSys = new HashMap<String, Object>() {
            {
                put("Mount_Options", null);
                put("Name", null);
                put(DEVICE, null);
                put("Host", null);
                put(REMOTEDIR, null);
                put(MOUNTPOINT, null);
                put("Uuid", null);
            }
            private static final long serialVersionUID = 123L;
        };

        public Boolean setDetails(Map<String, Object> fs) {
            fileSys = fs;
            return true;
        }
        public Map<String, Object> getDetails() {
            return fileSys;
        }
        public String getUuid() {
            return (String) fileSys.get("Uuid");
        }

        public String setUuid(String uuid) {
            return (String) fileSys.put("Uuid", uuid);
        }

        public String getDevice() {
            return (String) fileSys.get(DEVICE);
        }

        public String setDevice(String dev) {
            return (String) fileSys.put(DEVICE, dev);
        }

        public String getHost() {
            if (getDevice() != null && getDevice().contains(":")) {
                String[] spl = getDevice().split(":");
                setHost(spl[0]);
                setRemoteDir(spl[1]);
            } else {
                return null;
            }
            return (String) fileSys.get("Host");
        }

        public String setHost(String host) {
            return (String) fileSys.put("Host", host);
        }

        public String setRemoteDir(String dir) {
            return (String) fileSys.put(REMOTEDIR, dir);
        }

        public String getRemoteDir() {
            if (getHost() != null) {
                return (String) fileSys.get(REMOTEDIR);
            }
            return null;
        }

        public String setMountPoint(String pnt) {
            return (String) fileSys.put(MOUNTPOINT, pnt);
        }
        public String getMountPoint() {
            return (String) fileSys.get(MOUNTPOINT);
        }
    }

    /* should actually be called "getMountedsFsDevice" or something */
    /* takes nfs,ext3 etc as parameter it reads from /proc/mounts */
    public Map<String, FileSystem> discoverMountedFs(String type) throws Ovm3ResourceException {
        fsMap = new HashMap<String, FileSystem>();
        Object x = callWrapper("discover_mounted_file_systems", type);
        if (x == null) {
            return fsMap;
        }
        Document xmlDocument = prepParse((String) x);
        String bpath = "//Discover_Mounted_File_Systems_Result/Filesystem";
        String mpath = bpath + "/Mount/@Dir";
        mounts = xmlToList(mpath, xmlDocument);
        for (String mnt : mounts) {
            String dpath = bpath + "/Mount[@Dir='" + mnt + "']";
            Map<String, Object> fs = xmlToMap(dpath, xmlDocument);
            FileSystem f = new FileSystem();
            f.setDetails(fs);
            String[] spl = mnt.split("/");
            String uuid = spl[spl.length - 1];
            f.setUuid(uuid);
            f.setMountPoint(mnt);
            /* sets it up per mountpoint, not the ID!!! */
            fsMap.put(mnt, f);
        }
        setFileSystemMap(fsMap);
        return fsMap;
    }

    /* TODO: in 3.3.x this changed to user, pass, oldpass */
    public Boolean updateAgentPassword(String user, String pass) throws Ovm3ResourceException {
        Object x = callWrapper("update_agent_password", user, pass);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean discoverHardware() throws Ovm3ResourceException {
        Object result = callWrapper("discover_hardware");
        if (result == null) {
            return false;
        }
        Document xmlDocument;
        xmlDocument = prepParse((String) result);
        /* could be more subtle */
        String path = "//Discover_Hardware_Result/NodeInformation";
        /* we don't care a bout IO/SCSI for now..., we might care about
         * CPUs later: NodeInformation/CPUInfo/Proc_Info/CPU[@ID=0]
         */
        hwPhysicalInfo = xmlToMap(path + "/VMM/PhysicalInfo", xmlDocument);
        hwSystemInfo = xmlToMap(path + "/DMTF/System", xmlDocument);
        return true;
    }

    public Integer getDateTime() throws Ovm3ResourceException {
        getLastBootTime();
        return localTime;
    }

    /* Pushes the statistics out to a url, the statistics are in the form of a dict */
    public Boolean setStatisticsInterval(int val) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("set_statistics_interval", val);
    }

    public Boolean getTimeZone() throws Ovm3ResourceException  {
        Object[] result = (Object[]) callWrapper("get_timezone");
        if (result != null) {
            setTimeZ(result[0].toString());
            setTimeUTC(result[1].toString());
            return true;
        }
        return false;
    }

    public String getTimeUTC() {
        return timeUTC;
    }

    private void setTimeUTC(String timeUTC) {
        this.timeUTC = timeUTC;
    }

    public String getTimeZ() {
        return timeZ;
    }

    private void setTimeZ(String timeZ) {
        this.timeZ = timeZ;
    }
}
