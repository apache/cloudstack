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
package com.cloud.vpc;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network.Service;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManagerImpl;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/VpcTestContext.xml")
public class VpcApiUnitTest extends TestCase{
    @Inject VpcManagerImpl _vpcService = null;

    @Before
    public void setUp() throws Exception {
        ComponentContext.initComponentsLifeCycle();
    }
    
    
    @Test
    public void getActiveVpc() {
        //test for active vpc
        boolean result = false;
        Vpc vpc = null;
        try {
            List<String> svcs = new ArrayList<String>();
            svcs.add(Service.SourceNat.getName());
            vpc = _vpcService.getActiveVpc(1);
            if (vpc != null) {
                result = true;
            }
        }  catch (Exception ex) {
        } finally {
            assertTrue("Get active Vpc: TEST FAILED, active vpc is not returned", result);
        }
        
        //test for inactive vpc
        result = false;
        vpc = null;
        try {
            List<String> svcs = new ArrayList<String>();
            svcs.add(Service.SourceNat.getName());
            vpc = _vpcService.getActiveVpc(2);
            if (vpc != null) {
                result = true;
            }
        }  catch (Exception ex) {
        } finally {
            assertFalse("Get active Vpc: TEST FAILED, non active vpc is returned", result);
        }
    }
    

    @Test
    public void validateNtwkOffForVpc() {
        //validate network offering
        //1) correct network offering
        boolean result = false;
        try {
            _vpcService.validateNtwkOffForNtwkInVpc(2L, 1, "0.0.0.0", "111-", _vpcService.getVpc(1), "10.1.1.1", new AccountVO(), null);
            result = true;
        } catch (Exception ex) {
        } finally {
            assertTrue("Validate network offering: Test passed: the offering is valid for vpc creation", result);
        }
        
        //2) invalid offering - source nat is not included
        result = false;
        try {
            _vpcService.validateNtwkOffForNtwkInVpc(2L, 2, "0.0.0.0", "111-", _vpcService.getVpc(1), "10.1.1.1", new AccountVO(), null);
            result = true;
        } catch (InvalidParameterValueException ex) {
        } finally {
            assertFalse("Validate network offering: TEST FAILED, can't use network offering without SourceNat service", result);
        }
        
        //3) invalid offering - conserve mode is off
        result = false;
        try {
            _vpcService.validateNtwkOffForNtwkInVpc(2L, 3, "0.0.0.0", "111-", _vpcService.getVpc(1), "10.1.1.1", new AccountVO(), null);
            result = true;
        } catch (InvalidParameterValueException ex) {
        } finally {
            assertFalse("Validate network offering: TEST FAILED, can't use network offering without conserve mode = true", result);
        }
        
        //4) invalid offering - guest type shared
        result = false;
        try {
            _vpcService.validateNtwkOffForNtwkInVpc(2L, 4, "0.0.0.0", "111-", _vpcService.getVpc(1), "10.1.1.1", new AccountVO(), null);
            result = true;
        } catch (InvalidParameterValueException ex) {
        } finally {
            assertFalse("Validate network offering: TEST FAILED, can't use network offering with guest type = Shared", result);
        }
        
        //5) Invalid offering - no redundant router support
        result = false;
        try {
            _vpcService.validateNtwkOffForNtwkInVpc(2L, 5, "0.0.0.0", "111-", _vpcService.getVpc(1), "10.1.1.1", new AccountVO(), null);
            result = true;
        } catch (InvalidParameterValueException ex) {
        } finally {
            assertFalse("TEST FAILED, can't use network offering with guest type = Shared", result);
        }
    }
    
    
//    public void destroyVpc() {
//        boolean result = false;
//        try {
//            result = _vpcService.destroyVpc(_vpcService.getVpc(1), new AccountVO(), 1L);
//        } catch (Exception ex) {
//            s_logger.debug(ex);
//        } finally {
//            assertTrue("Failed to destroy VPC", result);
//        }
//    }
//
//    public void deleteVpc() {
//        //delete existing offering
//        boolean result = false;
//        try {
//            List<String> svcs = new ArrayList<String>();
//            svcs.add(Service.SourceNat.getName());
//            result = _vpcService.deleteVpc(1);
//        }  catch (Exception ex) {
//        } finally {
//            assertTrue("Delete vpc: TEST FAILED, vpc failed to delete" + result, result);
//        }
//        
//        //delete non-existing offering
//        result = false;
//        try {
//            List<String> svcs = new ArrayList<String>();
//            svcs.add(Service.SourceNat.getName());
//            result = _vpcService.deleteVpc(100);
//        }  catch (Exception ex) {
//        } finally {
//            assertFalse("Delete vpc: TEST FAILED, true is returned when try to delete non existing vpc" + result, result);
//        }
//    }
}
