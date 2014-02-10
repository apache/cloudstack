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
package org.apache.cloudstack.network.element;

import java.util.List;

import com.cloud.network.VirtualRouterProvider;
import com.cloud.utils.component.PluggableService;

public interface InternalLoadBalancerElementService extends PluggableService {
    /**
     * Configures existing Internal Load Balancer Element (enables or disables it)
     * @param id
     * @param enable
     * @return
     */
    VirtualRouterProvider configureInternalLoadBalancerElement(long id, boolean enable);

    /**
     * Adds Internal Load Balancer element to the Network Service Provider
     * @param ntwkSvcProviderId
     * @return
     */
    VirtualRouterProvider addInternalLoadBalancerElement(long ntwkSvcProviderId);

    /**
     * Retrieves existing Internal Load Balancer element
     * @param id
     * @return
     */
    VirtualRouterProvider getInternalLoadBalancerElement(long id);

    /**
     * Searches for existing Internal Load Balancer elements based on parameters passed to the call
     * @param id
     * @param ntwkSvsProviderId
     * @param enabled
     * @return
     */
    List<? extends VirtualRouterProvider> searchForInternalLoadBalancerElements(Long id, Long ntwkSvsProviderId, Boolean enabled);
}
