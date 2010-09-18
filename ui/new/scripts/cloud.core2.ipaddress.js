//***** baseline (begin) *******************************************************************************************************************
function afterLoadIpJSP() {
    //switch to different tab
    $("#tab_details").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_port_forwarding, #tab_load_balancer").removeClass("on").addClass("off");  
        $("#tab_content_details").show();     
        $("#tab_content_port_forwarding, #tab_content_load_balancer").hide();   
        return false;
    });    
    $("#tab_port_forwarding").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_details, #tab_load_balancer").removeClass("on").addClass("off");   
        $("#tab_content_port_forwarding").show();    
        $("#tab_content_details, #tab_content_load_balancer").hide();    
        return false;
    });    
    $("#tab_load_balancer").bind("click", function(event){
        $(this).removeClass("off").addClass("on");
        $("#tab_details, #tab_port_forwarding").removeClass("on").addClass("off");  
        $("#tab_content_load_balancer").show();   
        $("#tab_content_details, #tab_content_port_forwarding").hide();      
        return false;
    }); 
    
    
    //Port Fowording tab
    var $createPortForwardingRow = $("#create_port_forwarding_row");     
    
    $createPortForwardingRow.find("#add_link").bind("click", function(event){	        
		var isValid = true;				
		isValid &= validateNumber("Public Port", $createPortForwardingRow.find("#public_port"), $createPortForwardingRow.find("#public_port_errormsg"), 1, 65535);
		isValid &= validateNumber("Private Port", $createPortForwardingRow.find("#private_port"), $createPortForwardingRow.find("#private_port_errormsg"), 1, 65535);				
		if (!isValid) return;			
	    
	    var $template = $("#port_forwarding_template").clone();
	    $("#tab_content_port_forwarding #grid_container").append($template.show());		
	    
	    var $spinningWheel = $template.find("row_container").find("#spinning_wheel");	
	    $spinningWheel.find("#description").text("Adding....");	
        $spinningWheel.show();   
	    
	    var $detailsTab = $("#right_panel_content #tab_content_details");   
        var jsonObj = $detailsTab.data("jsonObj");          
		var ipAddress = jsonObj.ipaddress;		
	    var publicPort = $createPortForwardingRow.find("#public_port").val();
	    var privatePort = $createPortForwardingRow.find("#private_port").val();
	    var protocol = $createPortForwardingRow.find("#protocol").val();
	    var virtualMachineId = $createPortForwardingRow.find("#vm").val();		   
	    		    
	    var array1 = [];
        array1.push("&ipaddress="+ipAddress);    
        array1.push("&privateport="+privatePort);
        array1.push("&publicport="+publicPort);
        array1.push("&protocol="+protocol);
        array1.push("&virtualmachineid=" + virtualMachineId);
        $.ajax({						
	        data: createURL("command=createPortForwardingRule"+array1.join("")),
	        dataType: "json",
	        success: function(json) {		                      
	            var items = json.createportforwardingruleresponse.portforwardingrule;	  	        	
	            portForwardingJsonToTemplate(items[0],$template);
	            $spinningWheel.hide();   
	            refreshCreatePortForwardingRow();			   						
	        },
		    error: function(XMLHttpResponse) {				    
			    handleError(XMLHttpResponse);
			    $template.slideUp("slow", function() {
					$(this).remove();
				});
		    }	
        });	    
	    
	    return false;
	});
    
    
}

function ipGetMidmenuId(jsonObj) {   
    return "midmenuItem_" + jsonObj.ipaddress.replace(/\./g, "_");   //e.g. "192.168.33.108" => "192_168_33_108"
}

function ipToMidmenu(jsonObj, $midmenuItem1) {    
    var id = ipGetMidmenuId(jsonObj);
    $midmenuItem1.attr("id", id);  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    var $iconContainer = $midmenuItem1.find("#icon_container").show();
    $iconContainer.find("#icon").attr("src", "images/midmenuicon_network_networkgroup.png");
    
    $midmenuItem1.find("#first_row").text(jsonObj.ipaddress.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(fromdb(jsonObj.account).substring(0,25));    
}

function ipToRigntPanel($midmenuItem1) {       
    var jsonObj = $midmenuItem1.data("jsonObj");
    
    //Details tab
    ipJsonToDetailsTab(jsonObj);   
    
    //Port Forwarding tab
    listPortForwardingRules();
    refreshCreatePortForwardingRow(); 
}

function ipJsonToDetailsTab(jsonObj) {   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    $detailsTab.data("jsonObj", jsonObj);      
    
    $detailsTab.find("#ipaddress").text(fromdb(jsonObj.ipaddress));
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#vlanname").text(fromdb(jsonObj.vlanname));    
    setSourceNatField(jsonObj.issourcenat, $detailsTab.find("#source_nat")); 
    setNetworkTypeField(jsonObj.forvirtualnetwork, $detailsTab.find("#network_type"));    
    
    $detailsTab.find("#domain").text(fromdb(jsonObj.domain));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));
    $detailsTab.find("#allocated").text(fromdb(jsonObj.allocated));
}
//***** baseline (end) *********************************************************************************************************************

