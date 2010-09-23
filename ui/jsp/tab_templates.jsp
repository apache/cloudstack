<%@ page import="java.util.Date" %>
<%
long milliseconds = new Date().getTime();
%>

<script type="text/javascript" src="scripts/cloud.core.templates.js?t=<%=milliseconds%>"></script>

<div class="submenu_links">		
    <div class="submenu_links_on" id="submenu_template">Template</div>
    <div class="submenu_links_off" id="submenu_iso">ISO</div>       
</div>


<!-- *** Template (begin) *** -->
<div class="maincontent" id="submenu_content_template" style="display:none;">
	<div id="maincontent_title">
    	<div class="maintitle_icon"> <img src="images/templatestitle_icons.gif" title="templates" /> </div>
		<h1>Templates</h1>
		<a class="add_newtemplatebutton" id="template_action_new" href="#" style="display:none"></a>
        
		<div class="search_formarea">
			<form action="#" method="post">
				<ol>
					<li><input class="text" type="text" name="search_input" id="search_input" /></li>
				</ol>
			</form>
			<a class="search_button" href="#" id="search_button"></a>
			
			<div id="advanced_search_link" class="advsearch_link" style="display: none;">Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>Advance Search</h3>
                    <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>                       
                    </ol>
                    </form>
                   <div class="adv_search_actionbox">
                    	<div class="adv_searchpopup_button" id="adv_search_button"></div>
					</div>
                </div>
            </div>
			
		</div>
	</div>
	
    <div class="filter_actionbox">
    	<div class="selection_formarea">
        	<form action="#" method="post">
            	<label for="filter">Filters:</label>
				<select class="select" id="template_type">
                  <option value="self">My Templates</option>
                  <option value="featured">Featured</option>
                  <option value="community">Community</option>
         		</select>
			</form>
         </div>
    </div>
		
	<div class="grid_container">
    	<div id="loading_gridtable" class="loading_gridtable">
                  <div class="loading_gridanimation"></div>
                   <p>Loading...</p>
             </div>
		<div class="grid_header">
			<div class="grid_genheader_cell" style="width:3%;">
				<div class="grid_headertitles">ID</div>
			</div>
            <div class="grid_genheader_cell" style="width:5%;">
				<div class="grid_headertitles">Zone</div>
			</div>
			<div class="grid_genheader_cell" style="width:10%;">
				<div class="grid_headertitles">Name</div>
			</div>
			<div class="grid_genheader_cell" style="width:9%;">
				<div class="grid_headertitles">Display Text</div>
			</div>
			<div class="grid_genheader_cell" style="width:7%;">
				<div class="grid_headertitles">Status</div>
			</div>
            <div class="grid_genheader_cell" style="width:20%;">
				<div class="grid_headertitles">Attributes</div>
			</div>
			<div class="grid_genheader_cell" style="width:7%;">
				<div class="grid_headertitles">OS Type</div>
			</div>
			<div class="grid_genheader_cell" style="width:6%;">
				<div class="grid_headertitles">Account</div>
			</div>
            <div class="grid_genheader_cell" style="width:8%;">
				<div class="grid_headertitles">Created</div>
			</div>
			<div class="grid_genheader_cell" style="width:8%;">
				<div class="grid_headertitles">Size</div>
			</div>
            <div class="grid_genheader_cell" style="width:15%;">
				<div class="grid_headertitles">Actions</div>
			</div>
		</div>
		<div id="grid_content">
        	
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display:none;">
    	<p id="grid_rows_total" />
    	<div class="pagination_actionbox">
        	<div class="pagination_actions">
            	<div class="pagination_actionicon"><img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#"> Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#"> Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#"> Next</a>
            </div>
        </div>
    </div>
</div>	

