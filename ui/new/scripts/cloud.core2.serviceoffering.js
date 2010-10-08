function afterLoadServiceOfferingJSP() {
     //dialogs
     initDialog("dialog_add_service");
     
     //add button ***
    $("#midmenu_add_link").show();     
    $("#midmenu_add_link").unbind("click").bind("click", function(event) {      
        var dialogAddService = $("#dialog_add_service");
		
		dialogAddService.find("#add_service_name").val("");
		dialogAddService.find("#add_service_display").val("");
		dialogAddService.find("#add_service_cpucore").val("");
		dialogAddService.find("#add_service_cpu").val("");
		dialogAddService.find("#add_service_memory").val("");
		dialogAddService.find("#add_service_offerha").val("false");
			
		(g_hypervisorType == "kvm")? dialogAddService.find("#add_service_offerha_container").hide():dialogAddService.find("#add_service_offerha_container").show();            
				
		dialogAddService
		.dialog('option', 'buttons', { 				
			"Add": function() { 	
			    var thisDialog = $(this);
							
				// validate values
				var isValid = true;					
				isValid &= validateString("Name", thisDialog.find("#add_service_name"), thisDialog.find("#add_service_name_errormsg"));
				isValid &= validateString("Display Text", thisDialog.find("#add_service_display"), thisDialog.find("#add_service_display_errormsg"));
				isValid &= validateNumber("# of CPU Core", thisDialog.find("#add_service_cpucore"), thisDialog.find("#add_service_cpucore_errormsg"), 1, 1000);		
				isValid &= validateNumber("CPU", thisDialog.find("#add_service_cpu"), thisDialog.find("#add_service_cpu_errormsg"), 100, 100000);		
				isValid &= validateNumber("Memory", thisDialog.find("#add_service_memory"), thisDialog.find("#add_service_memory_errormsg"), 64, 1000000);	
				isValid &= validateString("Tags", thisDialog.find("#add_service_tags"), thisDialog.find("#add_service_tags_errormsg"), true);	//optional							
				if (!isValid) 
				    return;										
									
				var $midmenuItem1 = beforeAddingMidMenuItem() ;			
									
				var array1 = [];						
				var name = trim(thisDialog.find("#add_service_name").val());
				array1.push("&name="+todb(name));	
				
				var display = trim(thisDialog.find("#add_service_display").val());
				array1.push("&displayText="+todb(display));	
				
				var storagetype = trim(thisDialog.find("#add_service_storagetype").val());
				array1.push("&storageType="+storagetype);	
				
				var core = trim(thisDialog.find("#add_service_cpucore").val());
				array1.push("&cpuNumber="+core);	
				
				var cpu = trim(thisDialog.find("#add_service_cpu").val());
				array1.push("&cpuSpeed="+cpu);	
				
				var memory = trim(thisDialog.find("#add_service_memory").val());
				array1.push("&memory="+memory);	
					
				var offerha = thisDialog.find("#add_service_offerha").val();	
				array1.push("&offerha="+offerha);								
									
				var networkType = thisDialog.find("#add_service_networktype").val();
				var useVirtualNetwork = (networkType=="direct")? false:true;
				array1.push("&usevirtualnetwork="+useVirtualNetwork);		
				
				var tags = trim(thisDialog.find("#add_service_tags").val());
				if(tags != null && tags.length > 0)
				    array1.push("&tags="+todb(tags));		
				
				thisDialog.dialog("close");
				$.ajax({
				  data: createURL("command=createServiceOffering"+array1.join("")+"&response=json"),
					dataType: "json",
					success: function(json) {					    				
						var item = json.createserviceofferingresponse;							
						serviceOfferingToMidmenu(item, $midmenuItem1);	
						bindClickToMidMenu($midmenuItem1, serviceOfferingToRigntPanel);  
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

function serviceOfferingToMidmenu(jsonObj, $midmenuItem1) {  
    $midmenuItem1.attr("id", ("midmenuItem_"+jsonObj.id));  
    $midmenuItem1.data("jsonObj", jsonObj); 
        
    //var $iconContainer = $midmenuItem1.find("#icon_container").show();   
    //$iconContainer.find("#icon").attr("src", "images/midmenuicon_storage_volume.png");	
    
    $midmenuItem1.find("#first_row").text(fromdb(jsonObj.name).substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));  
}

function serviceOfferingToRigntPanel($midmenuItem) {
    var jsonObj = $midmenuItem.data("jsonObj");
    serviceOfferingJsonToDetailsTab(jsonObj);   
}

function serviceOfferingJsonToDetailsTab(jsonObj) { 
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    $detailsTab.find("#id").text(jsonObj.id);
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#displaytext").text(fromdb(jsonObj.displaytext));
    $detailsTab.find("#storagetype").text(jsonObj.storagetype);
    $detailsTab.find("#cpu").text(jsonObj.cpunumber + " x " + convertHz(jsonObj.cpuspeed));
    $detailsTab.find("#memory").text(convertBytes(parseInt(jsonObj.memory)*1024*1024));
    $detailsTab.find("#offerha").text(toBooleanText(jsonObj.offerha));
    $detailsTab.find("#networktype").text(toNetworkType(jsonObj.usevirtualnetwork));
    $detailsTab.find("#tags").text(fromdb(jsonObj.tags));   
    setDateField(jsonObj.created, $detailsTab.find("#created"));	
}