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

package org.apache.cloudstack.veeam.services;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.component.ManagerBase;

public class PkiResourceRouteHandler extends ManagerBase implements RouteHandler {
    private static final String BASE_ROUTE = "/services/pki-resource";
    private static final String RESOURCE_KEY = "resource";
    private static final String RESOURCE_VALUE = "ca-certificate";
    private static final String FORMAT_KEY = "format";
    private static final String FORMAT_VALUE = "X509-PEM-CA";
    private static final Charset OUTPUT_CHARSET = StandardCharsets.ISO_8859_1;

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE) && "GET".equalsIgnoreCase(req.getMethod())) {
            handleGet(req, resp, outFormat, io);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    protected void handleGet(HttpServletRequest req, HttpServletResponse resp,
                 Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        try {
            final String resource = req.getParameter(RESOURCE_KEY);
            final String format = req.getParameter(FORMAT_KEY);

            if (StringUtils.isNotBlank(resource) && !RESOURCE_VALUE.equals(resource)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported resource");
                return;
            }

            if (StringUtils.isNotBlank(format) && !FORMAT_VALUE.equals(format)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
                return;
            }

            final String keystorePath = ServerPropertiesUtil.getKeystoreFile();
            final String keystorePassword = ServerPropertiesUtil.getKeystorePassword();

            Path path = Path.of(keystorePath);
            if (keystorePath.isBlank() || !Files.exists(path)) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "CloudStack HTTPS keystore not found");
                return;
            }

            final X509Certificate caCert =
                    extractCaFromKeystore(path, keystorePassword);

            // DER encoding → browser downloads as .cer (oVirt behavior)
            final byte[] pemBytes =
                    toPem(caCert).getBytes(OUTPUT_CHARSET);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader("Cache-Control", "no-store");
            resp.setContentType("application/x-x509-ca-cert; charset=" + OUTPUT_CHARSET.name());
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"pki-resource.cer\"");
            resp.setContentLength(pemBytes.length);

            try (OutputStream os = resp.getOutputStream()) {
                os.write(pemBytes);
            }
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            String msg = "Failed to retrieve server CA certificate";
            logger.error(msg, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    private static X509Certificate extractCaFromKeystore(Path ksPath, String ksPassword)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        final String path = ksPath.toString().toLowerCase();
        final String storeType =
                (path.endsWith(".p12") || path.endsWith(".pfx"))
                        ? "PKCS12"
                        : KeyStore.getDefaultType();

        KeyStore ks = KeyStore.getInstance(storeType);
        try (var in = Files.newInputStream(ksPath)) {
            ks.load(in, ksPassword != null ? ksPassword.toCharArray() : new char[0]);
        }

        // Prefer HTTPS keypair alias (one with a chain)
        String alias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String a = aliases.nextElement();
            Certificate[] chain = ks.getCertificateChain(a);
            if (chain != null && chain.length > 0) {
                alias = a;
                break;
            }
        }

        if (alias == null && ks.aliases().hasMoreElements()) {
            alias = ks.aliases().nextElement();
        }

        if (alias == null) {
            throw new IllegalStateException("No certificate aliases in keystore");
        }

        Certificate[] chain = ks.getCertificateChain(alias);
        Certificate cert =
                (chain != null && chain.length > 0)
                        ? chain[chain.length - 1]   // root-most
                        : ks.getCertificate(alias);

        if (!(cert instanceof X509Certificate)) {
            throw new IllegalStateException("Certificate is not X509");
        }

        return (X509Certificate) cert;
    }

    private static String toPem(X509Certificate cert) throws CertificateEncodingException {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(cert.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n"
                + base64
                + "\n-----END CERTIFICATE-----\n";
    }
}
