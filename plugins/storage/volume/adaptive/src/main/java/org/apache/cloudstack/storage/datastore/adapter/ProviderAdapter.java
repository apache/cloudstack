// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.adapter;

import java.util.Map;

/**
 * A simple DataStore adaptive interface.  This interface allows the ManagedVolumeDataStoreDriverImpl
 * to interact with the external provider without the provider needing to interface with any CloudStack
 * objects, factories or database tables, simplifying the implementation and maintenance of the provider
 * interface.
 */
public interface ProviderAdapter {
    // some common keys across providers.  Provider code determines what to do with it
    public static final String API_USERNAME_KEY = "api_username";
    public static final String API_PASSWORD_KEY = "api_password";
    public static final String API_TOKEN_KEY = "api_token";
    public static final String API_PRIVATE_KEY = "api_privatekey";
    public static final String API_URL_KEY = "api_url";
    public static final String API_SKIP_TLS_VALIDATION_KEY = "api_skiptlsvalidation";
    // one of: basicauth (default), apitoken, privatekey
    public static final String API_AUTHENTICATION_TYPE_KEY = "api_authn_type";

    /**
     * Refresh the connector with the provided details
     * @param details
     */
    public void refresh(Map<String,String> details);

    /**
     * Return if currently connected/configured properly, otherwise throws a RuntimeException
     * with information about what is misconfigured
     * @return
     */
    public void validate();

    /**
     * Forcefully remove/disconnect
     */
    public void disconnect();

    /**
     * Create a new volume on the storage provider
     * @param context
     * @param volume
     * @param diskOffering
     * @param sizeInBytes
     * @return
     */
    public ProviderVolume create(ProviderAdapterContext context, ProviderAdapterDataObject volume, ProviderAdapterDiskOffering diskOffering, long sizeInBytes);

    /**
     * Attach the volume to the target object for the provided context.  Returns the scope-specific connection value (for example, the LUN)
     * @param context
     * @param request
     * @return
     */
    public String attach(ProviderAdapterContext context, ProviderAdapterDataObject request, String hostname);

    /**
     * Detach the host from the storage context
     * @param context
     * @param request
     */
    public void detach(ProviderAdapterContext context, ProviderAdapterDataObject request, String hostname);

    /**
     * Delete the provided volume/object
     * @param context
     * @param request
     */
    public void delete(ProviderAdapterContext context, ProviderAdapterDataObject request);

    /**
     * Copy a source object to a destination volume.  The source object can be a Volume, Snapshot, or Template
     */
    public ProviderVolume copy(ProviderAdapterContext context, ProviderAdapterDataObject sourceVolume, ProviderAdapterDataObject targetVolume);

    /**
     * Make a device-specific snapshot of the provided volume
     */
    public ProviderSnapshot snapshot(ProviderAdapterContext context, ProviderAdapterDataObject sourceVolume, ProviderAdapterDataObject targetSnapshot);

    /**
     * Revert the snapshot to its base volume.  Replaces the base volume with the snapshot point on the storage array
     * @param context
     * @param request
     * @return
     */
    public ProviderVolume revert(ProviderAdapterContext context, ProviderAdapterDataObject request);

    /**
     * Resize a volume
     * @param context
     * @param request
     * @param totalNewSizeInBytes
     */
    public void resize(ProviderAdapterContext context, ProviderAdapterDataObject request, long totalNewSizeInBytes);

    /**
     * Return the managed volume info from storage system.
     * @param context
     * @param request
     * @return ProviderVolume object or null if the object was not found but no errors were encountered.
     */
    public ProviderVolume getVolume(ProviderAdapterContext context, ProviderAdapterDataObject request);

    /**
     * Return the managed snapshot info from storage system
     * @param context
     * @param request
     * @return ProviderSnapshot object or null if the object was not found but no errors were encountered.
     */
    public ProviderSnapshot getSnapshot(ProviderAdapterContext context, ProviderAdapterDataObject request);

    /**
     * Given an array-specific address, find the matching volume information from the array
     * @param addressType
     * @param address
     * @return
     */
    public ProviderVolume getVolumeByAddress(ProviderAdapterContext context, ProviderVolume.AddressType addressType, String address);

    /**
     * Returns stats about the managed storage where the volumes and snapshots are created/managed
     * @return
     */
    public ProviderVolumeStorageStats getManagedStorageStats();

    /**
     * Returns stats about a specific volume
     * @return
     */
    public ProviderVolumeStats getVolumeStats(ProviderAdapterContext context, ProviderAdapterDataObject request);

    /**
     * Returns true if the given hostname is accessible to the storage provider.
     * @param context
     * @param request
     * @return
     */
    public boolean canAccessHost(ProviderAdapterContext context, String hostname);

    /**
     * Returns true if the provider allows direct attach/connection of snapshots to a host
     * @return
     */
    public boolean canDirectAttachSnapshot();


    /**
     * Given a ProviderAdapterDataObject, return a map of connection IDs to connection values.  Generally
     * this would be used to return a map of hostnames and the VLUN ID for the attachment associated with
     * that hostname.  If the provider is using a hostgroup/hostset model where the ID is assigned in common
     * across all hosts in the group, then the map MUST contain a single entry with host key set as a wildcard
     * character (exactly '*').
     * @param dataIn
     * @return
     */
    public Map<String, String> getConnectionIdMap(ProviderAdapterDataObject dataIn);
}
