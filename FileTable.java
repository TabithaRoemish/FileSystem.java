//---------------------------------------------------------------------------
// --------------------------- FileTable.java------------------------------------
// Angie(Nomingerel) Tserenjav & Tabitha Roemish
// CSS 430 Section A
// Creation Date: 11/25/19
// Date of Last Modification: 12/06/19
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
// Note: The file system maintains the file (structure) table shared among all 
// user threads.
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------

import java.util.Vector;

public class FileTable {
    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public final static int UNUD = 0; 
    public final static int UD = 1; 
    public final static int RD = 2; 
    public final static int WR = 3; 


     public FileTable( Directory directory ) { // constructor
      table = new Vector( );     // instantiate a file (structure) table
      dir = directory;           // receive a reference to the Director
   }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc(String filename, String mode) {
        Inode tempINode;
        short iNumber;

        for (; ; ) {
            if (filename.equals("/")) {
                iNumber = 0;
            } else {
                iNumber = dir.namei(filename);
            }

            if (iNumber >= 0) {
                tempInode = new Inode(iNumber);

                if (mode.equals("r")) {
                    if (tempInode.flag <= RD) {
                        tempInode.flag = RD;
                        break;
                    } else if (tempInode.flag == WR) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    if (tempInode.flag == UD || tempInode.flag == UNUD) {
                        tempInode.flag = WR;
                        break;
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } else if (mode.equals("r") == false) {
                iNumber = dir.ialloc(filename);
                tempInode = new Inode(iNumber);
                tempInode.flag = WR;
                break;
            } else {
                return null;
            }

        }
        tempInode.count += 1;
        tempInode.toDisk(iNumber);

        FileTableEntry tableEntry = new FileTableEntry(tempInode, iNumber, mode);
        table.addElement(tableEntry);
        return tableEntry;
    }

    public synchronized boolean ffree(FileTableEntry e) {
        Inode tempNode = new Inode(e.iNumber);
        if (table.remove(e)) {
            if (tempNode.flag == RD) {
                notify();
                tempNode.flag = UD;
            }
            else if (tempNode.flag == WR) {
                tempNode.flag = UD;
                notify();
            }

            tempNode.count -= 1;
            tempNode.toDisk(e.iNumber);
            return true;
        }
        return false;
    }

     public synchronized boolean fempty( ) {
      return table.isEmpty( );  // return if table is empty 
   }                            // should be called before starting a format

}
