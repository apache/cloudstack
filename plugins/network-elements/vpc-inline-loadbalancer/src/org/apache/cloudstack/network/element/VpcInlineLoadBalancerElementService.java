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

public interface VpcInlineLoadBalancerElementService extends PluggableService {
    /**
     * Configures existing VpcInline Load Balancer Element (enables or disables it)
     * @param id
     * @param enable
     * @return
     */
    VirtualRouterProvider configureVpcInlineLoadBalancerElement(long id, boolean enable);

    /**
     * Configures existing VpcInline Load Balancer Element (enables or disables it)
     * @param nspId
     * @param enable
     * @return
     */
    VirtualRouterProvider configureVpcInlineLoadBalancerElementByNspId(long nspId, boolean enable);


    /**
     * Adds VpcInline Load Balancer element to the Network Service Provider
     * @param ntwkSvcProviderId
     * @return
     */
    VirtualRouterProvider addVpcInlineLoadBalancerElement(long ntwkSvcProviderId);

    /**
     * Retrieves existing VpcInline Load Balancer element
     * @param id
     * @return
     */
    VirtualRouterProvider getVpcInlineLoadBalancerElement(long id);

    /**
     * Searches for existing VpcInline Load Balancer elements based on parameters passed to the call
     * @param id
     * @param ntwkSvsProviderId
     * @param enabled
     * @return
     */
    List<? extends VirtualRouterProvider> searchForVpcInlineLoadBalancerElements(Long id, Long ntwkSvsProviderId, Boolean enabled);
}