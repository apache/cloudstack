
package com.cloud.conf.meta_inf.cloudstack.server_alert_adapter_backend;

import com.cloud.alert.ClusterAlertAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerAlertAdapterBackendContext {


    @Bean("clusterAlertAdapter")
    public ClusterAlertAdapter clusterAlertAdapter() {
        return new ClusterAlertAdapter();
    }

}
