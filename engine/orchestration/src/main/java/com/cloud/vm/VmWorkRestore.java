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
package com.cloud.vm;

import java.util.Map;

public class VmWorkRestore extends VmWork {
    private static final long serialVersionUID = 195901782359759635L;

    private Long templateId;
    private Long rootDiskOfferingId;
    private Map<String,String> details;

    private boolean expunge;

    public VmWorkRestore(VmWork vmWork, Long templateId, Long rootDiskOfferingId, boolean expunge, Map<String,String> details) {
        super(vmWork);
        this.templateId = templateId;
        this.rootDiskOfferingId = rootDiskOfferingId;
        this.expunge = expunge;
        this.details = details;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getRootDiskOfferingId() {
        return rootDiskOfferingId;
    }

    public boolean getExpunge() {
        return expunge;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
