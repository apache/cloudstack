//
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
//

package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.VolumeTO;

public class CreateAnswer extends Answer {
    VolumeTO volume;
    boolean requestTemplateReload = false;

    protected CreateAnswer() {
        super();
    }

    public CreateAnswer(CreateCommand cmd, VolumeTO volume) {
        super(cmd, true, null);
        this.volume = volume;
    }

    public CreateAnswer(CreateCommand cmd, String details) {
        super(cmd, false, details);
    }

    public CreateAnswer(CreateCommand cmd, String details, boolean requestTemplateReload) {
        super(cmd, false, details);
        this.requestTemplateReload = requestTemplateReload;
    }

    public CreateAnswer(CreateCommand cmd, Exception e) {
        super(cmd, e);
    }

    public VolumeTO getVolume() {
        return volume;
    }

    public boolean templateReloadRequested() {
        return requestTemplateReload;
    }
}
