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
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.hypervisor.ovm3.object.Linux.FileSystem;

class Test {
    private static final Logger LOGGER = Logger
            .getLogger(Test.class);
    private Test() {
    }

    /*
     * Other trinket
     * https://192.168.1.51:7002/ovm/core/OVMManagerCoreServlet&c=1
     * &s=-1&lb=p&t=2
     * &p=1dd5e891d9d0edbd81c2a69ab3d1b7ea%2C2f3b7fca202045058ae388d22f21f508'
     */
    private static Socket getSocket(String hostname, Integer port) throws UnknownHostException, IOException {
        Socket client = null;
        try {
            client = new Socket(hostname, port);
            LOGGER.error("Host " + hostname + " is connected");
        } catch (UnknownHostException e) {
            LOGGER.error("Host " + hostname + "not known: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error("Host " + hostname + " IOException " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Host " + hostname + " General Exception;: " + e.getMessage());
        }
        return client;
    }
    private static Connection agentConnect(String host, Integer port, String user, String pass) throws XmlRpcException, Exception {
        Connection c;
        try {
            c = new Connection(host, port, user, pass);
            LOGGER.debug("Agent connection to " + host + " succeeded");
        } catch (XmlRpcException e) {
            LOGGER.info("Agent connection for " + host + " caught XmlRpcException" + e.getMessage());
            throw new XmlRpcException(e.getMessage());
        } catch (Exception e) {
            LOGGER.info("Agent connection for " + host + " caught Exception" + e.getMessage());
            throw new Exception(e.getMessage());
        }
        return c;
    }


