
package com.cloud.conf.meta_inf.cloudstack.server_discoverer;

import com.cloud.hypervisor.kvm.discoverer.KvmServerDiscoverer;
import com.cloud.hypervisor.kvm.discoverer.LxcServerDiscoverer;
import com.cloud.resource.DummyHostDiscoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerDiscovererContext {


    @Bean("dummyHostDiscoverer")
    public DummyHostDiscoverer dummyHostDiscoverer() {
        DummyHostDiscoverer bean = new DummyHostDiscoverer();
        bean.setName("dummyHostDiscoverer");
        return bean;
    }

    @Bean("KvmServerDiscoverer")
    public KvmServerDiscoverer KvmServerDiscoverer() {
        KvmServerDiscoverer bean = new KvmServerDiscoverer();
        bean.setName("KVM Agent");
        return bean;
    }

    @Bean("LxcServerDiscoverer")
    public LxcServerDiscoverer LxcServerDiscoverer() {
        LxcServerDiscoverer bean = new LxcServerDiscoverer();
        bean.setName("Lxc Discover");
        return bean;
    }

}
