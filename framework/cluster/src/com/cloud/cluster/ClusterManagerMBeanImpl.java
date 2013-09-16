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
package com.cloud.cluster;

import java.util.Date;
import java.util.TimeZone;

import javax.management.StandardMBean;

import com.cloud.utils.DateUtil;

public class ClusterManagerMBeanImpl extends StandardMBean implements ClusterManagerMBean {
    private final ClusterManagerImpl _clusterMgr;
    private final ManagementServerHostVO _mshostVo;

    public ClusterManagerMBeanImpl(ClusterManagerImpl clusterMgr, ManagementServerHostVO mshostVo) {
        super(ClusterManagerMBean.class, false);

        _clusterMgr = clusterMgr;
        _mshostVo = mshostVo;
    }

    @Override
    public long getMsid() {
        return _mshostVo.getMsid();
    }

    @Override
    public String getLastUpdateTime() {
        Date date = _mshostVo.getLastUpdateTime();
        return DateUtil.getDateDisplayString(TimeZone.getDefault(), date);
    }

    @Override
    public String getClusterNodeIP() {
        return _mshostVo.getServiceIP();
    }

    @Override
    public String getVersion() {
        return _mshostVo.getVersion();
    }

    @Override
    public int getHeartbeatInterval() {
        return _clusterMgr.getHeartbeatInterval();
    }

    @Override
    public int getHeartbeatThreshold() {
        return ClusterManager.HeartbeatThreshold.value();
    }
}
