/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.domain.DomainVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={VMTemplateDao.class})
public class VMTemplateDaoImpl extends GenericDaoBase<VMTemplateVO, Long> implements VMTemplateDao {
    private static final Logger s_logger = Logger.getLogger(VMTemplateDaoImpl.class);
    private HypervisorType _defaultHyperType;
    
    @Inject
    VMTemplateZoneDao _templateZoneDao;
    @Inject
    ConfigurationDao  _configDao;

    private final String SELECT_ALL = "SELECT t.id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, " +
                                       "t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t";
    
    private final String SELECT_TEMPLATE_HOST_REF = "SELECT t.id, h.data_center_id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, " +
    								"t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t";
    
    private final String SELECT_TEMPLATE_ZONE_REF = "SELECT t.id, tzr.zone_id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, " +
									"t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones, t.hypervisor_type FROM vm_template t INNER JOIN template_zone_ref tzr on (t.id = tzr.template_id) ";
    
    protected SearchBuilder<VMTemplateVO> TemplateNameSearch;
    protected SearchBuilder<VMTemplateVO> UniqueNameSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeSearch;
    protected SearchBuilder<VMTemplateVO> tmpltTypeHyperSearch;
    protected SearchBuilder<VMTemplateVO> AccountIdSearch;
    protected SearchBuilder<VMTemplateVO> NameSearch;
    protected SearchBuilder<VMTemplateVO> TmpltsInZoneSearch;
    private SearchBuilder<VMTemplateVO> PublicSearch;
    private SearchBuilder<VMTemplateVO> NameAccountIdSearch;
    private SearchBuilder<VMTemplateVO> PublicIsoSearch;
    
    private String routerTmpltName;
    private String consoleProxyTmpltName;
    
