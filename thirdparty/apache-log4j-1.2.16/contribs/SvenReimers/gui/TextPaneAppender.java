/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j.gui;


import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

import org.apache.log4j.*;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.helpers.QuietWriter;
import org.apache.log4j.helpers.TracerPrintWriter;
import org.apache.log4j.helpers.OptionConverter;


/**
 * <b>Experimental</b> TextPaneAppender. <br>
 *
 *
 * Created: Sat Feb 26 18:50:27 2000 <br>
 *
 * @author Sven Reimers
 */

public class TextPaneAppender extends AppenderSkeleton {
    
  JTextPane textpane;
  StyledDocument doc;
  TracerPrintWriter tp;
  StringWriter sw;
  QuietWriter qw;
  Hashtable attributes;
  Hashtable icons;
  
  private String label;
  
  private boolean fancy;
    
  final String LABEL_OPTION = "Label";
  final String COLOR_OPTION_FATAL = "Color.Emerg";
  final String COLOR_OPTION_ERROR = "Color.Error";
  final String COLOR_OPTION_WARN = "Color.Warn";
  final String COLOR_OPTION_INFO = "Color.Info";
  final String COLOR_OPTION_DEBUG = "Color.Debug";
  final String COLOR_OPTION_BACKGROUND = "Color.Background";
  final String FANCY_OPTION = "Fancy";
  final String FONT_NAME_OPTION = "Font.Name";
  final String FONT_SIZE_OPTION = "Font.Size";
  
  public static Image loadIcon ( String path ) {
    Image img = null;
    try {
      URL url = ClassLoader.getSystemResource(path);
      img = (Image) (Toolkit.getDefaultToolkit()).getImage(url);
    } catch (Exception e) {
      System.out.println("Exception occured: " + e.getMessage() + 
			 " - " + e );   
    }	
    return (img);
  }
  
  public TextPaneAppender(Layout layout, String name) {
    this();
    this.layout = layout;
    this.name = name;
    setTextPane(new JTextPane());
    createAttributes();
    createIcons();
  }
    
  public TextPaneAppender() {
    super();
    setTextPane(new JTextPane());
    createAttributes();
    createIcons();
    this.label="";
    this.sw = new StringWriter();
    this.qw = new QuietWriter(sw, errorHandler);
    this.tp = new TracerPrintWriter(qw);
    this.fancy =true;
  }

  public
  void close() {
    
  }
  
  private void createAttributes() {	
    Priority prio[] = Priority.getAllPossiblePriorities();
    
    attributes = new Hashtable();
    for (int i=0; i<prio.length;i++) {
      MutableAttributeSet att = new SimpleAttributeSet();
      attributes.put(prio[i], att);
      StyleConstants.setFontSize(att,14);
    }
    StyleConstants.setForeground((MutableAttributeSet)attributes.get(Priority.ERROR),Color.red);
    StyleConstants.setForeground((MutableAttributeSet)attributes.get(Priority.WARN),Color.orange);
    StyleConstants.setForeground((MutableAttributeSet)attributes.get(Priority.INFO),Color.gray);
    StyleConstants.setForeground((MutableAttributeSet)attributes.get(Priority.DEBUG),Color.black);
  }

  private void createIcons() {
    Priority prio[] = Priority.getAllPossiblePriorities();
    
    icons = new Hashtable();
    for (int i=0; i<prio.length;i++) {
      if (prio[i].equals(Priority.FATAL))
	icons.put(prio[i],new ImageIcon(loadIcon("icons/RedFlag.gif")));
      if (prio[i].equals(Priority.ERROR))		
	icons.put(prio[i],new ImageIcon(loadIcon("icons/RedFlag.gif")));
      if (prio[i].equals(Priority.WARN))		
	icons.put(prio[i],new ImageIcon(loadIcon("icons/BlueFlag.gif")));
      if (prio[i].equals(Priority.INFO))		
	icons.put(prio[i],new ImageIcon(loadIcon("icons/GreenFlag.gif")));
      if (prio[i].equals(Priority.DEBUG))		
	icons.put(prio[i],new ImageIcon(loadIcon("icons/GreenFlag.gif")));
    }
  }

