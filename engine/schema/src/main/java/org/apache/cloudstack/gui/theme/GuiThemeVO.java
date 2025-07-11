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

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "gui_themes")
public class GuiThemeVO implements GuiTheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, length = 2048)
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "css", length = 65535)
    private String css;

    @Column(name = "json_configuration", length = 65535)
    private String jsonConfiguration;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "recursive_domains")
    private boolean recursiveDomains = false;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    public GuiThemeVO() {

    }

    public GuiThemeVO(String name, String description, String css, String jsonConfiguration, boolean recursiveDomains, boolean isPublic, Date created, Date removed) {
        this.name = name;
        this.description = description;
        this.css = css;
        this.jsonConfiguration = jsonConfiguration;
        this.recursiveDomains = recursiveDomains;
        this.isPublic = isPublic;
        this.created = created;
        this.removed = removed;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCss() {
        return css;
    }

    @Override
    public String getJsonConfiguration() {
        return jsonConfiguration;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public boolean getIsPublic() {
        return isPublic;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setCss(String css) {
        this.css = css;
    }

    @Override
    public void setJsonConfiguration(String jsonConfiguration) {
        this.jsonConfiguration = jsonConfiguration;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    public boolean isRecursiveDomains() {
        return recursiveDomains;
    }

    @Override
    public void setRecursiveDomains(boolean recursiveDomains) {
        this.recursiveDomains = recursiveDomains;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "uuid", "name", "description", "isPublic", "recursiveDomains");
    }
}