    protected VMTemplateDaoImpl() {
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
    public VMTemplateVO findByTemplateNameAccountId(String templateName, Long accountId) {
        SearchCriteria<VMTemplateVO> sc = NameAccountIdSearch.create();
        sc.setParameters("name", templateName);
        sc.setParameters("accountId", accountId);
        return findOneBy(sc);
    }

    @Override
    public List<VMTemplateVO> publicIsoSearch(){
        SearchCriteria<VMTemplateVO> sc = PublicIsoSearch.create();
    	sc.setParameters("public", 1);
    	sc.setParameters("format", "ISO");
    	sc.setParameters("type", TemplateType.PERHOST.toString());
        return listBy(sc);
    }
    
	@Override
	public List<VMTemplateVO> listAllRoutingTemplates() {
		SearchCriteria<VMTemplateVO> sc = tmpltTypeSearch.create();
		sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
		return listBy(sc);
	}

	@Override
	public VMTemplateVO findRoutingTemplate() {
		return findSystemVMTemplate();
	}
	
	@Override
	public VMTemplateVO findConsoleProxyTemplate() {
		return findSystemVMTemplate();
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
		if (domainId != null)
			sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
		if (accountId != null)
			sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
		if (path != null)
			sc.addAnd("path", SearchCriteria.Op.EQ, path);
		return listIncludingRemovedBy(sc);
	}

	@Override
	public List<VMTemplateVO> listByAccountId(long accountId) {
        SearchCriteria<VMTemplateVO> sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
	}
	
	@Override
	public List<VMTemplateVO> listByHypervisorType(HypervisorType hyperType) {
		SearchCriteria<VMTemplateVO> sc = createSearchCriteria();
		sc.addAnd("hypervisor_type", SearchCriteria.Op.EQ, hyperType.toString());
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
		if(consoleProxyTmpltName == null)
			consoleProxyTmpltName = "routing";
		if(s_logger.isDebugEnabled())
			s_logger.debug("Use console proxy template : " + consoleProxyTmpltName);

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
		
		tmpltTypeHyperSearch = createSearchBuilder();
		tmpltTypeHyperSearch.and("templateType", tmpltTypeHyperSearch.entity().getTemplateType(), SearchCriteria.Op.EQ);
		tmpltTypeHyperSearch.and("hypervisor_type", tmpltTypeHyperSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
		
		tmpltTypeSearch = createSearchBuilder();
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
			
		_defaultHyperType = HypervisorType.getType(_configDao.getValue("hypervisor.type"));
		return result;
	}

	@Override
	public String getRoutingTemplateUniqueName() {
		return routerTmpltName;
	}

	@Override
	public Set<Pair<Long, Long>> searchTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Account account, DomainVO domain, Long pageSize, Long startIndex, Long zoneId, HypervisorType hyperType, boolean onlyReady,boolean showDomr) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        Set<Pair<Long, Long>> templateZonePairList = new HashSet<Pair<Long, Long>>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = SELECT_TEMPLATE_ZONE_REF;
        try {        	
        	short accountType;
        	String accountId = null;
        	if (account != null) {
        		accountType = account.getType();
        		accountId = Long.toString(account.getId());
        	} else {
        		accountType = Account.ACCOUNT_TYPE_ADMIN;
        	}
        	
        	String guestOSJoin = "";  
        	StringBuilder templateHostRefJoin = new StringBuilder();

        	if (isIso && !hyperType.equals(HypervisorType.None)) { 
        		guestOSJoin = " INNER JOIN guest_os guestOS on (guestOS.id = t.guest_os_id) INNER JOIN guest_os_hypervisor goh on ( goh.guest_os_id = guestOS.id) ";
        	}
        	if (onlyReady){
        		templateHostRefJoin.append(" INNER JOIN  template_host_ref thr on (t.id = thr.template_id) INNER JOIN host h on (thr.host_id = h.id)");
        		sql = SELECT_TEMPLATE_HOST_REF;
        	}
        	
        	sql +=  guestOSJoin + templateHostRefJoin;
        	String whereClause = "";        	
        	
            if (templateFilter == TemplateFilter.featured) {
            	whereClause += " WHERE t.public = 1 AND t.featured = 1";
            } else if ((templateFilter == TemplateFilter.self || templateFilter == TemplateFilter.selfexecutable) && accountType != Account.ACCOUNT_TYPE_ADMIN) {
            	if (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
            		whereClause += " INNER JOIN account a on (t.account_id = a.id) INNER JOIN domain d on (a.domain_id = d.id) WHERE d.path LIKE '" + domain.getPath() + "%'";
            	} else {
            		whereClause += " WHERE t.account_id = " + accountId;
            	}
            } else if (templateFilter == TemplateFilter.sharedexecutable && accountType != Account.ACCOUNT_TYPE_ADMIN) {
            	if (accountType == Account.ACCOUNT_TYPE_NORMAL) {
            		whereClause += " LEFT JOIN launch_permission lp ON t.id = lp.template_id WHERE" +
                	" (t.account_id = " + accountId + " OR" +
                	" lp.account_id = " + accountId + ")";
            	} else {
            		whereClause += " INNER JOIN account a on (t.account_id = a.id) INNER JOIN domain d on (a.domain_id = d.id) WHERE d.path LIKE '" + domain.getPath() + "%'";
            	}            	
            } else if (templateFilter == TemplateFilter.executable && accountId != null) {
            	whereClause += " WHERE (t.public = 1 OR t.account_id = " + accountId + ")";
            } else if (templateFilter == TemplateFilter.community) {
            	whereClause += " WHERE t.public = 1 AND t.featured = 0";
            } else if (templateFilter == TemplateFilter.all && accountType == Account.ACCOUNT_TYPE_ADMIN) {
            	whereClause += " WHERE ";
            } else if (accountType != Account.ACCOUNT_TYPE_ADMIN) {
            	return templateZonePairList;
            }
            
            if (whereClause.equals("")) {
            	whereClause += " WHERE ";
            } else if (!whereClause.equals(" WHERE ")) {
            	whereClause += " AND ";
            }
            
            sql += whereClause + getExtrasWhere(templateFilter, name, keyword, isIso, bootable, hyperType, zoneId, onlyReady, showDomr, accountType) + getOrderByLimit(pageSize, startIndex);

            pstmt = txn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
            	Pair<Long, Long> templateZonePair = new Pair<Long, Long>(rs.getLong(1), rs.getLong(2));            	
				templateZonePairList.add(templateZonePair);    		
            }
            
           //for now, defaulting pageSize to a large val if null; may need to revisit post 2.2RC2 
           if(isIso && templateZonePairList.size() < (pageSize != null ? pageSize : 500) && templateFilter != TemplateFilter.community){
            	List<VMTemplateVO> publicIsos = publicIsoSearch();            	
            	for( int i=0; i < publicIsos.size(); i++){
            		templateZonePairList.add(new Pair<Long,Long>(publicIsos.get(i).getId(), null));
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

	private String getExtrasWhere(TemplateFilter templateFilter, String name, String keyword, boolean isIso, Boolean bootable, HypervisorType hyperType, Long zoneId, boolean onlyReady, boolean showDomr, short accountType) {
	    String sql = "";
        if (keyword != null) {
            sql += " t.name LIKE \"%" + keyword + "%\" AND";
        } else if (name != null) {
            sql += " t.name LIKE \"%" + name + "%\" AND";
        }

        if (isIso) {
            sql += " t.format = 'ISO'";
//            if (accountType == Account.ACCOUNT_TYPE_NORMAL){
//            	sql += " AND t.public = 1 ";
//            }
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
        	sql += " AND thr.download_state = '" +Status.DOWNLOADED.toString() + "'";
        	if (zoneId != null){
        		sql += " AND h.data_center_id = " +zoneId;
            }
        }else if (zoneId != null){
        	sql += " AND tzr.zone_id = " +zoneId;
        }
        if (!showDomr){
        	sql += " AND t.type != '" +Storage.TemplateType.SYSTEM.toString() + "'";
        }        

        sql += " AND t.removed IS NULL";

        return sql;
	}

	private String getOrderByLimit(Long pageSize, Long startIndex) {
        String sql = " ORDER BY t.created DESC";
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
			persist(tmplt);
		}
		VMTemplateZoneVO tmpltZoneVO = new VMTemplateZoneVO(zoneId, tmplt.getId(), new Date());
		_templateZoneDao.persist(tmpltZoneVO);
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
	
	private VMTemplateVO findSystemVMTemplate() {
		SearchCriteria<VMTemplateVO> sc = tmpltTypeHyperSearch.create();
		sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
		sc.setParameters("hypervisor_type", _defaultHyperType.toString());
		VMTemplateVO tmplt = findOneBy(sc);
		
		if (tmplt == null) {
			/*Can't find it? We'd like to prefer xenserver */
			if (_defaultHyperType != HypervisorType.XenServer) {
				sc = tmpltTypeHyperSearch.create();
				sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
				sc.setParameters("hypervisor_type", HypervisorType.XenServer.toString());
				tmplt = findOneBy(sc);
				
				/*Still can't find it? return a random one*/
				if (tmplt == null) {
					sc = tmpltTypeSearch.create();
					sc.setParameters("templateType", Storage.TemplateType.SYSTEM);
					tmplt = findOneBy(sc);
				}
			}
		}
		
		return tmplt;
	}
}
