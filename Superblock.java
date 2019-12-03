
class Superblock {

   public int totalBlocks; // the number of disk blocks (1000)
   public int totalInodes; // the number of inodes (64)
   public int freeList;    // the block number of the free list's head 
   private static int MAX_INODE_BLOCKS = 64;
   private static int AT_TB = 0; //beginning of existing superblock
   private static int AT_TI = 4; //+ 4 size of int in superblock
   private static int AT_FL = 8; // + 4 + 4 in superblock byte array
   private static int ERROR = -1;
   private static int OK = 0;
   

   public Superblock( int diskSize ) {
	 
	SysLib.cout("Superblock created!");
	// read superblock
	byte[] SB = new byte[Disk.blockSize];
	SysLib.rawread( 0, SB );
	
	//read superblock disk into properties
	totalBlocks = SysLib.bytes2int( SB, AT_TB ); 
	totalInodes = SysLib.bytes2int( SB, AT_TI ); 
	freeList = SysLib.bytes2int( SB, AT_FL ); 
	
	//check if disk data matches parameter,
	// if not - initialize new superblock
	if( totalBlocks != diskSize )
	{
		this.totalBlocks = diskSize;
		format( MAX_INODE_BLOCKS ); // calls save( )
	}

   }
   
   public int lookforFreeBlk( )
   {
      int freeBlk = freeList;
      if (freeList > 0) 
	  {
          // Check if it is still within the range
            if (freeList < totalBlocks) 
			{
				byte[] blockInfo = new byte[Disk.blockSize];
				SysLib.rawread(freeList, blockInfo);
				freeList = SysLib.bytes2int(blockInfo, 0);
				SysLib.int2bytes(0, blockInfo, 0);
				SysLib.rawwrite(freeBlk, blockInfo);
            }
        }
        return freeBlk;   
   }
   
   public void pushFreeBlk( int blkNum )
   { //release block
        if (blkNum < 0) {
            return;
        } else {
            byte[] data = new byte[Disk.blockSize];
            for (int i = 0; i < Disk.blockSize; i++) {
                data[i] = 0;
            }

            SysLib.int2bytes(freeList, data, 0);
            SysLib.rawwrite(blkNum, data);
            freeList = blkNum;

        }
        return;
   }
   
   //mimics SysLib.format requirements 
   public int format( int inodeBlocks)
   {
	 if( inodeBlocks > MAX_INODE_BLOCKS)
		return ERROR;
	 else
	 {
		initializeInodeBlocks( inodeBlocks);
		initializeFreeList( );
		sync( );
		return OK;
	 }
   }
   
   //builds the Inode blocks for format and sets totalInodes
   public void initializeInodeBlocks( int inodeBlockTotal)
   {
	   	for (short i = 0; i < inodeBlockTotal; i++)
		{
		  Inode node = new Inode();
		  node.flag = 0; // mark unused
		  node.toDisk(i);
		}
		this.totalInodes = inodeBlockTotal;
   }
   
   //sets FreeList initial pointer
   // and fills remaining blocks with next block pointer
   public void initializeFreeList( )
   {
	   this.freeList = (totalInodes / (Disk.blockSize/Inode.iNodeSize)) + 2; 
	   for (int i = this.freeList; i < totalBlocks; i++)
	   {
            byte[] data = new byte[Disk.blockSize];
            SysLib.int2bytes(i + 1, data, 0);
            SysLib.rawwrite(i, data);
	   }
   }
   
   // saves all SuperBlock properties to buffer and writes buffer to block 0
   public void sync( )
   {
	   byte[] saveBuffer = new byte[Disk.blockSize];
	   SysLib.int2bytes( totalBlocks, saveBuffer, AT_TB );
	   SysLib.int2bytes( totalInodes, saveBuffer, AT_TI );
	   SysLib.int2bytes( freeList, saveBuffer, AT_FL );
	   SysLib.rawwrite( 0, saveBuffer );
	   
   }
   
}
