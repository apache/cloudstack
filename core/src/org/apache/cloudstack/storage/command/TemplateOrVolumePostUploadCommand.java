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

package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;

public class TemplateOrVolumePostUploadCommand {
    DataObject dataObject;
    EndPoint endPoint;

    public TemplateOrVolumePostUploadCommand(DataObject dataObject, EndPoint endPoint) {
        this.dataObject = dataObject;
        this.endPoint = endPoint;
    }

    public TemplateOrVolumePostUploadCommand() {
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(EndPoint endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TemplateOrVolumePostUploadCommand that = (TemplateOrVolumePostUploadCommand)o;

        return dataObject.equals(that.dataObject) && endPoint.equals(that.endPoint);

    }

    @Override
    public int hashCode() {
        int result = dataObject.hashCode();
        result = 31 * result + endPoint.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TemplateOrVolumePostUploadCommand{" + "dataObject=" + dataObject + ", endPoint=" + endPoint + '}';
    }
}
