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
 * Root response for GET /ovirt-engine/api
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Api {

    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;
    private EmptyElement engineBackup;
    private ProductInfo productInfo;
    private SpecialObjects specialObjects;
    private ApiSummary summary;
    private Long time;
    private Ref authenticatedUser;
    private Ref effectiveUser;

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public EmptyElement getEngineBackup() {
        return engineBackup;
    }

    public void setEngineBackup(EmptyElement engineBackup) {
        this.engineBackup = engineBackup;
    }

    public ProductInfo getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(ProductInfo productInfo) {
        this.productInfo = productInfo;
    }

    public SpecialObjects getSpecialObjects() {
        return specialObjects;
    }

    public void setSpecialObjects(SpecialObjects specialObjects) {
        this.specialObjects = specialObjects;
    }

    public ApiSummary getSummary() {
        return summary;
    }

    public void setSummary(ApiSummary summary) {
        this.summary = summary;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Ref getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(Ref authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public Ref getEffectiveUser() {
        return effectiveUser;
    }

    public void setEffectiveUser(Ref effectiveUser) {
        this.effectiveUser = effectiveUser;
    }
}
