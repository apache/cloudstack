var xsToolsIsoId = 200;

var g_zoneIds = []; 
var g_zoneNames = [];	

function isoGetSearchParams() {
    var moreCriteria = [];	

	var searchInput = $("#basic_search").find("#search_input").val();	 
    if (searchInput != null && searchInput.length > 0) {	           
        moreCriteria.push("&keyword="+todb(searchInput));	       
    }     

	var $advancedSearchPopup = getAdvancedSearchPopupInSearchContainer();
	if ($advancedSearchPopup.length > 0 && $advancedSearchPopup.css("display") != "none" ) {   
		var zone = $advancedSearchPopup.find("#adv_search_zone").val();	
	    if (zone!=null && zone.length > 0) 
			moreCriteria.push("&zoneId="+zone);	
				
        if ($advancedSearchPopup.find("#adv_search_domain_li").css("display") != "none"
	        && $advancedSearchPopup.find("#domain").hasClass("textwatermark") == false) {
	        var domainName = $advancedSearchPopup.find("#domain").val();
	        if (domainName != null && domainName.length > 0) { 	
				var domainId;							    
			    if(autoCompleteDomains != null && autoCompleteDomains.length > 0) {									
				    for(var i=0; i < autoCompleteDomains.length; i++) {					        
				      if(fromdb(autoCompleteDomains[i].name).toLowerCase() == domainName.toLowerCase()) {
				          domainId = autoCompleteDomains[i].id;
				          break;	
				      }
			        } 					   			    
			    } 	     	
	            if(domainId == null) { 
			        showError(false, $advancedSearchPopup.find("#domain"), $advancedSearchPopup.find("#domain_errormsg"), g_dictionary["label.not.found"]);
			    }
			    else { //e.g. domainId == 5 (number)
			        showError(true, $advancedSearchPopup.find("#domain"), $advancedSearchPopup.find("#domain_errormsg"), null)
			        moreCriteria.push("&domainid="+todb(domainId));	
			    }
			}
	    }
    	
		if ($advancedSearchPopup.find("#adv_search_account_li").css("display") != "none" 
    	    && $advancedSearchPopup.find("#adv_search_account").hasClass("textwatermark") == false) {	
		    var account = $advancedSearchPopup.find("#adv_search_account").val();		
		    if (account!=null && account.length > 0) 
			    moreCriteria.push("&account="+account);		
		}	
	} 	
	
	return moreCriteria.join("");          
}

function afterLoadIsoJSP() {   
    initDialog("dialog_copy_iso", 300);    
    initDialog("dialog_download_ISO");
    
    initAddIsoDialog();
    initCreateVmFromIsoDialog();
    
    var $detailsTab = $("#tab_content_details");
    if(isAdmin()) {
		$readonlyFields  = $detailsTab.find("#name, #displaytext, #ispublic, #ostypename, #isfeatured");
		$editFields = $detailsTab.find("#name_edit, #displaytext_edit, #ispublic_edit, #ostypename_edit, #isfeatured_edit"); 
    }
    else {  
		if (getUserPublicTemplateEnabled() == "true") {
			$readonlyFields  = $detailsTab.find("#name, #displaytext, #ispublic, #ostypename");
			$editFields = $detailsTab.find("#name_edit, #displaytext_edit, #ispublic_edit, #ostypename_edit"); 
		} else {
			$readonlyFields  = $detailsTab.find("#name, #displaytext, #ostypename");
			$editFields = $detailsTab.find("#name_edit, #displaytext_edit, #ostypename_edit"); 
		}
    }           
}

