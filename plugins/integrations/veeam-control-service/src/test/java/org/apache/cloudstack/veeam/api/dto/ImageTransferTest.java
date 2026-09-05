// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class ImageTransferTest {
    @Test
    public void gettersSetters() {
        ImageTransfer it = new ImageTransfer();
        it.setPhase("initializing");
        it.setDirection("upload");
        it.setFormat("raw");
        it.setTransferUrl("http://host:54322/images/xxx");
        it.setTransferred("0");
        it.setActive("true");
        assertEquals("initializing", it.getPhase());
        assertEquals("upload", it.getDirection());
        assertEquals("raw", it.getFormat());
        assertEquals("http://host:54322/images/xxx", it.getTransferUrl());
        assertEquals("0", it.getTransferred());
        assertEquals("true", it.getActive());
    }

    @Test
    public void hostAndDiskRefs() {
        ImageTransfer it = new ImageTransfer();
        Ref host = Ref.of("/api/hosts/h1", "h1");
        Ref disk = Ref.of("/api/disks/d1", "d1");
        Ref image = Ref.of("/api/images/img1", "img1");
        it.setHost(host);
        it.setDisk(disk);
        it.setImage(image);
        assertEquals("h1", it.getHost().getId());
        assertEquals("d1", it.getDisk().getId());
        assertEquals("img1", it.getImage().getId());
    }

    @Test
    public void json_ContainsPhaseAndDirection() throws Exception {
        Mapper mapper = new Mapper();
        ImageTransfer it = new ImageTransfer();
        it.setPhase("transferring");
        it.setDirection("download");
        String json = mapper.toJson(it);
        assertTrue(json.contains("\"phase\":\"transferring\""));
        assertTrue(json.contains("\"direction\":\"download\""));
    }
}
