/*
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

/**
 * Provides the XML definition to a memory device which can be hotpluged to the VM.<br/>
 * Memory is provided in KiB.
 *
 */
public class LibvirtVmMemoryDeviceDef {

    private final long memorySize;

    public LibvirtVmMemoryDeviceDef(long memorySize) {
        this.memorySize = memorySize;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder();
        response.append("<memory model='dimm'>");
        response.append("<target>");
        response.append(String.format("<size unit='KiB'>%s</size>", memorySize));
        response.append("<node>0</node>");
        response.append("</target>");
        response.append("</memory>");

        return response.toString();
    }

}
