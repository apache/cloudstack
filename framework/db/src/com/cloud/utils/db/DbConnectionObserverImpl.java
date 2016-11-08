// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.cluster.ClusterShutdownManager;
import com.google.common.collect.ImmutableMap;

public class DbConnectionObserverImpl implements DbConnectionObserver {
    private static final Logger s_logger = Logger.getLogger(DbConnectionObserverImpl.class);

    private final int dbconnectionErrorTolerance;
    private final boolean dbconnectionFence;
    private final ImmutableMap<Integer, String> fatalErrors = ImmutableMap.<Integer, String> builder()
            .put(1129, "Management Server is blocked because of many connection errors; unblock with mysqladmin flush-hosts").build();
    private static AtomicInteger dbconnectionErrorCount = new AtomicInteger();
    private static final int DEFAULT_CONNECTION_ERROR_TOLERANCE = 100;
    private static ClusterShutdownManager clusterShutdownManager;

    public static void setDbconnectionErrorCount(AtomicInteger dbconnectionErrorCount) {
        DbConnectionObserverImpl.dbconnectionErrorCount = dbconnectionErrorCount;
    }

    public DbConnectionObserverImpl() {
        Properties dbProps = DbProperties.getDbProperties();
        dbconnectionErrorTolerance = NumberUtils.toInt(dbProps.getProperty("db.connection.error.tolerance"), DEFAULT_CONNECTION_ERROR_TOLERANCE);
        if (dbconnectionErrorTolerance < 0) {
            throw new IllegalStateException("Negative value for db.connection.error.tolerance in db.properties, value=" + dbconnectionErrorTolerance);
        }
        dbconnectionFence = Boolean.parseBoolean(dbProps.getProperty("db.connection.errors.fence"));
        s_logger.info(this);
    }

    @Override
    public void onError(SQLException se) {
        if (dbconnectionFence) {
            String desc = isFatal(se);
            if (desc != null) {
                s_logger.error("Fencing management server due to fatal exception " + desc, se);
                initiateClusterFencing();
            } else if (dbconnectionErrorCount.intValue() > dbconnectionErrorTolerance) {
                s_logger.error("Fencing management server due to database connection errors " + se.getMessage(), se);
                initiateClusterFencing();
            }
            dbconnectionErrorCount.incrementAndGet();
        }
    }

    private String isFatal(SQLException se) {
        Integer error_code = se.getErrorCode();
        return fatalErrors.get(error_code);
    }

    @Override
    public void onSuccess() {
        dbconnectionErrorCount.set(0);
    }

    @Override
    public String toString() {
        return "DbConnectionObserverImpl [dbconnectionErrorTolerance=" + dbconnectionErrorTolerance + ", dbconnectionFence=" + dbconnectionFence + ", fatalErrors=" + fatalErrors
                + "]";
    }

    @Override
    public void registerClusterShutdownManager(ClusterShutdownManager csm) {
        clusterShutdownManager = csm;
    }

    private void initiateClusterFencing() {
        if (clusterShutdownManager != null)clusterShutdownManager.notifyNodeIsolated();
    }

}
