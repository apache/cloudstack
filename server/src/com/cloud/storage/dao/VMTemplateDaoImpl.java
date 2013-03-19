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

import static com.cloud.utils.StringUtils.join;
import static com.cloud.utils.db.DbUtil.closeResources;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateEvent;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateState;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ResourceTag.TaggedResourceType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value={VMTemplateDao.class})
public class VMTemplateDaoImpl extends GenericDaoBase<VMTemplateVO, Long> implements VMTemplateDao {
    private static final Logger s_logger = Logger.getLogger(VMTemplateDaoImpl.class);

    @Inject
    VMTemplateZoneDao _templateZoneDao;
    @Inject
    VMTemplateDetailsDao _templateDetailsDao;

    @Inject
    ConfigurationDao  _configDao;
    @Inject
    HostDao   _hostDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    DataCenterDao _dcDao;
    private final String SELECT_TEMPLATE_HOST_REF = "SELECT t.id, h.data_center_id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, " +
    								"t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t";

    private final String SELECT_TEMPLATE_ZONE_REF = "SELECT t.id, tzr.zone_id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, " +
									"t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t INNER JOIN template_zone_ref tzr on (t.id = tzr.template_id) ";

    private final String SELECT_TEMPLATE_SWIFT_REF = "SELECT t.id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, "
            + "t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t";

    private final String SELECT_TEMPLATE_S3_REF = "SELECT t.id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, "
            + "t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t";

    private static final String SELECT_S3_CANDIDATE_TEMPLATES = "SELECT t.id, t.unique_name, t.name, t.public, t.featured, " +
        "t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, t.checksum, t.display_text, " +
        "t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type " +
        "FROM vm_template t JOIN template_host_ref r ON t.id=r.template_id JOIN host h ON h.id=r.host_id " +
        "WHERE t.hypervisor_type IN (SELECT hypervisor_type FROM host) AND r.download_state = 'DOWNLOADED' AND " +
        "r.template_id NOT IN (SELECT template_id FROM template_s3_ref) AND r.destroyed = 0 AND t.type <> 'PERHOST'";

    protected SearchBuilder<VMTemplateVO> TemplateNameSearch;
    protected SearchBuilder<VMTemplateVO> UniqueNameSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeHyperSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeHyperSearch2;

    protected SearchBuilder<VMTemplateVO> AccountIdSearch;
    protected SearchBuilder<VMTemplateVO> NameSearch;
    protected SearchBuilder<VMTemplateVO> TmpltsInZoneSearch;
    private SearchBuilder<VMTemplateVO> PublicSearch;
    private SearchBuilder<VMTemplateVO> NameAccountIdSearch;
    private SearchBuilder<VMTemplateVO> PublicIsoSearch;
    private SearchBuilder<VMTemplateVO> UserIsoSearch;
    private GenericSearchBuilder<VMTemplateVO, Long> CountTemplatesByAccount;
    private SearchBuilder<VMTemplateVO> updateStateSearch;

