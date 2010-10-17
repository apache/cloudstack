function buildZoneTree() {      
    //***** build zone tree (begin) ***********************************************************************************************
    var forceLogout = true;  // We force a logout only if the user has first added a POD for the very first time     
    var $loading = $("#leftmenu_zone_tree").find("#loading_container").show();
    var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container").hide();
  
    $.ajax({
	    data: createURL("command=listZones&available=true"+maxPageSize),
		dataType: "json",		
		success: function(json) {
			var items = json.listzonesresponse.zone;
			var container = $zoneTree.empty();
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {
					var $zoneNode = $("#leftmenu_zone_node_template").clone(true);
					zoneJSONToTreeNode(items[i],$zoneNode);
					container.append($zoneNode.show());
				}
			}	
			$loading.hide();
            $zoneTree.show();
		}
	});  
    
	$("#leftmenu_zone_node_template").bind("click", function(event) {
		var template = $(this);
		var target = $(event.target);
		var action = target.attr("id");
		var id = template.data("id");
		var name = template.data("name");
		
		switch (action) {
			case "zone_arrow" :				  	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");					
					target.parent().parent().siblings("#zone_content").show();	
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");					
					target.parent().parent().siblings("#zone_content").hide();									
				}
				break;
											    
			case "pod_arrow" :		    	   
				if(target.hasClass("expanded_close")) {						
					target.removeClass("expanded_close").addClass("expanded_open");					
					target.parent().parent().siblings("#pod_content").show();	
				} 
				else if(target.hasClass("expanded_open")) {					
					target.removeClass("expanded_open").addClass("expanded_close");					
					target.parent().parent().siblings("#pod_content").hide();									
				}
				break;	
				
				
			case "zone_name":	
			    selectLeftMenu(target.parent().parent().parent());						    
			    var jsonObj = target.data("jsonObj");  
			    showPage($("#zone_page"), jsonObj);
			    zoneJsonToDetailsTab(jsonObj);
			    zoneJsonToNetworkTab(jsonObj);							    		   			    
			    break;		
			    	
			case "pod_name" :	
			    selectLeftMenu(target.parent().parent().parent());
			    var jsonObj = target.data("jsonObj");
			    showPage($("#pod_page"), jsonObj);		
			    podJsonToDetailsTab(jsonObj);				
				break;		
				    
			case "cluster_name" :	
			    selectLeftMenu(target.parent().parent().parent());			    
			    var jsonObj = target.data("jsonObj");
			    showPage($("#cluster_page"), jsonObj);
			    clusterJsonToDetailsTab(jsonObj);
			    var clusterId = jsonObj.id;
			    $("#midmenu_container").empty();
			    listItemsInMidmenu(("listHosts&clusterid="+clusterId), "listhostsresponse", "host", hostToMidmenu, hostToRigntPanel, getMidmenuId, false); 					
				listItemsInMidmenu(("listStoragePools&clusterid="+clusterId), "liststoragepoolsresponse", "storagepool", primarystorageToMidmenu, primarystorageToRigntPanel, getMidmenuId, false); 					
	    		break;								
						
			case "systemvm_name" :		
			    selectLeftMenu(target.parent().parent().parent());		
			    var jsonObj = target.data("jsonObj");	
			    showPage($("#systemvm_page"), jsonObj);					
				systemvmJsonToDetailsTab(jsonObj);			
				break;			
			
			default:
				break;
		}
		return false;
	});    	
	//***** build zone tree (end) *************************************************************************************************       
}    

function zoneJSONToTreeNode(json, $zoneNode) {
    var zoneid = json.id;
    $zoneNode.attr("id", "zone_" + zoneid);  
    $zoneNode.data("id", zoneid).data("name", fromdb(json.name));
    var zoneName = $zoneNode.find("#zone_name").text(fromdb(json.name));	    
    zoneName.data("jsonObj", json);	    
	
    $.ajax({
        data: createURL("command=listPods&zoneid="+zoneid+maxPageSize),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listpodsresponse.pod;			    
		    var container = $zoneNode.find("#pods_container");
		    if (items != null && items.length > 0) {					    
			    for (var i = 0; i < items.length; i++) {
				    var $podNode = $("#leftmenu_pod_node_template").clone(true);
				    podJSONToTreeNode(items[i], $podNode);
				    container.append($podNode.show());
				    forceLogout = false;  // We don't force a logout if pod(s) exit.
			    }
		    }
	    }
    });
    	    
    $.ajax({
        data: createURL("command=listSystemVms&zoneid="+zoneid+maxPageSize),
	    dataType: "json",
	    async: false,
	    success: function(json) {
		    var items = json.listsystemvmsresponse.systemvm;
		    var container = $zoneNode.find("#systemvms_container").empty();
		    if (items != null && items.length > 0) {					    
			    for (var i = 0; i < items.length; i++) {
				    var $systemvmNode = $("#leftmenu_systemvm_node_template").clone(true);
				    systemvmJSONToTreeNode(items[i], $systemvmNode);
				    container.append($systemvmNode.show());
			    }
		    }
	    }
    });
}

