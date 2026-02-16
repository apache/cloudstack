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
import com.linbit.linstor.api.ApiConsts;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.ApiCallRc;
import com.linbit.linstor.api.model.ApiCallRcList;
import com.linbit.linstor.api.model.AutoSelectFilter;
import com.linbit.linstor.api.model.LayerType;
import com.linbit.linstor.api.model.Node;
import com.linbit.linstor.api.model.Properties;
import com.linbit.linstor.api.model.ProviderKind;
import com.linbit.linstor.api.model.Resource;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceDefinitionModify;
import com.linbit.linstor.api.model.ResourceGroup;
import com.linbit.linstor.api.model.ResourceGroupSpawn;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.StoragePool;
import com.linbit.linstor.api.model.Volume;
import com.linbit.linstor.api.model.VolumeDefinition;
import com.linbit.linstor.api.model.VolumeDefinitionModify;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

public class LinstorUtil {
    protected static Logger LOGGER = LogManager.getLogger(LinstorUtil.class);

    public final static String PROVIDER_NAME = "Linstor";
    public static final String RSC_PREFIX = "cs-";
    public static final String RSC_GROUP = "resourceGroup";
    public static final String CS_TEMPLATE_FOR_PREFIX = "_cs-template-for-";

    public static final String LIN_PROP_DRBDOPT_EXACT_SIZE = "DrbdOptions/ExactSize";

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

    public static void logLinstorAnswer(@Nonnull ApiCallRc answer) {
        if (answer.isError()) {
            LOGGER.error(answer.getMessage());
        } else if (answer.isWarning()) {
            LOGGER.warn(answer.getMessage());
        } else if (answer.isInfo()) {
            LOGGER.info(answer.getMessage());
        }
    }

    public static void logLinstorAnswers(@Nonnull ApiCallRcList answers) {
        answers.forEach(LinstorUtil::logLinstorAnswer);
    }

