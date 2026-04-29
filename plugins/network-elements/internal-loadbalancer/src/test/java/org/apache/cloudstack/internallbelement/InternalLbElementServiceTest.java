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
package org.apache.cloudstack.internallbelement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/lb_element.xml")
public class InternalLbElementServiceTest {
    //The interface to test
    @Inject
    InternalLoadBalancerElementService _lbElSvc;

    //Mocked interfaces
    @Inject
    AccountManager _accountMgr;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNtwkProviderDao;

    long validElId = 1L;
    long nonExistingElId = 2L;
    long invalidElId = 3L; //not of VirtualRouterProviderType

    long validProviderId = 1L;
    long nonExistingProviderId = 2L;
    long invalidProviderId = 3L;

    @Before
    public void setUp() {

        ComponentContext.initComponentsLifeCycle();
        VirtualRouterProviderVO validElement = new VirtualRouterProviderVO(1, Type.InternalLbVm);
        VirtualRouterProviderVO invalidElement = new VirtualRouterProviderVO(1, Type.VirtualRouter);

        Mockito.when(_vrProviderDao.findById(validElId)).thenReturn(validElement);
        Mockito.when(_vrProviderDao.findById(invalidElId)).thenReturn(invalidElement);

        Mockito.when(_vrProviderDao.persist(validElement)).thenReturn(validElement);

        Mockito.when(_vrProviderDao.findByNspIdAndType(validProviderId, Type.InternalLbVm)).thenReturn(validElement);

        PhysicalNetworkServiceProviderVO validProvider = new PhysicalNetworkServiceProviderVO(1, "InternalLoadBalancerElement");
        PhysicalNetworkServiceProviderVO invalidProvider = new PhysicalNetworkServiceProviderVO(1, "Invalid name!");

        Mockito.when(_pNtwkProviderDao.findById(validProviderId)).thenReturn(validProvider);
        Mockito.when(_pNtwkProviderDao.findById(invalidProviderId)).thenReturn(invalidProvider);

        Mockito.when(_vrProviderDao.persist(ArgumentMatchers.any(VirtualRouterProviderVO.class))).thenReturn(validElement);
    }

    //TESTS FOR getInternalLoadBalancerElement METHOD

    @Test(expected = InvalidParameterValueException.class)
    public void findNonExistingVm() {
        String expectedExcText = null;
        try {
            _lbElSvc.getInternalLoadBalancerElement(nonExistingElId);
        } catch (InvalidParameterValueException e) {
            expectedExcText = e.getMessage();
            throw e;
        } finally {
            assertEquals("Test failed. The non-existing intenral lb provider was found" + expectedExcText, expectedExcText,
                "Unable to find InternalLoadBalancerElementService by id");
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void findInvalidVm() {
        String expectedExcText = null;
        try {
            _lbElSvc.getInternalLoadBalancerElement(invalidElId);
        } catch (InvalidParameterValueException e) {
            expectedExcText = e.getMessage();
            throw e;
        } finally {
            assertEquals("Test failed. The non-existing intenral lb provider was found" + expectedExcText, expectedExcText,
                "Unable to find InternalLoadBalancerElementService by id");
        }
    }

    @Test
    public void findValidVm() {
        VirtualRouterProvider provider = null;
        try {
            provider = _lbElSvc.getInternalLoadBalancerElement(validElId);
        } finally {
            assertNotNull("Test failed. Couldn't find the VR provider by the valid id", provider);
        }
    }

    //TESTS FOR configureInternalLoadBalancerElement METHOD

    @Test(expected = InvalidParameterValueException.class)
    public void configureNonExistingVm() {

        _lbElSvc.configureInternalLoadBalancerElement(nonExistingElId, true);

    }

    @Test(expected = InvalidParameterValueException.class)
    public void ConfigureInvalidVm() {
        _lbElSvc.configureInternalLoadBalancerElement(invalidElId, true);
    }

    @Test
    public void enableProvider() {
        VirtualRouterProvider provider = null;
        try {
            provider = _lbElSvc.configureInternalLoadBalancerElement(validElId, true);
        } finally {
            assertNotNull("Test failed. Couldn't find the VR provider by the valid id ", provider);
            assertTrue("Test failed. The provider wasn't eanbled ", provider.isEnabled());
        }
    }

    @Test
    public void disableProvider() {
        VirtualRouterProvider provider = null;
        try {
            provider = _lbElSvc.configureInternalLoadBalancerElement(validElId, false);
        } finally {
            assertNotNull("Test failed. Couldn't find the VR provider by the valid id ", provider);
            assertFalse("Test failed. The provider wasn't disabled ", provider.isEnabled());
        }
    }

    //TESTS FOR addInternalLoadBalancerElement METHOD

    @Test(expected = InvalidParameterValueException.class)
    public void addToNonExistingProvider() {

        _lbElSvc.addInternalLoadBalancerElement(nonExistingProviderId);

    }

    public void addToInvalidProvider() {
        _lbElSvc.addInternalLoadBalancerElement(invalidProviderId);
    }

    @Test
    public void addToExistingProvider() {
        _lbElSvc.addInternalLoadBalancerElement(validProviderId);
    }

}