function podJSONToTreeNode(json, $podNode) {	
    var podid = json.id;
    $podNode.attr("id", "pod_" + podid);      	
	$podNode.data("id", podid).data("name", fromdb(json.name));
	
	var podName = $podNode.find("#pod_name").text(fromdb(json.name));
	podName.data("jsonObj", json);	    
		
    $.ajax({
        data: createURL("command=listClusters&podid="+podid+maxPageSize),
        dataType: "json",
        async: false,
        success: function(json) {
	        var items = json.listclustersresponse.cluster;
	        var container = $podNode.find("#clusters_container").empty();
	        if (items != null && items.length > 0) {					    
		        for (var i = 0; i < items.length; i++) {
			        var clusterTemplate = $("#leftmenu_cluster_node_template").clone(true); 
			        clusterJSONToTreeNode(items[i], clusterTemplate);
			        container.append(clusterTemplate.show());
		        }
	        }
        }
    });		
}
	
function systemvmJSONToTreeNode(json, $systemvmNode) {	
    var systemvmid = json.id;	
    $systemvmNode.attr("id", "systemvm_"+systemvmid);
    $systemvmNode.data("id", systemvmid).data("name", json.name);	     
    var systeymvmName = $systemvmNode.find("#systemvm_name").text(json.name);	    
    systeymvmName.data("jsonObj", json);	    		
}
		
function clusterJSONToTreeNode(json, $clusterNode) {
    $clusterNode.attr("id", "cluster_"+json.id);
    $clusterNode.data("id", json.id).data("name", fromdb(json.name));	    
    var clusterName = $clusterNode.find("#cluster_name").text(fromdb(json.name));
    clusterName.data("jsonObj", json);	   
}			

function showPage($pageToShow, jsonObj) {   
    var pageArray = [$("#zone_page"), $("#pod_page"), $("#cluster_page"), $("#host_page"), $("#primarystorage_page"), $("#systemvm_page")];
    var pageLabelArray = ["Zone", "Pod", "Cluster", "Host", "Primary Storage", "System VM"];       
   
    for(var i=0; i<pageArray.length; i++) {
        if(pageArray[i].attr("id") == $pageToShow.attr("id")) {
            $("#right_panel_header").find("#label").text(pageLabelArray[i]);
            pageArray[i].show();
        }
        else {
            pageArray[i].hide();
        }
        $pageToShow.data("jsonObj", jsonObj);
    }   
    
    if($pageToShow.attr("id") == "zone_page") {        
        initAddPodButton($("#midmenu_add_link"), jsonObj);  
        $("#midmenu_add2_link").unbind("click").hide();   
    }
    else if($pageToShow.attr("id") == "pod_page") {
        initAddHostButton($("#midmenu_add_link"), jsonObj);  
        initAddPrimaryStorageButton($("#midmenu_add2_link"), jsonObj);                
    }      
    else {
        $("#midmenu_add_link").unbind("click").hide();              
        $("#midmenu_add2_link").unbind("click").hide();   
    }
}

//***** zone page (begin) *****************************************************************************************************	
function zoneJsonToDetailsTab(jsonObj) {	    
    var $detailsTab = $("#zone_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#dns1").text(fromdb(jsonObj.dns1));
    $detailsTab.find("#dns2").text(fromdb(jsonObj.dns2));
    $detailsTab.find("#internaldns1").text(fromdb(jsonObj.internaldns1));
    $detailsTab.find("#internaldns2").text(fromdb(jsonObj.internaldns2));	
    $detailsTab.find("#vlan").text(fromdb(jsonObj.vlan));
    $detailsTab.find("#guestcidraddress").text(fromdb(jsonObj.guestcidraddress));     
}	  

