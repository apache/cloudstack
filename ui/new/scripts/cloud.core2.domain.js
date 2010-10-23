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

function afterLoadDomainJSP() {   
	//***** switch between different tabs (begin) ********************************************************************
    var tabArray = [$("#tab_details"), $("#tab_resource_limits"), $("#tab_admin_account")];
    var tabContentArray = [$("#tab_content_details"), $("#tab_content_resource_limits"), $("#tab_content_admin_account")];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
    //***** switch between different tabs (end) **********************************************************************
	    
    //edit button ***    
    var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");	
    var $readonlyFields  =  $resourceLimitsTab.find("#limits_vm, #limits_ip, #limits_volume, #limits_snapshot, #limits_template");
    var $editFields =  $resourceLimitsTab.find("#limits_vm_edit, #limits_ip_edit, #limits_volume_edit, #limits_snapshot_edit, #limits_template_edit");   
    initializeEditFunction($readonlyFields, $editFields, doUpdateResourceLimits);    
}


function refreshWholeTree(rootDomainId, rootLevel) {
    drawRootNode(rootDomainId);
    drawTree(rootDomainId, (rootLevel+1), $("#domain_children_container_"+rootDomainId));  //draw the whole tree (under root node)			
    $("#domain_"+rootDomainId).show();	//show root node
    clickExpandIcon(rootDomainId);      //expand root node	    
}

//draw root node
function drawRootNode(rootDomainId) {
    $("#leftmenu_domain_tree").empty();
    $.ajax({
        data: createURL("command=listDomains&id="+rootDomainId+"&pageSize=-1"), //pageSize=-1 will return all items (no limitation)
        dataType: "json",
        async: false,
        success: function(json) {					        
            var domains = json.listdomainsresponse.domain;				        	    
	        if (domains != null && domains.length > 0) {				   					    
			    var node = drawNode(domains[0], defaultRootLevel, $("#leftmenu_domain_tree")); 
			    
			    var treeLevelsbox = node.find(".tree_levelsbox");	//root node shouldn't have margin-left:20px				   
			    if(treeLevelsbox!=null && treeLevelsbox.length >0)
			        treeLevelsbox[0].style.marginLeft="0px";        //set root node's margin-left to 0px.
			}				
        }
    }); 		
}

function drawNode(json, level, container) {		  
    if("parentdomainid" in json)
        childParentMap[json.id] = json.parentdomainid;	//map childDomainId to parentDomainId   
    domainIdNameMap[json.id] = json.name;               //map domainId to domainName

    var template = $("#domain_tree_node_template").clone(true);	  
    template.find("#domain_indent").css("marginLeft", (30*(level+1)));           
    template.attr("id", "domain_"+json.id);	         
    template.data("jsonObj", json).data("domainLevel", level); 	      
    template.find("#domain_title_container").attr("id", "domain_title_container_"+json.id); 	        
    template.find("#domain_expand_icon").attr("id", "domain_expand_icon_"+json.id); 
    template.find("#domain_name").attr("id", "domain_name_"+json.id).text(json.name);        	              	
    template.find("#domain_children_container").attr("id", "domain_children_container_"+json.id);          
    container.append(template.show());	 
    return template;   	       
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
				    if(domains[i].haschild == true || domains[i].haschild == "true") //After API refactor, returned boolean value is true/false instead of "true"/"false". For testing convenience (Some people might not have backend update-to-date), check both true and "true".
		                drawTree(domains[i].id, (level+1), $("#domain_children_container_"+domains[i].id));				   
			    }
		    }				
	    }
    }); 
}	

function clickExpandIcon(domainId) {
    var template = $("#domain_"+domainId);
    var expandIcon = template.find("#domain_expand_icon_"+domainId);
    if (expandIcon.hasClass("expanded_close")) {													
		template.find("#domain_children_container_"+domainId).show();							
		expandIcon.removeClass("expanded_close").addClass("expanded_open");
	} 
	else if (expandIcon.hasClass("expanded_open")) {																	
	    template.find("#domain_children_container_"+domainId).hide();						
		expandIcon.removeClass("expanded_open").addClass("expanded_close");
	}			
}					

