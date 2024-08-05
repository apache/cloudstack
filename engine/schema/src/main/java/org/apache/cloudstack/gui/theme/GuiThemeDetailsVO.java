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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "gui_themes_details")
public class GuiThemeDetailsVO implements GuiThemeDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "gui_theme_id", nullable = false)
    private Long guiThemeId;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "value", nullable = false, length = 65535)
    private String value;

    public GuiThemeDetailsVO() {
    }

    public GuiThemeDetailsVO(Long guiThemeId, String type, String value) {
        this.guiThemeId = guiThemeId;
        this.type = type;
        this.value = value;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getGuiThemeId() {
        return guiThemeId;
    }

    @Override
    public void setGuiThemeId(Long guiThemeId) {
        this.guiThemeId = guiThemeId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }
}
