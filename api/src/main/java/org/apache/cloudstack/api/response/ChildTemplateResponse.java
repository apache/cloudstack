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
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.template.VirtualMachineTemplate;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VirtualMachineTemplate.class)
@SuppressWarnings("unused")
public class ChildTemplateResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the template ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the template name")
    private String name;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "the size of the template")
    private Integer size;

    @SerializedName(ApiConstants.TEMPLATE_TYPE)
    @Param(description = "the type of the template")
    private String templateType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

}
