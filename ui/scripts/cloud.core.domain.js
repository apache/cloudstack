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

var $selectedDomainTreeNode;
var defaultRootLevel = 0;	   
var childParentMap = {};  //map childDomainId to parentDomainId
var domainIdNameMap = {}; //map domainId to domainName    

function refreshWholeTree(rootDomainId, rootLevel) {
    drawRootNode(rootDomainId);
    drawTree(rootDomainId, (rootLevel+1), $("#domain_children_container_"+rootDomainId));  //draw the whole tree (under root node)			
    $("#domain_"+rootDomainId).show();	//show root node
    clickExpandIcon(rootDomainId);      //expand root node	    
}

//draw root node
function drawRootNode(rootDomainId) {
    var $loading = $("#leftmenu_domain_tree").find("#loading_container").show();
    var $domainTree = $("#leftmenu_domain_tree").find("#tree_container").hide();
   
    $.ajax({
        data: createURL("command=listDomains&id="+rootDomainId+"&pageSize=-1"), //pageSize=-1 will return all items (no limitation)
        dataType: "json",
        async: false,
        success: function(json) {					        
            var domains = json.listdomainsresponse.domain;	
            $domainTree.empty();			        	    
	        if (domains != null && domains.length > 0) {				   					    
			    var node = drawNode(domains[0], defaultRootLevel, $domainTree); 
			    
			    var treeLevelsbox = node.find(".tree_levelsbox");	//root node shouldn't have margin-left:20px				   
			    if(treeLevelsbox!=null && treeLevelsbox.length >0)
			        treeLevelsbox[0].style.marginLeft="0px";        //set root node's margin-left to 0px.
			}		
			$loading.hide();
            $domainTree.show();			
        }
    }); 		
}

function drawNode(json, level, container) {		  
    if("parentdomainid" in json)
        childParentMap[json.id] = json.parentdomainid;	//map childDomainId to parentDomainId   
    domainIdNameMap[json.id] = json.name;               //map domainId to domainName

    var $treeNode = $("#domain_tree_node_template").clone(true);	  
    $treeNode.find("#domain_indent").css("marginLeft", (30*(level+1)));           
    $treeNode.attr("id", "domain_"+noNull(json.id));	         
    $treeNode.data("jsonObj", json).data("domainLevel", level); 	      
    $treeNode.find("#domain_title_container").attr("id", "domain_title_container_"+noNull(json.id)); 	        
    $treeNode.find("#domain_expand_icon").attr("id", "domain_expand_icon_"+noNull(json.id)); 
    $treeNode.find("#domain_name").attr("id", "domain_name_"+noNull(json.id)).text(fromdb(json.name));        	              	
    $treeNode.find("#domain_children_container").attr("id", "domain_children_container_"+noNull(json.id));          
    container.append($treeNode.show());	 
    return $treeNode;   	       
}          

function drawTree(id, level, container) {		        
    $.ajax({
	    data: createURL("command=listDomainChildren&id="+id+"&pageSize=-1"),
	    dataType: "json",
	    async: false,
	    success: function(json) {					        
	        var domains = json.listdomainchildrenresponse.domain;				        	    
		    if (domains != null && domains.length > 0) {					    
			    for (var i = 0; i < domains.length; i++) {						    
				    drawNode(domains[i], level, container);	
				    if(domains[i].haschild == true) 
		                drawTree(domains[i].id, (level+1), $("#domain_children_container_"+domains[i].id));				   
			    }
		    }				
	    }
    }); 
}	

function clickExpandIcon(domainId) {
    var $treeNode = $("#domain_"+domainId);
    var expandIcon = $treeNode.find("#domain_expand_icon_"+domainId);
    if (expandIcon.hasClass("expanded_close")) {													
		$treeNode.find("#domain_children_container_"+domainId).show();							
		expandIcon.removeClass("expanded_close").addClass("expanded_open");
	} 
	else if (expandIcon.hasClass("expanded_open")) {																	
	    $treeNode.find("#domain_children_container_"+domainId).hide();						
		expandIcon.removeClass("expanded_open").addClass("expanded_close");
	}			
}					

