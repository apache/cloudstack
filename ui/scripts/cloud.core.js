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
 
 $.urlParam = function(name){ var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href); if (!results) { return 0; } return results[1] || 0;} 

//***** actions for a tab in right panel (begin) ************************************************************************
function buildActionLinkForTab(label, actionMap, $actionMenu, $midmenuItem1, $thisTab) { 
    var apiInfo = actionMap[label];
    var $listItem = $("#action_list_item").clone();
    $actionMenu.find("#action_list").append($listItem.show());
    $listItem.find("#link").text(label);   
    $listItem.data("label", label);	  
    $listItem.data("apiInfo", apiInfo);	 
      
    var id = $midmenuItem1.data("jsonObj").id;
    
    $listItem.bind("click", function(event) {   
        $actionMenu.hide();    	 
        var $actionLink = $(this);   
                
        var dialogBeforeActionFn = apiInfo.dialogBeforeActionFn;         
        if(dialogBeforeActionFn == null) {	 
            var apiCommand = "command="+apiInfo.api+"&id="+id;                      
            doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $thisTab); 
        }
        else {
            dialogBeforeActionFn($actionLink, $thisTab, $midmenuItem1);	
        }               
        return false;
    });  
} 

function doActionToTab(id, $actionLink, apiCommand, $midmenuItem1, $thisTab) {  
    var label = $actionLink.data("label");	
    var apiInfo = $actionLink.data("apiInfo");	
    
    var inProcessText = apiInfo.inProcessText;		           
    var isAsyncJob = apiInfo.isAsyncJob;
    var asyncJobResponse = apiInfo.asyncJobResponse;	
    var afterActionSeccessFn = apiInfo.afterActionSeccessFn;	    
            
    var $spinningWheel = $thisTab.find("#spinning_wheel");
    $spinningWheel.find("#description").text(inProcessText);  
    $spinningWheel.show();  
        
    $midmenuItem1.find("#content").removeClass("selected").addClass("inaction");                          
    $midmenuItem1.find("#spinning_wheel").addClass("midmenu_addingloader").show();	
    $midmenuItem1.find("#spinning_wheel").data("inProcessText", inProcessText);
    $midmenuItem1.find("#info_icon").hide();		
    
    var $afterActionInfoContainer = $("#right_panel_content #after_action_info_container_on_top");   
    $afterActionInfoContainer.removeClass("errorbox").hide();       
    
	//Async job (begin) *****
	if(isAsyncJob == true) {	                     
        $.ajax({
            data: createURL(apiCommand),
            dataType: "json",           
            success: function(json) {	                       	                        
                var jobId = json[asyncJobResponse].jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
			                            handleMidMenuItemAfterDetailsTabAction($midmenuItem1, true, (label + " action succeeded."));	
			                            $afterActionInfoContainer.find("#after_action_info").text(label + " action succeeded.");
                                        $afterActionInfoContainer.removeClass("errorbox").show();  
                                        afterActionSeccessFn(json, $midmenuItem1, id);     
			                        } else if (result.jobstatus == 2) { // Failed	
			                            handleMidMenuItemAfterDetailsTabAction($midmenuItem1, false, (label + " action failed. Reason: " + fromdb(result.jobresult.errortext)));		
			                            $afterActionInfoContainer.find("#after_action_info").text(label + " action failed. Reason: " + fromdb(result.jobresult.errortext));
                                        $afterActionInfoContainer.addClass("errorbox").show();     
			                        }											                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {	                            
		                        $("body").stopTime(timerKey);	
								handleError(XMLHttpResponse, function() {
									handleErrorInDetailsTab(XMLHttpResponse, $thisTab, label, $afterActionInfoContainer, $midmenuItem1); 		
								});
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {
				handleError(XMLHttpResponse, function() {
					handleErrorInDetailsTab(XMLHttpResponse, $thisTab, label, $afterActionInfoContainer, $midmenuItem1); 
				});
            }
        });     
    }     
    //Async job (end) *****
    
    //Sync job (begin) *****
    else { 	    
        $.ajax({
            data: createURL(apiCommand),
	        dataType: "json",
	        async: false,
	        success: function(json) {	 	                  
	            $spinningWheel.hide(); 	      
	            handleMidMenuItemAfterDetailsTabAction($midmenuItem1, true, (label + " action succeeded."));	
	            	              
	            $afterActionInfoContainer.find("#after_action_info").text(label + " action succeeded.");
                $afterActionInfoContainer.removeClass("errorbox").show();              	
                                 				
				afterActionSeccessFn(json, $midmenuItem1, id);				
	        },
            error: function(XMLHttpResponse) {
				handleError(XMLHttpResponse, function() {
					handleErrorInDetailsTab(XMLHttpResponse, $thisTab, label, $afterActionInfoContainer, $midmenuItem1); 
				});
            }        
        });
    }
    //Sync job (end) *****
}

function handleErrorInDetailsTab(XMLHttpResponse, $thisTab, label, $afterActionInfoContainer, $midmenuItem1) {     
    var errorMsg = "";
    if(XMLHttpResponse.responseText != null & XMLHttpResponse.responseText.length > 0) {        
        errorMsg = parseXMLHttpResponse(XMLHttpResponse);	
    }        
   
    var afterActionInfo;
    if(errorMsg.length > 0) 
        afterActionInfo = label + " action failed. Reason: " + fromdb(errorMsg);
    else
        afterActionInfo = label + " action failed.";
    
    $afterActionInfoContainer.find("#after_action_info").text(afterActionInfo);         
    $afterActionInfoContainer.addClass("errorbox").show();
    
    $thisTab.find("#spinning_wheel").hide();  
    handleMidMenuItemAfterDetailsTabAction($midmenuItem1, false, afterActionInfo);	 	
}  

function handleMidMenuItemAfterDetailsTabAction($midmenuItem1, isSuccessful, afterActionInfo) {
    $midmenuItem1.find("#content").removeClass("inaction");
	$midmenuItem1.find("#spinning_wheel").hide();	
	
	$midmenuItem1.data("afterActionInfo", afterActionInfo); 
	
	var $infoIcon = $midmenuItem1.find("#info_icon").show();
	if(isSuccessful)
	    $infoIcon.removeClass("error");	    
	else
	    $infoIcon.addClass("error");	    
}
  	                
//***** actions for a tab in right panel (end) **************************************************************************

//***** actions for a subgrid item in right panel (begin) ************************************************************************
function buildActionLinkForSubgridItem(label, actionMap, $actionMenu, $subgridItem) {
    var apiInfo = actionMap[label];
    var $listItem = $("#action_list_item").clone();
    $actionMenu.find("#action_list").append($listItem.show());    
    $listItem.find("#link").text(label);   
    $listItem.data("label", label);	  
    $listItem.data("apiInfo", apiInfo);	 
    
    var id = $subgridItem.data("jsonObj").id;
    
    $listItem.bind("click", function(event) {   
        $actionMenu.hide();    	 
        var $actionLink = $(this);   
        
        var dialogBeforeActionFn = apiInfo.dialogBeforeActionFn;    
        if(dialogBeforeActionFn == null) {	 
            var apiCommand = "command="+apiInfo.api+"&id="+id;                      
            doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem); 
        }
        else {
            dialogBeforeActionFn($actionLink, $subgridItem);	
        }                        
        return false;
    });  
} 