function zoneJsonToNetworkTab(jsonObj) {	    
    var $networkTab = $("#zone_page").find("#tab_content_network");  
    $networkTab.find("#zone_cloud").find("#zone_name").text(fromdb(jsonObj.name));	 
    $networkTab.find("#zone_vlan").text(jsonObj.vlan);   
                  
    $.ajax({
	  data: createURL("command=listVlanIpRanges&zoneId="+jsonObj.id),
		dataType: "json",
		success: function(json) {
			var items = json.listvlaniprangesresponse.vlaniprange;		
			var $vlanContainer = $networkTab.find("#vlan_container").empty();   					
			if (items != null && items.length > 0) {					    
				for (var i = 0; i < items.length; i++) {	
				    var item = items[i];
				    					   
				    var $template1;
				    if(item.forvirtualnetwork == "false") 
				        $template1 = $("#direct_vlan_template").clone(); 
				    else
				    	$template1 = $("#virtual_vlan_template").clone();  					    
				    
				    vlanJsonToTemplate(item, $template1);
				    $vlanContainer.append($template1.show());											
				}
			}
		}
	});
}	 

function vlanJsonToTemplate(jsonObj, $template1) {
    $template1.data("jsonObj", jsonObj);
    $template1.find("#vlan_id").text(jsonObj.vlan);
    $template1.find("#ip_range").text(jsonObj.description);
} 	

//***** zone page (end) *******************************************************************************************************

//***** pod page (begin) ******************************************************************************************************
function podJsonToDetailsTab(jsonObj) {	    
    var $detailsTab = $("#pod_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#cidr").text(fromdb(jsonObj.cidr));        
    $detailsTab.find("#ipRange").text(getIpRange(jsonObj.startip, jsonObj.endip));
    $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway));  
    
    //if (getDirectAttachUntaggedEnabled() == "true") 
	//	$("#submenu_content_zones #action_add_directip_vlan").data("type", "pod").data("id", obj.id).data("name", obj.name).data("zoneid", obj.zoneid).show();		
}	
	
function getIpRange(startip, endip) {
    var ipRange = "";
	if (startip != null && startip.length > 0) {
		ipRange = startip;
	}
	if (endip != null && endip.length > 0) {
		ipRange = ipRange + " - " + endip;
	}		
	return ipRange;
}	

//***** pod page (end) ********************************************************************************************************

//***** cluster page (bgein) **************************************************************************************************
function clusterJsonToDetailsTab(jsonObj) {	    
    var $detailsTab = $("#cluster_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));        
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));            
}
//***** cluster page (end) ****************************************************************************************************

//***** host page (bgein) *****************************************************************************************************
function hostToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_host.png");      
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress.substring(0,25)); 
}

function hostToRigntPanel($midmenuItem1) {      
    var jsonObj = $midmenuItem1.data("jsonObj");
    hostJsonToDetailsTab(jsonObj);   
    showPage($("#host_page"));
}

function hostJsonToDetailsTab(jsonObj) {	    
    var $detailsTab = $("#host_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#state").text(fromdb(jsonObj.state));        
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));   
    $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));        
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress)); 
    $detailsTab.find("#version").text(fromdb(jsonObj.version));  
    $detailsTab.find("#oscategoryname").text(fromdb(jsonObj.oscategoryname));        
    $detailsTab.find("#disconnected").text(fromdb(jsonObj.disconnected));        
}
//***** host page (end) *******************************************************************************************************

//***** primary storage page (bgein) ******************************************************************************************
function primarystorageToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj);      
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_primarystorage.png");      
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.ipaddress.substring(0,25));          
}

function primarystorageToRigntPanel($midmenuItem1) {      
    var jsonObj = $midmenuItem1.data("jsonObj");
    primarystorageJsonToDetailsTab(jsonObj);   
    showPage($("#primarystorage_page"));
}

