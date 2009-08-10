/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

/*
 * The GIF encoding routines are based on the Acme libary
 *
 * The following changes and extensions were added by Juerg Lehni:
 * 
 * - encode now directly works on BufferedImage objects, the ImageEncoder
 *   and ImageConsumer oriented frameworks has been removed.
 * - Only BufferedImages with IndexColorModel are taken, so no more
 *   palette optimization with IntHashtable objects are necessary. If the
 *   BufferedImage is in wrong format, helma.image.ColorQuantizer is used to
 *   convert it into a index based image.
 * - This version is much less space consuming as it only takes one sample
 *   row of the rastered image at a time into memory during compression.
 * - The overall time for color reduction and gif compression should
 *   therefore be greatly reduced.
 *
 * Acme Disclaimer:
 *
 * Transparency handling and variable bit size courtesy of Jack Palevich.
 *
 * Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * Visit the ACME Labs Java page for up-to-date versions of this and other
 * fine Java utilities: http://www.acme.com/java/
 *
 * GifEncoder is adapted from ppmtogif, which is based on GIFENCOD by David
 * Rowley <mgardi@watdscu.waterloo.edu>.  Lempel-Zim compression
 * based on "compress".
 *
 */

package helma.image;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.DataOutput;
import java.io.IOException;

public class GIFEncoder {
    private boolean interlace = false;

    private int width, height;
    private Raster raster;
    // DataOutput is used for compatibility with ImageIO (see helma.image.imageio.gif.GIFImageWriter)
    // otherwise, OutputStream would make much more sense here:
    private DataOutput out;

    private int curx, cury;
    private int countdown;
    private int pass;
    private int[] row;

    public void encode(BufferedImage bi, DataOutput out) throws IOException {
        encode(bi, out, false, null);
    }

    public void encode(BufferedImage bi, DataOutput out, boolean interlace,
        String comment) throws IOException {
        this.out = out;
        this.interlace = interlace;

        // make sure it's index colors:
        if (bi.getType() != BufferedImage.TYPE_BYTE_INDEXED)
            bi = ColorQuantizer.quantizeImage(bi, 256, false, true);

        raster = bi.getRaster();

        width = bi.getWidth();
        height = bi.getHeight();

        int numPixels = width * height;

        IndexColorModel icm = (IndexColorModel) bi.getColorModel();
        int transparentIndex = icm.getTransparentPixel();
        int numColors = icm.getMapSize();

        // Figure out how many bits to use.
        int bitsPerPixel;
        if (numColors <= 2)
            bitsPerPixel = 1;
        else if (numColors <= 4)
            bitsPerPixel = 2;
        else if (numColors <= 16)
            bitsPerPixel = 4;
        else
            bitsPerPixel = 8;

        int initCodeSize;

        // Calculate number of bits we are expecting
        countdown = numPixels;

        // Indicate which pass we are on (if interlace)
        pass = 0;

        // The initial code size
        if (bitsPerPixel <= 1)
            initCodeSize = 2;
        else
            initCodeSize = bitsPerPixel;

        // Set up the current x and y position
        curx = 0;
        cury = 0;
        row = new int[width];

        // Write the Magic header
        writeString("GIF89a");

        // Write out the screen width and height
        writeWord(width);
        writeWord(height);

        // Indicate that there is a global colour map
        byte flags = (byte) 0x80; // Yes, there is a color map
        // OR in the resolution
        flags |= (byte) ((8 - 1) << 4);
        // Not sorted
        // OR in the Bits per Pixel
        flags |= (byte) ((bitsPerPixel - 1));
        // Write it out
        out.write(flags);

        // Write out the Background colour
        out.write((byte) 0);

        // Pixel aspect ratio - 1:1.
        //out.write((byte) 49);
        // Java's GIF reader currently has a bug, if the aspect ratio byte is
        // not zero it throws an ImageFormatException.  It doesn't know that
        // 49 means a 1:1 aspect ratio.  Well, whatever, zero works with all
        // the other decoders I've tried so it probably doesn't hurt.
        out.write((byte) 0);

        // Write out the Global Colour Map
        // Turn colors into colormap entries.
        int mapSize = 1 << bitsPerPixel;
        byte[] reds = new byte[mapSize], greens = new byte[mapSize], blues = new byte[mapSize];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);

        for (int i = 0; i < mapSize; ++i) {
            out.write(reds[i]);
            out.write(greens[i]);
            out.write(blues[i]);
        }

