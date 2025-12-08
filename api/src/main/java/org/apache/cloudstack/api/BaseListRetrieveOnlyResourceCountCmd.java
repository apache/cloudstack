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

import org.apache.commons.lang3.BooleanUtils;

public abstract class BaseListRetrieveOnlyResourceCountCmd extends BaseListTaggedResourcesCmd {
    @Parameter(name = ApiConstants.RETRIEVE_ONLY_RESOURCE_COUNT, type = CommandType.BOOLEAN, description = "makes the API's response contains only the resource count")
    private Boolean retrieveOnlyResourceCount;

    public Boolean getRetrieveOnlyResourceCount() {
        return BooleanUtils.toBooleanDefaultIfNull(retrieveOnlyResourceCount, false);
    }
}
