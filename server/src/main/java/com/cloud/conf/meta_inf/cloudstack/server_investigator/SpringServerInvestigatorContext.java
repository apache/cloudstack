
package com.cloud.conf.meta_inf.cloudstack.server_investigator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerInvestigatorContext {


    @Bean("ManagementIPSystemVMInvestigator")
    public com.cloud.ha.ManagementIPSystemVMInvestigator ManagementIPSystemVMInvestigator() {
        com.cloud.ha.ManagementIPSystemVMInvestigator bean = new com.cloud.ha.ManagementIPSystemVMInvestigator();
        bean.setName("ManagementIPSysVMInvestigator");
        return bean;
    }

    @Bean("UserVmDomRInvestigator")
    public com.cloud.ha.UserVmDomRInvestigator UserVmDomRInvestigator() {
        com.cloud.ha.UserVmDomRInvestigator bean = new com.cloud.ha.UserVmDomRInvestigator();
        bean.setName("PingInvestigator");
        return bean;
    }

    @Bean("XenServerInvestigator")
    public com.cloud.ha.XenServerInvestigator XenServerInvestigator() {
        com.cloud.ha.XenServerInvestigator bean = new com.cloud.ha.XenServerInvestigator();
        bean.setName("XenServerInvestigator");
        return bean;
    }

    @Bean("CheckOnAgentInvestigator")
    public com.cloud.ha.CheckOnAgentInvestigator CheckOnAgentInvestigator() {
        com.cloud.ha.CheckOnAgentInvestigator bean = new com.cloud.ha.CheckOnAgentInvestigator();
        bean.setName("SimpleInvestigator");
        return bean;
    }

}
