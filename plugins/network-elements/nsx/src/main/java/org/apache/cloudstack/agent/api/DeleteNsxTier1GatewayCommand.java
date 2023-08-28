package org.apache.cloudstack.agent.api;

public class DeleteNsxTier1GatewayCommand extends CreateNsxTier1GatewayCommand {

    public DeleteNsxTier1GatewayCommand(String zoneName, Long zoneId, String accountName, Long accountId, String vpcName) {
        super(zoneName, zoneId, accountName, accountId, vpcName);
    }
}
