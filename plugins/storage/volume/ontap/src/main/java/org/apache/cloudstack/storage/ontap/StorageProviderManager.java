package org.apache.cloudstack.storage.ontap;

import org.apache.cloudstack.storage.lifecycle.OntapPrimaryDatastoreLifecycle;
import org.apache.cloudstack.storage.strategy.NASStrategy;
import org.apache.cloudstack.storage.strategy.SANStrategy;
import org.apache.cloudstack.storage.strategy.UnifiedNASStrategy;
import org.apache.cloudstack.storage.strategy.UnifiedSANStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class StorageProviderManager {
    private final NASStrategy nasStrategy;
    private final SANStrategy sanStrategy;
    private static final Logger s_logger = (Logger) LogManager.getLogger(StorageProviderManager.class);

    public StorageProviderManager(Map<String, String> details, String protocol) {
        String svmName = details.get("svmName");
        String username = details.get("username");
        if ("nfs".equalsIgnoreCase(protocol)) { // TODO: Cloudstack protocol list is different than ONTAP supported protocols, so figure out the proper mapping
            this.nasStrategy = new UnifiedNASStrategy(details);
            this.sanStrategy = null;
        } else if ("iscsi".equalsIgnoreCase(protocol)) { // TODO: Cloudstack protocol list is different than ONTAP supported protocols, so figure out the proper mapping
            this.sanStrategy = new UnifiedSANStrategy(details);
            this.nasStrategy = null;
        } else {
            //TODO: Figure out the appropriate exception handling mechanism
            this.nasStrategy = null;
            this.sanStrategy = null;
            s_logger.error("Unsupported protocol: " + protocol);
            return;
        }
    }

    // Connect method to validate ONTAP cluster, credentials, protocol, and SVM
    public boolean connect(Map<String, String> details) {
        // 1. Check if ONTAP cluster is reachable
        // 2. Validate credentials
        // 3. Check protocol support
        // 4. Check if SVM with given name exists
        // Use Feign client and models for actual implementation
        // Return true if all validations pass, false otherwise

        return false;
    }

    // Common methods like create/delete etc., should be here
    public void createVolume(String volumeName, long size) {
       // TODO: Call the ontap feign client for creating volume here
    }
}