function domainAccountJSONToTemplate(jsonObj, $template) {   
    $template.data("jsonObj", jsonObj);  
    $template.find("#grid_header_title").text(fromdb(jsonObj.name));
    $template.find("#id").text(jsonObj.id);
    $template.find("#role").text(toRole(jsonObj.accounttype));
    $template.find("#account").text(fromdb(jsonObj.name));
    $template.find("#domain").text(fromdb(jsonObj.domain));
    $template.find("#vm_total").text(jsonObj.vmtotal);
    $template.find("#ip_total").text(jsonObj.iptotal);
    $template.find("#bytes_received").text(convertBytes(jsonObj.receivedbytes));
    $template.find("#bytes_sent").text(convertBytes(jsonObj.sentbytes));
    $template.find("#state").text(jsonObj.state);
}

function domainToRightPanel($leftmenuItem1) {  
    if($("#domain_grid_container").length == 0) { //domain.jsp is not loaded in right panel        
        $("#right_panel").load("jsp/domain.jsp", function(){         
            //switch between different tabs
            var tabArray = [$("#tab_details"), $("#tab_resource_limits"), $("#tab_admin_account")];
            var tabContentArray = [$("#tab_content_details"), $("#tab_content_resource_limits"), $("#tab_content_admin_account")];
            switchBetweenDifferentTabs(tabArray, tabContentArray);       
                           
			domainToRightPanel2($leftmenuItem1);       
		});        
    }
    else {        
        domainToRightPanel2($leftmenuItem1); 
    }
}

function domainToRightPanel2($leftmenuItem1) {
    $("#right_panel_content").data("$leftmenuItem1", $leftmenuItem1);
    var jsonObj = $leftmenuItem1.data("jsonObj");    
    var $detailsTab = $("#right_panel_content").find("#tab_content_details");    
    var domainId = jsonObj.id;
    $detailsTab.find("#id").text(domainId);
    $detailsTab.find("#grid_header_title").text(fromdb(jsonObj.name));	
    $detailsTab.find("#name").text(fromdb(jsonObj.name));	    	   
			  	
  	$.ajax({
	    cache: false,				
	    data: createURL("command=listAccounts&domainid="+domainId+maxPageSize),
	    dataType: "json",
	    success: function(json) {				       
		    var accounts = json.listaccountsresponse.account;					
		    if (accounts != null) 	
		        $detailsTab.find("#redirect_to_account_page").text(accounts.length);
		    else 
		        $detailsTab.find("#redirect_to_account_page").text("0");		
	    }		
    });		 
  			 				 			 
    $.ajax({
	    cache: false,				
	    data: createURL("command=listVirtualMachines&domainid="+domainId),
	    dataType: "json",
	    success: function(json) {
		    var instances = json.listvirtualmachinesresponse.virtualmachine;					
		    if (instances != null) 	
		        $detailsTab.find("#redirect_to_instance_page").text(instances.length);	
		    else 
		        $detailsTab.find("#redirect_to_instance_page").text("0");	
	    }		
    });		 
    			    
    $.ajax({
	    cache: false,				
	    data: createURL("command=listVolumes&domainid="+domainId),
	    dataType: "json",
	    success: function(json) {
		    var volumes = json.listvolumesresponse.volume;						
		    if (volumes != null) 	
		        $detailsTab.find("#redirect_to_volume_page").text(volumes.length);	
		    else 
		        $detailsTab.find("#redirect_to_volume_page").text("0");		
	    }		
    });

    listAdminAccounts(domainId);  

	if (isAdmin() || (isDomainAdmin() && (g_domainid != domainId))) {				
		var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");	
		$.ajax({
			cache: false,				
			data: createURL("command=listResourceLimits&domainid="+domainId),
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
								$resourceLimitsTab.find("#limits_vm").text(preInstanceLimit);
								$resourceLimitsTab.find("#limits_vm_edit").val(preInstanceLimit);
								break;
							case "1":
								preIpLimit = limit.max;
								$resourceLimitsTab.find("#limits_ip").text(preIpLimit);
								$resourceLimitsTab.find("#limits_ip_edit").val(preIpLimit);
								break;
							case "2":
								preDiskLimit = limit.max;
								$resourceLimitsTab.find("#limits_volume").text(preDiskLimit);
								$resourceLimitsTab.find("#limits_volume_edit").val(preDiskLimit);
								break;
							case "3":
								preSnapshotLimit = limit.max;
								$resourceLimitsTab.find("#limits_snapshot").text(preSnapshotLimit);
								$resourceLimitsTab.find("#limits_snapshot_edit").val(preSnapshotLimit);
								break;
							case "4":
								preTemplateLimit = limit.max;
								$resourceLimitsTab.find("#limits_template").text(preTemplateLimit);
								$resourceLimitsTab.find("#limits_template_edit").val(preTemplateLimit);
								break;
						}
					}
				}						
			}
		});		
		
		domainToResourceLimitsTab();	
		$("#tab_resource_limits").show();	
	} 
	else {
		$("#tab_resource_limits").hide();
	}		 		 
}

