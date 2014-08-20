package com.globo.globodns.cloudstack.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.globodns.client.model.Domain;

public class GloboDnsDomainResponse extends Answer {

    private Domain domain;

    public GloboDnsDomainResponse(Command command, Domain domain) {
        super(command, true, null);
        this.domain = domain;
    }

    public Domain getDomain() {
        return domain;
    }

}
