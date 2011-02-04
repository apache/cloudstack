/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@DB(txn=false)
public class Db22beta4to22GAMigrationUtil {
        
    private Map<Long,Long> pfRuleIdToIpAddressIdMap = new HashMap<Long, Long>();
    private final String FindPfIdToPublicIpId = "SELECT id,ip_address_id from firewall_rules where is_static_nat=1";
    private final String FindVmIdPerPfRule = "SELECT instance_id from port_forwarding_rules where id = ?";
    private final String WriteVmIdToIpAddrTable = "UPDATE user_ip_address set vm_id = ? where id = ?";
    protected Db22beta4to22GAMigrationUtil() {
    }
    
    @DB
    //This method gets us a map of pf/firewall id <-> ip address id
    //Using the keyset, we will iterate over the pf table to find corresponding vm id
    //When we get the vm id, we will use the val for each key to update the corresponding ip addr row with the vm id
    public void populateMap(){
        Long key = null;
        Long val = null;
        
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        
        StringBuilder sql = new StringBuilder(FindPfIdToPublicIpId);

        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                key = rs.getLong("id");
                val = rs.getLong("ip_address_id");
                pfRuleIdToIpAddressIdMap.put(key, val);
            }           

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute " + pstmt.toString(), e);
        }

    }
    
    @DB
    public void updateVmIdForIpAddresses(){
        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
        Set<Long> pfIds = pfRuleIdToIpAddressIdMap.keySet();
        StringBuilder sql = new StringBuilder(FindVmIdPerPfRule);
        Long vmId = null;
        Long ipAddressId = null;
        PreparedStatement pstmt = null;
        for(Long pfId : pfIds){
            try {
                pstmt = txn.prepareAutoCloseStatement(sql.toString());
                pstmt.setLong(1, pfId);
                ResultSet rs = pstmt.executeQuery();
                while(rs.next()) {
                    vmId = rs.getLong("instance_id");
                }
                ipAddressId = pfRuleIdToIpAddressIdMap.get(pfId);
                finallyUpdate(ipAddressId, vmId, txn);
            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable to execute " + pstmt.toString(), e);
            }
        }
    }
    
    @DB
    public void finallyUpdate(Long ipAddressId, Long vmId, Transaction txn){

        StringBuilder sql = new StringBuilder(WriteVmIdToIpAddrTable);

        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql.toString());
            pstmt.setLong(1, vmId);
            pstmt.setLong(2, ipAddressId);
            int rs = pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute " + pstmt.toString(), e);
        }
    }
    
    public static void main(String[] args) {

        File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");

        if(file != null) {
            System.out.println("Log4j configuration from : " + file.getAbsolutePath());
            DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
        } else {
            System.out.println("Configure log4j with default properties");
        }
        
        Db22beta4to22GAMigrationUtil util = new Db22beta4to22GAMigrationUtil();
        util.populateMap();
        util.updateVmIdForIpAddresses();
    }
}
