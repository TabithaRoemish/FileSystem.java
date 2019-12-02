//---------------------------------------------------------------------------
// --------------------------- Directory.java--------------------------------
// Angie(Nomingerel) Tserenjav & Tabitha Roemish
// CSS 430 Section A
// Creation Date: 11/25/19
// Date of Last Modification: 12/06/19
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
// Notes: The "/" root directory maintains each file in a different directory 
// entry that contains its file name (maximum 30 characters; 60 bytes in Java) 
// and the corresponding inode number.
// --------------------------------------------------------------------------
// --------------------------------------------------------------------------
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.
    private int size;          // size of directory

    public Directory(int maxInumber) {  //directory constructor
        fsize = new int[maxInumber];     // maxInumber = max files
        for (int i = 0; i < maxInumber; i++)
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        size = maxInumber;
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length();        // fsize[0] is the size of "/".
        root.getChars(0, fsize[0], fnames[0], 0); // fnames[0] includes "/"
    }


    public void bytes2directory(byte data[]) {
        if (data != null && data.length > 0) {
            // assumes data[] received directory information from disk
            int offSet = 0;
            for (int i = 0; i < size; i++) {
                fsize[i] = SysLib.bytes2int(data, offSet);
                offSet += 4;
            }
            // initializes the Directory instance with this data[]
            for (int i = 0; i < size; i++) {
                String str = new String(data, offSet, maxChars * 2);
                str.getChars(0, fsize[i], fnames[i], 0);
                offSet += (maxChars * 2);
            }
        }
    }

    public byte[] directory2bytes() {
      // converts and return Directory information into a plain byte array
      // this byte array will be written back to disk
        byte[] dir = new byte[fnames.length * maxChars * 2 + size * 4];
        int diff = 0;
        int offSet = 0;

        for (int i = 0; i < size; i++) {
            SysLib.int2bytes(fsize[i], dir, diff);
            diff += 4;
        }

        for (int i = 0; i < fnames.length; i++) {
            String strName= new String(fnames[i], offSet, fsize[i]);
            byte[]data = strName.getBytes();
            System.arraycopy(data,0,dir,diff,data.length);

            diff = diff + maxChars * 2;
        }

        return dir;
    }

    public short ialloc(String filename) {
       // filename is the one of a file to be created.
       // allocates a new inode number for this filename
        for (int i = 0; i < size; i++) {
            if (fsize[i] == 0) {
                int minimum = Math.min(filename.length(), maxChars);
                for (int j = 0; j < minimum; j++) {
                    fnames[i][j] = filename.charAt(j);
                }
                fsize[i] = minimum;
                return (short) i;
            }
        }
        return -1;
    }

    public boolean ifree(short iNumber) {
      // deallocates this inumber (inode number)
      // the corresponding file will be deleted.
        if (iNumber >= 0 && fsize[iNumber] > 0) {
            int filePtr = 0;

            while (filePtr < maxChars) {
                fnames[iNumber][filePtr] = 0;
                filePtr++;
            }
            fsize[iNumber] = 0;
            return true;
        }
        return false;
    }

    public short namei(String filename) {
       // returns the inumber corresponding to this filename
        for (int i = 0; i < size; i++) {
            if (fsize[i] == filename.length()) {
                String str= "";
                for (int j = 0; j < fsize[i]; j++) {
                    str += fnames[i][j];
                }

                if (str.equals(filename))
                    return (short) i;
            }
        }
        return -1;
    }
}
