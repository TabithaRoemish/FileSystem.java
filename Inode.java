public class Inode {

   public final static int iNodeSize = 32;       // fix to 32 bytes
   public final static int directSize = 11;      // # direct pointers
   private final static int ERROR = -1;
   private final static int OK = 0;
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
	  this.length = SysLib.bytes2int( b, InodeAddress );
	  this.count = SysLib.bytes2short( b, ( InodeAddress + AT_CNT ) );
	  this.flag = SysLib.bytes2short( b, ( InodeAddress + AT_FLG ) );
	  for (int i = 0; i < directSize; i++ )
	  {
		  this.direct[i] = SysLib.bytes2short(b, ( InodeAddress + AT_DIR + ( i * 2 ) ) ); //2 bytes per direct
	  }
	  this.indirect = SysLib.bytes2short( b, (InodeAddress + AT_IND ) );
	  
   }



	int toDisk( short iNumber ) {                  // save to disk as the i-th inode
      

	  if( iNumber < 0 || iNumber > 64 ) // total number of iNodes
		return ERROR;
		
	  // find diskblock and InodeAddress
	  int diskBlock = iNumber / NODES_PER_DISKBLOCK + 1;
	  int InodeAddress = ( iNumber % NODES_PER_DISKBLOCK ) * iNodeSize;
	  
	  //get current block data
	  byte[] updateBlock = new byte[Disk.blockSize];
	  if (SysLib.rawread(diskBlock, updateBlock) == ERROR )
		return ERROR;
	  
	  // add properties to updateBlock
	  SysLib.int2bytes(this.length, updateBlock, InodeAddress);
	  SysLib.short2bytes(this.count, updateBlock, ( InodeAddress + AT_CNT ) );
	  SysLib.short2bytes(this.flag, updateBlock, ( InodeAddress + AT_FLG ) );
	  for (int i = 0; i < directSize; i++ )
	  {
		  SysLib.short2bytes(this.direct[i], updateBlock, ( InodeAddress + AT_DIR + ( i * 2 ) ) ); 
	  }
	  SysLib.short2bytes(this.indirect, updateBlock, ( InodeAddress + AT_IND ) );
	  
	  // write updateBlock to disk
	  if (SysLib.rawwrite( diskBlock, updateBlock ) == ERROR )
		return ERROR; 
	  
	  
	  return OK;
	  
   }

   // need to add methods to match fileSystem
   int takeTargetBlk(int Ptr)
   {
	   //return block number
	   //or return -1 for error
	   int targetBlock = ERROR;
	   int location = Ptr / Disk.blockSize;
	   
	   if ( location < directSize )
			targetBlock = direct[location];
		
	   if ( this.indirect != ERROR )
	   {
		   byte [] diskBlock = new byte[Disk.blockSize];
		   if (SysLib.rawread(this.indirect, diskBlock) != ERROR )
				targetBlock = SysLib.bytes2short(diskBlock, ( ( location - directSize ) * 2 ) );
	   }
	   
	   return targetBlock;   
   }

   int getIndexBlkNum(int Ptr, short block)
   {
	   // return the diskblockindex of Inode of the block or
	   // return ERROR - return -3 if indirect is blank
	   int diskBlockIndex = Ptr / Disk.blockSize;
	   if (diskBlockIndex < directSize )
	   {
		   if (direct[diskBlockIndex] >= 0 )
		   {
			   return ERROR;
		   }
		   if (diskBlockIndex > 0 && direct[diskBlockIndex - 1] == ERROR )
		   {
			   return ERROR;
		   }
		   
		   direct[diskBlockIndex] = block;
		   return OK;
	   }
	   
	   if (this.indirect < 0 )
	   {
		   return -3; // error checked for in fileSystem
	   }
	   else
	   {
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect,data);

            int cal = (diskBlockIndex - directSize) * 2;
            if ( SysLib.bytes2short(data, cal) > 0){
                return -1;
            }
            else
            {
                SysLib.short2bytes(block, data, cal);
                SysLib.rawwrite(indirect, data);
            }
        }
        return 0;  
   }
   
   
   boolean setIndexBlock( short FreeBlock)
   {
		for (int i = 0; i < directSize; i++ )
		{
			if ( direct[i] == ERROR )
				return false;
		}
		if ( indirect != ERROR )
			return false;
		
		this.indirect = FreeBlock;
		byte[] b = new byte[Disk.blockSize];
		
		for (int i = 0; i < (Disk.blockSize/2); i++)
		{
			SysLib.short2bytes( ( short ) -1,  b , i*2);
		}
		SysLib.rawwrite(FreeBlock, b);
		
		return true;
   }
   
   byte[] freeIndirectBlk()
   {
	   if ( this.indirect >=0 )
	   {
			byte[] b = new byte[Disk.blockSize];
			SysLib.rawread( indirect, b );
			this.indirect = ERROR; //release indirect
			return b;
	   }
	   else 
			return null;
   }
   
   

}
