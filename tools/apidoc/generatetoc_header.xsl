<?xml version="1.0"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">
<xsl:output method="html" doctype-public="-//W3C//DTD HTML 1.0 Transitional//EN"/>
<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml"><head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link rel= "stylesheet" href="includes/main.css" type="text/css" />
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />

<title>CloudStack API Reference</title>
</head>

<body>
<div id="insidetopbg">
<div id="inside_wrapper">
	<div class="uppermenu_panel">
            <div class="uppermenu_box"></div>
        </div>
        
        <div id="main_master">
            <div id="inside_header">
                <div class="header_top">
                    <a class="cloud_logo" href="http://cloudstack.org"></a>
                    <div class="mainemenu_panel">
                        
                    </div>
                </div>
                
            
            </div>
            <div id="main_content">
             	
                <div class="inside_apileftpanel">
                	<div class="inside_contentpanel" style="width:930px;">
              		  	<!-- Modify this line for the release version -->
                    	<h1>Apache CloudStack API Documentation (v4.6.0)</h1>
                      <a class="api_backbutton" href="http://cloudstack.apache.org/docs/api/"></a>
                        <div class="apiannouncement_box">
                        	<div class="apiannouncement_contentarea">
                                <h3>Using the CloudStack API</h3>
                                <p>For information about how the APIs work, and tips on how to use them, see the
                                    <a href="http://docs.cloudstack.apache.org/en/latest/dev.html">Developer's Guide</a>.</p>
       					 	</div>
       					 </div>
                         
                         <div class="api_leftsections">
                      			<h3>%API_HEADER%</h3>
                                <span>Commands available through the developer API URL and the integration API URL.</span>
                                <div class="api_legends">
           				<p><span class="api_legends_async">(A)</span> implies that the command is asynchronous.</p>
					<p>(*) implies element has a child.</p>
 				</div>
