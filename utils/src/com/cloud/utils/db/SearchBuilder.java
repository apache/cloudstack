/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.db;

import java.util.Map;

/**
 * SearchBuilder is meant as a static query construct.  Often times in DAO code,
 * we write static sql that just assumes the database table does not change.
 * But if it does change, we're left with a bunch of strings that we have to
 * change by hand.  SearchBuilder is meant to replace that.  It provides load-time
 * checking for the fields within a VO object.
 * 
 * SearchBuilder is really meant to replace the static SQL strings and it should
 * be used as such.  The following is an example of how to use SearchBuilder.
 * You should not the following things.
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
    
    @SuppressWarnings("unchecked")
    public SearchBuilder(T entity, Map<String, Attribute> attrs) {
        super(entity, (Class<T>)entity.getClass(), attrs);
    }
}