<!-- VM Template template -->
<div style="display:none;" id="vm_template_template">  
    <div class="adding_loading" style="height: 25px; display: none;" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">  
	    <div class="grid_smallgenrow_cell" style="width:3%;" >
		    <div class="netgrid_celltitles" id="template_id"></div>
	    </div>
        <div class="grid_smallgenrow_cell" style="width:5%;" >
		    <div class="netgrid_celltitles" id="template_zone"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;" >
		    <div class="netgrid_celltitles" id="template_name"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:9%;" >
		    <div class="netgrid_celltitles" id="template_display_text"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:7%;" >
		    <div class="netgrid_celltitles" id="template_status"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:20%;" >
		    <div class="netgrid_celltitles">
			    <div class="tempbiticons"><img src="images/password_selectedicon.gif" id="template_password"/></div>	
			    <div class="tempbiticons"><img src="images/public_selectedicon.gif"   id="template_public"/></div>		
                <div class="tempbiticons"><img src="images/featured_selectedicon.gif" id="template_featured"/></div>                				
		    </div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:7%;" >
		    <div class="netgrid_celltitles" id="template_ostype"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:6%;" >
		    <div class="netgrid_celltitles" id="template_account"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:8%;" >
		    <div class="netgrid_celltitles" id="template_created"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:8%;" >
		    <div class="netgrid_celltitles" id="template_size"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" id="template_crud" style="width:15%;">
		    <div class="netgrid_celltitles">
			    <span id="template_edit_container"><a href="#" id="template_edit"> Edit</a> | </span>
			    <span id="template_delete_container"><a href="#" id="template_delete">Delete</a> | </span>
                <span id="template_copy_container"><a href="#" id="template_copy">Copy</a> | </span>
                <span id="template_create_vm_container"><a href="#" id="template_create_vm">Create VM</a> </span>
		    </div>
	    </div>
	</div>
</div>

<!-- Edit Template Dialog -->
<div id="dialog_edit_template" title="Edit Template" style="display:none">
	<p>Please review your changes before clicking 'Change'</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label>Name:</label>
					<input class="text" type="text" id="edit_template_name" style="width:250px"/>
					<div id="edit_template_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
				</li>
				<li>
					<label>Display Text:</label>
					<input class="text" type="text" id="edit_template_display_text" style="width:250px"/>
					<div id="edit_template_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label>Password Enabled?:</label>
					<select class="select" id="edit_template_password">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>
				<li>
					<label>Public?:</label>
					<select class="select" id="edit_template_public">
						<option value="true">Yes</option>
						<option value="false">No</option>
					</select>
				</li>				
				<li id="edit_template_featured_container" style="display:none">
					<label>Featured?:</label>
					<select class="select" id="edit_template_featured">
						<option value="true">Yes</option>
						<option value="false">No</option>
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>

<!-- Add Template Dialog (begin) -->
<div id="dialog_add_template" title="Add Template" style="display:none">
	<p>Please enter the following data to create your new template</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="add_template_name" id="add_template_name" style="width:250px"/>
					<div id="add_template_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label for="user_name">Display Text:</label>
					<input class="text" type="text" name="add_template_display_text" id="add_template_display_text" style="width:250px"/>
					<div id="add_template_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label for="user_name">URL:</label>
					<input class="text" type="text" name="add_template_url" id="add_template_url" style="width:250px"/>
					<div id="add_template_url_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
                    <label>Zone:</label>
                    <select class="select" id="add_template_zone">
                    </select>
                </li>					
				<li>
					<label for="add_template_os_type">OS Type:</label>
					<select class="select" name="add_template_os_type" id="add_template_os_type">
					</select>
				</li>	
				<li>
					<label for="add_template_format">Format:</label>
					<select class="select" name="add_template_format" id="add_template_format">
					</select>
				</li>	
				<!--		
				<li>
					<label for="user_name">Require HVM?:</label>
					<select class="select" name="add_template_hvm" id="add_template_hvm">
						<option value="true">Yes</option>
						<option value="false">No</option>
					</select>
				</li>
				<li>
					<label for="user_name">OS Arch:</label>
					<select class="select" name="add_template_os" id="add_template_os">
						<option value="64">64 Bit</option>
						<option value="32">32 Bit</option>
					</select>
				</li>
				!-->
				<li>
					<label>Password Enabled?:</label>
					<select class="select" id="add_template_password">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>
				<li>
					<label>Public?:</label>
					<select class="select" id="add_template_public">
						<option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>				
				<li id="add_template_featured_container" style="display:none">
					<label>Featured?:</label>
					<select class="select" id="add_template_featured">
					    <option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Add Template Dialog (end) -->
<!-- Copy Template Dialog (begin) -->
<div id="dialog_copy_template" title="Copy Template" style="display:none">
	<p>Copy template <b id="copy_template_name_text">XXX</b> from zone <b id="copy_template_source_zone_text">XXX</b> to</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form3">
			<ol>				
				<li>
                    <label>Zone:</label>
                    <select class="select" id="copy_template_zone">  
                        <option value=""></option>                      
                    </select>
                </li>		
			</ol>
			<div id="copy_template_zone_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
		</form>
	</div>
</div>
<!--  Copy Template Dialog (end) -->
<!-- *** Template (end) *** -->






