function afterLoadDomainJSP() {
    var defaultRootDomainId = g_domainid;
    var defaultRootLevel = 0;	 
    var index = 1;	       
    var treeContentBox = $("#midmenu_container");      
    var treenodeTemplate = $("#treenode_template");	  	    
    var grid = $("#right_panel_grid");  
    var gridRowTemplate = $("#grid_row_template");  
    var gridContent = grid.find("#grid_content");	
	var gridHeader = grid.find("#grid_header");	    
	var rightPanelDetailContent = $("#right_panel_detail_content");	
	var rightPanelSearchResult = $("#right_panel_search_result");		    
	var rightPanelGrid = rightPanelDetailContent.find("#right_panel_grid");
	var domainDetail = rightPanelDetailContent.find("#domain_detail");	
	var submenuContent = $("#submenu_content_domains");
    var searchButton = submenuContent.find("#search_button");
    var searchInput = submenuContent.find("#search_input");
    var searchResultsContainer = submenuContent.find("#search_results_container");
    var searchResultTemplate = $("#search_result_template");
    //var breadcrumbBox = submenuContent.find("#breadcrumb_box");
    //var breadcrumbPieceTemplate = $("#breadcrumb_piece_template");
    var childParentMap = {};  //map childDomainId to parentDomainId
    var domainIdNameMap = {}; //map domainId to domainName
		
	var $detailsTab = $("#right_panel_content #tab_content_details");   
	var $resourceLimitsTab = $("#right_panel_content #tab_content_resource_limits");
	
	/*
	activateDialog($("#dialog_resource_limits").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	}));
	*/
  			    
    function drawNode(json, level, container) {		  
        if("parentdomainid" in json)
            childParentMap[json.id] = json.parentdomainid;	 //map childDomainId to parentDomainId   
        domainIdNameMap[json.id] = json.name;           //map domainId to domainName
    
        var template = treenodeTemplate.clone(true);	            
        template.attr("id", "domain_"+json.id);	 
        //template.data("domainId", json.id).data("domainName", fromdb(json.name)).data("domainLevel", level); 	   
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
		    data: createURL("command=listDomainChildren&id="+id+"&response=json&pageSize=-1"),
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
	
	function accountJSONToTemplate(json, template) {           
        if (index++ % 2 == 0) {
		    template.addClass("smallrow_odd");
	    } else {
		    template.addClass("smallrow_even");
	    }			    	        
	    template.find("#grid_row_cell1").text(json.domain);
	    template.find("#grid_row_cell2").text(json.name);		    	    
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
	    gridContent.empty();
	    index = 0;		    
	    rightPanelDetailContent.find("#loading_gridtable").show(); 		
	    var accountType = (domainId==1)? 1: 2; 	    		
	    $.ajax({
			cache: false,				
		       data: createURL("command=listAccounts&domainid="+domainId+"&accounttype="+accountType+"&response=json"+maxPageSize),
			dataType: "json",
			success: function(json) {
				var accounts = json.listaccountsresponse.account;					
				if (accounts != null && accounts.length > 0) {					    
					for (var i = 0; i < accounts.length; i++) {
						var template = gridRowTemplate.clone(true).attr("id","account"+accounts[i].id);
						accountJSONToTemplate(accounts[i], template);							
						gridContent.append(template.show());
					}						
				} 
			    rightPanelDetailContent.find("#loading_gridtable").hide();                  
			},
			error: function(XMLHttpResponse) {									
				handleError(XMLHttpResponse);
				rightPanelDetailContent.find("#loading_gridtable").hide(); 
			}			
		});		
	}
	
	treenodeTemplate.bind("click", function(event) {			     
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
				    if (accounts != null) {	
				        $detailsTab.find("#redirect_to_account_page").text(accounts.length);	
				        /*
				        $detailsTab.find("#redirect_to_account_page").bind("click", function() {
				            $("#menutab_role_root #menutab_accounts").data("domainId", domainId).click();
				        });	
				        */
				    }
				    else {
				        $detailsTab.find("#redirect_to_account_page").text("");
				        //$detailsTab.find("#redirect_to_account_page").unbind("click");	
				    }						   		                 
			    }		
		    });		 
		  			 				 			 
		    $.ajax({
			    cache: false,				
			    data: createURL("command=listVirtualMachines&domainid="+domainId+"&response=json"),
			    dataType: "json",
			    success: function(json) {
				    var instances = json.listvirtualmachinesresponse.virtualmachine;					
				    if (instances != null) {	
				        $detailsTab.find("#redirect_to_instance_page").text(instances.length);	
				        /*
				        $detailsTab.find("#redirect_to_instance_page").bind("click", function() {
				            $("#menutab_role_root #menutab_vm").data("domainId", domainId).click();
				        });	
				        */
				    }
				    else {
				        $detailsTab.find("#redirect_to_instance_page").text("");
				        //$detailsTab.find("#redirect_to_instance_page").unbind("click");	
				    }						   		                 
			    }		
		    });		 
		    			    
		    $.ajax({
			    cache: false,				
			    data: createURL("command=listVolumes&domainid="+domainId+"&response=json"),
			    dataType: "json",
			    success: function(json) {
				    var volumes = json.listvolumesresponse.volume;						
				    if (volumes != null) {	
				        $detailsTab.find("#redirect_to_volume_page").text(volumes.length);	
				        /*
				        $detailsTab.find("#redirect_to_volume_page").bind("click", function() {
				            $("#menutab_role_root #menutab_storage").data("domainId", domainId).data("targetTab", "submenu_volume").click();
				        });	
				        */
				    }	
				    else {
				        $detailsTab.find("#redirect_to_volume_page").text("");
				        //$detailsTab.find("#redirect_to_volume_page").unbind("click");	
				    }							   		                 
			    }		
		    });

			if (isAdmin() || (isDomainAdmin() && (g_domainid != domainId))) {
				$("#tab_resource_limits").show();
			
			    //???
				//$("#account_resource_limits").data("domainId", domainId).unbind("click").bind("click", function() {
					//debugger;
					$.ajax({
						cache: false,				
						data: createURL("command=listResourceLimits&domainid="+domainId+"&response=json"),
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
							
							/*
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
										updateResourceLimit(domainId, 0, instanceLimit);
									}
									if (ipLimit != preIpLimit) {
										updateResourceLimit(domainId, 1, ipLimit);
									}
									if (diskLimit != preDiskLimit) {
										updateResourceLimit(domainId, 2, diskLimit);
									}
									if (snapshotLimit != preSnapshotLimit) {
										updateResourceLimit(domainId, 3, snapshotLimit);
									}
									if (templateLimit != preTemplateLimit) {
										updateResourceLimit(domainId, 4, templateLimit);
									}
								}, 
								"Cancel": function() { 
									$(this).dialog("close"); 
								} 
							}).dialog("open");
							*/
							
							
						}
					});
					//return false;
				//});
				//???
				
				
			} else {
				$("#tab_resource_limits").hide();
			}
		 
		    rightPanelDetailContent.show();	
	        rightPanelSearchResult.hide();	 
		    			    
		    listAdminAccounts(domainId);  
		    rightPanelGrid.show();		    
		}
					
		return false;
    });
	
	/*
	searchResultTemplate.bind("click", function(event) {
	    var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.attr("id");	
		var domainId = template.data("domainId");			
		if(action=="domain_name") 			    
		    refreshWholeTree(domainId, defaultRootLevel);										
	});
	*/
	
	/*
	searchButton.bind("click", function(event) {
	    searchResultsContainer.empty();		
	    rightPanelDetailContent.hide();
	    rightPanelSearchResult.show();	                	        	
        var keyword = searchInput.val();             
        $.ajax({
	        data: createURL("command=listDomains&keyword="+keyword+"&response=json&pageSize=-1"), //pageSize=-1 will return all items (no limitation)
	        dataType: "json",
	        async: false,
	        success: function(json) {					        
	            var domains = json.listdomainsresponse.domain;			           			        	    
		        if (domains != null && domains.length > 0) {
		            for(var i=0; i<domains.length; i++) {
		                var template = searchResultTemplate.clone(true).attr("id", "searchresult"+domains[i].id).data("domainId", domains[i].id);
		                template.find("#domain_name").text(domains[i].name);
		                searchResultsContainer.append(template.show());
		            }
		        }    				
	        }
        }); 	            	        
        return false;
    });
	*/
	
	/*
    searchInput.bind("keypress", function(event) {		        
        if(event.keyCode == keycode_Enter) {                 		        
            searchButton.click();			
            return false;     
        }		    
    }); 
    */  	    

			
	//draw root node
	function drawRootNode(rootDomainId) {
	    treeContentBox.empty();
	    $.ajax({
	        data: createURL("command=listDomains&id="+rootDomainId+"&response=json&pageSize=-1"), //pageSize=-1 will return all items (no limitation)
	        dataType: "json",
	        async: false,
	        success: function(json) {					        
	            var domains = json.listdomainsresponse.domain;				        	    
		        if (domains != null && domains.length > 0) {				   					    
				    var node = drawNode(domains[0], defaultRootLevel, treeContentBox); 
				    
				    var treeLevelsbox = node.find(".tree_levelsbox");	//root node shouldn't have margin-left:20px				   
				    if(treeLevelsbox!=null && treeLevelsbox.length >0)
				        treeLevelsbox[0].style.marginLeft="0px";        //set root node's margin-left to 0px.
				}				
	        }
        }); 		
    }	
	
	/*
	breadcrumbPieceTemplate.bind("click", function(event) {	
	    var domainId = $(this).data("domainId");	
	    refreshWholeTree(domainId);
	});
	*/
	
	//draw breadcrumb all the way up
	/*
	function drawBreadcrumb(domainId) {		    
	    var domainName = domainIdNameMap[domainId];
	    if(domainName == null)
	        return;
	    
	    var onePiece = breadcrumbPieceTemplate.clone(true).attr("id", "breadcrumb_"+domainId).data("domainId", domainId).text(" > "+domainName);
	    breadcrumbBox.prepend(onePiece.show());		    
	    
	    var parentDomainId = childParentMap[domainId];
	    if(parentDomainId!=null)
	        drawBreadcrumb(parentDomainId);
	}
	*/
	
	function refreshWholeTree(rootDomainId, rootLevel) {
	    drawRootNode(rootDomainId);
	    drawTree(rootDomainId, (rootLevel+1), $("#domain_children_container_"+rootDomainId));  //draw the whole tree (under root node)			
	    $("#domain_"+rootDomainId).show();	//show root node
	    clickExpandIcon(rootDomainId);      //expand root node
	    
	    //breadcrumbBox.empty();
	    //drawBreadcrumb(rootDomainId);
	}
	
	refreshWholeTree(defaultRootDomainId, defaultRootLevel);
	
	 //***** switch to different tab (begin) ********************************************************************
    $("#tab_details").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_resource_limits").removeClass("on").addClass("off");  
        $("#tab_content_details").show();     
        $("#tab_content_resource_limits").hide();   
        return false;
    });
    
    $("#tab_resource_limits").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_details").removeClass("on").addClass("off");   
        $("#tab_content_resource_limits").show();    
        $("#tab_content_details").hide();    
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
