// jdk1.4.1
import javax.xml.transform.*;
import java.io.*;

public class XmlToHtmlConverter extends XmlToHtmlConverterData {
	
	// To turn off generation of API docs for certain roles, comment out
	// the appropriate populateFor*() line(s) below.
	public static void main(String[] args) {
		
		XmlToHtmlConverter x = new XmlToHtmlConverter();
		x.populateForRootAdmin();
		x.populateForDomainAdmin();
		x.populateForUser();
		x.generateToc();
		x.generateIndividualCommandPages();
	}
	
	public void generateToc() {
		try {
			
		    TransformerFactory tFactory = TransformerFactory.newInstance();

		    // Generate the TOC for the API reference for User role
		    Transformer transformer =
		      tFactory.newTransformer
		         (new javax.xml.transform.stream.StreamSource
		            ("generatetocforuser.xsl"));
		
		    // The XML to be transformed must be at the location below.
		    // Modify this path to match your own setup.
		    transformer.transform
		      (new javax.xml.transform.stream.StreamSource
		            ("regular_user/regularUserSummary.xml"),
			// Modify this path to your own desired output location.
		       new javax.xml.transform.stream.StreamResult
		            ( new FileOutputStream("html/TOC_User.html")));
		    
		    // Generate the TOC for root administrator role
		    Transformer transformer1 =
		      tFactory.newTransformer
		         (new javax.xml.transform.stream.StreamSource
		            ("generatetocforadmin.xsl"));
		    
		    // The XML to be transformed must be at the location below.
		    // Modify this path to match your own setup.
		    transformer1.transform
		      (new javax.xml.transform.stream.StreamSource
		            ("root_admin/rootAdminSummary.xml"),
		    // Modify this path to your own desired output location.
		       new javax.xml.transform.stream.StreamResult
		            ( new FileOutputStream("html/TOC_Root_Admin.html")));
		    
		    // Generate the TOC for domain admin role
		    Transformer transformer2 =
		      tFactory.newTransformer
		         (new javax.xml.transform.stream.StreamSource
		            ("generatetocfordomainadmin.xsl"));
		    
		    // The XML to be transformed must be at the location below.
		    // Modify this path to match your own setup.	
		    transformer2.transform
		      (new javax.xml.transform.stream.StreamSource
		            ("domain_admin/domainAdminSummary.xml"),
		    // Modify this path to your own desired output location.
		       new javax.xml.transform.stream.StreamResult
		            ( new FileOutputStream("html/TOC_Domain_Admin.html")));
		    
		}
		catch (Exception e) {
		    e.printStackTrace( );
		}
	}
	
	// Create man pages
	public void generateIndividualCommandPages() {
		for(String commandName : rootAdminCommandNames) {
		
				try {
			
				    TransformerFactory tFactory = TransformerFactory.newInstance();
				    Transformer transformer =
				      tFactory.newTransformer
				         (new javax.xml.transform.stream.StreamSource
				            ("generateadmincommands.xsl"));
				
				    transformer.transform
				    // Modify this path to the location of the input files on your system.
				      (new javax.xml.transform.stream.StreamSource
				            ("root_admin/"+commandName+".xml"),
				    // Modify this path with the desired output location.
				       new javax.xml.transform.stream.StreamResult
				            ( new FileOutputStream("html/root_admin/"+commandName+".html")));
				} catch (Exception e) {
				    e.printStackTrace( );
				}
		}
		
		for(String commandName : domainAdminCommandNames) {
			
			try {
		
			    TransformerFactory tFactory = TransformerFactory.newInstance();
			    Transformer transformer =
			      tFactory.newTransformer
			         (new javax.xml.transform.stream.StreamSource
			            ("generatedomainadmincommands.xsl"));
			
			    transformer.transform
			    // Modify this path with the location of the input files on your system.
			      (new javax.xml.transform.stream.StreamSource
			            ("domain_admin/"+commandName+".xml"),
			    // Modify this path to the desired output location.
			       new javax.xml.transform.stream.StreamResult
			            ( new FileOutputStream("html/domain_admin/"+commandName+".html")));
			} catch (Exception e) {
			    e.printStackTrace( );
			}
		}
		
		for(String commandName : userCommandNames) {
			
			try {
		
			    TransformerFactory tFactory = TransformerFactory.newInstance();
			
			    Transformer transformer =
			      tFactory.newTransformer
			         (new javax.xml.transform.stream.StreamSource
			            ("generateusercommands.xsl"));
			
			    transformer.transform
			      (new javax.xml.transform.stream.StreamSource
			            ("regular_user/"+commandName+".xml"),
			       new javax.xml.transform.stream.StreamResult
			            ( new FileOutputStream("html/user/"+commandName+".html")));
			} catch (Exception e) {
			    e.printStackTrace( );
			}
		}
	}
}