function initAddIsoDialog() {
    initDialog("dialog_add_iso", 450);   

    var $dialogAddIso = $("#dialog_add_iso");
    var $detailsTab = $("#right_panel_content").find("#tab_content_details");             
    
    if(isAdmin())
	    $dialogAddIso.find("#isfeatured_container").show();
	else
	    $dialogAddIso.find("#isfeatured_container").hide();		
    
    var addIsoZoneField = $dialogAddIso.find("#add_iso_zone");    	
	if (isAdmin())  
		addIsoZoneField.append("<option value='-1'>All Zones</option>"); 	
    $.ajax({
        data: createURL("command=listZones&available=true"),
	    dataType: "json",
	    success: function(json) {	
    	    g_zoneIds = [];
			g_zoneNames = [];
		    var zones = json.listzonesresponse.zone;	 			     			    	
		    if (zones != null && zones.length > 0) {
		        for (var i = 0; i < zones.length; i++) {
			        addIsoZoneField.append("<option value='" + zones[i].id + "'>" + fromdb(zones[i].name) + "</option>"); 			        
			        g_zoneIds.push(zones[i].id);
			        g_zoneNames.push(zones[i].name);			       
		        }
		    }				    			
	    }
	});	
    
    $.ajax({
	    data: createURL("command=listOsTypes"),
		dataType: "json",
		async: false,
		success: function(json) {		
		    var osTypeDropDownAdd = $dialogAddIso.find("#add_iso_os_type").empty();
			var osTypeDropdownEdit = $detailsTab.find("#ostypename_edit").empty();
		    
		    var html = "<option value=''>" + g_dictionary["label.none"] +  "</option>";
			osTypeDropDownAdd.append(html);			
			//osTypeDropdownEdit.append(html);	//OSType is required for ISO. So, shouldn't provide "none" option when updating ISO.
		
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {				
				for (var i = 0; i < types.length; i++) {
					var html = "<option value='" + types[i].id + "'>" + fromdb(types[i].description) + "</option>";
					osTypeDropDownAdd.append(html);			
					osTypeDropdownEdit.append(html);					
				}
			}	
		}
	});
    
    $dialogAddIso.find("#add_iso_bootable").unbind("change").bind("change", function(event) {        
        if($(this).val() == "true") {
            $dialogAddIso.find("#add_iso_os_type_container").show(); 
        }
        else {  //$(this).val() == "false"
            $dialogAddIso.find("#add_iso_os_type_container").hide();
            $dialogAddIso.find("#add_iso_os_type").val(""); //set OS Type back to "None"
        }
        
        return false;
    });
    
    //add button ***     
    $("#add_iso_button").unbind("click").bind("click", function(event) {  
		if (getUserPublicTemplateEnabled() == "true" || isAdmin()) {
			$("#dialog_add_iso #add_iso_public_container").show();
		}
	
        $dialogAddIso
	    .dialog('option', 'buttons', { 				
		    "Add": function() { 	
		        var $thisDialog = $(this);
    				
			    // validate values
			    var isValid = true;	
			    isValid &= validateString("Name", $thisDialog.find("#add_iso_name"), $thisDialog.find("#add_iso_name_errormsg"));				
			    isValid &= validateString("Display Text", $thisDialog.find("#add_iso_display_text"), $thisDialog.find("#add_iso_display_text_errormsg"));
			    isValid &= validateString("URL", $thisDialog.find("#add_iso_url"), $thisDialog.find("#add_iso_url_errormsg"));				    			   
			    if($thisDialog.find("#add_iso_bootable").val() == "true") 
			        isValid &= validateDropDownBox("OS Type", $thisDialog.find("#add_iso_os_type"), $thisDialog.find("#add_iso_os_type_errormsg"));				    		
			    if (!isValid) 
			        return;		
			        
			    $thisDialog.dialog("close");	
			    
			    var array1 = [];
			    var name = $thisDialog.find("#add_iso_name").val();
			    array1.push("&name="+todb(name));
			    
			    var desc = $thisDialog.find("#add_iso_display_text").val();
			    array1.push("&displayText="+todb(desc));
			    
			    var url = $thisDialog.find("#add_iso_url").val();	
			    array1.push("&url="+todb(url));
			    					
			    var zoneId = $thisDialog.find("#add_iso_zone").val();
			    array1.push("&zoneId="+zoneId);	
			    
			    var isextractable = $thisDialog.find("#isextractable").val();		
				array1.push("&isextractable="+isextractable);				
							    
			    var isPublic = $thisDialog.find("#add_iso_public").val();
			    array1.push("&isPublic="+isPublic);	
			    					    
			    var bootable = $thisDialog.find("#add_iso_bootable").val();	
			    array1.push("&bootable="+bootable);
			    
			    var osType = $thisDialog.find("#add_iso_os_type").val();			   
			    if(osType != null && osType.length > 0)
			        array1.push("&osTypeId="+osType);
			    			    
			    if($thisDialog.find("#isfeatured_container").css("display") != "none") {				
				    var isFeatured = $thisDialog.find("#isfeatured").val();						    	
                    array1.push("&isfeatured="+isFeatured);
                }			
    		    				    
		        var $midmenuItem1 = beforeAddingMidMenuItem() ;				    
    		       		    				
			    $.ajax({
			        data: createURL("command=registerIso"+array1.join("")),
				    dataType: "json",
				    success: function(json) {					
				        var items = json.registerisoresponse.iso;				       
				        isoToMidmenu(items[0], $midmenuItem1);
						bindClickToMidMenu($midmenuItem1, isoToRightPanel, isoGetMidmenuId);  
						afterAddingMidMenuItem($midmenuItem1, true);
						                        
                        if(items.length > 1) {                               
                            for(var i=1; i<items.length; i++) {   
                                var $midmenuItem2 = $("#midmenu_item").clone();
                                isoToMidmenu(items[i], $midmenuItem2);
                                bindClickToMidMenu($midmenuItem2, isoToRightPanel, isoGetMidmenuId); 
                                $("#midmenu_container").append($midmenuItem2.show());
                            }                                    
                        }  						
				    }, 
					error: function(XMLHttpResponse) {
						handleError(XMLHttpResponse, function() {
							afterAddingMidMenuItem($midmenuItem1, false, parseXMLHttpResponse(XMLHttpResponse));
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

function initCreateVmFromIsoDialog() {
    initDialog("dialog_create_vm_from_iso", 450);    
    
    var $dialogCreateVmFromIso = $("#dialog_create_vm_from_iso");
    
    $.ajax({
	    data: createURL("command=listServiceOfferings&issystem=false"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listserviceofferingsresponse.serviceoffering;
	        if(items != null && items.length > 0 ) {
	            var serviceOfferingField = $dialogCreateVmFromIso.find("#service_offering").empty();
	            for(var i = 0; i < items.length; i++)		        
	                serviceOfferingField.append("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");
	        }		        
	    }
	});		
	
	$.ajax({
	    data: createURL("command=listDiskOfferings"),
	    dataType: "json",
	    success: function(json) {
	        var items = json.listdiskofferingsresponse.diskoffering;
	        if(items != null && items.length > 0 ) {
	            var diskOfferingField = $dialogCreateVmFromIso.find("#disk_offering").empty();
	            for(var i = 0; i < items.length; i++) {		  
	                var $option = $("<option value='" + items[i].id + "'>" + fromdb(items[i].name) + "</option>");	
		            $option.data("jsonObj", items[i]);			                              
	                diskOfferingField.append($option);	            
	            }
	            $dialogCreateVmFromIso.find("#disk_offering").change();
	        }		  
	        
	    }
	});		
    
    $dialogCreateVmFromIso.find("#disk_offering").bind("change", function(event) {  	         
        var jsonObj = $(this).find("option:selected").data("jsonObj");
        if(jsonObj != null && jsonObj.isCustomized == true) { //jsonObj is null when "<option value=''>No disk offering</option>" is selected
            $dialogCreateVmFromIso.find("#size_container").show();
        }
        else {
            $dialogCreateVmFromIso.find("#size_container").hide();  
            $dialogCreateVmFromIso.find("#size").val("");
        }      
    });    
}

function isoGetMidmenuId(jsonObj) {
    return "midmenuItem_" + jsonObj.id + "_z" + jsonObj.zoneid; //remove all spaces in zonename
}

function isoToMidmenu(jsonObj, $midmenuItem1) {    
    var id = isoGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);   
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_iso.png");
    
    var firstRowText = fromdb(jsonObj.name);
    $midmenuItem1.find("#first_row").text(clippedText(firstRowText, midMenuFirstRowLength));     
    $midmenuItem1.find("#first_row_container").attr("title", firstRowText);   
    
    var secondRowText = fromdb(jsonObj.zonename);
    $midmenuItem1.find("#second_row").text(clippedText(secondRowText, midMenuSecondRowLength));
    $midmenuItem1.find("#second_row_container").attr("title", secondRowText); 
}

function isoToRightPanel($midmenuItem1) {  
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1); 
    $("#right_panel_content").data("$midmenuItem1", $midmenuItem1);
    isoJsonToDetailsTab();   
}

function isoJsonToDetailsTab() {  
	var timerKey = "isoDownloadProgress";	
	$("body").stopTime(timerKey);	//stop timer on previously selected middle menu item in ISO page	
	
    var $midmenuItem1 = $("#right_panel_content").data("$midmenuItem1");
    if($midmenuItem1 == null) {
        isoClearDetailsTab(); 
        return;
    }
    
    var jsonObj = $midmenuItem1.data("jsonObj");
    if(jsonObj == null) {
        isoClearDetailsTab(); 
        return;    
    }      
    
    var strCmd = "command=listIsos&isofilter=self&id="+jsonObj.id;
    if(jsonObj.zoneid != null)
        strCmd = strCmd + "&zoneid="+jsonObj.zoneid;    
    
    var itemExists = true; 
    $.ajax({
        data: createURL(strCmd),
        dataType: "json",
        async: false,
        success: function(json) {            
            var items = json.listisosresponse.iso;
            if(items != null && items.length > 0) {
                jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);                  
            }
            else {
                itemExists = false;
            }
        } 
	    ,			
	    error: function(XMLHttpResponse) {	
			handleError(XMLHttpResponse, function() {
				itemExists = false;
			});
	    }	      
    });
    if(itemExists == false)
        return;
    
    var $thisTab = $("#right_panel_content #tab_content_details");  
    $thisTab.find("#tab_container").hide(); 
    $thisTab.find("#tab_spinning_wheel").show();  
         
    $thisTab.find("#grid_header_title").text(fromdb(jsonObj.name));
        
    $thisTab.find("#id").text(fromdb(jsonObj.id));
    $thisTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $thisTab.find("#zoneid").text(fromdb(jsonObj.zoneid));
    
    $thisTab.find("#name").text(fromdb(jsonObj.name));
    $thisTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $thisTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $thisTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    $thisTab.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $thisTab.find("#ostypename_edit").val(fromdb(jsonObj.ostypeid));    
    $thisTab.find("#account").text(fromdb(jsonObj.account));
	$thisTab.find("#domain").text(fromdb(jsonObj.domain));
    
    
    //refresh status field every 2 seconds if ISO is in download progress	
			
	if(jsonObj.isready == true){
	    setTemplateStateInRightPanel("Ready", $thisTab.find("#status"));
	    $("#progressbar_container").hide();
	}
	else if(jsonObj.status == null || jsonObj.status == "" || jsonObj.status.indexOf("%") != -1) {  //ISO is downloading....
	    $("#progressbar_container").show();	   
	    setTemplateStateInRightPanel(fromdb(jsonObj.status), $thisTab.find("#status"));
        var progressBarValue = 0;
        if(jsonObj.status != null && jsonObj.status.indexOf("%") != -1) {      //e.g. jsonObj.status == "95% Downloaded" 	    
            var s = jsonObj.status.substring(0, jsonObj.status.indexOf("%"));  //e.g. s	== "95"
            if(isNaN(s) == false) {	        
                progressBarValue = parseInt(s);	//e.g. progressBarValue	== 95   
            } 
        }
        $("#progressbar").progressbar({
            value: progressBarValue             //e.g. progressBarValue	== 95  
        });	 
	   	        
        $("body").everyTime(
            2000,
            timerKey,
            function() {   
                isoRefreshStatusDownloadProgress(jsonObj, $thisTab, $midmenuItem1, timerKey);                                   	
            }
        )	     
	}
	else { //error status
	    setTemplateStateInRightPanel(fromdb(jsonObj.status), $thisTab.find("#status"));
	    $("#progressbar_container").hide();
	}
	
	if(jsonObj.size != null)
	    $thisTab.find("#size").text(convertBytes(parseInt(jsonObj.size)));  
	else
	    $thisTab.find("#size").text("");    
              
    setBooleanReadField(jsonObj.bootable, $thisTab.find("#bootable"));	
    
    setBooleanReadField(jsonObj.isextractable, $thisTab.find("#isextractable"));   
    
    setBooleanReadField(jsonObj.ispublic, $thisTab.find("#ispublic"));	
    setBooleanEditField(jsonObj.ispublic, $thisTab.find("#ispublic_edit"));
    
    setBooleanReadField(jsonObj.isfeatured, $thisTab.find("#isfeatured"));
    setBooleanEditField(jsonObj.isfeatured, $thisTab.find("#isfeatured_edit"));
    
    setBooleanReadField(jsonObj.crossZones, $thisTab.find("#crossZones"));	     
    setDateField(jsonObj.created, $thisTab.find("#created"));	  
    
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;

    // "Edit ISO", "Copy ISO"
	//if ((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) 
	if ((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))  //if neither root-admin, nor item owner 
	    || (jsonObj.isready == false)
	    || (jsonObj.domainid ==	1 && jsonObj.account ==	"system")
	    ) {		
		//do nothing
    }
    else {        
        buildActionLinkForTab("label.action.edit.ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
        noAvailableActions = false;	
        
        if(jsonObj.id != xsToolsIsoId)
            buildActionLinkForTab("label.action.copy.ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	        
    }
		
	// "Create VM"
	// Commenting this out for Beta2 as it does not support the new network.
	/*
	//if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) 
	if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))  //if neither root-admin, nor item owner 
	    || jsonObj.isready == false) 
	    || (jsonObj.bootable == false)
	    || (jsonObj.domainid ==	1 && jsonObj.account ==	"system")
	    ) {
	    //do nothing
	}
    else {        
        buildActionLinkForTab("label.action.create.vm", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
        noAvailableActions = false;
    }
	*/
    
	// "Download ISO"
	//if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))) 
	if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)))  //if neither root-admin, nor item owner 
	    || (jsonObj.isready == false)
	    || (jsonObj.domainid ==	1 && jsonObj.account ==	"system")
	    ) {
	    //do nothing
	}
	else {	    
	    buildActionLinkForTab("label.action.download.ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);		    
	    noAvailableActions = false;
	}    		   
	
	// "Delete ISO"
	//if (((isUser() && jsonObj.ispublic == true && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account))) 
	if (((isAdmin() == false && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)))  //if neither root-admin, nor item owner 
	    || (jsonObj.isready == false && jsonObj.status != null && jsonObj.status.indexOf("Downloaded") != -1)
	    || (jsonObj.domainid ==	1 && jsonObj.account ==	"system")
	    ) {
	    //do nothing
	}
	else {	   	    
	    buildActionLinkForTab("label.action.delete.ISO", isoActionMap, $actionMenu, $midmenuItem1, $thisTab);	
	    noAvailableActions = false;
	}    	
	
	// no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	 
	
	$thisTab.find("#tab_spinning_wheel").hide();    
    $thisTab.find("#tab_container").show();     
}

function isoRefreshStatusDownloadProgress(oldJsonObj, $thisTab, $midmenuItem1, timerKey) {    
    var strCmd = "command=listIsos&isofilter=self&id="+oldJsonObj.id;
    if(oldJsonObj.zoneid != null)
        strCmd = strCmd + "&zoneid="+oldJsonObj.zoneid;    
    $.ajax({
        data: createURL(strCmd),
        dataType: "json",        
        success: function(json) { 
            var items = json.listisosresponse.iso;
            if(items != null && items.length > 0) {
                var jsonObj = items[0];
                $midmenuItem1.data("jsonObj", jsonObj);    
                
                if(jsonObj.isready == true) {
                    setTemplateStateInRightPanel("Ready", $thisTab.find("#status"));
                    $("#progressbar_container").hide();
                    $("body").stopTime(timerKey);   
                }
                else {
                	if(jsonObj.status != null && jsonObj.status != "" &&  jsonObj.status.indexOf("%") == -1) { //error state 
                        setTemplateStateInRightPanel(fromdb(jsonObj.status), $thisTab.find("#status"));
                        $("#progressbar_container").hide();
                        $("body").stopTime(timerKey);  
                    }
                    else { 
	                    setTemplateStateInRightPanel(fromdb(jsonObj.status), $thisTab.find("#status"));
	                    var progressBarValue = 0;
	                    if(jsonObj.status != null && jsonObj.status.indexOf("%") != -1) {      //e.g. jsonObj.status == "95% Downloaded" 	    
	                        var s = jsonObj.status.substring(0, jsonObj.status.indexOf("%"));  //e.g. s	== "95"
	                        if(isNaN(s) == false) {	        
	                            progressBarValue = parseInt(s);	//e.g. progressBarValue	== 95   
	                        } 
	                    }
	                    $("#progressbar").progressbar({
	                        value: progressBarValue             //e.g. progressBarValue	== 95  
	                    });	 
                    } 
                }   
            }            
        }    
    });      
}

function isoClearRightPanel() {
    isoClearDetailsTab(); 
}

function isoClearDetailsTab() {
    var $thisTab = $("#right_panel_content #tab_content_details");   
    
    $thisTab.find("#grid_header_title").text("");
    
    $thisTab.find("#id").text("");
    $thisTab.find("#zonename").text("");
    $thisTab.find("#zoneid").text("");
    
    $thisTab.find("#name").text("");
    $thisTab.find("#name_edit").val("");
    
    $thisTab.find("#displaytext").text("");
    $thisTab.find("#displaytext_edit").val("");
    
    $thisTab.find("#ostypename").text("");
    
    $thisTab.find("#account").text("");  
	$thisTab.find("#domain").text("");
	$thisTab.find("#ostypename_edit").val(null);   
    $thisTab.find("#size").text("");  
	$thisTab.find("#status").text(""); 
	$thisTab.find("#bootable").text("");
	$thisTab.find("#ispublic").text("");	
	$thisTab.find("#isfeatured").text("");  
	$thisTab.find("#crossZones").text("");
    $thisTab.find("#created").text("");  
    
     //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
}

var isoActionMap = {  
    "label.action.edit.ISO": {
        dialogBeforeActionFn: doEditISO  
    },
    "label.action.delete.ISO": {                  
        isAsyncJob: true,
        asyncJobResponse: "deleteisosresponse",
        dialogBeforeActionFn: doDeleteIso,
        inProcessText: "label.action.delete.ISO.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){   
            var jsonObj = $midmenuItem1.data("jsonObj");
            $midmenuItem1.remove();
            if((jsonObj.id == $("#right_panel_content").find("#tab_content_details").find("#id").text()) 
               && ((jsonObj.zoneid == null) || (jsonObj.zoneid != null && jsonObj.zoneid == $("#right_panel_content").find("#tab_content_details").find("#zoneid").text()))) {
                clearRightPanel();
                isoClearRightPanel();  
            }            
             
            /*            
            $midmenuItem1.slideUp("slow", function() {
                var jsonObj = $midmenuItem1.data("jsonObj");
                $(this).remove();                
                if((jsonObj.id == $("#right_panel_content").find("#tab_content_details").find("#id").text()) && (jsonObj.zoneid == $("#right_panel_content").find("#tab_content_details").find("#zoneid").text())) {
                    clearRightPanel();
                    isoClearRightPanel();
                }
            });    
            */             
        }        
    },
    "label.action.copy.ISO": {
        isAsyncJob: true,
        asyncJobResponse: "copyisoresponse",            
        dialogBeforeActionFn: doCopyIso,
        inProcessText: "label.action.copy.ISO.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}   
    }  
    ,
    "label.action.create.vm": {
        isAsyncJob: true,
        asyncJobResponse: "deployvirtualmachineresponse",            
        dialogBeforeActionFn: doCreateVMFromIso,
        inProcessText: "label.action.create.vm.processing",
        afterActionSeccessFn: function(json, $midmenuItem1, id){}   
    },
    "label.action.download.ISO": {               
        dialogBeforeActionFn : doDownloadISO        
    }     
}   

function doEditISO($actionLink, $detailsTab, $midmenuItem1) {       
    $readonlyFields.hide();
    $editFields.show();  
    $detailsTab.find("#cancel_button, #save_button").show();
    
    $detailsTab.find("#cancel_button").unbind("click").bind("click", function(event){    
        cancelEditMode($detailsTab);    
        return false;
    });
    $detailsTab.find("#save_button").unbind("click").bind("click", function(event){        
        doEditISO2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields);   
        return false;
    });   
}

function doEditISO2($actionLink, $detailsTab, $midmenuItem1, $readonlyFields, $editFields) {     
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"));			
    if (!isValid) 
        return;
       
    var jsonObj = $midmenuItem1.data("jsonObj"); 
	var id = jsonObj.id;
	
	var array1 = [];
	array1.push("&id="+id);
							
	var name = $detailsTab.find("#name_edit").val();
	array1.push("&name="+todb(name));
		
	var displaytext = $detailsTab.find("#displaytext_edit").val();
	array1.push("&displayText="+todb(displaytext));
	
	var oldOsTypeId = jsonObj.ostypeid;
	var newOsTypeId = $detailsTab.find("#ostypename_edit").val();
	if(newOsTypeId != oldOsTypeId)
	    array1.push("&ostypeid="+newOsTypeId);
		
	$.ajax({
	    data: createURL("command=updateIso"+array1.join("")),
		dataType: "json",
		async: false,
		success: function(json) {	
		    $detailsTab.find("#name").text(name);
		    $midmenuItem1.find("#first_row").text(name.substring(0,25)); 
		    $detailsTab.find("#displaytext").text(displaytext);		           
		    $detailsTab.find("#ostypename").text($detailsTab.find("#ostypename_edit option:selected").text());				    
		    jsonObj.name = name;
		    jsonObj.displaytext = displaytext;		    
		    jsonObj.ostypeid = parseInt(newOsTypeId);
		    jsonObj.ostypename = $detailsTab.find("#ostypename_edit option:selected").text();    		
		}
	});
	
	//updateIsoPermissions
	var array2 = [];	
	var oldIsPublic = jsonObj.ispublic.toString();	
	var newIsPublic = $detailsTab.find("#ispublic_edit").val();       
	if(newIsPublic != oldIsPublic)
	    array2.push("&ispublic="+newIsPublic);	    						
		    
	var oldIsFeatured = jsonObj.isfeatured.toString();	
	var newIsFeatured = $detailsTab.find("#isfeatured_edit").val();           
    if(newIsFeatured != oldIsFeatured)
        array2.push("&isfeatured="+newIsFeatured);		
								
	if(array2.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateIsoPermissions&id="+id+array2.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {			    	        						       					    
		        setBooleanReadField(newIsPublic, $detailsTab.find("#ispublic"));
		        setBooleanReadField(newIsFeatured, $detailsTab.find("#isfeatured"));		       
		        jsonObj.ispublic = (newIsPublic == "true");
		        jsonObj.isfeatured = (newIsFeatured == "true");
    		}
	    });
	}	
	
	$editFields.hide();      
    $readonlyFields.show();       
    $("#save_button, #cancel_button").hide();       
}

