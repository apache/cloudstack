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
import java.awt.BorderLayout;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.ArrayList;

import org.apache.log4j.*;

public class LogTextPanel extends JPanel {

  private JScrollBar scrollBar;
  private JTextPane textPane;
  private JCheckBox cbxTail;
  private StyledDocument doc;

  private Hashtable fontAttributes;

  private int eventBufferMaxSize = 10000;
  private ArrayList eventBuffer = new ArrayList(eventBufferMaxSize);
  private int eventViewIndex = 0;

  public LogTextPanel() {
    constructComponents();
    createDefaultFontAttributes();
  }

  private void constructComponents() {
      // setup the panel's additional components...
    this.setLayout(new BorderLayout());

    cbxTail = new JCheckBox();
    cbxTail.setSelected(true);
    cbxTail.setText("Tail log events");

    JPanel bottomPanel = new JPanel();
    bottomPanel.add(cbxTail, null);

    textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText("");
    doc = textPane.getStyledDocument();

    scrollBar = new JScrollBar(JScrollBar.VERTICAL);

    this.add(bottomPanel, BorderLayout.SOUTH);
    this.add(scrollBar, BorderLayout.EAST);
    this.add(textPane, BorderLayout.CENTER);
  }

  public
  void setTextBackground(Color color) {
    textPane.setBackground(color);
  }

  public
  void setTextBackground(String v) {
    textPane.setBackground(parseColor(v));
  }

  private void createDefaultFontAttributes() {
    Priority[] prio = Priority.getAllPossiblePriorities();

    fontAttributes = new Hashtable();
    for (int i=0; i<prio.length;i++) {
      MutableAttributeSet att = new SimpleAttributeSet();
      fontAttributes.put(prio[i], att);
      //StyleConstants.setFontSize(att,11);
    }

    setTextColor(Priority.FATAL, Color.red);
    setTextColor(Priority.ERROR, Color.magenta);
    setTextColor(Priority.WARN, Color.orange);
    setTextColor(Priority.INFO, Color.blue);
    setTextColor(Priority.DEBUG, Color.black);
  }

  private
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

  void setTextColor(Priority p, String v) {
    StyleConstants.setForeground(
          (MutableAttributeSet)fontAttributes.get(p),parseColor(v));
  }

  void setTextColor(Priority p, Color c) {
    StyleConstants.setForeground(
          (MutableAttributeSet)fontAttributes.get(p),c);
  }

  void setTextFontSize(int size) {
    Enumeration e = fontAttributes.elements();
    while (e.hasMoreElements()) {
      StyleConstants.setFontSize((MutableAttributeSet)e.nextElement(),size);
    }
    return;
  }

  void setTextFontName(String name) {
    Enumeration e = fontAttributes.elements();
    while (e.hasMoreElements()) {
      StyleConstants.setFontFamily((MutableAttributeSet)e.nextElement(),name);
    }
    return;
  }

  void setEventBufferSize(int bufferSize) {
    eventBufferMaxSize = bufferSize;
  }

  void newEvents(EventBufferElement[] evts) {

    if((eventBuffer.size() + evts.length) >= eventBufferMaxSize) {
      for(int i=0; i < evts.length; i++) {
        eventBuffer.remove(0);
      }
      eventViewIndex -= evts.length;
      if(eventViewIndex < 0)
        eventViewIndex = 0;
    }
    for(int i=0; i < evts.length; i++)
      eventBuffer.add(evts[i]);

    if((eventBuffer.size() > maxR) && cbxTail.isSelected()) {
      eventViewIndex = (eventBuffer.size() - maxR);
    }

    // only redraw if new line is visible...
    if((maxR < 0) || (eventBuffer.size() >= eventViewIndex && eventBuffer.size() <= (eventViewIndex + maxR)))
      drawText();
  }

  int maxR = -1;

  void drawText() {
    if(maxR < 0)
      maxR =  textPane.getHeight() / textPane.getFontMetrics(textPane.getFont()).getHeight();
    try {
      doc.remove(0, doc.getLength());
    } catch(Exception e) { e.printStackTrace(); }

    for(int i=eventViewIndex; (i < eventBuffer.size()) && (i < (eventViewIndex + maxR)); i++) {
      EventBufferElement evt = (EventBufferElement)eventBuffer.get(i);

      try {
        doc.insertString(doc.getLength(), evt.text, (MutableAttributeSet)fontAttributes.get(evt.prio));
      } catch(Exception e) { e.printStackTrace(); }
    }
  }


}