//***** Details tab (begin) ****************************************************************************************************************
function setSourceNatField(value, $field) {
    if(value == "true")
        $field.text("Yes");
    else if(value == "false")
        $field.text("No");
    else
        $field.text("");
}

function setNetworkTypeField(value, $field) {
    if(value == "true")
        $field.text("Public");
    else if(value == "false")
        $field.text("Direct");
    else
        $field.text("");
}
//***** Details tab (end) ******************************************************************************************************************

//***** Port Forwarding tab (begin) ********************************************************************************************************

//var portForwardingIndex = 0;	
function portForwardingJsonToTemplate(jsonObj, template) {				        
    //(portForwardingIndex++ % 2 == 0)? template.find("#row_container").addClass("smallrow_even"): template.find("#row_container").addClass("smallrow_odd");		    
    template.attr("id", "portForwarding_" + jsonObj.id).data("portForwardingId", jsonObj.id);	
    		     
    template.find("#row_container #public_port").text(jsonObj.publicport);
    template.find("#row_container_edit #public_port").text(jsonObj.publicport);
    
    template.find("#row_container #private_port").text(jsonObj.privateport);
    template.find("#row_container_edit #private_port").val(jsonObj.privateport);
    
    template.find("#row_container #protocol").text(jsonObj.protocol);
    template.find("#row_container_edit #protocol").text(jsonObj.protocol);
    
    template.find("#row_container #vm_name").text(jsonObj.vmname);		    
    var virtualMachineId = jsonObj.virtualmachineid;
   
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    var ipObj = $detailsTab.data("jsonObj");  
    var ipAddress = jsonObj.ipaddress;  
    var IpDomainid = ipObj.domainid;
    var IpAccount = ipObj.account;    
    	    
    $.ajax({
	   data: createURL("command=listVirtualMachines&domainid="+IpDomainid+"&account="+IpAccount+maxPageSize),
	    dataType: "json",
	    success: function(json) {			    
		    var instances = json.listvirtualmachinesresponse.virtualmachine;			   
		    var vmSelect = template.find("#row_container_edit #vm").empty();							
		    if (instances != null && instances.length > 0) {
			    for (var i = 0; i < instances.length; i++) {								
			        var html = $("<option value='" + instances[i].id + "'>" +  getVmName(instances[i].name, instances[i].displayname) + "</option>");							        
		            vmSelect.append(html); 								
			    }
			    vmSelect.val(virtualMachineId);
		    } 
	    }
    });		    
   	   	    	   
    var $rowContainer = template.find("#row_container");      
    var $rowContainerEdit = template.find("#row_container_edit");    
    		    
    template.find("#delete_link").unbind("click").bind("click", function(event){   		                    
        var $spinningWheel = $rowContainer.find("#spinning_wheel");		
        $spinningWheel.find("#description").text("Deleting....");	
        $spinningWheel.show();   
        $.ajax({						
	       data: createURL("command=deletePortForwardingRule&id="+jsonObj.id),
            dataType: "json",
            success: function(json) {             
                template.slideUp("slow", function(){		                    
                    $(this).remove();
                });	   						
            },
            error: function(XMLHttpResponse) {
                handleError(XMLHttpResponse);
                $spinningWheel.hide(); 
            }
        });	     
        return false;
    });
    
    template.find("#edit_link").unbind("click").bind("click", function(event){   		    
        $rowContainer.hide();
        $rowContainerEdit.show();
    });
    
    template.find("#cancel_link").unbind("click").bind("click", function(event){   		    
        $rowContainer.show();
        $rowContainerEdit.hide();
    });
    
    template.find("#save_link").unbind("click").bind("click", function(event){          		       
        // validate values		    
	    var isValid = true;					    
	    isValid &= validateNumber("Private Port", $rowContainerEdit.find("#private_port"), $rowContainerEdit.find("#private_port_errormsg"), 1, 65535);				
	    if (!isValid) return;		    		        
	    
        var $spinningWheel = $rowContainerEdit.find("#spinning_wheel");	                     
        $spinningWheel.find("#description").text("Saving....");	
        $spinningWheel.show();  
	    
        var publicPort = $rowContainerEdit.find("#public_port").text();
        var privatePort = $rowContainerEdit.find("#private_port").val();
        var protocol = $rowContainerEdit.find("#protocol").text();
        var virtualMachineId = $rowContainerEdit.find("#vm").val();		   
	    		    
        var array1 = [];
        array1.push("&publicip="+ipAddress);    
        array1.push("&privateport="+privatePort);
        array1.push("&publicport="+publicPort);
        array1.push("&protocol="+protocol);
        array1.push("&virtualmachineid=" + virtualMachineId);
                      
        $.ajax({
             data: createURL("command=updatePortForwardingRule"+array1.join("")),
			 dataType: "json",
			 success: function(json) {					    									 
				var jobId = json.updateportforwardingruleresponse.jobid;					        
		        var timerKey = "updateportforwardingruleJob"+jobId;
		        
                $("body").everyTime(2000, timerKey, function() {
				    $.ajax({
					   data: createURL("command=queryAsyncJobResult&jobId="+jobId),
					    dataType: "json",
					    success: function(json) {										       						   
						    var result = json.queryasyncjobresultresponse;									    
						    if (result.jobstatus == 0) {
							    return; //Job has not completed
						    } else {											    
							    $("body").stopTime(timerKey);
							    if (result.jobstatus == 1) { // Succeeded										        								    
								    var items = result.portforwardingrule;	            	
                                    portForwardingJsonToTemplate(items[0],template);
                                    $spinningWheel.hide(); 	     
                                    $rowContainerEdit.hide();
                                    $rowContainer.show();                                                      
							    } else if (result.jobstatus == 2) { //Fail
							        $spinningWheel.hide(); 		
						            $("#dialog_alert").html("<p>" + sanitizeXSS(result.jobresult) + "</p>").dialog("open");											    					    
							    }
						    }
					    },
					    error: function(XMLHttpResponse) {	
					        handleError(XMLHttpResponse);								        
						    $("body").stopTime(timerKey);
						    $spinningWheel.hide(); 									    								    
					    }
				    });
			    }, 0);							 
			 },
			 error: function(XMLHttpResponse) {
			     handleError(XMLHttpResponse);		
			     $spinningWheel.hide(); 						 
			 }
		 });                   
    });   
}	  
    
