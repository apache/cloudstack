<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">
<xsl:output method="html" doctype-public="-//W3C//DTD HTML 1.0 Transitional//EN"/>
<xsl:template match="/">
<html xmlns="http://www.w3.org/1999/xhtml"><head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link rel= "stylesheet" href="../includes/main.css" type="text/css" />
<link rel="shortcut icon" href="../favicon.ico" type="image/x-icon" />

<title>CloudStack API Reference</title>
</head>

<body>
<div id="insidetopbg">
<div id="inside_wrapper">
	<div class="uppermenu_panel">
            <div class="uppermenu_box">       
</div>
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
                    	<div class="api_titlebox">
                        	<div class="api_titlebox_left">
				<xsl:for-each select="command/command">
                           		<!-- Modify this line for the release version -->
                           		<span>
									CloudStack 2.2.14 Root Admin API Reference
								</span>
								<p></p>
                                <h1><xsl:value-of select="name"/></h1>
                                <p><xsl:value-of select="description"/></p>
				</xsl:for-each>
                            </div>

                            
                            <div class="api_titlebox_right">
                            	<a class="api_backbutton" href="../TOC_Root_Admin.html"></a>
                            </div>
                        </div>
                    	<div class="api_tablepanel">     
                         	<h2>Request parameters</h2>
                        	<table class="apitable">
                            	<tr class="hed">
                                	<td style="width:200px;"><strong>Parameter Name</strong></td>

                                    <td style="width:500px;">Description</td>
                                    <td style="width:180px;">Required</td>
                                </tr>
				<xsl:for-each select="command/command/request/arg">
                                <tr>
				    <xsl:if test="required='true'">
                                    <td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
				    <td style="width:500px;"><strong><xsl:value-of select="description"/></strong></td>
                                    <td style="width:180px;"><strong><xsl:value-of select="required"/></strong></td>
				    </xsl:if>
				    <xsl:if test="required='false'">
                			<td style="width:200px;"><i><xsl:value-of select="name"/></i></td>
                                    <td style="width:500px;"><i><xsl:value-of select="description"/></i></td>
                                    <td style="width:180px;"><i><xsl:value-of select="required"/></i></td>
				    </xsl:if>
                                </tr>
				</xsl:for-each>
                            </table>
                        </div>
                         
                         
                         <div class="api_tablepanel">     
                         	<h2>Response Tags</h2>
                        	<table class="apitable">
                            	<tr class="hed">
                                	<td style="width:200px;"><strong>Response Name</strong></td>
                                    <td style="width:500px;">Description</td>
                                </tr>
                                
				<xsl:for-each select="command/command/response/arg">
                                <tr>
					<td style="width:200px;"><strong><xsl:value-of select="name"/></strong></td>
                                    <td style="width:500px;"><xsl:value-of select="description"/></td>
					<xsl:for-each select="./arguments/arg">
					<tr>
					<td style="width:180px; padding-left:25px;"><strong><xsl:value-of select="name"/></strong></td>
                                    	<td style="width:500px;"><xsl:value-of select="description"/></td>
					</tr>
						<xsl:for-each select="./arguments/arg">
						<tr>
						<td style="width:165px; padding-left:40px;"><xsl:value-of select="name"/></td>
		                            	<td style="width:500px;"><xsl:value-of select="description"/></td>
						</tr>
					</xsl:for-each>					
					</xsl:for-each>					
                                </tr>
				</xsl:for-each>
                                
                         
                                
                              
                            
                            </table>

                        </div>
                        
                        
                </div> 
                </div>
                  
     
            </div>
        </div><!-- #BeginLibraryItem "/libraries/footer.lbi" -->
<div id="footer">

        	<div id="footer_mainmaster">
            	
                <p>Copyright 2011, 2012 Citrix Systems, Inc. All rights reserved </p>
            </div>
        </div>
        <script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
try {
var pageTracker = _gat._getTracker("UA-16163918-1");
pageTracker._setDomainName(".cloud.com");
pageTracker._trackPageview();
} catch(err) {}</script>

<script type="text/javascript" language="javascript">llactid=14481</script>
				<script type="text/javascript" language="javascript" src="http://t5.trackalyzer.com/trackalyze.js"></script><div class="clear"></div>      
  </div>
 </div>
</body>
</html>
</xsl:template>
</xsl:stylesheet>

