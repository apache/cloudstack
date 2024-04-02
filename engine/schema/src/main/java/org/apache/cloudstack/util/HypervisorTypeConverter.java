// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.util;

import com.cloud.hypervisor.Hypervisor;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Converts {@link com.cloud.hypervisor.Hypervisor.HypervisorType} to and from {@link String} using {@link com.cloud.hypervisor.Hypervisor.HypervisorType#name()}.
 */
@Converter
public class HypervisorTypeConverter implements AttributeConverter<Hypervisor.HypervisorType, String> {
    @Override
    public String convertToDatabaseColumn(Hypervisor.HypervisorType attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public Hypervisor.HypervisorType convertToEntityAttribute(String dbData) {
        return dbData != null ? Hypervisor.HypervisorType.valueOf(dbData) : null;
    }
}
