The Jaxen libraries included here are based on Jaxen 1.0-FCS.  Jaxen 1.0-FCS
was written against JDOM Beta 9, and for Jaxen to support JDOM Beta 10+, we
had to make the following change to org.jaxen.jdom.DocumentNavigator.  The
jaxen-jdom.jar provided here includes this change, and it's the only
difference between the JARs here and the Jaxen 1.0-FCS JARs.


--- DocumentNavigator.java.orig Tue Aug 31 16:23:32 2004
+++ DocumentNavigator.java      Fri Sep 03 00:08:37 2004
@@ -72,6 +72,8 @@

 import org.jdom.Document;
 import org.jdom.Element;
+import org.jdom.Parent;
+import org.jdom.Content;
 import org.jdom.Comment;
 import org.jdom.Text;
 import org.jdom.Attribute;
@@ -231,7 +233,7 @@
                     nsMap.put( ns.getPrefix(), new XPathNamespace(elem, ns) );
             }

-            current = current.getParent();
+            current = current.getParentElement();
         }

         nsMap.put( "xml", new XPathNamespace(elem, Namespace.XML_NAMESPACE) );
@@ -247,17 +249,9 @@
         {
             parent = contextNode;
         }
-        else if ( contextNode instanceof Element )
+        else if ( contextNode instanceof Content )
         {
-            parent = ((Element)contextNode).getParent();
-
-            if ( parent == null )
-            {
-                if ( ((Element)contextNode).isRootElement() )
-                {
-                    parent = ((Element)contextNode).getDocument();
-                }
-            }
+            parent = ((Content)contextNode).getParent();
         }
         else if ( contextNode instanceof Attribute )
         {
@@ -267,18 +261,6 @@
         {
             parent = ((XPathNamespace)contextNode).getJDOMElement();
         }
-        else if ( contextNode instanceof ProcessingInstruction )
-        {
-            parent = ((ProcessingInstruction)contextNode).getParent();
-        }
-        else if ( contextNode instanceof Comment )
-        {
-            parent = ((Comment)contextNode).getParent();
-        }
-        else if ( contextNode instanceof Text )
-        {
-            parent = ((Text)contextNode).getParent();
-        }

         if ( parent != null )
         {
@@ -456,9 +438,9 @@
         {
             element = (Element) context;
         }
-        else if ( context instanceof Text )
+        else if ( context instanceof Content )
         {
-            element = ((Text)context).getParent();
+            element = ((Content)context).getParentElement();
         }
         else if ( context instanceof Attribute )
         {
@@ -467,14 +449,6 @@
         else if ( context instanceof XPathNamespace )
         {
             element = ((XPathNamespace)context).getJDOMElement();
-        }
-        else if ( context instanceof Comment )
-        {
-            element = ((Comment)context).getParent();
-        }
-        else if ( context instanceof ProcessingInstruction )
-        {
-            element = ((ProcessingInstruction)context).getParent();
         }

         if ( element != null )
