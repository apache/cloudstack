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

package com.cloud.kubernetes.cluster.utils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helpers for the control plane certificate expiry check and renewal of CKS clusters.
 * Everything here is free of side effects so it can be unit tested.
 */
public class KubernetesClusterCertificateUtil {

    protected static Logger LOGGER = LogManager.getLogger(KubernetesClusterCertificateUtil.class);

    /** {@code kubeadm certs} replaced {@code kubeadm alpha certs} in this version. */
    public static final String MIN_KUBERNETES_VERSION_CERTIFICATES_SUPPORT = "1.20.0";

    public static final String CHECK_EXPIRATION_COMMAND = "sudo /opt/bin/kubeadm certs check-expiration";

    /** Prints only the client certificate line of root's kubeconfig, which CKS itself uses for kubectl. Never the key. */
    public static final String ROOT_KUBECONFIG_CERTIFICATE_COMMAND = "sudo grep -m1 'client-certificate-data:' /root/.kube/config";

    public static final String ROOT_KUBECONFIG_ENTRY_NAME = "/root/.kube/config";
    /**
     * getKubernetesClusterConfig hands out an operator-supplied user.conf instead of admin.conf when there is one, so
     * the stored kubeconfig has to be compared with that. Prints a marker when it exists, then its certificate line.
     */
    public static final String USER_KUBECONFIG_PRESENT = "user.conf present";
    public static final String USER_KUBECONFIG_CERTIFICATE_COMMAND = "if sudo test -f /etc/kubernetes/user.conf; then echo '" + USER_KUBECONFIG_PRESENT +
            "'; sudo grep -m1 'client-certificate-data:' /etc/kubernetes/user.conf; fi";

    /**
     * kubeadm issues certificates for one year; renewing more than this many days ahead would leave them inside the
     * renewal window right after a renewal and renew on every check.
     */
    public static final int MAX_AUTO_RENEW_DAYS = 180;

    /**
     * Expiries this close are treated as the same one for alerting: control nodes renewed in one rotation differ by
     * minutes, and which node reports the first expiry changes when one can't be checked. A renewal moves it by months.
     */
    private static final long SAME_EXPIRY_TOLERANCE_MS = TimeUnit.DAYS.toMillis(7);

    private static final String MISSING_MARKER = "!MISSING!";

    // kubeadm formats the dates with Go's "Jan 02, 2006 15:04 MST", always in UTC
    private static final Pattern CHECK_EXPIRATION_LINE = Pattern.compile(
            "^(\\S+)\\s+([A-Z][a-z]{2} \\d{1,2}, \\d{4} \\d{2}:\\d{2} [A-Z]+)\\s+.*$");
    private static final DateTimeFormatter CHECK_EXPIRATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm z", Locale.ENGLISH);
    private static final Pattern CLIENT_CERTIFICATE_DATA = Pattern.compile("client-certificate-data:\\s*([A-Za-z0-9+/=]+)");

    /** One line of {@code kubeadm certs check-expiration}. */
    public static class CertificateExpiry {
        private final String name;
        private final Date expires;
        private final boolean authority;
        private final boolean externallyManaged;

        public CertificateExpiry(String name, Date expires, boolean authority, boolean externallyManaged) {
            this.name = name;
            this.expires = expires;
            this.authority = authority;
            this.externallyManaged = externallyManaged;
        }

        public String getName() {
            return name;
        }

        /** @return the expiry date, or null when kubeadm reported the certificate as missing */
        public Date getExpires() {
            return expires;
        }

        public boolean isAuthority() {
            return authority;
        }

        public boolean isMissing() {
            return expires == null;
        }

        /** kubeadm cannot renew a certificate it reports as externally managed */
        public boolean isExternallyManaged() {
            return externallyManaged;
        }

        @Override
        public String toString() {
            return String.format("%s%s%s: %s", name, authority ? " (CA)" : "", externallyManaged ? " (externally managed)" : "",
                    isMissing() ? "missing" : expires);
        }
    }

