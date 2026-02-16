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

package org.apache.cloudstack.veeam.api.dto;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageTransfer extends BaseDto {

    private String active;
    private String direction;
    private String format;
    private String inactivityTimeout;
    private String phase;
    private String proxyUrl;
    private String shallow;
    private String timeoutPolicy;
    private String transferUrl;
    private String transferred;
    private Backup backup;
    private Ref host;
    private Ref image;
    private Ref disk;
    private Actions actions;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getInactivityTimeout() {
        return inactivityTimeout;
    }

    public void setInactivityTimeout(String inactivityTimeout) {
        this.inactivityTimeout = inactivityTimeout;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public String getShallow() {
        return shallow;
    }

    public void setShallow(String shallow) {
        this.shallow = shallow;
    }

    public String getTimeoutPolicy() {
        return timeoutPolicy;
    }

    public void setTimeoutPolicy(String timeoutPolicy) {
        this.timeoutPolicy = timeoutPolicy;
    }

    public String getTransferUrl() {
        return transferUrl;
    }

    public void setTransferUrl(String transferUrl) {
        this.transferUrl = transferUrl;
    }

    public String getTransferred() {
        return transferred;
    }

    public void setTransferred(String transferred) {
        this.transferred = transferred;
    }

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    public Ref getHost() {
        return host;
    }

    public void setHost(Ref host) {
        this.host = host;
    }

    public Ref getImage() {
        return image;
    }

    public void setImage(Ref image) {
        this.image = image;
    }

    public Ref getDisk() {
        return disk;
    }

    public void setDisk(Ref disk) {
        this.disk = disk;
    }

    public Actions getActions() {
        return actions;
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }
}