function primarystorageJsonToDetailsTab(jsonObj) {	    
    var $detailsTab = $("#primarystorage_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);           
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#podname").text(fromdb(jsonObj.podname));
    $detailsTab.find("#clustername").text(fromdb(jsonObj.clustername));
    $detailsTab.find("#type").text(fromdb(jsonObj.type));
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    $detailsTab.find("#path").text(fromdb(jsonObj.path));                
	$detailsTab.find("#disksizetotal").text(convertBytes(jsonObj.disksizetotal));
	$detailsTab.find("#disksizeallocated").text(convertBytes(jsonObj.disksizeallocated));
	$detailsTab.find("#tags").text(fromdb(jsonObj.tags));         
}
//***** primary storage page (end) *********************************************************************************************

//***** systemVM page (begin) *************************************************************************************************
function systemvmJsonToDetailsTab(jsonObj) {	   
    var $detailsTab = $("#systemvm_page").find("#tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);   
    
    $detailsTab.find("#state").text(fromdb(jsonObj.state));     
    $detailsTab.find("#systemvmtype").text(toSystemVMTypeText(jsonObj.systemvmtype));    
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename)); 
    $detailsTab.find("#id").text(fromdb(jsonObj.id));  
    $detailsTab.find("#name").text(fromdb(jsonObj.name));   
    $detailsTab.find("#activeviewersessions").text(fromdb(jsonObj.activeviewersessions)); 
    $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip)); 
    $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip)); 
    $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#gateway").text(fromdb(jsonObj.gateway)); 
    $detailsTab.find("#created").text(fromdb(jsonObj.created));             
}

function toSystemVMTypeText(value) {
    var text = "";
    if(value == "consoleproxy")
        text = "Console Proxy VM";
    else if(value == "secondarystoragevm")
        text = "Secondary Storage VM";
    return text;        
}
//***** systemVM page (end) ***************************************************************************************************

function afterSwitchToDetailsTab() {
    $("#midmenu_add2_link").hide();  
}
function afterSwitchToNetworkTab() {
    $("#midmenu_add2_link").find("#label").text("Add VLAN IP Range");  
    $("#midmenu_add2_link").show();
}
function afterSwitchToSecondaryStorageTab() {
    $("#midmenu_add2_link").find("#label").text("Add Secondary Storage").show();  
    $("#midmenu_add2_link").show();
}

function afterLoadResourceJSP() {	
	//switch between different tabs in zone page 
	var $zonePage = $("#zone_page");
    var tabArray = [$zonePage.find("#tab_details"), $zonePage.find("#tab_network"), $zonePage.find("#tab_secondary_storage")];
    var tabContentArray = [$zonePage.find("#tab_content_details"), $zonePage.find("#tab_content_network"), $zonePage.find("#tab_content_secondary_storage")];
    var afterSwitchFnArray = [afterSwitchToDetailsTab, afterSwitchToNetworkTab, afterSwitchToSecondaryStorageTab];
    switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray);     
      
	//dialogs	
	initDialog("dialog_add_zone");
	initDialog("dialog_add_pod", 320);
	initDialog("dialog_add_host");
	initDialog("dialog_add_pool");
	
	// if hypervisor is KVM, limit the server option to NFS for now
	if (getHypervisorType() == 'kvm') {
		$("#dialog_add_pool").find("#add_pool_protocol").empty().html('<option value="nfs">NFS</option>');
	}
	
    $("#dialog_add_pool").find("#add_pool_protocol").change(function(event) {
		if ($(this).val() == "iscsi") {
			$("#dialog_add_pool #add_pool_path_container").hide();
			$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").show();
		} else {
			$("#dialog_add_pool #add_pool_path_container").show();
			$("#dialog_add_pool #add_pool_iqn_container, #dialog_add_pool #add_pool_lun_container").hide();
		}
	});
		
	//initialize Add Zone button 
    initAddZoneButton($("#midmenu_add_link"));	
}

function nfsURL(server, path) {
    var url;
    if(server.indexOf("://")==-1)
	    url = "nfs://" + server + path;
	else
	    url = server + path;
	return url;
}

function iscsiURL(server, iqn, lun) {
    var url;
    if(server.indexOf("://")==-1)
	    url = "iscsi://" + server + iqn + "/" + lun;
	else
	    url = server + iqn + "/" + lun;
	return url;
}

