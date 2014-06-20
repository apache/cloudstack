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

// import java.io.File;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import java.util.Map.Entry;

// mport org.apache.commons.io.FileUtils;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.hypervisor.ovm3.object.Linux.FileSystem;

public class Test {
    // private static Sanitize _insane = new Sanitize();
    /*
     * Other trinket
     * https://192.168.1.51:7002/ovm/core/OVMManagerCoreServlet&c=1
     * &s=-1&lb=p&t=2
     * &p=1dd5e891d9d0edbd81c2a69ab3d1b7ea%2C2f3b7fca202045058ae388d22f21f508'
     */
    public static void main(final String[] args) throws Exception {
        boolean CheckNet = false;
        boolean CheckNtp = false;
        boolean CheckLinux = false;
        boolean CheckCommon = false;
        boolean CheckCluster = false;
        boolean CheckRepo = false;
        boolean CheckPool = false;
        boolean CheckOcfs2 = false;
        boolean CheckNFSPlugin = false;
        boolean CheckXen = false;
        boolean CheckVnc = false;

        boolean CheckCombine = false;
        boolean CheckVmInfo = false;
        boolean CheckUuid = false;
        boolean CheckBridge = false;
        boolean CheckFs = false;
        boolean CheckPlugin = true;


        try {
            Connection c;
            Socket client;
            String hostname = "ovm-2";
            try{
                client = new Socket(hostname, 8899);
            } catch (Exception e) {
                hostname = "localhost";
                client = new Socket(hostname, 8899);
            }
            if (!client.isConnected()) {
                System.out.println("No connection");
            } else {
                System.out.println("connected to: " + hostname);
            }
            client.close();
            try {
                System.out.println("trying to connect to " + hostname);
                c = new Connection(hostname, 8899, "oracle", "test123");
            } catch (Exception e) {
                throw new Exception("Unable to connect to " + hostname
                       + " port seemed to listen...");
            }
            /*
             * needs to be finished and implement ovs + bridge, or do we count
             * on chef ?
             */
            if (CheckPlugin) {
                CloudStackPlugin csp = new CloudStackPlugin(c);
                try {
                    System.out.println(csp.ovsUploadSshKey("test", "testing 123"));
//                            FileUtils.readFileToString(new File(""))));
                    String ip = "169.254.1.202";
                    String domain = "i-2-29-VM";
                    String pubnic = "bond0";
                    // System.out.println(csp.ovsDom0Stats(pubnic));
                    // System.out.println(csp.domrCheckPort(ip, 3922, 3, 3));
                    // System.out.println(csp.domrCheckSsh(ip));
                    System.out.println("vnc Port: "+ csp.getVncPort(domain));
                    // System.out.println(csp.domrExec(ip, "ls -l").getStdOut());
                    Map<String, String> stats = csp.ovsDomUStats(domain);
                    /* for (final Entry<String, String> stat : stats.entrySet()) {
                        System.out.println(stat.getKey() + " "
                                + Double.parseDouble(stat.getValue()));
                    } */
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
                    Double cpupct = d_cpu/d_time * 100 * cpus;
                    System.out.println(cpupct);
                } catch (Exception e) {
                    System.out.println("nooooo!!!" + e.getMessage());
                    throw new Exception(e.getMessage());
                }
            }
            if (CheckFs) {
                Linux host = new Linux(c);

                Map<String, Linux.FileSystem> fsList = host
                        .getFileSystemList("nfs");
                Linux.FileSystem fs = fsList.get("nfs");
                System.out.println(fs + " " + fsList);
            }
            if (CheckUuid) {
                System.out.println(UUID.nameUUIDFromBytes(("test@test-test")
                        .getBytes()));
            }
            if (CheckNet) {
                Network net = new Network(c);
                System.out.println(net.getInterfaceByName("c0a80100"));
                // net.discoverNetwork();
                System.out
                        .println(net.getInterfaceByName("c0a80100").getAddress());
                System.out.println(net.getInterfaceByIp("192.168.1.65").getName());
                // System.out.println(bridge.getMac());
            }
            if (CheckCommon == true) {
                Common Com = new Common(c);
                String x = Com.getApiVersion();
                System.out.println("Api Version: " + x);
                String y = Com.sleep(1);
                System.out.println("Sleep: " + y);
                String msg = Com.echo("testing 1 2 3");
                System.out.println("Echo: " + msg);
                /*
                 * String disp = Com.dispatch ("192.168.1.60", "hoeleboele");
                 * System.out. println("dispatch" + disp);
                 */
            }
            /* check stuff */
            if (CheckLinux == true) {
                Linux Host = new Linux(c);
                Host.discoverHardware();
                Host.discoverServer();
                System.out.println("hwVMM: " + Host.hwVMM.toString());
                System.out.println("hwSystem: " + Host.hwSystem.toString());
                System.out.println("Cap: " + Host.Capabilities.toString());
                System.out.println("VMM: " + Host.VMM.toString());
                System.out.println("NTP: " + Host.NTP.toString());
                System.out.println("DT: " + Host.DateTime.toString());
                System.out.println("Gen: " + Host.Generic.toString());
                System.out.println("time; " + Host.getDateTime());
                // needs to be within bounds of 1970... *grin*
                System.out.println("update time to 1999: "
                        + Host.setDateTime(1999, 12, 31, 12, 0, 0));
                System.out.println("lastboot: " + Host.getLastBootTime());
                System.out.println("time: " + Host.localTime);
                Calendar now = Calendar.getInstance();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH); // Note: zero based!
                int day = now.get(Calendar.DAY_OF_MONTH);
                int hour = now.get(Calendar.HOUR_OF_DAY);
                int minute = now.get(Calendar.MINUTE);
                int second = now.get(Calendar.SECOND);
                int millis = now.get(Calendar.MILLISECOND);
                System.out.println("set time to now: "
                        + Host.setDateTime(year, month, day, hour, minute,
                                second));
                System.out.println("lastboot: " + Host.getLastBootTime());
                System.out.println("time: " + Host.localTime);
                System.out.println("update password: "
                        + Host.updateAgentPassword("oracle", "test123"));
                System.out.println("set time zone: "
                        + Host.setTimeZone("Europe/London", false));
                System.out.println("time zone: " + Host.getTimeZone() + ", "
                        + Host.timeZone + ", " + Host.timeUTC);
                System.out.println("set time zone: "
                        + Host.setTimeZone("Europe/Amsterdam", true));
                System.out.println("time zone: " + Host.getTimeZone() + ", "
                        + Host.timeZone + ", " + Host.timeUTC);
                // System.out.println("Luns: " + Host.discoverPhysicalLuns());

            }

            /* setting up ntp */
            if (CheckNtp == true) {
                Ntp ntp = new Ntp(c);
                ntp.getDetails();
                System.out.println("ntp isServer: " + ntp.isServer());
                System.out.println("ntp isRunning: " + ntp.isRunning());
                System.out.println("ntp Servers: " + ntp.servers());
                ntp.addServer("192.168.1.1");
                ntp.addServer("192.168.1.61");
                System.out.println("ntp set: " + ntp.setNtp(true));
                System.out.println("ntp enable: " + ntp.enableNtp());
                ntp.getDetails();
                System.out.println("ntp isServer: " + ntp.isServer());
                System.out.println("ntp isRunning: " + ntp.isRunning());
                System.out.println("ntp Servers: " + ntp.servers());
                System.out.println("ntp disable: " + ntp.disableNtp());
                System.out.println("ntp reset: " + ntp.setNtp("", false));
            }

            if (CheckNFSPlugin == true) {
                Linux lin = new Linux(c);
                lin.discoverServer();
                System.out.println(lin.getCapabilities());
                Map<String, FileSystem> fsList = lin.getFileSystemList("nfs");
                System.out.println(fsList);
                System.out.println(BigInteger.valueOf(lin.getMemory()
                        .longValue()));
                System.out.println(lin.getFreeMemory());
                BigInteger totalmem = BigInteger.valueOf(lin.getMemory()
                        .longValue());
                BigInteger freemem = BigInteger.valueOf(lin.getFreeMemory()
                        .longValue());
                System.out.println(totalmem.subtract(freemem));
                /*
                 * for (final Map.Entry<String, Linux.FileSystem> entry : fsList
                 * .entrySet()) {
                 * Linux.FileSystem fs = entry.getValue();
                 * StoragePlugin sp = new StoragePlugin(c);
                 * String propUuid = sp.deDash(fs.getUuid());
                 * String mntUuid = fs.getUuid();
                 * String fsType = "FileSys";
                 * sp.setUuid(propUuid);
                 * sp.setSsUuid(propUuid);
                 * sp.setName(propUuid);
                 * sp.setFsType(fsType);
                 * sp.setFsServer(fs.getHost());
                 * sp.setFsSourcePath(fs.getDevice());
                 * sp.storagePluginGetFileSystemInfo();
                 * }
                 */
                /*
                 * StoragePlugin sp = new StoragePlugin(c);
                 * String propUuid = sp.deDash(sp.newUuid());
                 * String mntUuid = sp.newUuid();
                 * String nfsHost = "cs-mgmt";
                 * String nfsPath = "/volumes/cs-data/primary";
                 * String fsType = "FileSys";
                 * sp.setUuid(propUuid);
                 * sp.setName(propUuid);
                 * sp.setFsType(fsType);
                 * sp.setFsServer(nfsHost);
                 * sp.setFsSourcePath(nfsHost + ":" + nfsPath);
                 * // sp.fsTarget("/nfsmnt/" + mntUuid);
                 * sp.setFsMountPoint("/nfsmnt/" + mntUuid);
                 * sp.setMntUuid(mntUuid);
                 * sp.setSsUuid(propUuid);
                 * sp.setSsName("nfs:" + nfsPath);
                 * if (sp.storagePluginMount() != null) {
                 * lin.discoverMountedFs("nfs");
                 * // System.out.println(sp.extprops);
                 * StoragePlugin store = new StoragePlugin(c);
                 * store.setUuid(propUuid);
                 * store.setSsUuid(propUuid);
                 * store.setMntUuid(mntUuid);
                 * store.setFsHost(nfsHost);
                 * store.setFsSourcePath(nfsHost + ":" + nfsPath);
                 * // store.setFsMountPoint(pool.getPath());
                 * store.storagePluginGetFileSystemInfo();
                 * System.out.println(store.getTotalSize());
                 * sp.setFsSourcePath(nfsHost + ":" + nfsPath);
                 * sp.storagePluginUnmount();
                 * }
                 */
            }

            /* still needs to be finished! */
            if (CheckRepo == true) {
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

            if (CheckPool == true) {
                System.out.println("checking pool");
                Pool pool = new Pool(c);
                pool.discoverServerPool();
                System.out.println(pool.getPoolAlias());
                System.out.println(pool.getPoolId());
                if (pool.getPoolId().contentEquals("TEST")) {
                    System.out.println("pool equals test");
                } else {
                    System.out.println("pool" + pool.getPoolId());
                }
                List<String> ips = new ArrayList<String>();
                ips.add("192.168.1.64");
                ips.add("192.168.1.65");
                /*
                 * pool.setPoolIps(ips);
                 * pool.setPoolMemberIpList();
                 */
                /*
                 * if (pool.poolFsId != null) {
                 * pool.leaveServerPool(pool.poolFsId);
                 * }
                 */
                System.out.println("pool members: "
                        + pool.getPoolMemberIpList());
            }

            if (CheckOcfs2 == true) {
                PoolOCFS2 poolocfs = new PoolOCFS2(c);
                poolocfs.discoverPoolFs();
                // poolocfs.ocfs2GetMetaData();
            }

            if (CheckCluster == true) {
                Pool pool = new Pool(c);
                pool.discoverServerPool();
                Cluster Clos = new Cluster(c);
                // Clos.destroyCluster(pool.poolFsId);
                if (pool.getPoolId() != null) {
                    // Clos.deconfigureServerForCluster(pool.poolId);
                }
                System.out.println("Cluster online: " + Clos.isClusterOnline());
                System.out.println("Cluster discover: "
                        + Clos.discoverCluster());

            }

            if (CheckXen == true) {
                Xen xen = new Xen(c);
                xen.listVms();
                xen.createVm("xx", "xx");
                /* xen.deleteVm(repoId, vmId); */
            }

            /* check the combination of stuff */
            if (CheckCombine == true) {
                /* prepare host, mgr should have "steady uuid" */
                OvmObject Go = new OvmObject();
                String masterUuid = Go.deDash(Go.newUuid());

                /* check capabilities */
                Linux Host = new Linux(c);
                Host.discoverServer();
                /* setup pool and role, needs utility to be able to do shit */
                Pool pool = new Pool(c);

                /* Info comes from Linux, not the pool, but needs to be set in the pool -sigh- */
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
                    /* the "solving" of no real primary and secondary storage in OVS */
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
                    xen.createVm(repouuid, vm.vmName);
                    xen.startVm(repouuid, vm.vmName);
                    /*
                     * vm.stopVm(repouuid, vm.vmUuid); vm.deleteVm(repouuid,
                     * vm.vmUuid);
                     */
                    System.out.println("Created VM with: " + vmName);
                    System.out.println("repo: " + repouuid);
                    System.out.println("image: " + imgname);
                    System.out.println("disk: " + dstvmimg);
                    System.out.println("master: " + masterUuid);
                }
            }
            if (CheckVmInfo == true) {
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
                    System.out.println(ovm.getVmRootDiskPoolId());
                    /* new style */
                    vm = host.getVmConfig(vmName);
                    vm.addIso("test.iso");
                    if (vm.getVmUuid().equals("")) {
                        System.out.println("no vm found");
                    } else {
                        System.out.println(vm.getVmParams());
                        System.out.println(vm.getVmDisks());
                        System.out.println(vm.getVmUuid());
                        System.out.println(vm.getPrimaryPoolUuid());
                        vm.removeDisk("test.iso");
                        System.out.println(vm.getVmParams().get("disk"));
                    }

                } catch (XmlRpcException e) {
                    System.out.println("Failed to get VM details for " + vmName
                            + " on " + c.getIp());
                }
            }
            if (CheckVnc == true) {
                Xen vms = new Xen(c);
                Xen.Vm vm = vms.listVms().get("Domain-0");
                vm.setVncAddress("0.0.0.0");
                vm.setVncPassword("testikkel");
                vm.setVnc();
                System.out.println(vm._vmVnc + " " + vm.vmVnc);
            }
            if (CheckBridge) {
                Network net = new Network(c);
                for (final Map.Entry<String, Network.Interface> entry : net.getInterfaceList().entrySet()) {
                    Network.Interface iface = entry.getValue();
                    System.out.println("interface: " + iface.getName() + ", phys: " + iface.getPhysical() + ", type: " + iface.getIfType());
                }
                String physInterface = "bond0";
                Integer vlanId = 2;
                String physVlanInt = physInterface + "." + vlanId.toString();
                String brName = "c0a80100" + "." + vlanId.toString();
                System.out.println(net.getInterfaceByName(physVlanInt)
                        + " " + net.getInterfaceByName(brName));

                if (net.getInterfaceByName(physVlanInt) == null)
                    net.startOvsVlanConfig(physInterface, vlanId);

                if (net.getInterfaceByName(brName) == null)
                    net.startOvsBrConfig(brName, physVlanInt);

                // net.startOvsLocalConfig("control0");
                // net.ovsBrConfig("start", "control0", "lo");
                // net.ovsIpConfig("control0", "static", "169.254.0.1",
                // "255.255.0.0");
                // execOverSsh("route del -net 169.254.0.0/16");
            }
                /* cleanup */
                /*
                 * repo.deleteRepo(repouuid, true);
                 * repo.unmountRepoFs(repopath); repo.discoverRepoDb();
                 * repo.discoverRepo(repouuid); sp.storagePluginUnmount();
                 */

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.getMessage();
        }
    }
}
