package com.cloud.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="account_details")
public class AccountDetailVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="account_id")
    private long accountId;
    
    @Column(name="name")
    private String name;
    
    @Column(name="value", encryptable=true)
    private String value;
    
    protected AccountDetailVO() {
    }
    
    public AccountDetailVO(long accountId, String name, String value) {
        this.accountId = accountId;
        this.name = name;
        this.value = value;
    }
 
    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getId() {
        return id;
    }
}
