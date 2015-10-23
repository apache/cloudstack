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

package org.apache.cloudstack.utils.graphite;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class GraphiteClient {

    private String graphiteHost;
    private int graphitePort;

    /**
     * Create a new Graphite client
     *
     * @param graphiteHost Hostname of the Graphite host
     * @param graphitePort UDP port of the Graphite host
     */
    public GraphiteClient(String graphiteHost, int graphitePort) {
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
    }

    /**
     * Create a new Graphite client
     *
     * @param graphiteHost Hostname of the Graphite host. Will default to port 2003
     */
    public GraphiteClient(String graphiteHost) {
        this.graphiteHost = graphiteHost;
        graphitePort = 2003;
    }

    /**
     * Get the current system timestamp to pass to Graphite
     *
     * @return Seconds passed since epoch (01-01-1970)
     */
    protected long getCurrentSystemTime() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Send a array of metrics to graphite.
     *
     * @param metrics the metrics as key-value-pairs
     */
    public void sendMetrics(Map<String, Integer> metrics) {
        sendMetrics(metrics, getCurrentSystemTime());
    }

    /**
     * Send a array of metrics with a given timestamp to graphite.
     *
     * @param metrics the metrics as key-value-pairs
     * @param timeStamp the timestamp
     */
    public void sendMetrics(Map<String, Integer> metrics, long timeStamp) {
        try (DatagramSocket sock = new DatagramSocket()){
            java.security.Security.setProperty("networkaddress.cache.ttl", "0");
            InetAddress addr = InetAddress.getByName(this.graphiteHost);

            for (Map.Entry<String, Integer> metric: metrics.entrySet()) {
                byte[] message = new String(metric.getKey() + " " + metric.getValue() + " " + timeStamp + "\n").getBytes();
                DatagramPacket packet = new DatagramPacket(message, message.length, addr, graphitePort);
                sock.send(packet);
            }
        } catch (UnknownHostException e) {
            throw new GraphiteException("Unknown host: " + graphiteHost);
        } catch (IOException e) {
            throw new GraphiteException("Error while writing to graphite: " + e.getMessage(), e);
        }
    }

    /**
     * Send a single metric with the current time as timestamp to graphite.
     *
     * @param key The metric key
     * @param value the metric value
     *
     * @throws GraphiteException if sending data to graphite failed
     */
    public void sendMetric(String key, int value) {
        sendMetric(key, value, getCurrentSystemTime());
    }

    /**
     * Send a single metric with a given timestamp to graphite.
     *
     * @param key The metric key
     * @param value The metric value
     * @param timeStamp the timestamp to use
     *
     * @throws GraphiteException if sending data to graphite failed
     */
    public void sendMetric(final String key, final int value, long timeStamp) {
        HashMap metrics = new HashMap<String, Integer>();
        metrics.put(key, value);
        sendMetrics(metrics, timeStamp);
    }
}
