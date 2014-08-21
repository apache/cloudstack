/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class Linux extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(Linux.class);
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
     * Is_Current_Master=false, OS_Name=Oracle VM Server,
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
    private Map<String, FileSystem> fsList = null;

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
        return this.get("Agent_Version");
    }

    public String getHostKernelRelease() throws Ovm3ResourceException {
        return this.get("Host_Kernel_Release");
    }

    public String getHostOs() throws Ovm3ResourceException {
        return this.get("OS_Name");
    }

    public String getHostOsVersion() throws Ovm3ResourceException {
        return this.get("OS_Major_Version") + "."
                + this.get("OS_Minor_Version");
    }

    public String getHypervisorName() throws Ovm3ResourceException {
        return this.get("Hypervisor_Name");
    }

    public String getHypervisorVersion() throws Ovm3ResourceException {
        return this.getHypervisorMajor() + "."
                + this.getHypervisorMinor() + "." + this.getHypervisorExtra();
    }

    public String getCapabilities() throws Ovm3ResourceException {
        return this.get("Capabilities");
    }

    public String getHypervisorMajor() throws Ovm3ResourceException {
        return this.get("Major");
    }

    public String getHypervisorMinor() throws Ovm3ResourceException{
        return this.get("Minor");
    }

    public String getHypervisorExtra() throws Ovm3ResourceException {
        return this.get("Extra").replace(".", "");
    }

    public String getManagerUuid() throws Ovm3ResourceException {
        return this.get("Manager_Unique_Id");
    }

    public String getMembershipState() throws Ovm3ResourceException {
        return this.get("Membership_State");
    }

    public String getServerRoles() throws Ovm3ResourceException{
        return this.get("Server_Roles");
    }

    public boolean getIsMaster() throws Ovm3ResourceException {
        return Boolean.parseBoolean(this.get("Is_Current_Master"));
    }

    public String getOvmVersion() throws Ovm3ResourceException {
        return this.get("OVM_Version");
    }

    public String getHostName() throws Ovm3ResourceException {
        return this.get("Hostname");
    }

    public Integer getCpuKhz() throws Ovm3ResourceException {
        return Integer.valueOf(this.get("CPUKHz"));
    }

    public Integer getCpuSockets() throws Ovm3ResourceException {
        return Integer.valueOf(this.get("SocketsPerNode"));
    }

    public Integer getCpuThreads() throws Ovm3ResourceException {
        return Integer.valueOf(this.get("ThreadsPerCore"));
    }

    public Integer getCpuCores() throws Ovm3ResourceException {
        return Integer.valueOf(this.get("CoresPerSocket"));
    }

    public Integer getTotalThreads() throws Ovm3ResourceException {
        return this.getCpuSockets() * this.getCpuCores() * this.getCpuThreads();
    }

    public Double getMemory() throws Ovm3ResourceException {
        return Double.valueOf(this.get("TotalPages")) * 4096;
    }

    public Double getFreeMemory() throws Ovm3ResourceException {
        return Double.valueOf(this.get("FreePages")) * 4096;
    }

    public String getUuid() throws Ovm3ResourceException {
        return this.get("Unique_Id");
    }

    private void initMaps() throws Ovm3ResourceException {
        if (this.initMaps == 1) {
            this.discoverHardware();
            this.discoverServer();
            this.initMaps = 0;
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
     * unexport_fs, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: export_uuid - default: None
     */

    /*
     * get_last_boot_time, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Integer getLastBootTime() throws Ovm3ResourceException {
        Map<String, Long> result = callMap("get_last_boot_time");
        if (result == null) {
            return null;
        }
        this.lastBootTime = result.get("last_boot_time").intValue();
        this.localTime = result.get("local_time").intValue();
        return lastBootTime;
    }

    /*
     * delete_yum_repo, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: repo_id - default: None
     */

    /*
     * notify_manager, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: notification - default: None argument: data -
     * default: None
     */

    /*
     * update_core_api_bindings, <class 'agent.api.host.linux.Linux'> argument:
     * self - default: None argument: url - default: None argument: option -
     * default: None
     */

    /*
     * set_datetime, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: year - default: None argument: month - default:
     * None argument: day - default: None argument: hour - default: None
     * argument: min - default: None argument: sec - default: None
     */
    public Boolean setDateTime(int year, int month, int day, int hour, int min,
            int sec) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("set_datetime", year, month, day, hour, min, sec);
    }

    /*
     * list_package, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: name - default: None
     */

    /*
     * discover_physical_luns, <class 'agent.api.host.linux.Linux'> argument:
     * self - default: None argument: args - default: None
     */
    public String discoverPhysicalLuns() throws Ovm3ResourceException {
        return (String) callWrapper("discover_physical_luns", "");
    }

    /*
     * ovs_download_file, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: url - default: None argument: filename - default:
     * None argument: option - default: None argument: obj - default: None
     * argument: obj_current - default: None argument: obj_total - default: None
     * argument: update_period - default: None
     */

    /*
     * install_package, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: pkg_data - default: None argument: option -
     * default: None
     */

    /*
     * get_support_files, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */

    /*
     * export_fs, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None argument: export_uuid - default: None argument: export_type -
     * default: None argument: client - default: None argument: path - default:
     * None argument: options - default: None
     */

    /*
     * ovs_async_proc_status, <class 'agent.api.host.linux.Linux'> argument:
     * self - default: None argument: pid - default: None
     */

    /*
     * set_timezone, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: timezone - default: None argument: utc - default:
     * None
     */
    public Boolean setTimeZone(String tz, Boolean utc) throws Ovm3ResourceException {
        Object x = callWrapper("set_timezone", tz, utc);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * copy_file, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None argument: src - default: None argument: dst - default: None
     * argument: sparse - default: None argument: update_period - default: None
     */
    public Boolean copyFile(String src, String dst) throws Ovm3ResourceException {
        Object x = callWrapper("copy_file", src, dst, false);
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

    /*
     * discover_mounted_file_systems, <class 'agent.api.host.linux.Linux'>
     * argument: self - default: None argument: args - default: None
     */
    public Boolean discoverMountedFs() throws Ovm3ResourceException {
        Object x = callWrapper("discover_mounted_file_systems");
        if (x == null) {
            return true;
        }
        return false;
    }

    public Map<String, FileSystem> getFileSystemList(String type) throws Ovm3ResourceException {
        if (fsList == null) {
            this.discoverMountedFs(type);
        }
        return fsList;
    }

    public void setFileSystemList(Map<String, FileSystem> list) {
        fsList = list;
    }

    public static class FileSystem {
        private Map<String, Object> fileSys = new HashMap<String, Object>() {
            {
                put("Mount_Options", null);
                put("Name", null);
                put("Device", null);
                put("Host", null);
                put("Dir", null);
                put("Mount_Point", null);
                put("Uuid", null);
            }
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

        public String getName() {
            return (String) fileSys.get("Name");
        }

        public String setName(String name) {
            return (String) fileSys.put("Name", name);
        }

        public String getDevice() {
            return (String) fileSys.get("Device");
        }

        public String setDevice(String dev) {
            return (String) fileSys.put("Device", dev);
        }

        public String getHost() {
            if (getDevice() != null && getDevice().contains(":")) {
                String[] spl = getDevice().split(":");
                setHost(spl[0]);
                setMountPoint(spl[1]);
            } else {
                return null;
            }
            return (String) fileSys.get("Host");
        }

        public String setHost(String host) {
            return (String) fileSys.put("Host", host);
        }

        public String getDir() {
            return (String) fileSys.get("Dir");
        }

        public String setDir(String dir) {
            return (String) fileSys.put("Dir", dir);
        }

        public String getMountPoint() {
            if (getHost() != null) {
                return (String) fileSys.get("Mount_Point");
            }
            return null;
        }

        public String setMountPoint(String pnt) {
            return (String) fileSys.put("Mount_Point", pnt);
        }
    }

    /* should actually be called "getMountedsFsDevice" or something */
    public Map<String, FileSystem> discoverMountedFs(String type) throws Ovm3ResourceException {
        this.fsList = new HashMap<String, FileSystem>();
        Object x = callWrapper("discover_mounted_file_systems", type);
        if (x == null) {
            return this.fsList;
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
            f.setDir(mnt);
            fsList.put(uuid, f);
        }
        setFileSystemList(fsList);
        return this.fsList;
    }

    /*
     * ovs_async_proc, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: func - default: None
     */

    /*
     * get_log, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None argument: loglist - default: None
     */

    /*
     * update_agent_password, <class 'agent.api.host.linux.Linux'> argument:
     * self - default: None argument: username - default: None argument:
     * password - default: None
     */
    public Boolean updateAgentPassword(String user, String pass) throws Ovm3ResourceException {
        Object x = callWrapper("update_agent_password", user, pass);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * yum_update, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: option - default: None
     */

    /*
     * discover_hardware, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Boolean discoverHardware() throws Ovm3ResourceException {
        Object result = callWrapper("discover_hardware");
        if (result == null) {
            return false;
        }
        Document xmlDocument;
        xmlDocument = prepParse((String) result);
        /* could be more subtle */
        String path = "//Discover_Hardware_Result/NodeInformation";
        hwPhysicalInfo = xmlToMap(path + "/VMM/PhysicalInfo", xmlDocument);
        hwSystemInfo = xmlToMap(path + "/DMTF/System", xmlDocument);
        return true;
    }

    /*
     * uninstall_package, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: pkg_list - default: None argument: option -
     * default: None
     */

    /*
     * get_datetime, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Integer getDateTime() throws Ovm3ResourceException {
        this.getLastBootTime();
        return this.localTime;
    }

    /*
     * configure_yum, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: section - default: None argument: params -
     * default: None
     */

    /*
     * get_yum_config, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    /* TODO: need to parse this */
    public Boolean getYumConfig() throws Ovm3ResourceException {
        Object x = callWrapper("get_yum_config");
        if (x == null) {
            return false;
        }
        return true;
    }

    /*
     * ovs_async_proc_stop, <class 'agent.api.host.linux.Linux'> argument: self
     * - default: None argument: pid - default: None
     */

    /*
     * set_statistic_interval, <class 'agent.api.host.linux.Linux'> argument:
     * interval - default: None
     */
    public Boolean setStatisticsInterval(int val) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("set_statistics_interval", val);
    }

    /*
     * yum_list_package, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: pkgnarrow - default: None argument: patterns -
     * default: None argument: showdups - default: None argument: ignore_case -
     * default: None
     */

    /*
     * get_timezone, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Boolean getTimeZone() throws Ovm3ResourceException  {
        Object[] result = (Object[]) callWrapper("get_timezone");
        if (result != null) {
            this.setTimeZ(result[0].toString());
            this.setTimeUTC(result[1].toString());
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
