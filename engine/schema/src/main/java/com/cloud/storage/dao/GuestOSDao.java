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
package com.cloud.storage.dao;

import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

import java.util.List;
import java.util.Set;

public interface GuestOSDao extends GenericDao<GuestOSVO, Long> {

    GuestOSVO findOneByDisplayName(String displayName);

    GuestOSVO findByCategoryIdAndDisplayNameOrderByCreatedDesc(long categoryId, String displayName);

    Set<String> findDoubleNames();

    List<GuestOSVO> listByDisplayName(String displayName);

    Pair<List<? extends GuestOS>, Integer> listGuestOSByCriteria(Long startIndex, Long pageSize, Long id, Long osCategoryId, String description, String keyword, Boolean forDisplay);
}
