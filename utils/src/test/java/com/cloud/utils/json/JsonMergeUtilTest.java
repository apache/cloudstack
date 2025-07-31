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

package com.cloud.utils.json;

import org.junit.Assert;
import org.junit.Test;

public class JsonMergeUtilTest {

    @Test
    public void testMergePatchIntoObjectJson() {
        String base = "{\n" +
                "  \"id\": 20,\n" +
                "  \"name\": \"i-2-20-QA\",\n" +
                "  \"state\": \"Starting\",\n" +
                "  \"type\": \"User\",\n" +
                "  \"cpus\": 1,\n" +
                "  \"minSpeed\": 500,\n" +
                "  \"maxSpeed\": 500,\n" +
                "  \"minRam\": 536870912,\n" +
                "  \"maxRam\": 536870912,\n" +
                "  \"arch\": \"x86_64\",\n" +
                "  \"bootArgs\": \"\",\n" +
                "  \"enableHA\": false,\n" +
                "  \"limitCpuUse\": false,\n" +
                "  \"enableDynamicallyScaleVm\": false,\n" +
                "  \"vncPassword\": \"qWI23VL8vzAYpeKcdMF_Kw\",\n" +
                "  \"details\": {\n" +
                "    \"deployvm\": \"true\",\n" +
                "    \"External:tempalteid\": \"1\"\n" +
                "  },\n" +
                "  \"uuid\": \"ba4024c3-a2fd-4fb9-af9e-e4a4b2be59dc\",\n" +
                "  \"enterHardwareSetup\": false,\n" +
                "  \"disks\": [\n" +
                "    {\n" +
                "      \"data\": {\n" +
                "        \"org.apache.cloudstack.storage.to.TemplateObjectTO\": {\n" +
                "          \"id\": 0,\n" +
                "          \"format\": \"ISO\",\n" +
                "          \"accountId\": 0,\n" +
                "          \"hvm\": false,\n" +
                "          \"bootable\": false,\n" +
                "          \"directDownload\": false,\n" +
                "          \"deployAsIs\": false,\n" +
                "          \"followRedirects\": false\n" +
                "        }\n" +
                "      },\n" +
                "      \"diskSeq\": 3,\n" +
                "      \"type\": \"ISO\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"nics\": [\n" +
                "    {\n" +
                "      \"deviceId\": 0,\n" +
                "      \"networkRateMbps\": 200,\n" +
                "      \"defaultNic\": true,\n" +
                "      \"pxeDisable\": false,\n" +
                "      \"nicUuid\": \"f3188e9f-37ba-4853-b711-d6f60dddc5a2\",\n" +
                "      \"details\": {\n" +
                "        \"MacLearning\": \"false\",\n" +
                "        \"ForgedTransmits\": \"true\",\n" +
                "        \"PromiscuousMode\": \"false\",\n" +
                "        \"MacAddressChanges\": \"true\"\n" +
                "      },\n" +
                "      \"dpdkEnabled\": false,\n" +
                "      \"networkId\": 208,\n" +
                "      \"networkSegmentName\": \"D1-A2-Z1-S208\",\n" +
                "      \"uuid\": \"e754f4e2-5a1c-4686-995e-1d2eb1235eb4\",\n" +
                "      \"mac\": \"02:01:00:d0:00:10\",\n" +
                "      \"dns1\": \"10.147.28.6\",\n" +
                "      \"broadcastType\": \"Vlan\",\n" +
                "      \"type\": \"Guest\",\n" +
                "      \"broadcastUri\": \"vlan://166\",\n" +
                "      \"isolationUri\": \"vlan://166\",\n" +
                "      \"securityGroupEnabled\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"configDriveLocation\": \"SECONDARY\",\n" +
                "  \"guestOsDetails\": {},\n" +
                "  \"extraConfig\": {},\n" +
                "  \"networkIdToNetworkNameMap\": {\n" +
                "    \"208\": \"D1-A2-Z1-S208\"\n" +
                "  },\n" +
                "  \"metadataManufacturer\": \"Apache Software Foundation\",\n" +
                "  \"metadataProductName\": \"CloudStack External Hypervisor\"\n" +
                "}";
        String mac = "52:54:00:30:CE:91";
        String patch = "{\"nics\":[{\"uuid\":\"e754f4e2-5a1c-4686-995e-1d2eb1235eb4\",\"mac\":\"" + mac + "\"}]}";
        String merged = JsonMergeUtil.mergeJsonPatch(base, patch);
        System.out.println(merged);
        Assert.assertTrue(merged.contains(mac));
    }
}
