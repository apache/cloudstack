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

package org.apache.cloudstack.command;

import com.cloud.agent.api.to.DataTO;

import java.util.Map;

public class ReconcileCopyCommand extends ReconcileCommand {

    DataTO srcData;
    DataTO destData;
    Map<String, String> option;     // details of source volume
    Map<String, String> option2;    // details of destination volume

    public ReconcileCopyCommand(DataTO srcData, DataTO destData, Map<String, String> option, Map<String, String> option2) {
        this.srcData = srcData;
        this.destData = destData;
        this.option = option;
        this.option2 = option2;
    }

    public DataTO getSrcData() {
        return srcData;
    }

    public DataTO getDestData() {
        return destData;
    }

    public Map<String, String> getOption() {
        return option;
    }

    public Map<String, String> getOption2() {
        return option2;
    }
}
