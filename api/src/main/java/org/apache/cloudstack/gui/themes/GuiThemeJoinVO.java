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
package org.apache.cloudstack.gui.themes;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "gui_themes_view")
public class GuiThemeJoinVO implements InternalIdentity {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid;

    @Column(name = "name", nullable = false, length = 2048)
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "css", nullable = false, length = 65535)
    private String css;

    @Column(name = "json_configuration", nullable = false, length = 65535)
    private String jsonConfiguration;

    @Column(name = "common_names", length = 65535)
    private String commonNames;

    @Column(name = "domains", length = 65535)
    private String domains;

    @Column(name = "accounts", length = 65535)
    private String accounts;

    @Column(name = "recursive_domains")
    private boolean recursiveDomains;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    public GuiThemeJoinVO() {
    }

    public GuiThemeJoinVO(Long id, String uuid, String name, String description, String css, String jsonConfiguration, String commonNames, String domains,
                          String accounts, boolean recursiveDomains, boolean isPublic, Date created, Date removed) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.css = css;
        this.jsonConfiguration = jsonConfiguration;
        this.commonNames = commonNames;
        this.domains = domains;
        this.accounts = accounts;
        this.recursiveDomains = recursiveDomains;
        this.isPublic = isPublic;
        this.created = created;
        this.removed = removed;
    }

    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCss() {
        return css;
    }

    public String getJsonConfiguration() {
        return jsonConfiguration;
    }

    public String getCommonNames() {
        return commonNames;
    }

    public String getDomains() {
        return domains;
    }

    public String getAccounts() {
        return accounts;
    }

    public boolean isRecursiveDomains() {
        return recursiveDomains;
    }

    public boolean getIsPublic() {
        return isPublic;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }
}
