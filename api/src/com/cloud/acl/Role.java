package com.cloud.acl;

//metadata - consists of default dynamic roles in CS + any custom roles added by user
public interface Role {

    public static final short ROOT_ADMIN = 0;
    public static final short DOMAIN_ADMIN = 1;
    public static final short DOMAIN_USER = 2;
    public static final short OWNER = 3;
    public static final short PARENT_DOMAIN_ADMIN = 4;
    public static final short PARENT_DOMAIN_USER = 5;
    public static final short CHILD_DOMAIN_ADMIN = 6;
    public static final short CHILD_DOMAIN_USER = 7;
    
    public long getId();
    
    public short getRoleType();
    
    
 }
