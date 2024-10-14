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
package org.apache.cloudstack.storage.datastore.util;

import com.linbit.linstor.api.ApiClient;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.Node;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.Resource;
import com.linbit.linstor.api.model.ResourceDefinitionModify;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.StoragePool;
import com.linbit.linstor.api.model.Volume;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LinstorUtil {
    protected static Logger LOGGER = LogManager.getLogger(LinstorUtil.class);

    public final static String PROVIDER_NAME = "Linstor";
    public static final String RSC_PREFIX = "cs-";
    public static final String RSC_GROUP = "resourceGroup";

    public static final String TEMP_VOLUME_ID = "tempVolumeId";

    public static final String CLUSTER_DEFAULT_MIN_IOPS = "clusterDefaultMinIops";
    public static final String CLUSTER_DEFAULT_MAX_IOPS = "clusterDefaultMaxIops";

    public static DevelopersApi getLinstorAPI(String linstorUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(linstorUrl);
        return new DevelopersApi(client);
    }

    public static String getBestErrorMessage(ApiCallRcList answers) {
        return answers != null && !answers.isEmpty() ?
            answers.stream()
                .filter(ApiCallRc::isError)
                .findFirst()
                .map(ApiCallRc::getMessage)
                .orElse((answers.get(0)).getMessage()) : null;
    }

    public static List<String> getLinstorNodeNames(@Nonnull DevelopersApi api) throws ApiException
    {
        List<Node> nodes = api.nodeList(
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            null
        );

        return nodes.stream().map(Node::getName).collect(Collectors.toList());
    }

    public static List<com.linbit.linstor.api.model.StoragePool>
    getDiskfulStoragePools(@Nonnull DevelopersApi api, @Nonnull String rscName) throws ApiException
    {
        List<ResourceWithVolumes> resources = api.viewResources(
                Collections.emptyList(),
                Collections.singletonList(rscName),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null);

        String nodeName = null;
        String storagePoolName = null;
        for (ResourceWithVolumes rwv : resources) {
            if (rwv.getVolumes().isEmpty()) {
                continue;
            }
            Volume vol = rwv.getVolumes().get(0);
            if (vol.getProviderKind() != ProviderKind.DISKLESS) {
                nodeName = rwv.getNodeName();
                storagePoolName = vol.getStoragePoolName();
                break;
            }
        }

        if (nodeName == null) {
            return null;
        }

        List<com.linbit.linstor.api.model.StoragePool> sps = api.viewStoragePools(
                Collections.singletonList(nodeName),
                Collections.singletonList(storagePoolName),
                Collections.emptyList(),
                null,
                null,
                true
        );
        return sps != null ? sps : Collections.emptyList();
    }

    public static com.linbit.linstor.api.model.StoragePool
    getDiskfulStoragePool(@Nonnull DevelopersApi api, @Nonnull String rscName) throws ApiException
    {
        List<com.linbit.linstor.api.model.StoragePool> sps = getDiskfulStoragePools(api, rscName);
        if (sps != null) {
            return !sps.isEmpty() ? sps.get(0) : null;
        }
        return null;
    }

    public static String getSnapshotPath(com.linbit.linstor.api.model.StoragePool sp, String rscName, String snapshotName) {
        final String suffix = "00000";
        final String backingPool = sp.getProps().get("StorDriver/StorPoolName");
        final String path;
        switch (sp.getProviderKind()) {
            case LVM_THIN:
                path = String.format("/dev/mapper/%s-%s_%s_%s",
                    backingPool.split("/")[0], rscName.replace("-", "--"), suffix, snapshotName.replace("-", "--"));
                break;
            case ZFS:
            case ZFS_THIN:
                path = String.format("zfs://%s/%s_%s@%s", backingPool.split("/")[0], rscName, suffix, snapshotName);
                break;
            default:
                throw new CloudRuntimeException(
                    String.format("Linstor: storage pool type %s doesn't support snapshots.", sp.getProviderKind()));
        }
        return path;
    }

    public static List<StoragePool> getRscGroupStoragePools(DevelopersApi api, String rscGroupName)
            throws ApiException {
        List<ResourceGroup> rscGrps = api.resourceGroupList(
                Collections.singletonList(rscGroupName),
                null,
                null,
                null);

        if (rscGrps.isEmpty()) {
            final String errMsg = String.format("Linstor: Resource group '%s' not found", rscGroupName);
            LOGGER.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }

        return api.viewStoragePools(
                Collections.emptyList(),
                rscGrps.get(0).getSelectFilter().getStoragePoolList(),
                null,
                null,
                null,
                true
        );
    }

    public static long getCapacityBytes(String linstorUrl, String rscGroupName) {
        DevelopersApi linstorApi = getLinstorAPI(linstorUrl);
        try {
            List<StoragePool> storagePools = getRscGroupStoragePools(linstorApi, rscGroupName);

            return storagePools.stream()
                .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                .mapToLong(sp -> sp.getTotalCapacity() != null ? sp.getTotalCapacity() : 0L)
                .sum() * 1024;  // linstor uses kiB
        } catch (ApiException apiEx) {
            LOGGER.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx);
        }
    }

    /**
     * Check if any resource of the given name is InUse on any host.
     *
     * @param api developer api object to use
     * @param rscName resource name to check in use state.
     * @return NodeName where the resource is inUse, if not in use `null`
     * @throws ApiException forwards api errors
     */
    public static String isResourceInUse(DevelopersApi api, String rscName) throws ApiException {
        List<Resource> rscs = api.resourceList(rscName, null, null);
        if (rscs != null) {
            return rscs.stream()
                    .filter(rsc -> rsc.getState() != null && Boolean.TRUE.equals(rsc.getState().isInUse()))
                    .map(Resource::getNodeName)
                    .findFirst()
                    .orElse(null);
        }
        LOGGER.error("isResourceInUse: null returned from resourceList");
        return null;
    }

    /**
     * Try to get the device path for the given resource name.
     * This could be made a bit more direct after java-linstor api is fixed for layer data subtypes.
     * @param api developer api object to use
     * @param rscName resource name to get the device path
     * @return The device path of the resource.
     * @throws ApiException if Linstor API call failed.
     * @throws CloudRuntimeException if no device path could be found.
     */
    public static String getDevicePath(DevelopersApi api, String rscName) throws ApiException, CloudRuntimeException {
        List<ResourceWithVolumes> resources = api.viewResources(
                Collections.emptyList(),
                Collections.singletonList(rscName),
                Collections.emptyList(),
                null,
                null,
                null);
        for (ResourceWithVolumes rsc : resources) {
            if (!rsc.getVolumes().isEmpty()) {
                // CloudStack resource always only have 1 volume
                String devicePath = rsc.getVolumes().get(0).getDevicePath();
                if (devicePath != null && !devicePath.isEmpty()) {
                    LOGGER.debug("getDevicePath: {} -> {}", rscName, devicePath);
                    return devicePath;
                }
            }
        }

        final String errMsg = "viewResources didn't return resources or volumes for " + rscName;
        LOGGER.error(errMsg);
        throw new CloudRuntimeException("Linstor: " + errMsg);
    }

    public static ApiCallRcList applyAuxProps(DevelopersApi api, String rscName, String dispName, String vmName)
            throws ApiException
    {
        ResourceDefinitionModify rdm = new ResourceDefinitionModify();
        Properties props = new Properties();
        if (dispName != null)
        {
            props.put("Aux/cs-name", dispName);
        }
        if (vmName != null)
        {
            props.put("Aux/cs-vm-name", vmName);
        }
        ApiCallRcList answers = new ApiCallRcList();
        if (!props.isEmpty())
        {
            rdm.setOverrideProps(props);
            answers = api.resourceDefinitionModify(rscName, rdm);
        }
        return answers;
    }
}