    @Inject ResourceTagDao _tagsDao;


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
    public List<VMTemplateVO> publicIsoSearch(Boolean bootable, boolean listRemoved, Map<String, String> tags){

        SearchBuilder<VMTemplateVO> sb = null;
        if (tags == null || tags.isEmpty()) {
            sb = PublicIsoSearch;
        } else {
            sb = createSearchBuilder();
            sb.and("public", sb.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
            sb.and("format", sb.entity().getFormat(), SearchCriteria.Op.EQ);
            sb.and("type", sb.entity().getTemplateType(), SearchCriteria.Op.EQ);
            sb.and("bootable", sb.entity().isBootable(), SearchCriteria.Op.EQ);
            sb.and("removed", sb.entity().getRemoved(), SearchCriteria.Op.EQ);

            SearchBuilder<ResourceTagVO> tagSearch = _tagsDao.createSearchBuilder();
            for (int count=0; count < tags.size(); count++) {
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
    		sc.setParameters("removed", (Object)null);
    	}

    	if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", TaggedResourceType.ISO.toString());
            for (String key : tags.keySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), key);
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), tags.get(key));
                count++;
            }
        }

        return listBy(sc);
    }

    @Override
    public List<VMTemplateVO> userIsoSearch(boolean listRemoved){

        SearchBuilder<VMTemplateVO> sb = null;
        sb = UserIsoSearch;
        SearchCriteria<VMTemplateVO> sc = sb.create();

        sc.setParameters("format", Storage.ImageFormat.ISO);
        sc.setParameters("type", TemplateType.USER.toString());

        if (!listRemoved) {
            sc.setParameters("removed", (Object)null);
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
    public List<Long> listPrivateTemplatesByHost(Long hostId) {

        String sql = "select * from template_host_ref as thr INNER JOIN vm_template as t ON t.id=thr.template_id "
            + "where thr.host_id=? and t.public=0 and t.featured=0 and t.type='USER' and t.removed is NULL";

        List<Long> l = new ArrayList<Long>();

        Transaction txn = Transaction.currentTxn();

        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, hostId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                l.add(rs.getLong(1));
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
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
		if (routerTmpltName==null) {
			routerTmpltName="routing";
		}

		consoleProxyTmpltName = (String)params.get("consoleproxy.uniquename");
		if(consoleProxyTmpltName == null) {
            consoleProxyTmpltName = "routing";
        }
		if(s_logger.isDebugEnabled()) {
            s_logger.debug("Use console proxy template : " + consoleProxyTmpltName);
        }

		UniqueNameSearch = createSearchBuilder();
		UniqueNameSearch.and("uniqueName", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
		NameSearch = createSearchBuilder();
		NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);

		NameAccountIdSearch = createSearchBuilder();
		NameAccountIdSearch.and("name", NameAccountIdSearch.entity().getName(), SearchCriteria.Op.EQ);
		NameAccountIdSearch.and("accountId", NameAccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);

		PublicIsoSearch = createSearchBuilder();
		PublicIsoSearch.and("public", PublicIsoSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
		PublicIsoSearch.and("format", PublicIsoSearch.entity().getFormat(), SearchCriteria.Op.EQ);
		PublicIsoSearch.and("type", PublicIsoSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
		PublicIsoSearch.and("bootable", PublicIsoSearch.entity().isBootable(), SearchCriteria.Op.EQ);
		PublicIsoSearch.and("removed", PublicIsoSearch.entity().getRemoved(), SearchCriteria.Op.EQ);

		UserIsoSearch = createSearchBuilder();
		UserIsoSearch.and("format", UserIsoSearch.entity().getFormat(), SearchCriteria.Op.EQ);
		UserIsoSearch.and("type", UserIsoSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
		UserIsoSearch.and("removed", UserIsoSearch.entity().getRemoved(), SearchCriteria.Op.EQ);

		tmpltTypeHyperSearch = createSearchBuilder();
		tmpltTypeHyperSearch.and("templateType", tmpltTypeHyperSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
		SearchBuilder<HostVO> hostHyperSearch = _hostDao.createSearchBuilder();
		hostHyperSearch.and("type", hostHyperSearch.entity().getType(), SearchCriteria.Op.EQ);
		hostHyperSearch.and("zoneId", hostHyperSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
		hostHyperSearch.groupBy(hostHyperSearch.entity().getHypervisorType());

		tmpltTypeHyperSearch.join("tmplHyper", hostHyperSearch, hostHyperSearch.entity().getHypervisorType(), tmpltTypeHyperSearch.entity().getHypervisorType(), JoinBuilder.JoinType.INNER);
		hostHyperSearch.done();
		tmpltTypeHyperSearch.done();

		tmpltTypeHyperSearch2 = createSearchBuilder();
		tmpltTypeHyperSearch2.and("templateType", tmpltTypeHyperSearch2.entity().getTemplateType(), SearchCriteria.Op.EQ);
		tmpltTypeHyperSearch2.and("hypervisorType", tmpltTypeHyperSearch2.entity().getHypervisorType(), SearchCriteria.Op.EQ);


		tmpltTypeSearch = createSearchBuilder();
        tmpltTypeSearch.and("removed", tmpltTypeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
		tmpltTypeSearch.and("templateType", tmpltTypeSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);

		AccountIdSearch = createSearchBuilder();
		AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.and("publicTemplate", AccountIdSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
		AccountIdSearch.done();

		SearchBuilder<VMTemplateZoneVO> tmpltZoneSearch = _templateZoneDao.createSearchBuilder();
		tmpltZoneSearch.and("removed", tmpltZoneSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
		tmpltZoneSearch.and("zoneId", tmpltZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);

		TmpltsInZoneSearch = createSearchBuilder();
		TmpltsInZoneSearch.and("removed", TmpltsInZoneSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
		TmpltsInZoneSearch.and().op("avoidtype", TmpltsInZoneSearch.entity().getTemplateType(), SearchCriteria.Op.NEQ);
		TmpltsInZoneSearch.or("templateType", TmpltsInZoneSearch.entity().getTemplateType(), SearchCriteria.Op.NULL);
		TmpltsInZoneSearch.cp();
		TmpltsInZoneSearch.join("tmpltzone", tmpltZoneSearch, tmpltZoneSearch.entity().getTemplateId(), TmpltsInZoneSearch.entity().getId(), JoinBuilder.JoinType.INNER);
		tmpltZoneSearch.done();
		TmpltsInZoneSearch.done();

		CountTemplatesByAccount = createSearchBuilder(Long.class);
		CountTemplatesByAccount.select(null, Func.COUNT, null);
		CountTemplatesByAccount.and("account", CountTemplatesByAccount.entity().getAccountId(), SearchCriteria.Op.EQ);
		CountTemplatesByAccount.and("removed", CountTemplatesByAccount.entity().getRemoved(), SearchCriteria.Op.NULL);
		CountTemplatesByAccount.done();

        updateStateSearch = this.createSearchBuilder();
        updateStateSearch.and("id", updateStateSearch.entity().getId(), Op.EQ);
        updateStateSearch.and("state", updateStateSearch.entity().getState(), Op.EQ);
        updateStateSearch.and("updatedCount", updateStateSearch.entity().getUpdatedCount(), Op.EQ);
        updateStateSearch.done();

		return result;
	}

	@Override
	public String getRoutingTemplateUniqueName() {
		return routerTmpltName;
	}

    @Override
    public Set<Pair<Long, Long>> searchSwiftTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, List<HypervisorType> hypers, Boolean bootable, DomainVO domain,
            Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean onlyReady, boolean showDomr, List<Account> permittedAccounts, Account caller, Map<String, String> tags) {

        StringBuilder builder = new StringBuilder();
        if (!permittedAccounts.isEmpty()) {
            for (Account permittedAccount : permittedAccounts) {
                builder.append(permittedAccount.getAccountId() + ",");
            }
        }

        String permittedAccountsStr = builder.toString();

        if (permittedAccountsStr.length() > 0) {
            // chop the "," off
            permittedAccountsStr = permittedAccountsStr.substring(0, permittedAccountsStr.length() - 1);
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        Set<Pair<Long, Long>> templateZonePairList = new HashSet<Pair<Long, Long>>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = SELECT_TEMPLATE_SWIFT_REF;
        try {
            String joinClause = "";
            String whereClause = " WHERE t.removed IS NULL";

            if (isIso) {
                whereClause += " AND t.format = 'ISO'";
                if (!hyperType.equals(HypervisorType.None)) {
                    joinClause = " INNER JOIN guest_os guestOS on (guestOS.id = t.guest_os_id) INNER JOIN guest_os_hypervisor goh on ( goh.guest_os_id = guestOS.id) ";
                    whereClause += " AND goh.hypervisor_type = '" + hyperType.toString() + "'";
                }
            } else {
                whereClause += " AND t.format <> 'ISO'";
                if (hypers.isEmpty()) {
                    return templateZonePairList;
                } else {
                    StringBuilder relatedHypers = new StringBuilder();
                    for (HypervisorType hyper : hypers) {
                        relatedHypers.append("'");
                        relatedHypers.append(hyper.toString());
                        relatedHypers.append("'");
                        relatedHypers.append(",");
                    }
                    relatedHypers.setLength(relatedHypers.length() - 1);
                    whereClause += " AND t.hypervisor_type IN (" + relatedHypers + ")";
                }
            }
            joinClause += " INNER JOIN  template_swift_ref tsr on (t.id = tsr.template_id)";
            if (keyword != null) {
                whereClause += " AND t.name LIKE \"%" + keyword + "%\"";
            } else if (name != null) {
                whereClause += " AND t.name LIKE \"%" + name + "%\"";
            }

            if (bootable != null) {
                whereClause += " AND t.bootable = " + bootable;
            }

            if (!showDomr) {
                whereClause += " AND t.type != '" + Storage.TemplateType.SYSTEM.toString() + "'";
            }

            if (templateFilter == TemplateFilter.featured) {
                whereClause += " AND t.public = 1 AND t.featured = 1";
            } else if ((templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable) && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                    joinClause += " INNER JOIN account a on (t.account_id = a.id) INNER JOIN domain d on (a.domain_id = d.id)";
                    whereClause += "  AND d.path LIKE '" + domain.getPath() + "%'";
                } else {
                    whereClause += " AND t.account_id IN (" + permittedAccountsStr + ")";
                }
            } else if ((templateFilter == TemplateFilter.shared || templateFilter == TemplateFilter.sharedexecutable) && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                    joinClause += " LEFT JOIN launch_permission lp ON t.id = lp.template_id WHERE" + " (t.account_id IN (" + permittedAccountsStr + ") OR" + " lp.account_id IN ("
                            + permittedAccountsStr + "))";
                } else {
                    joinClause += " INNER JOIN account a on (t.account_id = a.id) ";
                }
            } else if (templateFilter == TemplateFilter.executable && !permittedAccounts.isEmpty()) {
                whereClause += " AND (t.public = 1 OR t.account_id IN (" + permittedAccountsStr + "))";
            } else if (templateFilter == TemplateFilter.community) {
                whereClause += " AND t.public = 1 AND t.featured = 0";
            } else if (templateFilter == TemplateFilter.all && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            } else if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                return templateZonePairList;
            }

            sql += joinClause + whereClause + getOrderByLimit(pageSize, startIndex);
            pstmt = txn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Pair<Long, Long> templateZonePair = new Pair<Long, Long>(rs.getLong(1), -1L);
                templateZonePairList.add(templateZonePair);
            }

        } catch (Exception e) {
            s_logger.warn("Error listing templates", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                txn.commit();
            } catch (SQLException sqle) {
                s_logger.warn("Error in cleaning up", sqle);
            }
        }

        return templateZonePairList;
    }


	@Override
	public Set<Pair<Long, Long>> searchTemplates(String name, String keyword, TemplateFilter templateFilter,
	        boolean isIso, List<HypervisorType> hypers, Boolean bootable, DomainVO domain, Long pageSize, Long startIndex,
	        Long zoneId, HypervisorType hyperType, boolean onlyReady, boolean showDomr,List<Account> permittedAccounts,
	        Account caller, ListProjectResourcesCriteria listProjectResourcesCriteria, Map<String, String> tags) {
        StringBuilder builder = new StringBuilder();
        if (!permittedAccounts.isEmpty()) {
            for (Account permittedAccount : permittedAccounts) {
                builder.append(permittedAccount.getAccountId() + ",");
            }
        }

        String permittedAccountsStr = builder.toString();

        if (permittedAccountsStr.length() > 0) {
            //chop the "," off
            permittedAccountsStr = permittedAccountsStr.substring(0, permittedAccountsStr.length()-1);
        }

	    Transaction txn = Transaction.currentTxn();
        txn.start();

        /* Use LinkedHashSet here to guarantee iteration order */
        Set<Pair<Long, Long>> templateZonePairList = new LinkedHashSet<Pair<Long, Long>>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        StringBuilder relatedDomainIds = new StringBuilder();
        String sql = SELECT_TEMPLATE_ZONE_REF;
        String groupByClause = "";
        try {
        	//short accountType;
        	//String accountId = null;
        	String guestOSJoin = "";
        	StringBuilder templateHostRefJoin = new StringBuilder();
        	String dataCenterJoin = "", lpjoin = "";
        	String tagsJoin = "";

        	if (isIso && !hyperType.equals(HypervisorType.None)) {
        		guestOSJoin = " INNER JOIN guest_os guestOS on (guestOS.id = t.guest_os_id) INNER JOIN guest_os_hypervisor goh on ( goh.guest_os_id = guestOS.id) ";
        	}
        	if (onlyReady){
        		templateHostRefJoin.append(" INNER JOIN  template_host_ref thr on (t.id = thr.template_id) INNER JOIN host h on (thr.host_id = h.id)");
        		sql = SELECT_TEMPLATE_HOST_REF;
                groupByClause = " GROUP BY t.id, h.data_center_id ";
        	}
        	if ((templateFilter == TemplateFilter.featured) || (templateFilter == TemplateFilter.community)) {
        	    dataCenterJoin = " INNER JOIN data_center dc on (h.data_center_id = dc.id)";
        	}

        	if (templateFilter == TemplateFilter.sharedexecutable || templateFilter == TemplateFilter.shared ){
        	    lpjoin = " INNER JOIN launch_permission lp ON t.id = lp.template_id ";
        	}

        	if (tags != null && !tags.isEmpty()) {
        	    tagsJoin = " INNER JOIN resource_tags r ON t.id = r.resource_id ";
        	}

        	sql +=  guestOSJoin + templateHostRefJoin + dataCenterJoin + lpjoin + tagsJoin;
        	String whereClause = "";

        	//All joins have to be made before we start setting the condition settings
        	if ((listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources
        			|| (!permittedAccounts.isEmpty() && !(templateFilter == TemplateFilter.community || templateFilter == TemplateFilter.featured))) &&
        			!(caller.getType() != Account.ACCOUNT_TYPE_NORMAL && templateFilter == TemplateFilter.all)) {
        		whereClause += " INNER JOIN account a on (t.account_id = a.id)";
        		if ((templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable) && (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN)) {
            		 whereClause += " INNER JOIN domain d on (a.domain_id = d.id) WHERE d.path LIKE '" + domain.getPath() + "%'";
             		if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
            			whereClause += " AND a.type != " + Account.ACCOUNT_TYPE_PROJECT;
            		}
        		} else
        			if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
        				whereClause += " WHERE a.type != " + Account.ACCOUNT_TYPE_PROJECT;
        		}
        	}

            if (!permittedAccounts.isEmpty()) {
                for (Account account : permittedAccounts) {
                    //accountType = account.getType();
                    //accountId = Long.toString(account.getId());
                    DomainVO accountDomain = _domainDao.findById(account.getDomainId());

                    // get all parent domain ID's all the way till root domain
                    DomainVO domainTreeNode = accountDomain;
                    while (true) {
                        relatedDomainIds.append(domainTreeNode.getId());
                        relatedDomainIds.append(",");
                        if (domainTreeNode.getParent() != null) {
                            domainTreeNode = _domainDao.findById(domainTreeNode.getParent());
                        } else {
                            break;
                        }
                    }

                    // get all child domain ID's
                    if (isAdmin(account.getType()) ) {
                        List<DomainVO> allChildDomains = _domainDao.findAllChildren(accountDomain.getPath(), accountDomain.getId());
                        for (DomainVO childDomain : allChildDomains) {
                            relatedDomainIds.append(childDomain.getId());
                            relatedDomainIds.append(",");
                        }
                    }
                    relatedDomainIds.setLength(relatedDomainIds.length()-1);
                }
            }

            String attr = " AND ";
            if (whereClause.endsWith(" WHERE ")) {
            	attr += " WHERE ";
            }

            if (!isIso) {
        	    if ( hypers.isEmpty() ) {
        	        return templateZonePairList;
        	    } else {
        	        StringBuilder relatedHypers = new StringBuilder();
        	        for (HypervisorType hyper : hypers ) {
        	            relatedHypers.append("'");
        	            relatedHypers.append(hyper.toString());
                        relatedHypers.append("'");
        	            relatedHypers.append(",");
        	        }
        	        relatedHypers.setLength(relatedHypers.length()-1);
                    whereClause += attr + " t.hypervisor_type IN (" + relatedHypers + ")";
        	    }
        	}

            if (!permittedAccounts.isEmpty() && !(templateFilter == TemplateFilter.featured ||
                    templateFilter == TemplateFilter.community || templateFilter == TemplateFilter.executable
                    || templateFilter == TemplateFilter.shared || templateFilter == TemplateFilter.sharedexecutable)  && !isAdmin(caller.getType()) ) {
            	whereClause += attr + "t.account_id IN (" + permittedAccountsStr + ")";
            }

        	if (templateFilter == TemplateFilter.featured) {
            	whereClause += attr + "t.public = 1 AND t.featured = 1";
            	if (!permittedAccounts.isEmpty()) {
            	    whereClause += attr + "(dc.domain_id IN (" + relatedDomainIds + ") OR dc.domain_id is NULL)";
            	}
            } else if (templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable) {
                whereClause += " AND t.account_id IN (" + permittedAccountsStr + ")";
            } else if (templateFilter == TemplateFilter.sharedexecutable || templateFilter == TemplateFilter.shared ) {
            		whereClause += " AND " +
                	" (t.account_id IN (" + permittedAccountsStr + ") OR" +
                	" lp.account_id IN (" + permittedAccountsStr + "))";
            } else if (templateFilter == TemplateFilter.executable && !permittedAccounts.isEmpty()) {
            	whereClause += attr + "(t.public = 1 OR t.account_id IN (" + permittedAccountsStr + "))";
            } else if (templateFilter == TemplateFilter.community) {
            	whereClause += attr + "t.public = 1 AND t.featured = 0";
            	if (!permittedAccounts.isEmpty()) {
            	    whereClause += attr + "(dc.domain_id IN (" + relatedDomainIds + ") OR dc.domain_id is NULL)";
            	}
            } else if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN && !isIso) {
            	return templateZonePairList;
            }

        	if (tags != null && !tags.isEmpty()) {
        	    whereClause += " AND (";
        	    boolean first = true;
        	    for (String key : tags.keySet()) {
        	        if (!first) {
        	            whereClause += " OR ";
        	        }
        	        whereClause += "(r.key=\"" + key + "\" and r.value=\"" + tags.get(key) + "\")";
        	        first = false;
        	    }
        	    whereClause += ")";
        	}

            if (whereClause.equals("")) {
            	whereClause += " WHERE ";
            } else if (!whereClause.equals(" WHERE ")) {
            	whereClause += " AND ";
            }

            sql += whereClause + getExtrasWhere(templateFilter, name, keyword, isIso, bootable, hyperType, zoneId,
                    onlyReady, showDomr) + groupByClause + getOrderByLimit(pageSize, startIndex);

            pstmt = txn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
               Pair<Long, Long> templateZonePair = new Pair<Long, Long>(rs.getLong(1), rs.getLong(2));
                               templateZonePairList.add(templateZonePair);
            }
           //for now, defaulting pageSize to a large val if null; may need to revisit post 2.2RC2
           if(isIso && templateZonePairList.size() < (pageSize != null ? pageSize : 500)
                   && templateFilter != TemplateFilter.community
                   && !(templateFilter == TemplateFilter.self && !BaseCmd.isRootAdmin(caller.getType())) ){ //evaluates to true If root admin and filter=self

               List<VMTemplateVO> publicIsos = publicIsoSearch(bootable, false, tags);
               List<VMTemplateVO> userIsos = userIsoSearch(false);

               //Listing the ISOs according to the page size.Restricting the total no. of ISOs on a page
               //to be less than or equal to the pageSize parameter

               int i=0;

               if (startIndex > userIsos.size()) {
                   i=(int) (startIndex - userIsos.size());
               }

               for (; i < publicIsos.size(); i++) {
                   if(templateZonePairList.size() >= pageSize){
                        break;
                        } else {
                        if (keyword != null && publicIsos.get(i).getName().contains(keyword)) {
                            templateZonePairList.add(new Pair<Long,Long>(publicIsos.get(i).getId(), null));
                            continue;
                        } else if (name != null && publicIsos.get(i).getName().contains(name)) {
                            templateZonePairList.add(new Pair<Long,Long>(publicIsos.get(i).getId(), null));
                            continue;
                        } else if (keyword == null && name == null){
                            templateZonePairList.add(new Pair<Long,Long>(publicIsos.get(i).getId(), null));
                        }
                      }
                    }
                }
        } catch (Exception e) {
            s_logger.warn("Error listing templates", e);
        } finally {
        	try {
        		if (rs != null) {
        			rs.close();
        		}
        		if (pstmt != null) {
        			pstmt.close();
        		}
        		txn.commit();
        	} catch( SQLException sqle) {
        		s_logger.warn("Error in cleaning up", sqle);
        	}
        }

        return templateZonePairList;
	}

	private String getExtrasWhere(TemplateFilter templateFilter, String name, String keyword, boolean isIso, Boolean bootable, HypervisorType hyperType, Long zoneId, boolean onlyReady, boolean showDomr) {
	    String sql = "";
        if (keyword != null) {
            sql += " t.name LIKE \"%" + keyword + "%\" AND";
        } else if (name != null) {
            sql += " t.name LIKE \"%" + name + "%\" AND";
        }

        if (isIso) {
            sql += " t.format = 'ISO'";
            if (!hyperType.equals(HypervisorType.None)) {
            	sql += " AND goh.hypervisor_type = '" + hyperType.toString() + "'";
            }
        } else {
            sql += " t.format <> 'ISO'";
            if (!hyperType.equals(HypervisorType.None)) {
            	sql += " AND t.hypervisor_type = '" + hyperType.toString() + "'";
            }
        }

        if (bootable != null) {
        	sql += " AND t.bootable = " + bootable;
        }

        if (onlyReady){
        	sql += " AND thr.download_state = '" +Status.DOWNLOADED.toString() + "'" + " AND thr.destroyed=0 ";
        	if (zoneId != null){
        		sql += " AND h.data_center_id = " +zoneId;
            }
        }else if (zoneId != null){
        	sql += " AND tzr.zone_id = " +zoneId+ " AND tzr.removed is null" ;
        }else{
        	sql += " AND tzr.removed is null ";
        }
        if (!showDomr){
        	sql += " AND t.type != '" +Storage.TemplateType.SYSTEM.toString() + "'";
        }

        sql += " AND t.removed IS NULL";

        return sql;
	}

	private String getOrderByLimit(Long pageSize, Long startIndex) {
    	Boolean isAscending = Boolean.parseBoolean(_configDao.getValue("sortkey.algorithm"));
    	isAscending = (isAscending == null ? true : isAscending);

		String sql;
		if (isAscending) {
			sql = " ORDER BY t.sort_key ASC";
		} else {
			sql = " ORDER BY t.sort_key DESC";
		}

        if ((pageSize != null) && (startIndex != null)) {
            sql += " LIMIT " + startIndex.toString() + "," + pageSize.toString();
        }
        return sql;
	}

	@Override
	@DB
	public long addTemplateToZone(VMTemplateVO tmplt, long zoneId) {
		Transaction txn = Transaction.currentTxn();
		txn.start();
		VMTemplateVO tmplt2 = findById(tmplt.getId());
		if (tmplt2 == null){
			if (persist(tmplt) == null) {
				throw new CloudRuntimeException("Failed to persist the template " + tmplt);
			}
			if(tmplt.getDetails() != null) {
				_templateDetailsDao.persist(tmplt.getId(), tmplt.getDetails());
			}
		}
		VMTemplateZoneVO tmpltZoneVO = _templateZoneDao.findByZoneTemplate(zoneId, tmplt.getId());
		if (tmpltZoneVO == null ) {
		    tmpltZoneVO = new VMTemplateZoneVO(zoneId, tmplt.getId(), new Date());
		    _templateZoneDao.persist(tmpltZoneVO);
		} else {
		    tmpltZoneVO.setRemoved(null);
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
		sc.setJoinParameters("tmpltzone", "zoneId", dataCenterId);
		return listBy(sc);
	}

	@Override
	public List<VMTemplateVO> listDefaultBuiltinTemplates() {
		SearchCriteria<VMTemplateVO> sc = tmpltTypeSearch.create();
		sc.setParameters("templateType", Storage.TemplateType.BUILTIN);
		return listBy(sc);
	}

	@Override
	public VMTemplateVO findSystemVMTemplate(long zoneId) {
		SearchCriteria<VMTemplateVO> sc = tmpltTypeHyperSearch.create();
		sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
		sc.setJoinParameters("tmplHyper",  "type", Host.Type.Routing);
		sc.setJoinParameters("tmplHyper", "zoneId", zoneId);

		//order by descending order of id and select the first (this is going to be the latest)
		List<VMTemplateVO> tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, 1l));

		if (tmplts.size() > 0) {
			return tmplts.get(0);
		} else {
			return null;
		}
	}

	public VMTemplateVO findSystemVMTemplate(long zoneId, HypervisorType hType) {
	    SearchCriteria<VMTemplateVO> sc = tmpltTypeHyperSearch.create();
	    sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
	    sc.setJoinParameters("tmplHyper",  "type", Host.Type.Routing);
	    sc.setJoinParameters("tmplHyper", "zoneId", zoneId);

	    //order by descending order of id
	    List<VMTemplateVO> tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, null));

	    for (VMTemplateVO tmplt: tmplts) {
	        if (tmplt.getHypervisorType() == hType) {
	            return tmplt;
	        }
	    }
	    if (tmplts.size() > 0 && hType == HypervisorType.Any) {
	        return tmplts.get(0);
	    }
	    return null;
	}

	@Override
	public VMTemplateVO findRoutingTemplate(HypervisorType hType) {
	    SearchCriteria<VMTemplateVO> sc = tmpltTypeHyperSearch2.create();
        sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
        sc.setParameters("hypervisorType", hType);

        //order by descending order of id and select the first (this is going to be the latest)
        List<VMTemplateVO> tmplts = listBy(sc, new Filter(VMTemplateVO.class, "id", false, null, 1l));

        if (tmplts.size() > 0) {
            return tmplts.get(0);
        } else {
            return null;
        }
	}

    @Override
    public Long countTemplatesForAccount(long accountId) {
    	SearchCriteria<Long> sc = CountTemplatesByAccount.create();
        sc.setParameters("account", accountId);
        return customSearch(sc, null).get(0);
    }

    @Override
    @DB
    public boolean remove(Long id) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        VMTemplateVO template = createForUpdate();
        template.setRemoved(new Date());

        VMTemplateVO vo = findById(id);
        if (vo != null) {
            if (vo.getFormat() == ImageFormat.ISO) {
                _tagsDao.removeByIdAndType(id, TaggedResourceType.ISO);
            } else {
                _tagsDao.removeByIdAndType(id, TaggedResourceType.Template);
            }
        }

        boolean result = update(id, template);
        txn.commit();
        return result;
    }

    private boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	    	    (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}

    @Override
    public List<VMTemplateVO> findTemplatesToSyncToS3() {
        return executeList(SELECT_S3_CANDIDATE_TEMPLATES, new Object[] {});
    }

    @Override
    public Set<Pair<Long, Long>> searchS3Templates(final String name,
            final String keyword, final TemplateFilter templateFilter,
            final boolean isIso, final List<HypervisorType> hypers,
            final Boolean bootable, final DomainVO domain, final Long pageSize,
            final Long startIndex, final Long zoneId,
            final HypervisorType hyperType, final boolean onlyReady,
            final boolean showDomr, final List<Account> permittedAccounts,
            final Account caller, final Map<String, String> tags) {

        final String permittedAccountsStr = join(",", permittedAccounts);

        final Transaction txn = Transaction.currentTxn();
        txn.start();

        Set<Pair<Long, Long>> templateZonePairList = new HashSet<Pair<Long, Long>>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {

            final StringBuilder joinClause = new StringBuilder();
            final StringBuilder whereClause = new StringBuilder(" WHERE t.removed IS NULL");

            if (isIso) {
                whereClause.append(" AND t.format = 'ISO'");
                if (!hyperType.equals(HypervisorType.None)) {
                    joinClause.append(" INNER JOIN guest_os guestOS on (guestOS.id = t.guest_os_id) INNER JOIN guest_os_hypervisor goh on ( goh.guest_os_id = guestOS.id) ");
                    whereClause.append(" AND goh.hypervisor_type = '");
                    whereClause.append(hyperType);
                    whereClause.append("'");
                }
            } else {
                whereClause.append(" AND t.format <> 'ISO'");
                if (hypers.isEmpty()) {
                    return templateZonePairList;
                } else {
                    final StringBuilder relatedHypers = new StringBuilder();
                    for (HypervisorType hyper : hypers) {
                        relatedHypers.append("'");
                        relatedHypers.append(hyper.toString());
                        relatedHypers.append("'");
                        relatedHypers.append(",");
                    }
                    relatedHypers.setLength(relatedHypers.length() - 1);
                    whereClause.append(" AND t.hypervisor_type IN (");
                    whereClause.append(relatedHypers);
                    whereClause.append(")");
                }
            }

            joinClause.append(" INNER JOIN  template_s3_ref tsr on (t.id = tsr.template_id)");

            whereClause.append("AND t.name LIKE \"%");
            whereClause.append(keyword == null ? keyword : name);
            whereClause.append("%\"");

            if (bootable != null) {
                whereClause.append(" AND t.bootable = ");
                whereClause.append(bootable);
            }

            if (!showDomr) {
                whereClause.append(" AND t.type != '");
                whereClause.append(Storage.TemplateType.SYSTEM);
                whereClause.append("'");
            }

            if (templateFilter == TemplateFilter.featured) {
                whereClause.append(" AND t.public = 1 AND t.featured = 1");
            } else if ((templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable)
                    && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN
                        || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
                    joinClause.append(" INNER JOIN account a on (t.account_id = a.id) INNER JOIN domain d on (a.domain_id = d.id)");
                    whereClause.append("  AND d.path LIKE '");
                    whereClause.append(domain.getPath());
                    whereClause.append("%'");
                } else {
                    whereClause.append(" AND t.account_id IN (");
                    whereClause.append(permittedAccountsStr);
                    whereClause.append(")");
                }
            } else if (templateFilter == TemplateFilter.sharedexecutable
                    && caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL) {
                    joinClause.append(" LEFT JOIN launch_permission lp ON t.id = lp.template_id WHERE (t.account_id IN (");
                    joinClause.append(permittedAccountsStr);
                    joinClause.append(") OR lp.account_id IN (");
                    joinClause.append(permittedAccountsStr);
                    joinClause.append("))");
                } else {
                    joinClause.append(" INNER JOIN account a on (t.account_id = a.id) ");
                }
            } else if (templateFilter == TemplateFilter.executable
                    && !permittedAccounts.isEmpty()) {
                whereClause.append(" AND (t.public = 1 OR t.account_id IN (");
                whereClause.append(permittedAccountsStr);
                whereClause.append("))");
            } else if (templateFilter == TemplateFilter.community) {
                whereClause.append(" AND t.public = 1 AND t.featured = 0");
            } else if (templateFilter == TemplateFilter.all
                    && caller.getType() == Account.ACCOUNT_TYPE_ADMIN) {
            } else if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
                return templateZonePairList;
            }

            final StringBuilder sql = new StringBuilder(SELECT_TEMPLATE_S3_REF);
            sql.append(joinClause);
            sql.append(whereClause);
            sql.append(getOrderByLimit(pageSize, startIndex));

            pstmt = txn.prepareStatement(sql.toString());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                final Pair<Long, Long> templateZonePair = new Pair<Long, Long>(
                        rs.getLong(1), -1L);
                templateZonePairList.add(templateZonePair);
            }
            txn.commit();
        } catch (Exception e) {
            s_logger.warn("Error listing S3 templates", e);
            if (txn != null) {
                txn.rollback();
            }
        } finally {
            closeResources(pstmt, rs);
            if (txn != null) {
                txn.close();
            }
        }

        return templateZonePairList;
    }

    @Override
    public boolean updateState(TemplateState currentState, TemplateEvent event,
            TemplateState nextState, VMTemplateVO vo, Object data) {
        Long oldUpdated = vo.getUpdatedCount();
        Date oldUpdatedTime = vo.getUpdated();


        SearchCriteria<VMTemplateVO> sc = updateStateSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);
        sc.setParameters("updatedCount", vo.getUpdatedCount());

        vo.incrUpdatedCount();

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "updated", new Date());

        int rows = update((VMTemplateVO) vo, sc);
        if (rows == 0 && s_logger.isDebugEnabled()) {
            VMTemplateVO dbVol = findByIdIncludingRemoved(vo.getId());
            if (dbVol != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbVol.getId()).append("; state=").append(dbVol.getState()).append("; updatecount=").append(dbVol.getUpdatedCount()).append(";updatedTime=")
                        .append(dbVol.getUpdated());
                str.append(": New Data={id=").append(vo.getId()).append("; state=").append(nextState).append("; event=").append(event).append("; updatecount=").append(vo.getUpdatedCount())
                        .append("; updatedTime=").append(vo.getUpdated());
                str.append(": stale Data={id=").append(vo.getId()).append("; state=").append(currentState).append("; event=").append(event).append("; updatecount=").append(oldUpdated)
                        .append("; updatedTime=").append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update objectIndatastore: id=" + vo.getId() + ", as there is no such object exists in the database anymore");
            }
        }
        return rows > 0;
    }
}
