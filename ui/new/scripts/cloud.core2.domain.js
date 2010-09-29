function afterLoadDomainJSP() {
    var defaultRootDomainId = g_domainid;
    var defaultRootLevel = 0;	   
    var childParentMap = {};  //map childDomainId to parentDomainId
    var domainIdNameMap = {}; //map domainId to domainName
    
    var $treeContentBox = $("#midmenu_container");      
    var $treenodeTemplate = $("#treenode_template");	 
	var $detailsTab = $("#right_panel_content #tab_content_details");   
	var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");
	  			    
    function drawNode(json, level, container) {		  
        if("parentdomainid" in json)
            childParentMap[json.id] = json.parentdomainid;	//map childDomainId to parentDomainId   
        domainIdNameMap[json.id] = json.name;               //map domainId to domainName
    
        var template = $treenodeTemplate.clone(true);	            
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
					    if(domains[i].haschild=="true")
			                drawTree(domains[i].id, (level+1), $("#domain_children_container_"+domains[i].id));				   
				    }
			    }				
		    }
	    }); 
	}	
		
	function clickExpandIcon(domainId) {
	    var template = $("#domain_"+domainId);
	    var expandIcon = template.find("#domain_expand_icon_"+domainId);
	    if (expandIcon.hasClass("zonetree_closedarrows")) {													
			template.find("#domain_children_container_"+domainId).show();							
			expandIcon.removeClass().addClass("zonetree_openarrows");
		} else {																	
		    template.find("#domain_children_container_"+domainId).hide();						
			expandIcon.removeClass().addClass("zonetree_closedarrows");
		}			
	}					
	
	function accountJSONToTemplate(jsonObj, $template) {   
        $template.data("jsonObj", jsonObj);  
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
				if (items != null && items.length > 0) {	
				    var $container = $("#right_panel_content #tab_content_admin_account").empty();
					var $template = $("#admin_account_tab_template");				
					for (var i = 0; i < items.length; i++) {
						var $newTemplate = $template.clone(true);
		                accountJSONToTemplate(items[i], $newTemplate); 
		                $container.append($newTemplate.show());	
					}				    				
				} 			         
			}		
		});		
	}
	
	$treenodeTemplate.bind("click", function(event) {			     
		var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.attr("id");
		var jsonObj = template.data("jsonObj");	
		var domainId = jsonObj.id;	
		var domainName = jsonObj.name;										
		if (action.indexOf("domain_expand_icon")!=-1) {		
		    clickExpandIcon(domainId);					
		}
		else if(action.indexOf("domain_name")!=-1) {
            $detailsTab.data("jsonObj", jsonObj);  
            $detailsTab.find("#id").text(domainId);
            $detailsTab.find("#name").text(domainName);		   
					  	
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
				$("#tab_resource_limits").show();			
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
			} 
			else {
				$("#tab_resource_limits").hide();
			}		 		   
		}					
		return false;
    });
		
	//draw root node
	function drawRootNode(rootDomainId) {
	    $treeContentBox.empty();
	    $.ajax({
	        data: createURL("command=listDomains&id="+rootDomainId+"&pageSize=-1"), //pageSize=-1 will return all items (no limitation)
	        dataType: "json",
	        async: false,
	        success: function(json) {					        
	            var domains = json.listdomainsresponse.domain;				        	    
		        if (domains != null && domains.length > 0) {				   					    
				    var node = drawNode(domains[0], defaultRootLevel, $treeContentBox); 
				    
				    var treeLevelsbox = node.find(".tree_levelsbox");	//root node shouldn't have margin-left:20px				   
				    if(treeLevelsbox!=null && treeLevelsbox.length >0)
				        treeLevelsbox[0].style.marginLeft="0px";        //set root node's margin-left to 0px.
				}				
	        }
        }); 		
    }	
	
	function refreshWholeTree(rootDomainId, rootLevel) {
	    drawRootNode(rootDomainId);
	    drawTree(rootDomainId, (rootLevel+1), $("#domain_children_container_"+rootDomainId));  //draw the whole tree (under root node)			
	    $("#domain_"+rootDomainId).show();	//show root node
	    clickExpandIcon(rootDomainId);      //expand root node	    
	}
	
	refreshWholeTree(defaultRootDomainId, defaultRootLevel);
	
	 //***** switch to different tab (begin) ********************************************************************
    $("#tab_details").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_resource_limits, #tab_admin_account").removeClass("on").addClass("off");  
        $("#tab_content_details").show();     
        $("#tab_content_resource_limits, #tab_content_admin_account").hide();   
        return false;
    });
    
    $("#tab_resource_limits").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_details, #tab_admin_account").removeClass("on").addClass("off");   
        $("#tab_content_resource_limits").show();    
        $("#tab_content_details, #tab_content_admin_account").hide();    
        return false;
    });
    
    $("#tab_admin_account").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_details, #tab_resource_limits").removeClass("on").addClass("off");   
        $("#tab_content_admin_account").show();    
        $("#tab_content_details, #tab_content_resource_limits").hide();    
        return false;
    });
    //***** switch to different tab (end) ********************************************************************** 
    
    //edit button ***    
    var $readonlyFields  =  $resourceLimitsTab.find("#limits_vm, #limits_ip, #limits_volume, #limits_snapshot, #limits_template");
    var $editFields =  $resourceLimitsTab.find("#limits_vm_edit, #limits_ip_edit, #limits_volume_edit, #limits_snapshot_edit, #limits_template_edit");   
    initializeEditFunction($readonlyFields, $editFields, doUpdateResourceLimits); 
    
    function doUpdateResourceLimits() {
        var isValid = true;	        			
		isValid &= validateNumber("Instance Limit", $resourceLimitsTab.find("#limits_vm_edit"), $resourceLimitsTab.find("#limits_vm_edit_errormsg"), -1, 32000, false);
		isValid &= validateNumber("Public IP Limit", $resourceLimitsTab.find("#limits_ip_edit"), $resourceLimitsTab.find("#limits_ip_edit_errormsg"), -1, 32000, false);
		isValid &= validateNumber("Disk Volume Limit", $resourceLimitsTab.find("#limits_volume_edit"), $resourceLimitsTab.find("#limits_volume_edit_errormsg"), -1, 32000, false);
		isValid &= validateNumber("Snapshot Limit", $resourceLimitsTab.find("#limits_snapshot_edit"), $resourceLimitsTab.find("#limits_snapshot_edit_errormsg"), -1, 32000, false);
		isValid &= validateNumber("Template Limit", $resourceLimitsTab.find("#limits_template_edit"), $resourceLimitsTab.find("#limits_template_edit_errormsg"), -1, 32000, false);
		if (!isValid) return;
									
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
}