    public static void main(final String[] args) throws Exception {
        boolean checkNet = false;
        boolean checkNtp = false;
        boolean checkLinux = false;
        boolean checkCommon = false;
        boolean checkCluster = false;
        boolean checkRepo = false;
        boolean checkPool = false;
        boolean checkOcfs2 = false;
        boolean checkNFSPlugin = false;
        boolean checkXen = false;
        boolean checkVnc = false;

        boolean checkCombine = false;
        boolean checkVmInfo = false;
        boolean checkUuid = false;
        boolean checkBridge = false;
        boolean checkFs = false;
        boolean checkPlugin = true;

        String[] hostnames = {"ovm-1", "ovm-2", "localhost"};
        Integer port = 8899;
        String agentuser = "oracle";
        String agentpass = "test123";
        try {
            Connection c;
            Socket client = new Socket();
            String hostname = "localhost";
            for (String host: hostnames) {
                client = getSocket(host, port);
                if (client != null) {
                    hostname = host;
                    break;
                }
            }
            if (client == null || !client.isConnected()) {
                LOGGER.debug("Fatal no connection to " + hostname);
                return;
            }
            c = agentConnect(hostname, port, agentuser, agentpass);

            /*
             * needs to be finished and implement ovs + bridge, or do we count
             * on chef ?
             */
            if (checkPlugin) {
                CloudStackPlugin csp = new CloudStackPlugin(c);
                try {
                    LOGGER.debug(csp.ovsUploadSshKey("test",
                            "testing 123"));
                    // FileUtils.readFileToString(new File(""))));
                    String ip = "169.254.1.202";
                    String domain = "i-2-29-VM";
                    String pubnic = "bond0";
                    // LOGGER.debug(csp.ovsDom0Stats(pubnic));
                    // LOGGER.debug(csp.domrcheckPort(ip, 3922, 3, 3));
                    // LOGGER.debug(csp.domrcheckSsh(ip));
                    LOGGER.debug("vnc Port: " + csp.getVncPort(domain));
                    // LOGGER.debug(csp.domrExec(ip,
                    // "ls -l").getStdOut());
                    Map<String, String> stats = csp.ovsDomUStats(domain);
                    /*
                     * for (final Entry<String, String> stat : stats.entrySet())
                     * { LOGGER.debug(stat.getKey() + " " +
                     * Double.parseDouble(stat.getValue())); }
                     */
                    Thread.sleep(1000);
                    Map<String, String> stats2 = csp.ovsDomUStats(domain);
                    for (final Entry<String, String> stat : stats2.entrySet()) {
                        String key = stat.getKey();
                        Double delta = Double.parseDouble(stat.getValue())
                                - Double.parseDouble(stats.get(key));
                        LOGGER.debug(stat.getKey() + ": " + delta);
                    }
                    Integer cpus = Integer.parseInt(stats.get("vcpus"));
                    Double d_cpu = Double.parseDouble(stats.get("cputime"))
                            - Double.parseDouble(stats2.get("cputime"));
                    Double d_time = Double.parseDouble(stats.get("uptime"))
                            - Double.parseDouble(stats2.get("uptime"));
                    Double cpupct = d_cpu / d_time * 100 * cpus;
                    LOGGER.debug(cpupct);
                } catch (Exception e) {
                    LOGGER.debug("nooooo!!!" + e.getMessage());
                    throw new Exception(e.getMessage());
                }
            }
            if (checkFs) {
                Linux host = new Linux(c);

                Map<String, Linux.FileSystem> fsList = host
                        .getFileSystemList("nfs");
                Linux.FileSystem fs = fsList.get("nfs");
                LOGGER.debug(fs + " " + fsList);
            }
            if (checkUuid) {
                LOGGER.debug(UUID.nameUUIDFromBytes(("test@test-test")
                        .getBytes()));
            }
            if (checkNet) {
                Network net = new Network(c);
                LOGGER.debug(net.getInterfaceByName("c0a80100"));
                // net.discoverNetwork();
                LOGGER.debug(net.getInterfaceByName("c0a80100")
                        .getAddress());
                LOGGER.debug(net.getInterfaceByIp("192.168.1.65")
                        .getName());
                // LOGGER.debug(bridge.getMac());
            }
            if (checkCommon == true) {
                Common Com = new Common(c);
                String x = Com.getApiVersion();
                LOGGER.debug("Api Version: " + x);
                String y = Com.sleep(1);
                LOGGER.debug("Sleep: " + y);
                String msg = Com.echo("testing 1 2 3");
                LOGGER.debug("Echo: " + msg);
                /*
                 * String disp = Com.dispatch ("192.168.1.60", "hoeleboele");
                 * System.out. println("dispatch" + disp);
                 */
            }
            /* check stuff */
            if (checkLinux == true) {
                Linux Host = new Linux(c);
                Host.discoverHardware();
                Host.discoverServer();
                LOGGER.debug("cpus: " + Host.getCpuCores());
                LOGGER.debug("time; " + Host.getDateTime());
                // needs to be within bounds of 1970... *grin*
                LOGGER.debug("update time to 1999: "
                        + Host.setDateTime(1999, 12, 31, 12, 0, 0));
                LOGGER.debug("lastboot: " + Host.getLastBootTime());
                LOGGER.debug("time: " + Host.getTimeUTC());
                Calendar now = Calendar.getInstance();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH); // Note: zero based!
                int day = now.get(Calendar.DAY_OF_MONTH);
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);
                int second = now.get(Calendar.SECOND);
                int millis = now.get(Calendar.MILLISECOND);
                LOGGER.debug("set time to now: "
                        + Host.setDateTime(year, month, day, hour, minute,
                                second));
                LOGGER.debug("lastboot: " + Host.getLastBootTime());
                LOGGER.debug("UTC time: " + Host.getTimeUTC());
                LOGGER.debug("TZ time: " + Host.getTimeZ());
                LOGGER.debug("update password: "
                        + Host.updateAgentPassword("oracle", "test123"));
                LOGGER.debug("set time zone: "
                        + Host.setTimeZone("Europe/London", false));
                LOGGER.debug("time zone: " + Host.getTimeZone() + ", "
                        + Host.getTimeZ() + ", " + Host.getTimeUTC());
                LOGGER.debug("set time zone: "
                        + Host.setTimeZone("Europe/Amsterdam", true));
                LOGGER.debug("time zone: " + Host.getTimeZone() + ", "
                        + Host.getTimeZ() + ", " + Host.getTimeUTC());
                // LOGGER.debug("Luns: " + Host.discoverPhysicalLuns());

            }

            /* setting up ntp */
            if (checkNtp == true) {
                Ntp ntp = new Ntp(c);
                ntp.getDetails();
                LOGGER.debug("ntp isServer: " + ntp.isServer());
                LOGGER.debug("ntp isRunning: " + ntp.isRunning());
                LOGGER.debug("ntp Servers: " + ntp.servers());
                ntp.addServer("192.168.1.1");
                ntp.addServer("192.168.1.61");
                LOGGER.debug("ntp set: " + ntp.setNtp(true));
                LOGGER.debug("ntp enable: " + ntp.enableNtp());
                ntp.getDetails();
                LOGGER.debug("ntp isServer: " + ntp.isServer());
                LOGGER.debug("ntp isRunning: " + ntp.isRunning());
                LOGGER.debug("ntp Servers: " + ntp.servers());
                LOGGER.debug("ntp disable: " + ntp.disableNtp());
                LOGGER.debug("ntp reset: " + ntp.setNtp("", false));
            }

