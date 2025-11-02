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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.utils.security.HMACSignUtil;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;

/**
 * HMAC token check: /share/...?...&exp=1699999999&sig=BASE64URL(HMACSHA256(path|exp))
 */
public class ShareSignedUrlFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        HttpServletResponse w = (HttpServletResponse) res;
        String secret = ServerPropertiesUtil.getShareSecret();
        if (StringUtils.isBlank(secret)) {
            chain.doFilter(req, res);
            return;
        }

        String expStr = r.getParameter("exp");
        String sig = r.getParameter("sig");

        if (expStr == null || sig == null) {
            w.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing token");
            return;
        }
        long exp;
        try {
            exp = Long.parseLong(expStr);
        } catch (NumberFormatException e) {
            w.sendError(HttpServletResponse.SC_FORBIDDEN, "Bad exp");
            return;
        }
        if (Instant.now().getEpochSecond() > exp) {
            w.sendError(HttpServletResponse.SC_FORBIDDEN, "Token expired");
            return;
        }
        String want = "";
        try {
            String data = r.getRequestURI() + "|" + expStr;
            want = HMACSignUtil.generateSignature(data, secret);
        } catch (InvalidKeyException | NoSuchAlgorithmException | DecoderException e) {
            w.sendError(HttpServletResponse.SC_FORBIDDEN, "Auth error");
            return;
        }
        if (!want.equals(sig)) {
            w.sendError(HttpServletResponse.SC_FORBIDDEN, "Bad signature");
            return;
        }
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}
