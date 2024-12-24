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
package org.apache.cloudstack.gui.theme;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface GuiTheme extends InternalIdentity, Identity {

    String getName();

    String getDescription();

    String getCss();

    String getJsonConfiguration();

    Date getCreated();

    Date getRemoved();

    boolean getIsPublic();

    void setId(Long id);

    void setUuid(String uuid);

    void setName(String name);

    void setDescription(String description);

    void setCss(String css);

    void setJsonConfiguration(String jsonConfiguration);

    void setCreated(Date created);

    void setRemoved(Date removed);

    void setIsPublic(boolean isPublic);

    boolean isRecursiveDomains();

    void setRecursiveDomains(boolean recursiveDomains);
}
