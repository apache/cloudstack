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
package org.apache.cloudstack;

import com.cloud.utils.StringUtils;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.DateCache;
import org.eclipse.jetty.util.component.LifeCycle;

import java.util.Locale;
import java.util.TimeZone;

import static org.apache.commons.configuration.DataConfiguration.DEFAULT_DATE_FORMAT;

public class ACSRequestLog extends NCSARequestLog {
    private static final ThreadLocal<StringBuilder> buffers =
            ThreadLocal.withInitial(() -> new StringBuilder(256));

    private final DateCache dateCache;

    public ACSRequestLog() {
        super();

        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        Locale locale = Locale.getDefault();
        dateCache = new DateCache(DEFAULT_DATE_FORMAT, locale, timeZone);
    }

    @Override
    public void log(Request request, Response response) {
        String requestURI = StringUtils.cleanString(request.getOriginalURI());
        try {
            StringBuilder sb = buffers.get();
            sb.setLength(0);

            sb.append(request.getHttpChannel().getEndPoint()
                            .getRemoteAddress().getAddress()
                            .getHostAddress())
                    .append(" - - [")
                    .append(dateCache.format(request.getTimeStamp()))
                    .append("] \"")
                    .append(request.getMethod())
                    .append(" ")
                    .append(requestURI)
                    .append(" ")
                    .append(request.getProtocol())
                    .append("\" ")
                    .append(response.getStatus())
                    .append(" ")
                    .append(response.getHttpChannel().getBytesWritten()) // apply filter here?
                    .append(" \"-\" \"")
                    .append(request.getHeader("User-Agent"))
                    .append("\"");

            write(sb.toString());
        } catch (Exception e) {
            LOG.warn("Unable to log request", e);
        }
    }

    @Override
    protected void stop(LifeCycle lifeCycle) throws Exception {
        buffers.remove();
        super.stop(lifeCycle);
    }
}
