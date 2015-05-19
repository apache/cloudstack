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
package org.apache.cloudstack.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.cloud.exception.InvalidParameterValueException;

public abstract class BaseListTaggedResourcesCmd extends BaseListProjectAndAccountResourcesCmd implements IBaseListTaggedResourcesCmd {
    @Parameter(name = ApiConstants.TAGS, type = CommandType.MAP, description = "List resources by tags (key/value pairs)")
    private Map tags;

    @Override
    public Map<String, String> getTags() {
        Map<String, String> tagsMap = null;
        if (tags != null && !tags.isEmpty()) {
            tagsMap = new HashMap<String, String>();
            Collection<?> servicesCollection = tags.values();
            Iterator<?> iter = servicesCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> services = (HashMap<String, String>)iter.next();
                String key = services.get("key");
                String value = services.get("value");
                if (value == null) {
                    throw new InvalidParameterValueException("No value is passed in for key " + key);
                }
                tagsMap.put(key, value);
            }
        }
        return tagsMap;
    }
}
