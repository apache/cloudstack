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
package org.apache.cloudstack.framework.config.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.framework.config.impl.CommandTimeoutVO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;

public class CommandTimeoutDaoImpl extends GenericDaoBase<CommandTimeoutVO, String> implements CommandTimeoutDao {

    private GenericSearchBuilder<CommandTimeoutVO, Integer> maxCommandTimeoutSearchBuilder;

    public CommandTimeoutDaoImpl() {
        super();

        maxCommandTimeoutSearchBuilder = createSearchBuilder(Integer.class);
        maxCommandTimeoutSearchBuilder.select(null, SearchCriteria.Func.MAX, maxCommandTimeoutSearchBuilder.entity().getTimeout());
        maxCommandTimeoutSearchBuilder.and("command_classpath", maxCommandTimeoutSearchBuilder.entity().getCommandClasspath(), SearchCriteria.Op.IN);
        maxCommandTimeoutSearchBuilder.done();
    }

    @Override
    public int findMaxTimeoutBetweenCommands(Set<String> commandsClassPath) {
        SearchCriteria<Integer> searchCriteria = maxCommandTimeoutSearchBuilder.create();
        searchCriteria.setParameters("command_classpath", commandsClassPath.toArray());
        Integer max = customSearch(searchCriteria, null).get(0);
        return ObjectUtils.defaultIfNull(max, 0);
    }
}
