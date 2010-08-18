/**
 * 
 */
package com.cloud.domain;

/**
 * PartOf must be implemented by all objects that belongs 
 * in a domain.
 */
public interface PartOf {
    /**
     * @return domain id that the object belongs to.
     */
    long getDomainId();
}
