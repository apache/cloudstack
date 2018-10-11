package com.cloud.storage.download.managementserver;

import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.download.managementserver.system.HttpSystemTemplateDownloader;
import com.cloud.storage.mount.MountManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagementServerDownloader {
    private static final Logger s_logger = Logger.getLogger(ManagementServerDownloader.class);

    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;

    @Inject
    VMTemplatePoolDao vmTemplatePoolDao;

    @Inject
    private VMTemplateDao templateDao;

    @Inject
    private TemplateDataStoreDao vmTemplateStoreDao;

    @Inject
    private MountManager mountManager;

    private static final ExecutorService THREAD_SERVICE = Executors.newCachedThreadPool(new NamedThreadFactory("ManagementServerDownloader"));

    public void download(VirtualMachineTemplate tmpl, DataObject template) {
        downloadFromUrlToNfs(tmpl, template, template.getUuid());
    }


    public void downloadFromUrlToNfs(VirtualMachineTemplate tmpl, DataObject template, String fileName) {
        Runnable downloadFile = () -> {
            Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(template.getDataStore().getId());
            String mountPoint = mountManager.getMountPoint(template.getDataStore().getUri(), nfsVersion);
            VMTemplateVO templateVO = Optional.ofNullable(templateDao.findById(template.getId()))
                .orElseThrow(() -> new CloudRuntimeException(String.format("Unable to find template by id %d.%n", template.getId())));
            HttpSystemTemplateDownloader downloader = new HttpSystemTemplateDownloader(tmpl, templateVO, mountPoint);
            if (downloader.downloadTemplate()) {
                if (downloader.extractAndInstallDownloadedTemplate()) {
                    TemplateDownloader.TemplateInformation info = downloader.getTemplateInformation();
                    try(TransactionLegacy txn = TransactionLegacy.open(ManagementServerDownloader.class.getName())) {
                        persistTemplate(template, templateVO, info);
                        persistTemplateStorePoolRef(tmpl, template.getDataStore().getId(), info);
                        persistTemplateStoreRef(template, info);
                    }
                } else {
                    throw new CloudRuntimeException(String.format("Failed to extract template %s from url %s", fileName, tmpl.getUrl()));
                }
            } else {
                throw new CloudRuntimeException(String.format("Failed to download template %s from url %s", fileName, tmpl.getUrl()));
            }
        };
        THREAD_SERVICE.submit(downloadFile);
    }

    private void persistTemplate(DataObject template, VMTemplateVO templateVO, TemplateDownloader.TemplateInformation info) {
        templateVO.setSize(info.getSize());
        templateDao.update(template.getId(), templateVO);
    }

    private void persistTemplateStoreRef(DataObject template, TemplateDownloader.TemplateInformation info) {
        DataStore store = template.getDataStore();
        TemplateDataStoreVO vmTemplateStore = vmTemplateStoreDao.findByStoreTemplate(store.getId(), template.getId());
        if (vmTemplateStore == null) {
            vmTemplateStore = new TemplateDataStoreVO(store.getId(), template.getId(), new Date(), 100,
                    VMTemplateStorageResourceAssoc.Status.DOWNLOADED, info.getInstallPath(), null, null, info.getInstallPath(), template.getUri());
            vmTemplateStore.setDataStoreRole(store.getRole());
            vmTemplateStoreDao.persist(vmTemplateStore);
        } else {
            vmTemplateStore.setDownloadPercent(100);
            vmTemplateStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            vmTemplateStore.setSize(info.getSize());
            vmTemplateStore.setPhysicalSize(info.getSize());
            vmTemplateStore.setLastUpdated(new Date());
            vmTemplateStore.setInstallPath(info.getInstallPath());
            vmTemplateStore.setLocalDownloadPath(info.getLocalPath());
            vmTemplateStore.setDownloadUrl(template.getUri());
            vmTemplateStore.setState(ObjectInDataStoreStateMachine.State.Ready);
            vmTemplateStoreDao.update(vmTemplateStore.getId(), vmTemplateStore);
        }
    }

    private void persistTemplateStorePoolRef(VirtualMachineTemplate tmpl, long poolId, TemplateDownloader.TemplateInformation info) {
        VMTemplateStoragePoolVO sPoolRef = vmTemplatePoolDao.findByPoolTemplate(poolId, tmpl.getId());
        if (sPoolRef == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Not found (templateId:" + tmpl.getId() + " poolId: " + poolId + ") in template_spool_ref, persisting it");
            }
            sPoolRef = new VMTemplateStoragePoolVO(poolId, tmpl.getId());
            sPoolRef.setDownloadPercent(100);
            sPoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sPoolRef.setState(ObjectInDataStoreStateMachine.State.Ready);
            sPoolRef.setTemplateSize(info.getSize());
            sPoolRef.setLocalDownloadPath(info.getLocalPath());
            sPoolRef.setInstallPath(info.getInstallPath());
            vmTemplatePoolDao.persist(sPoolRef);
        } else {
            sPoolRef.setDownloadPercent(100);
            sPoolRef.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sPoolRef.setState(ObjectInDataStoreStateMachine.State.Ready);
            sPoolRef.setTemplateSize(info.getSize());
            sPoolRef.setLocalDownloadPath(info.getLocalPath());
            sPoolRef.setInstallPath(info.getInstallPath());
            vmTemplatePoolDao.update(sPoolRef.getId(), sPoolRef);
        }
    }
}
