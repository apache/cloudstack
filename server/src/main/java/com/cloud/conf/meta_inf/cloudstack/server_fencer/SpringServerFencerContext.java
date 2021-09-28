
package com.cloud.conf.meta_inf.cloudstack.server_fencer;

import com.cloud.ha.KVMFencer;
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
    public KVMFencer KVMFencer() {
        KVMFencer bean = new KVMFencer();
        bean.setName("KVMFenceBuilder");
        return bean;
    }

}
