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
package com.cloud.vpc;


import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.springframework.stereotype.Component;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimit;
import com.cloud.domain.Domain;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.component.ManagerBase;

@Component
public class MockResourceLimitManagerImpl extends ManagerBase implements ResourceLimitService {

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#updateResourceLimit(java.lang.Long, java.lang.Long, java.lang.Integer, java.lang.Long, java.lang.String)
     */
    @Override
    public ResourceLimit updateResourceLimit(Long accountId, Long domainId, Integer resourceType, Long max, String tag) {
        return null;
    }

    @Override
    public List<? extends ResourceCount> recalculateResourceCount(Long accountId, Long domainId, Integer typeId, String tag) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#recalculateResourceCount(java.lang.Long, java.lang.Long, java.lang.Integer)
     */
    @Override
    public List<? extends ResourceCount> recalculateResourceCount(Long accountId, Long domainId, Integer typeId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#searchForLimits(java.lang.Long, java.lang.Long, java.lang.Long, com.cloud.user.ResourceLimitService, java.lang.Long, java.lang.Long)
     */
    @Override
    public List<? extends ResourceLimit> searchForLimits(Long id, Long accountId, Long domainId, ResourceType resourceType, String tag, Long startIndex, Long pageSizeVal) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#findCorrectResourceLimitForAccount(com.cloud.user.Account, com.cloud.configuration.Resource.ResourceType, java.lang.String)
     */
    @Override
    public long findCorrectResourceLimitForAccount(Account account, ResourceType type, String tag) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long findCorrectResourceLimitForAccount(long accountId, Long limit, ResourceType type) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#findCorrectResourceLimitForDomain(com.cloud.domain.Domain, com.cloud.configuration.Resource.ResourceType, java.lang.String)
     */
    @Override
    public long findCorrectResourceLimitForDomain(Domain domain, ResourceType type, String tag) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long findDefaultResourceLimitForDomain(ResourceType resourceType) {
        return 0;
    }

    @Override
    public long findCorrectResourceLimitForAccountAndDomain(Account account, Domain domain, ResourceType type, String tag) {
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#incrementResourceCount(long, com.cloud.configuration.Resource.ResourceType, java.lang.Long[])
     */
    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Long... delta) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#decrementResourceCount(long, com.cloud.configuration.Resource.ResourceType, java.lang.Long[])
     */
    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Long... delta) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#checkResourceLimit(com.cloud.user.Account, com.cloud.configuration.Resource.ResourceType, long[])
     */
    @Override
    public void checkResourceLimit(Account account, ResourceType type, long... count) throws ResourceAllocationException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#countCpusForAccount(long)
     */
    public long countCpusForAccount(long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#calculateRAMForAccount(long)
     */
    public long calculateMemoryForAccount(long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#calculateSecondaryStorageForAccount(long)
     */
    public long calculateSecondaryStorageForAccount(long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.user.ResourceLimitService#getResourceCount(com.cloud.user.Account, com.cloud.configuration.Resource.ResourceType, java.lang.String)
     */
    @Override
    public long getResourceCount(Account account, ResourceType type, String tag) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void checkResourceLimit(Account account, ResourceType type, Boolean displayResource, long... count) throws ResourceAllocationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void incrementResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void changeResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void decrementResourceCount(long accountId, ResourceType type, Boolean displayResource, Long... delta) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void incrementResourceCountWithTag(long accountId, ResourceType type, String tag, Long... delta) {

    }

    @Override
    public void decrementResourceCountWithTag(long accountId, ResourceType type, String tag, Long... delta) {

    }

    @Override
    public void checkResourceLimitWithTag(Account account, ResourceType type, String tag, long... count) throws ResourceAllocationException {

    }

    @Override
    public List<String> getResourceLimitHostTags() {
        return null;
    }

    @Override
    public List<String> getResourceLimitStorageTags() {
        return null;
    }

    @Override
    public void updateTaggedResourceLimitsAndCountsForAccounts(List<AccountResponse> responses, String tag) {

    }

    @Override
    public void updateTaggedResourceLimitsAndCountsForDomains(List<DomainResponse> responses, String tag) {

    }

    @Override
    public List<String> getResourceLimitHostTags(ServiceOffering serviceOffering, VirtualMachineTemplate template) {
        return null;
    }

    @Override
    public List<String> getResourceLimitStorageTags(DiskOffering diskOffering) {
        return null;
    }

    @Override
    public void checkVolumeResourceLimit(Account owner, Boolean display, Long size, DiskOffering diskOffering) throws ResourceAllocationException {

    }

    @Override
    public void checkVolumeResourceLimitForDiskOfferingChange(Account owner, Boolean display, Long currentSize, Long newSize,
            DiskOffering currentOffering, DiskOffering newOffering) throws ResourceAllocationException {

    }

    @Override
    public void checkPrimaryStorageResourceLimit(Account owner, Boolean display, Long size,
            DiskOffering diskOffering) {

    }

    @Override
    public void incrementVolumeResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {

    }

    @Override
    public void decrementVolumeResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {

    }

    @Override
    public void updateVmResourceCountForTemplateChange(long accountId, Boolean display, ServiceOffering offering,
            VirtualMachineTemplate currentTemplate, VirtualMachineTemplate newTemplate) {

    }

    @Override
    public void updateVmResourceCountForServiceOfferingChange(long accountId, Boolean display, Long currentCpu, Long newCpu,
            Long currentMemory, Long newMemory, ServiceOffering currentOffering, ServiceOffering newOffering,
            VirtualMachineTemplate template) {

    }

    @Override
    public void updateVolumeResourceCountForDiskOfferingChange(long accountId, Boolean display, Long currentSize, Long newSize,
            DiskOffering currentDiskOffering, DiskOffering newDiskOffering) {

    }

    @Override
    public void incrementVolumePrimaryStorageResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {

    }

    @Override
    public void decrementVolumePrimaryStorageResourceCount(long accountId, Boolean display, Long size, DiskOffering diskOffering) {

    }

    @Override
    public void checkVmResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) throws ResourceAllocationException {

    }

    @Override
    public void incrementVmResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) {

    }

    @Override
    public void decrementVmResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template) {

    }

    @Override
    public void checkVmResourceLimitsForServiceOfferingChange(Account owner, Boolean display, Long currentCpu, Long newCpu,
            Long currentMemory, Long newMemory, ServiceOffering currentOffering, ServiceOffering newOffering,
            VirtualMachineTemplate template) throws ResourceAllocationException {

    }

    @Override
    public void checkVmResourceLimitsForTemplateChange(Account owner, Boolean display, ServiceOffering offering,
            VirtualMachineTemplate currentTemplate,
            VirtualMachineTemplate newTemplate) throws ResourceAllocationException {

    }

    @Override
    public void checkVmCpuResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) throws ResourceAllocationException {

    }

    @Override
    public void incrementVmCpuResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) {

    }

    @Override
    public void decrementVmCpuResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long cpu) {

    }

    @Override
    public void checkVmMemoryResourceLimit(Account owner, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) throws ResourceAllocationException {

    }

    @Override
    public void incrementVmMemoryResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) {

    }

    @Override
    public void decrementVmMemoryResourceCount(long accountId, Boolean display, ServiceOffering serviceOffering, VirtualMachineTemplate template, Long memory) {

    }
}
