/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.hypervisor.kvm.resource;

import com.cloud.resource.AgentStatusUpdater;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.libvirt.event.DomainEvent;
import org.libvirt.event.DomainEventDetail;
import org.libvirt.event.LifecycleListener;
import org.libvirt.event.StoppedDetail;

public class LibvirtDomainListener implements LifecycleListener {
    private static final Logger LOGGER = LogManager.getLogger(LibvirtDomainListener.class);

    private final AgentStatusUpdater agentStatusUpdater;

    public LibvirtDomainListener(AgentStatusUpdater updater) {
        agentStatusUpdater = updater;
    }

    public int onLifecycleChange(Domain domain, DomainEvent domainEvent) {
        try {
            LOGGER.debug(String.format("Got event lifecycle change on Domain %s, event %s", domain.getName(), domainEvent));
            if (domainEvent != null) {
                switch (domainEvent.getType()) {
                    case STOPPED:
                        /* libvirt-destroyed VMs have detail StoppedDetail.DESTROYED, self shutdown guests are StoppedDetail.SHUTDOWN
                         * Checking for this helps us differentiate between events where cloudstack or admin stopped the VM vs guest
                         * initiated, and avoid pushing extra updates for actions we are initiating without a need for extra tracking */
                        DomainEventDetail detail = domainEvent.getDetail();
                        if (StoppedDetail.SHUTDOWN.equals(detail) || StoppedDetail.CRASHED.equals(detail) || StoppedDetail.FAILED.equals(detail)) {
                            if (agentStatusUpdater != null) {
                                LOGGER.info("Triggering out of band status update due to completed self-shutdown or crash of VM");
                                agentStatusUpdater.triggerUpdate();
                            }
                        } else {
                            LOGGER.debug("Event detail: " + detail);
                        }
                        break;
                    default:
                        LOGGER.debug(String.format("No handling for event %s", domainEvent));
                }
            }
        } catch (LibvirtException e) {
            LOGGER.error("Libvirt exception while processing lifecycle event", e);
        } catch (Throwable e) {
            LOGGER.error("Error during lifecycle", e);
        }
        return 0;
    }
}
