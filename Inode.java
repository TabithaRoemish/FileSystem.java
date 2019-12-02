public class Inode {

   private final static int iNodeSize = 32;       // fix to 32 bytes
   private final static int directSize = 11;      // # direct pointers
   private final static int ERROR = -1;
   private final static int OK = 1;
   private final static int AT_LNG = 0;
   private final static int AT_CNT = 4;
   private final static int AT_FLG = 6;
   private final static int AT_DIR = 8; // 11 pointers 2bytes each
   private final static int AT_IND = 30; //2*11 + 8
   private final static int NODES_PER_DISKBLOCK = 16; //512 (size of block) / 32 (size of Inode)
   
   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers
   public short indirect;                         // a indirect pointer


   Inode( ) {                                     // a default constructor

      length = 0;
      count = 0;
      flag = 1;
      for ( int i = 0; i < directSize; i++ )
         direct[i] = -1;
      indirect = -1;

   }


   Inode( short iNumber ) {                       // retrieving inode from disk
      
	  byte[] b = new byte[Disk.blockSize];
	  int diskBlock = iNumber / NODES_PER_DISKBLOCK + 1;
	  SysLib.rawread(diskBlock, b);
	  
	  // read Inode values from block
	  int InodeAddress = ( iNumber % NODES_PER_DISKBLOCK ) * iNodeSize;
	  this.length = SysLib.bytes2int( diskBlock, InodeAddress );
	  this.count = SysLib.bytes2short(diskBlock, ( InodeAddress + AT_CNT ) );
	  this.flag = SysLib.bytes2short(diskBlock, ( IndodeAddress + AT_FLG ) );
	  for (int i = 0; i < directSize; i++ )
	  {
		  this.direct[i] = SysLib.bytes2short(diskBlock, ( InodeAddress + AT_DIR + ( i * 2 ) ) ); //2 bytes per direct[]
	  }
	  this.indirect = SysLib.bytes2short( diskBlock, (InodeAddress + AT_IND ) );
	  
   }



	int toDisk( short iNumber ) {                  // save to disk as the i-th inode
      

	  if( iNumber < 0 || iNumber > Superblock.MAX_INODE_BLOCKS )
		return ERROR;
		
	  // find diskblock and InodeAddress
	  int diskBlock = iNumber / NODES_PER_DISKBLOCK + 1;
	  int InodeAddress = ( iNumber % NODES_PER_DISKBLOCK ) * iNodeSize;
	  
	  //get current block data
	  byte[] updateBlock = byte[Disk.blockSize];
	  if (SysLib.rawread(diskBlock, updateBlock) == ERROR )
		return ERROR;
	  
	  // add properties to updateBlock
	  SysLib.int2bytes(this.length, updateBlock, InodeAddress);
	  SysLib.short2bytes(this.count, updateBlock, ( InodeAddress + AT_CNT ) );
	  SysLib.short2bytes(this.flag, updateBlock, ( IndodeAddress + AT_FLG ) );
	  for (int i = 0; i < directSize; i++ )
	  {
		  SysLib.short2bytes(this.indirect[i], updateBlock, ( InodeAddress + AT_DIR + ( i * 2 ) ) ); // 2 bytes per direct[]
	  }
	  SysLib.short2bytes(this.indirect, updateBlock, ( InodeAddress + AT_IND ) );
	  
	  // write updateBlock to disk
	  if (SysLib.rawwrite( diskBlock, updateBlock ) == ERROR )
		return ERROR; 
	  
	  
	  return OK;
	  
   }
   
   int addDiskBlock ( int freeBlock )
   {
	   
	   
   }
   
   int getSize( )
   {
	   return this.length;
   }

}
