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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ListQuery {
    private static final Pattern PAGE_CLAUSE_PATTERN = Pattern.compile("(?i)\\bpage\\s+(\\d+)\\b");

    boolean allContent;
    Long max;
    Long page;
    Map<String, String> search;
    List<String> follow;

    public boolean isAllContent() {
        return allContent;
    }

    public void setAllContent(boolean allContent) {
        this.allContent = allContent;
    }

    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public void setSearch(Map<String, String> search) {
        this.search = search;
    }

    public void setFollow(String followStr) {
        if (StringUtils.isBlank(followStr)) {
            this.follow = null;
            return;
        }
        this.follow = Arrays.stream(followStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public Long getOffset() {
        if (page == null || max == null) {
            return null;
        }
        return Math.max(0,   (page - 1)) * max;
    }

    public Long getLimit() {
        return max;
    }

    public boolean followContains(String part) {
        if (CollectionUtils.isEmpty(follow)) {
            return false;
        }
        return follow.contains(part);
    }

    public static ListQuery fromRequest(HttpServletRequest request) {
        ListQuery query = new ListQuery();
        if (MapUtils.isEmpty(request.getParameterMap())) {
            return query;
        }

        String allContent = request.getParameter("all_content");
        if (StringUtils.isNotBlank(allContent)) {
            query.setAllContent(Boolean.parseBoolean(allContent));
        }
        String max = request.getParameter("max");
        if (StringUtils.isNotBlank(max)) {
            try {
                query.setMax(Long.parseLong(max));
            } catch (NumberFormatException e) {
                // Ignore invalid max and keep default null value.
            }
        }
        String follow = request.getParameter("follow");
        query.setFollow(follow);
        Map<String, String> searchItems = getSearchMap(request.getParameter("search"));
        if (!searchItems.isEmpty()) {
            String pageValue = searchItems.get("page");
            if (StringUtils.isNotBlank(pageValue)) {
                try {
                    query.setPage(Long.parseLong(pageValue));
                } catch (NumberFormatException e) {
                    // Ignore invalid page and keep default null value.
                }
            }
            query.setSearch(searchItems);
        }

        return query;
    }

    // Parse search clause. For now, only extract the oVirt paging clause.
    // Examples:
    //   page 3 --> {page=3}
    //   sortby name page 2 --> {page=2}
    @NotNull
    private static Map<String, String> getSearchMap(String searchClause) {
        Map<String, String> searchItems = new LinkedHashMap<>();
        if (StringUtils.isBlank(searchClause)) {
            return searchItems;
        }
        Matcher matcher = PAGE_CLAUSE_PATTERN.matcher(searchClause);
        if (matcher.find()) {
            searchItems.put("page", matcher.group(1));
        }
        return searchItems;
    }
}
