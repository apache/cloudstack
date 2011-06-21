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
package org.apache.log4j.lf5.viewer.configure;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;


/**
 * <p>MRUFileManager handles the storage and retrival the most
 * recently opened log files.
 *
 * @author Brad Marlborough
 * @author Richard Hurst
 */

// Contributed by ThoughtWorks Inc.

public class MRUFileManager {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------
  private static final String CONFIG_FILE_NAME = "mru_file_manager";
  private static final int DEFAULT_MAX_SIZE = 3;

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private int _maxSize = 0;
  private LinkedList _mruFileList;

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public MRUFileManager() {
    load();
    setMaxSize(DEFAULT_MAX_SIZE);
  }

  public MRUFileManager(int maxSize) {
    load();
    setMaxSize(maxSize);
  }
  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  /**
   * Saves a list of MRU files out to a file.
   */
  public void save() {
    File file = new File(getFilename());

    try {
      ObjectOutputStream oos = new ObjectOutputStream(new
          FileOutputStream(file));
      oos.writeObject(_mruFileList);
      oos.flush();
      oos.close();
    } catch (Exception e) {
      // do nothing
      e.printStackTrace();
    }
  }

  /**
   * Gets the size of the MRU file list.
   */
  public int size() {
    return _mruFileList.size();
  }

  /**
   * Returns a particular file name stored in a MRU file
   * list based on an index value.
   */
  public Object getFile(int index) {
    if (index < size()) {
      return _mruFileList.get(index);
    }

    return null;
  }

  /**
   * Returns a input stream to the resource at the specified index
   */
  public InputStream getInputStream(int index) throws IOException,
      FileNotFoundException {
    if (index < size()) {
      Object o = getFile(index);
      if (o instanceof File) {
        return getInputStream((File) o);
      } else {
        return getInputStream((URL) o);
      }
    }
    return null;
  }

  /**
   * Adds a file name to the MRU file list.
   */
  public void set(File file) {
    setMRU(file);
  }

  /**
   * Adds a url to the MRU file list.
   */
  public void set(URL url) {
    setMRU(url);
  }

  /**
   * Gets the list of files stored in the MRU file list.
   */
  public String[] getMRUFileList() {
    if (size() == 0) {
      return null;
    }

    String[] ss = new String[size()];

    for (int i = 0; i < size(); i++) {
      Object o = getFile(i);
      if (o instanceof File) {
        ss[i] = ((File) o).getAbsolutePath();
      } else // must be a url
      {
        ss[i] = o.toString();
      }

    }

    return ss;
  }

  /**
   * Moves the the index to the top of the MRU List
   *
   * @param index The index to be first in the mru list
   */
  public void moveToTop(int index) {
    _mruFileList.add(0, _mruFileList.remove(index));
  }

  /**
   * Creates the directory where the MRU file list will be written.
   * The "lf5" directory is created in the Documents and Settings
   * directory on Windows 2000 machines and where ever the user.home
   * variable points on all other platforms.
   */
  public static void createConfigurationDirectory() {
    String home = System.getProperty("user.home");
    String sep = System.getProperty("file.separator");
    File f = new File(home + sep + "lf5");
    if (!f.exists()) {
      try {
        f.mkdir();
      } catch (SecurityException e) {
        e.printStackTrace();
      }
    }

  }
  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------
  /**
   * Gets an input stream for the corresponding file.
   *
   * @param file The file to create the input stream from.
   * @return InputStream
   */
  protected InputStream getInputStream(File file) throws IOException,
      FileNotFoundException {
    BufferedInputStream reader =
        new BufferedInputStream(new FileInputStream(file));

    return reader;
  }

  /**
   * Gets an input stream for the corresponding URL.
   *
   * @param url The url to create the input stream from.
   * @return InputStream
   */
  protected InputStream getInputStream(URL url) throws IOException {
    return url.openStream();
  }

  /**
   * Adds an object to the mru.
   */
  protected void setMRU(Object o) {
    int index = _mruFileList.indexOf(o);

    if (index == -1) {
      _mruFileList.add(0, o);
      setMaxSize(_maxSize);
    } else {
      moveToTop(index);
    }
  }

  /**
   * Loads the MRU file list in from a file and stores it in a LinkedList.
   * If no file exists, a new LinkedList is created.
   */
  protected void load() {
    createConfigurationDirectory();
    File file = new File(getFilename());
    if (file.exists()) {
      try {
        ObjectInputStream ois = new ObjectInputStream(
            new FileInputStream(file));
        _mruFileList = (LinkedList) ois.readObject();
        ois.close();

        // check that only files and url are in linked list
        Iterator it = _mruFileList.iterator();
        while (it.hasNext()) {
          Object o = it.next();
          if (!(o instanceof File) && !(o instanceof URL)) {
            it.remove();
          }
        }
      } catch (Exception e) {
        _mruFileList = new LinkedList();
      }
    } else {
      _mruFileList = new LinkedList();
    }

  }

  protected String getFilename() {
    String home = System.getProperty("user.home");
    String sep = System.getProperty("file.separator");

    return home + sep + "lf5" + sep + CONFIG_FILE_NAME;
  }

  /**
   * Ensures that the MRU list will have a MaxSize.
   */
  protected void setMaxSize(int maxSize) {
    if (maxSize < _mruFileList.size()) {
      for (int i = 0; i < _mruFileList.size() - maxSize; i++) {
        _mruFileList.removeLast();
      }
    }

    _maxSize = maxSize;
  }
  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces
  //--------------------------------------------------------------------------
}