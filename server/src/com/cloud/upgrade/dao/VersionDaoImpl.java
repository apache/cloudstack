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
package com.cloud.upgrade.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = VersionDao.class)
@DB(txn = false)
public class VersionDaoImpl extends GenericDaoBase<VersionVO, Long> implements VersionDao {
    private static final Logger s_logger = Logger.getLogger(VersionDaoImpl.class);

    final GenericSearchBuilder<VersionVO, String> CurrentVersionSearch;
    final SearchBuilder<VersionVO> AllFieldsSearch;

    protected VersionDaoImpl() {
        super();

        CurrentVersionSearch = createSearchBuilder(String.class);
        CurrentVersionSearch.selectField(CurrentVersionSearch.entity().getVersion());
        CurrentVersionSearch.and("step", CurrentVersionSearch.entity().getStep(), Op.EQ);
        CurrentVersionSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("version", AllFieldsSearch.entity().getVersion(), Op.EQ);
        AllFieldsSearch.and("step", AllFieldsSearch.entity().getStep(), Op.EQ);
        AllFieldsSearch.and("updated", AllFieldsSearch.entity().getUpdated(), Op.EQ);
        AllFieldsSearch.done();

    }

    @Override
    public VersionVO findByVersion(String version, Step step) {
        SearchCriteria<VersionVO> sc = AllFieldsSearch.create();
        sc.setParameters("version", version);
        sc.setParameters("step", step);

        return findOneBy(sc);
    }

    @Override
    public String getCurrentVersion() {
        Connection conn = null;
        try {
            s_logger.debug("Checking to see if the database is at a version before it was the version table is created");

            conn = Transaction.getStandaloneConnection();

            PreparedStatement pstmt = conn.prepareStatement("SHOW TABLES LIKE 'version'");
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                rs.close();
                pstmt.close();
                pstmt = conn.prepareStatement("SHOW TABLES LIKE 'nics'");
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    pstmt.close();

                    pstmt = conn.prepareStatement("SELECT domain_id FROM account_vlan_map LIMIT 1");
                    try {
                        pstmt.executeQuery();
                        return "2.1.8";
                    } catch (SQLException e) {
                        s_logger.debug("Assuming the exception means domain_id is not there.");
                        s_logger.debug("No version table and no nics table, returning 2.1.7");
                        return "2.1.7";
                    } finally {
                        pstmt.close();
                    }
                } else {
                    rs.close();
                    pstmt.close();
                    s_logger.debug("No version table but has nics table, returning 2.2.1 or 2.2.2");

                    // Use is_static_nat field from firewall_rules table to indicate that the version is 2.2.2
                    pstmt = conn.prepareStatement("SELECT is_static_nat FROM firewall_rules LIMIT 1");
                    try {
                        pstmt.executeQuery();
                        return "2.2.1";
                    } catch (SQLException e) {
                        s_logger.debug("Assuming the exception means is_static_nat field doesn't exist in firewall_rules table; returning 2.2.2");
                        return "2.2.2";
                    } finally {
                        pstmt.close();
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to get the current version", e);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }

        SearchCriteria<String> sc = CurrentVersionSearch.create();

        sc.setParameters("step", Step.Complete);
        Filter filter = new Filter(VersionVO.class, "id", false, 0l, 1l);

        List<String> vers = customSearch(sc, filter);
        return vers.get(0);
    }
}
