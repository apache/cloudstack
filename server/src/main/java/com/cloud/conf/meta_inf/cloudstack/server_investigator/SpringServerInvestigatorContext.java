
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
