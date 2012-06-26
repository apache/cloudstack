/*Copyright 2012 Citrix Systems, Inc. Licensed under the
Apache License, Version 2.0 (the "License"); you may not use this
file except in compliance with the License.  Citrix Systems, Inc.
reserves all rights not expressly granted by the License.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/


package com.cloud.upgrade.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade303to304 extends Upgrade30xBase implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade303to304.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "3.0.3", "3.0.4" };
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.4";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        return null;
    }

    @Override
    public void performDataMigration(Connection conn) {
        correctMultiplePhysicaNetworkSetups(conn);
    }

    private void correctMultiplePhysicaNetworkSetups(Connection conn) {
        PreparedStatement pstmtZone = null;
        ResultSet rsZone = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try{
    
            //check if multiple physical networks with 'Guest' Traffic types are present
            //Yes: 
            //1) check if there are guest networks without tags, if yes then add a new physical network with default tag for them
            //2) Check if there are physical network tags present
                //No: Add unique tag to each physical network
            //3) Get all guest networks unique network offering id's  
          
            //Clone each for each physical network and add the tag.
            //add ntwk service map entries
            //update all guest networks of 1 physical network having this offering id to this new offering id
            
            pstmtZone = conn.prepareStatement("SELECT id, domain_id, networktype, name FROM `cloud`.`data_center`");
            rsZone = pstmtZone.executeQuery();
            while (rsZone.next()) {
                long zoneId = rsZone.getLong(1);
                Long domainId = rsZone.getLong(2);
                String networkType = rsZone.getString(3);
                String zoneName = rsZone.getString(4);
            
                PreparedStatement pstmtUpdate = null;
                boolean multiplePhysicalNetworks = false;
                
                pstmt = conn.prepareStatement("SELECT count(*) FROM `cloud`.`physical_network_traffic_types` pntt JOIN `cloud`.`physical_network` pn ON pntt.physical_network_id = pn.id WHERE pntt.traffic_type ='Guest' and pn.data_center_id = ?");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                if(rs.next()){
                    Long count = rs.getLong(1);
                    if(count >  1){
                        s_logger.debug("There are "+count+" physical networks setup");
                        multiplePhysicalNetworks = true;
                    }
                }
                rs.close();
                pstmt.close();
    
                if(multiplePhysicalNetworks){
                
                    //check if any networks were untagged and remaining to be mapped to a physical network
                    
                    pstmt = conn.prepareStatement("SELECT count(n.id) FROM networks n WHERE n.physical_network_id IS NULL AND n.traffic_type = 'Guest' and n.data_center_id = ? and n.removed is null");
                    pstmt.setLong(1, zoneId);
                    rs = pstmt.executeQuery();
                    if(rs.next()){
                        Long count = rs.getLong(1);
                        if(count > 0){
                            // find the default tag to use from global config or use 'cloud-private'
                            String xenGuestLabel = getNetworkLabelFromConfig(conn, "xen.guest.network.device");
                            if(xenGuestLabel == null){
                                xenGuestLabel = "cloud-private";
                            }
                            
                            //Create a physical network with guest traffic type and this tag
                            long physicalNetworkId = addPhysicalNetworkToZone(conn, zoneId, zoneName, networkType, null, domainId);
                            addTrafficType(conn, physicalNetworkId, "Guest", xenGuestLabel, null, null);
                            addDefaultServiceProviders(conn, physicalNetworkId, zoneId);
                            
                            PreparedStatement pstmt3 = conn.prepareStatement("SELECT n.id FROM networks n WHERE n.physical_network_id IS NULL AND n.traffic_type = 'Guest' and n.data_center_id = ? and n.removed is null");
                            pstmt3.setLong(1, zoneId);
                            ResultSet rsNet = pstmt3.executeQuery();
                            s_logger.debug("Adding PhysicalNetwork to VLAN");
                            s_logger.debug("Adding PhysicalNetwork to user_ip_address");
                            s_logger.debug("Adding PhysicalNetwork to networks");
                            while(rsNet.next()){
                                Long networkId = rsNet.getLong(1);
                                addPhysicalNtwk_To_Ntwk_IP_Vlan(conn, physicalNetworkId,networkId);
                            }
                            rsNet.close();
                            pstmt3.close();
                        }
                    }
                    rs.close();
                    pstmt.close();
                    
                    //add tags to the physical networks if not present and clone offerings
                    
                    pstmt = conn.prepareStatement("SELECT pn.id as pid , ptag.tag as tag FROM `cloud`.`physical_network` pn LEFT JOIN `cloud`.`physical_network_tags` ptag ON pn.id = ptag.physical_network_id where pn.data_center_id = ?");
                    pstmt.setLong(1, zoneId);
                    rs = pstmt.executeQuery();
                    while(rs.next()){
                        long physicalNetworkId = rs.getLong("pid");
                        String tag = rs.getString("tag");
                        if(tag == null){
                            //need to add unique tag
                            String newTag = "pNtwk-tag-" + physicalNetworkId;
                            
                            String updateVnet = "INSERT INTO `cloud`.`physical_network_tags`(tag, physical_network_id) VALUES( ?, ? )";
                            pstmtUpdate = conn.prepareStatement(updateVnet);
                            pstmtUpdate.setString(1, newTag);
                            pstmtUpdate.setLong(2, physicalNetworkId);
                            pstmtUpdate.executeUpdate();
                            pstmtUpdate.close();
                            
                            //clone offerings and tag them with this new tag, if there are any guest networks for this physical network
                            
                            PreparedStatement pstmt2 = null;
                            ResultSet rs2 = null;
    
                            pstmt2 = conn.prepareStatement("SELECT distinct network_offering_id FROM `cloud`.`networks` where traffic_type= 'Guest' and physical_network_id = ? and removed is null");
                            pstmt2.setLong(1, physicalNetworkId);
                            rs2 = pstmt2.executeQuery();
                            
                            while(rs2.next()){
                                //clone each offering, add new tag, clone offering-svc-map, update guest networks with new offering id
                                long networkOfferingId = rs2.getLong(1);
                                cloneOfferingAndAddTag(conn, networkOfferingId, physicalNetworkId, newTag);
                            }
                            rs2.close();
                            pstmt2.close();
                        }
                    }
                    rs.close();
                    pstmt.close();
               }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while correcting PhysicalNetwork setup", e);
        } finally {
            if (rsZone != null) {
                try {
                    rsZone.close();
                }catch (SQLException e) {
                }
            }
            
            if (pstmtZone != null) {
                try {
                    pstmtZone.close();
                } catch (SQLException e) {
                }
            }
            
            if (rs != null) {
                try {
                    rs.close();
                }catch (SQLException e) {
                }
            }
            
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }
    }

        
    private void cloneOfferingAndAddTag(Connection conn, long networkOfferingId, long physicalNetworkId, String newTag) {

        
        PreparedStatement pstmt = null;
        ResultSet rs = null;        
        try{
            pstmt = conn.prepareStatement("select count(*) from `cloud`.`network_offerings`");
            rs = pstmt.executeQuery();
            long ntwkOffCount = 0;
            while (rs.next()) {
                ntwkOffCount = rs.getLong(1);
            }
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("DROP TEMPORARY TABLE IF EXISTS `cloud`.`network_offerings2`");
            pstmt.executeUpdate();
            
            pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE `cloud`.`network_offerings2` ENGINE=MEMORY SELECT * FROM `cloud`.`network_offerings` WHERE id=1");
            pstmt.executeUpdate();
            pstmt.close();
            
            // clone the record to
            pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings2` SELECT * FROM `cloud`.`network_offerings` WHERE id=?");
            pstmt.setLong(1, networkOfferingId);
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = conn.prepareStatement("SELECT unique_name FROM `cloud`.`network_offerings` WHERE id=?");
            pstmt.setLong(1, networkOfferingId);
            rs = pstmt.executeQuery();
            String uniqueName = null;
            while (rs.next()) {
                uniqueName = rs.getString(1) + "-" + physicalNetworkId;
            }
            rs.close();
            pstmt.close();
            

            pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings2` SET id=?, unique_name=?, name=?, tags=?, uuid=?  WHERE id=?");
            ntwkOffCount = ntwkOffCount + 1;
            long newNetworkOfferingId = ntwkOffCount;
            pstmt.setLong(1, newNetworkOfferingId);
            pstmt.setString(2, uniqueName);
            pstmt.setString(3, uniqueName);
            pstmt.setString(4, newTag);
            String uuid = UUID.randomUUID().toString();
            pstmt.setString(5, uuid); 
            pstmt.setLong(6, networkOfferingId);
            pstmt.executeUpdate();
            pstmt.close();
            
            pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings` SELECT * from `cloud`.`network_offerings2` WHERE id=" + newNetworkOfferingId);
            pstmt.executeUpdate();
            pstmt.close();
            
            //clone service map
            pstmt = conn.prepareStatement("select service, provider from `cloud`.`ntwk_offering_service_map` where network_offering_id=?");
            pstmt.setLong(1, networkOfferingId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String service = rs.getString(1);
                String provider = rs.getString(2);
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`ntwk_offering_service_map` (`network_offering_id`, `service`, `provider`, `created`) values (?,?,?, now())");
                pstmt.setLong(1, newNetworkOfferingId);
                pstmt.setString(2, service);
                pstmt.setString(3, provider);
                pstmt.executeUpdate();
            }
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where physical_network_id=? and traffic_type ='Guest' and network_offering_id="+networkOfferingId);
            pstmt.setLong(1, newNetworkOfferingId);
            pstmt.setLong(2, physicalNetworkId);
            pstmt.executeUpdate();
            pstmt.close();
            
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while cloning NetworkOffering", e);
        } finally {
            try {
                pstmt = conn.prepareStatement("DROP TEMPORARY TABLE `cloud`.`network_offerings2`");
                pstmt.executeUpdate();
            
                if (rs != null) {
                    rs.close();
                }
                
                if (pstmt != null) {
                    pstmt.close();
                }
            }catch (SQLException e) {
            }
        }
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
}
