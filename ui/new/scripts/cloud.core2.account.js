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

var systemAccountId = 1;
var adminAccountId = 2;

function afterLoadAccountJSP() {
    initDialog("dialog_resource_limits");
    initDialog("dialog_disable_account");
    initDialog("dialog_lock_account");
    initDialog("dialog_enable_account");      
}

function accountToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    if (jsonObj.accounttype == roleTypeUser) 
        $iconContainer.find("#icon").attr("src", "images/midmenuicon_account_user.png");		
	else if (jsonObj.accounttype == roleTypeAdmin) 
	    $iconContainer.find("#icon").attr("src", "images/midmenuicon_account_admin.png");		
	else if (jsonObj.accounttype == roleTypeDomainAdmin) 
	    $iconContainer.find("#icon").attr("src", "images/midmenuicon_account_domain.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.domain).substring(0,25));   
}

function accountToRightPanel($midmenuItem1) { 
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);  
    accountJsonToDetailsTab($midmenuItem1);   
}

function accountJsonToDetailsTab($midmenuItem1) {  
    var jsonObj = $midmenuItem1.data("jsonObj");    
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);  
    $detailsTab.find("#grid_header_title").text(fromdb(jsonObj.name));
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#role").text(toRole(jsonObj.accounttype));
    $detailsTab.find("#account").text(fromdb(jsonObj.name));
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));
    $detailsTab.find("#vm_total").text(jsonObj.vmtotal);
    $detailsTab.find("#ip_total").text(jsonObj.iptotal);
    $detailsTab.find("#bytes_received").text(convertBytes(jsonObj.receivedbytes));
    $detailsTab.find("#bytes_sent").text(convertBytes(jsonObj.sentbytes));
    $detailsTab.find("#state").text(jsonObj.state);
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty(); 
    var noAvailableActions = true;
        
    if(jsonObj.id != systemAccountId && jsonObj.id != adminAccountId) {
        if (jsonObj.accounttype == roleTypeUser || jsonObj.accounttype == roleTypeDomainAdmin) {
            buildActionLinkForDetailsTab("Resource limits", accountActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
            noAvailableActions = false;	
        }
        
        if(jsonObj.state == "enabled") {
            buildActionLinkForDetailsTab("Disable account", accountActionMap, $actionMenu, $midmenuItem1, $detailsTab);  
            buildActionLinkForDetailsTab("Lock account", accountActionMap, $actionMenu, $midmenuItem1, $detailsTab);
            noAvailableActions = false;	
        }          	        
        else if(jsonObj.state == "disabled" || jsonObj.state == "locked") {
            buildActionLinkForDetailsTab("Enable account", accountActionMap, $actionMenu, $midmenuItem1, $detailsTab);   
            noAvailableActions = false;	
        }           
    }  
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	  
}

var accountActionMap = {  
    "Resource limits": {                 
        dialogBeforeActionFn : doResourceLimits 
    } 
    ,
    "Disable account": {              
        isAsyncJob: true,
        asyncJobResponse: "disableaccountresponse",
        dialogBeforeActionFn : doDisableAccount,
        inProcessText: "Disabling account....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) { 
            //Get embedded object from listVolume API until bug 6482("enableAccount, disableAccount, lockAccount should return an embedded object") is fixed.
            var jsonObj;           
            $.ajax({
                data: createURL("command=listAccounts&id="+id),
                dataType: "json",
                async: false,
                success: function(json) { 
                    jsonObj = json.listaccountsresponse.account[0];
                }            
            });           
                
            accountToMidmenu(jsonObj, $midmenuItem1);           
            accountJsonToDetailsTab($midmenuItem1);
        }
    }    
    ,
    "Lock account": {              
        isAsyncJob: false,       
        dialogBeforeActionFn : doLockAccount,
        inProcessText: "Locking account....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {  
            //Get embedded object from listVolume API until bug 6482("enableAccount, disableAccount, lockAccount should return an embedded object") is fixed.
            var jsonObj;           
            $.ajax({
                data: createURL("command=listAccounts&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {    
                    jsonObj = json.listaccountsresponse.account[0];
                }            
            });       
                  
            accountToMidmenu(jsonObj, $midmenuItem1);           
            accountJsonToDetailsTab($midmenuItem1);
        }
    }    
    ,
    "Enable account": {              
        isAsyncJob: false,       
        dialogBeforeActionFn : doEnableAccount,
        inProcessText: "Enabling account....",
        afterActionSeccessFn: function(json, $midmenuItem1, id) {   
            //Get embedded object from listVolume API until bug 6482("enableAccount, disableAccount, lockAccount should return an embedded object") is fixed.
            var jsonObj;           
            $.ajax({
                data: createURL("command=listAccounts&id="+id),
                dataType: "json",
                async: false,
                success: function(json) {  
                    jsonObj = json.listaccountsresponse.account[0];
                }            
            });       
                
            accountToMidmenu(jsonObj, $midmenuItem1);           
            accountJsonToDetailsTab($midmenuItem1);
        }
    }    
}; 

function updateResourceLimit(domainId, account, type, max) {
	$.ajax({
	    data: createURL("command=updateResourceLimit&domainid="+domainId+"&account="+account+"&resourceType="+type+"&max="+max),
		dataType: "json",
		success: function(json) {								    												
		}
	});
}

