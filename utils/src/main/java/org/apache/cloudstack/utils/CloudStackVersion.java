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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.substringBefore;

/**
 *
 * A value object representing a version of the Management or Usage Server (as opposed to a Virtual Router).  It is
 * intended to supersede {@link com.cloud.maint.Version}.
 *
 * @since 4.8.2.0
 *
 */
public final class CloudStackVersion implements Comparable<CloudStackVersion> {

    private final static Pattern VERSION_FORMAT = Pattern.compile("(\\d+\\.){2}(\\d+\\.)?\\d+");

    /**
     *
     * Parses a <code>String</code> representation of a version that conforms one of the following
     * formats into a <code>CloudStackVersion</code> instance:
     * <ul>
     *     <li><code><major version>.<minor version>.<patch release></code></li>
     *     <li><code><major version>.<minor version>.<patch release>.<security release></code></li>
     *     <li><code><major version>.<minor version>.<patch release>.<security release>-<any string></code></li>
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
        final String trimmedValue = substringBefore(value, "-");

        checkArgument(isNotBlank(trimmedValue), CloudStackVersion.class.getName() + ".parse(String) requires a non-blank value");
        checkArgument(VERSION_FORMAT.matcher(trimmedValue).matches(), CloudStackVersion.class.getName() + "parse(String) passed " +
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

    private static ImmutableList<Integer> normalizeVersionValues(final ImmutableList<Integer> values) {

        checkArgument(values != null);
        checkArgument(values.size() == 3 || values.size() == 4);

        if (values.size() == 3) {
            return ImmutableList.<Integer>builder().addAll(values).add(0).build();
        }

        return values;

    }

    /**
     * {@inheritDoc}
     *
     * A couple of notes about the comparison rules for this method:
     * <ul>
     *     <li>Three position versions are normalized to four position versions with the security release being
     *         defaulted to zero (0).  For example, for the purposes of comparision, <code>4.8.1</code> would be
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

}
