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

package org.apache.cloudstack.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * A servlet to serve files from a configured share directory.
 * This is used only for local maven run. For production deployments, share context handling is in ServerDaemon.
 * Configuration properties read from server.properties:
 * <ul>
 *   <li>share.enabled - Enable or disable the share servlet.</li>
 *   <li>share.base.dir - The base directory from which files will be served.</li>
 *   <li>share.cache.control - Cache-Control header value for served files.</li>
 * </ul>
 */
@Component("shareServlet")
public class ShareServlet extends HttpServlet {
    private static final Logger LOG = LogManager.getLogger(ShareServlet.class);

    private Path baseDir;
    private String cacheControl;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());

        if (!ServerPropertiesUtil.getShareEnabled()) {
            LOG.info("ShareServlet: share disabled, skipping initialization");
            return;
        }

        try {
            baseDir = Paths.get(ServerPropertiesUtil.getShareBaseDirectory());
            Files.createDirectories(baseDir);
            cacheControl = ServerPropertiesUtil.getShareCacheControl();
            LOG.info("ShareServlet initialized at baseDir={}, cacheControl={}", baseDir, cacheControl);
        } catch (IOException e) {
            throw new ServletException("Failed to initialize ShareServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (baseDir == null) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Share feature disabled");
            return;
        }

        // Resolve relative path safely
        final String relPath = StringUtils.removeStart(req.getPathInfo(), "/");
        if (relPath == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        final Path target = baseDir.resolve(relPath).normalize();
        if (!target.startsWith(baseDir)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Add basic caching headers
        if (StringUtils.isNotBlank(cacheControl)) {
            resp.setHeader("Cache-Control", cacheControl);
        }
        resp.setHeader("Accept-Ranges", "bytes");

        final BasicFileAttributes attrs = Files.readAttributes(target, BasicFileAttributes.class);
        resp.setDateHeader("Last-Modified", attrs.lastModifiedTime().toMillis());
        resp.setContentLengthLong(attrs.size());

        final String mime = Files.probeContentType(target);
        if (mime != null) {
            resp.setContentType(mime);
        }

        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(target, out);
        }
    }
}
