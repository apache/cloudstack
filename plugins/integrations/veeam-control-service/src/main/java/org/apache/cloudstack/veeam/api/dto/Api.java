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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Root response for GET /ovirt-engine/api
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "api")
public final class Api {

    // <link .../> repeated
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;

    // <engine_backup/> (empty element)
    @JacksonXmlProperty(localName = "engine_backup")
    public EmptyElement engineBackup;

    @JacksonXmlProperty(localName = "product_info")
    public ProductInfo productInfo;

    @JacksonXmlProperty(localName = "special_objects")
    public SpecialObjects specialObjects;

    @JacksonXmlProperty(localName = "summary")
    public Summary summary;

    // Keep as String to avoid timezone/date parsing friction; you control formatting.
    @JacksonXmlProperty(localName = "time")
    public Long time;

    @JacksonXmlProperty(localName = "authenticated_user")
    public Ref authenticatedUser;

    @JacksonXmlProperty(localName = "effective_user")
    public Ref effectiveUser;

    public Api() {}
}
