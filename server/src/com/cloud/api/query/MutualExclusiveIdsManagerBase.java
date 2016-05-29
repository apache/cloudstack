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
package com.cloud.api.query;

import java.util.ArrayList;
import java.util.List;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.SearchCriteria;

public class MutualExclusiveIdsManagerBase extends ManagerBase {

    /***
     * Include ids list in query criteria if ids is not null
     * @param sc search criteria, class type SearchCriteria<Z>
     * @param ids ids list, class type List<T>
     */
    protected <Z,T> void setIdsListToSearchCriteria(SearchCriteria<Z> sc, List<T> ids){
        if (ids != null && !ids.isEmpty()) {
            sc.setParameters("idIN", ids.toArray());
        }
    }

    /***
     * Mutually exclusive parameters id and ids for API calls.<br/>
     * Retrieve a list of ids or a list containing id depending on which of them is not null, or null if both are null
     * @param id entity id, class type T
     * @param ids entities ids, class type List<T>
     * @return if id is not null return a list containing id else return ids, if both parameters are null -> return null
     * @throws InvalidParameterValueException - if id and ids are both not null
     */
    protected <T> List<T> getIdsListFromCmd(T id, List<T> ids){
        List<T> idsList = null;
        if (id != null) {
            if (ids != null && !ids.isEmpty()) {
                throw new InvalidParameterValueException("Specify either id or ids but not both parameters");
            }
            idsList = new ArrayList<T>();
            idsList.add(id);
        } else {
            idsList = ids;
        }
        return idsList;
    }
}
