/**
 * 
 */
package com.cloud.domain;

import java.util.Date;

import com.cloud.user.OwnedBy;

/**
 * Domain defines the Domain object.
 */
public interface Domain extends OwnedBy {
    public static final long ROOT_DOMAIN = 1L;
    
    long getId();

    Long getParent();
    
    void setParent(Long parent);

    String getName();
    
    void setName(String name);
    
    Date getRemoved();
    
    String getPath();
    
    void setPath(String path);
    
    int getLevel();
    
    int getChildCount();
    
    long getNextChildSeq();
}
