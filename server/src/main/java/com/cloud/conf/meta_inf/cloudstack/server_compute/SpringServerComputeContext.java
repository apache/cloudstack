
package com.cloud.conf.meta_inf.cloudstack.server_compute;

import com.cloud.hypervisor.KVMGuru;
import com.cloud.hypervisor.LXCGuru;
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
    public LXCGuru LXCGuru() {
        LXCGuru bean = new LXCGuru();
        bean.setName("LXCGuru");
        return bean;
    }

    @Bean("vmImportService")
    public UnmanagedVMsManagerImpl vmImportService() {
        return new UnmanagedVMsManagerImpl();
    }

    @Bean("KVMGuru")
    public KVMGuru KVMGuru() {
        KVMGuru bean = new KVMGuru();
        bean.setName("KVMGuru");
        return bean;
    }

}
