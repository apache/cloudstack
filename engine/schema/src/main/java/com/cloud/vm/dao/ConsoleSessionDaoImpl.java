//
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

package com.cloud.vm.dao;

import com.cloud.vm.ConsoleSessionVO;
import com.cloud.utils.db.GenericDaoBase;

public class ConsoleSessionDaoImpl extends GenericDaoBase<ConsoleSessionVO, Long> implements ConsoleSessionDao {

    @Override
    public void removeSession(String sessionUuid) {
        ConsoleSessionVO session = findByUuid(sessionUuid);
        remove(session.getId());
    }

    @Override
    public boolean isSessionAllowed(String sessionUuid) {
        return findByUuid(sessionUuid) != null;
    }
}
