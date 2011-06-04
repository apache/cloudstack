package com.cloud.vm.dao;

import junit.framework.TestCase;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;


public class UserVmDaoImplTest extends TestCase {
    public void testPersist() {
        UserVmDao dao = ComponentLocator.inject(UserVmDaoImpl.class);
        
        dao.expunge(1000l);
        
        UserVmVO vo = new UserVmVO(1000l, "instancename", "displayname", 1, HypervisorType.XenServer, 1, true, true, 1, 1, 1, "userdata", "name");
        dao.persist(vo);
        
        vo = dao.findById(1000l);
        assert (vo.getType() == VirtualMachine.Type.User) : "Incorrect type " + vo.getType();
    }

}
