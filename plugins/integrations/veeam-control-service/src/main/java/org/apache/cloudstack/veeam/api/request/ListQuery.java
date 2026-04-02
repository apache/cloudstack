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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ListQuery {
    boolean allContent;
    Long max;
    Long page;
    Map<String, String> search;

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

    public Map<String, String> getSearch() {
        return search;
    }

    public void setSearch(Map<String, String> search) {
        this.search = search;
    }

    public Long getPage() {
        return page;
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
        Map<String, String> searchItems = getSearchMap(request.getParameter("search"));
        if (!searchItems.isEmpty()) {
            try {
                query.setMax(Long.parseLong(searchItems.get("page")));
            } catch (NumberFormatException e) {
                // Ignore invalid page and keep default null value.
            }
            query.setSearch(searchItems);
        }

        return query;
    }

    // Parse search clause. Only keep items which use simple '=' operator, and ignore others. For example:
    //   name=myvm and status=up  --> {name=myvm, status=up}
    //   name=myvm and status!=down --> {name=myvm} (ignore status!=down because it uses '!=' operator)
    @NotNull
    private static Map<String, String> getSearchMap(String searchClause) {
        Map<String, String> searchItems = new LinkedHashMap<>();
        if (StringUtils.isBlank(searchClause)) {
            return searchItems;
        }
        String[] terms = searchClause.trim().split("(?i)\\s+and\\s+");
        for (String term : terms) {
            if (term == null) {
                continue;
            }
            String trimmedTerm = term.trim();
            if (trimmedTerm.isEmpty()) {
                continue;
            }

            int eqIdx = trimmedTerm.indexOf('=');
            if (eqIdx <= 0 || eqIdx != trimmedTerm.lastIndexOf('=')) {
                continue;
            }
            char prev = trimmedTerm.charAt(eqIdx - 1);
            if (prev == '!' || prev == '<' || prev == '>') {
                continue;
            }

            String key = trimmedTerm.substring(0, eqIdx).trim();
            String value = trimmedTerm.substring(eqIdx + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                searchItems.put(key, value);
            }
        }
        return searchItems;
    }
}