function initAddZoneButton($midmenuAddLink1) {
    $midmenuAddLink1.find("#label").text("Add Zone");     
    $midmenuAddLink1.show();     
    $midmenuAddLink1.unbind("click").bind("click", function(event) {  
        $("#dialog_add_zone")
		.dialog('option', 'buttons', { 				
			"Add": function() { 
			    var thisDialog = $(this);
								
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_zone_name"), thisDialog.find("#add_zone_name_errormsg"));
				isValid &= validateIp("DNS 1", thisDialog.find("#add_zone_dns1"), thisDialog.find("#add_zone_dns1_errormsg"), false); //required
				isValid &= validateIp("DNS 2", thisDialog.find("#add_zone_dns2"), thisDialog.find("#add_zone_dns2_errormsg"), true);  //optional	
				isValid &= validateIp("Internal DNS 1", thisDialog.find("#add_zone_internaldns1"), thisDialog.find("#add_zone_internaldns1_errormsg"), false); //required
				isValid &= validateIp("Internal DNS 2", thisDialog.find("#add_zone_internaldns2"), thisDialog.find("#add_zone_internaldns2_errormsg"), true);  //optional	
				if (getNetworkType() != "vnet") {
					isValid &= validateString("Zone - Start VLAN Range", thisDialog.find("#add_zone_startvlan"), thisDialog.find("#add_zone_startvlan_errormsg"), false); //required
					isValid &= validateString("Zone - End VLAN Range", thisDialog.find("#add_zone_endvlan"), thisDialog.find("#add_zone_endvlan_errormsg"), true);        //optional
				}
				isValid &= validateCIDR("Guest CIDR", thisDialog.find("#add_zone_guestcidraddress"), thisDialog.find("#add_zone_guestcidraddress_errormsg"), false); //required
				if (!isValid) 
				    return;							
				
				thisDialog.dialog("close"); 
				
				var moreCriteria = [];	
				
				var name = trim(thisDialog.find("#add_zone_name").val());
				moreCriteria.push("&name="+todb(name));
				
				var dns1 = trim(thisDialog.find("#add_zone_dns1").val());
				moreCriteria.push("&dns1="+encodeURIComponent(dns1));
				
				var dns2 = trim(thisDialog.find("#add_zone_dns2").val());
				if (dns2 != null && dns2.length > 0) 
				    moreCriteria.push("&dns2="+encodeURIComponent(dns2));						
									
				var internaldns1 = trim(thisDialog.find("#add_zone_internaldns1").val());
				moreCriteria.push("&internaldns1="+encodeURIComponent(internaldns1));
				
				var internaldns2 = trim(thisDialog.find("#add_zone_internaldns2").val());
				if (internaldns2 != null && internaldns2.length > 0) 
				    moreCriteria.push("&internaldns2="+encodeURIComponent(internaldns2));						
				 											
				if (getNetworkType() != "vnet") {
					var vlanStart = trim(thisDialog.find("#add_zone_startvlan").val());	
					var vlanEnd = trim(thisDialog.find("#add_zone_endvlan").val());						
					if (vlanEnd != null && vlanEnd.length > 0) 
					    moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart + "-" + vlanEnd));									
					else 							
						moreCriteria.push("&vlan=" + encodeURIComponent(vlanStart));		
				}					
				
				var guestcidraddress = trim(thisDialog.find("#add_zone_guestcidraddress").val());
				moreCriteria.push("&guestcidraddress="+encodeURIComponent(guestcidraddress));	
						
				
				var template = $("#leftmenu_zone_node_template").clone(true); 
			    var loadingImg = template.find(".adding_loading");										
			    var row_container = template.find("#row_container");  			   
			    var $zoneTree = $("#leftmenu_zone_tree").find("#tree_container");		     			
			    $zoneTree.prepend(template);						            
                loadingImg.show();  
                row_container.hide();             
	            template.fadeIn("slow");
                $.ajax({
			        data: createURL("command=createZone"+moreCriteria.join("")),
				    dataType: "json",
				    success: function(json) {
					    var item = json.createzoneresponse;					    
					    zoneJSONToTreeNode(item, template);	
					    loadingImg.hide();	
					    row_container.show();		        
				    },
			        error: function(XMLHttpResponse) {
			            handleError(XMLHttpResponse);
			            template.slideUp("slow", function() {
						    $(this).remove();
					    });
			        }
			    });
			}, 
			"Cancel": function() { 
			    var thisDialog = $(this);
				thisDialog.dialog("close"); 
				cleanErrMsg(thisDialog.find("#add_zone_name"), thisDialog.find("#add_zone_name_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_dns1"), thisDialog.find("#add_zone_dns1_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_dns2"), thisDialog.find("#add_zone_dns2_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_internaldns1"), thisDialog.find("#add_zone_internaldns1_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_internaldns2"), thisDialog.find("#add_zone_internaldns2_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_startvlan"), thisDialog.find("#add_zone_startvlan_errormsg"));
				cleanErrMsg(thisDialog.find("#add_zone_guestcidraddress"), thisDialog.find("#add_zone_guestcidraddress_errormsg"));
			} 
		}).dialog("open");        
        return false;
    });     
}

