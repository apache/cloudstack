package org.apache.cloudstack.api;

import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.metrics.MetricsService;
import org.apache.cloudstack.response.CpuSocketsMetricsResponse;
import javax.inject.Inject;
import java.util.List;

@APICommand(name = ListCpuSocketsMetricsCmd.APINAME, description = "Lists CPU Sockets Metrics", responseObject = CpuSocketsMetricsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, responseView = ResponseObject.ResponseView.Full)
public class ListCpuSocketsMetricsCmd extends BaseCmd {
    public static final String APINAME = "listCpuSocketsMetrics";

    @Inject
    private MetricsService metricsService;

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() {
        final List<CpuSocketsMetricsResponse> metricsResponses = metricsService.listCpuSocketsMetrics();
        ListResponse<CpuSocketsMetricsResponse> response = new ListResponse<>();
        response.setResponses(metricsResponses, metricsResponses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}