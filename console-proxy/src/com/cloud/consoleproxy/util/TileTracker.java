/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.consoleproxy.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class TileTracker {
	
	// 2 dimension tile status snapshot, a true value means the corresponding tile has been invalidated
	private boolean[][] snapshot;
	
	private int tileWidth = 0;
	private int tileHeight = 0;
	private int trackWidth = 0;
	private int trackHeight = 0;
	
	public TileTracker() {
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public void setTileWidth(int tileWidth) {
		this.tileWidth = tileWidth;
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public void setTileHeight(int tileHeight) {
		this.tileHeight = tileHeight;
	}

	public int getTrackWidth() {
		return trackWidth;
	}

	public void setTrackWidth(int trackWidth) {
		this.trackWidth = trackWidth;
	}

	public int getTrackHeight() {
		return trackHeight;
	}

	public void setTrackHeight(int trackHeight) {
		this.trackHeight = trackHeight;
	}
	
	public void initTracking(int tileWidth, int tileHeight, int trackWidth, int trackHeight) {
		assert(tileWidth > 0);
		assert(tileHeight > 0);
		assert(trackWidth > 0);
		assert(trackHeight > 0);
		assert(tileWidth <= trackWidth);
		assert(tileHeight <= trackHeight);
		
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.trackWidth = trackWidth;
		this.trackHeight = trackHeight;
		
		int cols = getTileCols();
		int rows = getTileRows();
		snapshot = new boolean[rows][cols];
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++)
				snapshot[i][j] = false;
	}
	
	public synchronized void resize(int trackWidth, int trackHeight) {
		assert(tileWidth > 0);
		assert(tileHeight > 0);
		assert(trackWidth > 0);
		assert(trackHeight > 0);
		
		this.trackWidth = trackWidth;
		this.trackHeight = trackHeight;
		
		int cols = getTileCols();
		int rows = getTileRows();
		snapshot = new boolean[rows][cols];
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++)
				snapshot[i][j] = true;
	}
	
	public void invalidate(Rectangle rect) {
		setTileFlag(rect, true);
	}
	
	public void validate(Rectangle rect) {
		setTileFlag(rect, false);
	}

	public List<TileInfo> scan(boolean init) {
		List<TileInfo> l = new ArrayList<TileInfo>();
		
		synchronized(this) {
			for(int i = 0; i < getTileRows(); i++) {
				for(int j = 0; j < getTileCols(); j++) {
					if(init || snapshot[i][j]) {
						Rectangle rect = new Rectangle();
						rect.y = i*tileHeight;
						rect.x = j*tileWidth;
						rect.width = Math.min(trackWidth - rect.x, tileWidth);
						rect.height = Math.min(trackHeight - rect.y, tileHeight);
						
						l.add(new TileInfo(i, j, rect));
						snapshot[i][j] = false;
					}
				}
			}
			
			return l;
		}	
	}
	
	public boolean hasFullCoverage() {
		synchronized(this) {
			for(int i = 0; i < getTileRows(); i++) {
				for(int j = 0; j < getTileCols(); j++) {
					if(!snapshot[i][j])
						return false;
				}
			}
		}	
		return true;
	}

	
	
	public void initCoverageTest() {
		synchronized(this) {
			for(int i = 0; i < getTileRows(); i++) {
				for(int j = 0; j < getTileCols(); j++) {
					snapshot[i][j] = false;
				}
			}
		}	
	}
	
	// listener will be called while holding the object lock, use it
	// with care to avoid deadlock condition being formed
	public synchronized void scan(int nStartRow, int nStartCol, ITileScanListener listener) {
		assert(listener != null);
		
		int cols = getTileCols();
		int rows = getTileRows();
		
		nStartRow = nStartRow % rows;
		nStartCol = nStartCol % cols;
		
		int nPos = nStartRow*cols + nStartCol;
		int nUnits = rows*cols;
		int nStartPos = nPos;
		int nRow;
		int nCol;
		do {
			nRow = nPos / cols;
			nCol = nPos % cols;
			
			if(snapshot[nRow][nCol]) {
				int nEndCol = nCol;
				for(; nEndCol < cols && snapshot[nRow][nEndCol]; nEndCol++) {
					snapshot[nRow][nEndCol] = false;
				}
				
				Rectangle rect = new Rectangle();
				rect.y = nRow*tileHeight;
				rect.height = tileHeight;
				rect.x = nCol*tileWidth;
				rect.width = (nEndCol - nCol)*tileWidth;
				
				if(!listener.onTileChange(rect, nRow, nEndCol))
					break;
			}
			
			nPos = (nPos + 1) % nUnits;
		} while(nPos != nStartPos);
	}
	
	public void capture(ITileScanListener listener) {
		assert(listener != null);
		
		int cols = getTileCols();
		int rows = getTileRows();
		
		RegionClassifier classifier = new RegionClassifier();
		int left, top, right, bottom;
		
		synchronized(this) {
			for(int i = 0; i < rows; i++) {
				top = i*tileHeight;
				bottom = Math.min(top + tileHeight, trackHeight); 
				for(int j = 0; j < cols; j++) {
					left = j*tileWidth;
					right = Math.min(left + tileWidth, trackWidth);
					
					if(snapshot[i][j]) {
						snapshot[i][j] = false;
						classifier.add(new Rectangle(left, top, right - left, bottom - top));
					}
				}
			}
		}
		listener.onRegionChange(classifier.getRegionList());
	}
	
	private synchronized void setTileFlag(Rectangle rect, boolean flag) {
		int nStartTileRow;
		int nStartTileCol;
		int nEndTileRow;
		int nEndTileCol;
		
		int cols = getTileCols();
		int rows = getTileRows();
		
		if(rect != null) {
			nStartTileRow = Math.min(getTileYPos(rect.y), rows - 1);
			nStartTileCol = Math.min(getTileXPos(rect.x), cols - 1);
			nEndTileRow = Math.min(getTileYPos(rect.y + rect.height - 1), rows -1);
			nEndTileCol = Math.min(getTileXPos(rect.x + rect.width - 1), cols -1);
		} else {
			nStartTileRow = 0;
			nStartTileCol = 0;
			nEndTileRow = rows - 1;
			nEndTileCol = cols - 1;
		}
		
		for(int i = nStartTileRow; i <= nEndTileRow; i++)
			for(int j = nStartTileCol; j <= nEndTileCol; j++)
				snapshot[i][j] = flag;
	}
	
	private int getTileRows() {
		return (trackHeight + tileHeight - 1) / tileHeight;
	}
	
	private int getTileCols() {
		return (trackWidth + tileWidth - 1) / tileWidth;
	}
	
	private int getTileXPos(int x) {
		return x / tileWidth;
	}
	
	public int getTileYPos(int y) {
		return y / tileHeight;
	}
}
