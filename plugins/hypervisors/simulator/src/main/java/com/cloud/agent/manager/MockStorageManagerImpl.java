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
package com.cloud.agent.manager;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.command.DownloadProgressCommand;
import org.apache.cloudstack.storage.command.UploadStatusAnswer;
import org.apache.cloudstack.storage.command.UploadStatusAnswer.UploadStatus;
import org.apache.cloudstack.storage.command.UploadStatusCommand;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.ComputeChecksumCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVolumeStatsAnswer;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.SecStorageSetupAnswer;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.SecStorageVMSetupCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.VolumeStatsEntry;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.ListTemplateAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.storage.ListVolumeAnswer;
import com.cloud.agent.api.storage.ListVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockSecStorageVO;
import com.cloud.simulator.MockStoragePoolVO;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.MockVm;
import com.cloud.simulator.MockVolumeVO;
import com.cloud.simulator.MockVolumeVO.MockVolumeType;
import com.cloud.simulator.dao.MockHostDao;
import com.cloud.simulator.dao.MockSecStorageDao;
import com.cloud.simulator.dao.MockStoragePoolDao;
import com.cloud.simulator.dao.MockVMDao;
import com.cloud.simulator.dao.MockVolumeDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.template.TemplateProp;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;

@Component
public class MockStorageManagerImpl extends ManagerBase implements MockStorageManager {
    private static final Logger s_logger = Logger.getLogger(MockStorageManagerImpl.class);
    @Inject
    MockStoragePoolDao _mockStoragePoolDao = null;
    @Inject
    MockSecStorageDao _mockSecStorageDao = null;
    @Inject
    MockVolumeDao _mockVolumeDao = null;
    @Inject
    MockVMDao _mockVMDao = null;
    @Inject
    MockHostDao _mockHostDao = null;
    @Inject
    VMTemplateDao templateDao;

