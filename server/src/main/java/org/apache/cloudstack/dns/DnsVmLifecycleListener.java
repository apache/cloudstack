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

package org.apache.cloudstack.dns;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBus;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.framework.events.EventSubscriber;
import org.apache.cloudstack.framework.events.EventTopic;
import org.springframework.stereotype.Component;

import com.cloud.event.EventTypes;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.Nic;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DnsVmLifecycleListener extends ManagerBase implements EventSubscriber {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    private EventBus eventBus = null;

    @Inject
    VMInstanceDao vmInstanceDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    NicDao nicDao;
    @Inject
    DnsProviderManager providerManager;
    @Inject
    NicDetailsDao nicDetailsDao;

    @Override
    public boolean configure(final String name, final Map<String, Object> params) {
        if (eventBus == null) {
            logger.info("EventBus is not available; DNS Instance lifecycle listener will not subscribe to events");
            return true;
        }
        try {
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_VM_CREATE, null, null, null), this);
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_VM_START, null, null, null), this);
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_VM_STOP, null, null, null), this);
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_VM_DESTROY, null, null, null), this);
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_NIC_CREATE, null, null, null), this);
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_NIC_DELETE, null, null, null), this);
            eventBus.subscribe(new EventTopic(null, EventTypes.EVENT_DNS_RECORD_DELETE, null, null, null), this);
        } catch (EventBusException ex) {
            logger.error("Failed to subscribe DnsVmLifecycleListener to EventBus", ex);
        }
        return true;
    }

    @Override
    public void onEvent(Event event) {
        JsonNode descJson = parseEventDescription(event);
        if (!isEventCompleted(descJson)) {
            return;
        }

        String eventType = event.getEventType();
        String resourceUuid = event.getResourceUUID();
        try {
            switch (eventType) {
                case EventTypes.EVENT_VM_CREATE:
                case EventTypes.EVENT_VM_START:
                    handleVmEvent(resourceUuid, true);
                    break;
                case EventTypes.EVENT_VM_STOP:
                case EventTypes.EVENT_VM_DESTROY:
                    handleVmEvent(resourceUuid, false);
                    break;
                case EventTypes.EVENT_NIC_CREATE:
                    handleNicEvent(descJson, true);
                    break;
                case EventTypes.EVENT_NIC_DELETE:
                    handleNicEvent(descJson, false);
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            logger.error("Failed to process DNS lifecycle event: type={}, resourceUuid={}",
                    eventType, event.getResourceUUID(), ex);
        }
    }

    private void handleNicEvent(JsonNode eventDesc, boolean isAddDnsRecord) {
        JsonNode nicUuid = eventDesc.get("Nic");
        JsonNode vmUuid = eventDesc.get("VirtualMachine");
        if (nicUuid == null || nicUuid.isNull() || vmUuid == null || vmUuid.isNull()) {
            logger.warn("Event has missing data to work on: {}", eventDesc);
            return;
        }
        VMInstanceVO vmInstanceVO = vmInstanceDao.findByUuid(vmUuid.asText());
        if (vmInstanceVO == null) {
            logger.error("Unable to find Instance with ID: {}", vmUuid);
            return;
        }
        Nic nic = nicDao.findByUuidIncludingRemoved(nicUuid.asText());
        if (nic == null) {
            logger.error("NIC is not found for the ID: {}", nicUuid);
            return;
        }
        Network network = networkDao.findById(nic.getNetworkId());
        if (network == null || !Network.GuestType.Shared.equals(network.getGuestType())) {
            logger.warn("Network is not eligible for DNS record registration");
            return;
        }
        processEventForDnsRecord(vmInstanceVO, network, nic, isAddDnsRecord);
    }

    private void handleVmEvent(String vmUuid, boolean isAddDnsRecord) {
        VMInstanceVO vmInstanceVO = vmInstanceDao.findByUuid(vmUuid);
        if (vmInstanceVO == null) {
            logger.error("Unable to find Instance with ID: {}", vmUuid);
            return;
        }
        List<NicVO> vmNics = nicDao.listByVmId(vmInstanceVO.getId());
        for (NicVO nic : vmNics) {
            Network network = networkDao.findById(nic.getNetworkId());
            if (network == null || !Network.GuestType.Shared.equals(network.getGuestType())) {
                continue;
            }
            processEventForDnsRecord(vmInstanceVO, network, nic, isAddDnsRecord);
        }
    }

    void processEventForDnsRecord(VMInstanceVO vmInstanceVO, Network network, Nic nic, boolean isAddDnsRecord) {
        String dnsRecordUrl = providerManager.processDnsRecordForInstance(vmInstanceVO, network, nic, isAddDnsRecord);
        if (dnsRecordUrl != null) {
            if (isAddDnsRecord) {
                nicDetailsDao.addDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD, dnsRecordUrl, true);
            } else {
                nicDetailsDao.removeDetail(nic.getId(), ApiConstants.NIC_DNS_RECORD);
            }
        } else {
            logger.error("Failure {} DNS record for Instance: {} for Network with ID: {}",
                    isAddDnsRecord ? "adding" : "removing", vmInstanceVO.getUuid(), network.getUuid());
        }
    }

    private JsonNode parseEventDescription(Event event) {
        String rawDescription = event.getDescription();
        if (StringUtils.isBlank(rawDescription)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(rawDescription);
        } catch (Exception ex) {
            logger.warn("parseEventDescription: failed to parse description for event [{}]: {}",
                    event.getEventType(), ex.getMessage());
            return null;
        }
    }

    private boolean isEventCompleted(JsonNode descJson) {
        if (descJson == null) {
            return false;
        }
        JsonNode statusNode = descJson.get(ApiConstants.STATUS);
        if (statusNode == null || statusNode.isNull()) {
            return false;
        }

        logger.debug("Processing Event: {}", descJson);
        return ApiConstants.COMPLETED.equalsIgnoreCase(statusNode.asText());
    }
}