            if (checkNFSPlugin == true) {
                Linux lin = new Linux(c);
                lin.discoverServer();
                LOGGER.debug(lin.getCapabilities());
                Map<String, FileSystem> fsList = lin.getFileSystemList("nfs");
                LOGGER.debug(fsList);
                LOGGER.debug(BigInteger.valueOf(lin.getMemory()
                        .longValue()));
                LOGGER.debug(lin.getFreeMemory());
                BigInteger totalmem = BigInteger.valueOf(lin.getMemory()
                        .longValue());
                BigInteger freemem = BigInteger.valueOf(lin.getFreeMemory()
                        .longValue());
                LOGGER.debug(totalmem.subtract(freemem));
                /*
                 * for (final Map.Entry<String, Linux.FileSystem> entry : fsList
                 * .entrySet()) { Linux.FileSystem fs = entry.getValue();
                 * StoragePlugin sp = new StoragePlugin(c); String propUuid =
                 * sp.deDash(fs.getUuid()); String mntUuid = fs.getUuid();
                 * String fsType = "FileSys"; sp.setUuid(propUuid);
                 * sp.setSsUuid(propUuid); sp.setName(propUuid);
                 * sp.setFsType(fsType); sp.setFsServer(fs.getHost());
                 * sp.setFsSourcePath(fs.getDevice());
                 * sp.storagePluginGetFileSystemInfo(); }
                 */
                /*
                 * StoragePlugin sp = new StoragePlugin(c); String propUuid =
                 * sp.deDash(sp.newUuid()); String mntUuid = sp.newUuid();
                 * String nfsHost = "cs-mgmt"; String nfsPath =
                 * "/volumes/cs-data/primary"; String fsType = "FileSys";
                 * sp.setUuid(propUuid); sp.setName(propUuid);
                 * sp.setFsType(fsType); sp.setFsServer(nfsHost);
                 * sp.setFsSourcePath(nfsHost + ":" + nfsPath); //
                 * sp.fsTarget("/nfsmnt/" + mntUuid);
                 * sp.setFsMountPoint("/nfsmnt/" + mntUuid);
                 * sp.setMntUuid(mntUuid); sp.setSsUuid(propUuid);
                 * sp.setSsName("nfs:" + nfsPath); if (sp.storagePluginMount()
                 * != null) { lin.discoverMountedFs("nfs"); //
                 * LOGGER.debug(sp.extprops); StoragePlugin store = new
                 * StoragePlugin(c); store.setUuid(propUuid);
                 * store.setSsUuid(propUuid); store.setMntUuid(mntUuid);
                 * store.setFsHost(nfsHost); store.setFsSourcePath(nfsHost + ":"
                 * + nfsPath); // store.setFsMountPoint(pool.getPath());
                 * store.storagePluginGetFileSystemInfo();
                 * LOGGER.debug(store.getTotalSize());
                 * sp.setFsSourcePath(nfsHost + ":" + nfsPath);
                 * sp.storagePluginUnmount(); }
                 */
            }

            /* still needs to be finished! */
            if (checkRepo == true) {
                Repository repo = new Repository(c);
                String repouuid = repo.deDash(repo.newUuid());
                String remote = "cs-mgmt:/volumes/cs-data/secondary";
                String local = "/OVS/Repositories/" + repouuid;
                String url = "http://nibbler/~funs/iso";
                String iso = url + "/gentoo.iso";
                String vhd = url + "/ovm.raw";
                String isouuid = repo.deDash(repo.newUuid());
                String vmuuid = repo.deDash(repo.newUuid());

                repo.mountRepoFs(remote, local);
                repo.createRepo(remote, repouuid, repouuid, "My Comment");
                repo.discoverRepoDb();
                // repo.discoverRepo(repouuid);
                repo.importIso(iso, isouuid + ".iso", repouuid, "");
                repo.importVirtualDisk(vhd, vmuuid + ".img", repouuid, "");
                repo.deleteRepo(repouuid, true);
                repo.unmountRepoFs(local);
                repo.discoverRepoDb();
                repo.discoverRepo(repouuid);
            }

