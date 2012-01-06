package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TrafficTypeImplementorResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Networks.TrafficType;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@Implementation(description="Lists implementors of implementor of a network traffic type or implementors of all network traffic types", responseObject=TrafficTypeImplementorResponse.class)
public class ListTafficTypeImplementorsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListTafficTypeImplementorsCmd.class);
	private static final String _name = "listtraffictypeimplementorsresponse";
	
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.TRAFFIC_TYPE, type=CommandType.STRING, description="Optional. The network traffic type, if specified, return its implementor. Otherwise, return all traffic types with their implementor")
    private String trafficType;
	
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public String getTrafficType() {
    	return trafficType;
    }
    
	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
	        ResourceAllocationException {
		List<Pair<TrafficType, String>> results = _networkService.listTrafficTypeImplementor(this);
		ListResponse<TrafficTypeImplementorResponse> response = new ListResponse<TrafficTypeImplementorResponse>();
		List<TrafficTypeImplementorResponse> responses= new ArrayList<TrafficTypeImplementorResponse>();
		for (Pair<TrafficType, String> r : results) {
			TrafficTypeImplementorResponse p = new TrafficTypeImplementorResponse();
			p.setTrafficType(r.first().toString());
			p.setImplementor(r.second());
			responses.add(p);
		}
		
		response.setResponses(responses);
		response.setResponseName(getCommandName());
		this.setResponseObject(response);
	}
	
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
    
	@Override
	public String getCommandName() {
		return _name;
	}
}