    /**
     * The certificates of every control node of one cluster, as far as they could be read.
     */
    public static class CertificateExpiryReport {
        private final List<CertificateExpiry> entries = new ArrayList<>();
        private final List<String> uncheckedNodes = new ArrayList<>();

        public void add(String node, List<CertificateExpiry> nodeEntries) {
            for (CertificateExpiry entry : nodeEntries) {
                entries.add(new CertificateExpiry(String.format("%s on %s", entry.getName(), node), entry.getExpires(), entry.isAuthority(),
                        entry.isExternallyManaged()));
            }
        }

        public void addUncheckedNode(String node) {
            uncheckedNodes.add(node);
        }

        public List<CertificateExpiry> getEntries() {
            return entries;
        }

        public List<String> getUncheckedNodes() {
            return uncheckedNodes;
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        /** @return the leaf certificate (not a CA) that expires first, or null */
        public CertificateExpiry getFirstLeafExpiry() {
            return getFirstExpiry(false, true);
        }

        /** @return the leaf certificate that kubeadm can renew which expires first, or null */
        public CertificateExpiry getFirstRenewableLeafExpiry() {
            return getFirstExpiry(false, false);
        }

        /** @return the certificate authority that expires first, or null */
        public CertificateExpiry getFirstAuthorityExpiry() {
            return getFirstExpiry(true, true);
        }

        private CertificateExpiry getFirstExpiry(boolean authority, boolean includeExternallyManaged) {
            CertificateExpiry first = null;
            for (CertificateExpiry entry : entries) {
                if (entry.isAuthority() != authority || entry.isMissing() || (!includeExternallyManaged && entry.isExternallyManaged())) {
                    continue;
                }
                if (first == null || entry.getExpires().before(first.getExpires())) {
                    first = entry;
                }
            }
            return first;
        }
    }

    /**
     * Parses the output of {@code kubeadm certs check-expiration}. It has a leaf certificate table followed by a
     * certificate authority table; the line starting with {@code CERTIFICATE AUTHORITY} heads the second one.
     * Certificates kubeadm cannot find are printed as {@code !MISSING! <name>} and returned with a null expiry.
     * The last column of both tables tells whether the certificate is externally managed.
     */
    public static List<CertificateExpiry> parseCheckExpirationOutput(String output) {
        List<CertificateExpiry> entries = new ArrayList<>();
        if (StringUtils.isBlank(output)) {
            return entries;
        }
        boolean authoritySection = false;
        for (String rawLine : output.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("[")) {
                continue;
            }
            if (line.startsWith("CERTIFICATE AUTHORITY")) {
                authoritySection = true;
                continue;
            }
            if (line.startsWith("CERTIFICATE")) {
                authoritySection = false;
                continue;
            }
            if (line.startsWith(MISSING_MARKER)) {
                String name = StringUtils.trimToNull(line.substring(MISSING_MARKER.length()));
                if (name != null) {
                    entries.add(new CertificateExpiry(name.split("\\s+")[0], null, authoritySection, false));
                }
                continue;
            }
            Matcher matcher = CHECK_EXPIRATION_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            try {
                Date expires = Date.from(ZonedDateTime.parse(matcher.group(2), CHECK_EXPIRATION_DATE_FORMAT).toInstant());
                String[] columns = line.split("\\s+");
                boolean externallyManaged = "yes".equalsIgnoreCase(columns[columns.length - 1]);
                entries.add(new CertificateExpiry(matcher.group(1), expires, authoritySection, externallyManaged));
            } catch (DateTimeParseException e) {
                LOGGER.debug("Unable to parse the expiry date in kubeadm output line: {}", line, e);
            }
        }
        return entries;
    }

