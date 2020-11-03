package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import net.juniper.tungsten.api.ApiObjectBase;

public class TungstenAnswer extends Answer {

    ApiObjectBase apiObjectBase;

    public TungstenAnswer(final Command command, final boolean success, final String details) {
        super(command, success, details);
    }

    public TungstenAnswer(final Command command, ApiObjectBase apiObjectBase, final boolean success,
        final String details) {
        super(command, success, details);
        setApiObjectBase(apiObjectBase);
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
}