function domainToResourceLimitsTab() {   
    var $leftmenuItem1 = $("#right_panel_content").data("$leftmenuItem1");
    if($leftmenuItem1 == null)
        return;
    
    var jsonObj = $leftmenuItem1.data("jsonObj");
    if(jsonObj == null)
        return;    
    
    var $thisTab = $("#right_panel_content").find("#tab_content_resource_limits");  
    
    //actions ***
    var $actionLink = $thisTab.find("#action_link"); 
    $actionLink.bind("mouseover", function(event) {	    
        $(this).find("#action_menu").show();    
        return false;
    });
    $actionLink.bind("mouseout", function(event) {       
        $(this).find("#action_menu").hide();    
        return false;
    });	  
    
    var $actionMenu = $thisTab.find("#action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    buildActionLinkForTab("Edit Resource Limits", domainResourceLimitsActionMap, $actionMenu, $leftmenuItem1, $thisTab);		
}

function bindEventHandlerToDomainTreeNode() {
	$("#domain_tree_node_template").unbind("click").bind("click", function(event) {			     
		var $thisNode = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = $thisNode.attr("id");
		var jsonObj = $thisNode.data("jsonObj");	
		var domainId = jsonObj.id;	
		var domainName = jsonObj.name;										
		if (action.indexOf("domain_expand_icon")!=-1) {		
		    clickExpandIcon(domainId);					
		}
		else {
            if($selectedDomainTreeNode != null && $selectedDomainTreeNode.data("jsonObj") != null)
                $selectedDomainTreeNode.find("#domain_title_container_"+$selectedDomainTreeNode.data("jsonObj").id).removeClass("selected");      
            $thisNode.find("#domain_title_container_"+domainId).addClass("selected");
            $selectedDomainTreeNode = $thisNode;            
            domainToRightPanel($thisNode);                   
		}					
		return false;
    });
}

function updateResourceLimitForDomain(domainId, type, max, $readonlyField) {
	$.ajax({
	    data: createURL("command=updateResourceLimit&domainid="+domainId+"&resourceType="+type+"&max="+max),
		dataType: "json",
		async: false,
		success: function(json) {	
		    $readonlyField.text(max);							    												
		}
	});
}

function listAdminAccounts(domainId) {   	   	
    var accountType = (domainId==1)? 1: 2; 	    		
    $.ajax({
		cache: false,				
	    data: createURL("command=listAccounts&domainid="+domainId+"&accounttype="+accountType+maxPageSize),
		dataType: "json",
		success: function(json) {
			var items = json.listaccountsresponse.account;		
			var $container = $("#right_panel_content #tab_content_admin_account").empty();			
			if (items != null && items.length > 0) {					    
				var $template = $("#admin_account_tab_template");				
				for (var i = 0; i < items.length; i++) {
					var $newTemplate = $template.clone(true);
	                domainAccountJSONToTemplate(items[i], $newTemplate); 
	                $container.append($newTemplate.show());	
				}				    				
			} 			         
		}		
	});		
}		

var domainResourceLimitsActionMap = {  
    "Edit Resource Limits": {
        dialogBeforeActionFn: doEditResourceLimits
    }
}   

function doEditResourceLimits($actionLink, $detailsTab, $midmenuItem1) {       
    var $readonlyFields  = $detailsTab.find("#limits_vm, #limits_ip, #limits_volume, #limits_snapshot, #limits_template");
    var $editFields = $detailsTab.find("#limits_vm_edit, #limits_ip_edit, #limits_volume_edit, #limits_snapshot_edit, #limits_template_edit"); 
           
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        $editFields.hide();
        $readonlyFields.show();   
        $("#save_button, #cancel_button").hide();       
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditResourceLimits2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditResourceLimits2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {  
    var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");

    var isValid = true;	        			
	isValid &= validateNumber("Instance Limit", $resourceLimitsTab.find("#limits_vm_edit"), $resourceLimitsTab.find("#limits_vm_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Public IP Limit", $resourceLimitsTab.find("#limits_ip_edit"), $resourceLimitsTab.find("#limits_ip_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Disk Volume Limit", $resourceLimitsTab.find("#limits_volume_edit"), $resourceLimitsTab.find("#limits_volume_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Snapshot Limit", $resourceLimitsTab.find("#limits_snapshot_edit"), $resourceLimitsTab.find("#limits_snapshot_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Template Limit", $resourceLimitsTab.find("#limits_template_edit"), $resourceLimitsTab.find("#limits_template_edit_errormsg"), -1, 32000, false);
	if (!isValid) 
	    return;
								
	var jsonObj = $midmenuItem1.data("jsonObj");
	var domainId = jsonObj.id;
	
	var instanceLimit = trim($resourceLimitsTab.find("#limits_vm_edit").val());
	var ipLimit = trim($resourceLimitsTab.find("#limits_ip_edit").val());
	var diskLimit = trim($resourceLimitsTab.find("#limits_volume_edit").val());
	var snapshotLimit = trim($resourceLimitsTab.find("#limits_snapshot_edit").val());
	var templateLimit = trim($resourceLimitsTab.find("#limits_template_edit").val());
				
	if (instanceLimit != $resourceLimitsTab.find("#limits_vm").text()) {
		updateResourceLimitForDomain(domainId, 0, instanceLimit, $resourceLimitsTab.find("#limits_vm"));
	}
	if (ipLimit != $resourceLimitsTab.find("#limits_ip").text()) {
		updateResourceLimitForDomain(domainId, 1, ipLimit, $resourceLimitsTab.find("#limits_ip"));
	}
	if (diskLimit != $resourceLimitsTab.find("#limits_volume").text()) {
		updateResourceLimitForDomain(domainId, 2, diskLimit, $resourceLimitsTab.find("#limits_volume"));
	}
	if (snapshotLimit != $resourceLimitsTab.find("#limits_snapshot").text()) {
		updateResourceLimitForDomain(domainId, 3, snapshotLimit, $resourceLimitsTab.find("#limits_snapshot"));
	}
	if (templateLimit != $resourceLimitsTab.find("#limits_template").text()) {
		updateResourceLimitForDomain(domainId, 4, templateLimit, $resourceLimitsTab.find("#limits_template"));
	}    
	
	$editFields.hide();      
    $readonlyFields.show();       
    $("#save_button, #cancel_button").hide();      
}