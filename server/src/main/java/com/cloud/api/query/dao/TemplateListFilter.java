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
package com.cloud.api.query.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate.State;
import com.cloud.user.Account;

/**
 * Immutable filter parameters for a listTemplates / listIsos call.
 *
 * Built once from the cmd parameters in QueryManagerImpl.searchForTemplatesInternal,
 * then either consumed by the SearchBuilder path (existing behavior) or by the
 * bypass-the-view path (TemplateJoinDaoImpl.findDistinctTempZonePairs).
 *
 * {@link #canBypass()} reports whether the bypass implementation supports this
 * filter combination. The dispatcher must fall back to the SearchBuilder path
 * when canBypass() returns false.
 */
public final class TemplateListFilter {

    // identity
    public final Long templateId;            // exact id match
    public final List<Long> ids;             // id IN list

    // text search
    public final String name;                // name EQ when keyword is null
    public final String keyword;             // name LIKE %keyword% (overrides name)

    // attributes
    public final HypervisorType hypervisorType;            // EQ when present and not None
    public final List<HypervisorType> availableHypervisors; // IN list (from listAvailHypervisorInZone)
    public final ImageFormat format;         // EQ for ISO, NEQ for non-ISO
    public final boolean isIso;
    public final boolean excludeSystemTemplates;  // templateType NEQ SYSTEM
    public final Boolean publicTemplate;     // EQ when non-null (forced by featured/community/all-non-admin paths)
    public final Boolean featured;           // EQ when non-null (true=featured, false=community)
    public final Boolean bootable;           // EQ when non-null
    public final Long parentTemplateId;
    public final boolean onlyReady;          // composite state-Ready filter

    // ACL
    public final Account.Type accountTypeNeq;     // accountType NEQ (typically !=Project)
    public final Account.Type accountTypeEq;      // accountType EQ (typically ==Project)
    public final List<Long> accountIds;           // EQ-OR-IN over account_id
    public final boolean accountIdRequiredForFilter; // when templateFilter forces it (self/selfexecutable)
    public final boolean publicOrAccountIdComposite; // executable composite: (public OR account_id IN)
    public final boolean publicOrDomainPathComposite; // all-non-admin SkipProjectResources composite: (public OR domain.path LIKE)

    // location
    public final Long zoneId;                // composite dataCenterId filter

    // state
    public final boolean showRemoved;        // when true, no templateState filter applied
    public final List<State> templateStates; // states IN list when showRemoved is false

    // pagination & shape
    public final boolean showUnique;
    public final Long startIndex;
    public final Long pageSize;
    public final boolean sortAscending;

    // hard filters — presence forces fallback to the SearchBuilder path
    public final List<Long> sharedAccountIds;        // sharedexecutable / shared / all-non-admin
    public final String domainPathLike;              // domain admin scoping
    public final List<Long> domainIdsForFeaturedCommunity; // featured/community related-domain hierarchy
    public final Map<String, String> tags;
    public final boolean requiresViewFallback;       // catch-all flag for templateFilter combinations the bypass SQL doesn't model

    private TemplateListFilter(Builder b) {
        this.templateId = b.templateId;
        this.ids = nullSafe(b.ids);
        this.name = b.name;
        this.keyword = b.keyword;
        this.hypervisorType = b.hypervisorType;
        this.availableHypervisors = nullSafe(b.availableHypervisors);
        this.format = b.format;
        this.isIso = b.isIso;
        this.excludeSystemTemplates = b.excludeSystemTemplates;
        this.publicTemplate = b.publicTemplate;
        this.featured = b.featured;
        this.bootable = b.bootable;
        this.parentTemplateId = b.parentTemplateId;
        this.onlyReady = b.onlyReady;
        this.accountTypeNeq = b.accountTypeNeq;
        this.accountTypeEq = b.accountTypeEq;
        this.accountIds = nullSafe(b.accountIds);
        this.accountIdRequiredForFilter = b.accountIdRequiredForFilter;
        this.publicOrAccountIdComposite = b.publicOrAccountIdComposite;
        this.publicOrDomainPathComposite = b.publicOrDomainPathComposite;
        this.zoneId = b.zoneId;
        this.showRemoved = b.showRemoved;
        this.templateStates = nullSafe(b.templateStates);
        this.showUnique = b.showUnique;
        this.startIndex = b.startIndex;
        this.pageSize = b.pageSize;
        this.sortAscending = b.sortAscending;
        this.sharedAccountIds = nullSafe(b.sharedAccountIds);
        this.domainPathLike = b.domainPathLike;
        this.domainIdsForFeaturedCommunity = nullSafe(b.domainIdsForFeaturedCommunity);
        this.tags = b.tags == null ? Collections.emptyMap() : Collections.unmodifiableMap(b.tags);
        this.requiresViewFallback = b.requiresViewFallback;
    }

