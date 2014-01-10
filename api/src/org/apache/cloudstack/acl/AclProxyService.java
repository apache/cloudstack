package org.apache.cloudstack.acl;

import java.util.List;

public interface AclProxyService {

    List<String> listAclGroupsByAccount(long accountId);

}
