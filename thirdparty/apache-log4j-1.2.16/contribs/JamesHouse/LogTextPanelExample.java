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
package org.apache.log4j.gui.examples;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import org.apache.log4j.*;
import org.apache.log4j.gui.TextPanelAppender;

public class LogTextPanelExample {
  boolean packFrame = false;

  String catName = "dum.cat.name";

  public LogTextPanelExample() {

    // setup the logging
    TextPanelAppender tpa = new TextPanelAppender(new PatternLayout("%-5p %d [%t]:  %m%n"), "logTextPanel");
    tpa.setThreshold(Priority.DEBUG);
    Category cat = Category.getInstance(catName);
    cat.addAppender(tpa);

    LogFrame frame = new LogFrame(tpa);
    frame.validate();

    //Center the frame (window), and show it
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }

  /**Main method*/
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    LogTextPanelExample foo = new LogTextPanelExample();
    new LogTextPanelExampleGenThread(foo.catName);
  }
}

class LogFrame extends JFrame {

  public LogFrame(TextPanelAppender tpa) {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    JPanel contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(new BorderLayout());
    this.setSize(new Dimension(600, 400));
    this.setTitle("LogTextPanel Example");
    contentPane.add(tpa.getLogTextPanel(), BorderLayout.CENTER);
  }

  // exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }
}

class LogTextPanelExampleGenThread extends Thread {

  String catName;

  public LogTextPanelExampleGenThread(String catName) {
    this.catName = catName;
    this.setPriority(Thread.NORM_PRIORITY - 1);
    this.start();
  }

  public void run() {
    Category cat = Category.getInstance(catName);
    int cnt = 0;
    while(true) {
      cnt++;
      int randEvt = (int)(Math.random() * 125);
      if(randEvt < 3)
        cat.fatal("{" + cnt + "} Something screwed up bad.");
      else if(randEvt < 10)
        cat.error("{" + cnt + "} An error occured while trying to delete all of your files.");
      else if(randEvt < 25)
        cat.warn("{" + cnt + "} It seems as if your hard disk is getting full.");
      else if(randEvt < 55)
        cat.info("{" + cnt + "} It is now time for tea.");
      else if(randEvt < 65)
        cat.debug("{" + cnt + "} Something bad is happening on line 565 of com.foo.Crap");
      else if(randEvt < 75)
        cat.debug("{" + cnt + "} Input value for xe343dd is not equal to xe39dfd!");
      else if(randEvt < 85)
        cat.debug("{" + cnt + "} Successfully reached line 2312 of com.foo.Goo");
      else if(randEvt < 105)
        cat.debug("{" + cnt + "} Here is some extra handy debugging information for you.");
      else if(randEvt < 115)
        cat.debug("{" + cnt + "} The file you are about to write to is not open.");
      else if(randEvt < 125)
        cat.debug("{" + cnt + "} The input value to the method was <null>.");

      try {
        Thread.sleep(10);
      }
      catch(Exception e) {}

    }
  }
}