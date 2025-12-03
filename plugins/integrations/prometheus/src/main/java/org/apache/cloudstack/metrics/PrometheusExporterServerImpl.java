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
package org.apache.cloudstack.metrics;

import com.cloud.utils.component.ManagerBase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PrometheusExporterServerImpl extends ManagerBase implements PrometheusExporterServer, Configurable {

    private static HttpServer httpServer;

    @Inject
    private PrometheusExporter prometheusExporter;

    private final class ExporterHandler implements HttpHandler {
        private PrometheusExporter prometheusExporter;

        ExporterHandler(final PrometheusExporter prometheusExporter) {
            super();
            this.prometheusExporter = prometheusExporter;
        }

        @Override
        public void handle(final HttpExchange httpExchange) throws IOException {
            final String remoteClientAddress = httpExchange.getRemoteAddress().getAddress().toString().replace("/", "");
            logger.debug("Prometheus exporter received client request from: " + remoteClientAddress);
            String response = "Forbidden";
            int responseCode = 403;
            if (Arrays.asList(PrometheusExporterAllowedAddresses.value().split(",")).contains(remoteClientAddress)) {
                prometheusExporter.updateMetrics();
                response = prometheusExporter.getMetrics();
                responseCode = 200;
            }
            byte[] bytesToOutput = response.getBytes(StandardCharsets.UTF_8);
            httpExchange.getResponseHeaders().set("content-type", "text/plain; charset=UTF-8");
            httpExchange.sendResponseHeaders(responseCode, bytesToOutput.length);
            final OutputStream os = httpExchange.getResponseBody();
            try {
                os.write(bytesToOutput);
            } catch (IOException e) {
                logger.error(String.format("could not export Prometheus data due to %s", e.getLocalizedMessage()));
                if (logger.isDebugEnabled()) {
                    logger.debug("Error during Prometheus export: ", e);
                }
                os.write("The system could not export Prometheus due to an internal error. Contact your operator to learn about the reason.".getBytes());
            } finally {
                os.close();
            }
        }
    }

    @Override
    public boolean start() {
        if (EnablePrometheusExporter.value()) {
            try {
                httpServer = HttpServer.create(new InetSocketAddress(PrometheusExporterServerPort.value()), 0);
                httpServer.createContext("/metrics", new ExporterHandler(prometheusExporter));
                httpServer.createContext("/", new HttpHandler() {
                    @Override
                    public void handle(HttpExchange httpExchange) throws IOException {
                        final String response = "<html><head><title>CloudStack Exporter</title></head>" +
                                "<body><h1>CloudStack Exporter</h1>" +
                                "<p><a href=\"/metrics\">Metrics</a></p>" +
                                "</body></html>";
                        httpExchange.sendResponseHeaders(200, response.length());
                        final OutputStream os = httpExchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                });
                httpServer.start();
                logger.debug("Started prometheus exporter http server");
            } catch (final IOException e) {
                logger.info("Failed to start prometheus exporter http server due to: ", e);
            }
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            logger.debug("Stopped Prometheus exporter http server");
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return PrometheusExporter.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                EnablePrometheusExporter,
                PrometheusExporterServerPort,
                PrometheusExporterAllowedAddresses,
                PrometheusExporterOfferingCountLimit
        };
    }
}
