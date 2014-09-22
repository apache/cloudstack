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
package com.cloud.simulator;

import com.cloud.vm.VirtualMachine.PowerState;

// As storage is mapped from storage device, can virtually treat that VM here does
public interface MockVm {

    public String getName();

    public PowerState getPowerState();

    public void setPowerState(PowerState state);

    public void setHostId(long hostId);

    public long getMemory();

    public int getCpu();

    public String getType();

    public int getVncPort();

    public void setName(String name);

    public void setMemory(long memory);

    public void setCpu(int cpu);

    public void setType(String type);

    public void setVncPort(int vncPort);

    public long getId();

    public String getBootargs();

    public void setBootargs(String bootargs);
}
