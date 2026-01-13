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

package org.apache.cloudstack.veeam.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Query parameters supported by GET /api/vms (oVirt-like).
 *
 * Examples:
 *   /api/vms?search=name=myvm&max=50&page=1
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class VmListQuery {

    /**
     * oVirt-like search expression, e.g.:
     *   name=myvm
     *   status=down
     *   name=myvm and status=up
     */
    @JsonProperty("search")
    private String search;

    /**
     * Max number of entries to return.
     */
    @JsonProperty("max")
    private Integer max;

    /**
     * 1-based page number.
     */
    @JsonProperty("page")
    private Integer page;

    public VmListQuery() {
    }

    public VmListQuery(final String search, final Integer max, final Integer page) {
        this.search = search;
        this.max = max;
        this.page = page;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(final String search) {
        this.search = search;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(final Integer max) {
        this.max = max;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(final Integer page) {
        this.page = page;
    }

    // ----- helpers (optional, but convenient) -----

    @JsonIgnore
    public int resolvedMax(final int defaultMax, final int hardCap) {
        final int m = (max == null || max <= 0) ? defaultMax : max;
        return Math.min(m, hardCap);
    }

    @JsonIgnore
    public int resolvedPage(final int defaultPage) {
        return (page == null || page <= 0) ? defaultPage : page;
    }

    @JsonIgnore
    public int offset(final int defaultMax, final int hardCap, final int defaultPage) {
        final int p = resolvedPage(defaultPage);
        final int m = resolvedMax(defaultMax, hardCap);
        return (p - 1) * m;
    }
}
