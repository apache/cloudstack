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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;

/*
 * should become an interface implementation
 */
public class Linux extends OvmObject {
    private Integer _init = 0;

    /*
     * use capabilities to match things later, perhaps also hardware discovery ?
     * wrap getters and setters.... for Mapps...
     */
    public Map<String, String> Capabilities = new HashMap<String, String>();
    /*
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
    public Map<String, String> VMM = new HashMap<String, String>();
    public Map<String, String> VMMc = new HashMap<String, String>();
    public Map<String, String> NTP = new HashMap<String, String>();
    public Map<String, String> DateTime = new HashMap<String, String>();
    public Map<String, String> Generic = new HashMap<String, String>();
    /*
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
    public Map<String, String> hwVMM = new HashMap<String, String>();
    public Map<String, String> hwSystem = new HashMap<String, String>();
    public int localTime;
    public int lastBootTime;
    public String timeZone;
    public String timeUTC;
    public List<String> _mounts = null;

    // public Map<String, Map> Settings = new HashMap<String, Map>();

    public Linux(Connection c) {
        client = c;
    }

    /*
     * discover_server, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Boolean discoverServer() throws Exception {
        String cmd = "discover_server";
        Object result = callWrapper(cmd);
        if (result == null) {
            return false;
        }

        Document xmlDocument = prepParse((String) result);
        /* System.out.println(result); */
        /* could be more subtle */
        String path = "//Discover_Server_Result/Server";
        Capabilities = xmlToMap(path + "/Capabilities", xmlDocument);
        VMM = xmlToMap(path + "/VMM/Version", xmlDocument);
        VMMc = xmlToMap(path + "/VMM", xmlDocument);
        NTP = xmlToMap(path + "/NTP", xmlDocument);
        DateTime = xmlToMap(path + "/Date_Time", xmlDocument);
        Generic = xmlToMap(path, xmlDocument);

        // System.out.println(Get("Agent_Version"));

