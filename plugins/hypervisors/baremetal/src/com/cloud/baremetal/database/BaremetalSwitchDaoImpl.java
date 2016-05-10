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
//
package com.cloud.baremetal.database;

import com.cloud.baremetal.networkservice.BaremetalSwitchResponse;
import com.cloud.utils.db.GenericDaoBase;

/**
 * @author fridvin
 */
public class BaremetalSwitchDaoImpl extends GenericDaoBase<BaremetalSwitchVO, Long> implements BaremetalSwitchDao {
    @Override
    public BaremetalSwitchResponse newBaremetalSwitchResponse(BaremetalSwitchVO vo) {
        BaremetalSwitchResponse rsp = new BaremetalSwitchResponse();
        rsp.setId(vo.getUuid());
        rsp.setIp(vo.getIp());
        rsp.setUsername(vo.getUsername());
        rsp.setType(vo.getType());
        rsp.setObjectName("baremetalswitch");
        return rsp;
    }
}