            if (checkPool == true) {
                LOGGER.debug("checking pool");
                Pool pool = new Pool(c);
                pool.discoverServerPool();
                LOGGER.debug(pool.getPoolAlias());
                LOGGER.debug(pool.getPoolId());
                if (pool.getPoolId().contentEquals("TEST")) {
                    LOGGER.debug("pool equals test");
                } else {
                    LOGGER.debug("pool" + pool.getPoolId());
                }
                List<String> ips = new ArrayList<String>();
                ips.add("192.168.1.64");
                ips.add("192.168.1.65");
                /*
                 * pool.setPoolIps(ips); pool.setPoolMemberIpList();
                 */
                /*
                 * if (pool.poolFsId != null) {
                 * pool.leaveServerPool(pool.poolFsId); }
                 */
                LOGGER.debug("pool members: "
                        + pool.getPoolMemberIpList());
            }

            if (checkOcfs2 == true) {
                PoolOCFS2 poolocfs = new PoolOCFS2(c);
                poolocfs.discoverPoolFs();
                // poolocfs.ocfs2GetMetaData();
            }

            if (checkCluster == true) {
                Pool pool = new Pool(c);
                pool.discoverServerPool();
                Cluster Clos = new Cluster(c);
                // Clos.destroyCluster(pool.poolFsId);
                if (pool.getPoolId() != null) {
                    // Clos.deconfigureServerForCluster(pool.poolId);
                }
                LOGGER.debug("Cluster online: " + Clos.isClusterOnline());
                LOGGER.debug("Cluster discover: "
                        + Clos.discoverCluster());

            }

            if (checkXen == true) {
                Xen xen = new Xen(c);
                xen.listVms();
                xen.createVm("xx", "xx");
                /* xen.deleteVm(repoId, vmId); */
            }

