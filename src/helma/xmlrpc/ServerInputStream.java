package helma.xmlrpc;

import java.io.*;

// This class is borrowed from Apache JServ
class ServerInputStream extends InputStream {
    // bytes remaining to be read from the input stream. This is
    // initialized from CONTENT_LENGTH (or getContentLength()).
    // This is used in order to correctly return a -1 when all the
    // data POSTed was read. If this is left to -1, content length is
    // assumed as unknown and the standard InputStream methods will be used
    long available = -1;
    long markedAvailable;

    private BufferedInputStream in;

    public ServerInputStream(BufferedInputStream in, int available) {
        this.in = in;
        this.available = available;
    }

    public int read() throws IOException {
        if (available > 0) {
            available--;
            return in.read();
        } else if (available == -1)
	return in.read ();
        return -1;
    }

    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (available > 0) {
            if (len > available) {
                // shrink len
                len = (int) available;
            }
            int read = in.read(b, off, len);
            if (read != -1) {
                available -= read;
            } else {
                available = -1;
            }
            return read;
        } else if (available == -1)
            return in.read (b, off, len);
        return -1;
    }

    public long skip(long n) throws IOException {
        long skip = in.skip(n);
        if (available > 0)
            available -= skip;
        return skip;
    }

    public void mark (int readlimit) {
        in.mark (readlimit);
        markedAvailable = available;
    }

    public void reset () throws IOException {
        in.reset ();
        available = markedAvailable;
    }

    public boolean markSupported () {
        return true;
    }

}
