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

package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;
import com.cloud.storage.Storage.ImageFormat;

public class ResignatureAnswer extends Answer {
    private long size;
    private String path;
    private ImageFormat format;

    public ResignatureAnswer() {
    }

    public ResignatureAnswer(String errMsg) {
        super(null, false, errMsg);
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public ImageFormat getFormat() {
        return format;
    }
}
