package com.cloud.hypervisor.ovm3.resources.helpers;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
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
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Linux;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.Pool;
import com.cloud.hypervisor.ovm3.objects.PoolOCFS2;
import com.cloud.hypervisor.ovm3.objects.Repository;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.FileProperties;
import com.cloud.hypervisor.ovm3.objects.StoragePlugin.StorageDetails;
import com.cloud.storage.Volume;
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
    public Ovm3StoragePool(Connection conn) {
        c = conn;
    }

    /* cleanup nested try stuff */
    private boolean prepareForPool() throws ConfigurationException {
        /* need single master uuid */
        try {
            Linux host = new Linux(c);
            Pool pool = new Pool(c);

            /* setup pool and role, needs utility to be able to do things */
            if (host.getServerRoles().contentEquals(
                    pool.getValidRoles().toString())) {
                LOGGER.debug("Server role for host " + agentHostname + " is ok");
            } else {
                try {
                    pool.setServerRoles(pool.getValidRoles());
                } catch (Ovm3ResourceException e) {
                    LOGGER.debug("Failed to set server role for host "
                            + agentHostname, e);
                    throw new ConfigurationException(
                            "Unable to set server role for host "
                                    + e.getMessage());
                }
            }
            if (host.getMembershipState().contentEquals("Unowned")) {
                try {
                    LOGGER.debug("Take ownership of host " + agentHostname);
                    pool.takeOwnership(agentOwnedByUuid, "");
                } catch (Ovm3ResourceException e) {
                    String msg = "Failed to take ownership of host "
                            + agentHostname;
                    throw new ConfigurationException(msg);
                }
            } else {
                /* TODO: check if it's part of our pool, give ok if it is */
                if (host.getManagerUuid().equals(agentOwnedByUuid)) {
                    String msg = "Host " + agentHostname + " owned by us";
                    LOGGER.debug(msg);
                    return true;
                } else {
                    String msg = "Host " + agentHostname
                            + " already part of a pool, and not owned by us";
                    LOGGER.debug(msg);
                    throw new ConfigurationException(msg);
                }
            }
        } catch (ConfigurationException | Ovm3ResourceException es) {
            String msg = "Failed to prepare " + agentHostname + " for pool";
            throw new ConfigurationException(msg + ": " + es.getMessage());
        }
        return true;
    }

    /*
     * TODO: redo this, it's a mess now.
     */
    private Boolean setupPool(StorageFilerTO cmd) throws Ovm3ResourceException {
        String primUuid = cmd.getUuid();
        String ssUuid = ovmObject.deDash(primUuid);
        String fsType = "nfs";
        /* TODO: 16, need to get this from the cluster id actually */
        String clusterUuid = agentOwnedByUuid.substring(0, 15);
        String managerId = agentOwnedByUuid;
        String poolAlias = cmd.getHost() + ":" + cmd.getPath();
        String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                cmd.getPath())
                + "/VirtualMachines";
        /* TODO: fix pool size retrieval */
        Integer poolSize = 0;

        Pool poolHost = new Pool(c);
        PoolOCFS2 poolFs = new PoolOCFS2(c);
        masterCheck();
        if (agentIsMaster) {
            try {
                LOGGER.debug("Create poolfs on " + agentHostname + " for repo "
                        + primUuid);
                /* double check if we're not overwritting anything here!@ */
                poolFs.createPoolFs(fsType, mountPoint, clusterUuid, primUuid,
                        ssUuid, managerId);
            } catch (Ovm3ResourceException e) {
                throw e;
            }
            try {
                poolHost.createServerPool(poolAlias, primUuid, ovm3PoolVip,
                        poolSize + 1, agentHostname, agentIp);
            } catch (Ovm3ResourceException e) {
                throw e;
            }
        } else if (agentHasMaster) {
            try {
                poolHost.joinServerPool(poolAlias, primUuid, ovm3PoolVip,
                        poolSize + 1, agentHostname, agentIp);
            } catch (Ovm3ResourceException e) {
                throw e;
            }
        }
        try {
            if (!addMembers()) {
                return false;
            }
        } catch (Ovm3ResourceException e) {
            throw e;
        }
        return true;
    }

    /* TODO: Fix member addition */
    private Boolean addMembers() throws Ovm3ResourceException {
        List<String> members = new ArrayList<String>();
        try {
            Connection m = new Connection(ovm3PoolVip, agentOvsAgentPort,
                    agentOvsAgentUser, agentOvsAgentPassword);
            Pool poolMaster = new Pool(m);
            if (poolMaster.isInAPool()) {
                members.addAll(poolMaster.getPoolMemberList());
                if (!poolMaster.getPoolMemberList().contains(agentIp)) {
                    members.add(agentIp);
                }
            } else {
                LOGGER.warn(agentIp + " noticed master " + ovm3PoolVip
                        + " is not part of pool");
                return false;
            }
            for (String member : members) {
                Connection x = new Connection(member, agentOvsAgentPort,
                        agentOvsAgentUser, agentOvsAgentPassword);
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
    /*
     * TODO: this ties into if we're in a cluster or just a pool, leaving a pool
     * means just leaving the pool and getting rid of the pooled fs, different a
     * repo though
     */
    private Answer execute(DeleteStoragePoolCommand cmd) {
        try {
            Pool pool = new Pool(c);
            pool.leaveServerPool(cmd.getPool().getUuid());
            /* also connect to the master and update the pool list ? */
        } catch (Ovm3ResourceException e) {
            LOGGER.debug(
                    "Delete storage pool on host "
                            + agentHostname
                            + " failed, however, we leave to user for cleanup and tell managment server it succeeded",
                    e);
        }

        return new Answer(cmd);
        
        /*
         * Primary storage, will throw an error if ownership does not match! Pooling
         * is a part of this, for now
         */
        private boolean createRepo(StorageFilerTO cmd) throws XmlRpcException {
            String basePath = agentOvmRepoPath;
            Repository repo = new Repository(c);
            String primUuid = repo.deDash(cmd.getUuid());
            String ovsRepo = basePath + "/" + primUuid;
            /* should add port ? */
            String mountPoint = String.format("%1$s:%2$s", cmd.getHost(),
                    cmd.getPath());
            String msg;

            if (cmd.getType() == StoragePoolType.NetworkFilesystem) {
                /* TODO: condense and move into Repository */
                Boolean repoExists = false;
                /* base repo first */
                try {
                    repo.mountRepoFs(mountPoint, ovsRepo);
                } catch (Ovm3ResourceException e) {
                    LOGGER.debug("Unable to mount NFS repository " + mountPoint
                            + " on " + ovsRepo + " requested for " + agentHostname
                            + ": " + e.getMessage());
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
                if (agentInOvm3Pool) {
                    try {
                        msg = "Configuring host for pool";
                        LOGGER.debug(msg);
                        setupPool(cmd);
                        msg = "Configured host for pool";
                        /* add clustering after pooling */
                        if (agentInOvm3Cluster) {
                            msg = "Configuring host for cluster";
                            LOGGER.debug(msg);
                            /* setup cluster */
                            /*
                             * From cluster.java
                             * configure_server_for_cluster(cluster conf, fs, mount,
                             * fsuuid, poolfsbaseuuid)
                             */
                            /* create_cluster(poolfsuuid,) */
                            msg = "Configuring host for cluster";
                        }
                    } catch (Ovm3ResourceException e) {
                        msg = "Unable to setup pool on " + ovsRepo;
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
                    msg = "NFS mount " + mountPoint + " on " + agentSecStoragePath
                            + "/" + cmd.getUuid() + " create failed!";
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

        /*  */
        private void prepareSecondaryStorageStore(String storageUrl,
                String poolUuid, String host) {
            String mountPoint = storageUrl;

            GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
            try {
                /* double check */
                if (agentHasMaster && agentInOvm3Pool) {
                    LOGGER.debug("Skip systemvm iso copy, leave it to the master");
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
                                    poolUuid, host, destPath + File.separator
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
                                SshHelper.scpTo(agentHostname, 22,
                                        agentSshUserName, null, agentSshPassword,
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
        /* the storage url combination of host and path is unique */
        public String setupSecondaryStorage(String url)
                throws Ovm3ResourceException {
            URI uri = URI.create(url);
            String uuid = ovmObject.newUuid(uri.getHost() + ":" + uri.getPath());
            LOGGER.info("Secondary storage with uuid: " + uuid);
            return setupNfsStorage(uri, uuid);
        }

        /* NFS only for now, matches FileSys */
        private String setupNfsStorage(URI uri, String uuid)
                throws Ovm3ResourceException {
            String fsUri = "nfs";
            String msg = "";
            String mountPoint = agentSecStoragePath + "/" + uuid;
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
        /* cleanup the storageplugin so we can use an object here */
        private GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
            LOGGER.debug("Getting stats for: " + cmd.getStorageId());
            try {
                Linux host = new Linux(c);
                /* TODO: NFS only for now */
                Linux.FileSystem fs = host.getFileSystemByUuid(cmd.getStorageId(),
                        "nfs");
                StoragePlugin store = new StoragePlugin(c);
                String propUuid = store.deDash(cmd.getStorageId());
                String mntUuid = cmd.getStorageId();
                /* or is it mntUuid ish ? */
                StorageDetails sd = store.storagePluginGetFileSystemInfo(propUuid,
                        mntUuid, fs.getHost(), fs.getDevice());
                long total = Long.parseLong(sd.getSize());
                long used = total - Long.parseLong(sd.getFreeSize());
                return new GetStorageStatsAnswer(cmd, total, used);
            } catch (Ovm3ResourceException e) {
                LOGGER.debug("GetStorageStatsCommand on pool " + cmd.getStorageId()
                        + " failed", e);
                return new GetStorageStatsAnswer(cmd, e.getMessage());
            }
        }
        private String getSystemVMIsoFileNameOnDatastore() {
            String version = this.getClass().getPackage()
                    .getImplementationVersion();
            String fileName = "systemvm-" + version + ".iso";
            return fileName.replace(':', '-');
        }

        /* stolen from vmware impl */
        private File getSystemVMPatchIsoFile() {
            String iso = "systemvm.iso";
            String svmName = getSystemVMIsoFileNameOnDatastore();
            String systemVmIsoPath = Script.findScript("", "vms/" + iso);
            File isoFile = null;
            if (systemVmIsoPath != null) {
                LOGGER.debug("found systemvm patch iso " + systemVmIsoPath);
                isoFile = new File(systemVmIsoPath);
            }
            if (isoFile == null || !isoFile.exists()) {
                LOGGER.debug("found no local systemvm patch iso " + systemVmIsoPath
                        + " moving on");
                isoFile = new File("/usr/share/cloudstack-common/vms/" + svmName);
            }
            if (isoFile == null || !isoFile.exists()) {
                String svm = "client/target/generated-webapp/WEB-INF/classes/vms/"
                        + iso;
                LOGGER.debug("last resort for systemvm patch iso " + svm);
                isoFile = new File(svm);
            }
            assert isoFile != null;
            if (!isoFile.exists()) {
                LOGGER.error("Unable to locate " + svmName + " in your setup at "
                        + isoFile.toString());
            }
            return isoFile;
        }

        /*
         * TODO: local OCFS2? or iSCSI OCFS2
         */
        private Boolean createOCFS2Sr(StorageFilerTO pool) throws XmlRpcException {
            LOGGER.debug("OCFS2 Not implemented yet");
            return false;
        }

        /* Setup a storage pool and also get the size */
        private Answer execute(ModifyStoragePoolCommand cmd) {
            StorageFilerTO pool = cmd.getPool();
            LOGGER.debug("modifying pool " + pool);
            try {
                if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                    /* TODO: this should actually not be here */
                    createRepo(pool);
                } else if (pool.getType() == StoragePoolType.OCFS2) {
                    createOCFS2Sr(pool);
                } else {
                    return new Answer(cmd, false, "The pool type: "
                            + pool.getType().name() + " is not supported.");
                }

                if (agentInOvm3Cluster) {
                    /* TODO: What extras do we need here ? HB? */
                }
                /* TODO: needs to be in network fs above */
                StoragePlugin store = new StoragePlugin(c);
                String propUuid = store.deDash(pool.getUuid());
                String mntUuid = pool.getUuid();
                String nfsHost = pool.getHost();
                String nfsPath = pool.getPath();
                StorageDetails ss = store.storagePluginGetFileSystemInfo(propUuid,
                        mntUuid, nfsHost, nfsPath);

                Map<String, TemplateProp> tInfo = new HashMap<String, TemplateProp>();
                return new ModifyStoragePoolAnswer(cmd,
                        Long.parseLong(ss.getSize()), Long.parseLong(ss
                                .getFreeSize()), tInfo);
            } catch (Exception e) {
                LOGGER.debug("ModifyStoragePoolCommand failed", e);
                return new Answer(cmd, false, e.getMessage());
            }
        }

        /* TODO: add iSCSI */
        private Answer execute(CreateStoragePoolCommand cmd) {
            StorageFilerTO pool = cmd.getPool();
            LOGGER.debug("creating pool " + pool);
            try {
                if (pool.getType() == StoragePoolType.NetworkFilesystem) {
                    createRepo(pool);
                } else if (pool.getType() == StoragePoolType.IscsiLUN) {
                    return new Answer(cmd, false,
                            "iSCSI is unsupported at the moment");
                    /*
                     * TODO: Implement iScsi like so: getIscsiSR(conn,
                     * pool.getUuid(), pool.getHost(), pool.getPath(), null, null,
                     * false);
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
                        + " on host:" + agentHostname + " pool: " + pool.getHost()
                        + pool.getPath();
                LOGGER.warn(msg, e);
                return new Answer(cmd, false, msg);
            }
            return new Answer(cmd, true, "success");
        }

        /*
         * Download some primary storage into the repository, we need the repoid to
         * do that, but also have a uuid for the disk...
         * TODO: looks like we don't need this for now! (dead code)
         */
        private PrimaryStorageDownloadAnswer execute(
                final PrimaryStorageDownloadCommand cmd) {
            try {
                Repository repo = new Repository(c);
                String tmplturl = cmd.getUrl();
                String poolName = cmd.getPoolUuid();
                String image = repo.deDash(repo.newUuid()) + ".raw";

                /* url to download from, image name, and repo to copy it to */
                repo.importVirtualDisk(tmplturl, image, poolName);

                /* TODO: return uuid and size */
                return new PrimaryStorageDownloadAnswer(image);
            } catch (Exception e) {
                LOGGER.debug("PrimaryStorageDownloadCommand failed", e);
                return new PrimaryStorageDownloadAnswer(e.getMessage());
            }
        }

        /*
         * Add rootdisk, datadisk and iso's
         */
        private Boolean createVbds(Xen.Vm vm, VirtualMachineTO spec) {
            for (DiskTO volume : spec.getDisks()) {
                try {
                    if (volume.getType() == Volume.Type.ROOT) {
                        VolumeObjectTO vol = (VolumeObjectTO) volume.getData();
                        DataStoreTO ds = vol.getDataStore();
                        String dsk = vol.getPath() + "/" + vol.getUuid() + ".raw";
                        vm.addRootDisk(dsk);
                        /* TODO: needs to be replaced by rootdiskuuid? */
                        vm.setPrimaryPoolUuid(ds.getUuid());
                        LOGGER.debug("Adding root disk: " + dsk);
                    } else if (volume.getType() == Volume.Type.ISO) {
                        DataTO isoTO = volume.getData();
                        if (isoTO.getPath() != null) {
                            TemplateObjectTO template = (TemplateObjectTO) isoTO;
                            DataStoreTO store = template.getDataStore();
                            if (!(store instanceof NfsTO)) {
                                throw new CloudRuntimeException(
                                        "unsupported protocol");
                            }
                            NfsTO nfsStore = (NfsTO) store;
                            String secPoolUuid = setupSecondaryStorage(nfsStore
                                    .getUrl());
                            String isoPath = agentSecStoragePath + File.separator
                                    + secPoolUuid + File.separator
                                    + template.getPath();
                            vm.addIso(isoPath);
                            /* check if secondary storage is mounted */
                            LOGGER.debug("Adding ISO: " + isoPath);
                        }
                    } else if (volume.getType() == Volume.Type.DATADISK) {
                        vm.addDataDisk(volume.getData().getPath());
                        LOGGER.debug("Adding data disk: "
                                + volume.getData().getPath());
                    } else {
                        throw new CloudRuntimeException("Unknown volume type: "
                                + volume.getType());
                    }
                } catch (Exception e) {
                    LOGGER.debug("CreateVbds failed", e);
                    throw new CloudRuntimeException("Exception" + e.getMessage(), e);
                }
            }
            return true;
        }

}
