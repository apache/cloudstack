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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportPolicy {

  @JsonProperty("id")
  private BigInteger id = null;
  @JsonProperty("name")
  private String name = null;
  @JsonProperty("rules")
  private List<ExportRule> rules = null;
  @JsonProperty("svm")
  private Svm svm = null;

  public BigInteger getId() {
    return id;
  }

  public ExportPolicy name(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ExportPolicy rules(List<ExportRule> rules) {
    this.rules = rules;
    return this;
  }

  public List<ExportRule> getRules() {
    return rules;
  }

  public void setRules(List<ExportRule> rules) {
    this.rules = rules;
  }

  public ExportPolicy svm(Svm svm) {
    this.svm = svm;
    return this;
  }

  public Svm getSvm() {
    return svm;
  }

  public void setSvm(Svm svm) {
    this.svm = svm;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExportPolicy exportPolicy = (ExportPolicy) o;
    return Objects.equals(this.id, exportPolicy.id) &&
        Objects.equals(this.name, exportPolicy.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash( id, name, rules, svm);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExportPolicy {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    rules: ").append(toIndentedString(rules)).append("\n");
    sb.append("    svm: ").append(toIndentedString(svm)).append("\n");
    sb.append("}");
    return sb.toString();
  }
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