    public static void checkLinstorAnswersThrow(@Nonnull ApiCallRcList answers) {
        logLinstorAnswers(answers);
        if (answers.hasError())
        {
            String errMsg = answers.stream()
                    .filter(ApiCallRc::isError)
                    .findFirst()
                    .map(ApiCallRc::getMessage).orElse("Unknown linstor error");
            throw new CloudRuntimeException(errMsg);
        }
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

    public static Pair<Long, Long> getStorageStats(String linstorUrl, String rscGroupName) {
        DevelopersApi linstorApi = getLinstorAPI(linstorUrl);
        try {
            List<StoragePool> storagePools = LinstorUtil.getRscGroupStoragePools(linstorApi, rscGroupName);

            long capacity = storagePools.stream()
                    .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                    .mapToLong(sp -> sp.getTotalCapacity() != null ? sp.getTotalCapacity() : 0L)
                    .sum() * 1024;  // linstor uses kiB

            long used = storagePools.stream()
                    .filter(sp -> sp.getProviderKind() != ProviderKind.DISKLESS)
                    .mapToLong(sp -> sp.getTotalCapacity() != null && sp.getFreeCapacity() != null ?
                            sp.getTotalCapacity() - sp.getFreeCapacity() : 0L)
                    .sum() * 1024; // linstor uses Kib
            LOGGER.debug(
                    String.format("Linstor(%s;%s): storageStats -> %d/%d", linstorUrl, rscGroupName, capacity, used));
            return new Pair<>(capacity, used);
        } catch (ApiException apiEx) {
            LOGGER.error(apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
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
     * Check if the given resources are diskless.
     *
     * @param api developer api object to use
     * @param rscName resource name to check in use state.
     * @return NodeName where the resource is inUse, if not in use `null`
     * @throws ApiException forwards api errors
     */
    public static boolean areResourcesDiskless(DevelopersApi api, String rscName, Collection<String> nodeNames)
            throws ApiException {
        List<Resource> rscs = api.resourceList(rscName, null, null);
        if (rscs != null) {
            Collection<String> disklessNodes = rscs.stream()
                .filter(rsc -> rsc.getFlags() != null && (rsc.getFlags().contains(ApiConsts.FLAG_DISKLESS) ||
                        rsc.getFlags().contains(ApiConsts.FLAG_DRBD_DISKLESS)))
                    .map(rsc -> rsc.getNodeName().toLowerCase())
                    .collect(Collectors.toList());
            return disklessNodes.containsAll(nodeNames.stream().map(String::toLowerCase).collect(Collectors.toList()));
        }
        return false;
    }

    /**
     * Format the device path for DRBD resources.
     * @param rscName
     * @return
     */
    public static String formatDrbdByResDevicePath(String rscName)
    {
        return String.format("/dev/drbd/by-res/%s/0", rscName);
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
                return LinstorUtil.getDevicePathFromResource(rsc);
            }
        }

        final String errMsg = "viewResources didn't return resources or volumes for " + rscName;
        LOGGER.error(errMsg);
        throw new CloudRuntimeException("Linstor: " + errMsg);
    }

    /**
     * Check if the resource has DRBD or not and deliver the correct device path.
     * @param rsc
     * @return
     */
    public static String getDevicePathFromResource(ResourceWithVolumes rsc) {
        if (!rsc.getVolumes().isEmpty()) {
            // CloudStack resource always only have 1 volume
            if (rsc.getLayerObject().getDrbd() != null) {
                return formatDrbdByResDevicePath(rsc.getName());
            } else {
                return rsc.getVolumes().get(0).getDevicePath();
            }
        }
        throw new CloudRuntimeException(
                String.format("getDevicePath: Resource %s/%s doesn't have volumes", rsc.getNodeName(), rsc.getName()));
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

    /**
     * Returns all resource definitions that start with the given `startWith` name.
     * @param api
     * @param startWith startWith String
     * @return a List with all ResourceDefinition starting with `startWith`
     * @throws ApiException
     */
    public static List<ResourceDefinition> getRDListStartingWith(DevelopersApi api, String startWith)
            throws ApiException
    {
        List<ResourceDefinition> rscDfns = api.resourceDefinitionList(null, false, null, null, null);

        return rscDfns.stream()
                .filter(rscDfn -> rscDfn.getName().toLowerCase().startsWith(startWith.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a pair list of resource-definitions with ther 1:1 mapped resource-group objects that start with the
     * resource name `startWith`
     * @param api
     * @param startWith
     * @return
     * @throws ApiException
     */
    public static List<Pair<ResourceDefinition, ResourceGroup>> getRDAndRGListStartingWith(DevelopersApi api, String startWith)
            throws ApiException
    {
        List<ResourceDefinition> foundRDs = getRDListStartingWith(api, startWith);

        List<String> rscGrpStrings = foundRDs.stream()
                .map(ResourceDefinition::getResourceGroupName)
                .collect(Collectors.toList());

        Map<String, ResourceGroup> rscGrps = api.resourceGroupList(rscGrpStrings, null, null, null).stream()
                .collect(Collectors.toMap(ResourceGroup::getName, rscGrp -> rscGrp));

        return foundRDs.stream()
                .map(rd -> new Pair<>(rd, rscGrps.get(rd.getResourceGroupName())))
                .collect(Collectors.toList());
    }

    /**
     * The full name of template-for aux property key.
     * @param rscGrpName
     * @return
     */
    public static String getTemplateForAuxPropKey(String rscGrpName) {
        return String.format("Aux/%s%s", CS_TEMPLATE_FOR_PREFIX, rscGrpName);
    }

    /**
     * Template resource should have a _cs-template-for-... property, that indicates to which resource-group
     * this template belongs, it works like a refcount to keep it alive if there are still such properties on the
     * template resource. That methods set the correct property on the given resource.
     * @param api
     * @param rscName Resource name to set the property.
     * @param rscGrpName Resource group this template should belong too.
     * @throws ApiException
     */
    public static void setAuxTemplateForProperty(DevelopersApi api, String rscName, String rscGrpName)
            throws ApiException
    {
        ResourceDefinitionModify rdm = new ResourceDefinitionModify();
        Properties props = new Properties();
        String propKey = LinstorUtil.getTemplateForAuxPropKey(rscGrpName);
        props.put(propKey, "true");
        rdm.setOverrideProps(props);
        ApiCallRcList answers = api.resourceDefinitionModify(rscName, rdm);

        if (answers.hasError()) {
            String bestError = LinstorUtil.getBestErrorMessage(answers);
            LOGGER.error("Set {} on {} error: {}", propKey, rscName, bestError);
            throw new CloudRuntimeException(bestError);
        } else {
            LOGGER.info("Set {} property on {}", propKey, rscName);
        }
    }

    /**
     * Find the correct resource definition to clone from.
     * There could be multiple resource definitions for the same template, with the same prefix.
     * This method searches for which resource group the resource definition was intended and returns that.
     * If no exact resource definition could be found, we return the first with a similar name as a fallback.
     * If there is not even one with the correct prefix, we return null.
     * @param api
     * @param rscName
     * @param rscGrpName
     * @return The resource-definition to clone from, if no template and no match, return null.
     * @throws ApiException
     */
    public static ResourceDefinition findResourceDefinition(DevelopersApi api, String rscName, String rscGrpName)
            throws ApiException {
        List<ResourceDefinition> rscDfns = api.resourceDefinitionList(null, false, null, null, null);

        List<ResourceDefinition> rdsStartingWith = rscDfns.stream()
                .filter(rscDfn -> rscDfn.getName().toLowerCase().startsWith(rscName.toLowerCase()))
                .collect(Collectors.toList());

        if (rdsStartingWith.isEmpty()) {
            return null;
        }

        Optional<ResourceDefinition> rd = rdsStartingWith.stream()
                .filter(rscDfn -> rscDfn.getProps().containsKey(LinstorUtil.getTemplateForAuxPropKey(rscGrpName)))
                .findFirst();

        return rd.orElseGet(() -> rdsStartingWith.get(0));
    }

    public static boolean isRscDiskless(ResourceWithVolumes rsc) {
        return rsc.getFlags() != null && rsc.getFlags().contains(ApiConsts.FLAG_DISKLESS);
    }

    /**
     * Checks if all diskful resource are on a zeroed block device.
     * @param pool Linstor pool to use
     * @param resName Linstor resource name
     * @return true if all resources are on a provider with zeroed blocks.
     */
    public static boolean resourceSupportZeroBlocks(KVMStoragePool pool, String resName) {
        final DevelopersApi api = getLinstorAPI(pool.getSourceHost());
        try {
            List<ResourceWithVolumes> resWithVols = api.viewResources(
                    Collections.emptyList(),
                    Collections.singletonList(resName),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    null,
                    null);

            if (resWithVols != null) {
                return resWithVols.stream()
                        .allMatch(res -> {
                            Volume vol0 = res.getVolumes().get(0);
                            return vol0 != null && (vol0.getProviderKind() == ProviderKind.LVM_THIN ||
                                    vol0.getProviderKind() == ProviderKind.ZFS ||
                                    vol0.getProviderKind() == ProviderKind.ZFS_THIN ||
                                    vol0.getProviderKind() == ProviderKind.DISKLESS);
                        } );
            }
        } catch (ApiException apiExc) {
            LOGGER.error(apiExc.getMessage());
        }
        return false;
    }

    public static String getRscGrp(com.cloud.storage.StoragePool storagePool) {
        return storagePool.getUserInfo() != null && !storagePool.getUserInfo().isEmpty() ?
                storagePool.getUserInfo() : "DfltRscGrp";
    }

    /**
     * Condition if a template resource can be shared with the given resource group.
     * @param tgtRscGrp
     * @param tgtLayerStack
     * @param rg
     * @return True if the template resource can be shared, else false.
     */
    private static boolean canShareTemplateForResourceGroup(
            ResourceGroup tgtRscGrp, List<String> tgtLayerStack, ResourceGroup rg) {
        List<String> rgLayerStack = rg.getSelectFilter() != null ?
                rg.getSelectFilter().getLayerStack() : null;
        return Objects.equals(tgtLayerStack, rgLayerStack) &&
                Objects.equals(tgtRscGrp.getSelectFilter().getStoragePoolList(),
                        rg.getSelectFilter().getStoragePoolList());
    }

    /**
     * Searches for a shareable template for this rscGrpName and sets the aux template property.
     * @param api
     * @param rscName
     * @param rscGrpName
     * @param existingRDs
     * @return
     * @throws ApiException
     */
    private static boolean foundShareableTemplate(
            DevelopersApi api, String rscName, String rscGrpName,
            List<Pair<ResourceDefinition, ResourceGroup>> existingRDs) throws ApiException {
        if (!existingRDs.isEmpty()) {
            ResourceGroup tgtRscGrp = api.resourceGroupList(
                    Collections.singletonList(rscGrpName), null, null, null).get(0);
            List<String> tgtLayerStack = tgtRscGrp.getSelectFilter() != null ?
                    tgtRscGrp.getSelectFilter().getLayerStack() : null;

            // check if there is already a template copy, that we could reuse
            // this means if select filters are similar enough to allow cloning from
            for (Pair<ResourceDefinition, ResourceGroup> rdPair : existingRDs) {
                ResourceGroup rg = rdPair.second();
                if (canShareTemplateForResourceGroup(tgtRscGrp, tgtLayerStack, rg)) {
                    LinstorUtil.setAuxTemplateForProperty(api, rscName, rscGrpName);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the layerlist of the resourceGroup with encryption(LUKS) added above STORAGE.
     * If the resourceGroup layer list already contains LUKS this layer list will be returned.
     * @param api Linstor developers API
     * @param resourceGroup Resource group to get the encryption layer list
     * @return layer list with LUKS added
     */
    public static List<LayerType> getEncryptedLayerList(DevelopersApi api, String resourceGroup) {
        try {
            List<ResourceGroup> rscGrps = api.resourceGroupList(
                    Collections.singletonList(resourceGroup), Collections.emptyList(), null, null);

            if (CollectionUtils.isEmpty(rscGrps)) {
                throw new CloudRuntimeException(
                        String.format("Resource Group %s not found on Linstor cluster.", resourceGroup));
            }

            final ResourceGroup rscGrp = rscGrps.get(0);
            List<LayerType> layers = Arrays.asList(LayerType.DRBD, LayerType.LUKS, LayerType.STORAGE);
            List<String> curLayerStack = rscGrp.getSelectFilter() != null ?
                    rscGrp.getSelectFilter().getLayerStack() : Collections.emptyList();
            if (CollectionUtils.isNotEmpty(curLayerStack)) {
                layers = curLayerStack.stream().map(LayerType::valueOf).collect(Collectors.toList());
                if (!layers.contains(LayerType.LUKS)) {
                    layers.add(layers.size() - 1, LayerType.LUKS); // lowest layer is STORAGE
                }
            }
            return layers;
        } catch (ApiException e) {
            throw new CloudRuntimeException(
                    String.format("Resource Group %s not found on Linstor cluster.", resourceGroup));
        }
    }

    /**
     * Spawns a new Linstor resource with the given arguments.
     * @param api
     * @param newRscName
     * @param sizeInBytes
     * @param isTemplate
     * @param rscGrpName
     * @param volName
     * @param vmName
     * @throws ApiException
     */
    private static void spawnResource(
            DevelopersApi api, String newRscName, long sizeInBytes, boolean isTemplate, String rscGrpName,
            String volName, String vmName, @Nullable Long passPhraseId, @Nullable byte[] passPhrase,
            boolean exactSize) throws ApiException
    {
        ResourceGroupSpawn rscGrpSpawn = new ResourceGroupSpawn();
        rscGrpSpawn.setResourceDefinitionName(newRscName);
        rscGrpSpawn.addVolumeSizesItem(sizeInBytes / 1024);
        if (passPhraseId != null) {
            AutoSelectFilter asf = new AutoSelectFilter();
            List<LayerType> luksLayers = getEncryptedLayerList(api, rscGrpName);
            asf.setLayerStack(luksLayers.stream().map(LayerType::toString).collect(Collectors.toList()));
            rscGrpSpawn.setSelectFilter(asf);
            if (passPhrase != null) {
                String utf8Passphrase = new String(passPhrase, StandardCharsets.UTF_8);
                rscGrpSpawn.setVolumePassphrases(Collections.singletonList(utf8Passphrase));
            }
        }

        Properties props = new Properties();
        if (isTemplate) {
            props.put(LinstorUtil.getTemplateForAuxPropKey(rscGrpName), "true");
        }
        if (exactSize) {
            props.put(LIN_PROP_DRBDOPT_EXACT_SIZE, "true");
        }
        rscGrpSpawn.setResourceDefinitionProps(props);

        LOGGER.info("Linstor: Spawn resource " + newRscName);
        ApiCallRcList answers = api.resourceGroupSpawn(rscGrpName, rscGrpSpawn);
        checkLinstorAnswersThrow(answers);

        answers = LinstorUtil.applyAuxProps(api, newRscName, volName, vmName);
        checkLinstorAnswersThrow(answers);
    }

    /**
     * Creates a new Linstor resource.
     * @param rscName
     * @param sizeInBytes
     * @param volName
     * @param vmName
     * @param api
     * @param rscGrp
     * @param poolId
     * @param isTemplate indicates if the resource is a template
     * @return true if a new resource was created, false if it already existed or was reused.
     */
    public static boolean createResourceBase(
            String rscName, long sizeInBytes, String volName, String vmName,
            @Nullable Long passPhraseId, @Nullable byte[] passPhrase, DevelopersApi api,
            String rscGrp, long poolId, boolean isTemplate, boolean exactSize)
    {
        try
        {
            LOGGER.debug("createRscBase: {} :: {} :: {} :: {}", rscName, rscGrp, isTemplate, exactSize);
            List<Pair<ResourceDefinition, ResourceGroup>> existingRDs = LinstorUtil.getRDAndRGListStartingWith(api, rscName);

            String fullRscName = String.format("%s-%d", rscName, poolId);
            boolean alreadyCreated = existingRDs.stream()
                    .anyMatch(p -> p.first().getName().equalsIgnoreCase(fullRscName)) ||
                    existingRDs.stream().anyMatch(p -> p.first().getProps().containsKey(LinstorUtil.getTemplateForAuxPropKey(rscGrp)));
            if (!alreadyCreated) {
                boolean createNewRsc = !foundShareableTemplate(api, rscName, rscGrp, existingRDs);
                if (createNewRsc) {
                    String newRscName = existingRDs.isEmpty() ? rscName : fullRscName;
                    spawnResource(api, newRscName, sizeInBytes, isTemplate, rscGrp,
                            volName, vmName, passPhraseId, passPhrase, exactSize);
                }
                return createNewRsc;
            }
            return false;
        } catch (ApiException apiEx)
        {
            LOGGER.error("Linstor: ApiEx - {}", apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }

    public static void applyQoSSettings(PrimaryDataStoreDao primaryDataStoreDao,
                                  StoragePoolVO storagePool, DevelopersApi api, String rscName, Long maxIops)
            throws ApiException
    {
        Long currentQosIops = null;
        List<VolumeDefinition> vlmDfns = api.volumeDefinitionList(rscName, null, null);
        if (!vlmDfns.isEmpty())
        {
            Properties props = vlmDfns.get(0).getProps();
            long iops = Long.parseLong(props.getOrDefault("sys/fs/blkio_throttle_write_iops", "0"));
            currentQosIops = iops > 0 ? iops : null;
        }

        if (!Objects.equals(maxIops, currentQosIops))
        {
            VolumeDefinitionModify vdm = new VolumeDefinitionModify();
            if (maxIops != null)
            {
                Properties props = new Properties();
                props.put("sys/fs/blkio_throttle_read_iops", "" + maxIops);
                props.put("sys/fs/blkio_throttle_write_iops", "" + maxIops);
                vdm.overrideProps(props);
                LOGGER.info("Apply qos setting: {} to {}", maxIops, rscName);
            }
            else
            {
                LOGGER.info("Remove QoS setting for {}", rscName);
                vdm.deleteProps(Arrays.asList("sys/fs/blkio_throttle_read_iops", "sys/fs/blkio_throttle_write_iops"));
            }
            ApiCallRcList answers = api.volumeDefinitionModify(rscName, 0, vdm);
            LinstorUtil.checkLinstorAnswersThrow(answers);

            Long capacityIops = storagePool.getCapacityIops();
            if (capacityIops != null)
            {
                long vcIops = currentQosIops != null ? currentQosIops * -1 : 0;
                long vMaxIops = maxIops != null ? maxIops : 0;
                long newIops = vcIops + vMaxIops;
                capacityIops -= newIops;
                LOGGER.info("Current storagepool {} iops capacity:  {}", storagePool, capacityIops);
                storagePool.setCapacityIops(Math.max(0, capacityIops));
                primaryDataStoreDao.update(storagePool.getId(), storagePool);
            }
        }
    }

    public static String createResource(VolumeInfo vol, StoragePoolVO storagePoolVO,
                                        PrimaryDataStoreDao primaryDataStoreDao) {
        return createResource(vol, storagePoolVO, primaryDataStoreDao, false);
    }

    public static String createResource(VolumeInfo vol, StoragePoolVO storagePoolVO,
                                        PrimaryDataStoreDao primaryDataStoreDao, boolean exactSize) {
        DevelopersApi linstorApi = LinstorUtil.getLinstorAPI(storagePoolVO.getHostAddress());
        final String rscGrp = getRscGrp(storagePoolVO);

        final String rscName = LinstorUtil.RSC_PREFIX + vol.getUuid();
        createResourceBase(
                rscName, vol.getSize(), vol.getName(), vol.getAttachedVmName(), vol.getPassphraseId(), vol.getPassphrase(),
                linstorApi, rscGrp, storagePoolVO.getId(), false, exactSize);

        try
        {
            applyQoSSettings(primaryDataStoreDao, storagePoolVO, linstorApi, rscName, vol.getMaxIops());

            return LinstorUtil.getDevicePath(linstorApi, rscName);
        } catch (ApiException apiEx)
        {
            LOGGER.error("Linstor: ApiEx - " + apiEx.getMessage());
            throw new CloudRuntimeException(apiEx.getBestMessage(), apiEx);
        }
    }
}
