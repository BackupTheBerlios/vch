package de.berlios.vch.http.filter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SniffingOutputStream extends FilterOutputStream {

    public SniffingOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        super.write(b);
    }

    int last4 = -1;
    int last3 = -1;
    int last2 = -1;
    int last1 = -1;
    
    @Override
    public void write(int b) throws IOException {
        super.write(b);
        
        System.err.println(b + "=" + ((char)b) + " written to stream");
        last4 = last3;
        last3 = last2;
        last2 = last1;
        last1 = b;
        if(last1 == 49 && last2 == 48 && last3 == 48 && last4 == 48 ) {
            Throwable t = new Throwable();
            t.printStackTrace();
        }
    }
}
