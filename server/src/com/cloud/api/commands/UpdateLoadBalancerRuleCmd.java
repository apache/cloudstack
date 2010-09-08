package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="updateLoadBalancerRule", manager=Manager.NetworkManager)
public class UpdateLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateLoadBalancerRuleCmd.class.getName());
    private static final String s_name = "updateloadbalancerruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="algorithm", type=CommandType.STRING)
    private String algorithm;

    @Parameter(name="description", type=CommandType.STRING)
    private String description;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String loadBalancerName;

    @Parameter(name="privateport", type=CommandType.STRING)
    private String privatePort;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAlgorithm() {
        return algorithm;
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
//        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
//        String description = (String)params.get(BaseCmd.Properties.DESCRIPTION.getName());
//        String privatePort = (String)params.get(BaseCmd.Properties.PRIVATE_PORT.getName());
//        String algorithm = (String)params.get(BaseCmd.Properties.ALGORITHM.getName());
//        Long loadBalancerId = (Long)params.get(BaseCmd.Properties.ID.getName());
//
//        if (userId == null) {
//            userId = Long.valueOf(1);
//        }
//
//        LoadBalancerVO lb = getManagementServer().findLoadBalancerById(loadBalancerId);
//        if (lb == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find load balancer rule " + loadBalancerId + " for update.");
//        }
//
//        // Verify input parameters
//        Account lbOwner = getManagementServer().findAccountById(lb.getAccountId());
//        if (lbOwner == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update load balancer rule, cannot find owning account");
//        }
//
//        Long accountId = lbOwner.getId();
//        if (account != null) {
//            if (!isAdmin(account.getType())) {
//                if (account.getId().longValue() != accountId.longValue()) {
//                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update load balancer rule, permission denied");
//                }
//            } else if (!getManagementServer().isChildDomain(account.getDomainId(), lbOwner.getDomainId())) {
//                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update load balancer rule, permission denied.");
//            }
//        }
//
//        long jobId = getManagementServer().updateLoadBalancerRuleAsync(userId, lb.getAccountId(), lb.getId().longValue(), name, description, privatePort, algorithm);
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId).toString()));
//        return returnValues;
//    }
    
	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
}
