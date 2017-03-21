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
package org.apache.cloudstack.network.element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/lb_element.xml")
public class VpcInlineLbElementServiceTest {
    //The interface to test
    @Inject
    private VpcInlineLoadBalancerElementService _lbElSvc;

    //Mocked interfaces
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private VirtualRouterProviderDao _vrProviderDao;
    @Inject
    private PhysicalNetworkServiceProviderDao _pNtwkProviderDao;
    @Inject
    private LoadBalancerDao _loadBalancerDao;
    @Inject
    private IPAddressDao _ipAddressDao;

    private long validElId = 1L;
    private long nonExistingElId = 2L;
    private long invalidElId = 3L; //not of VirtualRouterProviderType

    private long validProviderId = 1L;
    private long nonExistingProviderId = 2L;
    private long invalidProviderId = 3L;

    @Before
    public void setUp() {
        ComponentContext.initComponentsLifeCycle();
        VirtualRouterProviderVO validElement = new VirtualRouterProviderVO(1, Type.VpcInlineLbVm);
        VirtualRouterProviderVO invalidElement = new VirtualRouterProviderVO(1, Type.VirtualRouter);

        Mockito.when(_vrProviderDao.findById(validElId)).thenReturn(validElement);
        Mockito.when(_vrProviderDao.findById(invalidElId)).thenReturn(invalidElement);

        Mockito.when(_vrProviderDao.persist(validElement)).thenReturn(validElement);

        Mockito.when(_vrProviderDao.findByNspIdAndType(validProviderId, Type.VpcInlineLbVm)).thenReturn(validElement);

        PhysicalNetworkServiceProviderVO validProvider = new PhysicalNetworkServiceProviderVO(1, "VpcInlineLoadBalancerElement");
        PhysicalNetworkServiceProviderVO invalidProvider = new PhysicalNetworkServiceProviderVO(1, "Invalid name!");

        Mockito.when(_pNtwkProviderDao.findById(validProviderId)).thenReturn(validProvider);
        Mockito.when(_pNtwkProviderDao.findById(invalidProviderId)).thenReturn(invalidProvider);

        Mockito.when(_vrProviderDao.persist(Mockito.any(VirtualRouterProviderVO.class))).thenReturn(validElement);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void findNonExistingVm() {
        String expectedExcText = null;
        try {
            _lbElSvc.getVpcInlineLoadBalancerElement(nonExistingElId);
        } catch (InvalidParameterValueException e) {
            expectedExcText = e.getMessage();
            throw e;
        } finally {
            assertEquals("Test failed. The non-existing intenral lb provider was found"
        + expectedExcText, expectedExcText, "Unable to find VpcInlineLoadBalancerElementService by id");
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void findInvalidVm() {
        String expectedExcText = null;
        try {
            _lbElSvc.getVpcInlineLoadBalancerElement(invalidElId);
        } catch (InvalidParameterValueException e) {
            expectedExcText = e.getMessage();
            throw e;
        } finally {
            assertEquals("Test failed. The non-existing intenral lb provider was found"
                    + expectedExcText, expectedExcText, "Unable to find VpcInlineLoadBalancerElementService by id");
        }
    }

    @Test
    public void findValidVm() {
        VirtualRouterProvider provider = null;
        try {
            provider = _lbElSvc.getVpcInlineLoadBalancerElement(validElId);
        } finally {
            assertNotNull("Test failed. Couldn't find the VR provider by the valid id",provider);
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void configureNonExistingVm() {
        _lbElSvc.configureVpcInlineLoadBalancerElement(nonExistingElId, true);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void ConfigureInvalidVm() {
        _lbElSvc.configureVpcInlineLoadBalancerElement(invalidElId, true);
    }

    @Test
    public void enableProvider() {
        VirtualRouterProvider provider = null;
        try {
            provider = _lbElSvc.configureVpcInlineLoadBalancerElement(validElId, true);
        } finally {
            assertNotNull("Test failed. Couldn't find the VR provider by the valid id ",provider);
            assertTrue("Test failed. The provider wasn't eanbled ", provider.isEnabled());
        }
    }

    @Test
    public void disableProvider() {
        VirtualRouterProvider provider = null;
        try {
            provider = _lbElSvc.configureVpcInlineLoadBalancerElement(validElId, false);
        } finally {
            assertNotNull("Test failed. Couldn't find the VR provider by the valid id ",provider);
            assertFalse("Test failed. The provider wasn't disabled ", provider.isEnabled());
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void addToNonExistingProvider() {
        _lbElSvc.addVpcInlineLoadBalancerElement(nonExistingProviderId);
    }

    public void addToInvalidProvider() {
        _lbElSvc.addVpcInlineLoadBalancerElement(invalidProviderId);
    }

    @Test
    public void addToExistingProvider() {
        _lbElSvc.addVpcInlineLoadBalancerElement(validProviderId);
    }
}


