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

import org.junit.Test;

public class RepositoryTest {
    ConnectionTest con = new ConnectionTest();
    Repository repo = new Repository(con);
    XmlTestResultTest results = new XmlTestResultTest();

    private String REPOID = "f12842ebf5ed3fe78da1eb0e17f5ede8";
    private String MGRID = "d1a749d4295041fb99854f52ea4dea97";
    private String REPOALIAS = "OVS Repository";
    private String REMOTENFS = "cs-mgmt:/volumes/cs-data/primary/ovm";
    private String LOCALMOUNT = "/OVS/Repositories/f12842ebf5ed3fe78da1eb0e17f5ede8";
    private String REPOVERSION = "3.0";
    private String REPOSTATUS = "Mounted";
    private String ISO = "systemvm.iso";
    private String TEMPLATE = "0b53acb1-6554-41b8-b1e1-7b27a01b6acb";
    private String DISK = "cfb5ee12-b99d-41d1-a5b2-44dea36c9de1.raw";
    private String VM = "7efbdbe0-3d01-3b22-a8a0-1d41c6f2502f";
    private String REPODISCOVERXML = "<string>"
            + "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Repositories_Result&gt;" + "&lt;RepositoryList&gt;"
            + "&lt;Repository Name=\""
            + REPOID
            + "\"&gt;"
            + "&lt;Version&gt;"
            + REPOVERSION
            + "&lt;/Version&gt;"
            + "&lt;Manager_UUID&gt;"
            + MGRID
            + "&lt;/Manager_UUID&gt;"
            + "&lt;Repository_UUID&gt;"
            + REPOID
            + "&lt;/Repository_UUID&gt;"
            + "&lt;Repository_Alias&gt;"
            + REPOALIAS
            + "&lt;/Repository_Alias&gt;"
            + "&lt;Assemblies/&gt;"
            + "&lt;Templates&gt;"
            + "&lt;Template Name=\""
            + TEMPLATE
            + "\"&gt;"
            + "&lt;File&gt;"
            + TEMPLATE
            + ".raw&lt;/File&gt;"
            + "&lt;/Template&gt;"
            + "&lt;Template Name=\"7d5c29db-6343-4431-b509-9646b45cc31b\"&gt;"
            + "&lt;File&gt;7d5c29db-6343-4431-b509-9646b45cc31b.raw&lt;/File&gt;"
            + "&lt;/Template&gt;"
            + "&lt;/Templates&gt;"
            + "&lt;VirtualMachines&gt;"
            + "&lt;VirtualMachine Name=\""
            + VM
            + "\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"pool\"&gt;"
            + "&lt;File&gt;ovspoolfs.img&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"4fb50e31-b032-3012-b70b-7e27c2acdfe2\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"5ffe3ddc-606d-3fec-a956-67638f7ba838\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;VirtualMachine Name=\"14fc3846-45e5-3c08-ad23-432ceb07407b\"&gt;"
            + "&lt;File&gt;vm.cfg&lt;/File&gt;"
            + "&lt;/VirtualMachine&gt;"
            + "&lt;/VirtualMachines&gt;"
            + "&lt;VirtualDisks&gt;"
            + "&lt;Disk&gt;"
            + DISK
            + "&lt;/Disk&gt;"
            + "&lt;Disk&gt;b92dd5c5-0f24-4cca-b86a-153ac6e950a8.raw&lt;/Disk&gt;"
            + "&lt;Disk&gt;d144c278-0825-41d5-b9c6-8bb21f3dd1e7.raw&lt;/Disk&gt;"
            + "&lt;Disk&gt;99650d9f-c7ee-42df-92e3-6cafc0876141.raw&lt;/Disk&gt;"
            + "&lt;/VirtualDisks&gt;"
            + "&lt;ISOs&gt;"
            + "&lt;ISO&gt;"
            + ISO
            + "&lt;/ISO&gt;"
            + "&lt;/ISOs&gt;"
            + "&lt;/Repository&gt;"
            + "&lt;/RepositoryList&gt;"
            + "&lt;/Discover_Repositories_Result&gt;" + "</string>";
    private String REPODBDISCOVERXML = "<string>"
            + "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Repository_Db_Result&gt;"
            + "&lt;RepositoryDbList&gt;" + "&lt;Repository Uuid=\""
            + REPOID
            + "\"&gt;"
            + "&lt;Fs_location&gt;"
            + REMOTENFS
            + "&lt;/Fs_location&gt;"
            + "&lt;Mount_point&gt;"
            + LOCALMOUNT
            + "&lt;/Mount_point&gt;"
            + "&lt;Filesystem_type&gt;nfs&lt;/Filesystem_type&gt;"
            + "&lt;Version&gt;"
            + REPOVERSION
            + "&lt;/Version&gt;"
            + "&lt;Alias&gt;"
            + REPOALIAS
            + "&lt;/Alias&gt;"
            + "&lt;Manager_uuid&gt;"
            + MGRID
            + "&lt;/Manager_uuid&gt;"
            + "&lt;Status&gt;"
            + REPOSTATUS
            + "&lt;/Status&gt;"
            + "&lt;/Repository&gt;"
            + "&lt;/RepositoryDbList&gt;"
            + "&lt;/Discover_Repository_Db_Result&gt;" + "</string>";

