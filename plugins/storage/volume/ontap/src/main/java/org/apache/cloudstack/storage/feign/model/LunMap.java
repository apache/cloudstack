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


package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LunMap {
  @JsonProperty("igroup")
  private Igroup igroup = null;
  @JsonProperty("logical_unit_number")
  private Integer logicalUnitNumber = null;
  @JsonProperty("lun")
  private Lun lun = null;
  @JsonProperty("svm")
  @SerializedName("svm")
  private Svm svm = null;

  public LunMap igroup (Igroup igroup) {
    this.igroup = igroup;
    return this;
  }

  public Igroup getIgroup () {
    return igroup;
  }

  public void setIgroup (Igroup igroup) {
    this.igroup = igroup;
  }

  public LunMap logicalUnitNumber (Integer logicalUnitNumber) {
    this.logicalUnitNumber = logicalUnitNumber;
    return this;
  }

  public Integer getLogicalUnitNumber () {
    return logicalUnitNumber;
  }

  public void setLogicalUnitNumber (Integer logicalUnitNumber) {
    this.logicalUnitNumber = logicalUnitNumber;
  }

  public LunMap lun (Lun lun) {
    this.lun = lun;
    return this;
  }

  public Lun getLun () {
    return lun;
  }

  public void setLun (Lun lun) {
    this.lun = lun;
  }

  public LunMap svm (Svm svm) {
    this.svm = svm;
    return this;
  }

  public Svm getSvm () {
    return svm;
  }

  public void setSvm (Svm svm) {
    this.svm = svm;
  }

  @Override
  public String toString () {
    StringBuilder sb = new StringBuilder();
    sb.append("class LunMap {\n");
    sb.append("    igroup: ").append(toIndentedString(igroup)).append("\n");
    sb.append("    logicalUnitNumber: ").append(toIndentedString(logicalUnitNumber)).append("\n");
    sb.append("    lun: ").append(toIndentedString(lun)).append("\n");
    sb.append("    svm: ").append(toIndentedString(svm)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  private String toIndentedString (Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
