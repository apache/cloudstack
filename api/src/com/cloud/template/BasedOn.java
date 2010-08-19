/**
 * 
 */
package com.cloud.template;

/**
 * BasedOn is implemented by all objects that are based on a certain template.
 */
public interface BasedOn {
    
    /**
     * @return the template id that the volume is based on.
     */
    Long getTemplateId();

}
