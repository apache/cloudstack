package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.network.IPAddressVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.UserVmVO;

public class UpdateIPForwardingRuleCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateIPForwardingRuleCmd.class.getName());

    private static final String s_name = "updateportforwardingruleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_IP, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PRIVATE_IP, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PUBLIC_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PRIVATE_PORT, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PROTOCOL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String publicIp = (String)params.get(BaseCmd.Properties.PUBLIC_IP.getName());
        String publicPort = (String)params.get(BaseCmd.Properties.PUBLIC_PORT.getName());
        String privateIp = (String)params.get(BaseCmd.Properties.PRIVATE_IP.getName());
        String privatePort = (String)params.get(BaseCmd.Properties.PRIVATE_PORT.getName());
        String protocol = (String)params.get(BaseCmd.Properties.PROTOCOL.getName());
        Long vmId = (Long)params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());
        UserVmVO userVM = null;

        if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }

        IPAddressVO ipAddressVO = getManagementServer().findIPAddressById(publicIp);
        if (ipAddressVO == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find IP address " + publicIp);
        }

        if (ipAddressVO.getAccountId() == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update port forwarding rule, owner of IP address " + publicIp + " not found.");
        }

        if (privateIp != null) {
            if (!NetUtils.isValidIp(privateIp)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid private IP address specified: " + privateIp);
            }
            Criteria c = new Criteria();
            c.addCriteria(Criteria.ACCOUNTID, new Object[] {ipAddressVO.getAccountId()});
            c.addCriteria(Criteria.DATACENTERID, ipAddressVO.getDataCenterId());
            c.addCriteria(Criteria.IPADDRESS, privateIp);
            List<UserVmVO> userVMs = getManagementServer().searchForUserVMs(c);
            if ((userVMs == null) || userVMs.isEmpty()) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid private IP address specified: " + privateIp + ", no virtual machine instances running with that address.");
            }
            userVM = userVMs.get(0);
        } else if (vmId != null) {
            userVM = getManagementServer().findUserVMInstanceById(vmId);
            if (userVM == null) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find virtual machine with id " + vmId);
            }

            if ((ipAddressVO.getAccountId() == null) || (ipAddressVO.getAccountId().longValue() != userVM.getAccountId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update port forwarding rule on IP address " + publicIp + ", permission denied."); 
            }

            if (ipAddressVO.getDataCenterId() != userVM.getDataCenterId()) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to update port forwarding rule, IP address " + publicIp + " is not in the same availability zone as virtual machine " + userVM.toString());
            }

            privateIp = userVM.getGuestIpAddress();
        } else {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No private IP address (privateip) or virtual machine instance id (virtualmachineid) specified, unable to update port forwarding rule");
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if (account != null) {
            if (isAdmin(account.getType())) {
                if (!getManagementServer().isChildDomain(account.getDomainId(), ipAddressVO.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update port forwarding rule on IP address " + publicIp + ", permission denied.");
                }
            } else if (account.getId().longValue() != ipAddressVO.getAccountId()) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to update port forwarding rule on IP address " + publicIp + ", permission denied.");
            }
        }

        long jobId = getManagementServer().updatePortForwardingRuleAsync(userId, ipAddressVO.getAccountId().longValue(), publicIp, privateIp, publicPort, privatePort, protocol);

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId).toString()));
        return returnValues;
    }
}
