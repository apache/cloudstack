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
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.utils.EnumUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private MemoryPolicy memoryPolicy;
    private Cpu cpu;
    private Os os;
    private Bios bios;
    private String stateless;  // true|false
    private String type;    // "server"
    private String origin;  // "ovirt"
    private NamedList<Link> actions;      // actions.link[]
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;
    private NamedList<Tag> tags;
    private NamedList<DiskAttachment> diskAttachments;
    private NamedList<Nic> nics;
    private Initialization initialization;

    private Ref cpuProfile;

    public EmptyElement io = new EmptyElement();
    public EmptyElement migration = new EmptyElement();
    public EmptyElement sso = new EmptyElement();
    public EmptyElement usb = new EmptyElement();
    public EmptyElement quota = new EmptyElement();
    public EmptyElement highAvailability = new EmptyElement();
    public EmptyElement largeIcon = new EmptyElement();
    public EmptyElement smallIcon = new EmptyElement();
    public EmptyElement placementPolicy = new EmptyElement();
    public EmptyElement timeZone = new EmptyElement();
    public EmptyElement display = new EmptyElement();

    // CloudStack-specific fields
    private String accountId;
    private String affinityGroupId;
    private String userDataId;
    private Map<String, String> details;

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

    public MemoryPolicy getMemoryPolicy() {
        return memoryPolicy;
    }

    public void setMemoryPolicy(MemoryPolicy memoryPolicy) {
        this.memoryPolicy = memoryPolicy;
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

    public NamedList<Link> getActions() {
        return actions;
    }

    public void setActions(NamedList<Link> actions) {
        this.actions = actions;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public NamedList<Tag> getTags() {
        return tags;
    }

    public void setTags(NamedList<Tag> tags) {
        this.tags = tags;
    }

    public NamedList<DiskAttachment> getDiskAttachments() {
        return diskAttachments;
    }

    public void setDiskAttachments(NamedList<DiskAttachment> diskAttachments) {
        this.diskAttachments = diskAttachments;
    }

    public NamedList<Nic> getNics() {
        return nics;
    }

    public void setNics(NamedList<Nic> nics) {
        this.nics = nics;
    }

    public Initialization getInitialization() {
        return initialization;
    }

    public void setInitialization(Initialization initialization) {
        this.initialization = initialization;
    }

    public Ref getCpuProfile() {
        return cpuProfile;
    }

    public void setCpuProfile(Ref cpuProfile) {
        this.cpuProfile = cpuProfile;
    }

    @JsonIgnore
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @JsonIgnore
    public String getAffinityGroupId() {
        return affinityGroupId;
    }

    public void setAffinityGroupId(String affinityGroupId) {
        this.affinityGroupId = affinityGroupId;
    }

    @JsonIgnore
    public String getUserDataId() {
        return userDataId;
    }

    public void setUserDataId(String userDataId) {
        this.userDataId = userDataId;
    }

    @JsonIgnore
    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Bios {

        public enum Type {
            cluster_default,
            i440fx_sea_bios,
            q35_ovmf,
            q35_sea_bios,
            q35_secure_boot
        }

        private String type; // "uefi" or "bios" or whatever mapping you choose
        private BootMenu bootMenu = new BootMenu();

        public String getType() {
            return type;
        }

        @JsonIgnore
        public int getTypeOrdinal() {
            Type enumType = EnumUtils.fromString(Type.class, type, Type.q35_sea_bios);
            return enumType.ordinal();
        }

        public void setType(String type) {
            this.type = type;
        }

        public BootMenu getBootMenu() {
            return bootMenu;
        }

        public void setBootMenu(BootMenu bootMenu) {
            this.bootMenu = bootMenu;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class BootMenu {

            private String enabled;

            public String getEnabled() {
                return enabled;
            }

            public void setEnabled(String enabled) {
                this.enabled = enabled;
            }
        }

        public static Bios getDefault() {
            Bios bios = new Bios();
            bios.setType(Type.q35_sea_bios.name());
            BootMenu bootMenu = new BootMenu();
            bootMenu.setEnabled("false");
            bios.setBootMenu(bootMenu);
            return bios;
        }

        public static void updateBios(Bios bios, String bootMode) {
            if (StringUtils.isEmpty(bootMode)) {
                return;
            }
            if (ApiConstants.BootMode.SECURE.toString().equals(bootMode)) {
                bios.setType(Type.q35_secure_boot.name());
                return;
            }
            bios.setType(Type.q35_ovmf.name());
        }

        public static Bios getBiosFromOrdinal(String bootTypeStr) {
            Bios bios = getDefault();
            if (StringUtils.isEmpty(bootTypeStr)) {
                return bios;
            }
            int  type = 1;
            try {
                type = Integer.parseInt(bootTypeStr);
            } catch (NumberFormatException e) {
                return bios;
            }

            if (type == Type.q35_ovmf.ordinal()) {
                bios.setType(Type.q35_ovmf.name());
            } else if (type == Type.q35_secure_boot.ordinal()) {
                 bios.setType(Type.q35_secure_boot.name());
            }
            return bios;
        }

        public static Pair<ApiConstants.BootType, ApiConstants.BootMode> retrieveBootOptions(Bios bios) {
            Pair<ApiConstants.BootType, ApiConstants.BootMode> defaultValue =
                    new Pair<>(ApiConstants.BootType.BIOS, ApiConstants.BootMode.LEGACY);
            if (bios == null || StringUtils.isEmpty(bios.getType())) {
                return defaultValue;
            }
            if (Type.q35_secure_boot.name().equals(bios.getType())) {
                return new Pair<>(ApiConstants.BootType.UEFI, ApiConstants.BootMode.SECURE);
            }
            if (Type.q35_ovmf.name().equals(bios.getType())) {
                return new Pair<>(ApiConstants.BootType.UEFI, ApiConstants.BootMode.LEGACY);
            }
            return defaultValue;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class MemoryPolicy {

        private String guaranteed;
        private String max;
        private String ballooning;

        public String getGuaranteed() {
            return guaranteed;
        }

        public void setGuaranteed(String guaranteed) {
            this.guaranteed = guaranteed;
        }

        public String getMax() {
            return max;
        }

        public void setMax(String max) {
            this.max = max;
        }

        public String getBallooning() {
            return ballooning;
        }

        public void setBallooning(String ballooning) {
            this.ballooning = ballooning;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Initialization {

        private String customScript;
        private Configuration configuration;

        public String getCustomScript() {
            return customScript;
        }

        public void setCustomScript(String customScript) {
            this.customScript = customScript;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Configuration {

            private String data;
            private String type;

            public String getData() {
                return data;
            }

            public void setData(String data) {
                this.data = data;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }
    }

    public static Vm of(String href, String id) {
        return withHrefAndId(new Vm(), href, id);
    }
}
