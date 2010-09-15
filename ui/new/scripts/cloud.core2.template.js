function afterLoadTemplateJSP() {        
   var $detailsTab = $("#right_panel_content #tab_content_details");   
    
    //edit button
    var $readonlyFields  = $detailsTab.find("#name, #displaytext, #passwordenabled, #ispublic, #isfeatured, #ostypename");
    var $editFields = $detailsTab.find("#name_edit, #displaytext_edit, #passwordenabled_edit, #ispublic_edit, #isfeatured_edit, #ostypename_edit"); 
    $("#edit_button").bind("click", function(event){    
        $readonlyFields.hide();
        $editFields.show();        
        $(this).hide();
        $("#cancel_button, #save_button").show()
        return false;
    });    
    $("#cancel_button").bind("click", function(event){    
        $editFields.hide();
        $readonlyFields.show();   
        $("#save_button, #cancel_button").hide();
        $("#edit_button").show();
        return false;
    });
    $("#save_button").bind("click", function(event){        
        updateTemplate();     
        $editFields.hide();      
        $readonlyFields.show();       
        $("#save_button, #cancel_button").hide();
        $("#edit_button").show();
        return false;
    });
    
    
    //OS type dropdown
    $.ajax({
	    data: createURL("command=listOsTypes&response=json"+maxPageSize),
		dataType: "json",
		success: function(json) {
			types = json.listostypesresponse.ostype;
			if (types != null && types.length > 0) {
				var osTypeDropdown = $detailsTab.find("#ostypename_edit").empty();
				for (var i = 0; i < types.length; i++) {
					var html = "<option value='" + types[i].id + "'>" + types[i].description + "</option>";
					osTypeDropdown.append(html);					
				}
			}	
		}
	});
}

function updateTemplate() {    
    var $detailsTab = $("#right_panel_content #tab_content_details");  
            
    // validate values
    var isValid = true;					
    isValid &= validateString("Name", $detailsTab.find("#name_edit"), $detailsTab.find("#name_edit_errormsg"));
    isValid &= validateString("Display Text", $detailsTab.find("#displaytext_edit"), $detailsTab.find("#displaytext_edit_errormsg"));			
    if (!isValid) 
        return;					
	
	var jsonObj = $detailsTab.data("jsonObj"); 
	var id = jsonObj.id;
	
	//updateTemplate	
	var array1 = [];
	var oldName = jsonObj.name
	var newName = trim($detailsTab.find("#name_edit").val());
	if(newName != oldName)
	    array1.push("&name="+todb(newName));
	
	var oldDesc = jsonObj.displaytext;
	var newDesc = trim($detailsTab.find("#displaytext_edit").val());	
	if(newDesc != oldDesc)
	    array1.push("&displaytext="+todb(newDesc));
	    
	var oldPasswordEnabled = jsonObj.passwordenabled;	
	var newPasswordEnabled = $detailsTab.find("#passwordenabled_edit").val();     
	if(newPasswordEnabled != oldPasswordEnabled)
	    array1.push("&passwordenabled="+newPasswordEnabled);	
		
	var oldOsTypeId = jsonObj.ostypeid;
	var newOsTypeId = $detailsTab.find("#ostypename_edit").val();
	if(newOsTypeId != oldOsTypeId)
	    array1.push("&ostypeid="+newOsTypeId);
				
	if(array1.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateTemplate&id="+id+array1.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {		        
		        //embedded object (json.updatetemplateresponse) is returned, but the embedded object doesn't include all properties.(API needs to be fixed)		
		    }
	    });
	}
		
	//updateTemplatePermissions	
	var array2 = [];		
	var oldIsPublic = jsonObj.ispublic;
	var newIsPublic = $detailsTab.find("#ispublic_edit").val();        
	if(newIsPublic != oldIsPublic)
	    array2.push("&ispublic="+newIsPublic);
	    
	var oldIsFeatured = jsonObj.isfeatured;
	var newIsFeatured = $detailsTab.find("#isfeatured_edit").val();           
    if(newIsFeatured != oldIsFeatured)
        array2.push("&isfeatured="+newIsFeatured);											
								
	if(array2.length > 0) {	
	    $.ajax({
		    data: createURL("command=updateTemplatePermissions&id="+id+array2.join("")),
		    dataType: "json",
		    async: false,
		    success: function(json) {			        						       					    
		        //no embedded object is returned. (API needs to be fixed)		
    		}
	    });
	}	
	
	//since embedded object is not returned (updateTemplatePermissions API) or embedded object doesn't include all properties (updateTemplate API), call listTemplates API again.	
	$.ajax({
        data:createURL("command=listTemplates&templatefilter=self&id="+id),
        dataType: "json",
        success: function(json) {            
            templateAfterDetailsTabAction(json.listtemplatesresponse.template[0]);
        }
    });   
}

function templateToMidmenu(jsonObj, $midmenuItem1) {    
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
       
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    setIconByOsType(jsonObj.ostypename, $iconContainer.find("#icon"));
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.zonename).substring(0,25));   
}

