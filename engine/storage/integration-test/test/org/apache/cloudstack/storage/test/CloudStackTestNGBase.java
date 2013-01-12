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
package org.apache.cloudstack.storage.test;

import java.lang.reflect.Method;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;

public class CloudStackTestNGBase extends AbstractTestNGSpringContextTests {
    private String hostGateway;
    private String hostCidr;
    private String hostIp;
    private String hostGuid;
    private String templateUrl;
    private String localStorageUuid;
    private String primaryStorageUrl;
    private Transaction txn;
    
    protected void injectMockito() {
        
    }
    
    @BeforeMethod(alwaysRun = true)
    protected  void injectDB(Method testMethod) throws Exception {
        txn = Transaction.open(testMethod.getName());
    }
    
    @Test
    protected  void injectMockitoTest() {
        injectMockito();
    }
    
    @AfterMethod(alwaysRun = true)
    protected void closeDB(Method testMethod) throws Exception {
        if (txn != null) {
            txn.close();
        }
    }
    
    @BeforeMethod(alwaysRun = true)
    @Parameters({"devcloud-host-uuid", "devcloud-host-gateway", "devcloud-host-cidr", 
        "devcloud-host-ip", "template-url", "devcloud-local-storage-uuid", 
        "primary-storage-want-to-add"})
    protected void setup(String hostuuid, String gateway, String cidr, 
            String hostIp, String templateUrl, String localStorageUuid,
            String primaryStorage) {
        this.hostGuid = hostuuid;
        this.hostGateway = gateway;
        this.hostCidr = cidr;
        this.hostIp = hostIp;
        this.templateUrl = templateUrl;
        this.localStorageUuid = localStorageUuid;
        this.primaryStorageUrl = primaryStorage;
    }
    
    protected String getHostGuid() {
        return this.hostGuid;
    }
    
    protected String getHostGateway() {
        return this.hostGateway;
    }
    
    protected String getHostCidr() {
        return this.hostCidr;
    }
    
    protected String getHostIp() {
        return this.hostIp;
    }
    
    protected String getTemplateUrl() {
        return this.templateUrl;
    }
    
    protected String getLocalStorageUuid() {
        return this.localStorageUuid;
    }
    
    protected String getPrimaryStorageUrl() {
        return this.primaryStorageUrl;
    }
}