function doActionToSubgridItem(id, $actionLink, apiCommand, $subgridItem) {       
    var label = $actionLink.data("label");	
    var apiInfo = $actionLink.data("apiInfo");	
    
    var inProcessText = apiInfo.inProcessText;		           
    var isAsyncJob = apiInfo.isAsyncJob;
    var asyncJobResponse = apiInfo.asyncJobResponse;	
    var afterActionSeccessFn = apiInfo.afterActionSeccessFn;	 
           
    var $spinningWheel = $subgridItem.find("#spinning_wheel");
    $spinningWheel.find("#description").text(inProcessText);  
    $spinningWheel.show();  
    $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").hide();      
    
	//Async job (begin) *****
	if(isAsyncJob == true) {	                     
        $.ajax({
            data: createURL(apiCommand),
            dataType: "json",           
            success: function(json) {	                    	                        
                var jobId = json[asyncJobResponse].jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
			                            $subgridItem.find("#after_action_info").text(label + " action succeeded.");
                                        $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").show();  
			                            afterActionSeccessFn(json, id, $subgridItem);	
			                        } else if (result.jobstatus == 2) { // Failed
			                            $subgridItem.find("#after_action_info").text(label + " action failed. Reason: " + fromdb(result.jobresult.errortext));
                                        $subgridItem.find("#after_action_info_container").removeClass("success").addClass("error").show();			                          
			                        }											                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {	                  
		                        $("body").stopTime(timerKey);
								handleError(XMLHttpResponse, function() { 
									handleErrorInSubgridItem(XMLHttpResponse, $subgridItem, label); 
								});
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {	 
				handleError(XMLHttpResponse, function() {
					handleErrorInSubgridItem(XMLHttpResponse, $subgridItem, label);
				});
            }
        });     
    }     
    //Async job (end) *****
    
    //Sync job (begin) *****
    else { 	            
        $.ajax({
            data: createURL(apiCommand),
	        dataType: "json",
	        async: false,
	        success: function(json) {	   
	            $spinningWheel.hide();   
	            $subgridItem.find("#after_action_info").text(label + " action succeeded.");
                $subgridItem.find("#after_action_info_container").removeClass("error").addClass("success").show();  
                afterActionSeccessFn(json, id, $subgridItem);    
	        },
            error: function(XMLHttpResponse) {
				handleError(XMLHttpResponse, function() {
					handleErrorInSubgridItem(XMLHttpResponse, $subgridItem, label);    
				});
            }        
        });
    }
    //Sync job (end) *****
}

function handleErrorInSubgridItem(XMLHttpResponse, $subgridItem, label) { 
    $subgridItem.find("#spinning_wheel").hide();      
		                        
    var errorMsg = "";
    if(XMLHttpResponse.responseText != null & XMLHttpResponse.responseText.length > 0) {
        errorMsg = parseXMLHttpResponse(XMLHttpResponse);		
    }
    if(errorMsg.length > 0)  
        $subgridItem.find("#after_action_info").text(label + " action failed. Reason: " + fromdb(errorMsg));    
    else     
        $subgridItem.find("#after_action_info").text(label + " action failed."); 
        
	$subgridItem.find("#after_action_info_container").removeClass("success").addClass("error").show();  
}    	                
//***** actions for a subgrid item in right panel (end) **************************************************************************

//***** actions for middle menu (begin) ************************************************************************
var selectedItemsInMidMenu = {};

function buildActionLinkForMidMenu(label, actionMap, $actionMenu) {
    var apiInfo = actionMap[label];
    var $listItem = $("#action_list_item_middle_menu").clone();
    $actionMenu.find("#action_list").append($listItem.show());
    var $link = $listItem.find("#link").text(label);
    $link.data("apiInfo", apiInfo);
    
    /*
    $link.data("label", label);	  
    $link.data("api",apiInfo .api);				                 
    $link.data("isAsyncJob", apiInfo.isAsyncJob);
    $link.data("asyncJobResponse", apiInfo.asyncJobResponse);		     
    $link.data("afterActionSeccessFn", apiInfo.afterActionSeccessFn);
    $link.data("dialogBeforeActionFn", apiInfo.dialogBeforeActionFn);
    */
    
    $link.bind("click", function(event) {	
        $actionMenu.hide();    	 
                
        var itemCounts = 0;
        for(var id in selectedItemsInMidMenu) {
            itemCounts ++;
        }
        if(itemCounts == 0) {
            $("#dialog_info_please_select_one_item_in_middle_menu").dialog("open");		
            return;
        }        
        
        var $actionLink = $(this);   
        var dialogBeforeActionFn = $actionLink.data("dialogBeforeActionFn"); 
        if(dialogBeforeActionFn == null) {		                   
            for(var id in selectedItemsInMidMenu) {	
                var apiCommand = "command="+$actionLink.data("api")+"&id="+id;                      
                doActionToMidMenu(id, apiInfo, apiCommand); 	
            }
        }
        else {
            dialogBeforeActionFn(apiInfo, selectedItemsInMidMenu);	
            
        }        
        selectedItemsInMidMenu = {}; //clear selected items for action	                          
        return false;
    });  
} 

function doActionToMidMenu(id, apiInfo, apiCommand) {   
    var label = apiInfo.label;			           
    var isAsyncJob = apiInfo.isAsyncJob;
    var inProcessText = apiInfo.inProcessText;   
    var asyncJobResponse = apiInfo.asyncJobResponse;	
    var afterActionSeccessFn = apiInfo.afterActionSeccessFn;	    
        
    var $midmenuItem1 = $("#midmenuItem_"+id);	
    $midmenuItem1.find("#content").removeClass("selected").addClass("inaction");                          
    $midmenuItem1.find("#spinning_wheel").addClass("midmenu_addingloader").show();	
    $midmenuItem1.find("#spinning_wheel").data("inProcessText", inProcessText);
    $midmenuItem1.find("#info_icon").hide();		  
	
	var $detailsTab = $("#right_panel_content #tab_content_details");  
    var $spinningWheel = $detailsTab.find("#spinning_wheel");
    $spinningWheel.find("#description").text(inProcessText);  
    $spinningWheel.show();     
	
	//Async job (begin) *****
	if(isAsyncJob == true) {	                     
        $.ajax({
            data: createURL(apiCommand),
            dataType: "json",           
            success: function(json) {	                	                        
                var jobId = json[asyncJobResponse].jobid;                  			                        
                var timerKey = "asyncJob_" + jobId;					                       
                $("body").everyTime(
                    10000,
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
			                        $midmenuItem1.find("#content").removeClass("inaction");
			                        $midmenuItem1.find("#spinning_wheel").hide();			                        
			                        hideDetailsTabActionSpinningWheel(id, inProcessText);				                        			                       
			                        if (result.jobstatus == 1) { // Succeeded  
			                            $midmenuItem1.find("#info_icon").removeClass("error").show();
			                            $midmenuItem1.data("afterActionInfo", (label + " action succeeded.")); 			                            
			                            afterActionSeccessFn(json, $midmenuItem1, id);  			                            
			                        } else if (result.jobstatus == 2) { // Failed	
			                            $midmenuItem1.find("#info_icon").addClass("error").show();
			                            $midmenuItem1.data("afterActionInfo", (label + " action failed. Reason: " + fromdb(result.jobresult.errortext)));    
			                        }											                    
		                        }
	                        },
	                        error: function(XMLHttpResponse) {
		                        $("body").stopTime(timerKey);		                       		                        
		                        handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1, id, inProcessText); 		                        
	                        }
                        });
                    },
                    0
                );
            },
            error: function(XMLHttpResponse) {
				handleError(XMLHttpResponse);
		        handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1, id, inProcessText);    
            }
        });     
    }     
    //Async job (end) *****
    
    //Sync job (begin) *****
    else { 	              
        $.ajax({
            data: createURL(apiCommand),
	        dataType: "json",
	        async: false,
	        success: function(json) {
	            $midmenuItem1.find("#content").removeClass("inaction");
				$midmenuItem1.find("#spinning_wheel").hide();				
				$midmenuItem1.find("#info_icon").removeClass("error").show();
			    $midmenuItem1.data("afterActionInfo", (label + " action succeeded.")); 		
			    hideDetailsTabActionSpinningWheel(id, inProcessText);	
				afterActionSeccessFn(json, $midmenuItem1, id); 		
	        },
            error: function(XMLHttpResponse) {	                
		        handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1, id, inProcessText);    
            }        
        });
    }
    //Sync job (end) *****
}

function handleErrorInMidMenu(XMLHttpResponse, $midmenuItem1, id, inProcessText) { 
    $midmenuItem1.find("#content").removeClass("inaction");
	$midmenuItem1.find("#spinning_wheel").hide();	
	$midmenuItem1.find("#info_icon").addClass("error").show();		
	$midmenuItem1.find("#first_row").text("Action failed");		
	hideDetailsTabActionSpinningWheel(id, inProcessText);		
		                        
    var errorMsg = "";
    if(XMLHttpResponse.responseText != null & XMLHttpResponse.responseText.length > 0) {
        errorMsg = parseXMLHttpResponse(XMLHttpResponse);		
    }
    if(errorMsg.length > 0) 
        $midmenuItem1.find("#second_row").text(fromdb(errorMsg));   
    else
        $midmenuItem1.find("#second_row").html("&nbsp;");     
}  

