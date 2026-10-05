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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;
import com.linbit.linstor.api.ApiException;
import com.linbit.linstor.api.DevelopersApi;
import com.linbit.linstor.api.model.Node;
import com.linbit.linstor.api.model.ResourceDefinition;
import com.linbit.linstor.api.model.ResourceWithVolumes;
import com.linbit.linstor.api.model.VolumeDefinition;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.commons.lang3.StringUtils;

/**
 * Import validation must not change resource placement or live-migration properties.
 * Both single-volume inspection and listing use controller metadata, never local DRBD devices.
 */
public final class LinstorImportHelper {
    static final int PAGE_SIZE = 1000;

    private LinstorImportHelper() {
    }

    private static String resourceName(String path) {
        // CloudStack volume paths are the resource name without the cs- prefix, not device paths.
        if (StringUtils.isBlank(path) || !path.matches("[a-zA-Z0-9_-]+")) {
            throw new CloudRuntimeException("Invalid LINSTOR volume path: expected a resource name without a device path");
        }
        return LinstorUtil.RSC_PREFIX + path;
    }

    private static void requireGroup(String group) {
        if (StringUtils.isBlank(group)) {
            throw new CloudRuntimeException("Cannot validate LINSTOR import: the storage pool has no resource group");
        }
    }

    private static List<ResourceDefinition> definitions(DevelopersApi api, String path) throws ApiException {
        List<String> names = path == null ? Collections.emptyList() : Collections.singletonList(resourceName(path));
        List<ResourceDefinition> result = new ArrayList<>();
        for (int offset = 0; ; offset += PAGE_SIZE) {
            List<ResourceDefinition> page = api.resourceDefinitionList(names, true, null, PAGE_SIZE, offset);
            if (page == null) {
                throw new CloudRuntimeException("Cannot read LINSTOR resource definitions");
            }
            result.addAll(page);
            if (page.size() < PAGE_SIZE) {
                return result;
            }
        }
    }

    private static ResourceDefinition findDefinition(List<ResourceDefinition> definitions, String path) {
        String name = resourceName(path);
        return definitions.stream().filter(rd -> name.equals(rd.getName())).findFirst()
                .orElseThrow(() -> new CloudRuntimeException("LINSTOR resource not found: " + name));
    }

    private static void checkGroup(ResourceDefinition definition, String group) {
        if (!group.equalsIgnoreCase(definition.getResourceGroupName())) {
            throw new CloudRuntimeException(String.format(
                    "Cannot import resource %s: it belongs to resource group %s, but the selected storage pool uses %s",
                    definition.getName(), definition.getResourceGroupName(), group));
        }
    }

    public static void validateResourceGroup(DevelopersApi api, String group, String path) {
        requireGroup(group);
        try {
            checkGroup(findDefinition(definitions(api, path), path), group);
        } catch (ApiException e) {
            throw new CloudRuntimeException("Cannot verify LINSTOR resource ownership: " + e.getBestMessage(), e);
        }
    }

    private static List<ResourceWithVolumes> resources(DevelopersApi api, String path) throws ApiException {
        List<String> names = path == null ? Collections.emptyList() : Collections.singletonList(resourceName(path));
        List<ResourceWithVolumes> result = new ArrayList<>();
        for (int offset = 0; ; offset += PAGE_SIZE) {
            List<ResourceWithVolumes> page = api.viewResources(Collections.emptyList(), names,
                    Collections.emptyList(), null, PAGE_SIZE, offset);
            if (page == null) {
                throw new CloudRuntimeException("Cannot read LINSTOR resource usage");
            }
            result.addAll(page);
            if (page.size() < PAGE_SIZE) {
                return result;
            }
        }
    }

    private static boolean unavailable(List<String> flags) {
        return flags != null && flags.stream().anyMatch(flag ->
                "DELETE".equals(flag) || "DELETING".equals(flag) || "INACTIVE".equals(flag));
    }

