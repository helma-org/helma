package helma.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MD5Encoder {

    private static MessageDigest md;

	/** used by commandline script to create admin username & password	  */
    public static void main ( String args[] ) throws Exception  {
        if ( args.length < 2 )  {
            System.out.println( "\n\nUsage: helma.util.MD5Encoder <username> <password>");
            System.out.println( "Output:");
            System.out.println( "adminUsername=<encoded username>");
            System.out.println( "adminPassword=<encoded password>\n");
            System.exit(0);
        }
        System.out.println( "adminUsername=" + encode(args[0]) );
        System.out.println( "adminPassword=" + encode(args[1]) );
    }

    public static String encode(String str) throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(str.getBytes());
        StringBuffer buf = new StringBuffer();
        for ( int i=0; i<b.length; i++ )    {
            int j = (b[i]<0) ? 256+b[i] : b[i];
            if ( j<10 ) buf.append("0");
            buf.append(Integer.toHexString(j));
        }
        return buf.toString();
    }

}

