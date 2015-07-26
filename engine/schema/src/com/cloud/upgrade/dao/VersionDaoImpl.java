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
package com.cloud.upgrade.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value = VersionDao.class)
@DB()
public class VersionDaoImpl extends GenericDaoBase<VersionVO, Long> implements VersionDao {
    private static final Logger s_logger = Logger.getLogger(VersionDaoImpl.class);

    final GenericSearchBuilder<VersionVO, String> CurrentVersionSearch;
    final SearchBuilder<VersionVO> AllFieldsSearch;

    public VersionDaoImpl() {
        super();

        CurrentVersionSearch = createSearchBuilder(String.class);
        CurrentVersionSearch.selectFields(CurrentVersionSearch.entity().getVersion());
        CurrentVersionSearch.and("step", CurrentVersionSearch.entity().getStep(), Op.EQ);
        CurrentVersionSearch.done();

        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("version", AllFieldsSearch.entity().getVersion(), Op.EQ);
        AllFieldsSearch.and("step", AllFieldsSearch.entity().getStep(), Op.EQ);
        AllFieldsSearch.and("updated", AllFieldsSearch.entity().getUpdated(), Op.EQ);
        AllFieldsSearch.done();

    }

    @Override
    public VersionVO findByVersion(final String version, final Step step) {
        final SearchCriteria<VersionVO> sc = AllFieldsSearch.create();
        sc.setParameters("version", version);
        sc.setParameters("step", step);

        return findOneBy(sc);
    }

    @Override
    @DB
    public String getCurrentVersion() {
        try (Connection conn = TransactionLegacy.getStandaloneConnection();) {
            s_logger.debug("Checking to see if the database is at a version before it was the version table is created");

            try (
                    PreparedStatement pstmt = conn.prepareStatement("SHOW TABLES LIKE 'version'");
                    ResultSet rs = pstmt.executeQuery();
                ) {
                if (!rs.next()) {
                    try (PreparedStatement pstmt_nics = conn.prepareStatement("SHOW TABLES LIKE 'nics'");
                         ResultSet rs_nics = pstmt_nics.executeQuery();
                        ) {
                        if (!rs_nics.next()) {
                            try (PreparedStatement pstmt_domain = conn.prepareStatement("SELECT domain_id FROM account_vlan_map LIMIT 1"); ){
                                pstmt_domain.executeQuery();
                                return "2.1.8";
                            } catch (final SQLException e) {
                                s_logger.debug("Assuming the exception means domain_id is not there.");
                                s_logger.debug("No version table and no nics table, returning 2.1.7");
                                return "2.1.7";
                            }
                        } else {
                            try (PreparedStatement pstmt_static_nat = conn.prepareStatement("SELECT is_static_nat from firewall_rules");
                                 ResultSet rs_static_nat = pstmt_static_nat.executeQuery();){
                                return "2.2.1";
                            } catch (final SQLException e) {
                                s_logger.debug("Assuming the exception means static_nat field doesn't exist in firewall_rules table, returning version 2.2.2");
                                return "2.2.2";
                            }
                        }
                    }
                }
            } catch (final SQLException e) {
                throw new CloudRuntimeException("Unable to get the current version", e);
            }

            SearchCriteria<String> sc = CurrentVersionSearch.create();

            sc.setParameters("step", Step.Complete);
            Filter filter = new Filter(VersionVO.class, "id", false, 0l, 1l);
            final List<String> upgradedVersions = customSearch(sc, filter);

            if (upgradedVersions.isEmpty()) {

                // Check if there are records in Version table
                filter = new Filter(VersionVO.class, "id", false, 0l, 1l);
                sc = CurrentVersionSearch.create();
                final List<String> vers = customSearch(sc, filter);
                if (!vers.isEmpty()) {
                    throw new CloudRuntimeException("Version table contains records for which upgrade wasn't completed");
                }

                // Use nics table information and is_static_nat field from firewall_rules table to determine version information
                s_logger.debug("Version table exists, but it's empty; have to confirm that version is 2.2.2");
                try (PreparedStatement pstmt = conn.prepareStatement("SHOW TABLES LIKE 'nics'");
                     ResultSet rs = pstmt.executeQuery();){
                    if (!rs.next()) {
                        throw new CloudRuntimeException("Unable to determine the current version, version table exists and empty, nics table doesn't exist");
                    } else {
                        try (PreparedStatement pstmt_static_nat = conn.prepareStatement("SELECT is_static_nat from firewall_rules"); ) {
                            pstmt_static_nat.executeQuery();
                            throw new CloudRuntimeException("Unable to determine the current version, version table exists and empty, " +
                                    "nics table doesn't exist, is_static_nat field exists in firewall_rules table");
                        } catch (final SQLException e) {
                            s_logger.debug("Assuming the exception means static_nat field doesn't exist in firewall_rules table, returning version 2.2.2");
                            return "2.2.2";
                        }
                    }
                } catch (final SQLException e) {
                    throw new CloudRuntimeException("Unable to determine the current version, version table exists and empty, query for nics table yields SQL exception", e);
                }
            } else {
                return upgradedVersions.get(0);
            }

        } catch (final SQLException e) {
            throw new CloudRuntimeException("Unable to get the current version", e);
        }

    }
}