function doDeleteIso($actionLink, $detailsTab, $midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var zoneId = jsonObj.zoneid;

    var moreCriteria = [];						
	if (zoneId != null) 
		moreCriteria.push("&zoneid="+zoneId);	
	
	var $dialog1;
	if(jsonObj.crossZones == true)
	    $dialog1 = $("#dialog_confirmation").text(dictionary["message.action.delete.ISO.for.all.zones"]);
	else
	    $dialog1 = $("#dialog_confirmation").text(dictionary["message.action.delete.ISO"]);	
	
	$dialog1	
	.dialog('option', 'buttons', { 					
		"Confirm": function() { 			
			$(this).dialog("close");	
			$("body").stopTime("isoDownloadProgress");
			var apiCommand = "command=deleteIso&id="+id+moreCriteria.join("");
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	
		}, 
		"Cancel": function() { 
			$(this).dialog("close"); 
		}
	}).dialog("open");
}

function populateZoneFieldExcludeSourceZone(zoneField, excludeZoneId) {	  
    zoneField.empty();  
    if (g_zoneIds != null && g_zoneIds.length > 0) {
        for (var i = 0; i < g_zoneIds.length; i++) {
            if(g_zoneIds[i]	!= excludeZoneId)			            
	            zoneField.append("<option value='" + g_zoneIds[i] + "'>" + fromdb(g_zoneNames[i]) + "</option>"); 			        			       
        }
    }			    
}

