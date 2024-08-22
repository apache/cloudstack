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

import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.Domain;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;

public interface ResourceLimitService {

    static final ConfigKey<Long> MaxAccountSecondaryStorage = new ConfigKey<>("Account Defaults", Long.class, "max.account.secondary.storage", "400",
            "The default maximum secondary storage space (in GiB) that can be used for an account", false);
    static final ConfigKey<Long> MaxProjectSecondaryStorage = new ConfigKey<>("Project Defaults", Long.class, "max.project.secondary.storage", "400",
            "The default maximum secondary storage space (in GiB) that can be used for a project", false);
    static final ConfigKey<Long> ResourceCountCheckInterval = new ConfigKey<>("Advanced", Long.class, "resourcecount.check.interval", "300",
            "Time (in seconds) to wait before running resource recalculation and fixing tasks like stale resource reservation cleanup" +
                    ". Default is 300 seconds, Setting this to 0 disables execution of the task", true);
    static final ConfigKey<Long> ResourceReservationCleanupDelay = new ConfigKey<>("Advanced", Long.class, "resource.reservation.cleanup.delay", "3600",
            "Time (in seconds) after which a resource reservation gets deleted. Default is 3600 seconds, Setting this to 0 disables execution of the task", true);
    static final ConfigKey<String> ResourceLimitHostTags = new ConfigKey<>("Advanced", String.class, "resource.limit.host.tags", "",
            "A comma-separated list of tags for host resource limits", true);
    static final ConfigKey<String> ResourceLimitStorageTags = new ConfigKey<>("Advanced", String.class, "resource.limit.storage.tags", "",
            "A comma-separated list of tags for storage resource limits", true);
    static final ConfigKey<Long> DefaultMaxAccountProjects = new ConfigKey<>("Account Defaults",Long.class,"max.account.projects","10",
                "The default maximum number of projects that can be created for an account",false);
    static final ConfigKey<Long> DefaultMaxDomainProjects = new ConfigKey<>("Domain Defaults",Long.class,"max.domain.projects","50",
                        "The default maximum number of projects that can be created for a domain",false);

    static final List<ResourceType> HostTagsSupportingTypes = List.of(ResourceType.user_vm, ResourceType.cpu, ResourceType.memory);
    static final List<ResourceType> StorageTagsSupportingTypes = List.of(ResourceType.volume, ResourceType.primary_storage);

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
     * @param tag
     *            tag for the resource type
     *
     * @return the updated/created resource limit
     */
    ResourceLimit updateResourceLimit(Long accountId, Long domainId, Integer resourceType, Long max, String tag);

    /**
     * Updates an existing resource count details for the account/domain
     *
     * @param accountId
     *            Id of the account for which resource recalculation to be done
     * @param domainId
     *            Id of the domain for which resource recalculation to be doneDO
     * @param typeId
     *            type of the resource for which recalculation to be done
     * @param tag
     *            tag for the resource type for which recalculation to be done
     * @return the updated/created resource counts
     */
    List<? extends ResourceCount> recalculateResourceCount(Long accountId, Long domainId, Integer typeId, String tag);
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
     * @param resourceType
     *            TODO
     * @return a list of limits that match the criteria
     */
    public List<? extends ResourceLimit> searchForLimits(Long id, Long accountId, Long domainId, ResourceType resourceType, String tag, Long startIndex, Long pageSizeVal);

    /**
     * Finds the resource limit for a specified account and type. If the account has an infinite limit, will check
     * the account's parent domain, and if that limit is also infinite, will return the ROOT domain's limit.
     *
     * @param account
     * @param type
     * @param tag
     * @return resource limit
     */
    public long findCorrectResourceLimitForAccount(Account account, ResourceType type, String tag);

    /**
     * This call should be used when we have already queried resource limit for an account. This is to handle
     * some corner cases where queried limit may be null.
     * @param accountId
     * @param limit
     * @param type
     * @return
     */
    public long findCorrectResourceLimitForAccount(long accountId, Long limit, ResourceType type);