function templateAfterDetailsTabAction(jsonObj) {
    var $midmenuItem1 = $("#midmenuItem_"+jsonObj.id);
    $midmenuItem1.data("jsonObj", jsonObj);   
    templateToMidmenu(jsonObj, $midmenuItem1);
    templateJsonToDetailsTab(jsonObj);       
}

function templateToRigntPanel($midmenuItem) {       
    var jsonObj = $midmenuItem.data("jsonObj");
    templateJsonToDetailsTab(jsonObj);   
}

function templateJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);
    $detailsTab.find("#id").text(fromdb(jsonObj.id));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#name_edit").val(fromdb(jsonObj.name));
    
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#displaytext_edit").val(fromdb(jsonObj.displaytext));
    
    var status = "Ready";
	if (jsonObj.isready == "false") 
		status = jsonObj.templatestatus;	
	$detailsTab.find("#status").text(status);    
    
    setBooleanField(jsonObj.passwordenabled, $detailsTab.find("#passwordenabled"));	
    $detailsTab.find("#passwordenabled_edit").val(jsonObj.passwordenabled);
    
    setBooleanField(jsonObj.ispublic, $detailsTab.find("#ispublic"));	
    $detailsTab.find("#ispublic_edit").val(jsonObj.ispublic);
    
    setBooleanField(jsonObj.isfeatured, $detailsTab.find("#isfeatured"));
    $detailsTab.find("#isfeatured_edit").val(jsonObj.isfeatured);
    
    setBooleanField(jsonObj.crossZones, $detailsTab.find("#crossZones"));
    
    $detailsTab.find("#ostypename").text(fromdb(jsonObj.ostypename));
    $detailsTab.find("#ostypename_edit").val(jsonObj.ostypeid);    
    
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    
    if(jsonObj.size != null)
	    $detailsTab.find("#size").text(convertBytes(parseInt(jsonObj.size)));        
    
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
    
    //actions ***
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    
    // action Edit, Copy, Create VM 			
	if ((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.id==DomRTemplateId || jsonObj.isready == "false") {
		//template.find("#template_edit_container, #template_copy_container, #template_create_vm_container").hide(); 
		$("edit_button").hide();		
    }
    else {
        $("edit_button").show();
        //buildActionLinkForDetailsTab("Copy Template", templateActionMap, $actionMenu, templateListAPIMap);			
        //buildActionLinkForDetailsTab("Create VM", templateActionMap, $actionMenu, templateListAPIMap);			
    }
	
	// action Delete 			
	if (((isUser() && jsonObj.ispublic == "true" && !(jsonObj.domainid == g_domainid && jsonObj.account == g_account)) || jsonObj.id==DomRTemplateId) || (jsonObj.isready == "false" && jsonObj.templatestatus != null && jsonObj.templatestatus.indexOf("% Downloaded") != -1)) {
		//template.find("#template_delete_container").hide();
    }
    else {
        buildActionLinkForDetailsTab("Delete Template", templateActionMap, $actionMenu, templateListAPIMap);	
    }
}

//setIconByOsType() is shared by template page and ISO page
function setIconByOsType(osType, $field) {
	if (osType == null || osType.length == 0)
		return; 	
	if (osType.match("^CentOS") != null)
		$field.attr("src", "images/midmenuicon_template_centos.png");
	else if (osType.match("^Windows") != null) 
		$field.attr("src", "images/midmenuicon_template_windows.png");
	else 
		$field.attr("src", "images/midmenuicon_template_linux.png");
}

function templateClearRightPanel() {       
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", null);
    $detailsTab.find("#id").text("");
    $detailsTab.find("#zonename").text("");
    
    $detailsTab.find("#name").text("");
    $detailsTab.find("#name_edit").val("");
    
    $detailsTab.find("#displaytext").text("");
    $detailsTab.find("#displaytext_edit").val("");
        
	$detailsTab.find("#status").text("");    
    
    setBooleanField(null, $detailsTab.find("#passwordenabled"));	
    $detailsTab.find("#passwordenabled_edit").val(null);
    
    setBooleanField(null, $detailsTab.find("#ispublic"));	
    $detailsTab.find("#ispublic_edit").val(null);
    
    setBooleanField(null, $detailsTab.find("#isfeatured"));
    $detailsTab.find("#isfeatured_edit").val(null);
    
    setBooleanField(null, $detailsTab.find("#crossZones"));
    
    $detailsTab.find("#ostypename").text("");
    $detailsTab.find("#ostypename_edit").val(null);    
    
    $detailsTab.find("#account").text("");  
	$detailsTab.find("#size").text("");  
    $detailsTab.find("#created").text("");      
}

var templateActionMap = {  
    "Delete Template": {
        api: "deleteTemplate",            
        isAsyncJob: true,
        asyncJobResponse: "deletetemplateresponse",
        inProcessText: "Deleting Template....",
        afterActionSeccessFn: function(jsonObj) {           
            var $midmenuItem1 = $("#midmenuItem_"+jsonObj.id);
            $midmenuItem1.remove();
            clearRightPanel();
            templateClearRightPanel();
        }
    }
}   

var templateListAPIMap = {
    listAPI: "listTemplates&templatefilter=self",
    listAPIResponse: "listtemplatesresponse",
    listAPIResponseObj: "template"
}; 

var DomRTemplateId = 1;