        // System.out.println(Generic.toString());
        return true;
    }

    public String getAgentVersion() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Agent_Version");
    }
    public String getHostKernelRelease() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Host_Kernel_Release");
    }
    public String getHostOs() throws ParserConfigurationException, IOException,
            Exception {
        return this.Get("OS_Name");
    }
    public String getHostOsVersion() throws ParserConfigurationException,
            IOException, Exception {
        String ver = this.Get("OS_Major_Version") + "."
                + this.Get("OS_Minor_Version");
        return ver;
    }
    public String getHypervisorName() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Hypervisor_Name");
    }
    public String getHypervisorVersion() throws ParserConfigurationException, IOException, Exception {
        String ver = this.getHypervisorMajor() + "."
                + this.getHypervisorMinor() + "." + this.getHypervisorExtra();
        return ver;
    }
    public String getCapabilities() throws ParserConfigurationException,
            IOException, Exception {
        return this.Get("Capabilities");
    }
    public String getHypervisorMajor() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Major");
    }
    public String getHypervisorMinor() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Minor");
    }
    public String getHypervisorExtra() throws ParserConfigurationException,
            IOException, Exception {
        return this.Get("Extra").replace(".", "");
    }
    public String getManagerUuid() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Manager_Unique_Id");
    }

    public String getMembershipState() throws ParserConfigurationException,
            IOException, Exception {
        return this.Get("Membership_State");
    }

    public String getServerRoles() throws ParserConfigurationException,
            IOException, Exception {
        return this.Get("Server_Roles");
    }
    public boolean getIsMaster() throws ParserConfigurationException,
            IOException, Exception {
        return Boolean.parseBoolean(this.Get("Is_Current_Master"));
    }
    public String getOvmVersion() throws ParserConfigurationException, IOException, Exception {
        return this.Get("OVM_Version");
    }
    public String getHostName() throws ParserConfigurationException, IOException, Exception {
        return this.Get("Hostname");
    }
    public Integer getCpuKhz() throws NumberFormatException, ParserConfigurationException, IOException, Exception {
        return Integer.valueOf(this.Get("CPUKHz"));
    }
    public Integer getCpuSockets() throws NumberFormatException, ParserConfigurationException, IOException, Exception {
        return Integer.valueOf(this.Get("SocketsPerNode"));
    }
    public Integer getCpuThreads() throws NumberFormatException, ParserConfigurationException, IOException, Exception {
        return Integer.valueOf(this.Get("ThreadsPerCore"));
    }
    public Integer getCpuCores() throws NumberFormatException, ParserConfigurationException, IOException, Exception {
        return Integer.valueOf(this.Get("CoresPerSocket"));
    }
    public Integer getTotalThreads() throws NumberFormatException, ParserConfigurationException, IOException, Exception {
        return this.getCpuSockets() * this.getCpuCores() * this.getCpuThreads();
    }

    public Double getMemory() throws NumberFormatException,
            ParserConfigurationException, IOException, Exception {
        return Double.valueOf(this.Get("TotalPages")) * 4096;
    }

    public Double getFreeMemory() throws NumberFormatException,
            ParserConfigurationException, IOException, Exception {
        return Double.valueOf(this.Get("FreePages")) * 4096;
    }
    public String getUuid() throws NumberFormatException,
            ParserConfigurationException, IOException, Exception {
        return this.Get("Unique_Id");
    }

    public String Get(String element) throws ParserConfigurationException, IOException, Exception {
        if (this._init == 0) {
            this.discoverHardware();
            this.discoverServer();
            this._init = 1;
        }
        if (Generic.containsKey(element))
            return Generic.get(element);
        if (VMMc.containsKey(element))
            return VMMc.get(element);
        if (VMM.containsKey(element))
            return VMM.get(element);
        if (hwVMM.containsKey(element))
            return hwVMM.get(element);
        if (hwSystem.containsKey(element))
            return hwSystem.get(element);
        if (Capabilities.containsKey(element))
            return Capabilities.get(element);
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
    public Integer getLastBootTime() throws XmlRpcException {
        HashMap<String, Long> result = callMap("get_last_boot_time");
        if (result == null)
            return null;
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
            int sec) throws XmlRpcException {
        Object x = callWrapper("set_datetime", year, month, day, hour, min, sec);
        if (x == null)
            return true;

        return false;
    }

    /*
     * list_package, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None argument: name - default: None
     */

    /*
     * discover_physical_luns, <class 'agent.api.host.linux.Linux'> argument:
     * self - default: None argument: args - default: None
     */
    public String discoverPhysicalLuns() throws XmlRpcException {
        String x = (String) callWrapper("discover_physical_luns", "");
        return x;
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
    public Boolean setTimeZone(String tz, Boolean utc) throws XmlRpcException {
        Object x = callWrapper("set_timezone", tz, utc);
        if (x == null)
            return true;

        return false;
    }

    /*
     * copy_file, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None argument: src - default: None argument: dst - default: None
     * argument: sparse - default: None argument: update_period - default: None
     */
    public Boolean copyFile(String src, String dst) throws XmlRpcException {
        Object x = callWrapper("copy_file", src, dst, false);
        if (x == null)
            return true;

        return false;
    }

    public Boolean copyFile(String src, String dst, Boolean sparse)
            throws XmlRpcException {
        Object x = callWrapper("copy_file", src, dst, sparse);
        if (x == null)
            return true;

        return false;
    }

    /*
     * discover_mounted_file_systems, <class 'agent.api.host.linux.Linux'>
     * argument: self - default: None argument: args - default: None
     */
    /*
     * <Discover_Mounted_File_Systems_Result> <Filesystem Type="nfs"> <Mount
     * Dir="/nfsmnt/e080e318-91c2-47e5-a5ab-f3ab53790162">
     * <Device>cs-mgmt:/volumes/cs-data/secondary/</Device>
     * <Mount_Options>rw,relatime
     * ,vers=3,rsize=524288,wsize=524288,namlen=255,hard
     * ,proto=tcp,port=65535,timeo
     * =600,retrans=2,sec=sys,local_lock=none,addr=192.168.1.61</Mount_Options>
     * </Mount> </Filesystem> ... </Discover_Mounted_File_Systems_Result>
     */

    public Boolean discoverMountedFs() throws XmlRpcException {
        Object x = callWrapper("discover_mounted_file_systems");
        // System.out.println(x);
        if (x == null)
            return true;

        return false;
    }

    /* Filesystem bits and bobs */
    private Map<String, FileSystem> fsList = null;

    public Map<String, FileSystem> getFileSystemList(String type)
            throws ParserConfigurationException, IOException, Exception {
        if (fsList == null)
            this.discoverMountedFs(type);

        return fsList;
    }

    public void setFileSystemList(Map<String, FileSystem> list) {
        fsList = list;
    }
    public class FileSystem {
        public Map<String, Object> _fs = new HashMap<String, Object>() {
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

        public String getUuid() {
            return (String) _fs.get("Uuid");
        }

        public String setUuid(String uuid) {
            return (String) _fs.put("Uuid", uuid);
        }
        public String getName() {
            return (String) _fs.get("Name");
        }

        public String setName(String name) {
                return (String) _fs.put("Name", name);
        }

        public String getDevice() {
                return (String) _fs.get("Device");
        }

        public String setDevice(String dev) {
                return (String) _fs.put("Device", dev);
        }

        public String getHost() {
                if (getDevice() != null && getDevice().contains(":")) {
                    String spl[] = getDevice().split(":");
                    setHost(spl[0]);
                    setMountPoint(spl[1]);
                } else {
                    return null;
                }
                return (String) _fs.get("Host");
        }

        public String setHost(String host) {
                return (String) _fs.put("Host", host);
        }

        public String getDir() {
                return (String) _fs.get("Dir");
        }

        public String setDir(String dir) {
            return (String) _fs.put("Dir", dir);
        }

        public String getMountPoint() {
            if (getHost() != null) {
                return (String) _fs.get("Mount_Point");
            }
            return null;
        }
        public String setMountPoint(String pnt) {
            return (String) _fs.put("Mount_Point", pnt);
        }
    };

    /* should actually be called "getMountedsFsDevice" or something */
    public Map<String, FileSystem> discoverMountedFs(String type)
            throws ParserConfigurationException, IOException, Exception {
        // if (postDiscovery == null) {
        //    postDiscovery = callWrapper("discover_network");
        this.fsList = new HashMap<String, FileSystem>();
        //}
        Object x = callWrapper("discover_mounted_file_systems", type);
        Document xmlDocument = prepParse((String) x);
        // List<String> list = new ArrayList<String>();
        String bpath = "//Discover_Mounted_File_Systems_Result/Filesystem";
        String mpath = bpath + "/Mount/@Dir";
        _mounts = xmlToList(mpath, xmlDocument);
        for (String mnt : _mounts) {
            String dpath = bpath + "/Mount[@Dir='" + mnt + "']";
            Map<String, Object> fs = xmlToMap(dpath, xmlDocument);
            FileSystem f = new FileSystem();
            f._fs = fs;
            String[] spl = mnt.split("/");
            String uuid = spl[spl.length - 1];
            // System.out.println(uuid + " " + mnt);
            f.setUuid(uuid);
            f.setDir(mnt);
            fsList.put(uuid, f);
        }
        setFileSystemList(fsList);
        if (x == null) {
            return this.fsList;
        }

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
    public Boolean updateAgentPassword(String user, String pass)
            throws XmlRpcException {
        Object x = callWrapper("update_agent_password", user, pass);
        if (x == null)
            return true;

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
    public Boolean discoverHardware()
            throws ParserConfigurationException, IOException, Exception {
        Object result = callWrapper("discover_hardware");

        Document xmlDocument = prepParse((String) result);
        /* could be more subtle */
        String path = "//Discover_Hardware_Result/NodeInformation";
        hwVMM = xmlToMap(path + "/VMM/PhysicalInfo", xmlDocument);
        hwSystem = xmlToMap(path + "/DMTF/System", xmlDocument);

        if (result == null)
            return true;

        return false;
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
    public Integer getDateTime() throws XmlRpcException {
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
    public Boolean getYumConfig() throws XmlRpcException {
        Object x = callWrapper("get_yum_config");
        // System.out.println(x);
        if (x == null)
            return true;

        return false;
    }

    /*
     * ovs_async_proc_stop, <class 'agent.api.host.linux.Linux'> argument: self
     * - default: None argument: pid - default: None
     */

    /*
     * set_statistic_interval, <class 'agent.api.host.linux.Linux'> argument:
     * interval - default: None
     */
    public Boolean setStatisticsInterval(int val) throws XmlRpcException {
        Object x = callWrapper("set_statistics_interval", val);
        if (x == null)
            return true;

        return false;
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
    public Boolean getTimeZone() throws XmlRpcException {
        Object[] result = (Object[]) callWrapper("get_timezone");
        if (result != null) {
            this.timeZone = result[0].toString();
            this.timeUTC = result[1].toString();
            return true;
        }
        return false;
    }

}
