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
package com.cloud.ovm.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Test {
    public static void main(final String[] args) {
        try {
            /*Connection c = new Connection("192.168.105.155", "oracle", "password");
            Utils util = new UtilsImpl(c);
            Storage storage = new StorageImpl(c);
            String[] res = util.listDir("/etc", 1);
            for (String s : res) {
                System.out.println(s);
            }


            Pool pool = new PoolImpl(c);

            //pool.registerServer("192.168.105.155", Pool.ServerType.SITE);
            //pool.registerServer("192.168.105.155", Pool.ServerType.UTILITY);
            //pool.registerServer("192.168.105.155", Pool.ServerType.XEN);
            System.out.println("Is:" + pool.isServerRegistered());
            //String ip = pool.getMasterIp();
            //System.out.println("IP:" + ip);
            System.out.println(pool.getServerConfig());
            System.out.println(pool.getServerXmInfo());
            System.out.println(pool.getHostInfo());
            System.out.println(pool.getAgentVersion());
            String[] srs = storage.listSr();
            for (int i=0; i<srs.length; i++) {
                System.out.println(srs[i]);
            }
            String spuuid = storage.createSp(StorageType.OVSSPNFS, "192.168.110.232:/export/frank/nfs");
            System.out.println(spuuid);
            String sruuid = storage.createSr(spuuid, "hi");
            System.out.println(sruuid);
            storage.initSr();
            Pair<Long, Long> spaceInfo = storage.getSrSpaceInfo("192.168.110.232:/export/frank/nfs");
            System.out.println("Total:" + spaceInfo.first());
            System.out.println("Free:" + spaceInfo.second());*/
            final OvmVm.Details vm = new OvmVm.Details();
            vm.cpuNum = 1;
            vm.memory = 512;
            vm.name = "Test";
            vm.uuid = "This-is-a-test";
            final OvmDisk.Details rootDisk = new OvmDisk.Details();
            rootDisk.path = "/root/root.raw";
            rootDisk.type = OvmDisk.WRITE;
            vm.rootDisk = rootDisk;
            final OvmDisk.Details dataDisk = new OvmDisk.Details();
            dataDisk.path = "/tmp/data.raw";
            dataDisk.type = OvmDisk.SHAREDWRITE;
            vm.disks.add(dataDisk);
            vm.disks.add(dataDisk);
            vm.disks.add(dataDisk);
            vm.disks.add(dataDisk);
            vm.disks.add(dataDisk);
            final OvmVif.Details vif = new OvmVif.Details();
            vif.mac = "00:ff:ff:ff:ff:ee";
            vif.bridge = "xenbr0";
            vif.type = OvmVif.NETFRONT;
            vm.vifs.add(vif);
            vm.vifs.add(vif);
            vm.vifs.add(vif);
            vm.vifs.add(vif);
            vm.vifs.add(vif);
            //System.out.println(vm.toJson());
            final Connection c = new Connection("192.168.189.12", "oracle", "password");
            //System.out.println(Coder.toJson(OvmHost.getDetails(c)));

            /* This is not being used at the moment.
             * Coverity issue: 1012179
             */
            //final String txt =
            //        "{\"MasterIp\": \"192.168.189.12\", \"dom0Memory\": 790626304, \"freeMemory\": 16378757120, \"totalMemory\": 17169383424, \"cpuNum\": 4, \"agentVersion\": \"2.3-38\", \"cpuSpeed\": 2261}";

            //OvmHost.Details d = new GsonBuilder().create().fromJson(txt, OvmHost.Details.class);
            //OvmHost.Details d = Coder.fromJson(txt, OvmHost.Details.class);
            //OvmHost.Details d = OvmHost.getDetails(c);
            //System.out.println(Coder.toJson(d));
            //            OvmStoragePool.Details pool = new OvmStoragePool.Details();
            //            pool.path = "192.168.110.232:/export/frank/ovs";
            //            pool.type = OvmStoragePool.NFS;
            //            pool.uuid = "123";
            //            System.out.println(pool.toJson());

            String cmd = null;
            System.out.println(args.length);
            if (args.length >= 1) {
                cmd = args[0];
                final OvmVm.Details d = new OvmVm.Details();
                d.cpuNum = 1;
                d.memory = 512 * 1024 * 1024;
                d.name = "MyTest";
                d.uuid = "1-2-3-4-5";
                final OvmDisk.Details r = new OvmDisk.Details();
                r.path = "/var/ovs/mount/60D0985974CA425AAF5D01A1F161CC8B/running_pool/36_systemvm/System.img";
                r.type = OvmDisk.WRITE;
                d.rootDisk = r;
                final OvmVif.Details v = new OvmVif.Details();
                v.mac = "00:16:3E:5C:B1:D1";
                v.bridge = "xenbr0";
                v.type = OvmVif.NETFRONT;
                d.vifs.add(v);
                System.out.println(d.toJson());

                if (cmd.equalsIgnoreCase("create")) {
                    // String s =
                    // "{\"cpuNum\":1,\"memory\":512,\"rootDisk\":{\"type\":\"w\",\"path\":\"/var/ovs/mount/60D0985974CA425AAF5D01A1F161CC8B/running_pool/36_systemvm/System.img\"},\"disks\":[],\"vifs\":[{\"mac\":\"00:16:3E:5C:B1:D1\",\"bridge\":\"xenbr0\",\"type\":\"netfront\"}],\"name\":\"MyTest\",\"uuid\":\"1-2-3-4-5\"}";
                    OvmVm.create(c, d);
                    // c.call("OvmVm.echo", new Object[]{s});
                } else if (cmd.equalsIgnoreCase("reboot")) {
                    final Map<String, String> res = OvmVm.reboot(c, "MyTest");
                    System.out.println(res.get("vncPort"));
                    //OvmVm.stop(c, "MyTest");
                    //OvmVm.create(c, d);
                } else if (cmd.equalsIgnoreCase("stop")) {
                    OvmVm.stop(c, "MyTest");
                } else if (cmd.equalsIgnoreCase("details")) {
                    final OvmVm.Details ddd = OvmVm.getDetails(c, "MyTest");
                    System.out.println(ddd.vifs.size());
                    System.out.println(ddd.rootDisk.path);
                    System.out.println(ddd.powerState);
                } else if (cmd.equalsIgnoreCase("all")) {
                    System.out.println(OvmHost.getAllVms(c));
                } else if (cmd.equalsIgnoreCase("createBridge")) {
                    final OvmBridge.Details bd = new OvmBridge.Details();
                    bd.name = "xenbr10";
                    bd.attach = args[1];
                    OvmBridge.create(c, bd);
                } else if (cmd.equalsIgnoreCase("createVlan")) {
                    final OvmVlan.Details vd = new OvmVlan.Details();
                    vd.pif = "eth0";
                    vd.vid = 1000;
                    final String vname = OvmVlan.create(c, vd);
                    System.out.println(vname);
                } else if (cmd.equalsIgnoreCase("delVlan")) {
                    OvmVlan.delete(c, args[1]);
                } else if (cmd.equalsIgnoreCase("delBr")) {
                    OvmBridge.delete(c, args[1]);
                } else if (cmd.equalsIgnoreCase("getBrs")) {
                    final List<String> brs = OvmBridge.getAllBridges(c);
                    System.out.println(brs);
                } else if (cmd.equalsIgnoreCase("getBrDetails")) {
                    final OvmBridge.Details brd = OvmBridge.getDetails(c, args[1]);
                    System.out.println(brd.interfaces);
                }

            }

            final List<String> l = new ArrayList<String>();
            l.add("4b4d8951-f0b6-36c5-b4f3-a82ff2611c65");
            System.out.println(Coder.toJson(l));

            //            Map<String, String> res = OvmHost.getPerformanceStats(c, "xenbr0");
            //            System.out.println(res.toString());
            //            String stxt = "{\"vifs\": [{\"bridge\": \"xenbr0\", \"mac\": \"00:16:3E:5C:B1:D1\", \"type\": \"netfront\"}], \"powerState\": \"RUNNING\", \"disks\": [], \"cpuNum\": 1, \"memory\": 536870912, \"rootDisk\": {\"path\": \"/var/ovs/mount/60D0985974CA425AAF5D01A1F161CC8B/running_pool/MyTest/System.img\", \"type\": \"w\"}}";
            //            OvmVm.Details ddd = Coder.fromJson(stxt, OvmVm.Details.class);
            //            System.out.println(ddd.vifs.size());
            //            System.out.println(ddd.rootDisk.path);
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