function doCopyIso($actionLink, $detailsTab, $midmenuItem1) {   
	var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;
	var name = jsonObj.name;			
	var sourceZoneId = jsonObj.zoneid;				
	populateZoneFieldExcludeSourceZone($("#dialog_copy_iso #copy_iso_zone"), sourceZoneId);		
			
	$("#dialog_copy_iso")
	.dialog('option', 'buttons', {				    
	    "OK": function() {				       
	        var $thisDialog = $(this);
	        	        	        
	        var isValid = true;	 
            isValid &= validateDropDownBox("Zone", $thisDialog.find("#copy_iso_zone"), $thisDialog.find("#copy_iso_zone_errormsg"), false);  //reset error text		         
	        if (!isValid) return;     
	        
	        $thisDialog.dialog("close");
	        				        
	        var destZoneId = $thisDialog.find("#copy_iso_zone").val();	
            var apiCommand = "command=copyIso&id="+id+"&sourcezoneid="+sourceZoneId+"&destzoneid="+destZoneId;
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);	 
	    }, 
	    "Cancel": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
}	

function doCreateVMFromIso($actionLink, $detailsTab, $midmenuItem1) { 
    var jsonObj = $midmenuItem1.data("jsonObj");	
	var id = jsonObj.id;		
	var name = jsonObj.name;				
	var zoneId = jsonObj.zoneid;
	var createVmDialog = $("#dialog_create_vm_from_iso");				
			
	createVmDialog
	.dialog('option', 'buttons', {			    
	    "Create": function() {
	        var $thisDialog = $(this);	
	      
	        // validate values
		    var isValid = true;		
		    isValid &= validateString("Name", $thisDialog.find("#name"), $thisDialog.find("#name_errormsg"), true);
		    isValid &= validateString("Group", $thisDialog.find("#group"), $thisDialog.find("#group_errormsg"), true);	
		     if($thisDialog.find("#size_container").css("display") != "none")
			    isValid &= validateInteger("Size", $thisDialog.find("#size"), $thisDialog.find("#size_errormsg"));				
		    if (!isValid) 
		        return;	       
	           
	        $thisDialog.dialog("close");   
	        
	        var array1 = [];
	        
	        var name = trim($thisDialog.find("#name").val());
	        array1.push("&displayname="+todb(name));		
	        
	        var group = trim($thisDialog.find("#group").val());	
	        array1.push("&group="+todb(group));	
	        
	        var serviceOfferingId = $thisDialog.find("#service_offering").val();	
	        array1.push("&serviceOfferingId="+serviceOfferingId);			        
	        
	        var diskOfferingId = $thisDialog.find("#disk_offering").val();	
	        array1.push("&diskOfferingId="+diskOfferingId);
	        
	        if($thisDialog.find("#size_container").css("display") != "none") {
	            var size = $thisDialog.find("#size").val()
			    array1.push("&size="+size);
	        }
	        
	        var hypervisor = $thisDialog.find("#hypervisor").val();	
	        array1.push("&hypervisor="+hypervisor);	
	                         
		    var apiCommand = "command=deployVirtualMachine&zoneId="+zoneId+"&templateId="+id+array1.join("");
    	    doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $detailsTab);		
	    }, 
	    "Cancel": function() {
	        $(this).dialog("close");
	    }
	}).dialog("open");			
}	

