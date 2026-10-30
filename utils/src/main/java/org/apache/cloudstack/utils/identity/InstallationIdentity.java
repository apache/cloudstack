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
package org.apache.cloudstack.utils.identity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Derives the anonymous identity of a CloudStack installation.
 *
 * The identity is a SHA-256 over the version the database was originally created
 * with and the moment it was created, which is the oldest row of the version
 * table. That pair is fixed when the database is first initialised and is never
 * rewritten afterwards, so the identity is:
 *
 * <ul>
 * <li>the same for every Management Server sharing the database;</li>
 * <li>stable across restarts and upgrades;</li>
 * <li>different for every installation, including reinstalls, because the
 * creation timestamp differs;</li>
 * <li>one-way, so the originating version and install date cannot be recovered
 * from it.</li>
 * </ul>
 *
 * The timestamp is formatted as UTC before hashing so that the identity does not
 * depend on the timezone of the Management Server or on how the JDBC driver
 * happens to render a DATETIME.
 */
public final class InstallationIdentity {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Separates the two inputs so that different (version, created) pairs cannot
     * concatenate into the same string.
     */
    private static final String SEPARATOR = "|";

    private InstallationIdentity() {
    }

    /**
     * @param initialVersion the version the database was created with
     * @param created when the database was created
     * @return the 64 character lower case hex identity, or null if either input is
     *         missing, in which case the caller has no identity to report under
     */
    public static String generate(final String initialVersion, final Date created) {
        if (StringUtils.isBlank(initialVersion) || created == null) {
            return null;
        }

        return DigestUtils.sha256Hex(initialVersion + SEPARATOR + formatUtc(created));
    }

    private static String formatUtc(final Date created) {
        DateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(created);
    }
}
