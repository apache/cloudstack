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
package org.apache.cloudstack.network.tungsten.dao;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "tungsten_lb_health_monitor")
@PrimaryKeyJoinColumn(name = "load_balancer_id", referencedColumnName = "id")
public class TungstenFabricLBHealthMonitorVO implements InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "load_balancer_id")
    private long loadBalancerId;

    @Column(name = "uuid")
    private final String uuid;

    @Column(name = "type")
    private String type;

    @Column(name = "retry")
    private int retry;

    @Column(name = "timeout")
    private int timeout;

    @Column(name = "interval")
    private int interval;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "expected_code")
    private String expectedCode;

    @Column(name = "url_path")
    private String urlPath;

    public TungstenFabricLBHealthMonitorVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public TungstenFabricLBHealthMonitorVO(final long loadBalancerId, final String type, final int retry, final int timeout,
        final int interval, final String httpMethod, final String expectedCode, final String urlPath) {
        this.loadBalancerId = loadBalancerId;
        this.uuid = UUID.randomUUID().toString();
        this.type = type;
        this.retry = retry;
        this.timeout = timeout;
        this.interval = interval;
        this.httpMethod = httpMethod;
        this.expectedCode = expectedCode;
        this.urlPath = urlPath;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    public void setLoadBalancerId(final long loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(final int retry) {
        this.retry = retry;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(final int interval) {
        this.interval = interval;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getExpectedCode() {
        return expectedCode;
    }

    public void setExpectedCode(final String expectedCode) {
        this.expectedCode = expectedCode;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(final String urlPath) {
        this.urlPath = urlPath;
    }
}