function domainAccountJSONToTemplate(jsonObj, $template) {   
    $template.data("jsonObj", jsonObj);  
    $template.find("#title").text(fromdb(jsonObj.name));
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

function domainToRightPanel(jsonObj) {  
    if($("#domain_grid_container").length == 0) { //domain.jsp is not loaded in right panel        
        $("#right_panel").load("jsp/domain.jsp", function(){         
            //switch between different tabs
            var tabArray = [$("#tab_details"), $("#tab_resource_limits"), $("#tab_admin_account")];
            var tabContentArray = [$("#tab_content_details"), $("#tab_content_resource_limits"), $("#tab_content_admin_account")];
            switchBetweenDifferentTabs(tabArray, tabContentArray);       
              
            //initiailize edit button 
            var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");	
            var $readonlyFields  =  $resourceLimitsTab.find("#limits_vm, #limits_ip, #limits_volume, #limits_snapshot, #limits_template");
            var $editFields =  $resourceLimitsTab.find("#limits_vm_edit, #limits_ip_edit, #limits_volume_edit, #limits_snapshot_edit, #limits_template_edit");   
            initializeEditFunction($readonlyFields, $editFields, doUpdateResourceLimits);    
                
			domainToRightPanel2(jsonObj);       
		});        
    }
    else {        
        domainToRightPanel2(jsonObj); 
    }
}

function domainToRightPanel2(jsonObj) {
    var $detailsTab = $("#right_panel_content #tab_content_details");
    $detailsTab.data("jsonObj", jsonObj);  
    var domainId = jsonObj.id;
    $detailsTab.find("#id").text(domainId);
    $detailsTab.find("#name").text(jsonObj.name);		   
			  	
  	$.ajax({
	    cache: false,				
	    data: createURL("command=listAccounts&domainid="+domainId+maxPageSize),
	    dataType: "json",
	    success: function(json) {				       
		    var accounts = json.listaccountsresponse.account;					
		    if (accounts != null) 	
		        $detailsTab.find("#redirect_to_account_page").text(accounts.length);
		    else 
		        $detailsTab.find("#redirect_to_account_page").text("");		
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
		        $detailsTab.find("#redirect_to_instance_page").text("");	
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
		        $detailsTab.find("#redirect_to_volume_page").text("");		
	    }		
    });

    listAdminAccounts(domainId);  

	if (isAdmin() || (isDomainAdmin() && (g_domainid != domainId))) {		
		//comment this out until 6697(resourcetype parameter of ListResourceLimitsCmd should be optional instead of required) is fixed.
		/*	
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
		*/		
		
		$("#tab_resource_limits").show();	
	} 
	else {
		$("#tab_resource_limits").hide();
	}		 		 
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
		else if(action.indexOf("domain_name")!=-1) {
            if($selectedDomainTreeNode != null && $selectedDomainTreeNode.data("jsonObj") != null)
                $selectedDomainTreeNode.find("#domain_title_container_"+$selectedDomainTreeNode.data("jsonObj").id).removeClass("selected");      
            $thisNode.find("#domain_title_container_"+domainId).addClass("selected");
            $selectedDomainTreeNode = $thisNode;            
            domainToRightPanel(jsonObj);                   
		}					
		return false;
    });
}

function updateResourceLimit(domainId, type, max, $readonlyField) {
	$.ajax({
	    data: createURL("command=updateResourceLimit&domainid="+domainId+"&resourceType="+type+"&max="+max),
		dataType: "json",
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

function doUpdateResourceLimits() {
    var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");

    var isValid = true;	        			
	isValid &= validateNumber("Instance Limit", $resourceLimitsTab.find("#limits_vm_edit"), $resourceLimitsTab.find("#limits_vm_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Public IP Limit", $resourceLimitsTab.find("#limits_ip_edit"), $resourceLimitsTab.find("#limits_ip_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Disk Volume Limit", $resourceLimitsTab.find("#limits_volume_edit"), $resourceLimitsTab.find("#limits_volume_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Snapshot Limit", $resourceLimitsTab.find("#limits_snapshot_edit"), $resourceLimitsTab.find("#limits_snapshot_edit_errormsg"), -1, 32000, false);
	isValid &= validateNumber("Template Limit", $resourceLimitsTab.find("#limits_template_edit"), $resourceLimitsTab.find("#limits_template_edit_errormsg"), -1, 32000, false);
	if (!isValid) 
	    return;
								
	var jsonObj = $detailsTab.data("jsonObj");
	var domainId = jsonObj.id;
	
	var instanceLimit = trim($resourceLimitsTab.find("#limits_vm_edit").val());
	var ipLimit = trim($resourceLimitsTab.find("#limits_ip_edit").val());
	var diskLimit = trim($resourceLimitsTab.find("#limits_volume_edit").val());
	var snapshotLimit = trim($resourceLimitsTab.find("#limits_snapshot_edit").val());
	var templateLimit = trim($resourceLimitsTab.find("#limits_template_edit").val());
				
	if (instanceLimit != $resourceLimitsTab.find("#limits_vm").text()) {
		updateResourceLimit(domainId, 0, instanceLimit, $resourceLimitsTab.find("#limits_vm"));
	}
	if (ipLimit != $resourceLimitsTab.find("#limits_ip").text()) {
		updateResourceLimit(domainId, 1, ipLimit, $resourceLimitsTab.find("#limits_ip"));
	}
	if (diskLimit != $resourceLimitsTab.find("#limits_volume").text()) {
		updateResourceLimit(domainId, 2, diskLimit, $resourceLimitsTab.find("#limits_volume"));
	}
	if (snapshotLimit != $resourceLimitsTab.find("#limits_snapshot").text()) {
		updateResourceLimit(domainId, 3, snapshotLimit, $resourceLimitsTab.find("#limits_snapshot"));
	}
	if (templateLimit != $resourceLimitsTab.find("#limits_template").text()) {
		updateResourceLimit(domainId, 4, templateLimit, $resourceLimitsTab.find("#limits_template"));
	}    
}