function doDownloadISO($actionLink, $detailsTab, $midmenuItem1) { 
	var jsonObj = $midmenuItem1.data("jsonObj");
	var id = jsonObj.id;						
	var zoneId = jsonObj.zoneid;	
	
   var apiCommand = "command=extractIso&id="+id+"&zoneid="+zoneId+"&mode=HTTP_DOWNLOAD";
   
   var $dialogDownloadISO = $("#dialog_download_ISO");
   $spinningWheel = $dialogDownloadISO.find("#spinning_wheel");
   $spinningWheel.show();
   var $infoContainer = $dialogDownloadISO.find("#info_container");
   $infoContainer.hide();	
   
   $dialogDownloadISO
	.dialog('option', 'buttons', {	
	    "Close": function() {				        
		    $(this).dialog("close");
	    }				
	}).dialog("open");	
			  
   $.ajax({
       data: createURL(apiCommand),
       dataType: "json",           
       success: function(json) {	                       	                        
           var jobId = json.extractisoresponse.jobid;                  			                        
           var timerKey = "asyncJob_" + jobId;					                       
           $("body").everyTime(
               2000,  //this API returns fast. So, set 2 seconds instead of 10 seconds.
               timerKey,
               function() {
                   $.ajax({
                       data: createURL("command=queryAsyncJobResult&jobId="+jobId),
                       dataType: "json",									                    					                    
                       success: function(json) {		                                                     							                       
	                        var result = json.queryasyncjobresultresponse;										                   
	                        if (result.jobstatus == 0) {
		                        return; //Job has not completed
	                        } else {											                    
		                        $("body").stopTime(timerKey);				                        
		                        $spinningWheel.hide(); 		                 		                          			                                             
		                        if (result.jobstatus == 1) { // Succeeded 			                            
		                            $infoContainer.removeClass("error");
		                            $infoContainer.find("#icon,#info").removeClass("error");		                      
		                            var url = decodeURIComponent(json.queryasyncjobresultresponse.jobresult.iso.url);	
		                            var htmlMsg = dictionary["message.download.ISO"];		                            
		                            var htmlMsg2 = htmlMsg.replace(/#/, url).replace(/00000/, url);                        
		                            $infoContainer.find("#info").html(htmlMsg2);
		                            $infoContainer.show();		                        
		                        } else if (result.jobstatus == 2) { // Failed                            
		                            //var errorMsg = g_dictionary["label.failed"] + " - " + g_dictionary["label.error.code"] + " " + fromdb(result.jobresult.errorcode);
			                        var errorMsg = g_dictionary["label.failed"] + " - " + fromdb(result.jobresult.errortext);	
			                        handleErrorInDialog2(errorMsg, $dialogDownloadISO);		                        
		                        }											                    
	                        }
                       },
                       error: function(XMLHttpResponse) {	                            
	                        $("body").stopTime(timerKey);	
							handleError(XMLHttpResponse, function() {
							    handleErrorInDialog(XMLHttpResponse, $dialogDownloadISO);									
							});
                       }
                   });
               },
               0
           );
       },
       error: function(XMLHttpResponse) {
			handleError(XMLHttpResponse, function() {
				handleErrorInDialog(XMLHttpResponse, $dialogDownloadISO);			
			});
       }
   });  	
}
