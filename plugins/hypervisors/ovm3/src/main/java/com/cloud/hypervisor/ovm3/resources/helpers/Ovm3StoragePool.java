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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.Pool;
import com.cloud.hypervisor.ovm3.objects.PoolOCFS2;
import com.cloud.hypervisor.ovm3.objects.Repository;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.FileProperties;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.StorageDetails;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SshHelper;

public class Ovm3StoragePool {
    private static final Logger LOGGER = Logger
            .getLogger(Ovm3StoragePool.class);
    private Connection c;
    private Ovm3Configuration config;
    private OvmObject ovmObject = new OvmObject();

    public Ovm3StoragePool(Connection conn, Ovm3Configuration ovm3config) {
        c = conn;
        config = ovm3config;
    }

    /**
     * Setting up the roles on a host, we set all roles on all hosts!
     *
     * @param pool
     * @throws ConfigurationException
     */
    private void setRoles(Pool pool) throws ConfigurationException {
        try {
            pool.setServerRoles(pool.getValidRoles());
        } catch (Ovm3ResourceException e) {
            String msg = "Failed to set server role for host "
                    + config.getAgentHostname() + ": " + e.getMessage();
            LOGGER.error(msg);
            throw new ConfigurationException(msg);
        }
    }

    /**
     * If you don't own the host you can't fiddle with it.
     *
     * @param pool
     * @throws ConfigurationException
     */
    private void takeOwnership(Pool pool) throws ConfigurationException {
        try {
            LOGGER.debug("Take ownership of host " + config.getAgentHostname());
            pool.takeOwnership(config.getAgentOwnedByUuid(), "");
        } catch (Ovm3ResourceException e) {
            String msg = "Failed to take ownership of host "
                    + config.getAgentHostname();
            LOGGER.error(msg);
            throw new ConfigurationException(msg);
        }
    }

    /**
     * If you don't own the host you can't fiddle with it.
     *
     * @param pool
     * @throws ConfigurationException
     */
    /* FIXME: Placeholders for now, implement later!!!! */
    private void takeOwnership33x(Pool pool) throws ConfigurationException {
        try {
            LOGGER.debug("Take ownership of host " + config.getAgentHostname());
            String event = "http://localhost:10024/event";
            String stats = "http://localhost:10024/stats";
            String mgrCert = "None";
            String signCert = "None";
            pool.takeOwnership33x(config.getAgentOwnedByUuid(),
                    event,
                    stats,
                    mgrCert,
                    signCert);
        } catch (Ovm3ResourceException e) {
            String msg = "Failed to take ownership of host "
                    + config.getAgentHostname();
            LOGGER.error(msg);
            throw new ConfigurationException(msg);
        }
    }
    /**
     * Prepare a host to become part of a pool, the roles and ownership are
     * important here.
     *
     * @return
     * @throws ConfigurationException
     */
    public boolean prepareForPool() throws ConfigurationException {
        /* need single primary uuid */
        try {
            Linux host = new Linux(c);
            Pool pool = new Pool(c);

            /* setup pool and role, needs utility to be able to do things */
            if (host.getServerRoles().contentEquals(
                    pool.getValidRoles().toString())) {
                LOGGER.info("Server role for host " + config.getAgentHostname()
                        + " is ok");
            } else {
                setRoles(pool);
            }
            if (host.getMembershipState().contentEquals("Unowned")) {
                if (host.getOvmVersion().startsWith("3.2.")) {
                    takeOwnership(pool);
                } else if (host.getOvmVersion().startsWith("3.3.")) {
                    takeOwnership33x(pool);
                }
            } else {
                if (host.getManagerUuid().equals(config.getAgentOwnedByUuid())) {
                    String msg = "Host " + config.getAgentHostname()
                            + " owned by us";
                    LOGGER.debug(msg);
                    return true;
                } else {
                    String msg = "Host " + config.getAgentHostname()
                            + " already part of a pool, and not owned by us";
                    LOGGER.error(msg);
                    throw new ConfigurationException(msg);
                }
            }
        } catch (ConfigurationException | Ovm3ResourceException es) {
            String msg = "Failed to prepare " + config.getAgentHostname()
                    + " for pool: " + es.getMessage();
            LOGGER.error(msg);
            throw new ConfigurationException(msg);
        }
        return true;
    }

