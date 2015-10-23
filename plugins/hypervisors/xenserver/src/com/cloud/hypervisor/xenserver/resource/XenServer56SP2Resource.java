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
package com.cloud.hypervisor.xenserver.resource;

import javax.ejb.Local;

import com.cloud.resource.ServerResource;

@Local(value = ServerResource.class)
public class XenServer56SP2Resource extends XenServer56FP1Resource {

    public XenServer56SP2Resource() {
        super();
        _xsMemoryUsed = 128 * 1024 * 1024L;
        _xsVirtualizationFactor = 62.0 / 64.0;
    }
}
