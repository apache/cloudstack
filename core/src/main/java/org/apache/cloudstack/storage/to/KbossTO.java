package org.apache.cloudstack.storage.to;
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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class KbossTO {

    private String pathBackupParentOnSecondary;
    private VolumeObjectTO volumeObjectTO;
    private String deltaPathOnPrimary;
    private String parentDeltaPathOnPrimary;
    private String deltaPathOnSecondary;
    private String oldVolumePath;

    private DeltaMergeTreeTO deltaMergeTreeTO;

    private List<String> deltaPaths;

    public KbossTO(VolumeObjectTO volumeObjectTO, LinkedList<String> deltaPaths) {
        this.volumeObjectTO = volumeObjectTO;
        this.deltaPaths = deltaPaths;
    }

    public KbossTO(VolumeObjectTO volumeObjectTO, String deltaPathOnPrimary, String deltaPathOnSecondary, LinkedList<String> deltaPaths) {
        this.volumeObjectTO = volumeObjectTO;
        this.deltaPathOnPrimary = deltaPathOnPrimary;
        this.deltaPathOnSecondary = deltaPathOnSecondary;
        this.deltaPaths = deltaPaths;
    }

    public String getPathBackupParentOnSecondary() {
        return pathBackupParentOnSecondary;
    }

    public VolumeObjectTO getVolumeObjectTO() {
        return volumeObjectTO;
    }

    public DeltaMergeTreeTO getDeltaMergeTreeTO() {
        return deltaMergeTreeTO;
    }

    public List<String> getDeltaPaths() {
        return deltaPaths;
    }

    public String getDeltaPathOnPrimary() {
        return deltaPathOnPrimary;
    }

    public String getDeltaPathOnSecondary() {
        return deltaPathOnSecondary;
    }

    public String getParentDeltaPathOnPrimary() {
        return parentDeltaPathOnPrimary;
    }

    public void setParentDeltaPathOnPrimary(String parentDeltaPathOnPrimary) {
        this.parentDeltaPathOnPrimary = parentDeltaPathOnPrimary;
    }

    public void setPathBackupParentOnSecondary(String pathBackupParentOnSecondary) {
        this.pathBackupParentOnSecondary = pathBackupParentOnSecondary;
    }

    public void setDeltaMergeTreeTO(DeltaMergeTreeTO deltaMergeTreeTO) {
        this.deltaMergeTreeTO = deltaMergeTreeTO;
    }

    public void setDeltaPathOnPrimary(String deltaPathOnPrimary) {
        this.deltaPathOnPrimary = deltaPathOnPrimary;
    }

    public void setDeltaPathOnSecondary(String deltaPathOnSecondary) {
        this.deltaPathOnSecondary = deltaPathOnSecondary;
    }

    public String getOldVolumePath() {
        return oldVolumePath;
    }

    public void setOldVolumePath(String oldVolumePath) {
        this.oldVolumePath = oldVolumePath;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
