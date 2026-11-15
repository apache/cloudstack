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
package org.apache.cloudstack.api.command.offering;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;

import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper for commands that accept a domainIds or zoneIds string and need to
 * resolve them to lists of IDs, falling back to an offering-specific
 * default provider.
 */
public interface DomainAndZoneIdResolver {
    /**
     * Parse the provided domainIds string and return a list of domain IDs.
     * If domainIds is empty, the defaultDomainsProvider will be invoked with the
     * provided resource id to obtain the current domains.
     */
    default List<Long> resolveDomainIds(final String domainIds, final Long id, final LongFunction<List<Long>> defaultDomainsProvider, final String resourceTypeName) {
        final List<Long> validDomainIds = new ArrayList<>();
        final BaseCmd base = (BaseCmd) this;
        final Logger logger = LogManager.getLogger(base.getClass());

        if (StringUtils.isEmpty(domainIds)) {
            if (defaultDomainsProvider != null) {
                final List<Long> defaults = defaultDomainsProvider.apply(id);
                if (defaults != null) {
                    validDomainIds.addAll(defaults);
                }
            }
            return validDomainIds;
        }

        final String[] domains = domainIds.split(",");
        final String type = (resourceTypeName == null || resourceTypeName.isEmpty()) ? "offering" : resourceTypeName;
        for (String domain : domains) {
            final String trimmed = domain == null ? "" : domain.trim();
            if (trimmed.isEmpty() || "public".equalsIgnoreCase(trimmed)) {
                continue;
            }

            final Domain validDomain = base._entityMgr.findByUuid(Domain.class, trimmed);
            if (validDomain == null) {
                logger.warn("Invalid domain specified for {}", type);
                throw new InvalidParameterValueException("Failed to create " + type + " because invalid domain has been specified.");
            }
            validDomainIds.add(validDomain.getId());
        }

        return validDomainIds;
    }

    /**
     * Parse the provided zoneIds string and return a list of zone IDs.
     * If zoneIds is empty, the defaultZonesProvider will be invoked with the
     * provided resource id to obtain the current zones.
     */
    default List<Long> resolveZoneIds(final String zoneIds, final Long id, final LongFunction<List<Long>> defaultZonesProvider, final String resourceTypeName) {
        final List<Long> validZoneIds = new ArrayList<>();
        final BaseCmd base = (BaseCmd) this;
        final Logger logger = LogManager.getLogger(base.getClass());

        if (StringUtils.isEmpty(zoneIds)) {
            if (defaultZonesProvider != null) {
                final List<Long> defaults = defaultZonesProvider.apply(id);
                if (defaults != null) {
                    validZoneIds.addAll(defaults);
                }
            }
            return validZoneIds;
        }

        final String[] zones = zoneIds.split(",");
        final String type = (resourceTypeName == null || resourceTypeName.isEmpty()) ? "offering" : resourceTypeName;
        for (String zone : zones) {
            final String trimmed = zone == null ? "" : zone.trim();
            if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
                continue;
            }

            final DataCenter validZone = base._entityMgr.findByUuid(DataCenter.class, trimmed);
            if (validZone == null) {
                logger.warn("Invalid zone specified for {}: {}", type, trimmed);
                throw new InvalidParameterValueException("Failed to create " + type + " because invalid zone has been specified.");
            }
            validZoneIds.add(validZone.getId());
        }

        return validZoneIds;
    }
}
