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
package com.cloud.network.element;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.Site2SiteVpnTunnelInterface;
import com.cloud.network.vpc.Vpc;
import com.cloud.utils.component.Adapter;

public interface Site2SiteVpnServiceProvider extends Adapter {
    default void validateSite2SiteVpnCustomerGateway(Site2SiteCustomerGateway customerGateway) {
    }

    boolean startSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException;

    boolean stopSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException;

    /**
     * Permanently removes a provider-side connection.  This is distinct from stop: providers
     * may disable a tunnel while retaining its profiles for an immediate reconnect, but deletion
     * must remove all objects owned by the CloudStack connection.
     */
    default boolean deleteSite2SiteVpn(Site2SiteVpnConnection conn) throws ResourceUnavailableException {
        return stopSite2SiteVpn(conn);
    }

    /**
     * Lets the provider supply the public IP the VPN gateway should terminate on, instead of the
     * VPC source NAT IP. Providers that terminate VPN on an external gateway (e.g. NSX Tier-1)
     * acquire and return a dedicated IP here; requestedIp, when not null, is the IP the caller
     * asked for and must be validated by the provider. Returning null means the provider has no
     * preference and the manager falls back to the default IP selection.
     */
    default IpAddress acquireVpnGatewayIp(Vpc vpc, IpAddress requestedIp) {
        return null;
    }

    /**
     * Counterpart of {@link #acquireVpnGatewayIp(Vpc, IpAddress)}: invoked when a VPN gateway is
     * deleted so the provider can tear down external VPN resources and release the gateway IP if
     * it was acquired by the provider.
     */
    default void releaseVpnGatewayIp(Site2SiteVpnGateway gateway) {
    }

    /**
     * Identifies a gateway previously owned by this provider. This is used during teardown when
     * an offering has been edited since the gateway was created and the current service map no
     * longer advertises the provider.
     */
    default boolean ownsVpnGateway(Site2SiteVpnGateway gateway) {
        return false;
    }

    /**
     * Returns provider-specific route-based tunnel addressing that the peer must configure.
     * Policy-based providers return no tunnel-interface details.
     */
    default Site2SiteVpnTunnelInterface getSite2SiteVpnTunnelInterface(Site2SiteVpnConnection connection) {
        return null;
    }
}