<!-- *** ISO (begin) *** -->
<div class="maincontent" id="submenu_content_iso" style="display:none;">
	<div id="maincontent_title">
    	<div class="maintitle_icon"> <img src="images/isotitle_icons.gif" title="templates" /> </div>
		<h1>ISO</h1>
		<a class="add_newisobutton" id="iso_action_new" href="#" style="display:none"></a>
        
		<div class="search_formarea">
			<form action="#" method="post">
				<ol>
					<li><input class="text" type="text" name="search_input" id="search_input" /></li>
				</ol>
			</form>
			<a class="search_button" id="search_button" href="#"></a>
			
			<div id="advanced_search_link" class="advsearch_link" style="display: none;">Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>Advance Search</h3>
                    <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>                       
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                    	<div class="adv_searchpopup_button" id="adv_search_button"></div>
					</div>
                </div>
            </div>
			
		</div>
	</div>
   	
   	<div class="filter_actionbox">
    	<div class="selection_formarea">
        	<form action="#" method="post">
            	<label for="filter">Filters:</label>
				<select class="select" id="iso_type">
                  <option value="self">My ISOs</option>                  
                  <option value="community">Community</option>
         		</select>
			</form>
         </div>
    </div>
   	
	<div class="grid_container">
    	<div id="loading_gridtable" class="loading_gridtable">
                  <div class="loading_gridanimation"></div>
                   <p>Loading...</p>
             </div>
		<div class="grid_header">
			<!--
			<div class="temp_gridheader_cell4">
				<div class="grid_headertitles">Type</div>
			</div>
			-->
            <div class="grid_genheader_cell" style="width:3%;">
				<div class="grid_headertitles">ID</div>
			</div>
             <div class="grid_genheader_cell" style="width:8%;">
				<div class="grid_headertitles">Zone</div>
			</div>
			<div class="grid_genheader_cell" style="width:10%;">
				<div class="grid_headertitles">Name</div>
			</div>
			<div class="grid_genheader_cell" style="width:10%;">
				<div class="grid_headertitles">Display Text</div>
			</div>			
			<div class="grid_genheader_cell" style="width:15%;">
				<div class="grid_headertitles">Status</div>
			</div>
			<!--
			<div class="grid_genheader_cell" style="width:10%;">
				<div class="grid_headertitles">Attributes</div>
			</div>
			-->
            <div class="grid_genheader_cell" style="width:5%;">
				<div class="grid_headertitles">Bootable</div>
			</div>  
			<div class="grid_genheader_cell" style="width:10%;">
				<div class="grid_headertitles">Account</div>
			</div>			
            <div class="grid_genheader_cell" style="width:8%;">
				<div class="grid_headertitles">Created</div>
			</div>
			<div class="grid_genheader_cell" style="width:8%;">
				<div class="grid_headertitles">Size</div>
			</div>
            <div class="grid_genheader_cell" style="width:20%;">
				<div class="grid_headertitles">Actions</div>
			</div>
		</div>
		<div id="grid_content">
        	
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display:none;">
    	<p id="grid_rows_total" />
    	<div class="pagination_actionbox">
        	<div class="pagination_actions">
            	<div class="pagination_actionicon"><img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#"> Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#"> Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#"> Next</a>
            </div>
        </div>
    </div>
</div>	

<!-- ISO template -->
<div style="display:none;" id="vm_iso_template">   
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">  
	    <!--
	    <div class="tempgrid_row_cell4">
		    <div class="netgrid_celltitles">
			    <div class="temptypeicons"><img src="images/temp_windowsicon.gif" alt="windows" /></div>
		    </div>
	    </div>
	    -->
	    <div class="grid_smallgenrow_cell" style="width:3%;">
		    <div class="netgrid_celltitles" id="iso_id"></div>
	    </div>
         <div class="grid_smallgenrow_cell" style="width:8%;">
		    <div class="netgrid_celltitles" id="iso_zone"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="iso_name"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="iso_display_text"></div>
	    </div>	
	    <div class="grid_smallgenrow_cell" style="width:15%;">
		    <div class="netgrid_celltitles" id="iso_status"></div>
	    </div>	
	    <!--
	    <div class="grid_smallgenrow_cell" style="width:10%;" >
		    <div class="netgrid_celltitles">			    
			    <div class="tempbiticons"><img src="images/public_selectedicon.gif" id="iso_public"/></div>		                
		    </div>
	    </div>
	    -->
	    <div class="grid_smallgenrow_cell" style="width:5%;">
		    <div class="netgrid_celltitles" id="iso_bootable"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="iso_account"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:8%;">
		    <div class="netgrid_celltitles" id="iso_created"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:8%;">
		    <div class="netgrid_celltitles" id="iso_size"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" id="iso_crud" style="width:20%;">
		    <div class="netgrid_celltitles">
			    <span id="iso_edit_container"><a href="#" id="iso_edit">Edit</a> | </span>
			    <span id="iso_delete_container"><a href="#" id="iso_delete">Delete</a> | </span>
			    <span id="iso_copy_container"><a href="#" id="iso_copy">Copy</a> | </span>
			    <span id="iso_create_vm_container"><a href="#" id="iso_create_vm">Create VM</a> </span>
		    </div>
	    </div>
	</div>