function initAddPodButton($midmenuAddLink1, jsonObj) {
    $midmenuAddLink1.find("#label").text("Add Pod");  
    $midmenuAddLink1.data("jsonObj", jsonObj).show();   
    $midmenuAddLink1.unbind("click").bind("click", function(event) {    
        var zoneObj = $(this).data("jsonObj");		
        $("#dialog_add_pod").find("#add_pod_zone_name").text(fromdb(zoneObj.name));
        $("#dialog_add_pod #add_pod_name, #dialog_add_pod #add_pod_cidr, #dialog_add_pod #add_pod_startip, #dialog_add_pod #add_pod_endip, #add_pod_gateway").val("");
		
        $("#dialog_add_pod")
        .dialog('option', 'buttons', { 				
	        "Add": function() {		
	            var thisDialog = $(this);
						
		        // validate values
		        var isValid = true;					
		        isValid &= validateString("Name", thisDialog.find("#add_pod_name"), thisDialog.find("#add_pod_name_errormsg"));
		        isValid &= validateCIDR("CIDR", thisDialog.find("#add_pod_cidr"), thisDialog.find("#add_pod_cidr_errormsg"));	
		        isValid &= validateIp("Start IP Range", thisDialog.find("#add_pod_startip"), thisDialog.find("#add_pod_startip_errormsg"));  //required
		        isValid &= validateIp("End IP Range", thisDialog.find("#add_pod_endip"), thisDialog.find("#add_pod_endip_errormsg"), true);  //optional
		        isValid &= validateIp("Gateway", thisDialog.find("#add_pod_gateway"), thisDialog.find("#add_pod_gateway_errormsg"));  //required when creating
		        if (!isValid) 
		            return;			
                
                thisDialog.dialog("close"); 
                  
                var name = trim(thisDialog.find("#add_pod_name").val());
		        var cidr = trim(thisDialog.find("#add_pod_cidr").val());
		        var startip = trim(thisDialog.find("#add_pod_startip").val());
		        var endip = trim(thisDialog.find("#add_pod_endip").val());	    //optional
		        var gateway = trim(thisDialog.find("#add_pod_gateway").val());			

                var array1 = [];
                array1.push("&zoneId="+zoneObj.id);
                array1.push("&name="+todb(name));
                array1.push("&cidr="+encodeURIComponent(cidr));
                array1.push("&startIp="+encodeURIComponent(startip));
                if (endip != null && endip.length > 0)
                    array1.push("&endIp="+encodeURIComponent(endip));
                array1.push("&gateway="+encodeURIComponent(gateway));			
								    
		        var template = $("#leftmenu_pod_node_template").clone(true);
		        var loadingImg = template.find(".adding_loading");										
		        var row_container = template.find("#row_container");        				
		        $("#zone_" + zoneObj.id + " #zone_content").show();	
		        $("#zone_" + zoneObj.id + " #pods_container").prepend(template.show());						
		        $("#zone_" + zoneObj.id + " #zone_expand").removeClass().addClass("zonetree_openarrows");									            
                loadingImg.show();  
                row_container.hide();             
                template.fadeIn("slow");
				
		        $.ajax({
		          data: createURL("command=createPod"+array1.join("")), 
			        dataType: "json",
			        success: function(json) {
				        var item = json.createpodresponse; 	
			            podJSONToTreeNode(item, template);	
				        loadingImg.hide(); 								                            
                        row_container.show();
                        
                        forceLogout = false;  // We don't force a logout if pod(s) exit.
				        if (forceLogout) {
					        $("#dialog_confirmation")
						        .html("<p>You have successfully added your first Zone and Pod.  After clicking 'OK', this UI will automatically refresh to give you access to the rest of cloud features.</p>")
						        .dialog('option', 'buttons', { 
							        "OK": function() { 											
								        $(this).dialog("close");
								        window.location.reload();
							        } 
						        }).dialog("open");
				        }
			        },
		            error: function(XMLHttpResponse) {	
		                handleError(XMLHttpResponse);			    
			            template.slideUp("slow", function() {
					        $(this).remove();
				        });
		            }
		        });					
	        }, 
	        "Cancel": function() { 
		        $(this).dialog("close"); 
	        } 
        }).dialog("open");        
        return false;
    });            
}

