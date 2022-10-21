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

package org.apache.cloudstack.usage;

public enum UsageUnitTypes {
    COMPUTE_MONTH ("Compute*Month"),
    IP_MONTH ("IP*Month"),
    GB ("GB"),
    GB_MONTH ("GB*Month"),
    POLICY_MONTH ("Policy*Month");

    private final String description;

    private UsageUnitTypes(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * Retrieves the UsageUnitTypes according to the parameter.<br/><br/>
     * If there are no UsageUnitTypes with the description, it will try to retrieve it with {@link UsageUnitTypes#valueOf(String)} and will throw an
     * {@link IllegalArgumentException} if it not exist.
     */
    public static UsageUnitTypes getByDescription(String description) {
        for (UsageUnitTypes type : UsageUnitTypes.values()) {
            if (type.toString().equals(description)) {
                return type;
            }
        }

        return UsageUnitTypes.valueOf(description);
    }
}