function hideDetailsTabActionSpinningWheel(id, inProcessText) {			                        
    var $detailsTab = $("#right_panel_content #tab_content_details");  
    var jsonObj = $detailsTab.data("jsonObj");
    var $spinningWheel = $detailsTab.find("#spinning_wheel");         
    if(jsonObj != null && ("id" in jsonObj) && jsonObj.id == id && ($spinningWheel.find("#description").text() == inProcessText)) {                                                                           
        $spinningWheel.hide();     
    }
}	

/*
If Cancel button in dialog is clicked, action won't preceed. 
i.e. doActionToMidMenu() won't get called => highlight won't be removd from middle menu. 
So, we need to remove highlight here. Otherwise, it won't be consistent of selectedItemsInMidMenu which will be emptied soon.
*/
function removeHighlightInMiddleMenu(selectedItemsInMidMenu) {
    for(var id in selectedItemsInMidMenu) {
        var $midmenuItem1 = $("#midmenuItem_"+id);	
        $midmenuItem1.find("#content").removeClass("selected");
    }
}	     	

function copyActionInfoFromMidMenuToRightPanel($midmenuItem1) {     
    var $afterActionInfoContainer = $("#right_panel_content #after_action_info_container_on_top");       
    
    if($midmenuItem1.find("#info_icon").css("display") != "none") {                
        $afterActionInfoContainer.find("#after_action_info").text($midmenuItem1.data("afterActionInfo"));     
        if($midmenuItem1.find("#info_icon").hasClass("error"))
            $afterActionInfoContainer.addClass("errorbox");
        else
            $afterActionInfoContainer.removeClass("errorbox");                                        
        $afterActionInfoContainer.show(); 
        
        $midmenuItem1.find("#info_icon").hide(); //make info icon disappear after the middle menu item is clicked once                                    
    }  
    else {
        $afterActionInfoContainer.find("#after_action_info").text("");
        $afterActionInfoContainer.hide();                
    }  
    
    var $midMenuSpinningWheel = $midmenuItem1.find("#spinning_wheel");
    if($midMenuSpinningWheel.css("display") != "none") { 
        var $detailsTabSpinningWheel = $("#right_panel_content #tab_content_details").find("#spinning_wheel");
        if($detailsTabSpinningWheel.css("display") == "none") {
            var inProcessText = $midMenuSpinningWheel.data("inProcessText");
            $detailsTabSpinningWheel.find("#description").text(inProcessText);  
            $detailsTabSpinningWheel.show();  
        }
    }
}
                
//***** actions for middle menu (end) **************************************************************************


function createURL(url) {
    return url +"&response=json&sessionkey=" + g_sessionKey;
}

function fromdb(val) {
    return sanitizeXSS(noNull(val));
}

function todb(val) {
    return encodeURIComponent(val);
}

var midmenuItemCount = 20;

function setBooleanReadField(value, $field) {
    if(value == "true" || value == true)
        $field.text("Yes").show();
    else if(value == "false" || value == false)
        $field.text("No").show();	
    else
        $field.hide();
}

function setBooleanEditField(value, $field) {
    if(value == "true" || value == true)
        $field.val("true");
    else if(value == "false" || value == false)
        $field.val("false");	
    else
        $field.hide();
}

 var LeftMenuAdvancedSearchMap = {
    "leftmenu_instances_my_instances": "advanced_search_popup_nodomainaccount",
    "leftmenu_instances_running_instances": "advanced_search_popup_nostate",
    "leftmenu_instances_stopped_instances": "advanced_search_popup_nostate",
    "leftmenu_instances_destroyed_instances": "advanced_search_popup_nostate"
}			

function getAdvancedSearchPopup($container) {
    var $advancedSearchPopup;    
    if(currentLeftMenuId in LeftMenuAdvancedSearchMap)
        $advancedSearchPopup = $container.find("#"+LeftMenuAdvancedSearchMap[currentLeftMenuId]);
    else
        $advancedSearchPopup = $container.find("#advanced_search_popup");  //default
    return $advancedSearchPopup;
}

function getAdvancedSearchPopupInHiddenContainer() {
    return getAdvancedSearchPopup($("#hidden_container"));
}

function getAdvancedSearchPopupInSearchContainer() {
    return getAdvancedSearchPopup($("#advanced_search_container"));
}

function clearMiddleMenu() {
    $("#midmenu_container").empty();
    $("#midmenu_action_link").hide();       
    $("#basic_search").find("#search_input").val("");
    $("#advanced_search_icon").removeClass("up");    
    
    if($("#hidden_container").length > 0)   
	    getAdvancedSearchPopupInSearchContainer().hide().appendTo($("#hidden_container"));     
	else
	    getAdvancedSearchPopupInSearchContainer().remove();                
    
    $("#midmenu_prevbutton, #midmenu_nextbutton").hide();
    $("#middle_menu_pagination").data("params", null);
}

function clearButtonsOnTop() {
	return $("#top_button_container").empty(); 
}

function clearRightPanel() {
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
     
    $("#right_panel_content #tab_content_details #spinning_wheel").hide();
    $("#right_panel_content #after_action_info_container").hide(); 
}
    

var $selectedLeftMenu;
var $expandedLeftMenu;
function selectLeftMenu($menuToSelect, expandable, afterSelectFn) {
	if ($selectedLeftMenu == null || ($menuToSelect.attr("id") != $selectedLeftMenu.attr("id"))) {
		if($selectedLeftMenu != null) {
			$selectedLeftMenu.removeClass("selected");  
			$selectedLeftMenu.find(".leftmenu_arrows_firstlevel_open").hide();
		}
		$menuToSelect.addClass("selected");
		$menuToSelect.find(".leftmenu_arrows_firstlevel_open").show();
		$selectedLeftMenu = $menuToSelect; 
		
		// collapse any current expanded menu
		var $menuToExpand;
		if (expandable != undefined && expandable) {
			$menuToExpand = $selectedLeftMenu.siblings(".leftmenu_expandedbox");
		}
		
		if ($expandedLeftMenu != null) {
			$expandedLeftMenu.slideUp(500, afterSelectFn);
			if ($menuToExpand != null) $menuToExpand.slideDown(500);
			$expandedLeftMenu = null;
		} else if ($menuToExpand != null) {
			$menuToExpand.slideDown(500, afterSelectFn);
		} else if (afterSelectFn != undefined) {
			afterSelectFn();
		}
		$expandedLeftMenu = $menuToExpand;
		
		// Close the help link if it's opened
		$("#help_dropdown_dialog").hide();
		$("#help_button").removeClass("selected");
		return true;
	}
	return false;
}

var $selectedSubMenu;
function selectLeftSubMenu($menuToSelect) {
	if ($selectedSubMenu == null || ($menuToSelect.attr("id") != $selectedSubMenu.attr("id"))) {
		if($selectedSubMenu != null)
			$selectedSubMenu.removeClass("selected");  
		$menuToSelect.addClass("selected");
		$selectedSubMenu = $menuToSelect; 
	}
	// Every time you click on a different menu item, we should stop all previous async job queries
	$("body").stopTime();
}

var selected_midmenu_id = null; 
function hideMiddleMenu() {
    $("#middle_menu, #search_panel, #middle_menu_pagination").hide();
    $("#midmenu_container").empty();
    $("#right_panel").removeClass("main_contentarea_with_midmenu").addClass("main_contentarea_without_midmenu");            
}
function showMiddleMenu() {
    $("#middle_menu, #search_panel, #middle_menu_pagination").show();   
    $("#right_panel").removeClass("main_contentarea_without_midmenu").addClass("main_contentarea_with_midmenu");
} 

function isMiddleMenuShown() {
    if($("#middle_menu").css("display") == "none")
        return false;
    else
        return true;
}

