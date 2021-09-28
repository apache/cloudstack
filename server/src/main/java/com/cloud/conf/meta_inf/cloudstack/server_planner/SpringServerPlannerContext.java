
package com.cloud.conf.meta_inf.cloudstack.server_planner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerPlannerContext {


    @Bean("FirstFitPlanner")
    public com.cloud.deploy.FirstFitPlanner FirstFitPlanner() {
        com.cloud.deploy.FirstFitPlanner bean = new com.cloud.deploy.FirstFitPlanner();
        bean.setName("FirstFitPlanner");
        return bean;
    }

}
