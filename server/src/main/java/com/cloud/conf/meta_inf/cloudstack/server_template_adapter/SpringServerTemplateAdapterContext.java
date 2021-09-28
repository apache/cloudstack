
package com.cloud.conf.meta_inf.cloudstack.server_template_adapter;

import com.cloud.template.HypervisorTemplateAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerTemplateAdapterContext {


    @Bean("hypervisorTemplateAdapter")
    public HypervisorTemplateAdapter hypervisorTemplateAdapter() {
        return new HypervisorTemplateAdapter();
    }

}
