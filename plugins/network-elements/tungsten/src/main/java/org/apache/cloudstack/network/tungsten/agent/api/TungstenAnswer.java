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
package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import net.juniper.tungsten.api.ApiObjectBase;
import org.apache.cloudstack.network.tungsten.model.TungstenModel;

import java.util.List;
import java.util.Objects;

public class TungstenAnswer extends Answer {

    ApiObjectBase apiObjectBase;
    List<ApiObjectBase> apiObjectBaseList;
    TungstenModel tungstenModel;
    List<TungstenModel> tungstenModelList;

    public TungstenAnswer(final Command command, final boolean success, final String details) {
        super(command, success, details);
    }

    public TungstenAnswer(final Command command, ApiObjectBase apiObjectBase, final boolean success,
                          final String details) {
        super(command, success, details);
        setApiObjectBase(apiObjectBase);
    }

    public TungstenAnswer(final Command command, List<ApiObjectBase> apiObjectBaseList, final boolean success,
                          final String details) {
        super(command, success, details);
        setApiObjectBaseList(apiObjectBaseList);
    }

    public TungstenAnswer(final Command command, final TungstenModel tungstenModel, final boolean success,
        final String details) {
        super(command, success, details);
        setTungstenModel(tungstenModel);
    }

    public TungstenAnswer(final Command command, final boolean success, final List<TungstenModel> tungstenModelList,
        final String details) {
        super(command, success, details);
        setTungstenModelList(tungstenModelList);
    }

    public TungstenAnswer(final Command command, final Exception e) {
        super(command, e);
    }

    public ApiObjectBase getApiObjectBase() {
        return apiObjectBase;
    }

    public void setApiObjectBase(ApiObjectBase apiObjectBase) {
        this.apiObjectBase = apiObjectBase;
    }

    public List<ApiObjectBase> getApiObjectBaseList() {
        return apiObjectBaseList;
    }

    public void setApiObjectBaseList(final List<ApiObjectBase> apiObjectBaseList) {
        this.apiObjectBaseList = apiObjectBaseList;
    }

    public TungstenModel getTungstenModel() {
        return tungstenModel;
    }

    public void setTungstenModel(final TungstenModel tungstenModel) {
        this.tungstenModel = tungstenModel;
    }

    public List<TungstenModel> getTungstenModelList() {
        return tungstenModelList;
    }

    public void setTungstenModelList(final List<TungstenModel> tungstenModelList) {
        this.tungstenModelList = tungstenModelList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TungstenAnswer that = (TungstenAnswer) o;
        return Objects.equals(apiObjectBase, that.apiObjectBase) && Objects.equals(apiObjectBaseList,
                that.apiObjectBaseList) && Objects.equals(tungstenModel, that.tungstenModel) && Objects.equals(tungstenModelList, that.tungstenModelList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), apiObjectBase, apiObjectBaseList, tungstenModel, tungstenModelList);
    }
}
