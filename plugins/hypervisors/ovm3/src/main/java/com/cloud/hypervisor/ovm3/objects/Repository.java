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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class Repository extends OvmObject {
    private static final Logger LOGGER = Logger.getLogger(Repository.class);
    private static final String VERSION = "Version";
    private static final String NAMETAG = "[@Name='";
    private Object postDiscovery = null;
    private Object postDbDiscovery = null;
    private Map<String, RepoDbDetails> repoDbs = new HashMap<String, RepoDbDetails>();
    private Map<String, RepoDetails> repos = new HashMap<String, RepoDetails>();
    private List<String> repoDbList = new ArrayList<String>();
    private List<String> repoList = new ArrayList<String>();

    public Repository(Connection c) {
        setClient(c);
    }

    public RepoDbDetails getRepoDb(String id) throws Ovm3ResourceException {
        if (repoDbs.containsKey(id)) {
            return repoDbs.get(id);
        }
        return null;
    }

    public List<String> getRepoDbList() throws Ovm3ResourceException {
        return repoDbList;
    }

    public RepoDetails getRepo(String id) throws Ovm3ResourceException {
        if (repos.containsKey(id)) {
            return repos.get(id);
        }
        return null;
    }

    public List<String> getRepoList() throws Ovm3ResourceException {
        return repoList;
    }

    public static class RepoDbDetails {
        private static final long serialVersionUID = 2L;
        private final Map<String, String> dbEntry = new HashMap<String, String>() {
            {
                put("Uuid", null);
                put("Fs_location", null);
                put("Mount_point", null);
                put("Filesystem_type", null);
                put(VERSION, null);
                put("Alias", null);
                put("Manager_uuid", null);
                put("Status", null);
            }
            private static final long serialVersionUID = 1L;
        };

        public RepoDbDetails() {
        }

        public void setRepoDbDetails(Map<String, String> det) {
            dbEntry.putAll(det);
        }

        public void setUuid(String id) {
            dbEntry.put("Uuid", id);
        }

        public String getStatus() {
            return dbEntry.get("Status");
        }

        public String getManagerUuid() {
            return dbEntry.get("Manager_uuid");
        }

        public String getAlias() {
            return dbEntry.get("Alias");
        }

        public String getVersion() {
            return dbEntry.get(VERSION);
        }

        public String getFilesystemType() {
            return dbEntry.get("Filesystem_type");
        }

        public String getMountPoint() {
            return dbEntry.get("Mount_point");
        }

        public String getFsLocation() {
            return dbEntry.get("Fs_location");
        }

        public String getUuid() {
            return dbEntry.get("Uuid");
        }

    }

    public static class RepoDetails {
        private List<String> templates = new ArrayList<String>();
        private List<String> virtualMachines = new ArrayList<String>();
        private List<String> virtualDisks = new ArrayList<String>();
        private List<String> isos = new ArrayList<String>();
        private final Map<String, String> dbEntry = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
            {
                put("Repository_UUID", null);
                put(VERSION, null);
                put("Repository_Alias", null);
                put("Manager_UUID", null);
            }
        };

        public RepoDetails() {
        }

        public String getManagerUuid() {
            return dbEntry.get("Manager_UUID");
        }

        public String getAlias() {
            return dbEntry.get("Repository_Alias");
        }

        public String getVersion() {
            return dbEntry.get(VERSION);
        }

        public String getUuid() {
            return dbEntry.get("Repository_UUID");
        }

        public void setRepoDetails(Map<String, String> det) {
            dbEntry.putAll(det);
        }

        public void setRepoTemplates(List<String> temp) {
            templates.addAll(temp);
        }

        public List<String> getRepoTemplates() {
            return templates;
        }

        public void setRepoVirtualMachines(List<String> vms) {
            virtualMachines.addAll(vms);
        }

        public List<String> getRepoVirtualMachines() {
            return virtualMachines;
        }

        public void setRepoVirtualDisks(List<String> disks) {
            virtualDisks.addAll(disks);
        }

        public List<String> getRepoVirtualDisks() {
            return virtualDisks;
        }

        public void setRepoISOs(List<String> isolist) {
            isos.addAll(isolist);
        }

        public List<String> getRepoISOs() {
            return isos;
        }
    }

    /*
     * delete_repository, <class 'agent.api.repository.Repository'> argument:
     * repo_uuid - default: None argument: erase - default: None
     */
    public Boolean deleteRepo(String id, Boolean erase)
            throws Ovm3ResourceException {
        Object res = callWrapper("delete_repository", id, erase);
        if (res == null) {
            return true;
        }
        return false;
    }

    /*
     * import_virtual_disk, <class 'agent.api.repository.Repository'> argument:
     * url - default: None argument: virtual_disk_id - default: None argument:
     * repo_uuid - default: None argument: option - default: None
     */
    /* should add timeout ? */
    public Boolean importVirtualDisk(String url, String vdiskid, String repoid,
            String option) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("import_virtual_disk", url, vdiskid,
                repoid, option);
    }

    public Boolean importVirtualDisk(String url, String vdiskid, String repoid)
            throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("import_virtual_disk", url, vdiskid,
                repoid);
    }

    /*
     * discover_repositories, <class 'agent.api.repository.Repository'>
     * argument: args - default: None
     */
    /*
     * args are repo ids <Discover_Repositories_Result> <RepositoryList/>
     * </Discover_Repositories_Result>
     */
    public Boolean discoverRepo(String id) throws Ovm3ResourceException {
        postDiscovery = callWrapper("discover_repositories", id);
        if (postDiscovery == null) {
            return false;
        }
        Document xmlDocument = prepParse((String) postDiscovery);
        String path = "//Discover_Repositories_Result/RepositoryList/Repository";
        repoList = new ArrayList<String>();
        repoList.addAll(xmlToList(path + "/@Name", xmlDocument));
        for (String name : repoList) {
            RepoDetails repo = new RepoDetails();
            repo.setRepoTemplates(xmlToList(path + NAMETAG + id
                    + "']/Templates/Template/File", xmlDocument));
            repo.setRepoVirtualMachines(xmlToList(path + NAMETAG + id
                    + "']/VirtualMachines/VirtualMachine/@Name", xmlDocument));
            repo.setRepoVirtualDisks(xmlToList(path + NAMETAG + name
                    + "']/VirtualDisks/Disk", xmlDocument));
            repo.setRepoISOs(xmlToList(
                    path + NAMETAG + name + "']/ISOs/ISO", xmlDocument));
            Map<String, String> details = xmlToMap(path + NAMETAG + name
                    + "']", xmlDocument);
            repo.setRepoDetails(details);
            repos.put(name, repo);
        }
        return true;
    }

    /*
     * add_repository, <class 'agent.api.repository.Repository'> argument:
     * fs_location - default: None argument: mount_point - default: None
     */
    public Boolean addRepo(String remote, String local)
            throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("add_repository", remote, local);
    }

    /**
     * is the same as discoverRepoDb in principle (takes an id or mountpoint)
     * get_repository_meta_data, <class 'agent.api.repository.Repository'>
     * argument: repo_mount_point - default: None
     */

    /*
     * mount_repository_fs, <class 'agent.api.repository.Repository'> argument:
     * fs_location - default: None argument: mount_point - default: None
     */
    public Boolean mountRepoFs(String remote, String local)
            throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("mount_repository_fs", remote, local);
    }

    /*
     * unmount_repository_fs, <class 'agent.api.repository.Repository'>
     * argument: mount_point - default: None
     */
    public Boolean unmountRepoFs(String local) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("unmount_repository_fs", local);
    }

    /*
     * create_repository, <class 'agent.api.repository.Repository'> argument:
     * fs_location - default: None argument: mount_point - default: None
     * argument: repo_uuid - default: None argument: repo_alias - default: None
     */
    public Boolean createRepo(String remote, String local, String repoid,
            String repoalias) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("create_repository", remote, local,
                repoid, repoalias);
    }

    /*
     * discover_repository_db, <class 'agent.api.repository.Repository'>
     * <Discover_Repository_Db_Result> <RepositoryDbList> <Repository
     * Uuid="0004fb0000030000aeaca859e4a8f8c0">
     * <Fs_location>cs-mgmt:/volumes/cs-data/primary</Fs_location>
     * <Mount_point>/
     * OVS/Repositories/0004fb0000030000aeaca859e4a8f8c0</Mount_point>
     * <Filesystem_type>nfs</Filesystem_type> <Version>3.0</Version>
     * <Alias>MyRepo</Alias>
     * <Manager_uuid>0004fb00000100000af70d20dcce7d65</Manager_uuid>
     * <Status>Unmounted</Status> </Repository> <Repository> ... </Repository>
     * </RepositoryDbList> </Discover_Repository_Db_Result>
     */
    public Boolean discoverRepoDb() throws Ovm3ResourceException {
        postDbDiscovery = callWrapper("discover_repository_db");
        Document xmlDocument = prepParse((String) postDbDiscovery);
        String path = "//Discover_Repository_Db_Result/RepositoryDbList/Repository";
        repoDbList = new ArrayList<String>();
        repoDbList.addAll(xmlToList(path + "/@Uuid", xmlDocument));
        for (String id : repoDbList) {
            RepoDbDetails repoDb = new RepoDbDetails();
            Map<String, String> rep = xmlToMap(path + "[@Uuid='" + id + "']",
                    xmlDocument);
            repoDb.setRepoDbDetails(rep);
            repoDb.setUuid(id);
            repoDbs.put(id, repoDb);
        }
        return true;
    }

}
