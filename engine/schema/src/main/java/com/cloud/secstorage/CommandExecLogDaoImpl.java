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
package com.cloud.secstorage;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class CommandExecLogDaoImpl extends GenericDaoBase<CommandExecLogVO, Long> implements CommandExecLogDao {

    protected final SearchBuilder<CommandExecLogVO> ExpungeSearch;
    protected final SearchBuilder<CommandExecLogVO> CommandSearch;

    public CommandExecLogDaoImpl() {
        ExpungeSearch = createSearchBuilder();
        ExpungeSearch.and("created", ExpungeSearch.entity().getCreated(), Op.LT);
        ExpungeSearch.done();

        CommandSearch = createSearchBuilder();
        CommandSearch.and("host_id", CommandSearch.entity().getHostId(), Op.EQ);
        CommandSearch.and("command_name", CommandSearch.entity().getCommandName(), Op.EQ);
    }

    @Override
    public void expungeExpiredRecords(Date cutTime) {
        SearchCriteria<CommandExecLogVO> sc = ExpungeSearch.create();
        sc.setParameters("created", cutTime);
        expunge(sc);
    }

    @Override
    public Integer getCopyCmdCountForSSVM(Long id) {
        SearchCriteria<CommandExecLogVO> sc = CommandSearch.create();
        sc.setParameters("host_id", id);
        sc.setParameters("command_name", "CopyCommand");
        List<CommandExecLogVO> copyCmds = customSearch(sc, null);
        return copyCmds.size();
    }
}
