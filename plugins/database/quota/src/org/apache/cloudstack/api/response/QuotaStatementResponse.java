//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class QuotaStatementResponse extends BaseResponse {

  @SerializedName("usageType")
  @Param(description = "usageType")
  private String usageType;

  @SerializedName("usageUnit")
  @Param(description = "usageUnit")
  private String usageUnit;

  @SerializedName("usageDiscriminator")
  @Param(description = "usageDiscriminator")
  private String usageDiscriminator;

  @SerializedName("currencyValue")
  @Param(description = "currencyValue")
  private int currencyValue;

  @SerializedName("include")
  @Param(description = "include")
  private int include;

  @SerializedName("description")
  @Param(description = "description")
  private String description;


   public QuotaStatementResponse() {
       super();
   }

   public QuotaStatementResponse(final String usageType) {
       super();
       this.usageType = usageType;
   }

  public String getUsageType() {
      return usageType;
  }

  public void setUsageType(String usageType) {
      this.usageType = usageType;
  }

  public String getUsageUnit() {
      return usageUnit;
  }

  public void setUsageUnit(String usageUnit) {
      this.usageUnit = usageUnit;
  }

  public String getUsageDiscriminator() {
      return usageDiscriminator;
  }

  public void setUsageDiscriminator(String usageDiscriminator) {
      this.usageDiscriminator = usageDiscriminator;
  }

  public int getCurrencyValue() {
      return currencyValue;
  }

  public void setCurrencyValue(int currencyValue) {
      this.currencyValue = currencyValue;
  }

  public int getInclude() {
      return include;
  }

  public void setInclude(int include) {
      this.include = include;
  }

  public String getDescription() {
      return description;
  }

  public void setDescription(String description) {
      this.description = description;
  }

}
