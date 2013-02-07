/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image.db;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.storage.image.TemplateEvent;
import org.apache.cloudstack.storage.image.TemplateState;

import com.cloud.domain.DomainVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface ImageDataDao extends GenericDao<ImageDataVO, Long>, StateDao<TemplateState, TemplateEvent, ImageDataVO> {
    public List<ImageDataVO> listByPublic();

    public ImageDataVO findByName(String templateName);

    public ImageDataVO findByTemplateName(String templateName);

    // public void update(ImageDataVO template);

    public List<ImageDataVO> listAllSystemVMTemplates();

    public List<ImageDataVO> listDefaultBuiltinTemplates();

    public String getRoutingTemplateUniqueName();

    public List<ImageDataVO> findIsosByIdAndPath(Long domainId, Long accountId, String path);

    public List<ImageDataVO> listReadyTemplates();

    public List<ImageDataVO> listByAccountId(long accountId);

    public Set<Pair<Long, Long>> searchTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, List<HypervisorType> hypers, Boolean bootable, DomainVO domain,
            Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean onlyReady, boolean showDomr, List<Account> permittedAccounts, Account caller,
            ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags);

    public Set<Pair<Long, Long>> searchSwiftTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, List<HypervisorType> hypers, Boolean bootable, DomainVO domain,
            Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean onlyReady, boolean showDomr, List<Account> permittedAccounts, Account caller, Map<String, String> tags);

    public long addTemplateToZone(ImageDataVO tmplt, long zoneId);

    public List<ImageDataVO> listAllInZone(long dataCenterId);

    public List<ImageDataVO> listByHypervisorType(List<HypervisorType> hyperTypes);

    public List<ImageDataVO> publicIsoSearch(Boolean bootable, boolean listRemoved, Map<String, String> tags);

    public List<ImageDataVO> userIsoSearch(boolean listRemoved);

    ImageDataVO findSystemVMTemplate(long zoneId);

    ImageDataVO findSystemVMTemplate(long zoneId, HypervisorType hType);

    ImageDataVO findRoutingTemplate(HypervisorType type);

    List<Long> listPrivateTemplatesByHost(Long hostId);

    public Long countTemplatesForAccount(long accountId);

}
