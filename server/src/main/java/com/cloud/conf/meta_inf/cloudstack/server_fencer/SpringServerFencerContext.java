
package com.cloud.conf.meta_inf.cloudstack.server_fencer;

import com.cloud.ha.RecreatableFencer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerFencerContext {


    @Bean("recreatableFencer")
    public RecreatableFencer recreatableFencer() {
        return new RecreatableFencer();
    }

    @Bean("KVMFencer")
    public com.cloud.ha.KVMFencer KVMFencer() {
        com.cloud.ha.KVMFencer bean = new com.cloud.ha.KVMFencer();
        bean.setName("KVMFenceBuilder");
        return bean;
    }

}
