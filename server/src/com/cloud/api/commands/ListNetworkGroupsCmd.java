package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.IngressRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkGroupResponse;
import com.cloud.async.executor.IngressRuleResultObject;
import com.cloud.async.executor.NetworkGroupResultObject;
import com.cloud.network.security.NetworkGroupRulesVO;

@Implementation(method="searchForNetworkGroupRules", manager=Manager.NetworkGroupManager)
public class ListNetworkGroupsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListNetworkGroupsCmd.class.getName());

    private static final String s_name = "listnetworkgroupsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="networkgroupname", type=CommandType.STRING)
    private String networkGroupName;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<NetworkGroupRulesVO> networkGroups = (List<NetworkGroupRulesVO>)getResponseObject();
        List<NetworkGroupResultObject> groupResultObjs = NetworkGroupResultObject.transposeNetworkGroups(networkGroups);

        ListResponse response = new ListResponse();
        List<NetworkGroupResponse> netGrpResponses = new ArrayList<NetworkGroupResponse>();
        for (NetworkGroupResultObject networkGroup : groupResultObjs) {
            NetworkGroupResponse netGrpResponse = new NetworkGroupResponse();
            netGrpResponse.setId(networkGroup.getId());
            netGrpResponse.setName(networkGroup.getName());
            netGrpResponse.setDescription(networkGroup.getDescription());
            netGrpResponse.setAccountName(networkGroup.getAccountName());
            netGrpResponse.setDomainId(networkGroup.getDomainId());
            netGrpResponse.setDomainName(ApiDBUtils.findDomainById(networkGroup.getDomainId()).getName());

            List<IngressRuleResultObject> ingressRules = networkGroup.getIngressRules();
            if ((ingressRules != null) && !ingressRules.isEmpty()) {
                List<IngressRuleResponse> ingressRulesResponse = new ArrayList<IngressRuleResponse>();

                for (IngressRuleResultObject ingressRule : ingressRules) {
                    IngressRuleResponse ingressData = new IngressRuleResponse();

                    ingressData.setRuleId(ingressRule.getId());
                    ingressData.setProtocol(ingressRule.getProtocol());
                    if ("icmp".equalsIgnoreCase(ingressRule.getProtocol())) {
                        ingressData.setIcmpType(ingressRule.getStartPort());
                        ingressData.setIcmpCode(ingressRule.getEndPort());
                    } else {
                        ingressData.setStartPort(ingressRule.getStartPort());
                        ingressData.setEndPort(ingressRule.getEndPort());
                    }

                    if (ingressRule.getAllowedNetworkGroup() != null) {
                        ingressData.setNetworkGroupName(ingressRule.getAllowedNetworkGroup());
                        ingressData.setAccountName(ingressRule.getAllowedNetGroupAcct());
                    } else {
                        ingressData.setCidr(ingressRule.getAllowedSourceIpCidr());
                    }

                    ingressData.setResponseName("ingressrule");
                    ingressRulesResponse.add(ingressData);
                }
                netGrpResponse.setIngressRules(ingressRulesResponse);
            }
            netGrpResponse.setResponseName("networkgroup");
            netGrpResponses.add(netGrpResponse);
        }

        response.setResponses(netGrpResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
