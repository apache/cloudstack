package org.apache.cloudstack.api.command.admin.router;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.network.ListNetworkOfferingsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.OvsProviderResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.OvsProvider;
import com.cloud.network.element.VirtualRouterElementService;

@APICommand(name = "listOvsElements", description = "Lists all available ovs elements.", responseObject = OvsProviderResponse.class)
public class ListOvsElementsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger
			.getLogger(ListNetworkOfferingsCmd.class.getName());
	private static final String _name = "listovselementsresponse";
	@Inject
	private List<VirtualRouterElementService> _service;
	// ///////////////////////////////////////////////////
	// ////////////// API parameters /////////////////////
	// ///////////////////////////////////////////////////
	@Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = OvsProviderResponse.class, description = "list ovs elements by id")
	private Long id;

	@Parameter(name = ApiConstants.NSP_ID, type = CommandType.UUID, entityType = ProviderResponse.class, description = "list ovs elements by network service provider id")
	private Long nspId;

	@Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, description = "list network offerings by enabled state")
	private Boolean enabled;

	// ///////////////////////////////////////////////////
	// ///////////////// Accessors ///////////////////////
	// ///////////////////////////////////////////////////

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setNspId(Long nspId) {
		this.nspId = nspId;
	}

	public Long getNspId() {
		return nspId;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	@Override
	public String getCommandName() {
		return _name;
	}
	@Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        List<? extends OvsProvider> providers = _service.get(0).searchForOvsElement(this);
        ListResponse<OvsProviderResponse> response = new ListResponse<OvsProviderResponse>();
        List<OvsProviderResponse> providerResponses = new ArrayList<OvsProviderResponse>();
        for (OvsProvider provider : providers) {
        	OvsProviderResponse providerResponse = _responseGenerator.createOvsProviderResponse(provider);
            providerResponses.add(providerResponse);
        }
        response.setResponses(providerResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);

    }
}
