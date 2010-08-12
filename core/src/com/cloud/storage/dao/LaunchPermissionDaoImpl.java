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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.storage.Storage;

@Local(value={LaunchPermissionDao.class})
public class LaunchPermissionDaoImpl extends GenericDaoBase<LaunchPermissionVO, Long> implements LaunchPermissionDao {
    private static final Logger s_logger = Logger.getLogger(LaunchPermissionDaoImpl.class);
    private static final String REMOVE_LAUNCH_PERMISSION = "DELETE FROM `cloud`.`launch_permission`" +
                                                           "  WHERE template_id = ? AND account_id = ?";

    private static final String LIST_PERMITTED_TEMPLATES = "SELECT t.id, t.unique_name, t.name, t.public, t.format, t.type, t.hvm, t.bits, t.url, t.created, t.account_id, t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.featured" +
                                                           "  FROM `cloud`.`vm_template` t INNER JOIN (SELECT lp.template_id as lptid" +
                                                                                                      " FROM `cloud`.`launch_permission` lp" +
                                                                                                      " WHERE lp.account_id = ?) joinlp" +
                                                           "  WHERE t.id = joinlp.lptid" +
                                                           "  ORDER BY t.created DESC";

    private final SearchBuilder<LaunchPermissionVO> TemplateAndAccountSearch;
    private final SearchBuilder<LaunchPermissionVO> TemplateIdSearch;

    protected LaunchPermissionDaoImpl() {
        TemplateAndAccountSearch = createSearchBuilder();
        TemplateAndAccountSearch.and("templateId", TemplateAndAccountSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateAndAccountSearch.and("accountId", TemplateAndAccountSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        TemplateAndAccountSearch.done();

        TemplateIdSearch = createSearchBuilder();
        TemplateIdSearch.and("templateId", TemplateIdSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateIdSearch.done();
    }

    @Override
    public void removePermissions(long templateId, List<Long> accountIds) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        try {
            txn.start();
            String sql = REMOVE_LAUNCH_PERMISSION;
            pstmt = txn.prepareAutoCloseStatement(sql);
            for (Long accountId : accountIds) {
                pstmt.setLong(1, templateId);
                pstmt.setLong(2, accountId.longValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            s_logger.warn("Error removing launch permissions", e);
            throw new CloudRuntimeException("Error removing launch permissions", e);
        }
    }

    @Override
    public void removeAllPermissions(long templateId) {
        SearchCriteria sc = TemplateIdSearch.create();
        sc.setParameters("templateId", templateId);
        delete(sc);
    }

    @Override
    public LaunchPermissionVO findByTemplateAndAccount(long templateId, long accountId) {
        SearchCriteria sc = TemplateAndAccountSearch.create();
        sc.setParameters("templateId", templateId);
        sc.setParameters("accountId", accountId);
        return findOneActiveBy(sc);
    }

    @Override
    public List<VMTemplateVO> listPermittedTemplates(long accountId) {
        Transaction txn = Transaction.currentTxn();
        List<VMTemplateVO> permittedTemplates = new ArrayList<VMTemplateVO>();
        PreparedStatement pstmt = null;
        try {
            String sql = LIST_PERMITTED_TEMPLATES;
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String uniqueName = rs.getString(2);
                String name = rs.getString(3);
                boolean isPublic = rs.getBoolean(4);
                String value = rs.getString(5);
                ImageFormat format = ImageFormat.valueOf(value);
                String filesystem = rs.getString(6);
                boolean requiresHVM = rs.getBoolean(7);
                int bits = rs.getInt(8);
                String url = rs.getString(9);
                String createdTS = rs.getString(10);
                long templateAccountId = rs.getLong(11);
                String checksum = rs.getString(12);
                String displayText = rs.getString(13);
                boolean enablePassword = rs.getBoolean(14);
                long guestOSId = rs.getLong(15);
                boolean featured = rs.getBoolean(16);
                Date createdDate = null;

                if (createdTS != null) {
                    createdDate = DateUtil.parseDateString(s_gmtTimeZone, createdTS);
                }

                if (isPublic) {
                    continue; // if it's public already, skip adding it to permitted templates as this for private templates only
                }
                VMTemplateVO template = new VMTemplateVO(id, uniqueName, name, format, isPublic, featured, FileSystem.valueOf(filesystem), url, createdDate, requiresHVM, bits, templateAccountId, checksum, displayText, enablePassword, guestOSId, true);
                permittedTemplates.add(template);
            }
        } catch (Exception e) {
            s_logger.warn("Error listing permitted templates", e);
        }
        return permittedTemplates;
    }

    @Override
    public List<LaunchPermissionVO> findByTemplate(long templateId) {
        SearchCriteria sc = TemplateIdSearch.create();
        sc.setParameters("templateId", templateId);
        return listActiveBy(sc);
    }
}
