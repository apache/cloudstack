
package com.cloud.conf.meta_inf.cloudstack.server_discoverer;

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
    public com.cloud.hypervisor.kvm.discoverer.KvmServerDiscoverer KvmServerDiscoverer() {
        com.cloud.hypervisor.kvm.discoverer.KvmServerDiscoverer bean = new com.cloud.hypervisor.kvm.discoverer.KvmServerDiscoverer();
        bean.setName("KVM Agent");
        return bean;
    }

    @Bean("LxcServerDiscoverer")
    public com.cloud.hypervisor.kvm.discoverer.LxcServerDiscoverer LxcServerDiscoverer() {
        com.cloud.hypervisor.kvm.discoverer.LxcServerDiscoverer bean = new com.cloud.hypervisor.kvm.discoverer.LxcServerDiscoverer();
        bean.setName("Lxc Discover");
        return bean;
    }

}
