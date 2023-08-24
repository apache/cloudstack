//
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
//
package org.apache.cloudstack.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 *
 * A value object representing a version of the Management or Usage Server (as opposed to a Virtual Router).  It is
 * intended to supersede {@link com.cloud.maint.Version}.
 *
 * @since 4.8.2.0
 *
 */
public final class CloudStackVersion implements Comparable<CloudStackVersion> {

    private final static Pattern NUMBER_VERSION_FORMAT = Pattern.compile("(\\d+\\.){2}(\\d+\\.)?\\d+");
    private final static Pattern FULL_VERSION_FORMAT = Pattern.compile("(\\d+\\.){2}(\\d+\\.)?\\d+(-[a-zA-Z]+)?(-\\d+)?(-SNAPSHOT)?");

    private final int majorRelease;
    private final int minorRelease;
    private final int patchRelease;
    private final Integer securityRelease;

    private CloudStackVersion(final int majorRelease, final int minorRelease, final int patchRelease, final Integer securityRelease) {

        super();

        checkArgument(majorRelease >= 0, CloudStackVersion.class.getName() + "(int, int, int, Integer) requires a majorRelease greater than 0.");
        checkArgument(minorRelease >= 0, CloudStackVersion.class.getName() + "(int, int, int, Integer) requires a minorRelease greater than 0.");
        checkArgument(patchRelease >= 0, CloudStackVersion.class.getName() + "(int, int, int, Integer) requires a patchRelease greater than 0.");
        checkArgument((securityRelease != null && securityRelease >= 0) || (securityRelease == null),
                CloudStackVersion.class.getName() + "(int, int, int, Integer) requires a null securityRelease or a non-null value greater than 0.");

        this.majorRelease = majorRelease;
        this.minorRelease = minorRelease;
        this.patchRelease = patchRelease;
        this.securityRelease = securityRelease;

    }

    /**
     *
     * Parses a <code>String</code> representation of a version that conforms one of the following
     * formats into a <code>CloudStackVersion</code> instance:
     * <ul>
     *     <li><code>&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;.&lt;security&gt;</code></li>
     *     <li><code>&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;.&lt;security&gt;.&lt;security&gt;</code></li>
     *     <li><code>&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;.&lt;security&gt;.&lt;security&gt;-&lt;any string&gt;</code></li>
     * </ul>
     *
     * If the string contains a suffix that begins with a "-" character, then the "-" and all characters following it
     * will be dropped.
     *
     * @param value The value to parse which must be non-blank and conform the formats listed above
     *
     * @return <code>value</code> parsed into a <code>CloudStackVersion</code> instance
     *
     * @since 4.8.2
     *
     */
    public static CloudStackVersion parse(final String value) {

        // Strip out any legacy patch information from the version string ...
        final String trimmedValue = StringUtils.substringBefore(value, "-");

        checkArgument(StringUtils.isNotBlank(trimmedValue), CloudStackVersion.class.getName() + ".parse(String) requires a non-blank value");
        checkArgument(NUMBER_VERSION_FORMAT.matcher(trimmedValue).matches(), CloudStackVersion.class.getName() + ".parse(String) passed " +
                value + ", but requires a value in the format of int.int.int(.int)(-<legacy patch>)");

        final String[] components = trimmedValue.split("\\.");

        checkState(components != null && (components.length == 3 || components.length == 4), "Expected " + value +
                " to parse to 3 or 4 positions.");

        final int majorRelease = Integer.valueOf(components[0]);
        final int minorRelease = Integer.valueOf(components[1]);
        final int patchRelease = Integer.valueOf(components[2]);
        final Integer securityRelease = components.length == 3 ? null : Integer.valueOf(components[3]);

        return new CloudStackVersion(majorRelease, minorRelease, patchRelease, securityRelease);

    }

    /**
     * Shortcut method to {@link #parse(String)} and {@link #compareTo(CloudStackVersion)} two versions
     *
     * @param version1 the first value to be parsed and compared
     * @param version2 the second value to be parsed and compared
     *
     * @return A value less than zero (0) indicates <code>version1</code> is less than <code>version2</code>.  A value
     *         equal to zero (0) indicates <code>version1</code> equals <code>version2</code>.  A value greater than zero (0)
     *         indicates <code>version1</code> is greater than <code>version2</code>.
     *
     * @since 4.12.0.0
     */
    public static int compare(String version1, String version2) {
        return parse(version1).compareTo(parse(version2));
    }