    /**
     * Finds the resource limit for a specified domain and type. If the domain has an infinite limit, will check
     * up the domain hierarchy
     *
     * @param domain
     * @param type
     * @param tag
     * @return resource limit
     */
    public long findCorrectResourceLimitForDomain(Domain domain, ResourceType type, String tag);

    /**
     * Finds the default resource limit for a specified type.
     *
     * @param resourceType
     * @return resource limit
     */
    public long findDefaultResourceLimitForDomain(ResourceType resourceType);

    /**
     * Finds the resource limit for a specified account, domain and type.
     *
     * @param domain
     * @param type
     * @param tag
     * @return resource limit
     */
    public long findCorrectResourceLimitForAccountAndDomain(Account account, Domain domain, ResourceType type, String tag);

    /**
     * Increments the resource count
     *
     * @param accountId
     * @param type
     * @param delta
     */
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta);
    public void incrementResourceCountWithTag(long accountId, ResourceType type, String tag, Long... delta);

    /**
     * Decrements the resource count
     *
     * @param accountId
     * @param type
     * @param delta
     */
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta);
    public void decrementResourceCountWithTag(long accountId, ResourceType type, String tag, Long... delta);

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
    public void checkResourceLimitWithTag(Account account, ResourceCount.ResourceType type, String tag, long... count) throws ResourceAllocationException;

    /**
     * Gets the count of resources for a resource type and account
     *
     * @param account
     * @param type
     * @param tag
     * @return count of resources
     */
    public long getResourceCount(Account account, ResourceType type, String tag);

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

    List<String> getResourceLimitHostTags();
    List<String> getResourceLimitHostTags(ServiceOffering serviceOffering, VirtualMachineTemplate template);
    List<String> getResourceLimitStorageTags();
    List<String> getResourceLimitStorageTags(DiskOffering diskOffering);
    void updateTaggedResourceLimitsAndCountsForAccounts(List<AccountResponse> responses, String tag);
    void updateTaggedResourceLimitsAndCountsForDomains(List<DomainResponse> responses, String tag);
    void checkVolumeResourceLimit(Account owner, Boolean display, Long size, DiskOffering diskOffering) throws ResourceAllocationException;

    void checkVolumeResourceLimitForDiskOfferingChange(Account owner, Boolean display, Long currentSize, Long newSize,
            DiskOffering currentOffering, DiskOffering newOffering) throws ResourceAllocationException;

    void checkPrimaryStorageResourceLimit(Account owner, Boolean display, Long size, DiskOffering diskOffering) throws ResourceAllocationException;

    void incrementVolumeResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering);
    void decrementVolumeResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering);

    void updateVmResourceCountForTemplateChange(long accountId, Boolean display, ServiceOffering offering, VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate);

    void updateVmResourceCountForServiceOfferingChange(long accountId, Boolean display, Long currentCpu, Long newCpu, Long currentMemory,
            Long newMemory,
            ServiceOffering currentOffering, ServiceOffering newOffering,
            VirtualMachineTemplate template);

    void updateVolumeResourceCountForDiskOfferingChange(long accountId, Boolean display, Long currentSize, Long newSize,
            DiskOffering currentDiskOffering, DiskOffering newDiskOffering);

    void incrementVolumePrimaryStorageResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering);
    void decrementVolumePrimaryStorageResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering);
    void checkVmResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) throws ResourceAllocationException;
    void incrementVmResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template);
    void decrementVmResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template);

    void checkVmResourceLimitsForServiceOfferingChange(Account owner, Boolean display, Long currentCpu, Long newCpu,
            Long currentMemory, Long newMemory, ServiceOffering currentOffering, ServiceOffering newOffering, VirtualMachineTemplate template) throws ResourceAllocationException;

    void checkVmResourceLimitsForTemplateChange(Account owner, Boolean display, ServiceOffering offering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate) throws ResourceAllocationException;

    void checkVmCpuResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) throws ResourceAllocationException;
    void incrementVmCpuResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu);
    void decrementVmCpuResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu);
    void checkVmMemoryResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) throws ResourceAllocationException;
    void incrementVmMemoryResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory);
    void decrementVmMemoryResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory);

}
