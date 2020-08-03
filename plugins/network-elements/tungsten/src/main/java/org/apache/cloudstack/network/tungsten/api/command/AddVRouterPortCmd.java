package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;

import java.io.IOException;

import javax.inject.Inject;

@APICommand(name = "addVRouterPort", description = "Add VRouter Port", responseObject =
    SuccessResponse.class)
public class AddVRouterPortCmd extends BaseAsyncCmd {

  private static final String s_name = "addvrouterportcmd";

  //Owner information
  @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "An optional "
      + "account for the virtual machine. Must be used with domainId.")
  private String accountName;

  @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType =
      DomainResponse.class, description =
      "An optional domainId for the virtual machine. If the "
          + "account parameter is used, domainId must also be used.")
  private Long domainId;

  @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType =
      ProjectResponse.class, description = "Project ID for the service instance")
  private Long projectId;

  @Parameter(name = ApiConstants.TUNGSTEN_VM_INTERFACE_UUID, type = CommandType.STRING,
      description = "Tungsten Virtual Machine Interface UUID")
  private String tungstenVmInterfaceUuid;

  @Parameter(name = ApiConstants.TUNGSTEN_VIRTUAL_MACHINE_UUID, type = CommandType.STRING,
      description = "Tungsten virtual machine UUID")
  private String tungstenVirtualMachineUuid;

  @Parameter(name = ApiConstants.TUNGSTEN_VM_INTERFACE_MAC_ADDRESS, type = CommandType.STRING,
      description = "Tungsten virtual machine interface mac address")
  private String tungstenVmInterfaceMacAddress;

  @Parameter(name = ApiConstants.TUNGSTEN_INSTANCE_IP_ADDRESS, type = CommandType.STRING,
      description = "Tungsten Instance IP Address")
  private String tungstenInstanceIpAddress;

  @Parameter(name = ApiConstants.TUNGSTEN_VM_NAME, type = CommandType.STRING, description =
      "Tungsten virtual machine name")
  private String tungstenVmName;

  @Parameter(name = ApiConstants.TUNGSTEN_NETWORK_UUID, type = CommandType.STRING, description =
      "Tungsten network UUID")
  private String tungstenVnUuid;

  public String getTungstenVirtualMachineUuid() {
    return tungstenVirtualMachineUuid;
  }

  public String getTungstenVmInterfaceMacAddress() {
    return tungstenVmInterfaceMacAddress;
  }

  public String getTungstenInstanceIpAddress() {
    return tungstenInstanceIpAddress;
  }

  public String getTungstenVmInterfaceUuid() {
    return tungstenVmInterfaceUuid;
  }

  public Long getProjectId() {
    return projectId;
  }

  public String getTungstenVmName() {
    return tungstenVmName;
  }

  public String getTungstenVnUuid() {
    return tungstenVnUuid;
  }

  @Inject
  TungstenManager tungstenManager;

  @Override
  public String getEventType() {
    return EventTypes.EVENT_TUNGSTEN_ADD_VROUTER_PORT;
  }

  @Override
  public String getEventDescription() {
    return "Add vrouter port";
  }

  @Override
  public void execute()
      throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
      ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
    try {
      SuccessResponse response = tungstenManager.addVRouterPort(this);
      this.setResponseObject(response);
    } catch (IOException ex) {
      throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add vrouter port.");
    }
  }

  @Override
  public String getCommandName() {
    return s_name;
  }

  @Override
  public long getEntityOwnerId() {
    Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
    if (accountId == null) {
      return CallContext.current().getCallingAccount().getId();
    }

    return accountId;
  }
}
