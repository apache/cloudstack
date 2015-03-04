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

/**
 * SearchBuilder is meant as a static query construct.  Often times in DAO code,
 * we write static sql that just assumes the database table does not change.
 * change by hand.  SearchBuilder is meant to replace that.  It provides load-time
 *
 *   1. SearchBuilder is declared as final because it should not change after load time.
 *   2. search.entity().getHostId() allows you to declare which field you are searching
 *      on.  By doing this, we take advantage of the compiler to check that we
 *      are not searching on obsolete fields.  You should try to use this
 *      as much as possible as oppose to the addAndByField() and addOrByField() methods.
 *   3. Note that the same SearchBuilder is used to create multiple SearchCriteria.
 *      This is equivalent to clearing the parameters on PreparedStatement.  The
 *      multiple SearchCriteria does not interfere with each other.
 *   4. Note that the same field (getHostId()) was specified with two param
 *      names.  This is basically allowing you to reuse the same field in two
 *      parts of the search.
 *
 * {@code
 * final SearchBuilder<UserVmVO> search = _userVmDao.createSearchBuilder();
 * final String param1 = "param1";
 * final String param2 = "param2";
 * search.addAnd(param1, search.entity().getHostId(), SearchCriteria.Op.NEQ);
 * search.addAnd(param2, search.entity().getHostId(), SearchCriteria.op.GT);
 * ...
 * SearchCriteria sc = search.create();
 * sc.setParameters(param1, 3);
 * sc.setParameters(param2, 1);
 *
 * ...
 *
 * SearchCriteria sc2 = search.create();
 * sc2.setParameters(param1, 4);
 * sc2.setParameters(param2, 1);
 * }
 *
 * @param <T> VO object.
 */
public class SearchBuilder<T> extends GenericSearchBuilder<T, T> {

    public SearchBuilder(Class<T> entityType) {
        super(entityType, entityType);
    }
}
