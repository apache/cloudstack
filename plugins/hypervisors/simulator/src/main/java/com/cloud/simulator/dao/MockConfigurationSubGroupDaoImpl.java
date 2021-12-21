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
package com.cloud.simulator.dao;

import java.util.List;

import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;

import com.cloud.utils.db.GenericDaoBase;

public class MockConfigurationSubGroupDaoImpl extends GenericDaoBase<ConfigurationSubGroupVO, Long> implements ConfigurationSubGroupDao {
    public ConfigurationSubGroupVO findByName(String name) {
        return null;
    }

    public ConfigurationSubGroupVO findByKeyword(String keyword) {
        return null;
    }

    public List<ConfigurationSubGroupVO> findByGroup(Long groupId) {
        return null;
    }
}
