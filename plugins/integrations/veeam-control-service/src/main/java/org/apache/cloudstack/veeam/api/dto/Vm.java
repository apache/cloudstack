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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * VM DTO intentionally uses snake_case field names to match the required JSON.
 * Configure Jackson globally with SNAKE_CASE or keep as-is.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "vm")
public final class Vm extends BaseDto {
    private String name;
    private String description;
    private String status;        // "up", "down", ...
    private String stopReason;   // empty string allowed
    private Long creationTime;
    private Long stopTime;       // epoch millis
    private Long startTime;       // epoch millis
    private Ref template;
    private Ref originalTemplate;
    private Ref cluster;
    private Ref host;
    private String memory;          // bytes
    private Cpu cpu;
    private Os os;
    private Bios bios;
    private String stateless;  // true|false
    private String type;    // "server"
    private String origin;  // "ovirt"
    private Actions actions;      // actions.link[]
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;      // related resources
    private EmptyElement tags; // empty <tags/>
    private DiskAttachments diskAttachments;
    private Nics nics;
    private VmInitialization initialization;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Ref getTemplate() {
        return template;
    }

    public void setTemplate(Ref template) {
        this.template = template;
    }

    public Ref getOriginalTemplate() {
        return originalTemplate;
    }

    public void setOriginalTemplate(Ref originalTemplate) {
        this.originalTemplate = originalTemplate;
    }

    public Ref getCluster() {
        return cluster;
    }

    public void setCluster(Ref cluster) {
        this.cluster = cluster;
    }

    public Ref getHost() {
        return host;
    }

    public void setHost(Ref host) {
        this.host = host;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    public Os getOs() {
        return os;
    }

    public void setOs(Os os) {
        this.os = os;
    }

    public Bios getBios() {
        return bios;
    }

    public void setBios(Bios bios) {
        this.bios = bios;
    }

    public String getStateless() {
        return stateless;
    }

    public void setStateless(String stateless) {
        this.stateless = stateless;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Actions getActions() {
        return actions;
    }

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public EmptyElement getTags() {
        return tags;
    }

    public void setTags(EmptyElement tags) {
        this.tags = tags;
    }

    public DiskAttachments getDiskAttachments() {
        return diskAttachments;
    }

    public void setDiskAttachments(DiskAttachments diskAttachments) {
        this.diskAttachments = diskAttachments;
    }

    public Nics getNics() {
        return nics;
    }

    public void setNics(Nics nics) {
        this.nics = nics;
    }

    public VmInitialization getInitialization() {
        return initialization;
    }

    public void setInitialization(VmInitialization initialization) {
        this.initialization = initialization;
    }

    public static Vm of(String href, String id) {
        Vm vm = new Vm();
        vm.setHref(href);
        vm.setId(id);
        return vm;
    }
}
