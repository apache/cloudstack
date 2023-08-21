package com.cloud.hypervisor.kvm.storage;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;

@StorageAdaptorInfo(storagePoolType=Storage.StoragePoolType.FiberChannel)
public class FiberChannelAdapter extends MultipathSCSIAdapterBase {
    public FiberChannelAdapter() {
        LOGGER.info("Loaded FiberChannelAdapter for StorageLayer");
    }

    @Override
    public KVMStoragePool getStoragePool(String uuid) {
        KVMStoragePool pool = MapStorageUuidToStoragePool.get(uuid);
        if (pool == null) {
            // return a dummy pool - this adapter doesn't care about connectivity information
            pool = new MultipathSCSIPool(uuid, this);
            MapStorageUuidToStoragePool.put(uuid, pool);
         }
        LOGGER.info("FiberChannelAdapter return storage pool [" + uuid + "]");
        return pool;
    }

    public String getName() {
        return "FiberChannelAdapter";
    }

    public boolean isStoragePoolTypeSupported(Storage.StoragePoolType type) {
        if (Storage.StoragePoolType.FiberChannel.equals(type)) {
            return true;
        }
        return false;
    }

    @Override
    public AddressInfo parseAndValidatePath(String inPath) {
            // type=FIBERWWN; address=<address>; connid=<connid>
            String type = null;
            String address = null;
            String connectionId = null;
            String path = null;
            String[] parts = inPath.split(";");
            // handle initial code of wwn only
            if (parts.length == 1) {
                type = "FIBERWWN";
                address = parts[0];
            } else {
                for (String part: parts) {
                    String[] pair = part.split("=");
                    if (pair.length == 2) {
                        String key = pair[0].trim();
                        String value = pair[1].trim();
                        if (key.equals("type")) {
                            type = value.toUpperCase();
                        } else if (key.equals("address")) {
                            address = value;
                        } else if (key.equals("connid")) {
                            connectionId = value;
                        }
                    }
                }
            }

            if ("FIBERWWN".equals(type)) {
                path = "/dev/mapper/3" + address;
            } else {
                throw new CloudRuntimeException("Invalid address type provided for target disk: " + type);
            }

            return new AddressInfo(type, address, connectionId, path);
    }
}
