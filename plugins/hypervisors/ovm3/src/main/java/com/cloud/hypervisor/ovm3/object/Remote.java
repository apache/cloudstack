/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

public class Remote extends OvmObject {

    public Remote(Connection c) {
        setClient(c);
    }

    /*
     * sys_shutdown, <class
     * 'agent.api.remote.linux_remote.LinuxRemoteManagement'> argument: self -
     * default: None
     */
    public Boolean sysShutdown() throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("sys_shutdown");
    }

    /*
     * remote_power_off, <class
     * 'agent.api.remote.linux_remote.LinuxRemoteManagement'> argument: self -
     * default: None argument: controller_type - default: None ?> figure this
     * one out in the source argument: tgt_host - default: None argument:
     * bmc_username - default: None argument: bmc_password - default: None
     */

    /*
     * sys_reboot, <class 'agent.api.remote.linux_remote.LinuxRemoteManagement'>
     * argument: self - default: None
     */
    public Boolean sysReboot() throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("sys_reboot");
    }

    /*
     * remote_power_on, <class
     * 'agent.api.remote.linux_remote.LinuxRemoteManagement'> argument: self -
     * default: None argument: controller_type - default: None ?> same here
     * argument: tgt - default: None argument: arg1 - default: None argument:
     * arg2 - default: None
     */

}
