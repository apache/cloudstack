/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * ExportRule
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportRule {
    @JsonProperty("anonymous_user")
    private String anonymousUser ;

    @JsonProperty("clients")
    private List<ExportClient> clients = null;

    @JsonProperty("index")
    private Integer index = null;

    public enum ProtocolsEnum {
        any("any"),

        nfs("nfs"),

        nfs3("nfs3"),

        nfs4("nfs4");

        private String value;

        ProtocolsEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static ProtocolsEnum fromValue(String text) {
            for (ProtocolsEnum b : ProtocolsEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("protocols")
    private List<ProtocolsEnum> protocols = null;

    public ExportRule anonymousUser(String anonymousUser) {
        this.anonymousUser = anonymousUser;
        return this;
    }

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public void setAnonymousUser(String anonymousUser) {
        this.anonymousUser = anonymousUser;
    }

    public ExportRule clients(List<ExportClient> clients) {
        this.clients = clients;
        return this;
    }

    public List<ExportClient> getClients() {
        return clients;
    }

    public void setClients(List<ExportClient> clients) {
        this.clients = clients;
    }

    public Integer getIndex() {
        return index;
    }
    public void setIndex(Integer index)
    {
        this.index=index;
    }

    public ExportRule protocols(List<ProtocolsEnum> protocols) {
        this.protocols = protocols;
        return this;
    }

    public List<ProtocolsEnum> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolsEnum> protocols) {
        this.protocols = protocols;
    }

    public static class ExportClient {
        @JsonProperty("match")
        private String match = null;

        public ExportClient match (String match) {
            this.match = match;
            return this;
        }
        public String getMatch () {
            return match;
        }

        public void setMatch (String match) {
            this.match = match;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExportRule {\n");

        sb.append("    anonymousUser: ").append(toIndentedString(anonymousUser)).append("\n");
        sb.append("    clients: ").append(toIndentedString(clients)).append("\n");
        sb.append("    index: ").append(toIndentedString(index)).append("\n");
        sb.append("    protocols: ").append(toIndentedString(protocols)).append("\n");
        sb.append("}");
        return sb.toString();
    }
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
