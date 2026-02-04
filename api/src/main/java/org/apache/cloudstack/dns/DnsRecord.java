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

package org.apache.cloudstack.dns;

import com.cloud.utils.exception.CloudRuntimeException;

public class DnsRecord {

    public enum RecordType {
        A, AAAA, CNAME, MX, TXT, SRV, PTR, NS;

        public static RecordType fromString(String type) {
            if (type == null) return null;
            try {
                return RecordType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CloudRuntimeException("Invalid DNS Record Type: " + type +
                        ". Supported: " + java.util.Arrays.toString(values()));
            }
        }
    }

    private String name;
    private RecordType type; // Enforced Enum here
    private String content;
    private int ttl;

    public DnsRecord() {}

    public DnsRecord(String name, RecordType type, String content, int ttl) {
        this.name = name;
        this.type = type;
        this.content = content;
        this.ttl = ttl;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RecordType getType() { return type; }
    public void setType(RecordType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }
}