function initAddHostButton($midmenuAddLink1, jsonObj) {
    $midmenuAddLink1.find("#label").text("Add Host");  
    $midmenuAddLink1.data("jsonObj", jsonObj).show(); 
    $midmenuAddLink1.unbind("click").bind("click", function(event) {     
        dialogAddHost = $("#dialog_add_host");  
        var podObj = $(this).data("jsonObj");	
        dialogAddHost.find("#zone_name").text(fromdb(podObj.zonename));  
        dialogAddHost.find("#pod_name").text(fromdb(podObj.name)); 
        dialogAddHost.find("#new_cluster_name").val("");
                          
        $.ajax({
	       data: createURL("command=listClusters&podid="+podObj.id+maxPageSize),
            dataType: "json",
            success: function(json) {			            
                var items = json.listclustersresponse.cluster;
                var clusterSelect = dialogAddHost.find("#cluster_select").empty();		
                if(items != null && items.length > 0) {			                
                    for(var i=0; i<items.length; i++) 			                    
                        clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");		      
                    dialogAddHost.find("input[value=existing_cluster_radio]").attr("checked", true);
                }
                else {
				    clusterSelect.append("<option value='-1'>None Available</option>");
                    dialogAddHost.find("input[value=new_cluster_radio]").attr("checked", true);
                }
            }
        });           
	        	    
        dialogAddHost
        .dialog('option', 'buttons', { 				
	        "Add": function() { 
	            var dialogBox = $(this);				   
		        var clusterRadio = dialogBox.find("input[name=cluster]:checked").val();				
			
		        // validate values
		        var isValid = true;									
		        isValid &= validateString("Host name", dialogBox.find("#host_hostname"), dialogBox.find("#host_hostname_errormsg"));
		        isValid &= validateString("User name", dialogBox.find("#host_username"), dialogBox.find("#host_username_errormsg"));
		        isValid &= validateString("Password", dialogBox.find("#host_password"), dialogBox.find("#host_password_errormsg"));						
		        if (!isValid) 
		            return;
		            
				dialogBox.dialog("close");    				
				
		        var array1 = [];    
		        array1.push("&zoneId="+podObj.zoneid);
		        array1.push("&podId="+podObj.id);
						      
		        var username = trim(dialogBox.find("#host_username").val());
		        array1.push("&username="+encodeURIComponent(username));
				
		        var password = trim(dialogBox.find("#host_password").val());
		        array1.push("&password="+encodeURIComponent(password));
											
			    if(clusterRadio == "new_cluster_radio") {
		            var newClusterName = trim(dialogBox.find("#new_cluster_name").val());
		            array1.push("&clustername="+todb(newClusterName));				    
		        }
		        else if(clusterRadio == "existing_cluster_radio") {			            
		            var clusterId = dialogBox.find("#cluster_select").val();
				    // We will default to no cluster if someone selects Join Cluster with no cluster available.
				    if (clusterId != '-1') {
					    array1.push("&clusterid="+clusterId);
				    }
		        }				
				
		        var hostname = trim(dialogBox.find("#host_hostname").val());
		        var url;					
		        if(hostname.indexOf("http://")==-1)
		            url = "http://" + todb(hostname);
		        else
		            url = hostname;
		        array1.push("&url="+encodeURIComponent(url));
									
		        var $midmenuItem1 = beforeAddingMidMenuItem() ;    				
		        
		        $.ajax({
			       data: createURL("command=addHost" + array1.join("")),
			        dataType: "json",
			        success: function(json) {	
			            var items = json.addhostresponse.host;				            			      										   
					    hostToMidmenu(items[0], $midmenuItem1);
	                    bindClickToMidMenu($midmenuItem1, hostToRigntPanel, getMidmenuId);  
	                    afterAddingMidMenuItem($midmenuItem1, true);
                                                        
                        if(items.length > 1) { 
                            for(var i=1; i<items.length; i++) {                                    
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                hostToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, hostToRigntPanel, getMidmenuId); 
                                $("#midmenu_container").append($midmenuItem2.show());                                   
                            }	
                        }                                
                        
                        if(clusterRadio == "new_cluster_radio")
                            dialogBox.find("#new_cluster_name").val("");
			        },			
                    error: function(XMLHttpResponse) {		                   
                        handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1);					    
                    }				
		        });
	        }, 
	        "Cancel": function() { 
		        $(this).dialog("close"); 
	        } 
        }).dialog("open");            
        return false;
    });        
}

