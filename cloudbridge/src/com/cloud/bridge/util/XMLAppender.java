package com.cloud.bridge.util;

/* @auth John Zucker
 * XMLAppender is a helper class to allow a well formed XML response type and allow it to be
 * written incrementally to an instance of (a suitable subclass of) java.io.OutputStream.
 * It has a specialized purpose in starting a document and allowing it to be added to (i.e. appended onto)
 * so as to
 * * commence with a correct XML 1.0 tag
 * * contain the correct xml namespace for the entered tags
 * * allow additional tags to be entered
 * * provide a check of sufficient well formedness when the document is completed
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
// import java.io.Writer;
import java.util.Date;
import java.util.Stack;

import com.cloud.bridge.service.exception.InternalErrorException;

public class XMLAppender {
    List<String> tags = new ArrayList<String>();
    private Stack<String> elementStack = new Stack<String>();

    // Standard XML prolog to add to the beginning of each XML document.
    public static final String 
              XMLPROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    // The writer to which the XML document created by this writer will be written.
    private OutputStreamWriter writer;

    // XML namespace attribute value to include in the root element. Obligatory for the root element
    // only in the case of well formed AWS responses.

    private final String xmlns;

    //private Stack<String> elementStack = new Stack<String>();
    private boolean rootElement = true;   // for first time to append


    // The OutputStream to append the XML to
    public XMLAppender(OutputStream outputStream) {
      try {
      this.xmlns = "";
      this.writer = new OutputStreamWriter(outputStream, "UTF-8") ;
      }
      catch (UnsupportedEncodingException e) {
        throw new RuntimeException(
            "Noncompliant Java runtime: UTF-8 character encoding not supported on this platform");
         }
      }
    
      public XMLAppender(OutputStream outputStream, String xmlns) {
        try {
    	         this.xmlns = xmlns;
    	         this.writer = new OutputStreamWriter(outputStream, "UTF-8") ;
    	         }
    	         catch (UnsupportedEncodingException e) {
    	           throw new RuntimeException(
    	               "Noncompliant Java runtime: UTF-8 character encoding not supported on this platform");
    	            }
     append(XMLPROLOG);
     }
     
     // Create  new XMLAppender, ready to write an XML document to the specified
     // writer.  The XML document will not specify an xmlns attribute.
     public XMLAppender(OutputStreamWriter w) {
         this(w, null);
     }

// A new XMLAppender will append to writer w with an XML namespace attribute in the case of the
// root element
public XMLAppender(OutputStreamWriter w, String xmlns) {
    this.writer = w;
    this.xmlns = xmlns;
    append(XMLPROLOG);
}

// Starting from the current position in the document append the element with an XML namespace
// attribute at the start of the tag iff the element is the root element
public XMLAppender startElement(String element) {
    append("<" + element);
    if (rootElement && xmlns != null) {
        append(" xmlns=\"" + xmlns + "\"");
        rootElement = false;
    }
    append(">");
    elementStack.push(element);
    return this;
}

// Close the last opened element at the current position in the XML document
public XMLAppender endElement() {
    String lastElement = elementStack.pop();
    append("</" + lastElement + ">");
    return this;
}

// Append the string str to the document at current position
public XMLAppender value(String str) {
    append(str);
    return this;
}

//Append the ISO8601 string representation of any Date object to the document at current position
public XMLAppender value(Date date) {
	DateFormat df = new ISO8601SimpleDateTimeFormat();
	try
	{
	append(df.parse(date.toString()).toString());  // relies on GregorianTimestamp or just optimized for it?
    // TODO -jz - Check and recheck this is reliable
	}
	catch (ParseException e)
	{ }
    return this;
}

// Append the string representation of any generic object to the document at current position
public XMLAppender value(Object obj) {
    append(obj.toString());
    return this;
}

private void append(String s) {
    try {
        writer.append(s);
    } catch (IOException e) {
        throw new InternalErrorException("Unable to write XML document", e);
    // TODO -jz- !!!!!
    // Recategorize all cloudbridge S3 exceptions as either input errors or internal/capability errors
    // Allow 501s in the case of internal errors only.  Otherwise 400 series.
    }
}
    // Append the specified string suitably escaped to the specified StringBuilder.
   // TODO - Remember this utility method or possibly remove
   private void appendEscapedString(String s, StringBuilder stringBuilder) {
       int pos;
       int start = 0;
       int len = s.length();
	for (pos = 0; pos < len; pos++) {
           char ch = s.charAt(pos);
           String escape;
           switch (ch) {
           case '\t':
               escape = "&#9;";
               break;
           case '\n':
               escape = "&#10;";
               break;
           case '\r':
               escape = "&#13;";
               break;
           case '&':
               escape = "&amp;";
               break;
           case '"':
               escape = "&quote;";
               break;
           case '<':
               escape = "&lt;";
               break;
           case '>':
               escape = "&gt;";
               break;
           default:
               escape = null;
               break;
           }     
           // Write all the characters up to the escaped character, then write the escaped char
           if (escape != null) {
               if (start < pos)
                   stringBuilder.append(s, start, pos);
               stringBuilder.append(escape);
               start = pos + 1;
           }
       }       
       // Write the remaining string
       if (start < pos) stringBuilder.append(s, start, pos);
   }

    public byte[] getBytes() {
        assert(tags.size() == 0);
        byte[] byteArray = this.toString().getBytes();
            return byteArray;
    }
    
}

