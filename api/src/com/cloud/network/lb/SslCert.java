package com.cloud.network.lb;


import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface SslCert extends InternalIdentity, Identity, ControlledEntity {

    public String getCertificate();
    public String getKey() ;
    public String getChain();
    public String getPassword();
    public String getFingerPrint();

}