// adding middle menu item ***
function beforeAddingMidMenuItem() {
    var $midmenuItem1 = $("#midmenu_item").clone().attr("id", "midmenu_item_clone");
	$midmenuItem1.find("#first_row").text("Adding....");    	
	$midmenuItem1.find("#second_row").html("&nbsp;");    			
	$midmenuItem1.find("#content").addClass("inaction"); 
	$midmenuItem1.find("#spinning_wheel").show();
	$midmenuItem1.find("#info_icon").removeClass("error").hide();
	$("#midmenu_container").prepend($midmenuItem1.show());
	return $midmenuItem1;
}
function afterAddingMidMenuItem($midmenuItem1, isSuccessful, extraMessage) {
    $midmenuItem1.find("#content").removeClass("inaction"); 
	$midmenuItem1.find("#spinning_wheel").hide();	

    if(isSuccessful == true) {
        $midmenuItem1.find("#info_icon").removeClass("error").show();
	    $midmenuItem1.data("afterActionInfo", ("Adding succeeded.")); 
	    
	    var $container = $("#midmenu_container");
	    if($("#midmenu_container").find("#multiple_selection_sub_container").length > 0) {
	        $container = $("#midmenu_container").find("#multiple_selection_sub_container");
	        $midmenuItem1.appendTo("#midmenu_container #multiple_selection_sub_container"); 
	    }
	    	        
		var $noItemsAvailable = $container.find("#midmenu_container_no_items_available");
        if($noItemsAvailable.length > 0) {
            $noItemsAvailable.slideUp("slow", function() {
                $(this).remove();
            });
        }		   
	}
	else {	
	    $midmenuItem1.find("#content").addClass("addingfailed");	        	    
	    $midmenuItem1.find("#icon").attr("src", "images/addingfailed_icon.png");
	    $midmenuItem1.find("#icon_container").show();	    
	    $midmenuItem1.find("#first_row").text("Adding failed");	
	    
	    $midmenuItem1.find("#close_icon").show().bind("click", function(event) {	        
	        $midmenuItem1.slideUp("slow", function() {	            
	            $(this).remove();
	        });	        
	        return false;
	    });	    
	}
	
	if(extraMessage != null) {
	    $midmenuItem1.find("#second_row").text(extraMessage);  	 
	}
}

function bindClickToMidMenu($midmenuItem1, toRightPanelFn, getMidmenuIdFn) {
    $midmenuItem1.bind("click", function(event){  
        var thisMidmenuItem = $(this);
        
        if(selected_midmenu_id != null && selected_midmenu_id.length > 0)
            $("#"+selected_midmenu_id).find("#content").removeClass("selected");
        selected_midmenu_id = getMidmenuIdFn(thisMidmenuItem.data("jsonObj"));
               
        thisMidmenuItem.find("#content").addClass("selected");  
                                              
        clearRightPanel();        
        toRightPanelFn(thisMidmenuItem);	  
        return false;
    }); 
}

function switchBetweenDifferentTabs(tabArray, tabContentArray, afterSwitchFnArray) {        
    for(var tabIndex=0; tabIndex<tabArray.length; tabIndex++) {  
        switchToTab(tabIndex, tabArray, tabContentArray, afterSwitchFnArray);
    }
}

function switchToTab(tabIndex, tabArray, tabContentArray, afterSwitchFnArray) {
  tabArray[tabIndex].bind("click", function(event){               
        tabArray[tabIndex].removeClass("off").addClass("on"); //current tab turns on
        for(var k=0; k<tabArray.length; k++) { 
            if(k != tabIndex)
                tabArray[k].removeClass("on").addClass("off");  //other tabs turns off
        }                    
                        
        tabContentArray[tabIndex].show();    //current tab content shows             
        for(var k=0; k<tabContentArray.length; k++) {
            if(k != tabIndex)
                tabContentArray[k].hide();   //other tab content hide
        }   
        
        if(afterSwitchFnArray != null) {
            if(afterSwitchFnArray[tabIndex] != null)
                afterSwitchFnArray[tabIndex]();  
        }         
        return false;
    });   
}

function updateVmStateInMidMenu(jsonObj, $midmenuItem1) {         
    if(jsonObj.state == "Running")
        $midmenuItem1.find("#icon").attr("src", "images/status_green.png");
    else if(jsonObj.state == "Stopped")
        $midmenuItem1.find("#icon").attr("src", "images/status_red.png");
    else  //Destroyed, Creating, ~                                  
        $midmenuItem1.find("#icon").attr("src", "images/status_gray.png");
    
    $midmenuItem1.find("#icon_container").show();
}
 
function updateHostStateInMidMenu(jsonObj, $midmenuItem1) {         
    if(jsonObj.state == "Up" || jsonObj.state == "Connecting")
        $midmenuItem1.find("#icon").attr("src", "images/status_green.png");
    else if(jsonObj.state == "Down" || jsonObj.state == "Alert")
        $midmenuItem1.find("#icon").attr("src", "images/status_red.png");
    else  //ErrorInMaintenance, PrepareForMaintenance, Maintenance, Disconnected                               
        $midmenuItem1.find("#icon").attr("src", "images/status_gray.png");
    
    $midmenuItem1.find("#icon_container").show();
} 

function enableConsoleHover($viewConsoleTemplate) {
    var imgUrl = $viewConsoleTemplate.data("imgUrl");		
    var time = new Date();	
	
	$viewConsoleTemplate.find("#box0").css("background", "url("+imgUrl+"&t="+time.getTime()+")");
	$viewConsoleTemplate.find("#box1").css("background", "url("+imgUrl+"&t="+time.getTime()+")");
				
	var index = 0;
	$viewConsoleTemplate.everyTime(2000, function() {
		var time = new Date();	
		if ((index % 2) == 0) {
			$viewConsoleTemplate.find("#box0").hide().css("background", "url("+imgUrl+"&t="+time.getTime()+")");
			$viewConsoleTemplate.find("#box1").show();
		} else {
			$viewConsoleTemplate.find("#box1").hide().css("background", "url("+imgUrl+"&t="+time.getTime()+")");
			$viewConsoleTemplate.find("#box0").show();
		}
		index++;
	}, 0);	
} 
  
function disableConsoleHover($viewConsoleTemplate) {
    $viewConsoleTemplate.stopTime();		  
} 
  
function resetViewConsoleAction(jsonObj, $detailsTab) {
    var $viewConsoleContainer = $detailsTab.find("#view_console_container").empty(); //reset view console panel
    var $viewConsoleTemplate = $("#view_console_template").clone();
    $viewConsoleContainer.append($viewConsoleTemplate.show());       
	if (jsonObj.state == 'Running') {	
	    //console proxy thumbnail
	    var imgUrl = "console?cmd=thumbnail&vm=" + jsonObj.id + "&w=144&h=110";		
		$viewConsoleTemplate.data("imgUrl", imgUrl);
		$viewConsoleTemplate.bind("mouseover", function(event) {
		    enableConsoleHover($(this));
	    });
	    $viewConsoleTemplate.bind("mouseout", function(event) {
		    disableConsoleHover($(this));		      
	    });
				
		//console proxy popup
		$viewConsoleTemplate.data("proxyUrl", "console?cmd=access&vm=" + jsonObj.id).data("vmId",jsonObj.id).click(function(event) {				
			var proxyUrl = $(this).data("proxyUrl");				
			//proxyUrl = "http://localhost:8080/client/" + proxyUrl;  //***** temporary hack. This line will be removed after new UI code (/ui/new/*) moves to /ui/*
			var viewer = window.open(proxyUrl, $(this).data("vmId"),"width=820,height=640,resizable=yes,menubar=no,status=no,scrollbars=no,toolbar=no,location=no");
			viewer.focus();
			return false;
		});	
	} 	
}    

function setVmStateInRightPanel(stateValue, $stateField) {    
    if(stateValue == "Running")
        $stateField.text(stateValue).removeClass("red gray").addClass("green");
    else if(stateValue == "Stopped")
        $stateField.text(stateValue).removeClass("green gray").addClass("red");
    else  //Destroyed, Creating, ~                                  
        $stateField.text(stateValue).removeClass("green red").addClass("gray");            			       
}

function setHostStateInRightPanel(stateValue, $stateField) {    
    if(stateValue == "Up" || stateValue == "Connecting")
        $stateField.text(stateValue).removeClass("status_red status_gray").addClass("status_green");
    else if(stateValue == "Down" || stateValue == "Alert" || stateValue == "ErrorInMaintenance")
        $stateField.text(stateValue).removeClass("status_green status_gray").addClass("status_red");
    else  //"PrepareForMaintenance", "Maintenance", "Disconnected"                                
        $stateField.text(stateValue).removeClass("status_green status_red").addClass("status_gray");            			       
}

