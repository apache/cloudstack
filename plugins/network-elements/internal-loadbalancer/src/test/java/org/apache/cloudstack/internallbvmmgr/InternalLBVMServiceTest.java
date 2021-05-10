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
package org.apache.cloudstack.internallbvmmgr;

import static org.mockito.ArgumentMatchers.nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;

import junit.framework.TestCase;

/**
 * Set of unittests for InternalLoadBalancerVMService
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/lb_svc.xml")
public class InternalLBVMServiceTest extends TestCase {
    //The interface to test
    @Inject
    InternalLoadBalancerVMService _lbVmSvc;

    //Mocked interfaces
    @Inject
    AccountManager _accountMgr;
    @Inject
    ServiceOfferingDao _svcOffDao;
    @Inject
    DomainRouterDao _domainRouterDao;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    AccountDao _accountDao;

    long validVmId = 1L;
    long nonExistingVmId = 2L;
    long nonInternalLbVmId = 3L;

    @Override
    @Before
    public void setUp() {
        //mock system offering creation as it's used by configure() method called by initComponentsLifeCycle
        Mockito.when(_accountMgr.getAccount(1L)).thenReturn(new AccountVO());
        ServiceOfferingVO off = new ServiceOfferingVO("alena", 1, 1,
                1, 1, 1, false, "alena", Storage.ProvisioningType.THIN, false, false, null, false, VirtualMachine.Type.InternalLoadBalancerVm, false);
        off = setId(off, 1);
        List<ServiceOfferingVO> list = new ArrayList<ServiceOfferingVO>();
        list.add(off);
        list.add(off);
        Mockito.when(_svcOffDao.createSystemServiceOfferings(nullable(String.class), nullable(String.class), nullable(Integer.class), nullable(Integer.class), nullable(Integer.class),
                nullable(Integer.class), nullable(Integer.class), nullable(Boolean.class), nullable(String.class), nullable(ProvisioningType.class), nullable(Boolean.class),
                nullable(String.class), nullable(Boolean.class), nullable(VirtualMachine.Type.class), nullable(Boolean.class))).thenReturn(list);

        ComponentContext.initComponentsLifeCycle();

        Mockito.when(_accountMgr.getSystemUser()).thenReturn(new UserVO(1));
        Mockito.when(_accountMgr.getSystemAccount()).thenReturn(new AccountVO(2));
        Mockito.when(_accountDao.findByIdIncludingRemoved(Matchers.anyLong())).thenReturn(new AccountVO(2));
        CallContext.register(_accountMgr.getSystemUser(), _accountMgr.getSystemAccount());

        final DomainRouterVO validVm =
                new DomainRouterVO(validVmId, off.getId(), 1, "alena", 1, HypervisorType.XenServer, 1, 1, 1, 1, false, null, false, false,
                        VirtualMachine.Type.InternalLoadBalancerVm, null);
        validVm.setRole(Role.INTERNAL_LB_VM);
        final DomainRouterVO nonInternalLbVm =
                new DomainRouterVO(validVmId, off.getId(), 1, "alena", 1, HypervisorType.XenServer, 1, 1, 1, 1, false, null, false, false,
                        VirtualMachine.Type.DomainRouter, null);
        nonInternalLbVm.setRole(Role.VIRTUAL_ROUTER);

        Mockito.when(_domainRouterDao.findById(validVmId)).thenReturn(validVm);
        Mockito.when(_domainRouterDao.findById(nonExistingVmId)).thenReturn(null);
        Mockito.when(_domainRouterDao.findById(nonInternalLbVmId)).thenReturn(nonInternalLbVm);
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    //TESTS FOR START COMMAND

    @Test(expected = InvalidParameterValueException.class)
    public void startNonExistingVm() {
        final String expectedExcText = null;
        try {
            _lbVmSvc.startInternalLbVm(nonExistingVmId, _accountMgr.getAccount(1L), 1L);
        } catch (final StorageUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InsufficientCapacityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void startNonInternalLbVmVm() {
        final String expectedExcText = null;
        try {
            _lbVmSvc.startInternalLbVm(nonInternalLbVmId, _accountMgr.getAccount(1L), 1L);
        } catch (final StorageUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InsufficientCapacityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void startValidLbVmVm() {
        VirtualRouter vr = null;
        try {
            vr = _lbVmSvc.startInternalLbVm(validVmId, _accountMgr.getAccount(1L), 1L);
        } catch (final StorageUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InsufficientCapacityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            assertNotNull("Internal LB vm is null which means it failed to start " + vr, vr);
        }
    }

    //TEST FOR STOP COMMAND
    @Test(expected = InvalidParameterValueException.class)
    public void stopNonExistingVm() {
        final String expectedExcText = null;
        try {
            _lbVmSvc.stopInternalLbVm(nonExistingVmId, false, _accountMgr.getAccount(1L), 1L);
        } catch (final StorageUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void stopNonInternalLbVmVm() {
        final String expectedExcText = null;
        try {
            _lbVmSvc.stopInternalLbVm(nonInternalLbVmId, false, _accountMgr.getAccount(1L), 1L);
        } catch (final StorageUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void stopValidLbVmVm() {
        VirtualRouter vr = null;
        try {
            vr = _lbVmSvc.stopInternalLbVm(validVmId, false, _accountMgr.getAccount(1L), 1L);
        } catch (final StorageUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ConcurrentOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            assertNotNull("Internal LB vm is null which means it failed to stop " + vr, vr);
        }
    }

    private static ServiceOfferingVO setId(final ServiceOfferingVO vo, final long id) {
        final ServiceOfferingVO voToReturn = vo;
        final Class<?> c = voToReturn.getClass();
        try {
            final Field f = c.getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (final NoSuchFieldException ex) {
            return null;
        } catch (final IllegalAccessException ex) {
            return null;
        }

        return voToReturn;
    }
}
