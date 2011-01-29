package org.klomp.snark.cmd;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.klomp.snark.MetaInfo;
import org.klomp.snark.Storage;
import org.klomp.snark.StorageListener;

/**
 * Reports the status of allocation of space via logging data to
 * the console.
 * 
 * @author Elizabeth Fong (elizabeth@ctyalcove.org)
 */
public class ConsoleStorageReporter implements StorageListener
{
    public void storageCreateFile (Storage storage, String name, long length)
    {
        log.log(Level.FINE, "Creating file '" + name + "' of length " +
            length + ": ");
    }

    // How much storage space has been allocated
    private long allocated = 0;

    public void storageAllocated (Storage storage, long length)
    {
        System.err.print(".");
        allocated += length;
        if (allocated == storage.getMetaInfo().getTotalLength()) {
            System.err.println();
            log.log(Level.INFO, "Finished allocating storage space");
        }
    }

    boolean allChecked = false;

    boolean checking = false;

    public void storageChecked (Storage storage, int num, boolean checked)
    {
        if (!allChecked && !checking) {
            // Use the MetaInfo from the storage since our own might not
            // yet be setup correctly.
            MetaInfo meta = storage.getMetaInfo();
            if (meta != null) {
                log.log(Level.INFO, "Checking existing " + meta.getPieces()
                    + " pieces: ");
            }
            checking = true;
        }
        if (checking) {
            if (checked) {
                System.err.print("+");
            } else {
                System.err.print("-");
            }
        } else {
            log.log(Level.FINE, "Got " + (checked ? "" : "BAD ") + "piece: "
                + num);
        }
    }

    public void storageAllChecked (Storage storage)
    {
        allChecked = true;
        checking = false;
    }

    /** The Java logger used to process our log events. */
    protected static final Logger log = Logger.getLogger("org.klomp.snark.storage");
}
