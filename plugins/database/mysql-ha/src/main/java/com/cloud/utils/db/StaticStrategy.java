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

import java.lang.reflect.InvocationHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.mysql.cj.jdbc.ConnectionImpl;
import com.mysql.cj.jdbc.JdbcConnection;
import com.mysql.cj.jdbc.exceptions.SQLError;
import com.mysql.cj.jdbc.ha.BalanceStrategy;
import com.mysql.cj.jdbc.ha.LoadBalancedConnectionProxy;

public class StaticStrategy implements BalanceStrategy {
    private static final Logger s_logger = Logger.getLogger(StaticStrategy.class);

    public StaticStrategy() {
    }

    @Override
    public JdbcConnection pickConnection(InvocationHandler proxy, List<String> configuredHosts, Map<String, JdbcConnection> liveConnections,
                                         long[] responseTimes, int numRetries) throws SQLException {
        int numHosts = configuredHosts.size();

        SQLException ex = null;

        List<String> allowList = new ArrayList<String>(numHosts);
        allowList.addAll(configuredHosts);

        Map<String, Long> denylist = ((LoadBalancedConnectionProxy) proxy).getGlobalBlacklist();

        allowList.removeAll(denylist.keySet());

        Map<String, Integer> allowListMap = this.getArrayIndexMap(allowList);

        for (int attempts = 0; attempts < numRetries;) {
            if (allowList.size() == 0) {
                throw SQLError.createSQLException("No hosts configured", null);
            }

            String hostPortSpec = allowList.get(0);     //Always take the first host

            ConnectionImpl conn = (ConnectionImpl) liveConnections.get(hostPortSpec);

            if (conn == null) {
                try {
                    conn = ((LoadBalancedConnectionProxy) proxy).createConnectionForHost(hostPortSpec);
                } catch (SQLException sqlEx) {
                    ex = sqlEx;

                    if (((LoadBalancedConnectionProxy) proxy).shouldExceptionTriggerFailover(sqlEx)) {

                        Integer allowListIndex = allowListMap.get(hostPortSpec);

                        // exclude this host from being picked again
                        if (allowListIndex != null) {
                            allowList.remove(allowListIndex.intValue());
                            allowListMap = this.getArrayIndexMap(allowList);
                        }
                        ((LoadBalancedConnectionProxy) proxy).addToGlobalBlacklist(hostPortSpec);

                        if (allowList.size() == 0) {
                            attempts++;
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                s_logger.debug("[ignored] interrupted while fail over in progres.");
                            }

                            // start fresh
                            allowListMap = new HashMap<String, Integer>(numHosts);
                            allowList.addAll(configuredHosts);
                            denylist = ((LoadBalancedConnectionProxy) proxy).getGlobalBlacklist();

                            allowList.removeAll(denylist.keySet());
                            allowListMap = this.getArrayIndexMap(allowList);
                        }

                        continue;
                    }

                    throw sqlEx;
                }
            }

            return conn;
        }

        if (ex != null) {
            throw ex;
        }

        return null; // we won't get here, compiler can't tell
    }

    private Map<String, Integer> getArrayIndexMap(List<String> l) {
        Map<String, Integer> m = new HashMap<String, Integer>(l.size());
        for (int i = 0; i < l.size(); i++) {
            m.put(l.get(i), i);
        }
        return m;

    }
}
