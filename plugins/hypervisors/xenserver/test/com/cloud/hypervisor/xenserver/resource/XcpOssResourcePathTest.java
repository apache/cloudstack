/*
 * Copyright 2015 The Apache Software Foundation.
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
package com.cloud.hypervisor.xenserver.resource;

import org.junit.Assert;
import org.junit.Test;

public class XcpOssResourcePathTest {

    private XcpOssResource xcpOssResource = new XcpOssResource();

    @Test
    public void testPatchFilePath() {
        String patchFilePath = xcpOssResource.getPatchFilePath();
        String patch = "scripts/vm/hypervisor/xenserver/xcposs/patch";

        Assert.assertEquals(patch, patchFilePath);
    }
}
