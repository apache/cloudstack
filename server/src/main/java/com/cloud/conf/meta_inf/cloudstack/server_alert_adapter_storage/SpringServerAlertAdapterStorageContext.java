
package com.cloud.conf.meta_inf.cloudstack.server_alert_adapter_storage;

import com.cloud.alert.SecondaryStorageVmAlertAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerAlertAdapterStorageContext {


    @Bean("secondaryStorageVmAlertAdapter")
    public SecondaryStorageVmAlertAdapter secondaryStorageVmAlertAdapter() {
        return new SecondaryStorageVmAlertAdapter();
    }

}