            /* check the combination of stuff */
            if (checkCombine == true) {
                /* prepare host, mgr should have "steady uuid" */
                OvmObject Go = new OvmObject();
                String masterUuid = Go.deDash(Go.newUuid());

                /* check capabilities */
                Linux Host = new Linux(c);
                Host.discoverServer();
                /* setup pool and role, needs utility to be able to do shit */
                Pool pool = new Pool(c);

                /*
                 * Info comes from Linux, not the pool, but needs to be set in
                 * the pool -sigh-
                 */
                if (Host.Get("Server_Roles").contentEquals(
                        pool.getValidRoles().toString())) {
                    pool.setServerRoles(pool.getValidRoles());
                }
                if (Host.Get("Membership_State").contentEquals("Unowned")) {
                    pool.takeOwnership(masterUuid, "");
                }
                /* get primary storage mounted and registered */

                StoragePlugin sp = new StoragePlugin(c);
                String propUuid = sp.deDash(sp.newUuid());
                String mntUuid = sp.newUuid();
                String nfsHost = "cs-mgmt";
                String nfsPath = "/volumes/cs-data/primary";
                String fsType = "FileSys";
                sp.setUuid(propUuid);
                sp.setName(propUuid);
                sp.setFsType(fsType);
                sp.setFsServer(nfsHost);
                sp.setFsSourcePath(nfsHost + ":" + nfsPath);
                sp.setMntUuid(mntUuid);
                sp.setSsUuid(propUuid);
                sp.setSsName("nfs:" + nfsPath);
                sp.setFsMountPoint("/nfsmnt/" + mntUuid);

                /* setup a repo */
                Repository repo = new Repository(c);
                String repouuid = repo.deDash(repo.newUuid());
                String remote = "cs-mgmt:/volumes/cs-data/secondary";
                String repopath = "/OVS/Repositories/" + repouuid;
                String url = "http://nibbler/~funs/iso";
                String iso = url + "/gentoo.iso";
                String vhd = url + "/ovm.raw";
                String isouuid = repo.deDash(repo.newUuid());
                String vmuuid = repo.deDash(repo.newUuid());

                repo.discoverRepoDb();

                repo.mountRepoFs(remote, repopath);
                repo.createRepo(remote, repouuid, repouuid, "My Comment");
                repo.discoverRepoDb();
                // repo.discoverRepo(repouuid);
                String isoname = isouuid + ".iso";
                String imgname = vmuuid + ".img";
                repo.importIso(iso, isoname, repouuid, "");
                repo.importVirtualDisk(vhd, imgname, repouuid, "");

                if (sp.storagePluginMount() != null) {
                    /* prep the VM disk to go to primary storage */
                    Linux vmDisk = new Linux(c);
                    String srcvmimg = repopath + "/VirtualDisks/" + imgname;
                    String dstvmimg = sp.getFsMountPoint() + "/" + imgname;
                    /*
                     * the "solving" of no real primary and secondary storage in
                     * OVS
                     */
                    vmDisk.copyFile(srcvmimg, dstvmimg);
                    Xen xen = new Xen(c);

                    /*
                     * 'vfb':
                     * ['type=vnc,vncunused=1,vnclisten=127.0.0.1,keymap=en-us']
                     */
                    /*
                     * 'disk': [
                     * 'file:/OVS/Repositories/0004fb0000030000aeaca859e4a8f8c0/VirtualDisks/0004fb0000120000c444117fd87ea251.img,xvda,w']
                     */
                    /* 'vif': ['mac=00:21:f6:00:00:00,bridge=c0a80100'] */
                    String vmName = Go.deDash(Go.newUuid());

                    Xen.Vm vm = xen.getVmConfig();
                    vm.setVmName(vmName);
                    vm.setVmUuid(vmName);

                    vm.addRootDisk(dstvmimg);

                    vm.addVif(0, "c0a80100", "00:21:f6:00:00:02");
                    vm.setVnc("0.0.0.0");
                    xen.createVm(repouuid, vm.getVmName());
                    xen.startVm(repouuid, vm.getVmName());
                    /*
                     * vm.stopVm(repouuid, vm.vmUuid); vm.deleteVm(repouuid,
                     * vm.vmUuid);
                     */
                    LOGGER.debug("Created VM with: " + vmName);
                    LOGGER.debug("repo: " + repouuid);
                    LOGGER.debug("image: " + imgname);
                    LOGGER.debug("disk: " + dstvmimg);
                    LOGGER.debug("master: " + masterUuid);
                }
            }
            if (checkVmInfo == true) {
                Xen host = new Xen(c);
                /* make an itterator */
                // String vmId = "14fc3846-45e5-3c08-ad23-432ceb07407b";
                // String repoId = "f12842eb-f5ed-3fe7-8da1-eb0e17f5ede8";
                String vmName = "s-1-VM";
                Xen.Vm vm = null;
                Xen.Vm ovm = null;
                try {
                    /* backwards for now: */
                    ovm = host.getRunningVmConfig(vmName);
                    LOGGER.debug(ovm.getVmRootDiskPoolId());
                    /* new style */
                    vm = host.getVmConfig(vmName);
                    vm.addIso("test.iso");
                    if (vm.getVmUuid().equals("")) {
                        LOGGER.debug("no vm found");
                    } else {
                        LOGGER.debug(vm.getVmParams());
                        LOGGER.debug(vm.getVmDisks());
                        LOGGER.debug(vm.getVmUuid());
                        LOGGER.debug(vm.getPrimaryPoolUuid());
                        vm.removeDisk("test.iso");
                        LOGGER.debug(vm.getVmParams().get("disk"));
                    }

                } catch (XmlRpcException e) {
                    LOGGER.debug("Failed to get VM details for " + vmName
                            + " on " + c.getIp());
                }
            }
            if (checkVnc == true) {
                Xen vms = new Xen(c);
                Xen.Vm vm = vms.listVms().get("Domain-0");
                vm.setVncAddress("0.0.0.0");
                vm.setVncPassword("testikkel");
                vm.setVnc();
                LOGGER.debug(vm.getVmVncs());
            }
            if (checkBridge) {
                Network net = new Network(c);
                for (final Map.Entry<String, Network.Interface> entry : net
                        .getInterfaceList().entrySet()) {
                    Network.Interface iface = entry.getValue();
                    LOGGER.debug("interface: " + iface.getName()
                            + ", phys: " + iface.getPhysical() + ", type: "
                            + iface.getIfType());
                }
                String physInterface = "bond0";
                Integer vlanId = 2;
                String physVlanInt = physInterface + "." + vlanId.toString();
                String brName = "c0a80100" + "." + vlanId.toString();
                LOGGER.debug(net.getInterfaceByName(physVlanInt) + " "
                        + net.getInterfaceByName(brName));

                if (net.getInterfaceByName(physVlanInt) == null) {
                    net.startOvsVlanConfig(physInterface, vlanId);
                }
                if (net.getInterfaceByName(brName) == null) {
                    net.startOvsBrConfig(brName, physVlanInt);
                }
                // net.startOvsLocalConfig("control0");
                // net.ovsBrConfig("start", "control0", "lo");
                // net.ovsIpConfig("control0", "static", "169.254.0.1",
                // "255.255.0.0");
                // execOverSsh("route del -net 169.254.0.0/16");
            }
            /* cleanup */
            /*
             * repo.deleteRepo(repouuid, true); repo.unmountRepoFs(repopath);
             * repo.discoverRepoDb(); repo.discoverRepo(repouuid);
             * sp.storagePluginUnmount();
             */
            client.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOGGER.debug(e.getMessage());
        }
    }
}
