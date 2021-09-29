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
package com.cloud.conf.meta_inf.cloudstack.server_discoverer;

import com.cloud.hypervisor.kvm.discoverer.KvmServerDiscoverer;
import com.cloud.hypervisor.kvm.discoverer.LxcServerDiscoverer;
import com.cloud.resource.DummyHostDiscoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerDiscovererContext {


    @Bean("dummyHostDiscoverer")
    public DummyHostDiscoverer dummyHostDiscoverer() {
        DummyHostDiscoverer bean = new DummyHostDiscoverer();
        bean.setName("dummyHostDiscoverer");
        return bean;
    }

    @Bean("KvmServerDiscoverer")
    public KvmServerDiscoverer KvmServerDiscoverer() {
        KvmServerDiscoverer bean = new KvmServerDiscoverer();
        bean.setName("KVM Agent");
        return bean;
    }

    @Bean("LxcServerDiscoverer")
    public LxcServerDiscoverer LxcServerDiscoverer() {
        LxcServerDiscoverer bean = new LxcServerDiscoverer();
        bean.setName("Lxc Discover");
        return bean;
    }

}
