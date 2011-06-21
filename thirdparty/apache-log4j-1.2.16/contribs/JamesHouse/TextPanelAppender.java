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
import java.util.ArrayList;

import javax.swing.JPanel;

import org.apache.log4j.*;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.Loader;
import org.apache.log4j.helpers.QuietWriter;
import org.apache.log4j.helpers.TracerPrintWriter;
import org.apache.log4j.helpers.OptionConverter;


/**
 *
 * @author James House
 */

public class TextPanelAppender extends AppenderSkeleton {

  TracerPrintWriter tp;
  StringWriter sw;
  QuietWriter qw;
  LogTextPanel logTextPanel;
  LogPublishingThread logPublisher;

  final String COLOR_OPTION_FATAL = "Color.Fatal";
  final String COLOR_OPTION_ERROR = "Color.Error";
  final String COLOR_OPTION_WARN = "Color.Warn";
  final String COLOR_OPTION_INFO = "Color.Info";
  final String COLOR_OPTION_DEBUG = "Color.Debug";
  final String COLOR_OPTION_BACKGROUND = "Color.Background";
  final String FONT_NAME_OPTION = "Font.Name";
  final String FONT_SIZE_OPTION = "Font.Size";
  final String EVENT_BUFFER_SIZE_OPTION = "EventBuffer.Size";

  public TextPanelAppender(Layout layout, String name) {
    this.layout = layout;
    this.name = name;
    this.sw = new StringWriter();
    this.qw = new QuietWriter(sw, errorHandler);
    this.tp = new TracerPrintWriter(qw);
    setLogTextPanel(new LogTextPanel());
    logPublisher = new LogPublishingThread(logTextPanel, Priority.ERROR, 500);
    //logPublisher = new LogPublishingThread(logTextPanel, null, 500);
  }

  public
  void close() {
  }

  public void append(LoggingEvent event) {

    String text = this.layout.format(event);

    // Print Stacktrace
    // Quick Hack maybe there is a better/faster way?
    if (event.throwable!=null) {
      event.throwable.printStackTrace(tp);
      for (int i=0; i< sw.getBuffer().length(); i++) {
        if (sw.getBuffer().charAt(i)=='\t')
          sw.getBuffer().replace(i,i+1,"        ");
      }
      text += sw.toString();
      sw.getBuffer().delete(0,sw.getBuffer().length());
    }
    else
      if(!text.endsWith("\n"))
        text += "\n";

    logPublisher.publishEvent(event.priority, text);
  }

  public
  JPanel getLogTextPanel() {
    return logTextPanel;
  }

  public
  String[] getOptionStrings() {
    return new String[] { COLOR_OPTION_FATAL, COLOR_OPTION_ERROR,
         COLOR_OPTION_WARN, COLOR_OPTION_INFO, COLOR_OPTION_DEBUG,
         COLOR_OPTION_BACKGROUND, FONT_NAME_OPTION, FONT_SIZE_OPTION};
  }


  public
  void setName(String name) {
    this.name = name;
  }

  protected
  void setLogTextPanel(LogTextPanel logTextPanel) {
    this.logTextPanel = logTextPanel;
    logTextPanel.setTextBackground(Color.white);
  }

  public
  void setOption(String option, String value) {
    if (option.equalsIgnoreCase(COLOR_OPTION_FATAL))
      logTextPanel.setTextColor(Priority.FATAL,value);
    if (option.equalsIgnoreCase(COLOR_OPTION_ERROR))
      logTextPanel.setTextColor(Priority.ERROR,value);
    if (option.equalsIgnoreCase(COLOR_OPTION_WARN))
      logTextPanel.setTextColor(Priority.WARN,value);
    if (option.equalsIgnoreCase(COLOR_OPTION_INFO))
      logTextPanel.setTextColor(Priority.INFO,value);
    if (option.equalsIgnoreCase(COLOR_OPTION_DEBUG))
      logTextPanel.setTextColor(Priority.DEBUG,value);
    if (option.equalsIgnoreCase(COLOR_OPTION_BACKGROUND))
      logTextPanel.setTextBackground(value);
    if (option.equalsIgnoreCase(FONT_SIZE_OPTION))
      logTextPanel.setTextFontSize(Integer.parseInt(value));
    if (option.equalsIgnoreCase(FONT_NAME_OPTION))
      logTextPanel.setTextFontName(value);
    if (option.equalsIgnoreCase(EVENT_BUFFER_SIZE_OPTION))
      logTextPanel.setEventBufferSize(Integer.parseInt(value));
    return;
  }

  public
  boolean requiresLayout() {
    return true;
  }



  class LogPublishingThread extends Thread {

    LogTextPanel logTextPanel;
    ArrayList evts;
    Priority triggerPrio;
    long pubInterval;

    public LogPublishingThread(LogTextPanel logTextPanel, Priority triggerPrio, long pubInterval) {
      this.logTextPanel = logTextPanel;
      this.evts = new ArrayList(1000);
      this.triggerPrio = triggerPrio;
      this.pubInterval = pubInterval;
      //this.setPriority(Thread.NORM_PRIORITY - 1);
      this.start();
    }

    public void run() {
      while(true) {
        synchronized(evts) {
          try {
            evts.wait(pubInterval);
          }
          catch(InterruptedException e) {}

          logTextPanel.newEvents((EventBufferElement[])evts.toArray(new EventBufferElement[evts.size()]));

          evts.clear();
        }
      }

    }

    public void publishEvent(Priority prio, String text) {
      synchronized(evts) {
        evts.add(new EventBufferElement(prio, text));
        if(triggerPrio != null && prio.isGreaterOrEqual(triggerPrio))
          evts.notify();
      }
    }
  }

} // TextPaneAppender

class EventBufferElement {

  public String text;
  public Priority prio;
  public int numLines;

  EventBufferElement(Priority prio, String text) {
    this.prio = prio;
    this.text = text;
    numLines = 1;
    int pos = pos = text.indexOf('\n', 0);
    int len = text.length() - 1;

    while( (pos > 0) && (pos < len) )
      numLines++;
      pos = text.indexOf('\n', pos + 1);
  }
}


