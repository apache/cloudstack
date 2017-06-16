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
package com.cloud.network.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.network.NetScalerControlCenterVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;

@Component
@Local(value = NetScalerControlCenterDao.class)
@DB
public class NetScalerControlCenterDaoImpl extends GenericDaoBase<NetScalerControlCenterVO, Long> implements NetScalerControlCenterDao {

    @Override
    public NetScalerControlCenterVO findByPodId(long podId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetScalerControlCenterVO> listByNetScalerDeviceId(long netscalerDeviceId) {
        // TODO Auto-generated method stub
        return null;
    }

}
