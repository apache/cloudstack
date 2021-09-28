
package com.cloud.conf.meta_inf.cloudstack.server_planner;

import com.cloud.deploy.FirstFitPlanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerPlannerContext {


    @Bean("FirstFitPlanner")
    public FirstFitPlanner FirstFitPlanner() {
        FirstFitPlanner bean = new FirstFitPlanner();
        bean.setName("FirstFitPlanner");
        return bean;
    }

}