        // Write out extension for transparent colour index, if necessary.
        if (transparentIndex != -1) {
            out.write((byte) '!');
            out.write((byte) 0xf9);
            out.write((byte) 4);
            out.write((byte) 1);
            out.write((byte) 0);
            out.write((byte) 0);
            out.write((byte) transparentIndex);
            out.write((byte) 0);
        }

        // Write an Image separator
        out.write((byte) ',');

        // Write the Image header
        writeWord(0); // leftOfs
        writeWord(0); // topOfs
        writeWord(width);
        writeWord(height);

        // Write out whether or not the image is interlaced
        if (interlace)
            out.write((byte) 0x40);
        else
            out.write((byte) 0x00);

        // Write out the initial code size
        out.write((byte) initCodeSize);

        // Go and actually compress the data
        compress(initCodeSize + 1);

        // Write out a Zero-length packet (to end the series)
        out.write((byte) 0);

        // Write out the comment
        if (comment != null && comment.length() > 0) {
            out.write((byte) 0x21);
            out.write((byte) 0xFE);
            out.write((byte) comment.length());
            writeString(comment);
            out.write((byte) 0);
        }

        // Write the GIF file terminator
        out.write((byte) ';');
    }

    // Return the next pixel from the image
    int getNextPixel() throws IOException {
        if (countdown == 0)
            return -1;

        --countdown;

        if (curx == 0)
            row = raster.getSamples(0, cury, width, 1, 0, row);
        int index = row[curx];

        // Bump the current X position
        ++curx;

        // If we are at the end of a scan line, set curx back to the beginning
        // If we are interlaced, bump the cury to the appropriate spot,
        // otherwise, just increment it.
        if (curx == width) {
            curx = 0;

            if (!interlace) {
                ++cury;
            } else {
                switch (pass) {
                case 0:
                    cury += 8;
                    if (cury >= height) {
                        ++pass;
                        cury = 4;
                    }
                    break;

                case 1:
                    cury += 8;
                    if (cury >= height) {
                        ++pass;
                        cury = 2;
                    }
                    break;

                case 2:
                    cury += 4;
                    if (cury >= height) {
                        ++pass;
                        cury = 1;
                    }
                    break;

                case 3:
                    cury += 2;
                    break;
                }
            }
        }
        return index;
    }

    void writeString(String str) throws IOException {
        byte[] buf = str.getBytes();
        out.write(buf);
    }

    // Write out a word to the GIF file
    void writeWord(int w) throws IOException {
        out.write((byte) (w & 0xff));
        out.write((byte) ((w >> 8) & 0xff));
    }

    // GIFCOMPR.C       - GIF Image compression routines
    //
    // Lempel-Ziv compression based on 'compress'.  GIF modifications by
    // David Rowley (mgardi@watdcsu.waterloo.edu)

    // General DEFINEs

    static final int BITS = 12;

    static final int HASH_SIZE = 5003; // 80% occupancy

    // GIF Image compression - modified 'compress'
    //
    // Based on: compress.c - File compression ala IEEE Computer, June 1984.
    //
    // By Authors:  Spencer W. Thomas      (decvax!harpo!utah-cs!utah-gr!thomas)
    //              Jim McKie              (decvax!mcvax!jim)
    //              Steve Davies           (decvax!vax135!petsd!peora!srd)
    //              Ken Turkowski          (decvax!decwrl!turtlevax!ken)
    //              James A. Woods         (decvax!ihnp4!ames!jaw)
    //              Joe Orost              (decvax!vax135!petsd!joe)

    private int numBits; // number of bits/code
    private int maxBits = BITS; // user settable max # bits/code
    private int maxCode; // maximum code, given numBits
    private int maxMaxCode = 1 << BITS; // should NEVER generate this code

    final int getMaxCode(int numBits) {
        return (1 << numBits) - 1;
    }

    private int[] hashTable = new int[HASH_SIZE];
    private int[] codeTable = new int[HASH_SIZE];

    private int freeEntry = 0; // first unused entry

    // block compression parameters -- after all codes are used up,
    // and compression rate changes, start over.
    private boolean clearFlag = false;

    // Algorithm:  use open addressing double hashing (no chaining) on the
    // prefix code / next character combination.  We do a variant of Knuth's
    // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
    // secondary probe.  Here, the modular division first probe is gives way
    // to a faster exclusive-or manipulation.  Also do block compression with
    // an adaptive reset, whereby the code table is cleared when the compression
    // ratio decreases, but after the table fills.  The variable-length output
    // codes are re-sized at this point, and a special CLEAR code is generated
    // for the decompressor.  Late addition:  construct the table according to
    // file size for noticeable speed improvement on small files.  Please direct
    // questions about this implementation to ames!jaw.

    private int initBits;

    private int clearCode;
    private int EOFCode;

    void compress(int initBits) throws IOException {
        // Set up the globals:  initBits - initial number of bits
        this.initBits = initBits;

        // Set up the necessary values
        clearFlag = false;
        numBits = initBits;
        maxCode = getMaxCode(numBits);

        clearCode = 1 << (initBits - 1);
        EOFCode = clearCode + 1;
        freeEntry = clearCode + 2;

        charInit();

        int ent = getNextPixel();

        int hashShift = 0;
        for (int fcode = HASH_SIZE; fcode < 65536; fcode *= 2)
            ++hashShift;
        hashShift = 8 - hashShift; // set hash code range bound

        clearHash(); // clear hash table

        output(clearCode);

        int c;
        outerLoop: while ((c = getNextPixel()) != -1) {
            int fcode = (c << maxBits) + ent;
            int i = (c << hashShift) ^ ent; // xor hashing

            if (hashTable[i] == fcode) {
                ent = codeTable[i];
                continue;
            } else if (hashTable[i] >= 0) { // non-empty slot
                int disp = HASH_SIZE - i; // secondary hash (after G. Knott)
                if (i == 0)
                    disp = 1;
                do {
                    if ((i -= disp) < 0)
                        i += HASH_SIZE;

                    if (hashTable[i] == fcode) {
                        ent = codeTable[i];
                        continue outerLoop;
                    }
                } while (hashTable[i] >= 0);
            }
            output(ent);
            ent = c;
            if (freeEntry < maxMaxCode) {
                codeTable[i] = freeEntry++; // code -> hashtable
                hashTable[i] = fcode;
            } else
                clearBlock();
        }
        // Put out the final code.
        output(ent);
        output(EOFCode);
    }

    // output
    //
    // Output the given code.
    // Inputs:
    //      code:   A numBits-bit integer.  If == -1, then EOF.  This assumes
    //              that numBits =< wordsize - 1.
    // Outputs:
    //      Outputs code to the file.
    // Assumptions:
    //      Chars are 8 bits long.
    // Algorithm:
    //      Maintain a BITS character long buffer (so that 8 codes will
    // fit in it exactly).  Use the VAX insv instruction to insert each
    // code in turn.  When the buffer fills up empty it and start over.

    int curAccum = 0;
    int curBits = 0;

    int masks[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F,
            0x007F, 0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF,
            0x7FFF, 0xFFFF };

    void output(int code) throws IOException {
        curAccum &= masks[curBits];

        if (curBits > 0)
            curAccum |= (code << curBits);
        else
            curAccum = code;

        curBits += numBits;

        while (curBits >= 8) {
            charOut((byte) (curAccum & 0xff));
            curAccum >>= 8;
            curBits -= 8;
        }

        // If the next entry is going to be too big for the code size,
        // then increase it, if possible.
        if (freeEntry > maxCode || clearFlag) {
            if (clearFlag) {
                maxCode = getMaxCode(numBits = initBits);
                clearFlag = false;
            } else {
                ++numBits;
                if (numBits == maxBits)
                    maxCode = maxMaxCode;
                else
                    maxCode = getMaxCode(numBits);
            }
        }

        if (code == EOFCode) {
            // At EOF, write the rest of the buffer.
            while (curBits > 0) {
                charOut((byte) (curAccum & 0xff));
                curAccum >>= 8;
                curBits -= 8;
            }

            charFlush();
        }
    }

    // Clear out the hash table

    // table clear for block compress
    void clearBlock() throws IOException {
        clearHash();
        freeEntry = clearCode + 2;
        clearFlag = true;

        output(clearCode);
    }

    // reset code table
    void clearHash() {
        for (int i = 0; i < HASH_SIZE; ++i)
            hashTable[i] = -1;
    }

    // GIF Specific routines

    // Number of characters so far in this 'packet'
    private int a_count;

    // Set up the 'byte output' routine
    void charInit() {
        a_count = 0;
    }

    // Define the storage for the packet accumulator
    private byte[] accum = new byte[256];

    // Add a character to the end of the current packet, and if it is 254
    // characters, flush the packet to disk.
    void charOut(byte c) throws IOException {
        accum[a_count++] = c;
        if (a_count >= 254)
            charFlush();
    }

    // Flush the packet to disk, and reset the accumulator
    void charFlush() throws IOException {
        if (a_count > 0) {
            out.write(a_count);
            out.write(accum, 0, a_count);
            a_count = 0;
        }
    }
}