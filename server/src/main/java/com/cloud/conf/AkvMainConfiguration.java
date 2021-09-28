
package com.cloud.conf;

import com.cloud.conf.com.cloud.upgrade.DatabaseCreatorContext;
import com.cloud.conf.meta_inf.cloudstack.core.SpringServerCoreManagersContext;
import com.cloud.conf.meta_inf.cloudstack.core.SpringServerCoreMiscContext;
import com.cloud.conf.meta_inf.cloudstack.server_alert_adapter_backend.SpringServerAlertAdapterBackendContext;
import com.cloud.conf.meta_inf.cloudstack.server_alert_adapter_compute.SpringServerAlertAdapterComputeContext;
import com.cloud.conf.meta_inf.cloudstack.server_alert_adapter_storage.SpringServerAlertAdapterStorageContext;
import com.cloud.conf.meta_inf.cloudstack.server_allocator.SpringServerAllocatorContext;
import com.cloud.conf.meta_inf.cloudstack.server_api.SpringServerApiContext;
import com.cloud.conf.meta_inf.cloudstack.server_compute.SpringServerComputeContext;
import com.cloud.conf.meta_inf.cloudstack.server_discoverer.SpringServerDiscovererContext;
import com.cloud.conf.meta_inf.cloudstack.server_fencer.SpringServerFencerContext;
import com.cloud.conf.meta_inf.cloudstack.server_investigator.SpringServerInvestigatorContext;
import com.cloud.conf.meta_inf.cloudstack.server_network.SpringServerNetworkContext;
import com.cloud.conf.meta_inf.cloudstack.server_planner.SpringServerPlannerContext;
import com.cloud.conf.meta_inf.cloudstack.server_storage.SpringServerStorageContext;
import com.cloud.conf.meta_inf.cloudstack.server_template_adapter.SpringServerTemplateAdapterContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * Generated Main Java based configuration
 * 
 */
@Configuration
@Import({
    SpringServerComputeContext.class,
    SpringServerApiContext.class,
    SpringServerCoreManagersContext.class,
    DatabaseCreatorContext.class,
    SpringServerAllocatorContext.class,
    SpringServerCoreMiscContext.class,
    SpringServerPlannerContext.class,
    SpringServerAlertAdapterComputeContext.class,
    SpringServerInvestigatorContext.class,
    SpringServerNetworkContext.class,
    SpringServerDiscovererContext.class,
    SpringServerFencerContext.class,
    SpringServerAlertAdapterStorageContext.class,
    SpringServerTemplateAdapterContext.class,
    SpringServerAlertAdapterBackendContext.class,
    SpringServerStorageContext.class
})
public class AkvMainConfiguration {


}
