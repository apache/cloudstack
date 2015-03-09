/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.xenserver.resource;

import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Local(value=ServerResource.class)
public class XenServer650Resource extends Xenserver625Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer650Resource.class);

    public XenServer650Resource() {
        super();
    }

    protected List<File> getPatchFiles() {
        List files = new ArrayList();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver65/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }
}
