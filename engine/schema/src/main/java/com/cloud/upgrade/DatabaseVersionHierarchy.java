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
package com.cloud.upgrade;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import org.apache.cloudstack.utils.CloudStackVersion;

import com.cloud.upgrade.dao.DbUpgrade;

/**
 * @since 4.12.0.0
 */
public final class DatabaseVersionHierarchy {
    private final ImmutableList<VersionNode> hierarchy;

    private DatabaseVersionHierarchy(ImmutableList<VersionNode> hierarchy) {
        this.hierarchy = hierarchy;
    }

    public static DatabaseVersionHierarchyBuilder builder() {
        return new DatabaseVersionHierarchyBuilder();
    }

    /**
     * Check if current hierarchy of Database Versions contains <code>version</code>.
     *
     * @param version The version to check if hierarchy contains it
     *
     * @return true if hierarchy contains the version, false if not
     */
    public boolean contains(final CloudStackVersion version) {
        return toList().contains(version);
    }

    /**
     * Calculates an  upgrade path for the  passed <code>fromVersion</code>.  If the  <code>fromVersion</code>
     * doesn't exist in list  of  available <code>VersionNode</code> hierarchy,  then calculation assumes that
     * the <code>fromVersion</code> required no schema migrations or data conversions and no  upgrade path was
     * defined  for it.  Therefore,  we  find  the most  recent  version  with  database migrations before the
     * <code>fromVersion</code> and adopt that upgrade path list.
     *
     * @param fromVersion The version from which the upgrade will occur
     *
     * @return The upgrade path from <code>fromVersion</code> to <code>LATEST</code> version.
     */
    public DbUpgrade[] getPath(final CloudStackVersion fromVersion) {
        return getPath(fromVersion, null);
    }

    /**
     * Calculates an  upgrade path for the  passed <code>fromVersion</code> and <code>toVersion</code>. If the
     * <code>fromVersion</code> doesn't exist in list  of available <code>VersionNode</code> hierarchy,   then
     * calculation assumes that the <code>fromVersion</code> required no schema migrations or data conversions
     * and no  upgrade path was defined  for it.  Therefore, we find  the most recent  version  with  database
     * migrations before the <code>fromVersion</code> and adopt that upgrade path list up to <code>toVersion</code>.
     * If <code>toVersion</code> is null, we're going to find the upgrade path up to the latest available version.
     *
     * @param fromVersion The version from which the upgrade will occur
     * @param toVersion The version up to which the upgrade will occur (can be null)
     *
     * @return The upgrade path from <code>fromVersion</code> to <code>toVersion</code>
     */
    public DbUpgrade[] getPath(final CloudStackVersion fromVersion, final CloudStackVersion toVersion) {
        if (fromVersion == null) {
            return new DbUpgrade[0];
        }

        // we cannot find the version specified, so get the
        // most recent one immediately before this version
        if (!contains(fromVersion)) {
            return getPath(getRecentVersion(fromVersion), toVersion);
        }

        final Predicate<? super VersionNode> predicate;

        if (toVersion == null) {
            // all the available versions greater than or equal to fromVersion
            predicate = node -> node.version.compareTo(fromVersion) > -1;
        } else {
            // all the available versions greater than or equal to fromVersion AND less than toVersion
            predicate = node -> node.version.compareTo(fromVersion) > -1 && node.version.compareTo(toVersion) < 0;
        }

        // get upgrade path from version forward (include version itself in the path)
        return hierarchy
                    .stream()
                    .filter(predicate)
                    .filter(distinct(node -> node.upgrader.getUpgradedVersion()))
                    .map(node -> node.upgrader)
                    .toArray(DbUpgrade[]::new);
    }

    /**
     * Find the most recent <code>CloudStackVersion</code> immediately before <code>fromVersion</code>
     *
     * @param fromVersion The version to look up its immediate previous available version
     *
     * @return The <code>CloudStackVersion</code> or null
     *
     * @since 4.8.2.0 (refactored in 4.11.1.0)
     */
    protected CloudStackVersion getRecentVersion(final CloudStackVersion fromVersion) {
        if (fromVersion == null) {
            return null;
        }

        // find the most recent version immediately before fromVersion
        return toList()
                 .reverse()
                 .stream()
                 .filter(version -> fromVersion.compareTo(version) > 0)
                 .findFirst()
                 .orElse(null);
    }

    /**
     * Generate immutable list of available <code>CloudstackVersion</code> in the hierarchy
     *
     * @return list of available versions
     */
    public ImmutableList<CloudStackVersion> toList() {
        List<CloudStackVersion> versions = hierarchy
                                                .stream()
                                                .map(node -> node.version)
                                                .collect(Collectors.toList());

        return ImmutableList.copyOf(versions);
    }

    /**
     * Find the distinct <code>VersionNode</code> based on the provided <code>getUpgradedVersion()</code>
     */
    private Predicate<VersionNode> distinct(Function<VersionNode, String> keyExtractor) {
        Map<String, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    protected static class VersionNode {
        final CloudStackVersion version;
        final DbUpgrade upgrader;

        protected VersionNode(final CloudStackVersion version, final DbUpgrade upgrader) {
            this.version = version;
            this.upgrader = upgrader;
        }
    }


    public static final class DatabaseVersionHierarchyBuilder {
        private final List<VersionNode> hierarchyBuilder = new LinkedList<>();

        private DatabaseVersionHierarchyBuilder() {
        }

        public DatabaseVersionHierarchyBuilder next(final String version, final DbUpgrade upgrader) {
            hierarchyBuilder.add(new VersionNode(CloudStackVersion.parse(version), upgrader));
            return this;
        }

        public DatabaseVersionHierarchy build() {
            return new DatabaseVersionHierarchy(ImmutableList.copyOf(hierarchyBuilder));
        }
    }
}
