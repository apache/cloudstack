package org.apache.cloudstack.storage.datastore.command;

import org.apache.cloudstack.storage.datastore.client.BaseCommand;
import org.apache.cloudstack.storage.datastore.response.QueryAsyncJobResultResponse;

public class QueryAsyncJobResultCmd extends BaseCommand {

    public QueryAsyncJobResultCmd() {
        super("queryAsyncJobResult", new QueryAsyncJobResultResponse());
    }

}
