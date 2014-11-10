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
import java.util.HashMap;
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
    public static class Host {
        private final Map<String, String> hostDetail = new HashMap<String, String>() {
            {
                put("Hostname", null);
                put("Port", null);
                put("IP", null);
                put("Username", null);
                put("Password", null);
                put("Root", null);
                put("Pass", null);
            }
        };
        public Host() {
        }
        public Host(String name, String port, String uname, String pass) {
            setHostname(name);
            setPort(port);
            setUsername(uname);
            setPassword(pass);
        }

        public void setHostDetails(Map<String, String> details) {
            hostDetail.putAll(details);
        }
        public String getHostname() {
            return hostDetail.get("Hostname");
        }
        public String getPort() {
            return hostDetail.get("Port");
        }
        public String getUsername() {
            return hostDetail.get("Username");
        }
        public String getPassword() {
            return hostDetail.get("Password");
        }
        public String setHostname(String host) {
            return hostDetail.put("Hostname", host);
        }
        public String setPort(String port) {
            return hostDetail.put("Port", port);
        }
        public String setUsername(String user) {
            return hostDetail.put("Username", user);
        }
        public String setPassword(String pass) {
            return hostDetail.put("Password", pass);
        }
    };

    private static List<Connection> agentConnect(Map<String, Host> hosts) throws Ovm3ResourceException {
        List<Connection> c = new ArrayList();
        try {
            for (final Entry<String, Host> host : hosts.entrySet()) {
                Host hostDetail = host.getValue();
                Socket client = getSocket(hostDetail.getHostname(), Integer.valueOf(hostDetail.getPort()));
                if (client == null || !client.isConnected()) {
                    System.out.println("Unable to connect to " + hostDetail.getHostname() + ", " + hostDetail.getPort());
                } else {
                    client.close();
                    Connection con = new Connection(hostDetail.getHostname(),
                            Integer.valueOf(hostDetail.getPort()),
                            hostDetail.getUsername(),
                            hostDetail.getPassword());
                    System.out.println("Agent connected to " + hostDetail.getHostname() + ", " + hostDetail.getPort());
                    c.add(con);
                }
            }
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
        boolean checkRepo = true;
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
        boolean checkPlugin = false;
        boolean checkRemote = false;
        boolean checkError = false;
        boolean checkStoragePlugin = false;

        Integer hostCount = 0;
        String agentuser = "oracle";
        String agentpass = "test123";
        Map<String, Host> hosts = new HashMap<String, Host>();
        hosts.put("ovm-2", new Host("ovm-2", "8899", agentuser, agentpass));
        // hosts.put("192.168.56.101", new Host("192.168.56.101", "8899", agentuser, agentpass));
        // hosts.put("ovm-1-local", new Host("localhost", "8898", agentuser, agentpass));
        // hosts.put("ovm-2-local", new Host("localhost", "8899", agentuser, agentpass));
        hosts.put("ovm-1", new Host("ovm-1", "8899", agentuser, agentpass));
        try {
            List<Connection> connections = agentConnect(hosts);
            hostCount = connections.size();
            if (hostCount == 1) {
                System.out.println("Single host: " + hostCount);
            } else if (hostCount > 1) {
                System.out.println("Multiple hosts: " + hostCount);
            } else {
                throw new Ovm3ResourceException("No hosts found that were up!!!!");
            }
            Connection c = connections.get(0);

            /*
             * needs to be finished and implement ovs + bridge, or do we count
             * on chef ?
             */
            if (checkStoragePlugin) {
                StoragePlugin store = new StoragePlugin(c);
                String poolUuid = "f12842eb-f5ed-3fe7-8da1-eb0e17f5ede8";
                String file = "/OVS/Repositories/" + store.deDash(poolUuid)
                        + "/VirtualDisks/test.raw";
                // System.out.println(store.storagePluginListFs("cs-mgmt"));
                StoragePlugin.StorageDetails ds = store.storagePluginMountNFS("cs-mgmt", "/volumes/cs-data/secondary", "bd5c266b-5738-39a8-a6f0-6f2344ec861e", "/nfsmnt");
                System.out.println(store.storagePluginUnmountNFS("cs-mgmt", "/volumes/cs-data/secondary", "bd5c266b-5738-39a8-a6f0-6f2344ec861e", "/nfsmnt"));
                // System.out.println(ss.getUuid());
            }
            if (checkError) {
                Xen xen = new Xen(c);
                try {
                    Xen.Vm vm = xen.getRunningVmConfig("testikkel");
                    // Xen.Vm vm = xen.getVmConfig("test", "test");
                } catch (Ovm3ResourceException y) {
                    System.out.println(y);
                }
            }
            if (checkRemote) {
                Remote rem = new Remote(c);
                System.out.println(rem.sysReboot());
            }
            if (checkPlugin) {
                CloudStackPlugin csp = new CloudStackPlugin(c);
                try {
                    String domain = "r-4-VM";
                    String pubnic = "xenbr0";
                    System.out.println("vnc Port: " + csp.getVncPort(domain));
                    Map<String, String> stats = csp.ovsDomUStats(domain);
                    Thread.sleep(1000);
                    Map<String, String> stats2 = csp.ovsDomUStats(domain);
                    for (final Entry<String, String> stat : stats2.entrySet()) {
                        String key = stat.getKey();
                        Double delta = Double.parseDouble(stat.getValue())
                                - Double.parseDouble(stats.get(key));
                        System.out.println(stat.getKey() + ": " + delta);
                    }
                    Integer cpus = Integer.parseInt(stats.get("vcpus"));
                    Double d_cpu = Double.parseDouble(stats.get("cputime"))
                            - Double.parseDouble(stats2.get("cputime"));
                    Double d_time = Double.parseDouble(stats.get("uptime"))
                            - Double.parseDouble(stats2.get("uptime"));
                    Double cpupct = d_cpu / d_time * 100 * cpus;
                    System.out.println(cpupct);
                } catch (Exception e) {
                    LOGGER.error("nooooo!!!", e);
                    throw new Exception(e);
                }
            }
            if (checkFs) {
                Linux host = new Linux(c);

                Map<String, Linux.FileSystem> fsList = host
                        .getFileSystemMap("nfs");
                Linux.FileSystem fs = fsList.get("nfs");
                System.out.println(fs + " " + fsList);
            }
            if (checkUuid) {
                System.out.println(UUID.nameUUIDFromBytes(("test@test-test")
                        .getBytes()));
            }
            if (checkNet) {
                Network net = new Network(c);
                System.out.println(net.getInterfaceByName("c0a80100"));
                System.out.println(net.getInterfaceByName("c0a80100")
                        .getAddress());
                System.out.println(net.getInterfaceByIp("192.168.1.65")
                        .getName());
            }
            if (checkCommon) {
                Common com = new Common(c);
                Common com1 = new Common(connections.get(1));
                System.out.println(com.getClient().getPort() + " " + connections.get(0).getPort());
                System.out.println(com1.getClient().getPort() + " " + connections.get(1).getPort());
                Integer x = com.getApiVersion();
                System.out.println("Api Version: " + x);
                Boolean y = com.sleep(1);
                System.out.println("Sleep: " + y);
                String msg = com.echo("testing 1 2 3");
                System.out.println("Echo: " + msg);
            }
            /* check stuff */
            if (checkLinux) {
                Linux Host = new Linux(c);
                Host.discoverHardware();
                Host.discoverServer();
                System.out.println("cpus: " + Host.getCpuCores());
                System.out.println("time; " + Host.getDateTime());
                // needs to be within bounds of 1970... *grin*
                System.out.println("update time to 1999: "
                        + Host.setDateTime(1999, 12, 31, 12, 0, 0));
                System.out.println("lastboot: " + Host.getLastBootTime());
                System.out.println("time: " + Host.getTimeUTC());
                Calendar now = Calendar.getInstance();
                int year = now.get(Calendar.YEAR);
                // Note: zero based!
                int month = now.get(Calendar.MONTH);
                int day = now.get(Calendar.DAY_OF_MONTH);
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);
                int second = now.get(Calendar.SECOND);
                System.out.println("set time to now: "
                        + Host.setDateTime(year, month, day, hour, minute,
                                second));
                System.out.println("lastboot: " + Host.getLastBootTime());
                System.out.println("UTC time: " + Host.getTimeUTC());
                System.out.println("TZ time: " + Host.getTimeZ());
                System.out.println("update password: "
                        + Host.updateAgentPassword("oracle", "test123"));
                System.out.println("set time zone: "
                        + Host.setTimeZone("Europe/London", false));
                System.out.println("time zone: " + Host.getTimeZone() + ", "
                        + Host.getTimeZ() + ", " + Host.getTimeUTC());
                System.out.println("set time zone: "
                        + Host.setTimeZone("Europe/Amsterdam", true));
                System.out.println("time zone: " + Host.getTimeZone() + ", "
                        + Host.getTimeZ() + ", " + Host.getTimeUTC());
            }

            /* setting up ntp */
            if (checkNtp) {
                Ntp ntp = new Ntp(c);
                ntp.getDetails();
                System.out.println("ntp isServer: " + ntp.isServer());
                System.out.println("ntp isRunning: " + ntp.isRunning());
                System.out.println("ntp Servers: " + ntp.getServers());
                ntp.addServer("192.168.1.1");
                ntp.addServer("192.168.1.61");
                System.out.println("ntp set: " + ntp.setNtp(true));
                System.out.println("ntp enable: " + ntp.enableNtp());
                ntp.getDetails();
                System.out.println("ntp isServer: " + ntp.isServer());
                System.out.println("ntp isRunning: " + ntp.isRunning());
                System.out.println("ntp Servers: " + ntp.getServers());
                System.out.println("ntp disable: " + ntp.disableNtp());
                System.out.println("ntp reset: " + ntp.setNtp("", false));
            }

            if (checkNFSPlugin) {
                Linux lin = new Linux(c);
                lin.discoverServer();
                System.out.println(lin.getCapabilities());
                Map<String, FileSystem> fsList = lin.getFileSystemMap("nfs");
                System.out.println(fsList);
                System.out.println(BigInteger.valueOf(lin.getMemory()
                        .longValue()));
                System.out.println(lin.getFreeMemory());
                BigInteger totalmem = BigInteger.valueOf(lin.getMemory()
                        .longValue());
                BigInteger freemem = BigInteger.valueOf(lin.getFreeMemory()
                        .longValue());
                System.out.println(totalmem.subtract(freemem));
            }

            /* still needs to be finished! */
            if (checkRepo) {
                Linux host = new Linux(c);
                Pool pool = new Pool(c);
                if (host.get("Server_Roles").contentEquals(
                        pool.getValidRoles().toString())) {
                    pool.setServerRoles(pool.getValidRoles());
                }
                if (host.get("Membership_State").contentEquals("Unowned")) {
                    pool.takeOwnership(pool.deDash(pool.newUuid("master")), "");
                }
                Repository repo = new Repository(c);
                repo.discoverRepoDb();
                System.out.println(repo.repoDbs);
                for (String rid: repo.getRepoDbList()) {
                    System.out.println(repo.getRepoDb(rid).getMountPoint());
                    repo.discoverRepo(rid);
                    System.out.println(rid + ": " + repo.getRepo(rid).getAlias());
                    System.out.println(repo.getRepo(rid).getRepoVirtualDisks());
                    System.out.println(repo.getRepo(rid).getRepoVirtualMachines());
                    System.out.println(repo.getRepo(rid).getRepoISOs());
                    System.out.println(repo.getRepo(rid).getRepoTemplates());
                }
                /*
                String mntUuid = repo.newUuid("test_repo");
                String repoUuid = repo.deDash(mntUuid);
                String remoteHost = "cs-mgmt";
                String remotePath = "/volumes/cs-data/primary/tovm";
                String remoteUri = remoteHost+":"+remotePath;
                String local = "/OVS/Repositories/" + repoUuid;
                String url = "http://nibbler/~funs/iso";
                String iso = url + "/gentoo.iso";
                String vhd = url + "/ovm.raw";
                String isouuid = repo.deDash(repo.newUuid("iso"));
                String vmuuid = repo.deDash(repo.newUuid("vm"));
                repo.discoverRepo(repoUuid);
                repo.discoverRepoDb();
                repo.mountRepoFs(remoteUri, local);
                repo.createRepo(remoteUri, repoUuid, repoUuid, "My Comment");
                StoragePlugin store = new StoragePlugin(c);
                StoragePlugin.StorageDetails sd = store.storagePluginMountNFS(remoteHost,
                        remotePath+"/VirtualMachines",
                        mntUuid,
                        "/nfsmnt");
                System.out.println(sd.getUuid());
                sd = store.storagePluginMountNFS(remoteHost,
                        remotePath+"/VirtualMachines",
                        mntUuid,
                        "/nfsmnt");
                repo.discoverRepoDb();
                System.out.println(repo.getRepoDb());
                repo.importIso(iso, isouuid + ".iso", repoUuid, "");
                repo.importVirtualDisk(vhd, vmuuid + ".img", repoUuid);
                System.out.println(store.storagePluginGetFileSystemInfo(repoUuid, mntUuid, remoteHost, remotePath+"/VirtualMachines").getFreeSize());
                store.storagePluginUnmountNFS(remoteHost,
                        remotePath+"/VirtualMachines",
                        mntUuid,
                        "/nfsmnt");
                Thread.sleep(120000);
                System.out.println(store.storagePluginGetFileSystemInfo(repoUuid, mntUuid, remoteHost, remotePath+"/VirtualMachines").getSize());
                repo.deleteRepo(repoUuid, true);
                repo.unmountRepoFs(local);
                repo.discoverRepoDb();
                repo.discoverRepo(repoUuid);
                */
            }

            if (checkPool) {
                System.out.println("checking pool");
                List<String> memberList = new ArrayList<String>();
                for (final Connection member : connections) {
                    Linux lin = new Linux(member);
                    memberList.add(lin.getHostName());
                    Pool pool = new Pool(member);
                    System.out.println(pool.getPoolMemberList());
                    if (pool.isInAPool()) {
                        System.out.println("Is in a pool");
                        System.out.println("pool alias: " + pool.getPoolAlias());
                        System.out.println("pool id: " + pool.getPoolId());
                        System.out.println("pool members: "
                                + pool.getPoolMemberList());
                    }
                }
                for (final Connection member : connections) {
                    final Pool xpool = new Pool(member);
                    xpool.setPoolIps(memberList);
                    xpool.setPoolMemberList();
                    LOGGER.debug("Added " + memberList + " to pool " + xpool.getPoolId() + " on " + member.getIp());
                }
            }

            if (checkOcfs2) {
                PoolOCFS2 poolocfs = new PoolOCFS2(c);
                poolocfs.discoverPoolFs();
                System.out.println("poolfsid: " + poolocfs.getPoolFsId());
                System.out.println("is in a pool: " + poolocfs.hasAPoolFs().toString());
                // ocfs2GetMetaData
            }

            if (checkCluster) {
                Pool pool = new Pool(c);
                pool.discoverServerPool();
                Cluster clos = new Cluster(c);
                if (pool.getPoolId() != null) {
                    System.out.println("Pool found: " + pool.getPoolId());
                }
                System.out.println("Cluster online: " + clos.isClusterOnline());
                System.out.println("Cluster discover: "
                        + clos.discoverCluster());

            }

            if (checkXen) {
                try {
                    Xen xen = new Xen(c);
                    Xen.Vm vm = xen.getVmConfig("s-1-VM");
                    Xen.Vm vm1 = xen.getRunningVmConfig("v-2-VM");
                    System.out.println(vm.getVmUuid() + " " + vm1.getVmUuid());
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
                String mntUuid = sp.newUuid();
                String nfsHost = "cs-mgmt";
                String nfsPath = "/volumes/cs-data/primary";
                String mountPoint = "/nfsmnt/" + mntUuid;

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

                if (sp.storagePluginMountNFS(nfsHost, nfsPath, mntUuid, mountPoint) != null) {
                    /* prep the VM disk to go to primary storage */
                    Linux vmDisk = new Linux(c);
                    String srcvmimg = repopath + "/VirtualDisks/" + imgname;
                    String dstvmimg = mountPoint + "/" + imgname;
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
                    vm.setVnc("0.0.0.0", "meh");
                    xen.createVm(repouuid, vm.getVmName());
                    xen.startVm(repouuid, vm.getVmName());
                    System.out.println("Created VM with: " + vmName);
                    System.out.println("repo: " + repouuid);
                    System.out.println("image: " + imgname);
                    System.out.println("disk: " + dstvmimg);
                    System.out.println("master: " + masterUuid);
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
                    System.out.println(ovm.getVmRootDiskPoolId());

                    /* new style */
                    vm = host.getVmConfig(vmName);
                    vm.addIso("test.iso");
                    if ("".equals(vm.getVmUuid())) {
                        System.out.println("no vm found");
                    } else {
                        System.out.println(vm.getVmParams());
                        System.out.println(vm.getVmUuid());
                        System.out.println(vm.getPrimaryPoolUuid());
                        vm.removeDisk("test.iso");
                        System.out.println(vm.getVmParams().get("disk"));
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
                System.out.println(vm.getVmVncs());
            }
            if (checkBridge) {
                Network net = new Network(c);
                for (final Map.Entry<String, Network.Interface> entry : net
                        .getInterfaceList().entrySet()) {
                    Network.Interface iface = entry.getValue();
                    System.out.println("interface: " + iface.getName()
                            + ", phys: " + iface.getPhysical() + ", type: "
                            + iface.getIfType());
                }
                String physInterface = "bond0";
                Integer vlanId = 2;
                String physVlanInt = physInterface + "." + vlanId.toString();
                String brName = "c0a80100" + "." + vlanId.toString();
                System.out.println(net.getInterfaceByName(physVlanInt) + " "
                        + net.getInterfaceByName(brName));

                if (net.getInterfaceByName(physVlanInt) == null) {
                    net.startOvsVlanConfig(physInterface, vlanId);
                }
                if (net.getInterfaceByName(brName) == null) {
                    net.startOvsBrConfig(brName, physVlanInt);
                }
            }
        } catch (Exception e) {
            System.out.println("Something went wrong, I know for sure! " + e.getMessage());
            e.printStackTrace();
        }
    }
}
