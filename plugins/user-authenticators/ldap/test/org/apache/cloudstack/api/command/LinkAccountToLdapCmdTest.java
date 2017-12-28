package org.apache.cloudstack.api.command;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccountVO;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.response.LinkAccountToLdapResponse;
import org.apache.cloudstack.ldap.LdapManager;
import org.apache.cloudstack.ldap.LdapUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LinkAccountToLdapCmdTest implements LdapConfigurationChanger {

    @Mock
    LdapManager ldapManager;
    @Mock
    AccountService accountService;

    LinkAccountToLdapCmd linkAccountToLdapCmd;

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        linkAccountToLdapCmd = new LinkAccountToLdapCmd();
        setHiddenField(linkAccountToLdapCmd, "_ldapManager", ldapManager);
        setHiddenField(linkAccountToLdapCmd, "_accountService", accountService);
    }

    @Test
    public void execute() throws Exception {
        //      test with valid params and with admin who doesnt exist in cloudstack
        long domainId = 1;
        String type = "GROUP";
        String ldapDomain = "CN=test,DC=ccp,DC=Citrix,DC=com";
        short accountType = Account.ACCOUNT_TYPE_DOMAIN_ADMIN;
        String username = "admin";
        long accountId = 24;
        String accountName = "test";

        setHiddenField(linkAccountToLdapCmd, "ldapDomain", ldapDomain);
        setHiddenField(linkAccountToLdapCmd, "admin", username);
        setHiddenField(linkAccountToLdapCmd, "type", type);
        setHiddenField(linkAccountToLdapCmd, "domainId", domainId);
        setHiddenField(linkAccountToLdapCmd, "accountType", accountType);
        setHiddenField(linkAccountToLdapCmd, "accountName", accountName);


        LinkAccountToLdapResponse response = new LinkAccountToLdapResponse(String.valueOf(domainId), type, ldapDomain, (short)accountType, username, accountName);
        when(ldapManager.linkAccountToLdap(linkAccountToLdapCmd)).thenReturn(response);
        when(ldapManager.getUser(username, type, ldapDomain, 1L))
                .thenReturn(new LdapUser(username, "admin@ccp.citrix.com", "Admin", "Admin", ldapDomain, "ccp", false, null));

        when(accountService.getActiveAccountByName(username, domainId)).thenReturn(null);
        UserAccountVO userAccount =  new UserAccountVO();
        userAccount.setAccountId(24);
        when(accountService.createUserAccount(eq(username), eq(""), eq("Admin"), eq("Admin"), eq("admin@ccp.citrix.com"), isNull(String.class),
                eq(username), eq(Account.ACCOUNT_TYPE_DOMAIN_ADMIN), eq(RoleType.DomainAdmin.getId()), eq(domainId), isNull(String.class),
                (java.util.Map<String,String>)isNull(), anyString(), anyString(), eq(User.Source.LDAP))).thenReturn(userAccount);

        linkAccountToLdapCmd.execute();
        LinkAccountToLdapResponse result = (LinkAccountToLdapResponse)linkAccountToLdapCmd.getResponseObject();
        assertEquals("objectName", "LinkAccountToLdap", result.getObjectName());
        assertEquals("commandName", linkAccountToLdapCmd.getCommandName(), result.getResponseName());
        assertEquals("domainId", String.valueOf(domainId), result.getDomainId());
        assertEquals("type", type, result.getType());
        assertEquals("name", ldapDomain, result.getLdapDomain());
        assertEquals("accountId", String.valueOf(accountId), result.getAdminId());
    }
}