function setTemplateStateInRightPanel(stateValue, $stateField) {   
    $stateField.text(stateValue);    
     
    if(stateValue == "Ready")
        $stateField.text(stateValue).removeClass("status_red").addClass("status_green");
    else 
        $stateField.text(stateValue).removeClass("status_green").addClass("status_red");              			       
}

function initDialog(elementId, width1, addToActive) {
	if(width1 == null) {
	    activateDialog($("#"+elementId).dialog({    	            
	        autoOpen: false,
	        modal: true,
	        zIndex: 2000
        }), addToActive); 
    }
    else {
        activateDialog($("#"+elementId).dialog({ 
   	        width: width1,	
	        autoOpen: false,
	        modal: true,
	        zIndex: 2000
        }), addToActive); 
    }
} 

function initDialogWithOK(elementId, width1, addToActive) {
	if(width1 == null) {
	    activateDialog($("#"+elementId).dialog({    	            
	        autoOpen: false,
	        modal: true,
	        zIndex: 2000,
	        buttons: { "OK": function() { $(this).dialog("close"); } }
        }), addToActive); 
    }
    else {
        activateDialog($("#"+elementId).dialog({ 
   	        width: width1,	
	        autoOpen: false,
	        modal: true,
	        zIndex: 2000,
	        buttons: { "OK": function() { $(this).dialog("close"); } }
        }), addToActive); 
    }
} 

function createMultipleSelectionSubContainer() {      
    var $multipleSelectionSubContainer = $("<div id='multiple_selection_sub_container'></div>"); 
    $("#midmenu_container").append($multipleSelectionSubContainer);    
  
    $multipleSelectionSubContainer.selectable({
        selecting: function(event, ui) {	 	                               
            if(ui.selecting.id.indexOf("midmenuItem") != -1) {                     
                var $midmenuItem1 = $("#"+ui.selecting.id);
                if($midmenuItem1.find("#content").hasClass("inaction") == false) { //only items not in action are allowed to be selected
                    var id =$midmenuItem1.data("jsonObj").id;                
                    selectedItemsInMidMenu[id] = $midmenuItem1; 
                    $midmenuItem1.find("#content").addClass("selected");   
                }                               
                clearRightPanel();      
                var toRightPanelFn = $midmenuItem1.data("toRightPanelFn");
                toRightPanelFn($midmenuItem1);	          
            }                                             
        },
        unselecting: function(event, ui) {
            if(ui.unselecting.id.indexOf("midmenuItem") != -1) {                     
                var $midmenuItem1 = $("#"+ui.unselecting.id);
                var id = $midmenuItem1.data("jsonObj").id;
                if(id in selectedItemsInMidMenu) {                    
                    delete selectedItemsInMidMenu[id];
                    $midmenuItem1.find("#content").removeClass("selected"); 
                }
            }             
        }
    }); 
    
    return $multipleSelectionSubContainer;   
}

function getMidmenuId(jsonObj) {
    return "midmenuItem_" + jsonObj.id; 
}

var lastSearchType;
var currentCommandString;
function listMidMenuItems2(commandString, getSearchParamsFn, jsonResponse1, jsonResponse2, toMidmenuFn, toRightPanelFn, getMidmenuIdFn, isMultipleSelectionInMidMenu, page) {                
	var params = {
        "commandString": commandString,
        "getSearchParamsFn": getSearchParamsFn,
        "jsonResponse1": jsonResponse1,
        "jsonResponse2": jsonResponse2,
        "toMidmenuFn": toMidmenuFn,
        "toRightPanelFn": toRightPanelFn,
        "getMidmenuIdFn": getMidmenuIdFn,
        "isMultipleSelectionInMidMenu": isMultipleSelectionInMidMenu,        
        "page": page
    }                    
    $("#middle_menu_pagination").data("params", params);
	
	(page > 1)? $("#midmenu_prevbutton").show(): $("#midmenu_prevbutton").hide();
	
	//var $container = $("#midmenu_container").empty(); 
	//if(isMultipleSelectionInMidMenu == true)
    //    $container = createMultipleSelectionSubContainer();
       
    var count = 0;    
    $.ajax({
        cache: false,
        data: createURL("command="+commandString+getSearchParamsFn()+"&pagesize="+midmenuItemCount+"&page="+page),
        dataType: "json",
        async: false,
        success: function(json) {  
			var $container = $("#midmenu_container").empty(); 
			if(isMultipleSelectionInMidMenu == true)
				$container = createMultipleSelectionSubContainer();
		
            selectedItemsInMidMenu = {};                           
            var items = json[jsonResponse1][jsonResponse2];      
            if(items != null && items.length > 0) {
                (items.length == midmenuItemCount)? $("#midmenu_nextbutton").show(): $("#midmenu_nextbutton").hide();
                for(var i=0; i<items.length;i++) { 
                    var $midmenuItem1 = $("#midmenu_item").clone();                      
                    $midmenuItem1.data("toRightPanelFn", toRightPanelFn);                             
                    toMidmenuFn(items[i], $midmenuItem1);    
                    bindClickToMidMenu($midmenuItem1, toRightPanelFn, getMidmenuIdFn);             
                      
                    $container.append($midmenuItem1.show());   
                    if(i == 0)  { //click the 1st item in middle menu as default                        
                        $midmenuItem1.click();                           
                        if(isMultipleSelectionInMidMenu == true) {                           
                            $midmenuItem1.addClass("ui-selected");  //because instance page is using JQuery selectable widget to do multiple-selection
                            selectedItemsInMidMenu[items[i].id] = $midmenuItem1; //because instance page is using JQuery selectable widget to do multiple-selection                        
                        }                        
                    }                 
                }  
                count = items.length;
            }  
            else {
                $container.append($("#midmenu_container_no_items_available").clone().show());  
            }
            $("#midmenu_container").show();
	        $("#midmenu_spinning_wheel").hide();           
        }
    });	 
    
    return count;
}

var currentLeftMenuId;
var currentRightPanelJSP = null;
function listMidMenuItems(commandString, getSearchParamsFn, jsonResponse1, jsonResponse2, rightPanelJSP, afterLoadRightPanelJSPFn, toMidmenuFn, toRightPanelFn, getMidmenuIdFn, isMultipleSelectionInMidMenu, leftmenuId, refreshDataBindingFn) { 
	clearMiddleMenu();
	showMiddleMenu();	
	$("#midmenu_container").hide();
	$("#midmenu_spinning_wheel").show();
	
	currentLeftMenuId = leftmenuId;
	$("#right_panel").data("onRefreshFn", function() {
	    $("#"+leftmenuId).click();
	});

	if (currentRightPanelJSP != rightPanelJSP) {
		$("#right_panel").load(rightPanelJSP, function(){     
		    currentRightPanelJSP = rightPanelJSP;
		    
		    var $topButtonContainer = clearButtonsOnTop();			    	       
		    $("#top_buttons").appendTo($topButtonContainer); 
		    
			removeDialogs();			
								    
			var $actionLink = $("#right_panel_content #tab_content_details #action_link");
			$actionLink.bind("mouseover", function(event) {	    
				$(this).find("#action_menu").show();    
				return false;
			});
			$actionLink.bind("mouseout", function(event) {       
				$(this).find("#action_menu").hide();    
				return false;
			});	   
			
			afterLoadRightPanelJSPFn();                
			listMidMenuItems2(commandString, getSearchParamsFn, jsonResponse1, jsonResponse2, toMidmenuFn, toRightPanelFn, getMidmenuIdFn, isMultipleSelectionInMidMenu, 1);   			
		});     
	} else {	    
	    if(refreshDataBindingFn != null)
	        refreshDataBindingFn();
	
		listMidMenuItems2(commandString, getSearchParamsFn, jsonResponse1, jsonResponse2, toMidmenuFn, toRightPanelFn, getMidmenuIdFn, isMultipleSelectionInMidMenu, 1);        
	}
	return false;
}