    /**
     * @return the expiry of the first {@code client-certificate-data} in a kubeconfig (or in a single line of one),
     * or null if there is none or it cannot be decoded
     */
    public static Date getKubeConfigClientCertificateExpiry(String kubeConfig) {
        if (StringUtils.isBlank(kubeConfig)) {
            return null;
        }
        Matcher matcher = CLIENT_CERTIFICATE_DATA.matcher(kubeConfig);
        if (!matcher.find()) {
            return null;
        }
        try {
            byte[] pem = Base64.decodeBase64(matcher.group(1));
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(pem));
            return certificate.getNotAfter();
        } catch (Exception e) {
            LOGGER.debug("Unable to decode the client certificate of a kubeconfig", e);
            return null;
        }
    }

    /**
     * Parses a comma-separated list of day thresholds, ignoring blanks, non-numbers and non-positive values.
     * @return the distinct thresholds, largest first
     */
    public static List<Integer> parseAlertThresholds(String value) {
        TreeSet<Integer> thresholds = new TreeSet<>(Collections.reverseOrder());
        if (StringUtils.isBlank(value)) {
            return new ArrayList<>(thresholds);
        }
        for (String part : value.split(",")) {
            try {
                int days = Integer.parseInt(part.trim());
                if (days > 0) {
                    thresholds.add(days);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Ignoring invalid Kubernetes certificate expiry alert threshold: {}", part);
            }
        }
        return new ArrayList<>(thresholds);
    }

    /** @return whole days from now until expiry, rounded down; negative once expired */
    public static long daysUntil(Date expiry, Date now) {
        return Math.floorDiv(expiry.getTime() - now.getTime(), TimeUnit.DAYS.toMillis(1));
    }

    /**
     * Decides whether an expiry alert is due.
     * @param daysLeft days until the certificate expires
     * @param thresholds the alert thresholds, as returned by {@link #parseAlertThresholds(String)}
     * @param lastAlertedThreshold the smallest threshold already alerted for this same expiry date, or null
     * @return the threshold to alert for now, or null when no new alert is due. Only the smallest threshold reached
     * is returned, so a cluster first checked at 20 days alerts once for 30, not for 60 and 30. Expiry itself counts
     * as threshold 0, so it is always alerted.
     */
    public static Integer getThresholdToAlert(long daysLeft, List<Integer> thresholds, Integer lastAlertedThreshold) {
        if (daysLeft < 0) {
            return lastAlertedThreshold != null && lastAlertedThreshold <= 0 ? null : 0;
        }
        Integer reached = null;
        for (Integer threshold : thresholds) {
            if (daysLeft <= threshold && (reached == null || threshold < reached)) {
                reached = threshold;
            }
        }
        if (reached == null) {
            return null;
        }
        if (lastAlertedThreshold != null && lastAlertedThreshold <= reached) {
            return null;
        }
        return reached;
    }

    /**
     * Alert state persisted per cluster as {@code <expiry epoch millis>:<smallest threshold alerted>}, so a renewal,
     * which changes the expiry, starts the thresholds over.
     * @return the threshold already alerted for this expiry, or null
     */
    public static Integer getLastAlertedThreshold(String alertState, Date expiry) {
        if (StringUtils.isBlank(alertState) || expiry == null) {
            return null;
        }
        String[] parts = alertState.split(":");
        if (parts.length != 2) {
            return null;
        }
        try {
            if (Math.abs(Long.parseLong(parts[0]) - expiry.getTime()) > SAME_EXPIRY_TOLERANCE_MS) {
                return null;
            }
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String buildAlertState(Date expiry, int threshold) {
        return String.format("%d:%d", expiry.getTime(), threshold);
    }

    /** @param autoRenewDays the configured value, capped at {@link #MAX_AUTO_RENEW_DAYS} */
    public static boolean isAutoRenewDue(long daysLeft, int autoRenewDays) {
        return autoRenewDays > 0 && daysLeft <= Math.min(autoRenewDays, MAX_AUTO_RENEW_DAYS);
    }
}
