
package com.cloud.conf.meta_inf.cloudstack.server_alert_adapter_compute;

import com.cloud.alert.ConsoleProxyAlertAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerAlertAdapterComputeContext {


    @Bean("consoleProxyAlertAdapter")
    public ConsoleProxyAlertAdapter consoleProxyAlertAdapter() {
        return new ConsoleProxyAlertAdapter();
    }

}