function bindAndListMidMenuItems($leftmenu, commandString, getSearchParamsFn, jsonResponse1, jsonResponse2, rightPanelJSP, afterLoadRightPanelJSPFn, toMidmenuFn, toRightPanelFn, getMidmenuIdFn, isMultipleSelectionInMidMenu) {	
	$leftmenu.bind("click", function(event) {
		selectLeftSubMenu($(this));		
        listMidMenuItems(commandString, getSearchParamsFn, jsonResponse1, jsonResponse2, rightPanelJSP, afterLoadRightPanelJSPFn, toMidmenuFn, toRightPanelFn, getMidmenuIdFn, isMultipleSelectionInMidMenu, $(this).attr("id"), null);
        return false;
    });
}

function handleErrorInDialog(XMLHttpResponse, $thisDialog) {    
	var errorMsg = parseXMLHttpResponse(XMLHttpResponse);	
	handleErrorInDialog2(errorMsg, $thisDialog);
}

function handleErrorInDialog2(errorMsg, $thisDialog) { 
	var $infoContainer = $thisDialog.find("#info_container");

	if(errorMsg != null && errorMsg.length > 0) 	    
	    $infoContainer.find("#info").text(fromdb(errorMsg));	
	else 
	    $infoContainer.find("#info").text("action failed");	
	
	$thisDialog.find("#spinning_wheel").hide();
	$infoContainer.show();
}

function parseXMLHttpResponse(XMLHttpResponse) {
    var start = XMLHttpResponse.responseText.indexOf("h1") + 3;
	var end = XMLHttpResponse.responseText.indexOf("</h1");
	var errorMsg = XMLHttpResponse.responseText.substring(start, end);
	return fromdb(errorMsg);	
}

function showLeftNavigationBasedOnRole() {
    if (isAdmin()) {				    
	    $("#leftmenu_domain, #leftmenu_account, #leftmenu_configuration, #leftmenu_system, #leftmenu_alert_container, #launch_test").show();					
	} 
	else if(isDomainAdmin()){				    
	    $("#leftmenu_domain, #leftmenu_account").show();					
	}
	else{  //isUser() == true					
	}
}
   
function drawBarChart($capacity, percentused) { //percentused == "0.01%" (having % inside)    
    $capacity.find("#percentused").text(percentused);
    
    var percentusedFloat; 
    if(percentused.indexOf("%") != -1) {
        percentused = percentused.replace("%", "");
        percentusedFloat = parseFloat(percentused);
        percentusedFloat = percentusedFloat * 0.01;   //because % is removed.  percentusedFloat == 0.0001
    }
    else {
        percentusedFloat = parseFloat(percentused);
    }
      
    if (percentusedFloat <= 0.6)
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox low").css("width", percentused); 
    else if (percentusedFloat > 0.6 && percentusedFloat <= 0.8 )
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox mid").css("width", percentused);
    else if (percentusedFloat > 0.8 )
        $capacity.find("#bar_chart").removeClass().addClass("db_barbox high").css("width", percentused);
}   
 
var midmenuItemFirstRowMaxLength = 30;  
function getMidmenuItemFirstRow(text) { 
    var text2 = fromdb(text);    
    var text3;
    if(text2.length > midmenuItemFirstRowMaxLength)
        text3 = fromdb(text2).substring(0, (midmenuItemFirstRowMaxLength-4)) + "....";
    else
        text3 = fromdb(text2).substring(0, midmenuItemFirstRowMaxLength);
    return text3;
}
















                                  
                           










var g_mySession = null;
var g_sessionKey = null;
var g_role = null; // roles - root, domain-admin, ro-admin, user
var g_username = null;
var g_account = null;
var g_domainid = null;
var g_enableLogging = false; 
var g_timezoneoffset = null;
var g_timezone = null;

// capabilities
var g_hypervisorType = "kvm";
function getHypervisorType() { return g_hypervisorType; }

var g_directAttachNetworkGroupsEnabled = "false";
function getDirectAttachNetworkGroupsEnabled() { return g_directAttachNetworkGroupsEnabled; }

var g_directAttachedUntaggedEnabled = "false";
function getDirectAttachUntaggedEnabled() { return g_directAttachedUntaggedEnabled; }

var g_systemVmUseLocalStorage = "false";
function getSystemVmUseLocalStorage() { return g_systemVmUseLocalStorage; }

//keyboard keycode
var keycode_Enter = 13;

//XMLHttpResponse.status
var ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED = 401;
var ERROR_INTERNET_NAME_NOT_RESOLVED = 12007;
var ERROR_INTERNET_CANNOT_CONNECT = 12029;
var ERROR_VMOPS_ACCOUNT_ERROR = 531;

var g_logger = new Logger();
$(function() {
	if(g_enableLogging)
		g_logger.open();
});

// Test Tool.  Please comment this out or remove this when going production.
// This is intended to provide a simple test tool to create user accounts and
// domains.
function initializeTestTool() {
	$("#launch_test").click(function(event) {
		testWindow = window.open('/client/test');
                testWindow.g_sessionKey=g_sessionKey;
		return false;
	});
}

// Role Functions
function isAdmin() {
	return (g_role == 1);
}

function isDomainAdmin() {
	return (g_role == 2);
}

function isUser() {
	return (g_role == 0);
}

function setDateField(dateValue, dateField, htmlMarkup) {
    if (dateValue != null && dateValue.length > 0) {
	    var disconnected = new Date();
	    disconnected.setISO8601(dateValue);	
	    var showDate;			
	    if(g_timezoneoffset != null) 
	        showDate = disconnected.getTimePlusTimezoneOffset(g_timezoneoffset);
	    else 
	        showDate = disconnected.format("m/d/Y H:i:s");
	    if(htmlMarkup == null)
	        dateField.text(showDate);
	    else
	        dateField.html(htmlMarkup + showDate);
    }
}

function initResizable(resizeElement, alsoResizeElement) {         
    var alsoResizeUi_originalHeight;
    $("#"+resizeElement).resizable({
        handles: 'e, w',
        autoHide: true,            
        //containment: ".grid_header"  , 
        alsoResize: "."+alsoResizeElement
    }); 
}
    
var sortBy = "";
var parseFunction = function() {}
var sortingOrder = "asc";

function sortArrayAlphabetically(a, b) {      
    if(a[sortBy] == null || b[sortBy] == null)
        return 0;
      
    var A = a[sortBy].toLowerCase();
    var B = b[sortBy].toLowerCase();
    
    if(sortingOrder == "asc") {
        if (A < B) 
            return -1;
        if (A > B) 
            return 1;
    } else {
        if (A < B) 
            return 1;
        if (A > B) 
            return -1;
    }
    return 0;
}	    

function sortArrayAlphabeticallyParse(a, b) {     
    if(a[sortBy] == null || b[sortBy] == null)
        return 0;
             
    var A = parseFunction(a[sortBy]).toLowerCase();
    var B = parseFunction(b[sortBy]).toLowerCase();
    
    if(sortingOrder == "asc") {
        if (A < B) 
            return -1;
        if (A > B) 
            return 1;
    } else {
        if (A < B) 
            return 1;
        if (A > B) 
            return -1;
    }
    return 0;
}	 

function sortArrayNumerically(a, b) {      
    if(a[sortBy] == null || b[sortBy] == null)
        return 0;
      
    var A = parseInt(a[sortBy]);
    var B = parseInt(b[sortBy]);
    
    if(sortingOrder == "asc") {
        if (A < B) 
            return -1;
        if (A > B) 
            return 1;
    } else {
        if (A < B) 
            return 1;
        if (A > B) 
            return -1;
    }
    return 0;
}

function sortArrayNumericallyParse(a, b) {     
    if(a[sortBy] == null || b[sortBy] == null)
        return 0;
     
    var A = parseFunction(parseInt(a[sortBy]));
    var B = parseFunction(parseInt(b[sortBy]));
        
    if(sortingOrder == "asc") {
        if (A < B) 
            return -1;
        if (A > B) 
            return 1;
    } else {
        if (A < B) 
            return 1;
        if (A > B) 
            return -1;
    }
    return 0;
}   

function sortArrayByDate(a, b) {    
    if(a[sortBy] == null || b[sortBy] == null)
        return 0;
        
    var A = convertMilliseconds(a[sortBy]);
    var B = convertMilliseconds(b[sortBy]);
    
    if(sortingOrder == "asc") {
        if (A < B) 
            return -1;
        if (A > B) 
            return 1;
    } else {
        if (A < B) 
            return 1;
        if (A > B) 
            return -1;
    }
    return 0;
}	

