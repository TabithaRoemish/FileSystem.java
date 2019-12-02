
class Superblock {

   public int totalBlocks; // the number of disk blocks

   public int totalInodes; // the number of inodes

   public int freeList;    // the block number of the free list's head

   

   public SuperBlock( int diskSize ) {

	byte[] superBlock = new byte[Disk.blockSize];
	totalBlocks = 1000;
	totalInodes = 32;
	freeList = 2;

   }

   ...	

}
