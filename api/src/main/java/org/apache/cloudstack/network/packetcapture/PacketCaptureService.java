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
package org.apache.cloudstack.network.packetcapture;

import org.apache.cloudstack.api.response.PacketCaptureResponse;

public interface PacketCaptureService {

    /**
     * Name of the NIC detail that marks packet capture as enabled on a NIC.
     */
    String PACKET_CAPTURE_NIC_DETAIL = "packetcapture";

    /**
     * Enables packet capture on the NIC. If the VM owning the NIC is running,
     * the capture is started on its host immediately; otherwise it starts the
     * next time the VM starts.
     */
    void enablePacketCapture(long nicId);

    /**
     * Disables packet capture on the NIC and stops a running capture.
     */
    void disablePacketCapture(long nicId);

    /**
     * Returns whether packet capture is enabled on the NIC and whether a
     * capture is currently running on the host of the VM.
     */
    PacketCaptureResponse getPacketCaptureStatus(long nicId);
}
