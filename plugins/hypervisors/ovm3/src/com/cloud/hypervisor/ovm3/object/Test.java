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
    private static Socket getSocket(String hostname, Integer port) throws IOException {
        Socket client = null;
        try {
            client = new Socket(hostname, port);
            LOGGER.error("Host " + hostname + " is connected");
        } catch (UnknownHostException e) {
            LOGGER.error("Host " + hostname + "not known: ", e);
        } catch (IOException e) {
            LOGGER.error("Host " + hostname + " IOException ", e);
        } catch (Exception e) {
            LOGGER.error("Host " + hostname + " General Exception;: ", e);
        }
        return client;
    }
    private static Connection agentConnect(String[] hosts, Integer port, String user, String pass) throws Ovm3ResourceException {
        Connection c;
        String hostname = "localhost";
        try {
            Socket client = new Socket();
            for (String host: hosts) {
                client = getSocket(host, port);
                if (client != null) {
                    hostname = host;
                    break;
                }
            }
            if (client == null || !client.isConnected()) {
                LOGGER.debug("Fatal no connection to " + hostname);
                return null;
            }
            client.close();
            c = new Connection(hostname, port, user, pass);
            LOGGER.debug("Agent connection to " + hostname + " succeeded");
            return c;
        } catch (IOException e) {
            LOGGER.error("IOException: ", e);
            throw new Ovm3ResourceException("IOException occured: ", e);
        }
    }


    public static void main(final String[] args) {
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
        boolean checkVmInfo = true;
        boolean checkUuid = false;
        boolean checkBridge = false;
        boolean checkFs = false;
        boolean checkPlugin = false;

        String[] hostnames = {"ovm-2", "ovm-1", "localhost"};
        Integer port = 8899;
        String agentuser = "oracle";
        String agentpass = "test123";
        try {
            Connection c = agentConnect(hostnames, port, agentuser, agentpass);
            /*
             * needs to be finished and implement ovs + bridge, or do we count
             * on chef ?
             */
            if (checkPlugin) {
                CloudStackPlugin csp = new CloudStackPlugin(c);
                try {
                    LOGGER.debug(csp.ovsUploadSshKey("test",
                            "testing 123"));
                    String ip = "169.254.1.202";
                    String domain = "i-2-29-VM";
                    String pubnic = "bond0";
                    LOGGER.debug("vnc Port: " + csp.getVncPort(domain));
                    Map<String, String> stats = csp.ovsDomUStats(domain);
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
                    LOGGER.error("nooooo!!!", e);
                    throw new Exception(e);
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
                LOGGER.debug(net.getInterfaceByName("c0a80100")
                        .getAddress());
                LOGGER.debug(net.getInterfaceByIp("192.168.1.65")
                        .getName());
            }
            if (checkCommon) {
                Common Com = new Common(c);
                String x = Com.getApiVersion();
                LOGGER.debug("Api Version: " + x);
                String y = Com.sleep(1);
                LOGGER.debug("Sleep: " + y);
                String msg = Com.echo("testing 1 2 3");
                LOGGER.debug("Echo: " + msg);
            }
            /* check stuff */
            if (checkLinux) {
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
                // Note: zero based!
                int month = now.get(Calendar.MONTH);
                int day = now.get(Calendar.DAY_OF_MONTH);
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);
                int second = now.get(Calendar.SECOND);
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
            }

            /* setting up ntp */
            if (checkNtp) {
                Ntp ntp = new Ntp(c);
                ntp.getDetails();
                LOGGER.debug("ntp isServer: " + ntp.isServer());
                LOGGER.debug("ntp isRunning: " + ntp.isRunning());
                LOGGER.debug("ntp Servers: " + ntp.getServers());
                ntp.addServer("192.168.1.1");
                ntp.addServer("192.168.1.61");
                LOGGER.debug("ntp set: " + ntp.setNtp(true));
                LOGGER.debug("ntp enable: " + ntp.enableNtp());
                ntp.getDetails();
                LOGGER.debug("ntp isServer: " + ntp.isServer());
                LOGGER.debug("ntp isRunning: " + ntp.isRunning());
                LOGGER.debug("ntp Servers: " + ntp.getServers());
                LOGGER.debug("ntp disable: " + ntp.disableNtp());
                LOGGER.debug("ntp reset: " + ntp.setNtp("", false));
            }

            if (checkNFSPlugin) {
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
            }

            /* still needs to be finished! */
            if (checkRepo) {
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
                repo.importIso(iso, isouuid + ".iso", repouuid, "");
                repo.importVirtualDisk(vhd, vmuuid + ".img", repouuid, "");
                repo.deleteRepo(repouuid, true);
                repo.unmountRepoFs(local);
                repo.discoverRepoDb();
                repo.discoverRepo(repouuid);
            }

            if (checkPool) {
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
                LOGGER.debug("pool members: "
                        + pool.getPoolMemberIpList());
            }

            if (checkOcfs2) {
                PoolOCFS2 poolocfs = new PoolOCFS2(c);
                poolocfs.discoverPoolFs();
                // ocfs2GetMetaData
            }

            if (checkCluster) {
                Pool pool = new Pool(c);
                pool.discoverServerPool();
                Cluster clos = new Cluster(c);
                if (pool.getPoolId() != null) {
                    LOGGER.debug("Pool found: " + pool.getPoolId());
                }
                LOGGER.debug("Cluster online: " + clos.isClusterOnline());
                LOGGER.debug("Cluster discover: "
                        + clos.discoverCluster());

            }

            if (checkXen) {
                try {
                    Xen xen = new Xen(c);
                    Xen.Vm vm = xen.getVmConfig("s-1-VM");
                    Xen.Vm vm1 = xen.getRunningVmConfig("v-2-VM");
                    LOGGER.debug(vm.getVmUuid() + " " + vm1.getVmUuid());
                } catch (Exception e) {
                    throw e;
                }
            }

            /* check the combination of stuff */
            if (checkCombine) {
                /* prepare host, mgr should have "steady uuid" */
                OvmObject go = new OvmObject();
                String masterUuid = go.deDash(go.newUuid());

                /* check capabilities */
                Linux host = new Linux(c);
                host.discoverServer();
                /* setup pool and role, needs utility to be able to do shit */
                Pool pool = new Pool(c);

                /*
                 * Info comes from Linux, not the pool, but needs to be set in
                 * the pool -sigh-
                 */
                if (host.get("Server_Roles").contentEquals(
                        pool.getValidRoles().toString())) {
                    pool.setServerRoles(pool.getValidRoles());
                }
                if (host.get("Membership_State").contentEquals("Unowned")) {
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
                    String vmName = go.deDash(go.newUuid());

                    Xen.Vm vm = xen.getVmConfig();
                    vm.setVmName(vmName);
                    vm.setVmUuid(vmName);

                    vm.addRootDisk(dstvmimg);

                    vm.addVif(0, "c0a80100", "00:21:f6:00:00:02");
                    vm.setVnc("0.0.0.0");
                    xen.createVm(repouuid, vm.getVmName());
                    xen.startVm(repouuid, vm.getVmName());
                    LOGGER.debug("Created VM with: " + vmName);
                    LOGGER.debug("repo: " + repouuid);
                    LOGGER.debug("image: " + imgname);
                    LOGGER.debug("disk: " + dstvmimg);
                    LOGGER.debug("master: " + masterUuid);
                }
            }
            if (checkVmInfo) {
                Xen host = new Xen(c);
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
                    if ("".equals(vm.getVmUuid())) {
                        LOGGER.debug("no vm found");
                    } else {
                        LOGGER.debug(vm.getVmParams());
                        LOGGER.debug(vm.getVmUuid());
                        LOGGER.debug(vm.getPrimaryPoolUuid());
                        vm.removeDisk("test.iso");
                        LOGGER.debug(vm.getVmParams().get("disk"));
                    }

                } catch (Exception e) {
                    LOGGER.error("Failed to get VM details for " + vmName
                            + " on " + c.getIp(), e);
                }
            }
            if (checkVnc) {
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
            }
        } catch (Exception e) {
            LOGGER.error("Something went wrong, I know for sure!", e);
        }
    }
}