    /*
     * @Test Create a test repo that mimics the above so we can test against a
     * live box... public void testCreateRepo() throws Ovm3ResourceException {
     * con.setResult(null); repo.createRepo(REMOTENFS, LOCALMOUNT, REPOID,
     * REPOALIAS); }
     */
    @Test
    public void testDiscoverRepoBase() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(this.REPODISCOVERXML));
        repo.discoverRepo(REPOID);
        results.basicStringTest(repo.getRepo(REPOID).getUuid(), REPOID);
        results.basicStringTest(repo.getRepo(REPOID).getAlias(), REPOALIAS);
        results.basicStringTest(repo.getRepo(REPOID).getVersion(), REPOVERSION);
        results.basicStringTest(repo.getRepo(REPOID).getManagerUuid(), MGRID);
    }

    @Test
    public void testDiscoverRepoDetails() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(this.REPODISCOVERXML));
        repo.discoverRepo(REPOID);
        results.basicBooleanTest(results.basicListHasString(repo
                .getRepo(REPOID).getRepoVirtualDisks(), DISK), true);
        results.basicBooleanTest(results.basicListHasString(repo
                .getRepo(REPOID).getRepoVirtualMachines(), VM), true);
        results.basicBooleanTest(results.basicListHasString(repo
                .getRepo(REPOID).getRepoTemplates(), TEMPLATE + ".raw"), true);
        results.basicBooleanTest(results.basicListHasString(repo
                .getRepo(REPOID).getRepoISOs(), ISO), true);
        results.basicBooleanTest(results.basicListHasString(repo
                .getRepo(REPOID).getRepoISOs(), VM), false);
    }

    @Test
    public void testDiscoverRepoDb() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(this.REPODBDISCOVERXML));
        repo.discoverRepoDb();
        results.basicStringTest(repo.getRepoDb(REPOID).getUuid(), REPOID);
        results.basicStringTest(repo.getRepoDb(REPOID).getAlias(), REPOALIAS);
        results.basicStringTest(repo.getRepoDb(REPOID).getVersion(),
                REPOVERSION);
        results.basicStringTest(repo.getRepoDb(REPOID).getManagerUuid(), MGRID);
        results.basicStringTest(repo.getRepoDb(REPOID).getMountPoint(),
                LOCALMOUNT);
        results.basicStringTest(repo.getRepoDb(REPOID).getFsLocation(),
                REMOTENFS);
        results.basicStringTest(repo.getRepoDb(REPOID).getStatus(), REPOSTATUS);
        results.basicStringTest(repo.getRepoDb(REPOID).getFilesystemType(),
                "nfs");
    }
}
