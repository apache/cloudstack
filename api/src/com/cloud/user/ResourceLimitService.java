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
package com.cloud.user;

import java.util.List;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.Domain;
import com.cloud.exception.ResourceAllocationException;

public interface ResourceLimitService {

    /**
     * Updates an existing resource limit with the specified details. If a limit doesn't exist, will create one.
     *
     * @param accountId
     *            TODO
     * @param domainId
     *            TODO
     * @param resourceType
     *            TODO
     * @param max
     *            TODO
     *
     * @return the updated/created resource limit
     */
    ResourceLimit updateResourceLimit(Long accountId, Long domainId, Integer resourceType, Long max);

    /**
     * Updates an existing resource count details for the account/domain
     *
     * @param accountId
     *            TODO
     * @param domainId
     *            TODO
     * @param typeId
     *            TODO
     * @return the updated/created resource counts
     */
    List<? extends ResourceCount> recalculateResourceCount(Long accountId, Long domainId, Integer typeId);

    /**
     * Search for resource limits for the given id and/or account and/or type and/or domain.
     *
     * @param id
     *            TODO
     * @param accountId
     *            TODO
     * @param domainId
     *            TODO
     * @param type
     *            TODO
     * @return a list of limits that match the criteria
     */
    public List<? extends ResourceLimit> searchForLimits(Long id, Long accountId, Long domainId, Integer type, Long startIndex, Long pageSizeVal);

    /**
     * Finds the resource limit for a specified account and type. If the account has an infinite limit, will check
     * the account's parent domain, and if that limit is also infinite, will return the ROOT domain's limit.
     *
     * @param account
     * @param type
     * @return resource limit
     */
    public long findCorrectResourceLimitForAccount(Account account, ResourceType type);

    /**
     * This call should be used when we have already queried resource limit for an account. This is to handle
     * some corner cases where queried limit may be null.
     * @param accountType
     * @param limit
     * @param type
     * @return
     */
    public long findCorrectResourceLimitForAccount(long accountId, Long limit, ResourceType type);

    /**
     * Finds the resource limit for a specified domain and type. If the domain has an infinite limit, will check
     * up the domain hierarchy
     *
     * @param account
     * @param type
     * @return resource limit
     */
    public long findCorrectResourceLimitForDomain(Domain domain, ResourceType type);

    /**
     * Increments the resource count
     *
     * @param accountId
     * @param type
     * @param delta
     */
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta);

    /**
     * Decrements the resource count
     *
     * @param accountId
     * @param type
     * @param delta
     */
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta);

    /**
     * Checks if a limit has been exceeded for an account
     *
     * @param account
     * @param type
     * @param count
     *            the number of resources being allocated, count will be added to current allocation and compared
     *            against maximum allowed allocation
     * @throws ResourceAllocationException
     */
    public void checkResourceLimit(Account account, ResourceCount.ResourceType type, long... count) throws ResourceAllocationException;

    /**
     * Gets the count of resources for a resource type and account
     *
     * @param account
     * @param type
     * @return count of resources
     */
    public long getResourceCount(Account account, ResourceType type);

    /**
     * Checks if a limit has been exceeded for an account if displayResource flag is on
     *
     * @param account
     * @param type
     * @param displayResource
     * @param count
     *            the number of resources being allocated, count will be added to current allocation and compared
     *            against maximum allowed allocation
     * @throws ResourceAllocationException
     */
    void checkResourceLimit(Account account, ResourceType type, Boolean displayResource, long... count) throws ResourceAllocationException;

    /**
     * Increments the resource count  if displayResource flag is on
     *
     * @param accountId
     * @param type
     * @param displayResource
     * @param delta
     */
    void incrementResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta);

    /**
     * Increments/Decrements the resource count  depending on the displayResource flag is turned on or off respectively
     *
     * @param accountId
     * @param type
     * @param displayResource
     * @param delta
     */
    void changeResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta);

    /**
     * Decrements the resource count  if displayResource flag is on
     *
     * @param accountId
     * @param type
     * @param displayResource
     * @param delta
     */
    void decrementResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta);
}
