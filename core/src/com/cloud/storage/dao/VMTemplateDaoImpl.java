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
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.domain.DomainVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.user.Account;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value={VMTemplateDao.class})
public class VMTemplateDaoImpl extends GenericDaoBase<VMTemplateVO, Long> implements VMTemplateDao {
    private static final Logger s_logger = Logger.getLogger(VMTemplateDaoImpl.class);
    
    @Inject
    VMTemplateZoneDao _templateZoneDao;

    private final String SELECT_ALL = "SELECT t.id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.account_id, " +
                                       "t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones FROM vm_template t";

	protected static final String SELECT_ALL_IN_ZONE =
		"SELECT t.id, t.unique_name, t.name, t.public, t.featured, t.type, t.hvm, t.bits, t.url, t.format, t.created, t.removed, t.account_id, " +
		"t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.bootable, t.prepopulate, t.cross_zones FROM vm_template t, template_zone_ref tz where t.removed is null and tz.removed is null and t.id = tz.template_id and tz.zone_id=?  ";
    
    protected SearchBuilder<VMTemplateVO> TemplateNameSearch;
    protected SearchBuilder<VMTemplateVO> UniqueNameSearch;
    protected SearchBuilder<VMTemplateVO> AccountIdSearch;
    protected SearchBuilder<VMTemplateVO> NameSearch;

    protected SearchBuilder<VMTemplateVO> PublicSearch;
    private String routerTmpltName;
    private String consoleProxyTmpltName;
    
    protected VMTemplateDaoImpl() {
    }
    
    public List<VMTemplateVO> listByPublic() {
    	SearchCriteria sc = PublicSearch.create();
    	sc.setParameters("public", 1);
	    return listActiveBy(sc);
	}
    
	@Override
	public VMTemplateVO findByName(String templateName) {
		SearchCriteria sc = UniqueNameSearch.create();
		sc.setParameters("uniqueName", templateName);
		return findOneBy(sc);
	}

	@Override
	public VMTemplateVO findByTemplateName(String templateName) {
		SearchCriteria sc = NameSearch.create();
		sc.setParameters("name", templateName);
		return findOneBy(sc);
	}
	
	@Override
	public VMTemplateVO findRoutingTemplate() {
		SearchCriteria sc = UniqueNameSearch.create();
		sc.setParameters("uniqueName", routerTmpltName);
		return findOneBy(sc);
	}
	
	@Override
	public VMTemplateVO findConsoleProxyTemplate() {
		SearchCriteria sc = UniqueNameSearch.create();
		sc.setParameters("uniqueName", consoleProxyTmpltName);
		return findOneBy(sc);
	}
	
	@Override
	public List<VMTemplateVO> listReadyTemplates() {
		SearchCriteria sc = createSearchCriteria();
		sc.addAnd("ready", SearchCriteria.Op.EQ, true);
		sc.addAnd("format", SearchCriteria.Op.NEQ, Storage.ImageFormat.ISO);
		return listBy(sc);
	}
	
	@Override
	public List<VMTemplateVO> findIsosByIdAndPath(Long domainId, Long accountId, String path) {
		SearchCriteria sc = createSearchCriteria();
		sc.addAnd("iso", SearchCriteria.Op.EQ, true);
		if (domainId != null)
			sc.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
		if (accountId != null)
			sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
		if (path != null)
			sc.addAnd("path", SearchCriteria.Op.EQ, path);
		return listBy(sc);
	}

	@Override
	public List<VMTemplateVO> listByAccountId(long accountId) {
        SearchCriteria sc = AccountIdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("publicTemplate", false);
        return listActiveBy(sc);
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
		
		TemplateNameSearch = createSearchBuilder();
		TemplateNameSearch.and("name", TemplateNameSearch.entity().getName(), SearchCriteria.Op.EQ);
		UniqueNameSearch = createSearchBuilder();
		UniqueNameSearch.and("uniqueName", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
		NameSearch = createSearchBuilder();
		NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);

		AccountIdSearch = createSearchBuilder();
		AccountIdSearch.and("accountId", AccountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountIdSearch.and("publicTemplate", AccountIdSearch.entity().isPublicTemplate(), SearchCriteria.Op.EQ);
		AccountIdSearch.done();

		return result;
	}

	@Override
	public String getRoutingTemplateUniqueName() {
		return routerTmpltName;
	}

	@Override
	public List<VMTemplateVO> searchTemplates(String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Account account, DomainVO domain, Integer pageSize, Long startIndex, Long zoneId) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        List<VMTemplateVO> templates = new ArrayList<VMTemplateVO>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {        	
        	short accountType;
        	String accountId = null;
        	if (account != null) {
        		accountType = account.getType();
        		accountId = account.getId().toString();
        	} else {
        		accountType = Account.ACCOUNT_TYPE_ADMIN;
        	}
        	
        	String sql = SELECT_ALL;
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
            	return templates;
            }
            
            if (whereClause.equals("")) {
            	whereClause += " WHERE ";
            } else if (!whereClause.equals(" WHERE ")) {
            	whereClause += " AND ";
            }
            
            sql += whereClause + getExtrasWhere(templateFilter, name, keyword, isIso, bootable) + getOrderByLimit(pageSize, startIndex);

            pstmt = txn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
        		VMTemplateVO tmplt = toEntityBean(rs, false);
        		if (zoneId != null) {
        		  VMTemplateZoneVO vtzvo = _templateZoneDao.findByZoneTemplate(zoneId, tmplt.getId());
        		  if (vtzvo != null){
        			  templates.add(tmplt);
        		  }
        		} else {
        			templates.add(tmplt);
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
        
        return templates;
	}

	private String getExtrasWhere(TemplateFilter templateFilter, String name, String keyword, boolean isIso, Boolean bootable) {
	    String sql = "";
        if (keyword != null) {
            sql += " t.name LIKE \"%" + keyword + "%\" AND";
        } else if (name != null) {
            sql += " t.name LIKE \"%" + name + "%\" AND";
        }

        if (isIso) {
            sql += " t.format = 'ISO'";
        } else {
            sql += " t.format <> 'ISO'";
        }
        
        if (bootable != null) {
        	sql += " AND t.bootable = " + bootable;
        }

        sql += " AND t.removed IS NULL";

        return sql;
	}

	private String getOrderByLimit(Integer pageSize, Long startIndex) {
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
		Transaction txn = Transaction.currentTxn();
		txn.start();
		PreparedStatement pstmt = null;
		List<VMTemplateVO> result = new ArrayList<VMTemplateVO>();
		try {
			String sql = SELECT_ALL_IN_ZONE;
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setLong(1, dataCenterId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
                result.add(toEntityBean(rs, false));
            }
			txn.commit();
		} catch (SQLException sqle) {
			s_logger.warn("Exception: ",sqle);
			throw new CloudRuntimeException("Unable to list templates in zone", sqle);
		}

		return result;
	}

	@Override
	public VMTemplateVO findDefaultBuiltinTemplate() {
		return findById(TemplateConstants.DEFAULT_BUILTIN_VM_DB_ID);
	}
}
