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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraCpg {
    private long ddsRsvdMiB;
    private String tdvvVersion;
    private PrimeraCpgSAGrowth sAGrowth;
    private PrimeraCpgSAUsage sAUsage;
    private PrimeraCpgSDGrowth sDGrowth;
    private PrimeraCpgSDUsage sDUsage;
    private PrimeraCpgUsrUsage usrUsage;
    private ArrayList<Object> additionalStates;
    private boolean dedupCapable;
    private ArrayList<Object> degradedStates;
    private ArrayList<Object> failedStates;
    private int freeSpaceMiB;
    private String name;
    private int numFPVVs;
    private int numTDVVs;
    private int numTPVVs;
    private PrimeraCpgPrivateSpaceMiB privateSpaceMiB;
    private int rawFreeSpaceMiB;
    private int rawSharedSpaceMiB;
    private int rawTotalSpaceMiB;
    private int sharedSpaceMiB;
    private int state;
    private int totalSpaceMiB;
    private String uuid;
    private int id;
    public long getDdsRsvdMiB() {
        return ddsRsvdMiB;
    }
    public void setDdsRsvdMiB(long ddsRsvdMiB) {
        this.ddsRsvdMiB = ddsRsvdMiB;
    }
    public String getTdvvVersion() {
        return tdvvVersion;
    }
    public void setTdvvVersion(String tdvvVersion) {
        this.tdvvVersion = tdvvVersion;
    }
    public PrimeraCpgSAGrowth getsAGrowth() {
        return sAGrowth;
    }
    public void setsAGrowth(PrimeraCpgSAGrowth sAGrowth) {
        this.sAGrowth = sAGrowth;
    }
    public PrimeraCpgSAUsage getsAUsage() {
        return sAUsage;
    }
    public void setsAUsage(PrimeraCpgSAUsage sAUsage) {
        this.sAUsage = sAUsage;
    }
    public PrimeraCpgSDGrowth getsDGrowth() {
        return sDGrowth;
    }
    public void setsDGrowth(PrimeraCpgSDGrowth sDGrowth) {
        this.sDGrowth = sDGrowth;
    }
    public PrimeraCpgSDUsage getsDUsage() {
        return sDUsage;
    }
    public void setsDUsage(PrimeraCpgSDUsage sDUsage) {
        this.sDUsage = sDUsage;
    }
    public PrimeraCpgUsrUsage getUsrUsage() {
        return usrUsage;
    }
    public void setUsrUsage(PrimeraCpgUsrUsage usrUsage) {
        this.usrUsage = usrUsage;
    }
    public ArrayList<Object> getAdditionalStates() {
        return additionalStates;
    }
    public void setAdditionalStates(ArrayList<Object> additionalStates) {
        this.additionalStates = additionalStates;
    }
    public boolean isDedupCapable() {
        return dedupCapable;
    }
    public void setDedupCapable(boolean dedupCapable) {
        this.dedupCapable = dedupCapable;
    }
    public ArrayList<Object> getDegradedStates() {
        return degradedStates;
    }
    public void setDegradedStates(ArrayList<Object> degradedStates) {
        this.degradedStates = degradedStates;
    }
    public ArrayList<Object> getFailedStates() {
        return failedStates;
    }
    public void setFailedStates(ArrayList<Object> failedStates) {
        this.failedStates = failedStates;
    }
    public int getFreeSpaceMiB() {
        return freeSpaceMiB;
    }
    public void setFreeSpaceMiB(int freeSpaceMiB) {
        this.freeSpaceMiB = freeSpaceMiB;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getNumFPVVs() {
        return numFPVVs;
    }
    public void setNumFPVVs(int numFPVVs) {
        this.numFPVVs = numFPVVs;
    }
    public int getNumTDVVs() {
        return numTDVVs;
    }
    public void setNumTDVVs(int numTDVVs) {
        this.numTDVVs = numTDVVs;
    }
    public int getNumTPVVs() {
        return numTPVVs;
    }
    public void setNumTPVVs(int numTPVVs) {
        this.numTPVVs = numTPVVs;
    }
    public PrimeraCpgPrivateSpaceMiB getPrivateSpaceMiB() {
        return privateSpaceMiB;
    }
    public void setPrivateSpaceMiB(PrimeraCpgPrivateSpaceMiB privateSpaceMiB) {
        this.privateSpaceMiB = privateSpaceMiB;
    }
    public int getRawFreeSpaceMiB() {
        return rawFreeSpaceMiB;
    }
    public void setRawFreeSpaceMiB(int rawFreeSpaceMiB) {
        this.rawFreeSpaceMiB = rawFreeSpaceMiB;
    }
    public int getRawSharedSpaceMiB() {
        return rawSharedSpaceMiB;
    }
    public void setRawSharedSpaceMiB(int rawSharedSpaceMiB) {
        this.rawSharedSpaceMiB = rawSharedSpaceMiB;
    }
    public int getRawTotalSpaceMiB() {
        return rawTotalSpaceMiB;
    }
    public void setRawTotalSpaceMiB(int rawTotalSpaceMiB) {
        this.rawTotalSpaceMiB = rawTotalSpaceMiB;
    }
    public int getSharedSpaceMiB() {
        return sharedSpaceMiB;
    }
    public void setSharedSpaceMiB(int sharedSpaceMiB) {
        this.sharedSpaceMiB = sharedSpaceMiB;
    }
    public int getState() {
        return state;
    }
    public void setState(int state) {
        this.state = state;
    }
    public int getTotalSpaceMiB() {
        return totalSpaceMiB;
    }
    public void setTotalSpaceMiB(int totalSpaceMiB) {
        this.totalSpaceMiB = totalSpaceMiB;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

}
