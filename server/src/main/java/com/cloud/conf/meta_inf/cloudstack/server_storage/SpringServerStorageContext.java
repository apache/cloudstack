
package com.cloud.conf.meta_inf.cloudstack.server_storage;

import com.cloud.storage.listener.SnapshotStateListener;
import com.cloud.storage.secondary.SecondaryStorageVmDefaultAllocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerStorageContext {


    @Bean("snapshotStateListener")
    public SnapshotStateListener snapshotStateListener() {
        return new SnapshotStateListener();
    }

    @Bean("secondaryStorageVmDefaultAllocator")
    public SecondaryStorageVmDefaultAllocator secondaryStorageVmDefaultAllocator() {
        return new SecondaryStorageVmDefaultAllocator();
    }

}
