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

package org.apache.cloudstack.veeam.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

/**
 * Root collection wrapper:
 * {
 *   "data_center": [ { ... } ]
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DataCenters {

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<DataCenter> dataCenter;

    public DataCenters() {}
    public DataCenters(final List<DataCenter> dataCenter) {
        this.dataCenter = dataCenter;
    }

    public List<DataCenter> getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(List<DataCenter> dataCenter) {
        this.dataCenter = dataCenter;
    }
}