function listPortForwardingRules() {	
    var jsonObj = $("#right_panel_content #tab_content_details").data("jsonObj");          
	var ipAddress = jsonObj.ipaddress;
    if(ipAddress == null || ipAddress.length == 0)
        return;           		
    $.ajax({
        data: createURL("command=listPortForwardingRules&ipaddress=" + ipAddress),
        dataType: "json",
        success: function(json) {	                                    
            var items = json.listportforwardingrulesresponse.portforwardingrule;              
            var $portForwardingGrid = $("#tab_content_port_forwarding #grid_container #grid_content");            
            $portForwardingGrid.empty();                       		    		      	    		
            if (items != null && items.length > 0) {				        			        
                for (var i = 0; i < items.length; i++) {
	                var $template = $("#port_forwarding_template").clone(true);
	                portForwardingJsonToTemplate(items[i], $template); 
	                $portForwardingGrid.append($template.show());						   
                }			    
            } 	        	      		    						
        }
    });
}   

function refreshCreatePortForwardingRow() {      
    var $createPortForwardingRow = $("#create_port_forwarding_row");      
    $createPortForwardingRow.find("#public_port").val("");
    $createPortForwardingRow.find("#private_port").val("");
    $createPortForwardingRow.find("#protocol").val("TCP");  		    
       
    var $detailsTab = $("#right_panel_content #tab_content_details");   
    var jsonObj = $detailsTab.data("jsonObj");    
    var IpDomainid = jsonObj.domainid;
    var IpAccount = jsonObj.account;

    $.ajax({
	    data: createURL("command=listVirtualMachines&domainid="+IpDomainid+"&account="+IpAccount+maxPageSize),
	    dataType: "json",
	    success: function(json) {			    
		    var instances = json.listvirtualmachinesresponse.virtualmachine;
		    var vmSelect = $createPortForwardingRow.find("#vm").empty();							
		    if (instances != null && instances.length > 0) {
			    for (var i = 0; i < instances.length; i++) {								
			        var html = $("<option value='" + instances[i].id + "'>" +  getVmName(instances[i].name, instances[i].displayname) + "</option>");							        
		            vmSelect.append(html); 								
			    }
		    } 
	    }
    });		    
}	
    
//***** Port Forwarding tab (end) **********************************************************************************************************