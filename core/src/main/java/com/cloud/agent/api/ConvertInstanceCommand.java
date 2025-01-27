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
package com.cloud.agent.api;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.hypervisor.Hypervisor;

import java.util.List;

public class ConvertInstanceCommand extends Command {

    private RemoteInstanceTO sourceInstance;
    private Hypervisor.HypervisorType destinationHypervisorType;
    private List<String> destinationStoragePools;
    private DataStoreTO conversionTemporaryLocation;
    private String templateDirOnConversionLocation;
    private boolean checkConversionSupport;
    private boolean exportOvfToConversionLocation;
    private int threadsCountToExportOvf = 0;

    public ConvertInstanceCommand() {
    }

    public ConvertInstanceCommand(RemoteInstanceTO sourceInstance, Hypervisor.HypervisorType destinationHypervisorType,
                                  List<String> destinationStoragePools, DataStoreTO conversionTemporaryLocation,
                                  String templateDirOnConversionLocation, boolean checkConversionSupport, boolean exportOvfToConversionLocation) {
        this.sourceInstance = sourceInstance;
        this.destinationHypervisorType = destinationHypervisorType;
        this.destinationStoragePools = destinationStoragePools;
        this.conversionTemporaryLocation = conversionTemporaryLocation;
        this.templateDirOnConversionLocation = templateDirOnConversionLocation;
        this.checkConversionSupport = checkConversionSupport;
        this.exportOvfToConversionLocation = exportOvfToConversionLocation;
    }

    public RemoteInstanceTO getSourceInstance() {
        return sourceInstance;
    }

    public Hypervisor.HypervisorType getDestinationHypervisorType() {
        return destinationHypervisorType;
    }

    public List<String> getDestinationStoragePools() {
        return destinationStoragePools;
    }

    public DataStoreTO getConversionTemporaryLocation() {
        return conversionTemporaryLocation;
    }

    public String getTemplateDirOnConversionLocation() {
        return templateDirOnConversionLocation;
    }

    public boolean getCheckConversionSupport() {
        return checkConversionSupport;
    }

    public boolean getExportOvfToConversionLocation() {
        return exportOvfToConversionLocation;
    }

    public int getThreadsCountToExportOvf() {
        return threadsCountToExportOvf;
    }

    public void setThreadsCountToExportOvf(int threadsCountToExportOvf) {
        this.threadsCountToExportOvf = threadsCountToExportOvf;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
