/**
 * 
 */
package com.cloud.vm;

import com.cloud.offering.ServiceOffering;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.component.Adapter;

public interface VirtualMachineProfiler extends Adapter {
    
    VmCharacteristics convert(ServiceOffering offering, VirtualMachineTemplate template);
}
