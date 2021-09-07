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
package com.cloud.storage.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class VMTemplateDaoImpl extends GenericDaoBase<VMTemplateVO, Long> implements VMTemplateDao {
    private static final Logger s_logger = Logger.getLogger(VMTemplateDaoImpl.class);

    @Inject
    VMTemplateZoneDao _templateZoneDao;
    @Inject
    VMTemplateDetailsDao _templateDetailsDao;

    @Inject
    HostDao _hostDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    TemplateDataStoreDao _templateDataStoreDao;

    protected SearchBuilder<VMTemplateVO> TemplateNameSearch;
    protected SearchBuilder<VMTemplateVO> UniqueNameSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeHyperSearch;
    protected SearchBuilder<VMTemplateVO> readySystemTemplateSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeHyperSearch2;

    protected SearchBuilder<VMTemplateVO> AccountIdSearch;
    protected SearchBuilder<VMTemplateVO> NameSearch;
    protected SearchBuilder<VMTemplateVO> ValidNameSearch;
    protected SearchBuilder<VMTemplateVO> TmpltsInZoneSearch;
    protected SearchBuilder<VMTemplateVO> ActiveTmpltSearch;
    private SearchBuilder<VMTemplateVO> PublicSearch;
    private SearchBuilder<VMTemplateVO> NameAccountIdSearch;
    private SearchBuilder<VMTemplateVO> PublicIsoSearch;
    private SearchBuilder<VMTemplateVO> UserIsoSearch;
    private GenericSearchBuilder<VMTemplateVO, Long> CountTemplatesByAccount;
    // private SearchBuilder<VMTemplateVO> updateStateSearch;
    private SearchBuilder<VMTemplateVO> AllFieldsSearch;
    protected SearchBuilder<VMTemplateVO> ParentTemplateIdSearch;
    private SearchBuilder<VMTemplateVO> InactiveUnremovedTmpltSearch;

    @Inject
    ResourceTagDao _tagsDao;

    private String routerTmpltName;
    private String consoleProxyTmpltName;

    public VMTemplateDaoImpl() {
    }

    @Override
    public List<VMTemplateVO> listByPublic() {
        SearchCriteria<VMTemplateVO> sc = PublicSearch.create();
        sc.setParameters("public", 1);
        return listBy(sc);
    }

    @Override
    public VMTemplateVO findByName(String templateName) {
        SearchCriteria<VMTemplateVO> sc = UniqueNameSearch.create();
        sc.setParameters("uniqueName", templateName);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateVO findByTemplateName(String templateName) {
        SearchCriteria<VMTemplateVO> sc = NameSearch.create();
        sc.setParameters("name", templateName);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateVO findValidByTemplateName(String templateName) {
        SearchCriteria<VMTemplateVO> sc = ValidNameSearch.create();
        sc.setParameters("name", templateName);
        sc.setParameters("state", VirtualMachineTemplate.State.Active);
        return findOneBy(sc);
    }

    @Override
    public List<VMTemplateVO> listByParentTemplatetId(long parentTemplatetId) {
        SearchCriteria<VMTemplateVO> sc = ParentTemplateIdSearch.create();
        sc.setParameters("parentTemplateId", parentTemplatetId);
        sc.setParameters("state", VirtualMachineTemplate.State.Active);
        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> publicIsoSearch(Boolean bootable, boolean listRemoved, Map<String, String> tags) {

        SearchBuilder<VMTemplateVO> sb = null;
        if (tags == null || tags.isEmpty()) {
            sb = PublicIsoSearch;
        } else {
            sb = createSearchBuilder();
            sb.and("public", sb.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
            sb.and("format", sb.entity().getFormat(), SearchCriteria.Op.EQ);
            sb.and("type", sb.entity().getTemplateType(), SearchCriteria.Op.EQ);
            sb.and("bootable", sb.entity().isBootable(), SearchCriteria.Op.EQ);
            sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

            SearchBuilder<ResourceTagVO> tagSearch = _tagsDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<VMTemplateVO> sc = sb.create();

        sc.setParameters("public", 1);
        sc.setParameters("format", "ISO");
        sc.setParameters("type", TemplateType.PERHOST.toString());
        if (bootable != null) {
            sc.setParameters("bootable", bootable);
        }

        if (!listRemoved) {
            sc.setParameters("state", VirtualMachineTemplate.State.Active);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.ISO.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> userIsoSearch(boolean listRemoved) {

        SearchBuilder<VMTemplateVO> sb = null;
        sb = UserIsoSearch;
        SearchCriteria<VMTemplateVO> sc = sb.create();

        sc.setParameters("format", Storage.ImageFormat.ISO);
        sc.setParameters("type", TemplateType.USER.toString());

        if (!listRemoved) {
            sc.setParameters("state", VirtualMachineTemplate.State.Active);
        }

        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> listAllSystemVMTemplates() {
        SearchCriteria<VMTemplateVO> sc = tmpltTypeSearch.create();
        sc.setParameters("templateType", Storage.TemplateType.SYSTEM);

        Filter filter = new Filter(VMTemplateVO.class, "id", false, null, null);
        return listBy(sc, filter);
    }

    @Override
    public List<VMTemplateVO> listReadyTemplates() {
        SearchCriteria<VMTemplateVO> sc = createSearchCriteria();
        sc.addAnd("ready", SearchCriteria.Op.EQ, true);
        sc.addAnd("format", SearchCriteria.Op.NEQ, Storage.ImageFormat.ISO);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateVO> findIsosByIdAndPath(Long domainId, Long accountId, String path) {
        SearchCriteria<VMTemplateVO> sc = createSearchCriteria();
        sc.addAnd("iso", SearchCriteria.Op.EQ, true);
        if (domainId != null) {
            sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
        }
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        if (path != null) {
            sc.addAnd("path", SearchCriteria.Op.EQ, path);
        }
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateVO> listByAccountId(long accountId) {
        SearchCriteria<VMTemplateVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("state", VirtualMachineTemplate.State.Active);
        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> listByHypervisorType(List<HypervisorType> hyperTypes) {
        SearchCriteria<VMTemplateVO> sc = createSearchCriteria();
        hyperTypes.add(HypervisorType.None);
        sc.addAnd("hypervisorType", SearchCriteria.Op.IN, hyperTypes.toArray());
        return listBy(sc);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        boolean result = super.configure(name, params);

        PublicSearch = createSearchBuilder();
        PublicSearch.and("public", PublicSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);

        routerTmpltName = (String)params.get("routing.uniquename");

        s_logger.debug("Found parameter routing unique name " + routerTmpltName);
        if (routerTmpltName == null) {
            routerTmpltName = "routing";
        }

        consoleProxyTmpltName = (String)params.get("consoleproxy.uniquename");
        if (consoleProxyTmpltName == null) {
            consoleProxyTmpltName = "routing";
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Use console proxy template : " + consoleProxyTmpltName);
        }

        UniqueNameSearch = createSearchBuilder();
        UniqueNameSearch.and("uniqueName", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        ValidNameSearch = createSearchBuilder();
        ValidNameSearch.and("name", ValidNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        ValidNameSearch.and("state", ValidNameSearch.entity().getState(), SearchCriteria.Op.EQ);
        ValidNameSearch.and("removed", ValidNameSearch.entity().getRemoved(), SearchCriteria.Op.NULL);

        NameAccountIdSearch = createSearchBuilder();
        NameAccountIdSearch.and("name", NameAccountIdSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameAccountIdSearch.and("accountId", NameAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);

        PublicIsoSearch = createSearchBuilder();
        PublicIsoSearch.and("public", PublicIsoSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
        PublicIsoSearch.and("format", PublicIsoSearch.entity().getFormat(), SearchCriteria.Op.EQ);
        PublicIsoSearch.and("type", PublicIsoSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
        PublicIsoSearch.and("bootable", PublicIsoSearch.entity().isBootable(), SearchCriteria.Op.EQ);
        PublicIsoSearch.and("state", PublicIsoSearch.entity().getState(), SearchCriteria.Op.EQ);

        UserIsoSearch = createSearchBuilder();
        UserIsoSearch.and("format", UserIsoSearch.entity().getFormat(), SearchCriteria.Op.EQ);
        UserIsoSearch.and("type", UserIsoSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
        UserIsoSearch.and("state", UserIsoSearch.entity().getState(), SearchCriteria.Op.EQ);

        tmpltTypeHyperSearch = createSearchBuilder();
        tmpltTypeHyperSearch.and("templateType", tmpltTypeHyperSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
        SearchBuilder<HostVO> hostHyperSearch = _hostDao.createSearchBuilder();
        hostHyperSearch.and("type", hostHyperSearch.entity().getType(), SearchCriteria.Op.EQ);
        hostHyperSearch.and("zoneId", hostHyperSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        hostHyperSearch.groupBy(hostHyperSearch.entity().getHypervisorType());

        tmpltTypeHyperSearch.join("tmplHyper", hostHyperSearch, hostHyperSearch.entity().getHypervisorType(), tmpltTypeHyperSearch.entity().getHypervisorType(),
            JoinBuilder.JoinType.INNER);
        hostHyperSearch.done();
        tmpltTypeHyperSearch.done();

        readySystemTemplateSearch = createSearchBuilder();
        readySystemTemplateSearch.and("state", readySystemTemplateSearch.entity().getState(), SearchCriteria.Op.EQ);
        readySystemTemplateSearch.and("templateType", readySystemTemplateSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
        SearchBuilder<TemplateDataStoreVO> templateDownloadSearch = _templateDataStoreDao.createSearchBuilder();
        templateDownloadSearch.and("downloadState", templateDownloadSearch.entity().getDownloadState(), SearchCriteria.Op.IN);
        readySystemTemplateSearch.join("vmTemplateJoinTemplateStoreRef", templateDownloadSearch, templateDownloadSearch.entity().getTemplateId(),
            readySystemTemplateSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        SearchBuilder<HostVO> hostHyperSearch2 = _hostDao.createSearchBuilder();
        hostHyperSearch2.and("type", hostHyperSearch2.entity().getType(), SearchCriteria.Op.EQ);
        hostHyperSearch2.and("zoneId", hostHyperSearch2.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        hostHyperSearch2.and("removed", hostHyperSearch2.entity().getRemoved(), SearchCriteria.Op.NULL);
        hostHyperSearch2.groupBy(hostHyperSearch2.entity().getHypervisorType());

        readySystemTemplateSearch.join("tmplHyper", hostHyperSearch2, hostHyperSearch2.entity().getHypervisorType(), readySystemTemplateSearch.entity()
            .getHypervisorType(), JoinBuilder.JoinType.INNER);
        hostHyperSearch2.done();
        readySystemTemplateSearch.done();

        tmpltTypeHyperSearch2 = createSearchBuilder();
        tmpltTypeHyperSearch2.and("templateType", tmpltTypeHyperSearch2.entity().getTemplateType(), SearchCriteria.Op.EQ);
        tmpltTypeHyperSearch2.and("hypervisorType", tmpltTypeHyperSearch2.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        tmpltTypeHyperSearch2.and("templateName", tmpltTypeHyperSearch2.entity().getName(), SearchCriteria.Op.EQ);
        tmpltTypeHyperSearch2.and("state", tmpltTypeHyperSearch2.entity().getState(), SearchCriteria.Op.EQ);

        tmpltTypeSearch = createSearchBuilder();
        tmpltTypeSearch.and("state", tmpltTypeSearch.entity().getState(), SearchCriteria.Op.EQ);
        tmpltTypeSearch.and("templateType", tmpltTypeSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);

        AccountIdSearch = createSearchBuilder();
        AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.and("publicTemplate", AccountIdSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
        AccountIdSearch.and("state", AccountIdSearch.entity().getState(), SearchCriteria.Op.EQ); // only list not removed templates for this account
        AccountIdSearch.done();

        SearchBuilder<VMTemplateZoneVO> tmpltZoneSearch = _templateZoneDao.createSearchBuilder();
        tmpltZoneSearch.and("removed", tmpltZoneSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        tmpltZoneSearch.and("zoneId", tmpltZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);

        TmpltsInZoneSearch = createSearchBuilder();
        TmpltsInZoneSearch.and("state", TmpltsInZoneSearch.entity().getState(), SearchCriteria.Op.IN);
        TmpltsInZoneSearch.and().op("avoidtype", TmpltsInZoneSearch.entity().getTemplateType(), SearchCriteria.Op.NEQ);
        TmpltsInZoneSearch.or("templateType", TmpltsInZoneSearch.entity().getTemplateType(), SearchCriteria.Op.NULL);
        TmpltsInZoneSearch.cp();
        TmpltsInZoneSearch.join("tmpltzone", tmpltZoneSearch, tmpltZoneSearch.entity().getTemplateId(), TmpltsInZoneSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        tmpltZoneSearch.done();
        TmpltsInZoneSearch.done();

        ActiveTmpltSearch = createSearchBuilder();
        ActiveTmpltSearch.and("state", ActiveTmpltSearch.entity().getState(), SearchCriteria.Op.IN);

        CountTemplatesByAccount = createSearchBuilder(Long.class);
        CountTemplatesByAccount.select(null, Func.COUNT, null);
        CountTemplatesByAccount.and("account", CountTemplatesByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
        CountTemplatesByAccount.and("state", CountTemplatesByAccount.entity().getState(), SearchCriteria.Op.EQ);
        CountTemplatesByAccount.done();

        //        updateStateSearch = this.createSearchBuilder();
        //        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        //        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        //        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        //        updateStateSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("state", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("destroyed", AllFieldsSearch.entity().getState(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("notDestroyed", AllFieldsSearch.entity().getState(), SearchCriteria.Op.NEQ);
        AllFieldsSearch.and("updatedCount", AllFieldsSearch.entity().getUpdatedCount(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();

        ParentTemplateIdSearch = createSearchBuilder();
        ParentTemplateIdSearch.and("parentTemplateId", ParentTemplateIdSearch.entity().getParentTemplateId(), SearchCriteria.Op.EQ);
        ParentTemplateIdSearch.and("state", ParentTemplateIdSearch.entity().getState(), SearchCriteria.Op.EQ);
        ParentTemplateIdSearch.done();

        InactiveUnremovedTmpltSearch = createSearchBuilder();
        InactiveUnremovedTmpltSearch.and("state", InactiveUnremovedTmpltSearch.entity().getState(), SearchCriteria.Op.IN);
        InactiveUnremovedTmpltSearch.and("removed", InactiveUnremovedTmpltSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        InactiveUnremovedTmpltSearch.done();

        return result;
    }

    @Override
    public String getRoutingTemplateUniqueName() {
        return routerTmpltName;
    }

    @Override
    public void loadDetails(VMTemplateVO tmpl) {
        Map<String, String> details = _templateDetailsDao.listDetailsKeyPairs(tmpl.getId());
        tmpl.setDetails(details);
    }

    @Override
    public void saveDetails(VMTemplateVO tmpl) {
        Map<String, String> detailsStr = tmpl.getDetails();
        if (detailsStr == null) {
            return;
        }
        List<VMTemplateDetailVO> details = new ArrayList<VMTemplateDetailVO>();
        for (String key : detailsStr.keySet()) {
            VMTemplateDetailVO detail = new VMTemplateDetailVO(tmpl.getId(), key, detailsStr.get(key), true);
            details.add(detail);
        }

        _templateDetailsDao.saveDetails(details);
    }

    @SuppressWarnings("unchecked")
    @Override
    @DB
    public long addTemplateToZone(VMTemplateVO tmplt, long zoneId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        VMTemplateVO tmplt2 = findById(tmplt.getId());
        if (tmplt2 == null) {
            if (persist(tmplt) == null) {
                throw new CloudRuntimeException("Failed to persist the template " + tmplt);
            }

            if (tmplt.getDetails() != null) {
                List<VMTemplateDetailVO> details = new ArrayList<VMTemplateDetailVO>();
                for (String key : tmplt.getDetails().keySet()) {
                    details.add(new VMTemplateDetailVO(tmplt.getId(), key, tmplt.getDetails().get(key), true));
                }
                _templateDetailsDao.saveDetails(details);
            }
        }
        VMTemplateZoneVO tmpltZoneVO = _templateZoneDao.findByZoneTemplate(zoneId, tmplt.getId());
        if (tmpltZoneVO == null) {
            tmpltZoneVO = new VMTemplateZoneVO(zoneId, tmplt.getId(), new Date());
            _templateZoneDao.persist(tmpltZoneVO);
        } else {
            tmpltZoneVO.setRemoved(GenericDaoBase.DATE_TO_NULL);
            tmpltZoneVO.setLastUpdated(new Date());
            _templateZoneDao.update(tmpltZoneVO.getId(), tmpltZoneVO);
        }
        txn.commit();

        return tmplt.getId();
    }

    @Override
    @DB
    public List<VMTemplateVO> listAllInZone(long dataCenterId) {
        SearchCriteria<VMTemplateVO> sc = TmpltsInZoneSearch.create();
        sc.setParameters("avoidtype", TemplateType.PERHOST.toString());
        sc.setParameters("state", VirtualMachineTemplate.State.Active);
        sc.setJoinParameters("tmpltzone", "zoneId", dataCenterId);
        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> listInZoneByState(long dataCenterId, VirtualMachineTemplate.State... states) {
        SearchCriteria<VMTemplateVO> sc = TmpltsInZoneSearch.create();
        sc.setParameters("avoidtype", TemplateType.PERHOST.toString());
        sc.setParameters("state", (Object[])states);
        sc.setJoinParameters("tmpltzone", "zoneId", dataCenterId);
        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> listAllActive() {
        SearchCriteria<VMTemplateVO> sc = ActiveTmpltSearch.create();
        sc.setParameters("state", VirtualMachineTemplate.State.Active.toString());
        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> listByState(VirtualMachineTemplate.State... states) {
        SearchCriteria<VMTemplateVO> sc = ActiveTmpltSearch.create();
        sc.setParameters("state", (Object[])states);
        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> listDefaultBuiltinTemplates() {
        SearchCriteria<VMTemplateVO> sc = tmpltTypeSearch.create();
        sc.setParameters("templateType", Storage.TemplateType.BUILTIN);
        sc.setParameters("state", VirtualMachineTemplate.State.Active);
        return listBy(sc);
    }

    @Override
    public VMTemplateVO findSystemVMTemplate(long zoneId) {
        SearchCriteria<VMTemplateVO> sc = tmpltTypeHyperSearch.create();
        sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
        sc.setJoinParameters("tmplHyper", "type", Host.Type.Routing);
        sc.setJoinParameters("tmplHyper", "zoneId", zoneId);

        // order by descending order of id and select the first (this is going
        // to be the latest)
        List<VMTemplateVO> tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, 1l));

        if (tmplts.size() > 0) {
            return tmplts.get(0);
        } else {
            return null;
        }
    }

    @Override
    public VMTemplateVO findSystemVMReadyTemplate(long zoneId, HypervisorType hypervisorType) {
        SearchCriteria<VMTemplateVO> sc = readySystemTemplateSearch.create();
        sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
        sc.setParameters("state", VirtualMachineTemplate.State.Active);
        sc.setJoinParameters("tmplHyper", "type", Host.Type.Routing);
        sc.setJoinParameters("tmplHyper", "zoneId", zoneId);
        sc.setJoinParameters("vmTemplateJoinTemplateStoreRef", "downloadState", new VMTemplateStorageResourceAssoc.Status[] {VMTemplateStorageResourceAssoc.Status.DOWNLOADED, VMTemplateStorageResourceAssoc.Status.BYPASSED});

        // order by descending order of id
        List<VMTemplateVO> tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, null));

        if (tmplts.size() > 0) {
            if (hypervisorType == HypervisorType.Any) {
                return tmplts.get(0);
            }
            for (VMTemplateVO tmplt : tmplts) {
                if (tmplt.getHypervisorType() == hypervisorType) {
                    return tmplt;
                }
            }

        }
        return null;
    }

    @Override
    public VMTemplateVO findRoutingTemplate(HypervisorType hType, String templateName) {
        SearchCriteria<VMTemplateVO> sc = tmpltTypeHyperSearch2.create();
        sc.setParameters("templateType", TemplateType.ROUTING);
        sc.setParameters("hypervisorType", hType);
        sc.setParameters("state", VirtualMachineTemplate.State.Active.toString());
        if (templateName != null) {
            sc.setParameters("templateName", templateName);
        }

        // order by descending order of id and select the first (this is going
        // to be the latest)
        List<VMTemplateVO> tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, 1l));

        if (tmplts.size() > 0) {
            return tmplts.get(0);
        } else {
            sc = tmpltTypeHyperSearch2.create();
            sc.setParameters("templateType", TemplateType.SYSTEM);
            sc.setParameters("hypervisorType", hType);
            sc.setParameters("state", VirtualMachineTemplate.State.Active.toString());
            if (templateName != null) {
                sc.setParameters("templateName", templateName);
            }

            // order by descending order of id and select the first (this is going
            // to be the latest)
            tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, 1l));

            if (tmplts.size() > 0) {
                return tmplts.get(0);
            } else {
                return null;
            }
        }
    }

    @Override
    public Long countTemplatesForAccount(long accountId) {
        SearchCriteria<Long> sc = CountTemplatesByAccount.create();
        sc.setParameters("account", accountId);
        sc.setParameters("state", VirtualMachineTemplate.State.Active.toString());
        return customSearch(sc, null).get(0);
    }

    @Override
     public List<VMTemplateVO> listUnRemovedTemplatesByStates(VirtualMachineTemplate.State ...states) {
        SearchCriteria<VMTemplateVO> sc = InactiveUnremovedTmpltSearch.create();
        sc.setParameters("state", (Object[]) states);
        return listBy(sc);
    }

    @Override
    @DB
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        VMTemplateVO template = createForUpdate();
        template.setRemoved(new Date());

        VMTemplateVO vo = findById(id);
        if (vo != null) {
            if (vo.getFormat() == ImageFormat.ISO) {
                _tagsDao.removeByIdAndType(id, ResourceObjectType.ISO);
            } else {
                _tagsDao.removeByIdAndType(id, ResourceObjectType.Template);
            }
        }

        boolean result = update(id, template);
        txn.commit();
        return result;
    }

    @Override
    public boolean updateState(
            com.cloud.template.VirtualMachineTemplate.State currentState,
            com.cloud.template.VirtualMachineTemplate.Event event,
            com.cloud.template.VirtualMachineTemplate.State nextState,
            VirtualMachineTemplate vo, Object data) {

        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getUpdated();

        SearchCriteria<VMTemplateVO> sc = AllFieldsSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());

        vo.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VMTemplateVO)vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VMTemplateVO dbTemplate = findByIdIncludingRemoved(vo.getId());
            if (dbTemplate != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=")
                    .append(dbTemplate.getId())
                    .append("; state=")
                    .append(dbTemplate.getState())
                    .append("; updatecount=")
                    .append(dbTemplate.getUpdatedCount())
                    .append(";updatedTime=")
                    .append(dbTemplate.getUpdated());
                str.append(": New Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(nextState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(vo.getUpdatedCount())
                    .append("; updatedTime=")
                    .append(vo.getUpdated());
                str.append(": stale Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(currentState)
                    .append("; event=")
                    .append(event)
                    .append("; updatecount=")
                    .append(oldUpdated)
                    .append("; updatedTime=")
                    .append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update template: id=" + vo.getId() + ", as no such template exists in the database anymore");
            }
        }
        return rows > 0;
    }
}