    /**
     * {@inheritDoc}
     *
     * A couple of notes about the comparison rules for this method:
     * <ul>
     *     <li>Three position versions are normalized to four position versions with the security release being
     *         defaulted to zero (0).  For example, for the purposes of comparison, <code>4.8.1</code> would be
     *         normalized to <code>4.8.1.0</code> for all comparison operations.</li>
     *     <li>A three position version with a null security release is considered equal to a four position
     *         version number where the major release, minor release, and patch release are the same and the security
     *         release for the four position version is zero (0). Therefore, the results of this method are <b>not</b>
     *         symmetric with <code>equals</code></li>
     *     <li>When comparing to <code>null</code>, this version is always considered greater than (i.e. returning
     *         a value greater than zero (0).</li>
     * </ul>
     *
     * @param thatVersion The version to which to compare this instance
     *
     * @return A value less than zero (0) indicates this version is less than <code>thatVersion</code>.  A value
     *         equal to zero (0) indicates this value equals <code>thatValue</code>.  A value greater than zero (0)
     *         indicates this version is greater than <code>thatVersion</code>.
     *
     * @since 4.8.2.0
     *
     */
    @Override
    public int compareTo(final CloudStackVersion thatVersion) {

        if (thatVersion == null) {
            return 1;
        }

        // Normalize the versions to be 4 positions for the purposes of comparison ...
        final ImmutableList<Integer> values = normalizeVersionValues(asList());
        final ImmutableList<Integer> thoseValues = normalizeVersionValues(thatVersion.asList());

        for (int i = 0; i < values.size(); i++) {
            final int result = values.get(i).compareTo(thoseValues.get(i));
            if (result != 0) {
                return result;
            }
        }

        return 0;

    }

    /**
     * Trim full version from router version. Valid versions are:
     *
     * <ul>
     *    <li><code>&lt;major&gt;.&lt;minor&gt;[.&lt;patch&gt;[.&lt;security&gt;]]</li>
     *    <li><code>&lt;major&gt;.&lt;minor&gt;[.&lt;patch&gt;[.&lt;security&gt;]]-&lt;branding&gt;</li>
     *    <li><code>&lt;major&gt;.&lt;minor&gt;[.&lt;patch&gt;[.&lt;security&gt;]][-&lt;branding&gt;]-SNAPSHOT</li>
     *    <li><code>&lt;major&gt;.&lt;minor&gt;[.&lt;patch&gt;[.&lt;security&gt;]][-&lt;branding&gt;]-&lt;epoch timestamp&gt;</li>
     * </ul>
     *
     * @param version to trim
     *
     * @return actual trimmed version
     */
    public static String trimRouterVersion(String version) {
        final String[] tokens = version.split(" ");

        if (tokens.length >= 3 && FULL_VERSION_FORMAT.matcher(tokens[2]).matches()) {
            return tokens[2];
        }

        return "0";
    }

    private static ImmutableList<Integer> normalizeVersionValues(final ImmutableList<Integer> values) {

        checkArgument(values != null);
        checkArgument(values.size() == 3 || values.size() == 4);

        if (values.size() == 3) {
            return ImmutableList.<Integer>builder().addAll(values).add(0).build();
        }

        return values;

    }

    /**
     *
     * @return The components of this version as an {@link ImmutableList} in order of major release, minor release,
     * patch release, and security release
     *
     * @since 4.8.2.0
     *
     */
    public ImmutableList<Integer> asList() {

        final ImmutableList.Builder<Integer> values = ImmutableList.<Integer>builder().add
                (majorRelease, minorRelease, patchRelease);

        if (securityRelease != null) {
            values.add(securityRelease);
        }

        return values.build();

    }

    public int getMajorRelease() {
        return majorRelease;
    }

    public int getMinorRelease() {
        return minorRelease;
    }

    public int getPatchRelease() {
        return patchRelease;
    }

    public Integer getSecurityRelease() {
        return securityRelease;
    }

    @Override
    public boolean equals(final Object thatObject) {

        if (this == thatObject) {
            return true;
        }

        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        final CloudStackVersion thatVersion = (CloudStackVersion) thatObject;
        return majorRelease == thatVersion.majorRelease &&
                minorRelease == thatVersion.minorRelease &&
                patchRelease == thatVersion.patchRelease &&
                Objects.equal(securityRelease, thatVersion.securityRelease);

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(majorRelease, minorRelease, patchRelease, securityRelease);
    }

    @Override
    public String toString() {
        return Joiner.on(".").join(asList());
    }
}
