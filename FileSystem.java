//---------------------------------------------------------------------------
// --------------------------- FileSystem.java------------------------------------
// Angie(Nomingerel) Tserenjav & Tabitha Roemish
// CSS 430 Section A
// Creation Date: 11/25/19
// Date of Last Modification: 12/06/19
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
// Note: This is the main file system class.
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
public class FileSystem {

    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;

    private Superblock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem(int blocks) {
        superblock = new Superblock(blocks);
        directory = new Directory(superblock.totalInodes);
        filetable = new FileTable(directory);

        FileTableEntry dirEntry = open("/", "r");
        int directorySize = fsize(dirEntry);
        if (directorySize > 0) {
            byte[] directoryData = new byte[directorySize];
            read(dirEntry, directoryData);
            directory.bytes2directory(directoryData);
        }
        close(dirEntry);
    }

    public void sync() {
        byte[] dirData = directory.directory2bytes();
        //open root dir
        FileTableEntry rootDir = open("/", "w");
        //write
        write(rootDir, dirData);
        close(rootDir);
        superblock.sync();
    }

    public boolean format(int files) {
        if (files > 64 || files < 0)
            return false;
        superblock.format(files);
        return true;
    }

    public FileTableEntry open(String filename, String mode) {
        FileTableEntry tableEntry = filetable.falloc(filename, mode);

        if (mode.equals("w")) {
            if (!deallocAllBlocks(tableEntry))
                return null;
        }

        return tableEntry;
    }

    public boolean close(FileTableEntry tblEntry) {
        if (tblEntry == null)
            return false;

        synchronized (tblEntry) {
            tblEntry.count -= 1;

            if (tblEntry.count == 0) {
                return filetable.ffree(tblEntry);
            }
            return true;
        }
    }
    
    public int read(FileTableEntry tblEntry, byte[] buffer) {
        if (tblEntry.mode.equals("w") || tblEntry.mode.equals("a"))
            return -1;

        int sizeToRead = buffer.length;
        int readBytes = 0;
        int remToRead = 0;

        synchronized (tblEntry) {
            while (tblEntry.seekPtr < fsize(tblEntry) && sizeToRead > 0) {
                if (tblEntry.inode.takeTargetBlk(tblEntry.seekPtr) == -1)
                    break;

                byte[] data = new byte[Disk.blockSize];

                SysLib.rawread(tblEntry.inode.takeTargetBlk(tblEntry.seekPtr), data);

                int remBlocks = Disk.blockSize - remToRead;
                int remInFile = fsize(tblEntry) - tblEntry.seekPtr;
                int offset = tblEntry.seekPtr % Disk.blockSize;

                if (remBlocks > remInFile) {
                    remToRead = remInFile;
                } else {
                    remToRead = remBlocks;
                }

                if (remToRead > sizeToRead)
                    remToRead = sizeToRead;

                System.arraycopy(data, offset, buffer, readBytes, remToRead);
                tblEntry.seekPtr += remToRead;
                sizeToRead -= remToRead;
                readBytes += remToRead;
            }
            return readBytes;
        }
    }

    public int write(FileTableEntry tblEntry, byte[] buffer) {
        if (tblEntry.mode.equals("r") || tblEntry == null)
            return -1;

        synchronized (tblEntry) {
            int writeBytes = 0;
            int currentSize = buffer.length;

            while (currentSize > 0) {
                int target = tblEntry.inode.takeTargetBlk(tblEntry.seekPtr);

                if (target == -1) {
                    short newBlock = (short) superblock.lookforFreeBlk();
                    int newBlockIndex = tblEntry.inode.getIndexBlkNum(tblEntry.seekPtr, newBlock);
                    if (newBlockIndex == -1 || newBlockIndex == -2)
                        return -1;

                    else if (newBlockIndex == -3) {
                        short anotherFreeBlock = (short) superblock.lookforFreeBlk();

                        if (!tblEntry.inode.setIndexBlock(anotherFreeBlock) ||
                                tblEntry.inode.getIndexBlkNum(tblEntry.seekPtr, newBlock) != 0)
                            return -1;
                    }

                    target = newBlock;
                }

                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(target, data);

                int tempSeekPtr = tblEntry.seekPtr % Disk.blockSize;
                int difference = Disk.blockSize - tempSeekPtr;

                if (difference <= currentSize) {
                    System.arraycopy(buffer, writeBytes, data, tempSeekPtr, difference);

                    SysLib.rawwrite(target, data);

                    tblEntry.seekPtr += difference;
                    writeBytes += difference;
                    currentSize -= difference;
                } else {
                    System.arraycopy(buffer, writeBytes, data, tempSeekPtr, currentSize);

                    SysLib.rawwrite(target, data);

                    tblEntry.seekPtr += currentSize;
                    writeBytes += currentSize;
                    currentSize = 0;
                }
            }

            if (tblEntry.seekPtr > tblEntry.inode.length)
                tblEntry.inode.length = tblEntry.seekPtr;

            tblEntry.inode.toDisk(tblEntry.iNumber);
            return writeBytes;
        }
    }

    public int seek(FileTableEntry tblEntry, int offset, int location) {
        synchronized (tblEntry) {
            switch (location) {
                case SEEK_SET:
                    tblEntry.seekPtr = offset;
                    break;
                case SEEK_CUR:
                    tblEntry.seekPtr += offset;
                    break;
                case SEEK_END:
                    tblEntry.seekPtr = tblEntry.inode.length + offset;
                    break;

                default:
                    return -1;
            }
            if (tblEntry.seekPtr < 0)
                tblEntry.seekPtr = 0;

            if (tblEntry.seekPtr > tblEntry.inode.length)
                tblEntry.seekPtr = tblEntry.inode.length;

            return tblEntry.seekPtr;
        }
    }

    private boolean deallocAllBlocks(FileTableEntry tblEntry) {
        if (tblEntry.count != 1)
            return false;

        byte[] data = tblEntry.inode.freeIndirectBlk();
        int blockID;

        for (int i = 0; i < tblEntry.inode.directSize; i++) {
            if (tblEntry.inode.direct[i] != -1) {
                superblock.pushFreeBlk(i);
                tblEntry.inode.direct[i] = -1;
            }
        }
        if (data != null) {
            while ((blockID = SysLib.bytes2short(data, 0)) != -1) {
                superblock.pushFreeBlk(blockID);
            }
        }

        tblEntry.inode.toDisk(tblEntry.iNumber);
        return true;
    }

    boolean delete(String filename) {
        FileTableEntry tblEntry = open(filename, "w");
        return (directory.ifree(tblEntry.iNumber) && close(tblEntry));
    }

    public synchronized int fsize(FileTableEntry tblEntry) {
        synchronized (tblEntry) {
            return tblEntry.inode.length;
        }
    }
}