function convertMilliseconds(string) {
    if (string != null && string.length > 0) {
	    var date1 = new Date();
	    date1.setISO8601(string);	
	    return date1.getTime();	    
    } else {
        return null;
    }
}    

// Validation functions
function showError(isValid, field, errMsgField, errMsg) {    
	if(isValid) {
	    errMsgField.text("").hide();
	    field.addClass("text").removeClass("error_text");
	}
	else {
	    errMsgField.text(errMsg).show();
	    field.removeClass("text").addClass("error_text");	
	}
}

function showError2(isValid, field, errMsgField, errMsg, appendErrMsg) {    
	if(isValid) {
	    errMsgField.text("").hide();
	    field.addClass("text2").removeClass("error_text2");
	}
	else {
	    if(appendErrMsg) //append text
	        errMsgField.text(errMsgField.text()+errMsg).show();  
	    else  //reset text
	        errMsgField.text(errMsg).show();  
	    field.removeClass("text2").addClass("error_text2");	
	}
}

function showErrorInDropdown(isValid, field, errMsgField, errMsg, appendErrMsg) {    
	if(isValid) {
	    errMsgField.text("").hide();
	    field.addClass("select").removeClass("error_select");
	}
	else {
	    if(appendErrMsg) //append text
	        errMsgField.text(errMsgField.text()+errMsg).show();  
	    else  //reset text
	        errMsgField.text(errMsg).show();  
	    field.removeClass("select").addClass("error_select");	
	}
}

function validateDropDownBox(label, field, errMsgField, appendErrMsg) {  
    var isValid = true;
    var errMsg = "";   
    var value = field.val();     
	if (value == null || value.length == 0) {	   
	    errMsg = label + " is a required value. ";	   
		isValid = false;		
	} 		
	showErrorInDropdown(isValid, field, errMsgField, errMsg, appendErrMsg);	
	return isValid;
}

