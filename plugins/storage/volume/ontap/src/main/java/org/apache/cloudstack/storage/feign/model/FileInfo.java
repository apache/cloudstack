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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Information about a single file.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileInfo {
  @JsonProperty("bytes_used")
  private Long bytesUsed = null;
  @JsonProperty("creation_time")
  private OffsetDateTime creationTime = null;
  @JsonProperty("fill_enabled")
  private Boolean fillEnabled = null;
  @JsonProperty("is_empty")
  private Boolean isEmpty = null;
  @JsonProperty("is_snapshot")
  private Boolean isSnapshot = null;
  @JsonProperty("is_vm_aligned")
  private Boolean isVmAligned = null;
  @JsonProperty("modified_time")
  private OffsetDateTime modifiedTime = null;
  @JsonProperty("name")
  private String name = null;
  @JsonProperty("overwrite_enabled")
  private Boolean overwriteEnabled = null;
  @JsonProperty("path")
  private String path = null;
  @JsonProperty("size")
  private Long size = null;
  @JsonProperty("target")
  private String target = null;

  /**
   * Type of the file.
   */
  public enum TypeEnum {
    FILE("file"),
    DIRECTORY("directory");

    private String value;

    TypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TypeEnum fromValue(String value) {
      for (TypeEnum b : TypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      return null;
    }
  }

  @JsonProperty("type")
  private TypeEnum type = null;

  @JsonProperty("unique_bytes")
  private Long uniqueBytes = null;

  @JsonProperty("unix_permissions")
  private Integer unixPermissions = null;

   /**
   * The actual number of bytes used on disk by this file. If byte_offset and length parameters are specified, this will return the bytes used by the file within the given range.
   * @return bytesUsed
  **/
  public Long getBytesUsed() {
    return bytesUsed;
  }

  public OffsetDateTime getCreationTime() {
    return creationTime;
  }

  public FileInfo fillEnabled(Boolean fillEnabled) {
    this.fillEnabled = fillEnabled;
    return this;
  }

  public Boolean isFillEnabled() {
    return fillEnabled;
  }

  public void setFillEnabled(Boolean fillEnabled) {
    this.fillEnabled = fillEnabled;
  }


  public Boolean isIsEmpty() {
    return isEmpty;
  }

  public void setIsEmpty(Boolean isEmpty) {
    this.isEmpty = isEmpty;
  }

  public Boolean isIsSnapshot() {
    return isSnapshot;
  }

  public void setIsSnapshot(Boolean isSnapshot) {
    this.isSnapshot = isSnapshot;
  }


  public Boolean isIsVmAligned() {
    return isVmAligned;
  }


  public OffsetDateTime getModifiedTime() {
    return modifiedTime;
  }

  public FileInfo name(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FileInfo overwriteEnabled(Boolean overwriteEnabled) {
    this.overwriteEnabled = overwriteEnabled;
    return this;
  }

  public Boolean isOverwriteEnabled() {
    return overwriteEnabled;
  }

  public void setOverwriteEnabled(Boolean overwriteEnabled) {
    this.overwriteEnabled = overwriteEnabled;
  }

  public FileInfo path(String path) {
    this.path = path;
    return this;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public FileInfo target(String target) {
    this.target = target;
    return this;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public FileInfo type(TypeEnum type) {
    this.type = type;
    return this;
  }

  public TypeEnum getType() {
    return type;
  }

  public void setType(TypeEnum type) {
    this.type = type;
  }

  public Long getUniqueBytes() {
    return uniqueBytes;
  }

  public FileInfo unixPermissions(Integer unixPermissions) {
    this.unixPermissions = unixPermissions;
    return this;
  }

  public Integer getUnixPermissions() {
    return unixPermissions;
  }

  public void setUnixPermissions(Integer unixPermissions) {
    this.unixPermissions = unixPermissions;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileInfo fileInfo = (FileInfo) o;
    return Objects.equals(this.name, fileInfo.name) &&
        Objects.equals(this.path, fileInfo.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bytesUsed, creationTime, fillEnabled, isEmpty, isSnapshot, isVmAligned, modifiedTime, name, overwriteEnabled, path, size, target, type, uniqueBytes, unixPermissions);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileInfo {\n");
    sb.append("    bytesUsed: ").append(toIndentedString(bytesUsed)).append("\n");
    sb.append("    creationTime: ").append(toIndentedString(creationTime)).append("\n");
    sb.append("    fillEnabled: ").append(toIndentedString(fillEnabled)).append("\n");
    sb.append("    isEmpty: ").append(toIndentedString(isEmpty)).append("\n");
    sb.append("    isSnapshot: ").append(toIndentedString(isSnapshot)).append("\n");
    sb.append("    isVmAligned: ").append(toIndentedString(isVmAligned)).append("\n");
    sb.append("    modifiedTime: ").append(toIndentedString(modifiedTime)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    overwriteEnabled: ").append(toIndentedString(overwriteEnabled)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    uniqueBytes: ").append(toIndentedString(uniqueBytes)).append("\n");
    sb.append("    unixPermissions: ").append(toIndentedString(unixPermissions)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
