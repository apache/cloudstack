/*
 * StorageListener.java - Interface used as callback when storage changes.
 * Copyright (C) 2003 Mark J. Wielaard
 * 
 * This file is part of Snark.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.klomp.snark;

/**
 * Callback used when Storage changes.
 */
public interface StorageListener
{
    /**
     * Called when the storage creates a new file of a given length.
     */
    void storageCreateFile (Storage storage, String name, long length);

    /**
     * Called to indicate that length bytes have been allocated.
     */
    void storageAllocated (Storage storage, long length);

    /**
     * Called when storage is being checked and the num piece of that total
     * pieces has been checked. When the piece hash matches the expected piece
     * hash checked will be true, otherwise it will be false.
     */
    void storageChecked (Storage storage, int num, boolean checked);

    /**
     * Called when all pieces in the storage have been checked. Does not mean
     * that the storage is complete, just that the state of the storage is
     * known.
     */
    void storageAllChecked (Storage storage);
}
