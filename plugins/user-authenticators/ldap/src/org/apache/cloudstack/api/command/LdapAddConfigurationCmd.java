package org.apache.cloudstack.api.command;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LdapConfigurationResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "addLdapConfiguration", description = "Add a new Ldap Configuration", responseObject = LdapConfigurationResponse.class, since = "4.2.0")
public class LdapAddConfigurationCmd extends BaseCmd {
	public static final Logger s_logger = Logger
			.getLogger(LdapAddConfigurationCmd.class.getName());
	private static final String s_name = "ldapconfigurationresponse";

	@Inject
	private LdapManager _ldapManager;

	@Parameter(name = "hostname", type = CommandType.STRING, required = true, description = "Hostname")
	private String hostname;

	@Parameter(name = "port", type = CommandType.INTEGER, required = true, description = "Port")
	private int port;

	public LdapAddConfigurationCmd() {
		super();
	}

	public LdapAddConfigurationCmd(final LdapManager ldapManager) {
		super();
		_ldapManager = ldapManager;
	}

	@Override
	public void execute() throws ServerApiException {
		try {
			final LdapConfigurationResponse response = _ldapManager
					.addConfiguration(hostname, port);
			response.setObjectName("LdapAddConfiguration");
			response.setResponseName(getCommandName());
			setResponseObject(response);
		} catch (final InvalidParameterValueException e) {
			throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
					e.toString());
		}

	}

	@Override
	public String getCommandName() {
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		return Account.ACCOUNT_ID_SYSTEM;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}

	public void setPort(final int port) {
		this.port = port;
	}

}