    /**
     * True if the bypass-the-view DAO path can handle this filter set. False
     * if any "hard" filter is set that requires extra joins beyond the base 6
     * tables (vm_template, account, template_store_ref, image_store,
     * template_zone_ref, data_center).
     */
    public boolean canBypass() {
        if (requiresViewFallback) {
            return false;
        }
        if (!sharedAccountIds.isEmpty()) {
            return false;
        }
        if (!domainIdsForFeaturedCommunity.isEmpty()) {
            return false;
        }
        if (!tags.isEmpty()) {
            return false;
        }
        return true;
    }

    private static <T> List<T> nullSafe(List<T> in) {
        return in == null ? Collections.<T>emptyList() : Collections.unmodifiableList(in);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long templateId;
        private List<Long> ids;
        private String name;
        private String keyword;
        private HypervisorType hypervisorType;
        private List<HypervisorType> availableHypervisors;
        private ImageFormat format;
        private boolean isIso;
        private boolean excludeSystemTemplates;
        private Boolean publicTemplate;
        private Boolean featured;
        private Boolean bootable;
        private Long parentTemplateId;
        private boolean onlyReady;
        private Account.Type accountTypeNeq;
        private Account.Type accountTypeEq;
        private List<Long> accountIds;
        private boolean accountIdRequiredForFilter;
        private boolean publicOrAccountIdComposite;
        private boolean publicOrDomainPathComposite;
        private Long zoneId;
        private boolean showRemoved;
        private List<State> templateStates;
        private boolean showUnique;
        private Long startIndex;
        private Long pageSize;
        private boolean sortAscending = true;
        private List<Long> sharedAccountIds;
        private String domainPathLike;
        private List<Long> domainIdsForFeaturedCommunity;
        private Map<String, String> tags;
        private boolean requiresViewFallback;

        public Builder templateId(Long v) { this.templateId = v; return this; }
        public Builder ids(List<Long> v) { this.ids = v; return this; }
        public Builder name(String v) { this.name = v; return this; }
        public Builder keyword(String v) { this.keyword = v; return this; }
        public Builder hypervisorType(HypervisorType v) { this.hypervisorType = v; return this; }
        public Builder availableHypervisors(List<HypervisorType> v) { this.availableHypervisors = v; return this; }
        public Builder format(ImageFormat v) { this.format = v; return this; }
        public Builder isIso(boolean v) { this.isIso = v; return this; }
        public Builder excludeSystemTemplates(boolean v) { this.excludeSystemTemplates = v; return this; }
        public Builder publicTemplate(Boolean v) { this.publicTemplate = v; return this; }
        public Builder featured(Boolean v) { this.featured = v; return this; }
        public Builder bootable(Boolean v) { this.bootable = v; return this; }
        public Builder parentTemplateId(Long v) { this.parentTemplateId = v; return this; }
        public Builder onlyReady(boolean v) { this.onlyReady = v; return this; }
        public Builder accountTypeNeq(Account.Type v) { this.accountTypeNeq = v; return this; }
        public Builder accountTypeEq(Account.Type v) { this.accountTypeEq = v; return this; }
        public Builder accountIds(List<Long> v) { this.accountIds = v; return this; }
        public Builder accountIdRequiredForFilter(boolean v) { this.accountIdRequiredForFilter = v; return this; }
        public Builder publicOrAccountIdComposite(boolean v) { this.publicOrAccountIdComposite = v; return this; }
        public Builder publicOrDomainPathComposite(boolean v) { this.publicOrDomainPathComposite = v; return this; }
        public Builder zoneId(Long v) { this.zoneId = v; return this; }
        public Builder showRemoved(boolean v) { this.showRemoved = v; return this; }
        public Builder templateStates(List<State> v) { this.templateStates = v; return this; }
        public Builder showUnique(boolean v) { this.showUnique = v; return this; }
        public Builder startIndex(Long v) { this.startIndex = v; return this; }
        public Builder pageSize(Long v) { this.pageSize = v; return this; }
        public Builder sortAscending(boolean v) { this.sortAscending = v; return this; }
        public Builder sharedAccountIds(List<Long> v) { this.sharedAccountIds = v; return this; }
        public Builder domainPathLike(String v) { this.domainPathLike = v; return this; }
        public Builder domainIdsForFeaturedCommunity(List<Long> v) { this.domainIdsForFeaturedCommunity = v; return this; }
        public Builder tags(Map<String, String> v) { this.tags = v; return this; }
        public Builder requiresViewFallback(boolean v) { this.requiresViewFallback = v; return this; }

        public TemplateListFilter build() {
            return new TemplateListFilter(this);
        }
    }
}
