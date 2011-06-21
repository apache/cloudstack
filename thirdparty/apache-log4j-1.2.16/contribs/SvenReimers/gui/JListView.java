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

import org.apache.log4j.helpers.CyclicBuffer;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.Priority;
import org.apache.log4j.Category;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import javax.swing.JList;
import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Container;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.awt.Rectangle;

public class JListView extends JList {


  static Category cat = Category.getInstance(JListView.class.getName());


  //JListViewModel model;
  PatternLayout layout;

  static LoggingEvent proto = new LoggingEvent("x", cat, Priority.ERROR, 
					       "Message ", new Throwable());

  public
  JListView(JListViewModel model) {
    super(model);
    layout = new PatternLayout("%r %p %c [%t] -  %m");
    //this.setModel(model);
    this.setCellRenderer(new MyCellRenderer());
    //    setFixedCellWidth(10);
    //setFixedCellHeight(20);

  }

  public
  void add(LoggingEvent event) {
    ((JListViewModel)getModel()).add(event);
  }

  /*
  public
  Dimension getPreferredSize() {
    System.out.println("getPreferredSize() called");
    return super.getPreferredSize();
  }


  public
  int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
				 int direction) {
    System.out.println("getScrollableUnitIncrement called with " + visibleRect +
		       "orientation: "+orientation+", direction: "+direction);
    return super.getScrollableUnitIncrement(visibleRect, orientation, 
    				    direction);
  }

  public
  int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
				  int direction) {
    System.out.println("getScrollableBlockIncrement called with " + 
		       visibleRect + "orientation: "+orientation+
		       ", direction: "+direction);
    return super.getScrollableBlockIncrement(visibleRect, orientation, 
    				     direction);
  }
  */

  //public
  //boolean getScrollableTracksViewportWidth() {
  //System.out.println("getScrollableTracksViewportWidth called.");
  //return true;
    //boolean b = super.getScrollableTracksViewportWidth();
    //System.out.println("result is: "+b);
    //return b;
  //}
  
  //public
  //boolean getScrollableTracksViewportHeight() { 
  // System.out.println("getScrollableTracksViewportHeight called.");
  // return true;
     //boolean b = super.getScrollableTracksViewportHeight();
     //System.out.println("result is: "+b);
     //return b;
  //}

  //public 
  //int getFirstVisibleIndex() {
  //int r = getFirstVisibleIndex(); 
  // System.out.println("----------getFirstVisibleIndex called, result: "+r);
  //return r;
  //}

  //public
  //Object getPrototypeCellValue() {
  //return proto;
  //}

  
  
  static public void main(String[] args) {

    JFrame frame = new JFrame("JListView test");
    Container container = frame.getContentPane();

    JListView view = new JListView(new JListViewModel(Integer.parseInt(args[0])));


    JScrollPane sp = new JScrollPane(view);
    sp.setPreferredSize(new Dimension(250, 80));
    
    container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
    //container.add(view);
    container.add(sp);

    JButton b1 = new JButton("Add 1");
    JButton b10 = new JButton("Add 10");
    JButton b100 = new JButton("Add 100");
    JButton b1000 = new JButton("Add 1000");
    JButton b10000 = new JButton("Add 10000");

    JPanel panel = new JPanel(new GridLayout(0,1));
    container.add(panel);

    panel.add(b1);
    panel.add(b10);
    panel.add(b100);
    panel.add(b1000);
    panel.add(b10000);
    

    AddAction a1 = new AddAction(view, 1);
    AddAction a10 = new AddAction(view, 10);
    AddAction a100 = new AddAction(view, 100);
    AddAction a1000 = new AddAction(view, 1000);
    AddAction a10000 = new AddAction(view, 10000);

    b1.addActionListener(a1);
    b10.addActionListener(a10);
    b100.addActionListener(a100);
    b1000.addActionListener(a1000);
    b10000.addActionListener(a10000);

    frame.setVisible(true);
    frame.setSize(new Dimension(700,700));

    long before = System.currentTimeMillis();

    int RUN = 1000;
    int i = 0;
    while(i++ < RUN) {      
      LoggingEvent event0 = new LoggingEvent("x", cat, Priority.ERROR, 
					     "Message "+i, null);
      
      Throwable t = new Exception("hello "+i);
      LoggingEvent event1 = new LoggingEvent("x", cat, Priority.ERROR, 
					     "Message "+i, t);
      

      if(i % 10 == 0) {	
	event1.getThreadName();
	view.add(event1);
      } else {
	event0.getThreadName();
	view.add(event0);
      }
    }

    long after = System.currentTimeMillis();
    System.out.println("Time taken :"+ ((after-before)*1000/RUN));

  }

  class MyCellRenderer extends JTextArea implements ListCellRenderer {

    Object o = new Object();
    int i = 0;
    final ImageIcon longIcon = new ImageIcon("RedFlag.gif");

    public
    MyCellRenderer() {
      System.out.println("----------------------");
      
    }



    public
    int getTabSize()  {
      return 2;
    }

    public Image loadIcon ( String path ) {
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

    public Component getListCellRendererComponent(JList list,
						Object value, 
						int index, // cell index
						boolean isSelected, 
						boolean cellHasFocus) {

      //      System.out.println(o + " ============== " + i++);
      //LogLog.error("=======", new Exception());
      //setIcon(longIcon);
      if(value instanceof LoggingEvent) {
	LoggingEvent event = (LoggingEvent) value;
	String str = layout.format(event);
	String t = event.getThrowableInformation();

	if(t != null) {
	  setText(str + Layout.LINE_SEP + t);
	} else {	
	  setText(str);
	}
	
      } else {
	setText(value.toString());
      }


      return this;
    }
  }
}



class JListViewModel extends AbstractListModel {

  CyclicBuffer cb;
  
  JListViewModel(int size) {
    cb = new CyclicBuffer(size);
  }

  public
  void add(LoggingEvent event) {
    //System.out.println("JListViewModel.add called");
    cb.add(event);
    int j = cb.length();
    fireContentsChanged(this, 0, j);
  }
    


  public
  Object getElementAt(int index) {
    return cb.get(index);
  }

  public
  int getSize() {
    return cb.length();
  }
  
}

class AddAction implements ActionListener {

  Thread t;

  static int counter = 0;

  public
  AddAction(JListView view, int burst) {
    this.t = new AddThread(view, burst);
    t.start();
  }
    
  public
  void actionPerformed(ActionEvent e) {
    System.out.println("Action occured");
    synchronized(t) {
      t.notify();
    }
  }

  class AddThread extends Thread {
    int burst;
    JListView view;

    Category cat = Category.getInstance("x");
    
    AddThread(JListView view, int burst) {
      super();
      this.burst = burst;
      this.view = view;
      setName("AddThread"+burst);
    }

    public
    void run() {

      while(true) {
	synchronized(this) {
	  try {
	    this.wait();
	  } catch(Exception e) {
	  }
	}
	for(int i = 0; i < burst; i++) {
	  LoggingEvent event = new LoggingEvent("x", cat, Priority.DEBUG, 
						"Message "+counter, null);

	  event.getThreadName();    
	  if(counter % 50 == 0) {
	    //event.throwable = new Exception("hello "+counter);
	  }
	  counter++;
	  view.add(event);
	}
      }
    }
  }
}
