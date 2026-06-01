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
package com.cloud.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.domain.dao.DomainDao;

@Component
public class DomainHelper {

    @Inject
    private DomainDao domainDao;

    /**
     *
     * @param domainIds List of domain IDs to filter
     * @return Filtered list containing only domains that are not descendants of other domains in the list
     */
    public List<Long> filterChildSubDomains(final List<Long> domainIds) {
        if (domainIds == null || domainIds.size() <= 1) {
            return domainIds == null ? new ArrayList<>() : new ArrayList<>(domainIds);
        }

        final List<Long> result = new ArrayList<>();
        for (final Long candidate : domainIds) {
            boolean isDescendant = false;
            for (final Long other : domainIds) {
                if (Objects.equals(candidate, other)) {
                    continue;
                }
                if (domainDao.isChildDomain(other, candidate)) {
                    isDescendant = true;
                    break;
                }
            }
            if (!isDescendant) {
                result.add(candidate);
            }
        }
        return result;
    }
}
