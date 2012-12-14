/**
 * 
 */
package org.apache.cloudstack.engine.provisioning.test;

import java.util.HashMap;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.ZoneEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.db.dao.DataCenterDao;
import org.apache.cloudstack.engine.service.api.ProvisioningService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.engine.datacenter.entity.api.db.DataCenterVO;
import com.cloud.dc.DataCenter.NetworkType;

import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/resource/provisioningContext.xml")
public class ProvisioningTest extends TestCase {
	
	@Inject
	ProvisioningService service;
	
	@Inject
	DataCenterDao dcDao;
	
    @Before
	public void setUp() {
    	
    	DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24", 
				null, null, NetworkType.Basic, null, null, true,  true);
    	
		Mockito.when(dcDao.findById(Mockito.anyLong())).thenReturn(dc);
		Mockito.when(dcDao.persist((DataCenterVO) Mockito.anyObject())).thenReturn(dc);    	    	
    }

	private void registerAndEnableZone() {
		ZoneEntity zone = service.registerZone("47547648", "owner", null, new HashMap<String, String>());
		State state = zone.getState();
		System.out.println("state:"+state);
		boolean result = zone.enable();
		System.out.println("state:"+zone.getState());
	}

	@Test
	public void testProvisioning() {
		registerAndEnableZone();
	}


}