function doResourceLimits($actionLink, $detailsTab, $midmenuItem1) {
    var $detailsTab = $("#right_panel_content #tab_content_details");  
	var jsonObj = $detailsTab.data("jsonObj");
	var domainId = jsonObj.domainid;
	var account = jsonObj.name;
	$.ajax({
		cache: false,				
		data: createURL("command=listResourceLimits&domainid="+domainId+"&account="+account),
		dataType: "json",
		success: function(json) {
			var limits = json.listresourcelimitsresponse.resourcelimit;		
			var preInstanceLimit, preIpLimit, preDiskLimit, preSnapshotLimit, preTemplateLimit = -1;
			if (limits != null) {	
				for (var i = 0; i < limits.length; i++) {
					var limit = limits[i];
					switch (limit.resourcetype) {
						case "0":
							preInstanceLimit = limit.max;
							$("#dialog_resource_limits #limits_vm").val(limit.max);
							break;
						case "1":
							preIpLimit = limit.max;
							$("#dialog_resource_limits #limits_ip").val(limit.max);
							break;
						case "2":
							preDiskLimit = limit.max;
							$("#dialog_resource_limits #limits_volume").val(limit.max);
							break;
						case "3":
							preSnapshotLimit = limit.max;
							$("#dialog_resource_limits #limits_snapshot").val(limit.max);
							break;
						case "4":
							preTemplateLimit = limit.max;
							$("#dialog_resource_limits #limits_template").val(limit.max);
							break;
					}
				}
			}	
			$("#dialog_resource_limits")
			.dialog('option', 'buttons', { 								
				"Save": function() { 	
					// validate values
					var isValid = true;					
					isValid &= validateNumber("Instance Limit", $("#dialog_resource_limits #limits_vm"), $("#dialog_resource_limits #limits_vm_errormsg"), -1, 32000, false);
					isValid &= validateNumber("Public IP Limit", $("#dialog_resource_limits #limits_ip"), $("#dialog_resource_limits #limits_ip_errormsg"), -1, 32000, false);
					isValid &= validateNumber("Disk Volume Limit", $("#dialog_resource_limits #limits_volume"), $("#dialog_resource_limits #limits_volume_errormsg"), -1, 32000, false);
					isValid &= validateNumber("Snapshot Limit", $("#dialog_resource_limits #limits_snapshot"), $("#dialog_resource_limits #limits_snapshot_errormsg"), -1, 32000, false);
					isValid &= validateNumber("Template Limit", $("#dialog_resource_limits #limits_template"), $("#dialog_resource_limits #limits_template_errormsg"), -1, 32000, false);
					if (!isValid) return;
												
					var instanceLimit = trim($("#dialog_resource_limits #limits_vm").val());
					var ipLimit = trim($("#dialog_resource_limits #limits_ip").val());
					var diskLimit = trim($("#dialog_resource_limits #limits_volume").val());
					var snapshotLimit = trim($("#dialog_resource_limits #limits_snapshot").val());
					var templateLimit = trim($("#dialog_resource_limits #limits_template").val());
											
					$(this).dialog("close"); 
					if (instanceLimit != preInstanceLimit) {
						updateResourceLimit(domainId, account, 0, instanceLimit);
					}
					if (ipLimit != preIpLimit) {
						updateResourceLimit(domainId, account, 1, ipLimit);
					}
					if (diskLimit != preDiskLimit) {
						updateResourceLimit(domainId, account, 2, diskLimit);
					}
					if (snapshotLimit != preSnapshotLimit) {
						updateResourceLimit(domainId, account, 3, snapshotLimit);
					}
					if (templateLimit != preTemplateLimit) {
						updateResourceLimit(domainId, account, 4, templateLimit);
					}
				}, 
				"Cancel": function() { 
					$(this).dialog("close"); 
				} 
			}).dialog("open");
		}
	});	
}

function doDisableAccount($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $detailsTab.data("jsonObj");    
    var id = jsonObj.id;
    
    $("#dialog_disable_account")    
    .dialog('option', 'buttons', {                    
        "Yes": function() { 		                    
            $(this).dialog("close");	
			var apiCommand = "command=disableAccount&account="+jsonObj.name+"&domainId="+jsonObj.domainid;	    	
	    	doActionToDetailsTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab) ;         		                    	     
        },
        "Cancel": function() {
            $(this).dialog("close");		     
        }
    }).dialog("open");  
}

function doLockAccount($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $detailsTab.data("jsonObj");    
    
    $("#dialog_lock_account")    
    .dialog('option', 'buttons', {                    
        "Yes": function() { 		                    
            $(this).dialog("close");			
			var apiCommand = "command=lockAccount&account="+jsonObj.name+"&domainId="+jsonObj.domainid;
	    	doActionToDetailsTab(jsonObj.id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	         		                    	     
        },
        "Cancel": function() {
            $(this).dialog("close");		     
        }
    }).dialog("open");  
}

function doEnableAccount($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $detailsTab.data("jsonObj");    
    
    $("#dialog_enable_account")    
    .dialog('option', 'buttons', {                    
        "Yes": function() { 		                    
            $(this).dialog("close");	
			var apiCommand = "command=enableAccount&account="+jsonObj.name+"&domainId="+jsonObj.domainid;
	    	doActionToDetailsTab(jsonObj.id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	         		                    	     
        },
        "Cancel": function() {
            $(this).dialog("close");		     
        }
    }).dialog("open");  
}