</div>

<!-- Edit ISO Dialog -->
<div id="dialog_edit_iso" title="Edit ISO" style="display:none">
	<p>Please review your changes before clicking 'Change'</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form1">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="edit_iso_name" id="edit_iso_name" style="width:250px"/>
					<div id="edit_iso_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label for="user_name">Display Text:</label>
					<input class="text" type="text" name="edit_iso_display_text" id="edit_iso_display_text" style="width:250px"/>
					<div id="edit_iso_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<!--				
				<li id="edit_iso_public_container" style="display:none">
					<label for="user_name">Public:</label>
					<select class="select" name="edit_iso_public" id="edit_iso_public">
						<option value="true">Yes</option>
						<option value="false">No</option>
					</select>
				</li>	
				-->			
			</ol>
		</form>
	</div>
</div>

<!-- Add ISO Dialog (begin) -->
<div id="dialog_add_iso" title="Add ISO" style="display:none">
	<p>Please enter the following data to create your new ISO</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form2">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="add_iso_name" id="add_iso_name" style="width:250px"/>
					<div id="add_iso_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label for="user_name">Display Text:</label>
					<input class="text" type="text" name="add_iso_display_text" id="add_iso_display_text" style="width:250px"/>
					<div id="add_iso_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label for="user_name">URL:</label>
					<input class="text" type="text" name="add_iso_url" id="add_iso_url" style="width:250px"/>
					<div id="add_iso_url_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label>Zone:</label>
                    <select class="select" id="add_iso_zone">
                    </select>
                </li>					
				<!--
				<li>
					<label>Public?:</label>
					<select class="select" id="add_iso_public">
						<option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>	
				-->					
				<li>
					<label for="add_iso_public">Bootable:</label>
					<select class="select" name="add_iso_bootable" id="add_iso_bootable">
						<option value="true">Yes</option>
						<option value="false">No</option>
					</select>
				</li>
				<li>
					<label for="add_iso_os_type">OS Type:</label>
					<select class="select" name="add_iso_os_type" id="add_iso_os_type">
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Add ISO Dialog (end) -->
<!-- Copy ISO Dialog (begin) -->
<div id="dialog_copy_iso" title="Copy ISO" style="display:none">
	<p>Copy ISO <b id="copy_iso_name_text">XXX</b> from zone <b id="copy_iso_source_zone_text">XXX</b> to</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form4">
			<ol>				
				<li>
                    <label>Zone:</label>
                    <select class="select" id="copy_iso_zone">  
                        <option value=""></option>                        
                    </select>
                </li>		
				<div id="copy_iso_zone_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
			</ol>
		</form>
	</div>
</div>
<!--  Copy ISO Dialog (end) -->
<!-- *** ISO (end) *** -->





<!-- Create VM from template (begin) -->
<div id="dialog_create_vm_from_template" title="Create VM from template" style="display:none">
	<p>Create VM from <b id="source_name">xxx</b></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>			   
				<li>
					<label>Name:</label>
					<input class="text" type="text" id="name"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label>Group:</label>
					<input class="text" type="text" id="group"/>
					<div id="group_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label>Service Offering:</label>
                    <select class="select" id="service_offering">
                    </select>
                </li>					
				<li>
                    <label>Disk Offering:</label>
                    <select class="select" id="disk_offering">
                    </select>
                </li>					
			</ol>
		</form>
	</div>
</div>
<!-- Create VM from template (end) -->

<!-- Create VM from ISO (begin) -->
<div id="dialog_create_vm_from_iso" title="Create VM from ISO" style="display:none">
	<p>Create VM from <b id="source_name">xxx</b></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>			   
				<li>
					<label>Name:</label>
					<input class="text" type="text" id="name"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label>Group:</label>
					<input class="text" type="text" id="group"/>
					<div id="group_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label>Service Offering:</label>
                    <select class="select" id="service_offering">
                    </select>
                </li>					
				<li>
                    <label>Disk Offering:</label>
                    <select class="select" id="disk_offering">
                    </select>
                </li>					
			</ol>
		</form>
	</div>
</div>
<!-- Create VM from ISO (end) -->
