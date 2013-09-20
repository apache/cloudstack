package net.juniper.contrail.management;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.PropertiesUtil;

/**
 * ManagementNetworkGuru
 * 
 * Replace the default management network strategy (PodBasedNetworkGuru) by using a Isolated network for management
 * traffic.
 * 
 * @author roque
 *
 */
@Component
public class ManagementNetworkGuru extends ContrailGuru {
    private static final Logger s_logger = Logger.getLogger(ManagementNetworkGuru.class);
    private static final TrafficType[] _trafficTypes = {TrafficType.Management};

    private final String configuration = "contrail.properties";
    private String _mgmt_cidr;
    private String _mgmt_gateway;

    @Override
    public String getName() {
        return "ManagementNetworkGuru";
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        File configFile = PropertiesUtil.findConfigFile(configuration);
        final Properties configProps = new Properties();
        try {
            configProps.load(new FileInputStream(configFile));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException(ex.getMessage());
        }
        _mgmt_cidr = configProps.getProperty("management.cidr");
        _mgmt_gateway = configProps.getProperty("management.gateway");
        s_logger.info("Management network " + _mgmt_cidr + " gateway: " + _mgmt_gateway);
        return true;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return _trafficTypes;
    }

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : _trafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    private boolean canHandle(NetworkOffering offering) {
        TrafficType type = offering.getTrafficType();
        return (isMyTrafficType(type));
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan,
            Network userSpecified, Account owner) {
        
        if (!canHandle(offering)) {
            return null;
        }
        NetworkVO network = new NetworkVO(offering.getTrafficType(), Mode.Dhcp, BroadcastDomainType.Lswitch,
                offering.getId(), Network.State.Allocated, plan.getDataCenterId(), plan.getPhysicalNetworkId());
        if (_mgmt_cidr != null) {
            network.setCidr(_mgmt_cidr);
            network.setGateway(_mgmt_gateway);
        }
        s_logger.debug("Allocated network " + userSpecified.getName() +
                (network.getCidr() == null ? "" : " subnet: " + network.getCidr()));
        return network;
    }

}
