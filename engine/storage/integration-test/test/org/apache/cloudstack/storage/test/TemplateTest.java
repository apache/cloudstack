package org.apache.cloudstack.storage.test;

import static org.testng.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.MockLocalNfsSecondaryStorageResource;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.download.DownloadMonitorImpl;
import com.cloud.template.HypervisorTemplateAdapter;
import com.cloud.template.TemplateAdapter;
import com.cloud.utils.component.ComponentContext;

@ContextConfiguration(locations={"classpath:/storageContext.xml"})

public class TemplateTest extends CloudStackTestNGBase {
	@Inject
	DataCenterDao dcDao;
	ImageStoreVO imageStore;
	@Inject
	ImageStoreDao imageStoreDao;
	@Inject
	TemplateService templateSvr;
	@Inject
	VMTemplateDao templateDao;
	@Inject
	TemplateDataFactory templateFactory;
	@Inject
	DataStoreManager dataStoreMgr;
	@Inject
	EndPointSelector epSelector;
	@Inject
	DownloadMonitorImpl downloadMonitor;


	long dcId;
	long templateId;

	@Test(priority = -1)
	public void setUp() {
		ComponentContext.initComponentsLifeCycle();
	      System.setProperty("paths.script", this.getScriptPath());
		//create data center
		DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
				null, null, NetworkType.Basic, null, null, true,  true, null, null);
		dc = dcDao.persist(dc);
		dcId = dc.getId();

		imageStore = new ImageStoreVO();
		imageStore.setName("test");
		imageStore.setDataCenterId(dcId);
		imageStore.setProviderName(DataStoreProvider.NFS_IMAGE);
		imageStore.setRole(DataStoreRole.Image);
		imageStore.setUrl(this.getSecondaryStorage());
		imageStore.setUuid(UUID.randomUUID().toString());
		imageStore.setProtocol("nfs");
		imageStore = imageStoreDao.persist(imageStore);

		VMTemplateVO image = new VMTemplateVO();
		image.setTemplateType(TemplateType.USER);
		image.setUrl(this.getTemplateUrl());
		image.setUniqueName(UUID.randomUUID().toString());
		image.setName(UUID.randomUUID().toString());
		image.setPublicTemplate(true);
		image.setFeatured(true);
		image.setRequiresHvm(true);
		image.setBits(64);
		image.setFormat(Storage.ImageFormat.VHD);
		image.setEnablePassword(true);
		image.setEnableSshKey(true);
		image.setGuestOSId(1);
		image.setBootable(true);
		image.setPrepopulate(true);
		image.setCrossZones(true);
		image.setExtractable(true);


		//image.setImageDataStoreId(storeId);
		image = templateDao.persist(image);
		templateId = image.getId();

        // inject mockito
        LocalHostEndpoint ep = new LocalHostEndpoint();
        ep.setResource(new MockLocalNfsSecondaryStorageResource());
        Mockito.when(epSelector.select(Mockito.any(DataObject.class))).thenReturn(ep);
        Mockito.when(epSelector.select(Mockito.any(DataStore.class))).thenReturn(ep);
	}

	@Test
	public void registerTemplate() {
		TemplateInfo template = templateFactory.getTemplate(templateId);
		DataStore store = dataStoreMgr.getImageStore(dcId);
		AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
		templateSvr.createTemplateAsync(template, store, future);
        try {
            TemplateApiResult result = future.get();
            assertTrue(result.isSuccess(), "failed to register template: " + result.getResult());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false, e.getMessage());
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
             assertTrue(false, e.getMessage());
        }
	}

   // @Test
    public void deleteTemplate() {
        TemplateInfo template = templateFactory.getTemplate(templateId);
        DataStore store = dataStoreMgr.getImageStore(dcId);
        AsyncCallFuture<TemplateApiResult> future = new AsyncCallFuture<TemplateApiResult>();
        templateSvr.deleteTemplateAsync(template);
        try {
            TemplateApiResult result = future.get();
            assertTrue(result.isSuccess(), "failed to delete template: " + result.getResult());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assertTrue(false, e.getMessage());
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
             assertTrue(false, e.getMessage());
        }
    }
}
