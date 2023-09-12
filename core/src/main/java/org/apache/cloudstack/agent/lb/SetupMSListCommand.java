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

package org.apache.cloudstack.agent.lb;

import java.util.List;

import com.cloud.agent.api.Command;

public class SetupMSListCommand extends Command {

    private List<String> msList;
    private String lbAlgorithm;
    private Long lbCheckInterval;

    public SetupMSListCommand(final List<String> msList, final String lbAlgorithm, final Long lbCheckInterval) {
        super();
        this.msList = msList;
        this.lbAlgorithm = lbAlgorithm;
        this.lbCheckInterval = lbCheckInterval;
    }

    public List<String> getMsList() {
        return msList;
    }

    public String getLbAlgorithm() {
        return lbAlgorithm;
    }

    public Long getLbCheckInterval() {
        return lbCheckInterval;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
