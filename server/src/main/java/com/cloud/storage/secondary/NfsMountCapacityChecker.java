package com.cloud.storage.secondary;

import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.mount.MountManager;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Component
public class NfsMountCapacityChecker implements CapacityChecker {
    @Inject
    private MountManager mountManager;
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;

    @Override
    public boolean hasEnoughCapacity(DataStore imageStore) {
        String[] capacityInfo = getCapacityInfo(imageStore);
        Long capacity = parse(capacityInfo[0]);
        Long used = parse(capacityInfo[1]);
        if (used/(capacity * 1.0) <= CAPACITY_THRESHOLD) {
            return true;
        }
        return false;
    }

    private String[] getCapacityInfo(DataStore imageStore) {
        String mountPoint = mountManager.getMountPoint(imageStore.getUri(), imageStoreDetailsUtil.getNfsVersion(imageStore.getId()));
        String command = String.format("df %s | grep %s | awk -F' ' '{print $2, $3}'", mountPoint, hostFromUrl(imageStore.getUri()));
        return Optional.ofNullable(Script.runSimpleBashScript(command)).orElse("0 0").split(" ");
    }

    private String hostFromUrl(String url) {
        try {
            URI uri = new URI(UriUtils.encodeURIComponent(url));
            if (Optional.ofNullable(uri.getScheme()).orElse("").equalsIgnoreCase("nfs")) {
                return uri.getHost();
            }
            return url;
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Invalid NFS url %s caused error: %s.%n", url, e.getMessage()));
        }
    }
}
