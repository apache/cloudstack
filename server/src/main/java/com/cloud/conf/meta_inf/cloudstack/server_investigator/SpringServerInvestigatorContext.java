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
package com.cloud.conf.meta_inf.cloudstack.server_investigator;

import com.cloud.ha.CheckOnAgentInvestigator;
import com.cloud.ha.ManagementIPSystemVMInvestigator;
import com.cloud.ha.UserVmDomRInvestigator;
import com.cloud.ha.XenServerInvestigator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerInvestigatorContext {


    @Bean("ManagementIPSystemVMInvestigator")
    public ManagementIPSystemVMInvestigator ManagementIPSystemVMInvestigator() {
        ManagementIPSystemVMInvestigator bean = new ManagementIPSystemVMInvestigator();
        bean.setName("ManagementIPSysVMInvestigator");
        return bean;
    }

    @Bean("UserVmDomRInvestigator")
    public UserVmDomRInvestigator UserVmDomRInvestigator() {
        UserVmDomRInvestigator bean = new UserVmDomRInvestigator();
        bean.setName("PingInvestigator");
        return bean;
    }

    @Bean("XenServerInvestigator")
    public XenServerInvestigator XenServerInvestigator() {
        XenServerInvestigator bean = new XenServerInvestigator();
        bean.setName("XenServerInvestigator");
        return bean;
    }

    @Bean("CheckOnAgentInvestigator")
    public CheckOnAgentInvestigator CheckOnAgentInvestigator() {
        CheckOnAgentInvestigator bean = new CheckOnAgentInvestigator();
        bean.setName("SimpleInvestigator");
        return bean;
    }

}
