package com.cloud.agent.manager;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.MockComponentLocator;
import com.cloud.utils.db.DbTestUtils;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDaoImpl;

import junit.framework.TestCase;

public class SearchCriteria2Test extends TestCase {
    private static final Logger s_logger = Logger.getLogger(SearchCriteria2Test.class);
    
    @Override
    @Before
    public void setUp() throws Exception {
        DbTestUtils.executeScript("cleanup.sql", false, true);
        MockComponentLocator locator = new MockComponentLocator("management-server");
        locator.addDao("HostDao", HostDaoImpl.class);
        locator.addDao("VmInstance", VMInstanceDaoImpl.class);
        s_logger.debug("Finding sample data from 2.1.12");
        DbTestUtils.executeScript("fake", false, true);
    }
    
    public void testSearch() {
    	ComponentLocator locator = ComponentLocator.getCurrentLocator();
    	
    	HostDao _hostDao = locator.inject(HostDaoImpl.class);
    	VMInstanceDao _vmDao = locator.inject(VMInstanceDaoImpl.class);
    	
    	
    	s_logger.debug("Test seraching host:");
    	SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
    	sc.addAnd(sc.getEntity().getStatus(), Op.EQ, Status.Disconnected);
    	List<HostVO> ups = sc.list();
    	for (HostVO vo : ups) {
    		s_logger.info("Host id: " + vo.getId() + " is Disconnected");
    	}
    	
    	SearchCriteriaService<VMInstanceVO, VMInstanceVO> sc1 = SearchCriteria2.create(VMInstanceVO.class);
    	sc1.addAnd(sc1.getEntity().getState(), Op.EQ, VirtualMachine.State.Running);
    	List<VMInstanceVO> vms = sc1.list();
    	for (VMInstanceVO vm : vms) {
    		s_logger.info("Vm name:" + vm.getInstanceName());
    	}
    	
    	sc1 = SearchCriteria2.create(VMInstanceVO.class);
    	sc1.addAnd(sc1.getEntity().getInstanceName(), Op.EQ, "s-1-TEST");
    	VMInstanceVO vo = sc1.find();
    	s_logger.info("SSVM name is " + vo.getInstanceName());
    	
    	SearchCriteriaService<HostVO, Long> sc3 = SearchCriteria2.create(HostVO.class, Long.class);
    	sc3.selectField(sc3.getEntity().getId());
    	sc3.addAnd(sc3.getEntity().getStatus(), Op.EQ, Status.Disconnected);
    	sc3.addAnd(sc3.getEntity().getType(), Op.EQ, Host.Type.Routing);
    	List<Long> hostIds = sc3.list();
    	for (Long id : hostIds) {
    		s_logger.info("Host Id is " + id);
    	}
    }
    
    @Override
    @After
    public void tearDown() throws Exception {
    }
    
}