    private static Set<String> onlineNodes(DevelopersApi api) throws ApiException {
        Set<String> result = new HashSet<>();
        for (int offset = 0; ; offset += PAGE_SIZE) {
            List<Node> page = api.nodeList(Collections.emptyList(), Collections.emptyList(), PAGE_SIZE, offset);
            if (page == null) {
                throw new CloudRuntimeException("Cannot verify LINSTOR satellite connectivity");
            }
            for (Node node : page) {
                if (Node.ConnectionStatusEnum.ONLINE.equals(node.getConnectionStatus())) {
                    result.add(node.getName());
                }
            }
            if (page.size() < PAGE_SIZE) {
                return result;
            }
        }
    }

    private static boolean locked(ResourceDefinition definition, List<ResourceWithVolumes> resources, Set<String> onlineNodes) {
        if (unavailable(definition.getFlags()) || resources == null || resources.isEmpty()) {
            return true;
        }
        // Missing/unknown state is not evidence that a resource is free.
        return resources.stream().anyMatch(r -> !onlineNodes.contains(r.getNodeName())
                || unavailable(r.getFlags()) || r.getState() == null
                || r.getVolumes() == null || r.getVolumes().isEmpty()
                || !Boolean.FALSE.equals(r.getState().isInUse()));
    }

    private static String devicePath(String name, List<ResourceWithVolumes> resources) {
        if (resources != null) {
            for (ResourceWithVolumes resource : resources) {
                if (resource.getLayerObject() != null && resource.getVolumes() != null && !resource.getVolumes().isEmpty()) {
                    return LinstorUtil.getDevicePathFromResource(resource);
                }
            }
        }
        // No deployment information: useful for display only; locked() prevents adoption.
        return LinstorUtil.formatDrbdByResDevicePath(name);
    }

    public static List<VolumeOnStorageTO> getVolumesForImport(DevelopersApi api, String group, String path) {
        requireGroup(group);
        try {
            List<ResourceDefinition> definitions = definitions(api, path);
            if (path != null) {
                ResourceDefinition definition = findDefinition(definitions, path);
                checkGroup(definition, group);
                definitions = Collections.singletonList(definition);
            }
            Map<String, List<ResourceWithVolumes>> resources = resources(api, path).stream()
                    .collect(Collectors.groupingBy(ResourceWithVolumes::getName));
            Set<String> onlineNodes = onlineNodes(api);
            List<VolumeOnStorageTO> result = new ArrayList<>();
            for (ResourceDefinition definition : definitions) {
                if (definition.getName() == null || !definition.getName().startsWith(LinstorUtil.RSC_PREFIX)
                        || !group.equalsIgnoreCase(definition.getResourceGroupName())) {
                    continue;
                }
                List<VolumeDefinition> volumes = definition.getVolumeDefinitions();
                if (volumes == null || volumes.size() != 1 || !Integer.valueOf(0).equals(volumes.get(0).getVolumeNumber())
                        || volumes.get(0).getSizeKib() == null || volumes.get(0).getSizeKib() <= 0) {
                    throw new CloudRuntimeException("Invalid LINSTOR volume definition for " + definition.getName());
                }
                long size = Math.multiplyExact(volumes.get(0).getSizeKib(), 1024L);
                String name = definition.getName().substring(LinstorUtil.RSC_PREFIX.length());
                VolumeOnStorageTO volume = new VolumeOnStorageTO(Hypervisor.HypervisorType.KVM, name, name,
                        devicePath(definition.getName(), resources.get(definition.getName())), "RAW", size, size);
                volume.addDetail(VolumeOnStorageTO.Detail.FILE_FORMAT, "RAW");
                volume.addDetail(VolumeOnStorageTO.Detail.IS_LOCKED,
                        Boolean.toString(locked(definition, resources.get(definition.getName()), onlineNodes)));
                result.add(volume);
            }
            result.sort(Comparator.comparing(VolumeOnStorageTO::getName));
            return result;
        } catch (ApiException e) {
            throw new CloudRuntimeException("Cannot inspect LINSTOR volumes for import: " + e.getBestMessage(), e);
        }
    }
}
