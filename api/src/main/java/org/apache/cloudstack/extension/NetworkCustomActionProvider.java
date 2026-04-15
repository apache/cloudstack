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

import java.util.Map;

import com.cloud.network.Network;

/**
 * Implemented by network elements that support running custom actions on a
 * managed network (e.g. NetworkExtensionElement).
 *
 * <p>This interface is looked up by {@code ExtensionsManagerImpl} to dispatch
 * {@code runCustomAction} requests whose resource type is {@code Network}.</p>
 */
public interface NetworkCustomActionProvider {

    /**
     * Returns {@code true} if this provider handles networks whose physical
     * network has an ExternalNetwork service provider registered.
     *
     * @param network the target network
     * @return {@code true} if this provider can handle the network
     */
    boolean canHandleCustomAction(Network network);

    /**
     * Runs a named custom action against the external network device that
     * manages the given network.
     *
     * @param network    the CloudStack network on which to run the action
     * @param actionName the action name (e.g. {@code "reboot-device"}, {@code "dump-config"})
     * @param parameters optional parameters supplied by the caller
     * @return output from the action script, or {@code null} on failure
     */
    String runCustomAction(Network network, String actionName, Map<String, Object> parameters);
}
