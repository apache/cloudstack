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

package org.apache.cloudstack.extension;

import java.util.List;
import java.util.Map;

import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;

public interface ExtensionHelper {
    Long getExtensionIdForCluster(long clusterId);
    Extension getExtension(long id);
    Extension getExtensionForCluster(long clusterId);
    List<String> getExtensionReservedResourceDetails(long extensionId);

    /**
     * Detail key used to store the comma-separated list of network services provided
     * by a NetworkOrchestrator extension (e.g. {@code "SourceNat,StaticNat,Firewall"}).
     */
    String NETWORK_SERVICES_DETAIL_KEY = "network.services";

    /**
     * Detail key used to store a JSON object mapping each service name to its
     * CloudStack {@link com.cloud.network.Network.Capability} key/value pairs.
     * Example: {@code {"SourceNat":{"SupportedSourceNatTypes":"peraccount"}}}.
     * Used together with {@link #NETWORK_SERVICES_DETAIL_KEY}.
     */
    String NETWORK_SERVICE_CAPABILITIES_DETAIL_KEY = "network.service.capabilities";

    String getExtensionScriptPath(Extension extension);

    /**
     * Finds the extension registered with the given physical network whose name
     * matches the given provider name (case-insensitive).  Returns {@code null}
     * if no matching extension is found.
     *
     * <p>This is the preferred lookup when multiple extensions are registered on
     * the same physical network: the provider name stored in
     * {@code ntwk_service_map} is used to pinpoint the exact extension that
     * handles a given network.</p>
     *
     * @param physicalNetworkId the physical network ID
     * @param providerName      the provider name (must equal the extension name)
     * @return the matching {@link Extension}, or {@code null}
     */
    Extension getExtensionForPhysicalNetworkAndProvider(long physicalNetworkId, String providerName);

    /**
     * Returns ALL {@code extension_resource_map_details} (including hidden) for
     * the specific extension registered on the given physical network.  Used by
     * {@code NetworkExtensionElement} to inject device credentials into the script
     * environment for the correct extension when multiple different extensions are
     * registered on the same physical network.
     *
     * @param physicalNetworkId the physical network ID
     * @param extensionId       the extension ID
     * @return all key/value details including non-display ones, or an empty map
     */
    Map<String, String> getAllResourceMapDetailsForExtensionOnPhysicalNetwork(long physicalNetworkId, long extensionId);

    /**
     * Returns {@code true} if the given provider name is backed by a
     * {@code NetworkOrchestrator} extension registered on any physical network.
     * This is used by {@code NetworkModelImpl} to detect extension-backed providers
     * that are not in the static {@code s_providerToNetworkElementMap}.
     *
     * @param providerName the provider / extension name
     * @return true if the provider is a NetworkExtension provider
     */
    boolean isNetworkExtensionProvider(String providerName);

    /**
     * List all registered extensions filtered by extension {@link Extension.Type}.
     * Useful for callers that need to discover available providers of a given
     * type (e.g. Orchestrator, NetworkOrchestrator).
     *
     * @param type extension type to filter by
     * @return list of matching {@link Extension} instances (empty list if none)
     */
    List<Extension> listExtensionsByType(Extension.Type type);

    /**
     * Returns the effective {@link Service} → ({@link Capability} → value) capabilities
     * for the given external network provider, looking it up by name on the given
     * physical network.
     *
     * <p>If {@code physicalNetworkId} is {@code null}, the method searches across all
     * physical networks that have extensions registered and returns the capabilities for
     * the first matching extension.</p>
     *
     * @param physicalNetworkId physical network ID, or {@code null} for offering-level queries
     * @param providerName      provider / extension name
     * @return capabilities map, or the default capabilities if no matching extension is found
     */
    Map<Service, Map<Capability, String>> getNetworkCapabilitiesForProvider(Long physicalNetworkId, String providerName);

}
