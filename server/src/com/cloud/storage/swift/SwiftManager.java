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
package com.cloud.storage.swift;

import java.util.List;

import com.cloud.agent.api.to.SwiftTO;
import org.apache.cloudstack.api.command.admin.swift.AddSwiftCmd;
import org.apache.cloudstack.api.command.admin.swift.ListSwiftsCmd;
import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.template.DeleteTemplateCmd;
import com.cloud.exception.DiscoveryException;
import com.cloud.storage.Swift;
import com.cloud.storage.SwiftVO;
import com.cloud.storage.VMTemplateSwiftVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
public interface SwiftManager extends Manager {

    SwiftTO getSwiftTO(Long swiftId);

    SwiftTO getSwiftTO();

    Swift addSwift(AddSwiftCmd cmd) throws DiscoveryException;

    boolean isSwiftEnabled();

    public boolean isTemplateInstalled(Long templateId);

    void deleteIso(DeleteIsoCmd cmd);

    void deleteTemplate(DeleteTemplateCmd cmd);

    void propagateTemplateOnAllZones(Long tmpltId);

    void propagateSwiftTmplteOnZone(Long zoneId);

    Long chooseZoneForTmpltExtract(Long tmpltId);

    Pair<List<SwiftVO>, Integer> listSwifts(ListSwiftsCmd cmd);

    VMTemplateSwiftVO findByTmpltId(Long tmpltId);
}
