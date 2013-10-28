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
package org.apache.cloudstack.resourcedetail;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ResourceDetail;

import com.cloud.utils.db.GenericDao;

public interface ResourceDetailsDao<R extends ResourceDetail> extends GenericDao<R, Long>{
    /**
     * Finds detail by resourceId and key
     * @param resourceId
     * @param name
     * @return
     */
    public R findDetail(long resourceId, String name);
    
    /**
     * Removes all details for the resource specified
     * @param resourceId
     */
    public void removeDetails(long resourceId);

    /**
     * Removes detail having resourceId and key specified (unique combination)
     * @param resourceId
     * @param key
     */
    public void removeDetail(long resourceId, String key);

    /**
     * Lists all details for the resourceId
     * @param resourceId
     * @return list of details each implementing ResourceDetail interface
     */
    public List<R> listDetails(long resourceId);
    
    /**
     * List details for resourceId having display field = forDisplay value passed in
     * @param resourceId
     * @param forDisplay
     * @return
     */
    public List<R> listDetails(long resourceId, boolean forDisplay);

    public Map<String, String> listDetailsKeyPairs(long resourceId);
    
    public Map<String, String> listDetailsKeyPairs(long resourceId, boolean forDisplay);
    
    public void saveDetails(List<R> details);
        
    public void addDetail(long resourceId, String key, String value);

}
