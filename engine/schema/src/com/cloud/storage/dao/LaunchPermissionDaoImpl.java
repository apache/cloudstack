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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = {LaunchPermissionDao.class})
public class LaunchPermissionDaoImpl extends GenericDaoBase<LaunchPermissionVO, Long> implements LaunchPermissionDao {
    private static final String REMOVE_LAUNCH_PERMISSION = "DELETE FROM `cloud`.`launch_permission`" + "  WHERE template_id = ? AND account_id = ?";

    private static final String LIST_PERMITTED_TEMPLATES =
        "SELECT t.id, t.unique_name, t.name, t.public, t.format, t.type, t.hvm, t.bits, t.url, t.created, t.account_id, t.checksum, t.display_text, t.enable_password, t.guest_os_id, t.featured"
            + "  FROM `cloud`.`vm_template` t INNER JOIN (SELECT lp.template_id as lptid"
            + " FROM `cloud`.`launch_permission` lp"
            + " WHERE lp.account_id = ?) joinlp"
            + "  WHERE t.id = joinlp.lptid" + "  ORDER BY t.created DESC";

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
        TransactionLegacy txn = TransactionLegacy.currentTxn();
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
            logger.warn("Error removing launch permissions", e);
            throw new CloudRuntimeException("Error removing launch permissions", e);
        }
    }

    @Override
    public void removeAllPermissions(long templateId) {
        SearchCriteria<LaunchPermissionVO> sc = TemplateIdSearch.create();
        sc.setParameters("templateId", templateId);
        expunge(sc);
    }

    @Override
    public LaunchPermissionVO findByTemplateAndAccount(long templateId, long accountId) {
        SearchCriteria<LaunchPermissionVO> sc = TemplateAndAccountSearch.create();
        sc.setParameters("templateId", templateId);
        sc.setParameters("accountId", accountId);
        return findOneBy(sc);
    }

    @Override
    public List<VMTemplateVO> listPermittedTemplates(long accountId) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
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
                String tmpltType = rs.getString(6);
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
                    continue; // if it's public already, skip adding it to
                              // permitted templates as this for private
                              // templates only
                }
                VMTemplateVO template =
                    new VMTemplateVO(id, uniqueName, name, format, isPublic, featured, TemplateType.valueOf(tmpltType), url, createdDate, requiresHVM, bits,
                        templateAccountId, checksum, displayText, enablePassword, guestOSId, true, null);
                permittedTemplates.add(template);
            }
        } catch (Exception e) {
            logger.warn("Error listing permitted templates", e);
        }
        return permittedTemplates;
    }

    @Override
    public List<LaunchPermissionVO> findByTemplate(long templateId) {
        SearchCriteria<LaunchPermissionVO> sc = TemplateIdSearch.create();
        sc.setParameters("templateId", templateId);
        return listBy(sc);
    }
}
