
package com.cloud.conf.meta_inf.cloudstack.server_api;

import com.cloud.acl.AffinityGroupAccessChecker;
import com.cloud.acl.DomainChecker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerApiContext {


    @Bean("domainChecker")
    public DomainChecker domainChecker() {
        return new DomainChecker();
    }

    @Bean("affinityGroupAccessChecker")
    public AffinityGroupAccessChecker affinityGroupAccessChecker() {
        return new AffinityGroupAccessChecker();
    }

}
