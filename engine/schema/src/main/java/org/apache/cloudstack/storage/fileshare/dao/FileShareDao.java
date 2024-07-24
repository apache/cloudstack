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
package org.apache.cloudstack.storage.fileshare.dao;

import com.cloud.utils.Pair;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareVO;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

import java.util.Date;
import java.util.List;

public interface FileShareDao extends GenericDao<FileShareVO, Long>, StateDao<FileShare.State, FileShare.Event, FileShare> {
    Pair<List<FileShareVO>, Integer> searchAndCount(Long fileShareId, Long accountId, Long networkId, Long startIndex, Long pageSizeVal);

    List<FileShareVO> listFileSharesToBeDestroyed(Date date);
}
