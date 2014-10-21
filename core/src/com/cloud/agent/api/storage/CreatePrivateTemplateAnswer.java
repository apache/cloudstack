//
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
//

package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.storage.Storage.ImageFormat;

public class CreatePrivateTemplateAnswer extends Answer {
    private String _path;
    private long _virtualSize;
    private long _physicalSize;
    private String _uniqueName;
    private ImageFormat _format;

    public CreatePrivateTemplateAnswer() {
        super();
    }

    public CreatePrivateTemplateAnswer(Command cmd, boolean success, String result, String path, long virtualSize, long physicalSize, String uniqueName,
            ImageFormat format) {
        super(cmd, success, result);
        _path = path;
        _virtualSize = virtualSize;
        _physicalSize = physicalSize;
        _uniqueName = uniqueName;
        _format = format;
    }

    public CreatePrivateTemplateAnswer(Command cmd, boolean success, String result) {
        super(cmd, success, result);
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String path) {
        _path = path;
    }

    public long getVirtualSize() {
        return _virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        _virtualSize = virtualSize;
    }

    public void setphysicalSize(long physicalSize) {
        this._physicalSize = physicalSize;
    }

    public long getphysicalSize() {
        return _physicalSize;
    }

    public String getUniqueName() {
        return _uniqueName;
    }

    public ImageFormat getImageFormat() {
        return _format;
    }
}
