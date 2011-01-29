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

package org.apache.log4j.varia;

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.LogLog;

/**
 *  Plays a sound clip created using Applet.newAudioClip when an event is received.
 *  
 *  If the audio format is not supported, a message stating the SoundAppender could 
 *  not be initialized is logged.
 *  
 *  Use a filter in combination with this appender to control when the appender is 
 *  triggered.
 *  
 *  For example, in the appender definition, include a LevelMatchFilter configured
 *  to accept WARN or greater, followed by a DenyAllFilter.
 *  
 *  @author Scott Deboy
 * 
 */
public final class SoundAppender extends AppenderSkeleton {

	private AudioClip clip;
	private String audioURL;

	public SoundAppender() {
	}

	/**
	 * Attempt to initialize the appender by creating a reference to an AudioClip.
	 * 
	 * Will log a message if format is not supported, file not found, etc.
	 * 
	 */
	public void activateOptions() {
		/*
		 * AudioSystem.getAudioInputStream requires jdk 1.3,
		 * so we use applet.newaudioclip instead
		 *
		 */
		try {
			clip = Applet.newAudioClip(new URL(audioURL));
		} catch (MalformedURLException mue) {
            LogLog.error("unable to initialize SoundAppender", mue);}
		if (clip == null) {
		      LogLog.error("Unable to initialize SoundAppender");
		}
	}

	/**
	 * Accessor
	 * 
	 * @return audio file
	 */
	public String getAudioURL() {
		return audioURL;
	}

	/**
	 * Mutator - common format for a file-based url:
	 * file:///c:/path/someaudioclip.wav
	 * 
	 * @param audioURL
	 */
	public void setAudioURL(String audioURL) {
		this.audioURL = audioURL;
	}

	/**
	 * Play the sound if an event is being processed
	 */
	protected void append(LoggingEvent event) {
		if (clip != null) {
			clip.play();
		}
	}

	public void close() {
		//nothing to do
	}

    /**
     * Gets whether appender requires a layout.
     * @return false
     */
  public boolean requiresLayout() {
      return false;
  }

}
