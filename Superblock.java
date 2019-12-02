
class Superblock {

   public int totalBlocks; // the number of disk blocks (1000)
   public int totalInodes; // the number of inodes (64)
   public int freeList;    // the block number of the free list's head 
   private static int MAX_INODE_BLOCKS = 64;
   private static int AT_TB = 0; //beginning of existing superblock
   private static int AT_TI = 4; //+ 4 size of int in superblock
   private static int AT_FL = 8; // + 4 + 4 in superblock byte array
   private static int ERROR = -1;
   private static int OK = 1;
   

   public SuperBlock( int diskSize ) {
	 
	// read superblock
	byte[] SB = new byte[Disk.blockSize];
	if (SysLib.rawread( 0, SB ) == ERROR )
		throw new FileSystemException("Error reading Disk: SuperBlock");
	
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
   
   public int getFreeBlock( )
   {
	   if( freeList != ERROR )
	   {
			int freeBlock = freeList; // set return value
			//update freeList
			byte[] block = new byte[Disk.blockSize];
			if ( SysLib.rawread(freeList, block) == ERROR )
				return ERROR;
			int nextFree = SysLib.bytes2int( block, 0);
			this.freeList = nextFree;
			
			return freeBlock;
	   }
	   else
			return ERROR;
	   
   }
   
   public boolean releaseBlock( int blockNum )
   {
	   int initialBlock = ( totalInodes / (Disk.blockSize/Inode.iNodeSize)) + 1;
	   if( blockNum > totalBlocks || blockNum < initialBlock ) )
			return false;
	   else
	   {
			byte[] b = new byte[Disk.blockSize];
			SysLib.int2bytes( this.freeList, b ); //change freeBlock Int to bytes
			if (SysLib.rawwrite ( blockNum, b ) == ERROR ) // write bytes to released block
				return false;
			this.freeList = blockNum; //set freeList head to released Block
			return true;
	   }
	   
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
		save( );
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
	   this.freeList = (totalInodes / (Disk.blockSize/Inode.iNodeSize)) + 1; 
	   byte[] b = new byte[Disk.blockSize];
	   for (int i = this.freeList; i < totalBlocks; i++)
	   {
		   if( ( i + 1 ) = totalBlocks ) // end of freeList 
				SysLib.int2bytes( -1, b, 0 ); // add -1 to start of block
		   else
				SysLib.int2bytes( ( i + 1), b, 0 ); //otherwise add i+1
		   
		   //write buffer to block
		   if( SysLib.rawwrite( i, b ) == ERROR )
		     throw new FileSystemException("Could not write to disk: SuperBlock");
	   }
   }
   
   // saves all SuperBlock properties to buffer and writes buffer to block 0
   public void save( )
   {
	   byte[] saveBuffer = new byte[Disk.blockSize];
	   SysLib.int2bytes( totalBlocks, saveBuffer, AT_TB );
	   SysLib.int2bytes( totalInodes, saveBuffer, AT_TI );
	   SysLib.int2bytes( freeList, saveBuffer, AT_FL );
	   if (SysLib.rawwrite( 0, saveBuffer ) == ERROR )
		throw new FileSystemException ("Could not write to disk: Superblock");
	   
   }
   
}