    private MockVolumeVO findVolumeFromSecondary(String path, String ssUrl, MockVolumeType type) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            String volumePath = path.replaceAll(ssUrl, "");
            MockSecStorageVO secStorage = _mockSecStorageDao.findByUrl(ssUrl);
            if (secStorage == null) {
                return null;
            }
            volumePath = secStorage.getMountPoint() + volumePath;
            volumePath = volumePath.replaceAll("//", "/");
            MockVolumeVO volume = _mockVolumeDao.findByStoragePathAndType(volumePath);
            txn.commit();
            if (volume == null) {
                return null;
            }
            return volume;
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to find volume " + path + " on secondary " + ssUrl, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public PrimaryStorageDownloadAnswer primaryStorageDownload(PrimaryStorageDownloadCommand cmd) {
        MockVolumeVO template = findVolumeFromSecondary(cmd.getUrl(), cmd.getSecondaryStorageUrl(), MockVolumeType.TEMPLATE);
        if (template == null) {
            return new PrimaryStorageDownloadAnswer("Can't find primary storage");
        }

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockStoragePoolVO primaryStorage = null;
        try {
            txn.start();
            primaryStorage = _mockStoragePoolDao.findByUuid(cmd.getPoolUuid());
            txn.commit();
            if (primaryStorage == null) {
                return new PrimaryStorageDownloadAnswer("Can't find primary storage");
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding primary storagee " + cmd.getPoolUuid(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        String volumeName = UUID.randomUUID().toString();
        MockVolumeVO newVolume = new MockVolumeVO();
        newVolume.setName(volumeName);
        newVolume.setPath(primaryStorage.getMountPoint() + volumeName);
        newVolume.setPoolId(primaryStorage.getId());
        newVolume.setSize(template.getSize());
        newVolume.setType(MockVolumeType.VOLUME);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            _mockVolumeDao.persist(newVolume);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when saving volume " + newVolume, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new PrimaryStorageDownloadAnswer(newVolume.getPath(), newVolume.getSize());
    }

    @Override
    public CreateAnswer createVolume(CreateCommand cmd) {
        StorageFilerTO sf = cmd.getPool();
        DiskProfile dskch = cmd.getDiskCharacteristics();
        MockStoragePoolVO storagePool = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            storagePool = _mockStoragePoolDao.findByUuid(sf.getUuid());
            txn.commit();
            if (storagePool == null) {
                return new CreateAnswer(cmd, "Failed to find storage pool: " + sf.getUuid());
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding storage " + sf.getUuid(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        String volumeName = UUID.randomUUID().toString();
        MockVolumeVO volume = new MockVolumeVO();
        volume.setPoolId(storagePool.getId());
        volume.setName(volumeName);
        volume.setPath(storagePool.getMountPoint() + volumeName);
        volume.setSize(dskch.getSize());
        volume.setType(MockVolumeType.VOLUME);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            volume = _mockVolumeDao.persist(volume);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when saving volume " + volume, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        VolumeTO volumeTo =
            new VolumeTO(cmd.getVolumeId(), dskch.getType(), sf.getType(), sf.getUuid(), volume.getName(), storagePool.getMountPoint(), volume.getPath(),
                volume.getSize(), null);

        return new CreateAnswer(cmd, volumeTo);
    }

    @Override
    public Answer AttachIso(AttachIsoCommand cmd) {
        MockVolumeVO iso = findVolumeFromSecondary(cmd.getIsoPath(), cmd.getStoreUrl(), MockVolumeType.ISO);
        if (iso == null) {
            return new Answer(cmd, false, "Failed to find the iso: " + cmd.getIsoPath() + "on secondary storage " + cmd.getStoreUrl());
        }

        String vmName = cmd.getVmName();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockVMVO vm = null;
        try {
            txn.start();
            vm = _mockVMDao.findByVmName(vmName);
            txn.commit();
            if (vm == null) {
                return new Answer(cmd, false, "can't find vm :" + vmName);
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when attaching iso to vm " + vmName, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new Answer(cmd);
    }

    @Override
    public Answer DeleteStoragePool(DeleteStoragePoolCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            MockStoragePoolVO storage = _mockStoragePoolDao.findByUuid(cmd.getPool().getUuid());
            if (storage == null) {
                return new Answer(cmd, false, "can't find storage pool:" + cmd.getPool().getUuid());
            }
            _mockStoragePoolDao.remove(storage.getId());
            txn.commit();
            return new Answer(cmd);
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when deleting storage pool " + cmd.getPool().getPath(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public ModifyStoragePoolAnswer ModifyStoragePool(ModifyStoragePoolCommand cmd) {
        StorageFilerTO sf = cmd.getPool();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockStoragePoolVO storagePool = null;
        try {
            txn.start();
            storagePool = _mockStoragePoolDao.findByUuid(sf.getUuid());
            if (storagePool == null) {
                storagePool = new MockStoragePoolVO();
                storagePool.setUuid(sf.getUuid());
                storagePool.setMountPoint("/mnt/" + sf.getUuid());

                Long size = DEFAULT_HOST_STORAGE_SIZE;
                String path = sf.getPath();
                int index = path.lastIndexOf("/");
                if (index != -1) {
                    path = path.substring(index + 1);
                    if (path != null) {
                        String values[] = path.split("=");
                        if (values.length > 1 && values[0].equalsIgnoreCase("size")) {
                            size = Long.parseLong(values[1]);
                        }
                    }
                }
                storagePool.setCapacity(size);
                storagePool.setStorageType(sf.getType());
                storagePool = _mockStoragePoolDao.persist(storagePool);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when modifying storage pool " + cmd.getPool().getPath(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new ModifyStoragePoolAnswer(cmd, storagePool.getCapacity(), storagePool.getCapacity(), new HashMap<String, TemplateProp>());
    }

    @Override
    public Answer CreateStoragePool(CreateStoragePoolCommand cmd) {
        StorageFilerTO sf = cmd.getPool();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockStoragePoolVO storagePool = null;
        try {
            txn.start();
            storagePool = _mockStoragePoolDao.findByUuid(sf.getUuid());
            if (storagePool == null) {
                storagePool = new MockStoragePoolVO();
                storagePool.setUuid(sf.getUuid());
                storagePool.setMountPoint("/mnt/" + sf.getUuid());

                Long size = DEFAULT_HOST_STORAGE_SIZE;
                String path = sf.getPath();
                int index = path.lastIndexOf("/");
                if (index != -1) {
                    path = path.substring(index + 1);
                    if (path != null) {
                        String values[] = path.split("=");
                        if (values.length > 1 && values[0].equalsIgnoreCase("size")) {
                            size = Long.parseLong(values[1]);
                        }
                    }
                }
                storagePool.setCapacity(size);
                storagePool.setStorageType(sf.getType());
                storagePool = _mockStoragePoolDao.persist(storagePool);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when creating storage pool " + cmd.getPool().getPath(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new ModifyStoragePoolAnswer(cmd, storagePool.getCapacity(), 0, new HashMap<String, TemplateProp>());
    }

    @Override
    public Answer SecStorageSetup(SecStorageSetupCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockSecStorageVO storage = null;
        try {
            txn.start();
            storage = _mockSecStorageDao.findByUrl(cmd.getSecUrl());
            if (storage == null) {
                return new Answer(cmd, false, "can't find the storage");
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when setting up sec storage" + cmd.getSecUrl(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new SecStorageSetupAnswer(storage.getMountPoint());
    }

    @Override
    public Answer ListVolumes(ListVolumeCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockSecStorageVO storage = null;
        try {
            txn.start();
            storage = _mockSecStorageDao.findByUrl(cmd.getSecUrl());
            if (storage == null) {
                return new Answer(cmd, false, "Failed to get secondary storage");
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding sec storage " + cmd.getSecUrl(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            List<MockVolumeVO> volumes = _mockVolumeDao.findByStorageIdAndType(storage.getId(), MockVolumeType.VOLUME);

            Map<Long, TemplateProp> templateInfos = new HashMap<Long, TemplateProp>();
            for (MockVolumeVO volume : volumes) {
                templateInfos.put(volume.getId(),
                    new TemplateProp(volume.getName(), volume.getPath().replaceAll(storage.getMountPoint(), ""), volume.getSize(), volume.getSize(), true, false));
            }
            txn.commit();
            return new ListVolumeAnswer(cmd.getSecUrl(), templateInfos);
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding template on sec storage " + storage.getId(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public Answer ListTemplates(ListTemplateCommand cmd) {
        DataStoreTO store = cmd.getDataStore();
        if (!(store instanceof NfsTO)) {
            return new Answer(cmd, false, "Unsupported image data store: " + store);
        }

        MockSecStorageVO storage = null;
        String nfsUrl = ((NfsTO)store).getUrl();

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        storage = _mockSecStorageDao.findByUrl(nfsUrl);
        try {
            txn.start();
            List<MockVolumeVO> templates = _mockVolumeDao.findByStorageIdAndType(storage.getId(), MockVolumeType.TEMPLATE);

            Map<String, TemplateProp> templateInfos = new HashMap<String, TemplateProp>();
            for (MockVolumeVO template : templates) {
                templateInfos.put(template.getName(), new TemplateProp(template.getName(), template.getPath().replaceAll(storage.getMountPoint(), ""),
                    template.getSize(), template.getSize(), true, false));
            }
            return new ListTemplateAnswer(nfsUrl, templateInfos);
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding template on sec storage " + storage.getId(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public Answer Destroy(DestroyCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            MockVolumeVO volume = _mockVolumeDao.findByStoragePathAndType(cmd.getVolume().getPath());
            if (volume != null) {
                _mockVolumeDao.remove(volume.getId());
            }
            if (cmd.getVmName() != null) {
                MockVm vm = _mockVMDao.findByVmName(cmd.getVmName());
                if (vm != null) {
                    _mockVMDao.remove(vm.getId());
                }
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when destroying volume " + cmd.getVolume().getPath(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new Answer(cmd);
    }

    @Override
    public DownloadAnswer Download(DownloadCommand cmd) {
        MockSecStorageVO ssvo = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            ssvo = _mockSecStorageDao.findByUrl(cmd.getSecUrl());
            if (ssvo == null) {
                return new DownloadAnswer("can't find secondary storage", VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error accessing secondary storage " + cmd.getSecUrl(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        MockVolumeVO volume = new MockVolumeVO();
        volume.setPoolId(ssvo.getId());
        volume.setName(cmd.getName());
        volume.setPath(ssvo.getMountPoint() + cmd.getName());
        volume.setSize(0);
        volume.setType(MockVolumeType.TEMPLATE);
        volume.setStatus(Status.DOWNLOAD_IN_PROGRESS);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            volume = _mockVolumeDao.persist(volume);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when saving volume " + volume, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new DownloadAnswer(String.valueOf(volume.getId()), 0, "Downloading", Status.DOWNLOAD_IN_PROGRESS, cmd.getName(), cmd.getName(), volume.getSize(),
            volume.getSize(), null);
    }

    @Override
    public DownloadAnswer DownloadProcess(DownloadProgressCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            String volumeId = cmd.getJobId();
            MockVolumeVO volume = _mockVolumeDao.findById(Long.parseLong(volumeId));
            if (volume == null) {
                return new DownloadAnswer("Can't find the downloading volume", Status.ABANDONED);
            }

            long size = Math.min(volume.getSize() + DEFAULT_TEMPLATE_SIZE / 5, DEFAULT_TEMPLATE_SIZE);
            volume.setSize(size);

            double volumeSize = volume.getSize();
            double pct = volumeSize / DEFAULT_TEMPLATE_SIZE;
            if (pct >= 1.0) {
                volume.setStatus(Status.DOWNLOADED);
                _mockVolumeDao.update(volume.getId(), volume);
                txn.commit();
                return new DownloadAnswer(cmd.getJobId(), 100, cmd, com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED, volume.getPath(),
                    volume.getName());
            } else {
                _mockVolumeDao.update(volume.getId(), volume);
                txn.commit();
                return new DownloadAnswer(cmd.getJobId(), (int)(pct * 100.0), cmd, com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_IN_PROGRESS,
                    volume.getPath(), volume.getName());
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error during download job " + cmd.getJobId(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public GetVolumeStatsAnswer getVolumeStats(final GetVolumeStatsCommand cmd) {
        HashMap<String, VolumeStatsEntry> volumeStats =
                cmd.getVolumeUuids()
                .stream()
                .collect(Collectors.toMap(Function.identity(),
                                          this::getVolumeStat,
                                          (v1, v2) -> v1, HashMap::new));

        return new GetVolumeStatsAnswer(cmd, "", volumeStats);
    }

    private VolumeStatsEntry getVolumeStat(final String volumeUuid)  {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);

        try {
            txn.start();
            MockVolumeVO volume  = _mockVolumeDao.findByUuid(volumeUuid);
            txn.commit();
            return new VolumeStatsEntry(volumeUuid, volume.getSize(), volume.getSize());
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding volume " + volumeUuid, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

    }

    @Override
    public GetStorageStatsAnswer GetStorageStats(GetStorageStatsCommand cmd) {
        String uuid = cmd.getStorageId();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            if (uuid == null) {
                String secUrl = cmd.getSecUrl();
                MockSecStorageVO secondary = _mockSecStorageDao.findByUrl(secUrl);
                if (secondary == null) {
                    return new GetStorageStatsAnswer(cmd, "Can't find the secondary storage:" + secUrl);
                }
                Long totalUsed = _mockVolumeDao.findTotalStorageId(secondary.getId());
                txn.commit();
                return new GetStorageStatsAnswer(cmd, secondary.getCapacity(), totalUsed);
            } else {
                MockStoragePoolVO pool = _mockStoragePoolDao.findByUuid(uuid);
                if (pool == null) {
                    return new GetStorageStatsAnswer(cmd, "Can't find the pool");
                }
                Long totalUsed = _mockVolumeDao.findTotalStorageId(pool.getId());
                if (totalUsed == null) {
                    totalUsed = 0L;
                }
                txn.commit();
                return new GetStorageStatsAnswer(cmd, pool.getCapacity(), totalUsed);
            }
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("DBException during storage stats collection for pool " + uuid, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public ManageSnapshotAnswer ManageSnapshot(ManageSnapshotCommand cmd) {
        String volPath = cmd.getVolumePath();
        MockVolumeVO volume = null;
        MockStoragePoolVO storagePool = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            volume = _mockVolumeDao.findByStoragePathAndType(volPath);
            if (volume == null) {
                return new ManageSnapshotAnswer(cmd, false, "Can't find the volume");
            }
            storagePool = _mockStoragePoolDao.findById(volume.getPoolId());
            if (storagePool == null) {
                return new ManageSnapshotAnswer(cmd, false, "Can't find the storage pooll");
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to perform snapshot", ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        String mountPoint = storagePool.getMountPoint();
        MockVolumeVO snapshot = new MockVolumeVO();

        snapshot.setName(cmd.getSnapshotName());
        snapshot.setPath(mountPoint + cmd.getSnapshotName());
        snapshot.setSize(volume.getSize());
        snapshot.setPoolId(storagePool.getId());
        snapshot.setType(MockVolumeType.SNAPSHOT);
        snapshot.setStatus(Status.DOWNLOADED);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            snapshot = _mockVolumeDao.persist(snapshot);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when saving snapshot " + snapshot, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        return new ManageSnapshotAnswer(cmd, snapshot.getId(), snapshot.getPath(), true, "");
    }

    @Override
    public BackupSnapshotAnswer BackupSnapshot(BackupSnapshotCommand cmd, SimulatorInfo info) {
        // emulate xenserver backupsnapshot, if the base volume is deleted, then
        // backupsnapshot failed
        MockVolumeVO volume = null;
        MockVolumeVO snapshot = null;
        MockSecStorageVO secStorage = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            volume = _mockVolumeDao.findByStoragePathAndType(cmd.getVolumePath());
            if (volume == null) {
                return new BackupSnapshotAnswer(cmd, false, "Can't find base volume: " + cmd.getVolumePath(), null, true);
            }
            String snapshotPath = cmd.getSnapshotUuid();
            snapshot = _mockVolumeDao.findByStoragePathAndType(snapshotPath);
            if (snapshot == null) {
                return new BackupSnapshotAnswer(cmd, false, "can't find snapshot" + snapshotPath, null, true);
            }

            String secStorageUrl = cmd.getSecondaryStorageUrl();
            secStorage = _mockSecStorageDao.findByUrl(secStorageUrl);
            if (secStorage == null) {
                return new BackupSnapshotAnswer(cmd, false, "can't find sec storage" + snapshotPath, null, true);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when backing up snapshot");
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        MockVolumeVO newsnapshot = new MockVolumeVO();
        String name = UUID.randomUUID().toString();
        newsnapshot.setName(name);
        newsnapshot.setPath(secStorage.getMountPoint() + name);
        newsnapshot.setPoolId(secStorage.getId());
        newsnapshot.setSize(snapshot.getSize());
        newsnapshot.setStatus(Status.DOWNLOADED);
        newsnapshot.setType(MockVolumeType.SNAPSHOT);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            snapshot = _mockVolumeDao.persist(snapshot);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when backing up snapshot " + newsnapshot, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        return new BackupSnapshotAnswer(cmd, true, null, newsnapshot.getName(), true);
    }

    @Override
    public CreateVolumeFromSnapshotAnswer CreateVolumeFromSnapshot(CreateVolumeFromSnapshotCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockVolumeVO backSnapshot = null;
        MockStoragePoolVO primary = null;
        try {
            txn.start();
            backSnapshot = _mockVolumeDao.findByName(cmd.getSnapshotUuid());
            if (backSnapshot == null) {
                return new CreateVolumeFromSnapshotAnswer(cmd, false, "can't find the backupsnapshot: " + cmd.getSnapshotUuid(), null);
            }

            primary = _mockStoragePoolDao.findByUuid(cmd.getPrimaryStoragePoolNameLabel());
            if (primary == null) {
                return new CreateVolumeFromSnapshotAnswer(cmd, false, "can't find the primary storage: " + cmd.getPrimaryStoragePoolNameLabel(), null);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when creating volume from snapshot", ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        String uuid = UUID.randomUUID().toString();
        MockVolumeVO volume = new MockVolumeVO();

        volume.setName(uuid);
        volume.setPath(primary.getMountPoint() + uuid);
        volume.setPoolId(primary.getId());
        volume.setSize(backSnapshot.getSize());
        volume.setStatus(Status.DOWNLOADED);
        volume.setType(MockVolumeType.VOLUME);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            _mockVolumeDao.persist(volume);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when creating volume from snapshot " + volume, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        return new CreateVolumeFromSnapshotAnswer(cmd, true, null, volume.getPath());
    }

    @Override
    public Answer Delete(DeleteCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            MockVolumeVO template = _mockVolumeDao.findByStoragePathAndType(cmd.getData().getPath());
            if (template != null) {
                _mockVolumeDao.remove(template.getId());
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when deleting object");
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        return new Answer(cmd);
    }

    @Override
    public Answer SecStorageVMSetup(SecStorageVMSetupCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void preinstallTemplates(String url, long zoneId) {
        MockSecStorageVO storage = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            storage = _mockSecStorageDao.findByUrl(url);

            if (storage == null) {
                storage = new MockSecStorageVO();
                URI uri;
                try {
                    uri = new URI(url);
                } catch (URISyntaxException e) {
                    return;
                }

                String nfsHost = uri.getHost();
                String nfsPath = uri.getPath();
                String path = nfsHost + ":" + nfsPath;
                String dir = "/mnt/" + UUID.nameUUIDFromBytes(path.getBytes()).toString() + File.separator;

                storage.setUrl(url);
                storage.setCapacity(DEFAULT_HOST_STORAGE_SIZE);

                storage.setMountPoint(dir);

                storage = _mockSecStorageDao.persist(storage);

                // preinstall default templates into secondary storage
                long defaultTemplateSize = 2 * 1024 * 1024 * 1024L;
                MockVolumeVO template = new MockVolumeVO();
                template.setName("simulator-domR");
                template.setPath(storage.getMountPoint() + "template/tmpl/1/100/" + UUID.randomUUID().toString());
                template.setPoolId(storage.getId());
                template.setSize(defaultTemplateSize);
                template.setType(MockVolumeType.TEMPLATE);
                template.setStatus(Status.DOWNLOADED);
                template = _mockVolumeDao.persist(template);

                template = new MockVolumeVO();
                template.setName("simulator-Centos");
                template.setPath(storage.getMountPoint() + "template/tmpl/1/111/" + UUID.randomUUID().toString());
                template.setPoolId(storage.getId());
                template.setSize(defaultTemplateSize);
                template.setType(MockVolumeType.TEMPLATE);
                template.setStatus(Status.DOWNLOADED);

                template = _mockVolumeDao.persist(template);

                txn.commit();
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException("Unable to find sec storage at " + url, ex);
        } finally {
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public StoragePoolInfo getLocalStorage(String hostGuid) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockHost host = null;
        MockStoragePoolVO storagePool = null;
        try {
            txn.start();
            host = _mockHostDao.findByGuid(hostGuid);
            storagePool = _mockStoragePoolDao.findByHost(hostGuid);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to find host " + hostGuid, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        if (storagePool == null) {
            String uuid = UUID.randomUUID().toString();
            storagePool = new MockStoragePoolVO();
            storagePool.setUuid(uuid);
            storagePool.setMountPoint("/mnt/" + uuid);
            storagePool.setCapacity(DEFAULT_HOST_STORAGE_SIZE);
            storagePool.setHostGuid(hostGuid);
            storagePool.setStorageType(StoragePoolType.Filesystem);
            txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                txn.start();
                storagePool = _mockStoragePoolDao.persist(storagePool);
                txn.commit();
            } catch (Exception ex) {
                txn.rollback();
                throw new CloudRuntimeException("Error when saving storagePool " + storagePool, ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }
        }
        return new StoragePoolInfo(storagePool.getUuid(), host.getPrivateIpAddress(), storagePool.getMountPoint(), storagePool.getMountPoint(),
            storagePool.getPoolType(), storagePool.getCapacity(), storagePool.getCapacity());
    }

    @Override
    public StoragePoolInfo getLocalStorage(String hostGuid, Long storageSize) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockHost host = null;
        try {
            txn.start();
            host = _mockHostDao.findByGuid(hostGuid);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to find host " + hostGuid, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        if (storageSize == null) {
            storageSize = DEFAULT_HOST_STORAGE_SIZE;
        }
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockStoragePoolVO storagePool = null;
        try {
            txn.start();
            storagePool = _mockStoragePoolDao.findByHost(hostGuid);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when finding storagePool " + storagePool, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
        if (storagePool == null) {
            String uuid = UUID.randomUUID().toString();
            storagePool = new MockStoragePoolVO();
            storagePool.setUuid(uuid);
            storagePool.setMountPoint("/mnt/" + uuid);
            storagePool.setCapacity(storageSize);
            storagePool.setHostGuid(hostGuid);
            storagePool.setStorageType(StoragePoolType.Filesystem);
            txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                txn.start();
                storagePool = _mockStoragePoolDao.persist(storagePool);
                txn.commit();
            } catch (Exception ex) {
                txn.rollback();
                throw new CloudRuntimeException("Error when saving storagePool " + storagePool, ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }
        }
        return new StoragePoolInfo(storagePool.getUuid(), host.getPrivateIpAddress(), storagePool.getMountPoint(), storagePool.getMountPoint(),
            storagePool.getPoolType(), storagePool.getCapacity(), 0);
    }

    @Override
    public CreatePrivateTemplateAnswer CreatePrivateTemplateFromSnapshot(CreatePrivateTemplateFromSnapshotCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockVolumeVO snapshot = null;
        MockSecStorageVO sec = null;
        try {
            txn.start();
            String snapshotUUId = cmd.getSnapshotUuid();
            snapshot = _mockVolumeDao.findByName(snapshotUUId);
            if (snapshot == null) {
                snapshotUUId = cmd.getSnapshotName();
                snapshot = _mockVolumeDao.findByName(snapshotUUId);
                if (snapshot == null) {
                    return new CreatePrivateTemplateAnswer(cmd, false, "can't find snapshot:" + snapshotUUId);
                }
            }

            sec = _mockSecStorageDao.findByUrl(cmd.getSecondaryStorageUrl());
            if (sec == null) {
                return new CreatePrivateTemplateAnswer(cmd, false, "can't find secondary storage");
            }
            txn.commit();
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        MockVolumeVO template = new MockVolumeVO();
        String uuid = UUID.randomUUID().toString();
        template.setName(uuid);
        template.setPath(sec.getMountPoint() + uuid);
        template.setPoolId(sec.getId());
        template.setSize(snapshot.getSize());
        template.setStatus(Status.DOWNLOADED);
        template.setType(MockVolumeType.TEMPLATE);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            template = _mockVolumeDao.persist(template);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when saving template " + template, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        return new CreatePrivateTemplateAnswer(cmd, true, "", template.getName(), template.getSize(), template.getSize(), template.getName(), ImageFormat.QCOW2);
    }

    @Override
    public Answer ComputeChecksum(ComputeChecksumCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            MockVolumeVO volume = _mockVolumeDao.findByName(cmd.getTemplatePath());
            if (volume == null) {
                return new Answer(cmd, false, "can't find volume:" + cmd.getTemplatePath());
            }
            String md5 = null;
            try {
                MessageDigest md = MessageDigest.getInstance("md5");
                md5 = String.format("%032x", new BigInteger(1, md.digest(cmd.getTemplatePath().getBytes())));
            } catch (NoSuchAlgorithmException e) {
                s_logger.debug("failed to gernerate md5:" + e.toString());
            }
            txn.commit();
            return new Answer(cmd, true, md5);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public CreatePrivateTemplateAnswer CreatePrivateTemplateFromVolume(CreatePrivateTemplateFromVolumeCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockVolumeVO volume = null;
        MockSecStorageVO sec = null;
        try {
            txn.start();
            volume = _mockVolumeDao.findByStoragePathAndType(cmd.getVolumePath());
            if (volume == null) {
                return new CreatePrivateTemplateAnswer(cmd, false, "can't find volume" + cmd.getVolumePath());
            }

            sec = _mockSecStorageDao.findByUrl(cmd.getSecondaryStorageUrl());
            if (sec == null) {
                return new CreatePrivateTemplateAnswer(cmd, false, "can't find secondary storage");
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when creating private template from volume");
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        MockVolumeVO template = new MockVolumeVO();
        String uuid = UUID.randomUUID().toString();
        template.setName(uuid);
        template.setPath(sec.getMountPoint() + uuid);
        template.setPoolId(sec.getId());
        template.setSize(volume.getSize());
        template.setStatus(Status.DOWNLOADED);
        template.setType(MockVolumeType.TEMPLATE);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            template = _mockVolumeDao.persist(template);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when persisting template " + template.getName(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        return new CreatePrivateTemplateAnswer(cmd, true, "", template.getName(), template.getSize(), template.getSize(), template.getName(), ImageFormat.QCOW2);
    }

    @Override
    public CopyVolumeAnswer CopyVolume(CopyVolumeCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        boolean toSecondaryStorage = cmd.toSecondaryStorage();
        MockSecStorageVO sec = null;
        MockStoragePoolVO primaryStorage = null;
        try {
            txn.start();
            sec = _mockSecStorageDao.findByUrl(cmd.getSecondaryStorageURL());
            if (sec == null) {
                return new CopyVolumeAnswer(cmd, false, "can't find secondary storage", null, null);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when accessing secondary at " + cmd.getSecondaryStorageURL(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            primaryStorage = _mockStoragePoolDao.findByUuid(cmd.getPool().getUuid());
            if (primaryStorage == null) {
                return new CopyVolumeAnswer(cmd, false, "Can't find primary storage", null, null);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when accessing primary at " + cmd.getPool(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        MockVolumeVO volume = null;
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            volume = _mockVolumeDao.findByStoragePathAndType(cmd.getVolumePath());
            if (volume == null) {
                return new CopyVolumeAnswer(cmd, false, "can't find volume" + cmd.getVolumePath(), null, null);
            }
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when accessing volume at " + cmd.getVolumePath(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        String name = UUID.randomUUID().toString();
        if (toSecondaryStorage) {
            MockVolumeVO vol = new MockVolumeVO();
            vol.setName(name);
            vol.setPath(sec.getMountPoint() + name);
            vol.setPoolId(sec.getId());
            vol.setSize(volume.getSize());
            vol.setStatus(Status.DOWNLOADED);
            vol.setType(MockVolumeType.VOLUME);
            txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                txn.start();
                vol = _mockVolumeDao.persist(vol);
                txn.commit();
            } catch (Exception ex) {
                txn.rollback();
                throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when persisting volume " + vol.getName(), ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }
            return new CopyVolumeAnswer(cmd, true, null, sec.getMountPoint(), vol.getPath());
        } else {
            MockVolumeVO vol = new MockVolumeVO();
            vol.setName(name);
            vol.setPath(primaryStorage.getMountPoint() + name);
            vol.setPoolId(primaryStorage.getId());
            vol.setSize(volume.getSize());
            vol.setStatus(Status.DOWNLOADED);
            vol.setType(MockVolumeType.VOLUME);
            txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                txn.start();
                vol = _mockVolumeDao.persist(vol);
                txn.commit();
            } catch (Exception ex) {
                txn.rollback();
                throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when persisting volume " + vol.getName(), ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }
            return new CopyVolumeAnswer(cmd, true, null, primaryStorage.getMountPoint(), vol.getPath());
        }
    }

    @Override
    public UploadStatusAnswer getUploadStatus(UploadStatusCommand cmd) {
        return new UploadStatusAnswer(cmd, UploadStatus.COMPLETED);
    }

    @Override public Answer handleConfigDriveIso(HandleConfigDriveIsoCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockSecStorageVO sec;
        try {
            txn.start();
            sec = _mockSecStorageDao.findByUrl(cmd.getDestStore().getUrl());
            if (sec == null) {
                return new Answer(cmd, false, "can't find secondary storage");
            }

            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Error when creating config drive.");
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        MockVolumeVO template = new MockVolumeVO();
        String uuid = UUID.randomUUID().toString();
        template.setName(uuid);
        template.setPath(sec.getMountPoint() + cmd.getIsoFile());
        template.setPoolId(sec.getId());
        template.setSize((long)(Math.random() * 200L) + 200L);
        template.setStatus(Status.DOWNLOADED);
        template.setType(MockVolumeType.ISO);
        txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            template = _mockVolumeDao.persist(template);
            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Encountered " + ex.getMessage() + " when persisting config drive " + template.getName(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        return new Answer(cmd);
    }
}
