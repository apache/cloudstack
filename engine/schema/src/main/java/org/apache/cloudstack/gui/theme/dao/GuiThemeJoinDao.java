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
package org.apache.cloudstack.gui.theme.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.gui.themes.GuiThemeJoinVO;

import java.util.List;

public interface GuiThemeJoinDao extends GenericDao<GuiThemeJoinVO, Long> {
    GuiThemeJoinVO findDefaultTheme();

    Pair<List<GuiThemeJoinVO>, Integer> listGuiThemesWithNoAuthentication(String commonName);

    Pair<List<GuiThemeJoinVO>, Integer> listGuiThemes(Long id, String name, String commonName, String domainUuid, String accountUuid, boolean listAll, boolean showRemoved, Boolean showPublic);
}
