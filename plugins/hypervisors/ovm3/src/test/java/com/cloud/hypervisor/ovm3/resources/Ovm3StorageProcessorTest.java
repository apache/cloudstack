/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.resources;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3ConfigurationTest;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3StoragePool;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;

public class Ovm3StorageProcessorTest {
    ConnectionTest con = new ConnectionTest();
    OvmObject ovmObject = new OvmObject();
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    Ovm3VirtualRoutingResource virtualrouting = new Ovm3VirtualRoutingResource();
    Ovm3StorageProcessor storage;
    Ovm3StoragePool pool;
    Ovm3SupportTest support = new Ovm3SupportTest();
    
    private ConnectionTest prepare() throws ConfigurationException {  
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        pool = new Ovm3StoragePool(con, config);
        storage = new Ovm3StorageProcessor(con, config, pool);
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        virtualrouting.setConnection(con);
        return con;
    }
    
    @Test
    public void isoAttachTest() throws ConfigurationException {
        con = prepare();
    }
}
