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
package com.cloud.agent.properties;

/**
 * Class of constant agent's properties available to configure on
 * "agent.properties".
 *<br><br>
 * Not all available agent properties are defined here, but we should work to
 * migrate them on demand to this class.
 *
 * @param <T> type of the default value.
 */
public class AgentProperties{

    /**
     * Heartbeat update timeout. <br>
     * Data type: int. <br>
     * Default value: 60000 (ms).
     */
    public static final Property<Integer> HEARTBEAT_UPDATE_TIMEOUT = new Property<Integer>("heartbeat.update.timeout", 60000);

    /**
     * The timeout in seconds to retrieve the target's domain id when migrating a VM with KVM. <br>
     * Data type: int. <br>
     * Default value: 10 (sec).
     */
    public static final Property<Integer> VM_MIGRATE_DOMAIN_RETRIEVE_TIMEOUT = new Property<Integer>("vm.migrate.domain.retrieve.timeout", 10);

    /**
     * Reboot host and alert management on heartbeat timeout. <br>
     * Data type: boolean.<br>
     * Default value: true.
     */
    public static final Property<Boolean> REBOOT_HOST_AND_ALERT_MANAGEMENT_ON_HEARTBEAT_TIMEOUT
        = new Property<Boolean>("reboot.host.and.alert.management.on.heartbeat.timeout", true);

    /**
     * Enable manually setting CPU's topology on KVM's VM. <br>
     * Data type: boolean.<br>
     * Default value: true.
     */
    public static final Property<Boolean> ENABLE_MANUALLY_SETTING_CPU_TOPOLOGY_ON_KVM_VM = new Property<Boolean>("enable.manually.setting.cpu.topology.on.kvm.vm", true);

    public static class Property <T>{
        private final String name;
        private final T defaultValue;

        private Property(String name, T value) {
            this.name = name;
            this.defaultValue = value;
        }

        public String getName() {
            return name;
        }

        public T getDefaultValue() {
            return defaultValue;
        }
    }
}
