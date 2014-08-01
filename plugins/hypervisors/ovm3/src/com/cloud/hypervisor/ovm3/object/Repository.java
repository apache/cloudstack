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

import java.util.Map;


/*
 * should become an interface implementation
 */
public class Repository extends OvmObject {

    public Repository(Connection c) {
        setClient(c);
    }

    /*
     * delete_repository, <class 'agent.api.repository.Repository'> argument:
     * repo_uuid - default: None argument: erase - default: None
     */
    public Boolean deleteRepo(String id, Boolean erase) throws Ovm3ResourceException {
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
        return nullIsTrueCallWrapper("import_virtual_disk", url, vdiskid, repoid,
                option);
    }

    public Boolean importVirtualDisk(String url, String vdiskid, String repoid)
            throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("import_virtual_disk", url, vdiskid, repoid);
    }

    /*
     * discover_repositories, <class 'agent.api.repository.Repository'>
     * argument: args - default: None
     */
    /*
     * args are repo ids <Discover_Repositories_Result> <RepositoryList/>
     * </Discover_Repositories_Result>
     */
    public Boolean discoverRepo(Map<String, String> args)
            throws Ovm3ResourceException {
        Object x = callWrapper("discover_repositories", args);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean discoverRepo(String id) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("discover_repositories", id);
    }

    /*
     * add_repository, <class 'agent.api.repository.Repository'> argument:
     * fs_location - default: None argument: mount_point - default: None
     */
    public Boolean addRepo(String remote, String local) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("add_repository", remote, local);
    }

    /*
     * get_repository_meta_data, <class 'agent.api.repository.Repository'>
     * argument: repo_mount_point - default: None
     */
    public Boolean getRepoMetaData(String local) throws Ovm3ResourceException {
        Object x = callWrapper("get_repository_meta_data", local);
        if (x == null) {
            return true;
        }
        return false;
    }

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
        return nullIsTrueCallWrapper("create_repository", remote, local, repoid,
                repoalias);
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
        Object x = callWrapper("discover_repository_db");
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * edit_repository_db, <class 'agent.api.repository.Repository'> argument:
     * repo_uuid - default: None argument: db_changes - default: None
     */
    public Boolean editRepoDb(String repoId, Map<String, String> changes)
            throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("edit_repository_db", repoId, changes);
    }

    /*
     * import_ISO, <class 'agent.api.repository.Repository'> argument: url -
     * default: None argument: iso_id - default: None argument: repo_uuid -
     * default: None argument: option - default: None ?> it throws an error when
     * you add this...
     */
    /*
     * is async, need to find a way to do something with that.... add timeout
     * too
     */
    public Boolean importIso(String url, String isoId, String repoId,
            String option) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("import_ISO", url, isoId, repoId);
    }
}
