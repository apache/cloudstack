package org.apache.cloudstack.api.net;

/**
 * container for the network prerequisites as found in the appliance template
 *
 * for OVA:
 * {code}
 * <Network ovf:name="Management0-0">
 *   <Description>Management Network Interface</Description>
 * </Network>
 * {code}
 * {code}
 * <Item>
 *   <rasd:AddressOnParent>7</rasd:AddressOnParent>
 *   <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>
 *   <rasd:Connection>Management0-0</rasd:Connection>
 *   <rasd:Description>E1000 Ethernet adapter on "Management Network"</rasd:Description>
 *   <rasd:ElementName>Network adapter 1</rasd:ElementName>
 *   <rasd:InstanceID>6</rasd:InstanceID>
 *   <rasd:ResourceSubType>E1000</rasd:ResourceSubType>
 *   <rasd:ResourceType>10</rasd:ResourceType>
 * </Item>
 * {code}
 */
public class NetworkPrerequisiteTO implements NetworkPrerequisite {
    String name; // attribute on Network should match <rasd:Connection> on Item (virtual hardware)
    String networkDescription;

    int addressOnParent; // or String?
    boolean automaticAllocation;
    String nicDescription;
    String elementName;
    int InstanceID; // or String?
    String resourceSubType;
    String resourceType; // or int?

    @Override public int getAddressOnParent() {
        return addressOnParent;
    }

    public void setAddressOnParent(int addressOnParent) {
        this.addressOnParent = addressOnParent;
    }

    @Override public boolean isAutomaticAllocation() {
        return automaticAllocation;
    }

    public void setAutomaticAllocation(boolean automaticAllocation) {
        this.automaticAllocation = automaticAllocation;
    }

    @Override public String getNicDescription() {
        return nicDescription;
    }

    public void setNicDescription(String nicDescription) {
        this.nicDescription = nicDescription;
    }

    @Override public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    @Override public int getInstanceID() {
        return InstanceID;
    }

    public void setInstanceID(int instanceID) {
        InstanceID = instanceID;
    }

    @Override public String getResourceSubType() {
        return resourceSubType;
    }

    public void setResourceSubType(String resourceSubType) {
        this.resourceSubType = resourceSubType;
    }

    @Override public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override public String getNetworkDescription() {
        return networkDescription;
    }

    public void setNetworkDescription(String networkDescription) {
        this.networkDescription = networkDescription;
    }
}