    /**
     * Setup a pool in general, this creates a repo if it doesn't exist yet, if
     * it does however we mount it.
     *
     * @param cmd
     * @return
     * @throws Ovm3ResourceException
     */
    private Boolean setupPool(StorageFilerTO cmd) throws Ovm3ResourceException {
        String primUuid = cmd.getUuid();
        String ssUuid = ovmObject.deDash(primUuid);
        String fsType = "nfs";
        String clusterUuid = config.getAgentOwnedByUuid().substring(0, 15);
        String managerId = config.getAgentOwnedByUuid();
        String poolAlias = cmd.getHost() + ":" + cmd.getPath();
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath())
                + "/VirtualMachines";
        Integer poolSize = 0;

        Pool poolHost = new Pool(c);
        PoolOCFS2 poolFs = new PoolOCFS2(c);
        if (config.getAgentIsPrimary()) {
            try {
                LOGGER.debug("Create poolfs on " + config.getAgentHostname()
                        + " for repo " + primUuid);
                /* double check if we're not overwritting anything here!@ */
                poolFs.createPoolFs(fsType, mountPoint, clusterUuid, primUuid,
                        ssUuid, managerId);
            } catch (Ovm3ResourceException e) {
                throw e;
            }
            try {
                poolHost.createServerPool(poolAlias, primUuid,
                        config.getOvm3PoolVip(), poolSize + 1,
                        config.getAgentHostname(), c.getIp());
            } catch (Ovm3ResourceException e) {
                throw e;
            }
        } else if (config.getAgentHasPrimary()) {
            try {
                poolHost.joinServerPool(poolAlias, primUuid,
                        config.getOvm3PoolVip(), poolSize + 1,
                        config.getAgentHostname(), c.getIp());
            } catch (Ovm3ResourceException e) {
                throw e;
            }
        }
        try {
            /* should contain check if we're in an OVM pool or not */
            CloudstackPlugin csp = new CloudstackPlugin(c);
            Boolean vip = csp.dom0CheckPort(config.getOvm3PoolVip(), 22, 60, 1);
            if (!vip) {
                throw new Ovm3ResourceException(
                        "Unable to reach Ovm3 Pool VIP "
                                + config.getOvm3PoolVip());
            }
            /*
             * should also throw exception, we need to stop pool creation here,
             * or is the manual addition fine?
             */
            if (!addMembers()) {
                return false;
            }
        } catch (Ovm3ResourceException e) {
            throw new Ovm3ResourceException("Unable to add members to pool"
                    + e.getMessage());
        }
        return true;
    }

    /**
     * Adding members to a pool, this is seperate from cluster configuration in
     * OVM.
     *
     * @return
     * @throws Ovm3ResourceException
     */
    private Boolean addMembers() throws Ovm3ResourceException {
        List<String> members = new ArrayList<String>();
        try {
            Connection m = new Connection(config.getOvm3PoolVip(), c.getPort(),
                    c.getUserName(), c.getPassword());
            Pool poolPrimary = new Pool(m);
            if (poolPrimary.isInAPool()) {
                members.addAll(poolPrimary.getPoolMemberList());
                if (!poolPrimary.getPoolMemberList().contains(c.getIp())
                        && c.getIp().equals(config.getOvm3PoolVip())) {
                    members.add(c.getIp());
                }
            } else {
                LOGGER.warn(c.getIp() + " noticed primary "
                        + config.getOvm3PoolVip() + " is not part of pool");
                return false;
            }
            /* a cluster shares usernames and passwords */
            for (String member : members) {
                Connection x = new Connection(member, c.getPort(),
                        c.getUserName(), c.getPassword());
                Pool poolM = new Pool(x);
                if (poolM.isInAPool()) {
                    poolM.setPoolMemberList(members);
                    LOGGER.debug("Added " + members + " to pool "
                            + poolM.getPoolId() + " on member " + member);
                } else {
                    LOGGER.warn(member
                            + " unable to be member of a pool it's not in");
                    return false;
                }
            }
        } catch (Exception e) {
            throw new Ovm3ResourceException("Unable to add members: "
                    + e.getMessage(), e);
        }
        return true;
    }

    /**
     * Get a host out of a pool/cluster, this should unmount all FSs though.
     *
     * @param cmd
     * @return
     */
    public Answer execute(DeleteStoragePoolCommand cmd) {
        try {
            Pool pool = new Pool(c);
            pool.leaveServerPool(cmd.getPool().getUuid());
            /* also connect to the primary and update the pool list ? */
        } catch (Ovm3ResourceException e) {
            LOGGER.debug(
                    "Delete storage pool on host "
                            + config.getAgentHostname()
                            + " failed, however, we leave to user for cleanup and tell managment server it succeeded",
                    e);
        }

        return new Answer(cmd);
    }

    /**
     * Create primary storage, which is a repository in OVM. Pooling is part of
     * this too and clustering should be in the future.
     *
     * @param cmd
     * @return
     * @throws XmlRpcException
     */
    private boolean createRepo(StorageFilerTO cmd) throws XmlRpcException {
        String basePath = config.getAgentOvmRepoPath();
        Repository repo = new Repository(c);
        String primUuid = repo.deDash(cmd.getUuid());
        String ovsRepo = basePath + "/" + primUuid;
        /* should add port ? */
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath());
        String msg;

        if (cmd.getType() == StoragePoolType.NetworkFilesystem) {
            Boolean repoExists = false;
            /* base repo first */
            try {
                repo.mountRepoFs(mountPoint, ovsRepo);
            } catch (Ovm3ResourceException e) {
                LOGGER.debug("Unable to mount NFS repository " + mountPoint
                        + " on " + ovsRepo + " requested for "
                        + config.getAgentHostname() + ": " + e.getMessage());
            }
            try {
                repo.addRepo(mountPoint, ovsRepo);
                repoExists = true;
            } catch (Ovm3ResourceException e) {
                LOGGER.debug("NFS repository " + mountPoint + " on " + ovsRepo
                        + " not found creating repo: " + e.getMessage());
            }
            if (!repoExists) {
                try {
                    /*
                     * a mount of the NFS fs by the createrepo actually
                     * generates a null if it is already mounted... -sigh-
                     */
                    repo.createRepo(mountPoint, ovsRepo, primUuid,
                            "OVS Repository");
                } catch (Ovm3ResourceException e) {
                    msg = "NFS repository " + mountPoint + " on " + ovsRepo
                            + " create failed!";
                    LOGGER.debug(msg);
                    throw new CloudRuntimeException(msg + " " + e.getMessage(),
                            e);
                }
            }

            /* add base pooling first */
            if (config.getAgentInOvm3Pool()) {
                try {
                    msg = "Configuring " + config.getAgentHostname() + "("
                            + config.getAgentIp() + ") for pool";
                    LOGGER.debug(msg);
                    setupPool(cmd);
                    msg = "Configured host for pool";
                    /* add clustering after pooling */
                    if (config.getAgentInOvm3Cluster()) {
                        msg = "Setup " + config.getAgentHostname() + "("
                                + config.getAgentIp() + ")  for cluster";
                        LOGGER.debug(msg);
                        /* setup cluster */
                        /*
                         * From cluster.java
                         * configure_server_for_cluster(cluster conf, fs, mount,
                         * fsuuid, poolfsbaseuuid)
                         */
                        /* create_cluster(poolfsuuid,) */
                    }
                } catch (Ovm3ResourceException e) {
                    msg = "Unable to setup pool on  "
                            + config.getAgentHostname() + "("
                            + config.getAgentIp() + ") for " + ovsRepo;
                    throw new CloudRuntimeException(msg + " " + e.getMessage(),
                            e);
                }
            } else {
                msg = "no way dude I can't stand for this";
                LOGGER.debug(msg);
            }
            /*
             * this is to create the .generic_fs_stamp else we're not allowed to
             * create any data\disks on this thing
             */
            try {
                URI uri = new URI(cmd.getType() + "://" + cmd.getHost() + ":"
                        + +cmd.getPort() + cmd.getPath() + "/VirtualMachines");
                setupNfsStorage(uri, cmd.getUuid());
            } catch (Exception e) {
                msg = "NFS mount " + mountPoint + " on "
                        + config.getAgentSecStoragePath() + "/" + cmd.getUuid()
                        + " create failed!";
                throw new CloudRuntimeException(msg + " " + e.getMessage(), e);
            }
        } else {
            msg = "NFS repository " + mountPoint + " on " + ovsRepo
                    + " create failed, was type " + cmd.getType();
            LOGGER.debug(msg);
            return false;
        }

        try {
            /* systemvm iso is imported here */
            prepareSecondaryStorageStore(ovsRepo, cmd.getUuid(), cmd.getHost());
        } catch (Exception e) {
            msg = "systemvm.iso copy failed to " + ovsRepo;
            LOGGER.debug(msg, e);
            return false;
        }
        return true;
    }

    /**
     * Copy the systemvm.iso in if it doesn't exist or the size differs.
     *
     * @param storageUrl
     * @param poolUuid
     * @param host
     */
    private void prepareSecondaryStorageStore(String storageUrl,
            String poolUuid, String host) {
        String mountPoint = storageUrl;

        GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
        try {
            /* double check */
            if (config.getAgentHasPrimary() && config.getAgentInOvm3Pool()) {
                LOGGER.debug("Skip systemvm iso copy, leave it to the primary");
                return;
            }
            if (lock.lock(3600)) {
                try {
                    /*
                     * save src iso real name for reuse, so we don't depend on
                     * other happy little accidents.
                     */
                    File srcIso = getSystemVMPatchIsoFile();
                    String destPath = mountPoint + "/ISOs/";
                    try {
                        StoragePlugin sp = new StoragePlugin(c);
                        FileProperties fp = sp.storagePluginGetFileInfo(
                                poolUuid, host, destPath + "/"
                                        + srcIso.getName());
                        if (fp.getSize() != srcIso.getTotalSpace()) {
                            LOGGER.info(" System VM patch ISO file already exists: "
                                    + srcIso.getAbsolutePath().toString()
                                    + ", destination: " + destPath);
                        }
                    } catch (Exception e) {
                        LOGGER.info("Copy System VM patch ISO file to secondary storage. source ISO: "
                                + srcIso.getAbsolutePath()
                                + ", destination: "
                                + destPath);
                        try {
                            /* Perhaps use a key instead ? */
                            SshHelper
                                    .scpTo(c.getIp(), 22, config
                                            .getAgentSshUserName(), null,
                                            config.getAgentSshPassword(),
                                            destPath, srcIso.getAbsolutePath()
                                                    .toString(), "0644");
                        } catch (Exception es) {
                            LOGGER.error("Unexpected exception ", es);
                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: "
                                    + srcIso.toString()
                                    + ", dest location: "
                                    + destPath;
                            LOGGER.error(msg);
                            throw new CloudRuntimeException(msg, es);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }

    /**
     * The secondary storage mountpoint is a uuid based on the host combined
     * with the path.
     *
     * @param url
     * @return
     * @throws Ovm3ResourceException
     */
    public String setupSecondaryStorage(String url)
            throws Ovm3ResourceException {
        URI uri = URI.create(url);
        if (uri.getHost() == null) {
            throw new Ovm3ResourceException(
                    "Secondary storage host can not be empty!");
        }
        String uuid = ovmObject.newUuid(uri.getHost() + ":" + uri.getPath());
        LOGGER.info("Secondary storage with uuid: " + uuid);
        return setupNfsStorage(uri, uuid);
    }

    /**
     * Sets up NFS Storage
     *
     * @param uri
     * @param uuid
     * @return
     * @throws Ovm3ResourceException
     */
    private String setupNfsStorage(URI uri, String uuid)
            throws Ovm3ResourceException {
        String fsUri = "nfs";
        String msg = "";
        String mountPoint = config.getAgentSecStoragePath() + "/" + uuid;
        Linux host = new Linux(c);

        Map<String, Linux.FileSystem> fsList = host.getFileSystemMap(fsUri);
        Linux.FileSystem fs = fsList.get(uuid);
        if (fs == null || !fs.getRemoteDir().equals(mountPoint)) {
            try {
                StoragePlugin sp = new StoragePlugin(c);
                sp.storagePluginMountNFS(uri.getHost(), uri.getPath(), uuid,
                        mountPoint);
                msg = "Nfs storage " + uri + " mounted on " + mountPoint;
                return uuid;
            } catch (Ovm3ResourceException ec) {
                msg = "Nfs storage " + uri + " mount on " + mountPoint
                        + " FAILED " + ec.getMessage();
                LOGGER.error(msg);
                throw ec;
            }
        } else {
            msg = "NFS storage " + uri + " already mounted on " + mountPoint;
            return uuid;
        }
    }

    /**
     * Gets statistics for storage.
     *
     * @param cmd
     * @return
     */
    public GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        LOGGER.debug("Getting stats for: " + cmd.getStorageId());
        try {
            Linux host = new Linux(c);
            Linux.FileSystem fs = host.getFileSystemByUuid(cmd.getStorageId(),
                    "nfs");
            StoragePlugin store = new StoragePlugin(c);
            String propUuid = store.deDash(cmd.getStorageId());
            String mntUuid = cmd.getStorageId();
            if (store == null || propUuid == null || mntUuid == null
                    || fs == null) {
                String msg = "Null returned when retrieving stats for "
                        + cmd.getStorageId();
                LOGGER.error(msg);
                return new GetStorageStatsAnswer(cmd, msg);
            }
            /* or is it mntUuid ish ? */
            StorageDetails sd = store.storagePluginGetFileSystemInfo(propUuid,
                    mntUuid, fs.getHost(), fs.getDevice());
            /*
             * FIXME: cure me or kill me, this needs to trigger a reinit of
             * primary storage, actually the problem is more deeprooted, as when
             * the hypervisor reboots it looses partial context and needs to be
             * reinitiated.... actually a full configure round... how to trigger
             * that ?
             */
            if ("".equals(sd.getSize())) {
                String msg = "No size when retrieving stats for "
                        + cmd.getStorageId();
                LOGGER.debug(msg);
                return new GetStorageStatsAnswer(cmd, msg);
            }
            long total = Long.parseLong(sd.getSize());
            long used = total - Long.parseLong(sd.getFreeSize());
            return new GetStorageStatsAnswer(cmd, total, used);
        } catch (Ovm3ResourceException e) {
            LOGGER.debug("GetStorageStatsCommand for " + cmd.getStorageId()
                    + " failed", e);
            return new GetStorageStatsAnswer(cmd, e.getMessage());
        }
    }

    /**
     * Try to figure out where the systemvm.iso resides on the fs of the
     * management server
     *
     * @return
     */
    public File getSystemVMPatchIsoFile() {
        String iso = "systemvm.iso";
        String systemVmIsoPath = Script.findScript("", "vms/" + iso);
        File isoFile = null;
        if (systemVmIsoPath != null) {
            LOGGER.debug("found systemvm patch iso " + systemVmIsoPath);
            isoFile = new File(systemVmIsoPath);
        }
        if (isoFile == null || !isoFile.exists()) {
            String svm = "client/target/generated-webapp/WEB-INF/classes/vms/"
                    + iso;
            LOGGER.debug("last resort for systemvm patch iso " + svm);
            isoFile = new File(svm);
        }
        assert isoFile != null;
        if (!isoFile.exists()) {
            LOGGER.error("Unable to locate " + iso + " in your setup at "
                    + isoFile.toString());
        }
        return isoFile;
    }

    /**
     * Create and OCFS2 filesystem (not implemented)
     *
     * @param pool
     * @return
     * @throws XmlRpcException
     */
    private Boolean createOCFS2Sr(StorageFilerTO pool) throws XmlRpcException {
        LOGGER.debug("OCFS2 Not implemented yet");
        return false;
    }

    /**
     * Gets the details of a storage pool, size etc
     *
     * @param cmd
     * @return
     */
    public Answer execute(ModifyStoragePoolCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        LOGGER.debug("modifying pool " + pool);
        try {
            if (config.getAgentInOvm3Cluster()) {
                // no native ovm cluster for now, I got to break it in horrible
                // ways
            }
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                createRepo(pool);
                StoragePlugin store = new StoragePlugin(c);
                String propUuid = store.deDash(pool.getUuid());
                String mntUuid = pool.getUuid();
                String nfsHost = pool.getHost();
                String nfsPath = pool.getPath();
                StorageDetails ss = store.storagePluginGetFileSystemInfo(
                        propUuid, mntUuid, nfsHost, nfsPath);

                Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
                return new ModifyStoragePoolAnswer(cmd, Long.parseLong(ss
                        .getSize()), Long.parseLong(ss.getFreeSize()), tInfo);
            } else if (pool.getType() == StoragePoolType.OCFS2) {
                createOCFS2Sr(pool);
            }
            return new Answer(cmd, false, "The pool type: "
                    + pool.getType().name() + " is not supported.");
        } catch (Exception e) {
            LOGGER.debug("ModifyStoragePoolCommand failed", e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    /**
     * Create the primary storage pool, should add iSCSI and OCFS2
     *
     * @param cmd
     * @return
     */
    public Answer execute(CreateStoragePoolCommand cmd) {
        StorageFilerTO pool = cmd.getPool();
        LOGGER.debug("creating pool " + pool);
        try {
            if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                createRepo(pool);
            } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                return new Answer(cmd, false,
                        "iSCSI is unsupported at the moment");
                /*
                 * iScsi like so: getIscsiSR(conn, pool.getUuid(),
                 * pool.getHost(), pool.getPath(), null, null, false);
                 */
            } else if (pool.getType() == StoragePoolType.OCFS2) {
                return new Answer(cmd, false,
                        "OCFS2 is unsupported at the moment");
            } else if (pool.getType() == StoragePoolType.PreSetup) {
                LOGGER.warn("pre setup for pool " + pool);
            } else {
                return new Answer(cmd, false, "The pool type: "
                        + pool.getType().name() + " is not supported.");
            }
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName()
                    + ", create StoragePool failed due to " + e.toString()
                    + " on host:" + config.getAgentHostname() + " pool: "
                    + pool.getHost() + pool.getPath();
            LOGGER.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
        return new Answer(cmd, true, "success");
    }

    /**
     * Download from template url into primary storage ?.. is this relevant ?
     *
     * @param cmd
     * @return
     */
    public PrimaryStorageDownloadAnswer execute(
            final PrimaryStorageDownloadCommand cmd) {
        try {
            Repository repo = new Repository(c);
            String tmplturl = cmd.getUrl();
            String poolName = cmd.getPoolUuid();
            String image = repo.deDash(repo.newUuid()) + ".raw";

            /* url to download from, image name, and repo to copy it to */
            repo.importVirtualDisk(tmplturl, image, poolName);
            return new PrimaryStorageDownloadAnswer(image);
        } catch (Exception e) {
            LOGGER.debug("PrimaryStorageDownloadCommand failed", e);
            return new PrimaryStorageDownloadAnswer(e.getMessage());
        }
    }
}