function validateNumber(label, field, errMsgField, min, max, isOptional) {
    var isValid = true;
    var errMsg = "";
    var value = field.val();       
	if (value != null && value.length != 0) {
		if(isNaN(value)) {
			errMsg = label + " must be a number";
			isValid = false;
		} else {
			if (min != null && value < min) {
				errMsg = label + " must be a value greater than or equal to " + min;
				isValid = false;
			}
			if (max != null && value > max) {
				errMsg = label + " must be a value less than or equal to " + max;
				isValid = false;
			}
		}
	} else if(isOptional!=true){  //required field
		errMsg = label + " is a required value. ";
		isValid = false;
	}
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateString(label, field, errMsgField, isOptional, maxLength) {  
    var isValid = true;
    var errMsg = "";
    var value = field.val();     
	if (isOptional!=true && (value == null || value.length == 0)) {	 //required field   
	    errMsg = label + " is a required value. ";	   
		isValid = false;		
	} 	
	else if (value!=null && value.length >= maxLength) {	    
	    errMsg = label + " must be less than " + maxLength + " characters";	   
		isValid = false;		
	} 	
	else if(value!=null && value.indexOf('"')!=-1) {
	    errMsg = "Double quotes are not allowed.";	   
		isValid = false;	
	}
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateIp(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;
    var isValid = true;
    var errMsg = "";
    var value = field.val();     		    
    if(value!=null && value.length>0) {
        myregexp = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;	   
        var isMatch = myregexp.test(value);
        if(!isMatch) {
            errMsg = label + " should be like 75.52.126.11";	   
	        isValid = false;		
	    }
	}	 	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateCIDR(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;        
    var isValid = true;
    var errMsg = "";
    var value = field.val();     
    if(value!=null && value.length>0) {
        myregexp = /^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/;	   
        var isMatch = myregexp.test(value);
        if(!isMatch) {
            errMsg = label + " should be like 10.1.1.0/24";	   
	        isValid = false;		
	    }
	}	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validateFilename(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;        
    var isValid = true;
    var errMsg = "";
    var value = field.val();     
    if(value!=null && value.length>0) {
        myregexp = /[^a-zA-Z0-9_\-\.]/;	   
        var isMatch = myregexp.test(value);
        if(isMatch) {
            errMsg = "Only alphanumeric, dot, dashes and underscore characters allowed";	   
	        isValid = false;		
	    }
	}	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function validatePath(label, field, errMsgField, isOptional) {  
    if(validateString(label, field, errMsgField, isOptional) == false)
        return;
    var isValid = true;
    var errMsg = "";
    var value = field.val();    
    if(value!=null && value.length>0) {
        myregexp = /^\//;	   
        var isMatch = myregexp.test(value);
        if(!isMatch) {
            errMsg = label + " should be like /aaa/bbb/ccc";	   
	        isValid = false;		
	    }
	}	 	
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function cleanErrMsg(field, errMsgField) {
    showError(true, field, errMsgField);
}	

// setter 
function setGridRowsTotal(field, gridRowsTotal) {   
    if(gridRowsTotal==null) {
        field.text("");
        return;
    }
 
    if(gridRowsTotal==1)
	    field.text(gridRowsTotal + " item");
	else
	    field.text(gridRowsTotal + " items");
} 

function changeGridRowsTotal(field, difference) {   
    var t = field.text();
    var oldTotal = 0;
    if(t.length>0 && t.indexOf(" item")!=-1) {      
        var s = t.substring(0, t.indexOf(" item"));
        if(!isNaN(s))
            oldTotal = parseInt(s);
    }
    var newTotal = oldTotal + difference;
    setGridRowsTotal(field, newTotal);
}


// others
function trim(val) {
    if(val == null)
        return null;
    return val.replace(/^\s*/, "").replace(/\s*$/, "");
}

function noNull(val) {
    if(val == null)
        return "";
    else
        return val;
}

// Prevent cross-site-script(XSS) attack. 
// used right before adding user input to the DOM tree. e.g. DOM_element.html(fromdb(user_input));  
function sanitizeXSS(val) {     
    if(val == null || typeof(val) != "string")
        return val; 
    val = val.replace(/</g, "&lt;");  //replace < whose unicode is \u003c     
    val = val.replace(/>/g, "&gt;");  //replace > whose unicode is \u003e  
    return val;
}

function getVmName(p_vmName, p_vmDisplayname) {
    if(p_vmDisplayname == null)
        return fromdb(p_vmName);
    var vmName = null;
	if (isAdmin()) {
		if (p_vmDisplayname != p_vmName) {
			vmName = p_vmName + "(" + fromdb(p_vmDisplayname) + ")";
		} else {
			vmName = p_vmName;
		}
	} else {
		vmName = fromdb(p_vmDisplayname);
	}
	return vmName;
}

// FUNCTION: Handles AJAX error callbacks.  You can pass in an optional function to 
// handle errors that are not already handled by this method.  
function handleError(XMLHttpResponse, handleErrorCallback) {
	// User Not authenticated
	if (XMLHttpResponse.status == ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED) {
		$("#dialog_session_expired").dialog("open");
	} 	
	else if (XMLHttpResponse.status == ERROR_INTERNET_NAME_NOT_RESOLVED) {
		$("#dialog_error_internet_not_resolved").dialog("open");
	} 
	else if (XMLHttpResponse.status == ERROR_INTERNET_CANNOT_CONNECT) {
		$("#dialog_error_management_server_not_accessible").dialog("open");
	} 
	else if (XMLHttpResponse.status == ERROR_VMOPS_ACCOUNT_ERROR && handleErrorCallback != undefined) {
		handleErrorCallback();
	} 
	else if (handleErrorCallback != undefined) {
		handleErrorCallback();
	}
	else {	  
		var errorMsg = parseXMLHttpResponse(XMLHttpResponse);				
		$("#dialog_error").text(fromdb(errorMsg)).dialog("open");
	}
}

// FUNCTION: Adds a Dialog to the list of active Dialogs so that
// when you shift from one tab to another, we clean out the dialogs
var activeDialogs = new Array();
function activateDialog(dialog, addToActive) {
	if (addToActive == undefined || addToActive) {
		activeDialogs[activeDialogs.length] = dialog;
	}
	
	//bind Enter-Key-pressing event handler to the dialog 	
	dialog.keypress(function(event) {
	    if(event.keyCode == keycode_Enter) {	        
	        $('[aria-labelledby$='+dialog.attr("id")+']').find(":button:first").click();	
	        return false; //event.preventDefault() + event.stopPropagation()
	    }    
	});
}
function removeDialogs() {
	for (var i = 0; i < activeDialogs.length; i++) {
		activeDialogs[i].remove();
	}
	activeDialogs = new Array();
}

function convertBytes(bytes) {
	if (bytes < 1024 * 1024) {
		return (bytes / 1024).toFixed(2) + " KB";
	} else if (bytes < 1024 * 1024 * 1024) {
		return (bytes / 1024 / 1024).toFixed(2) + " MB";
	} else if (bytes < 1024 * 1024 * 1024 * 1024) {
		return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
	} else {
		return (bytes / 1024 / 1024 / 1024 / 1024).toFixed(2) + " TB";
	}
}

function convertHz(hz) {
	if (hz < 1000) {
		return hz + " MHZ";
	} else {
		return (hz / 1000).toFixed(2) + " GHZ";
	} 
}

function toDayOfMonthDesp(dayOfMonth) {
    return "Day "+dayOfMonth +" of Month";
}

function toDayOfWeekDesp(dayOfWeek) {
    if (dayOfWeek == "1")
        return "Sunday";
    else if (dayOfWeek == "2")
        return "Monday";
    else if (dayOfWeek == "3")
        return "Tuesday";
    else if (dayOfWeek == "4")
        return "Wednesday";
    else if (dayOfWeek == "5")
        return "Thursday"
    else if (dayOfWeek == "6")
        return "Friday";
    else if (dayOfWeek == "7")
        return "Saturday";    
}

function toBooleanText(booleanValue) {
    if(booleanValue == true || booleanValue == "true")
        return "Yes";
    else if(booleanValue == false ||booleanValue == "false")
        return "No";
}

function toBooleanValue(booleanText) {
    if(booleanText == "Yes")
        return "true";
    else if(booleanText == "No")
        return "false";
}

function toNetworkType(usevirtualnetwork) {
    if(usevirtualnetwork == true || usevirtualnetwork == "true")
        return "Public";
    else
        return "Direct";
}

var roleTypeUser = "0";
var roleTypeAdmin = "1";
var roleTypeDomainAdmin = "2";
function toRole(type) {
	if (type == roleTypeUser) {
		return "User";
	} else if (type == roleTypeAdmin) {
		return "Admin";
	} else if (type == roleTypeDomainAdmin) {
		return "Domain-Admin";
	}
}

function toAlertType(alertCode) {
	switch (alertCode) {
		case 0 : return "Capacity Threshold - Memory";
		case 1 : return "Capacity Threshold - CPU";
		case 2 : return "Capacity Threshold - Storage Used";
		case 3 : return "Capacity Threshold - Storage Allocated";
		case 4 : return "Capacity Threshold - Public IP";
		case 5 : return "Capacity Threshold - Private IP";
		case 6 : return "Monitoring - Host";
		case 7 : return "Monitoring - VM";
		case 8 : return "Monitoring - Domain Router";
		case 9 : return "Monitoring - Console Proxy";
		case 10 : return "Monitoring - Routing Host";
		case 11 : return "Monitoring - Storage";
		case 12 : return "Monitoring - Usage Server";
		case 13 : return "Monitoring - Management Server";
		case 14 : return "Migration - Domain Router";
		case 15 : return "Migration - Console Proxy";
		case 16 : return "Migration - User VM";
		case 17 : return "VLAN";
		case 18 : return "Monitoring - Secondary Storage VM";
	}
}

// Timezones
var timezones = new Object();
timezones['Etc/GMT+12']='[UTC-12:00] GMT-12:00';
timezones['Etc/GMT+11']='[UTC-11:00] GMT-11:00';
timezones['Pacific/Samoa']='[UTC-11:00] Samoa Standard Time';
timezones['Pacific/Honolulu']='[UTC-10:00] Hawaii Standard Time';
timezones['US/Alaska']='[UTC-09:00] Alaska Standard Time';
timezones['America/Los_Angeles']='[UTC-08:00] Pacific Standard Time';
timezones['Mexico/BajaNorte']='[UTC-08:00] Baja California';
timezones['US/Arizona']='[UTC-07:00] Arizona';
timezones['US/Mountain']='[UTC-07:00] Mountain Standard Time';
timezones['America/Chihuahua']='[UTC-07:00] Chihuahua, La Paz';
timezones['America/Chicago']='[UTC-06:00] Central Standard Time';
timezones['America/Costa_Rica']='[UTC-06:00] Central America';
timezones['America/Mexico_City']='[UTC-06:00] Mexico City, Monterrey';
timezones['Canada/Saskatchewan']='[UTC-06:00] Saskatchewan';
timezones['America/Bogota']='[UTC-05:00] Bogota, Lima';
timezones['America/New_York']='[UTC-05:00] Eastern Standard Time';
timezones['America/Caracas']='[UTC-04:00] Venezuela Time';
timezones['America/Asuncion']='[UTC-04:00] Paraguay Time';
timezones['America/Cuiaba']='[UTC-04:00] Amazon Time';
timezones['America/Halifax']='[UTC-04:00] Atlantic Standard Time';
timezones['America/La_Paz']='[UTC-04:00] Bolivia Time';
timezones['America/Santiago']='[UTC-04:00] Chile Time';
timezones['America/St_Johns']='[UTC-03:30] Newfoundland Standard Time';
timezones['America/Araguaina']='[UTC-03:00] Brasilia Time';
timezones['America/Argentina/Buenos_Aires']='[UTC-03:00] Argentine Time';
timezones['America/Cayenne']='[UTC-03:00] French Guiana Time';
timezones['America/Godthab']='[UTC-03:00] Greenland Time';
timezones['America/Montevideo']='[UTC-03:00] Uruguay Time]';
timezones['Etc/GMT+2']='[UTC-02:00] GMT-02:00';
timezones['Atlantic/Azores']='[UTC-01:00] Azores Time';
timezones['Atlantic/Cape_Verde']='[UTC-01:00] Cape Verde Time';
timezones['Africa/Casablanca']='[UTC] Casablanca';
timezones['Etc/UTC']='[UTC] Coordinated Universal Time';
timezones['Atlantic/Reykjavik']='[UTC] Reykjavik';
timezones['Europe/London']='[UTC] Western European Time';
timezones['CET']='[UTC+01:00] Central European Time';
timezones['Europe/Bucharest']='[UTC+02:00] Eastern European Time';
timezones['Africa/Johannesburg']='[UTC+02:00] South Africa Standard Time';
timezones['Asia/Beirut']='[UTC+02:00] Beirut';
timezones['Africa/Cairo']='[UTC+02:00] Cairo';
timezones['Asia/Jerusalem']='[UTC+02:00] Israel Standard Time';
timezones['Europe/Minsk']='[UTC+02:00] Minsk';
timezones['Europe/Moscow']='[UTC+03:00] Moscow Standard Time';
timezones['Africa/Nairobi']='[UTC+03:00] Eastern African Time';
timezones['Asia/Karachi']='[UTC+05:00] Pakistan Time';
timezones['Asia/Kolkata']='[UTC+05:30] India Standard Time';
timezones['Asia/Bangkok']='[UTC+05:30] Indochina Time';
timezones['Asia/Shanghai']='[UTC+08:00] China Standard Time';
timezones['Asia/Kuala_Lumpur']='[UTC+08:00] Malaysia Time';
timezones['Australia/Perth']='[UTC+08:00] Western Standard Time (Australia)';
timezones['Asia/Taipei']='[UTC+08:00] Taiwan';
timezones['Asia/Tokyo']='[UTC+09:00] Japan Standard Time';
timezones['Asia/Seoul']='[UTC+09:00] Korea Standard Time';
timezones['Australia/Adelaide']='[UTC+09:30] Central Standard Time (South Australia)';
timezones['Australia/Darwin']='[UTC+09:30] Central Standard Time (Northern Territory)';
timezones['Australia/Brisbane']='[UTC+10:00] Eastern Standard Time (Queensland)';
timezones['Australia/Canberra']='[UTC+10:00] Eastern Standard Time (New South Wales)';
timezones['Pacific/Guam']='[UTC+10:00] Chamorro Standard Time';
timezones['Pacific/Auckland']='[UTC+12:00] New Zealand Standard Time';