  public void append(LoggingEvent event) {
    String text = this.layout.format(event);
    String trace="";
    // Print Stacktrace
    // Quick Hack maybe there is a better/faster way?
    if (event.throwable!=null) {
      event.throwable.printStackTrace(tp);
      for (int i=0; i< sw.getBuffer().length(); i++) {
	if (sw.getBuffer().charAt(i)=='\t')
	  sw.getBuffer().replace(i,i+1,"        ");
      }
      trace = sw.toString();
      sw.getBuffer().delete(0,sw.getBuffer().length());
    }
    try {
      if (fancy) {
	textpane.setEditable(true);
	textpane.insertIcon((ImageIcon)icons.get(event.priority));
	textpane.setEditable(false);
      }
      doc.insertString(doc.getLength(),text+trace,
		       (MutableAttributeSet)attributes.get(event.priority));
	}	
    catch (BadLocationException badex) {
      System.err.println(badex);
    }	
    textpane.setCaretPosition(doc.getLength());
  }
  
  public
  JTextPane getTextPane() {
    return textpane;
  }
  
  private
  static
  Color parseColor (String v) {
    StringTokenizer st = new StringTokenizer(v,",");
    int val[] = {255,255,255,255};
    int i=0;
    while (st.hasMoreTokens()) {
      val[i]=Integer.parseInt(st.nextToken());
      i++;
    }
    return new Color(val[0],val[1],val[2],val[3]);
  }
  
  private
  static
  String colorToString(Color c) {
    // alpha component emitted only if not default (255)
    String res = ""+c.getRed()+","+c.getGreen()+","+c.getBlue();
    return c.getAlpha() >= 255 ? res : res + ","+c.getAlpha();
  }

  public
  void setLayout(Layout layout) {
    this.layout=layout;
  }
  
  public
  void setName(String name) {
    this.name = name;
  }
  
    
  public
  void setTextPane(JTextPane textpane) {
    this.textpane=textpane;
    textpane.setEditable(false);
    textpane.setBackground(Color.lightGray);
    this.doc=textpane.getStyledDocument();
  }
          
  private
  void setColor(Priority p, String v) {
    StyleConstants.setForeground(
		      (MutableAttributeSet)attributes.get(p),parseColor(v));	
  }
  
  private
  String getColor(Priority p) {
    Color c =  StyleConstants.getForeground(
		      (MutableAttributeSet)attributes.get(p));
    return c == null ? null : colorToString(c);
  }
  
  /////////////////////////////////////////////////////////////////////
  // option setters and getters
  
  public
  void setLabel(String label) {
    this.label = label;
  }
  public
  String getLabel() {
    return label;
  }
  
  public
  void setColorEmerg(String color) {
    setColor(Priority.FATAL, color);
  }
  public
  String getColorEmerg() {
    return getColor(Priority.FATAL);
  }
  
  public
  void setColorError(String color) {
    setColor(Priority.ERROR, color);
  }
  public
  String getColorError() {
    return getColor(Priority.ERROR);
  }
  
  public
  void setColorWarn(String color) {
    setColor(Priority.WARN, color);
  }
  public
  String getColorWarn() {
    return getColor(Priority.WARN);
  }
  
  public
  void setColorInfo(String color) {
    setColor(Priority.INFO, color);
  }
  public
  String getColorInfo() {
    return getColor(Priority.INFO);
  }
  
  public
  void setColorDebug(String color) {
    setColor(Priority.DEBUG, color);
  }
  public
  String getColorDebug() {
    return getColor(Priority.DEBUG);
  }
  
  public
  void setColorBackground(String color) {
    textpane.setBackground(parseColor(color));
  }
  public
  String getColorBackground() {
    return colorToString(textpane.getBackground());
  }
  
  public
  void setFancy(boolean fancy) {
    this.fancy = fancy;
  }
  public
  boolean getFancy() {
    return fancy;
  }
  
  public
  void setFontSize(int size) {
    Enumeration e = attributes.elements();
    while (e.hasMoreElements()) {
      StyleConstants.setFontSize((MutableAttributeSet)e.nextElement(),size);
    }
    return;
  }
  
  public
  int getFontSize() {
    AttributeSet attrSet = (AttributeSet) attributes.get(Priority.INFO);
    return StyleConstants.getFontSize(attrSet);
  }
  
  public
  void setFontName(String name) {
    Enumeration e = attributes.elements();
    while (e.hasMoreElements()) {
      StyleConstants.setFontFamily((MutableAttributeSet)e.nextElement(),name);
    }
    return;
  }
  
  public
  String getFontName() {
    AttributeSet attrSet = (AttributeSet) attributes.get(Priority.INFO);
    return StyleConstants.getFontFamily(attrSet);
  }

  public
  boolean requiresLayout() {
    return true;
  }
} // TextPaneAppender