function initAddPrimaryStorageButton($midmenuAddLink2, jsonObj) {
    $midmenuAddLink2.find("#label").text("Add Primary Storage"); 
    $midmenuAddLink2.data("jsonObj", jsonObj).show();   
    $midmenuAddLink2.unbind("click").bind("click", function(event) {                 
        dialogAddPool = $("#dialog_add_pool");  
        var podObj = $(this).data("jsonObj");	
        dialogAddPool.find("#zone_name").text(fromdb(podObj.zonename));  
        dialogAddPool.find("#pod_name").text(fromdb(podObj.name)); 
                                
        var clusterSelect = $("#dialog_add_pool").find("#pool_cluster").empty();			            
	    $.ajax({
		    data: createURL("command=listClusters&podid=" + podObj.id),
	        dataType: "json",
	        success: function(json) {				                        
	            var items = json.listclustersresponse.cluster;
	            if(items != null && items.length > 0) {				                		                
	                for(var i=0; i<items.length; i++) 			                    
	                    clusterSelect.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");		                
	            }			            
	        }
	    });		   
        
        $("#dialog_add_pool")
	    .dialog('option', 'buttons', { 				    
		    "Add": function() { 	
		    	var thisDialog = $(this);
		    	
			    // validate values
				var protocol = thisDialog.find("#add_pool_protocol").val();
				
			    var isValid = true;						    
			    isValid &= validateDropDownBox("Cluster", thisDialog.find("#pool_cluster"), thisDialog.find("#pool_cluster_errormsg"), false);  //required, reset error text					    				
			    isValid &= validateString("Name", thisDialog.find("#add_pool_name"), thisDialog.find("#add_pool_name_errormsg"));
			    isValid &= validateString("Server", thisDialog.find("#add_pool_nfs_server"), thisDialog.find("#add_pool_nfs_server_errormsg"));	
				if (protocol == "nfs") {
					isValid &= validateString("Path", thisDialog.find("#add_pool_path"), thisDialog.find("#add_pool_path_errormsg"));	
				} else {
					isValid &= validateString("Target IQN", thisDialog.find("#add_pool_iqn"), thisDialog.find("#add_pool_iqn_errormsg"));	
					isValid &= validateString("LUN #", thisDialog.find("#add_pool_lun"), thisDialog.find("#add_pool_lun_errormsg"));	
				}
				isValid &= validateString("Tags", thisDialog.find("#add_pool_tags"), thisDialog.find("#add_pool_tags_errormsg"), true);	//optional
			    if (!isValid) 
			        return;
			        
			    thisDialog.dialog("close");    
				
				var $midmenuItem1 = beforeAddingMidMenuItem() ;    	
				
				var array1 = [];
				array1.push("&zoneId="+podObj.zoneid);
		        array1.push("&podId="+podObj.id);
				
				var clusterId = thisDialog.find("#pool_cluster").val();
			    array1.push("&clusterid="+clusterId);	
				
			    var name = trim(thisDialog.find("#add_pool_name").val());
			    array1.push("&name="+todb(name));
			    
			    var server = trim(thisDialog.find("#add_pool_nfs_server").val());						
				
				var url = null;
				if (protocol == "nfs") {
					var path = trim(thisDialog.find("#add_pool_path").val());
					if(path.substring(0,1)!="/")
						path = "/" + path; 
					url = nfsURL(server, path);
				} else {
					var iqn = trim(thisDialog.find("#add_pool_iqn").val());
					if(iqn.substring(0,1)!="/")
						iqn = "/" + iqn; 
					var lun = trim(thisDialog.find("#add_pool_lun").val());
					url = iscsiURL(server, iqn, lun);
				}
				array1.push("&url="+encodeURIComponent(url));
				
			    var tags = trim(thisDialog.find("#add_pool_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));				    
			    
			    $.ajax({
				    data: createURL("command=createStoragePool&response=json" + array1.join("")),
				    dataType: "json",
				    success: function(json) {					        
				        var item = json.createstoragepoolresponse;				            			      										   
					    primarystorageToMidmenu(item, $midmenuItem1);
	                    bindClickToMidMenu($midmenuItem1, primarystorageToRigntPanel, getMidmenuId);  
	                    afterAddingMidMenuItem($midmenuItem1, true);
				    },			
                    error: function(XMLHttpResponse) {	
                        handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1);			                        					    
                    }							    
			    });
		    }, 
		    "Cancel": function() { 
			    $(this).dialog("close"); 
		    } 
	    }).dialog("open");            
        return false;
    });             
}
