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
package com.cloud.conf.meta_inf.cloudstack.server_compute;

import com.cloud.hypervisor.KVMGuru;
import com.cloud.hypervisor.LXCGuru;
import org.apache.cloudstack.vm.UnmanagedVMsManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerComputeContext {


    @Bean("LXCGuru")
    public LXCGuru LXCGuru() {
        LXCGuru bean = new LXCGuru();
        bean.setName("LXCGuru");
        return bean;
    }

    @Bean("vmImportService")
    public UnmanagedVMsManagerImpl vmImportService() {
        return new UnmanagedVMsManagerImpl();
    }

    @Bean("KVMGuru")
    public KVMGuru KVMGuru() {
        KVMGuru bean = new KVMGuru();
        bean.setName("KVMGuru");
        return bean;
    }

}
