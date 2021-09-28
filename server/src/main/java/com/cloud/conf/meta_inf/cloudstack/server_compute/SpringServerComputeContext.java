
package com.cloud.conf.meta_inf.cloudstack.server_compute;

import org.apache.cloudstack.vm.UnmanagedVMsManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerComputeContext {


    @Bean("LXCGuru")
    public com.cloud.hypervisor.LXCGuru LXCGuru() {
        com.cloud.hypervisor.LXCGuru bean = new com.cloud.hypervisor.LXCGuru();
        bean.setName("LXCGuru");
        return bean;
    }

    @Bean("vmImportService")
    public UnmanagedVMsManagerImpl vmImportService() {
        return new UnmanagedVMsManagerImpl();
    }

    @Bean("KVMGuru")
    public com.cloud.hypervisor.KVMGuru KVMGuru() {
        com.cloud.hypervisor.KVMGuru bean = new com.cloud.hypervisor.KVMGuru();
        bean.setName("KVMGuru");
        return bean;
    }

}
