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

package helma.image;

import java.awt.image.*;

/*
 * @(#)Quantize.java    0.90 9/19/00 Adam Doppelt
 * 
 * Modifications by JŸrg Lehni:
 * 
 * - Support for alpha-channels.
 * - Returns a BufferedImage of TYPE_BYTE_INDEXED with a IndexColorModel.
 * - Dithering of images through helma.image.DiffusionFilterOp by setting
 *   the dither parameter to true.
 * - Support for a transparent color, which is correctly rendered by GIFEncoder.
 *   All pixels with alpha < 0x80 are converted to this color when the parameter
 *   alphaToBitmask is set to true.
 * - Removed the SQUARES lookup tables as multiplications of integer values
 *   shouldn't take more than one clock nowadays anyhow.
 */

/**
 * An efficient color quantization algorithm, adapted from the C++
 * implementation quantize.c in <a
 * href="http://www.imagemagick.org/">ImageMagick</a>. The pixels for
 * an image are placed into an oct tree. The oct tree is reduced in
 * size, and the pixels from the original image are reassigned to the
 * nodes in the reduced tree.<p>
 *
 * Here is the copyright notice from ImageMagick:
 * 
 * <pre>
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%  Permission is hereby granted, free of charge, to any person obtaining a    %
%  copy of this software and associated documentation files ("ImageMagick"),  %
%  to deal in ImageMagick without restriction, including without limitation   %
%  the rights to use, copy, modify, merge, publish, distribute, sublicense,   %
%  and/or sell copies of ImageMagick, and to permit persons to whom the       %
%  ImageMagick is furnished to do so, subject to the following conditions:    %
%                                                                             %
%  The above copyright notice and this permission notice shall be included in %
%  all copies or substantial portions of ImageMagick.                         %
%                                                                             %
%  The software is provided "as is", without warranty of any kind, express or %
%  implied, including but not limited to the warranties of merchantability,   %
%  fitness for a particular purpose and noninfringement.  In no event shall   %
%  E. I. du Pont de Nemours and Company be liable for any claim, damages or   %
%  other liability, whether in an action of contract, tort or otherwise,      %
%  arising from, out of or in connection with ImageMagick or the use or other %
%  dealings in ImageMagick.                                                   %
%                                                                             %
%  Except as contained in this notice, the name of the E. I. du Pont de       %
%  Nemours and Company shall not be used in advertising or otherwise to       %
%  promote the sale, use or other dealings in ImageMagick without prior       %
%  written authorization from the E. I. du Pont de Nemours and Company.       %
%                                                                             %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
</pre>
 *
 *
 * @version 0.90 19 Sep 2000
 * @author <a href="http://www.gurge.com/amd/">Adam Doppelt</a>
 */
public class Quantize {

/*
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                                                                             %
%                                                                             %
%                                                                             %
%           QQQ   U   U   AAA   N   N  TTTTT  IIIII   ZZZZZ  EEEEE            %
%          Q   Q  U   U  A   A  NN  N    T      I        ZZ  E                %
%          Q   Q  U   U  AAAAA  N N N    T      I      ZZZ   EEEEE            %
%          Q  QQ  U   U  A   A  N  NN    T      I     ZZ     E                %
%           QQQQ   UUU   A   A  N   N    T    IIIII   ZZZZZ  EEEEE            %
%                                                                             %
%                                                                             %
%              Reduce the Number of Unique Colors in an Image                 %
%                                                                             %
%                                                                             %
%                           Software Design                                   %
%                             John Cristy                                     %
%                              July 1992                                      %
%                                                                             %
%                                                                             %
%  Copyright 1998 E. I. du Pont de Nemours and Company                        %
%                                                                             %
%  Permission is hereby granted, free of charge, to any person obtaining a    %
%  copy of this software and associated documentation files ("ImageMagick"),  %
%  to deal in ImageMagick without restriction, including without limitation   %
%  the rights to use, copy, modify, merge, publish, distribute, sublicense,   %
%  and/or sell copies of ImageMagick, and to permit persons to whom the       %
%  ImageMagick is furnished to do so, subject to the following conditions:    %
%                                                                             %
%  The above copyright notice and this permission notice shall be included in %
%  all copies or substantial portions of ImageMagick.                         %
%                                                                             %
%  The software is provided "as is", without warranty of any kind, express or %
%  implied, including but not limited to the warranties of merchantability,   %
%  fitness for a particular purpose and noninfringement.  In no event shall   %
%  E. I. du Pont de Nemours and Company be liable for any claim, damages or   %
%  other liability, whether in an action of contract, tort or otherwise,      %
%  arising from, out of or in connection with ImageMagick or the use or other %
%  dealings in ImageMagick.                                                   %
%                                                                             %
%  Except as contained in this notice, the name of the E. I. du Pont de       %
%  Nemours and Company shall not be used in advertising or otherwise to       %
%  promote the sale, use or other dealings in ImageMagick without prior       %
%  written authorization from the E. I. du Pont de Nemours and Company.       %
%                                                                             %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%  Realism in computer graphics typically requires using 24 bits/pixel to
%  generate an image. Yet many graphic display devices do not contain
%  the amount of memory necessary to match the spatial and color
%  resolution of the human eye. The QUANTIZE program takes a 24 bit
%  image and reduces the number of colors so it can be displayed on
%  raster device with less bits per pixel. In most instances, the
%  quantized image closely resembles the original reference image.
%
%  A reduction of colors in an image is also desirable for image
%  transmission and real-time animation.
%
%  Function Quantize takes a standard RGB or monochrome images and quantizes
%  them down to some fixed number of colors.
%
%  For purposes of color allocation, an image is a set of n pixels, where
%  each pixel is a point in RGB space. RGB space is a 3-dimensional
%  vector space, and each pixel, pi, is defined by an ordered triple of
%  red, green, and blue coordinates, (ri, gi, bi).
%
%  Each primary color component (red, green, or blue) represents an
%  intensity which varies linearly from 0 to a maximum value, cmax, which
%  corresponds to full saturation of that color. Color allocation is
%  defined over a domain consisting of the cube in RGB space with
%  opposite vertices at (0,0,0) and (cmax,cmax,cmax). QUANTIZE requires
%  cmax = 255.
%
%  The algorithm maps this domain onto a tree in which each node
%  represents a cube within that domain. In the following discussion
%  these cubes are defined by the coordinate of two opposite vertices:
%  The vertex nearest the origin in RGB space and the vertex farthest
%  from the origin.
%
%  The tree's root node represents the the entire domain, (0,0,0) through
%  (cmax,cmax,cmax). Each lower level in the tree is generated by
%  subdividing one node's cube into eight smaller cubes of equal size.
%  This corresponds to bisecting the parent cube with planes passing
%  through the midpoints of each edge.
%
%  The basic algorithm operates in three phases: Classification,
%  Reduction, and Assignment. Classification builds a color
%  description tree for the image. Reduction collapses the tree until
%  the number it represents, at most, the number of colors desired in the
%  output image. Assignment defines the output image's color map and
%  sets each pixel's color by reclassification in the reduced tree.
%  Our goal is to minimize the numerical discrepancies between the original
%  colors and quantized colors (quantization error).
%
%  Classification begins by initializing a color description tree of
%  sufficient depth to represent each possible input color in a leaf.
%  However, it is impractical to generate a fully-formed color
%  description tree in the classification phase for realistic values of
%  cmax. If colors components in the input image are quantized to k-bit
%  precision, so that cmax= 2k-1, the tree would need k levels below the
%  root node to allow representing each possible input color in a leaf.
%  This becomes prohibitive because the tree's total number of nodes is
%  1 + sum(i=1,k,8k).
%
%  A complete tree would require 19,173,961 nodes for k = 8, cmax = 255.
%  Therefore, to avoid building a fully populated tree, QUANTIZE: (1)
%  Initializes data structures for nodes only as they are needed;  (2)
%  Chooses a maximum depth for the tree as a function of the desired
%  number of colors in the output image (currently log2(colorMap size)).
%
%  For each pixel in the input image, classification scans downward from
%  the root of the color description tree. At each level of the tree it
%  identifies the single node which represents a cube in RGB space
%  containing the pixel's color. It updates the following data for each
%  such node:
%
%    n1: Number of pixels whose color is contained in the RGB cube
%    which this node represents;
%
%    n2: Number of pixels whose color is not represented in a node at
%    lower depth in the tree;  initially,  n2 = 0 for all nodes except
%    leaves of the tree.
%
%    Sr, Sg, Sb: Sums of the red, green, and blue component values for
%    all pixels not classified at a lower depth. The combination of
%    these sums and n2  will ultimately characterize the mean color of a
%    set of pixels represented by this node.
%
%    E: The distance squared in RGB space between each pixel contained
%    within a node and the nodes' center. This represents the quantization
%    error for a node.
%
%  Reduction repeatedly prunes the tree until the number of nodes with
%  n2 > 0 is less than or equal to the maximum number of colors allowed
%  in the output image. On any given iteration over the tree, it selects
%  those nodes whose E count is minimal for pruning and merges their
%  color statistics upward. It uses a pruning threshold, Ep, to govern
%  node selection as follows:
%
%    Ep = 0
%    while number of nodes with (n2 > 0) > required maximum number of colors
%      prune all nodes such that E <= Ep
%      Set Ep to minimum E in remaining nodes
%
%  This has the effect of minimizing any quantization error when merging
%  two nodes together.
%
%  When a node to be pruned has offspring, the pruning procedure invokes
%  itself recursively in order to prune the tree from the leaves upward.
%  n2,  Sr, Sg,  and  Sb in a node being pruned are always added to the
%  corresponding data in that node's parent. This retains the pruned
%  node's color characteristics for later averaging.
%
%  For each node, n2 pixels exist for which that node represents the
%  smallest volume in RGB space containing those pixel's colors. When n2
%  > 0 the node will uniquely define a color in the output image. At the
%  beginning of reduction,  n2 = 0  for all nodes except a the leaves of
%  the tree which represent colors present in the input image.
%
%  The other pixel count, n1, indicates the total number of colors
%  within the cubic volume which the node represents. This includes n1 -
%  n2  pixels whose colors should be defined by nodes at a lower level in
%  the tree.
%
%  Assignment generates the output image from the pruned tree. The
%  outpu				t image consists of two parts: (1)  A color map, which is an
%  array of color descriptions (RGB triples) for each color present in
%  the output image;  (2)  A pixel array, which represents each pixel as
%  an index into the color map array.
%
%  First, the assignment phase makes one pass over the pruned color
%  description tree to establish the image's color map. For each node
%  with n2  > 0, it divides Sr, Sg, and Sb by n2 . This produces the
%  mean color of all pixels that classify no lower than this node. Each
%  of these colors becomes an entry in the color map.
%
%  Finally,  the assignment phase reclassifies each pixel in the pruned
%  tree to identify the deepest node containing the pixel's color. The
%  pixel's value in the pixel array becomes the index of this node's mean
%  color in the color map.
%
%  With the permission of USC Information Sciences Institute, 4676 Admiralty
%  Way, Marina del Rey, California  90292, this code was adapted from module
%  ALCOLS written by Paul Raveling.
%
%  The names of ISI and USC are not used in advertising or publicity
%  pertaining to distribution of the software without prior specific
%  written permission from ISI.
%
*/
    
    final static boolean QUICK = true;

    final static int MAX_RGB = 255;
    final static int MAX_NODES = 266817;
    final static int MAX_TREE_DEPTH = 8;
    final static int MAX_CHILDREN = 16;

    // these are precomputed in advance
    //    static int SQUARES[];
    static int SHIFT[];

    static {
        /*
         * SQUARES = new int[MAX_RGB + MAX_RGB + 1]; for (int i= -MAX_RGB; i <=
         * MAX_RGB; i++) { SQUARES[i + MAX_RGB] = i * i; }
         */

        SHIFT = new int[MAX_TREE_DEPTH + 1];
        for (int i = 0; i < MAX_TREE_DEPTH + 1; ++i) {
            SHIFT[i] = 1 << (15 - i);
        }
    }

    /**
     * Reduce the image to the given number of colors. The pixels are reduced in
     * place.
     * 
     * @return The new color palette.
     */
    public static BufferedImage process(BufferedImage source, int maxColors,
        boolean dither, boolean alphaToBitmask) {
        int type = source.getType();
        int[] pixels;
        // try to get the direct pixels of the BufferedImage
        // this works for images of type INT_RGB, INT_ARGB and INT_ARGB_PRE
        // for all others, a new array with rgb pixels is created!
        if (type == BufferedImage.TYPE_INT_RGB
            || type == BufferedImage.TYPE_INT_ARGB
            || type == BufferedImage.TYPE_INT_ARGB_PRE) {
            pixels = ((DataBufferInt) source.getRaster().getDataBuffer()).getData();
        } else {
            pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth());
        }
        Cube cube = new Cube(source, pixels, maxColors, dither, alphaToBitmask);
        cube.classification();
        cube.reduction();
        return cube.assignment();
    }

    static class Cube {
        BufferedImage source;
        int[] pixels;
        int maxColors;
        byte colorMap[][];

        Node root;
        int depth;

        boolean dither;
        boolean alphaToBitmask;
        boolean addTransparency;
        // firstColor is set to 1 when when addTransparency is true!
        int firstColor = 0;

        // counter for the number of colors in the cube. this gets
        // recalculated often.
        int numColors;

        // counter for the number of nodes in the tree
        int numNodes;

        Cube(BufferedImage source, int[] pixels, int maxColors, boolean dither,
            boolean alphaToBitmask) {
            this.source = source;
            this.pixels = pixels;
            this.maxColors = maxColors;
            this.dither = dither;
            this.alphaToBitmask = alphaToBitmask;

            int i = maxColors;
            // tree_depth = log maxColors
            //                 4
            for (depth = 1; i != 0; depth++) {
                i /= 4;
            }
            if (depth > 1) {
                --depth;
            }
            if (depth > MAX_TREE_DEPTH) {
                depth = MAX_TREE_DEPTH;
            } else if (depth < 2) {
                depth = 2;
            }

            root = new Node(this);
        }

        /*
         * Procedure Classification begins by initializing a color description
         * tree of sufficient depth to represent each possible input color in a
         * leaf. However, it is impractical to generate a fully-formed color
         * description tree in the classification phase for realistic values of
         * cmax. If colors components in the input image are quantized to k-bit
         * precision, so that cmax= 2k-1, the tree would need k levels below the
         * root node to allow representing each possible input color in a leaf.
         * This becomes prohibitive because the tree's total number of nodes is
         * 1 + sum(i=1,k,8k).
         * 
         * A complete tree would require 19,173,961 nodes for k = 8, cmax = 255.
         * Therefore, to avoid building a fully populated tree, QUANTIZE: (1)
         * Initializes data structures for nodes only as they are needed; (2)
         * Chooses a maximum depth for the tree as a function of the desired
         * number of colors in the output image (currently log2(colorMap size)).
         * 
         * For each pixel in the input image, classification scans downward from
         * the root of the color description tree. At each level of the tree it
         * identifies the single node which represents a cube in RGB space
         * containing It updates the following data for each such node:
         * 
         * numPixels : Number of pixels whose color is contained in the RGB cube
         * which this node represents;
         * 
         * unique : Number of pixels whose color is not represented in a node at
         * lower depth in the tree; initially, n2 = 0 for all nodes except
         * leaves of the tree.
         * 
         * totalRed/green/blue : Sums of the red, green, and blue component
         * values for all pixels not classified at a lower depth. The
         * combination of these sums and n2 will ultimately characterize the
         * mean color of a set of pixels represented by this node.
         */
        void classification() {
            addTransparency = false;
            firstColor = 0;
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel >> 0) & 0xff;
                int alpha = (pixel >> 24) & 0xff;
                if (alphaToBitmask)
                    alpha = alpha < 0x80 ? 0 : 0xff;

                if (alpha > 0) {
                    // a hard limit on the number of nodes in the tree
                    if (numNodes > MAX_NODES) {
                        //	System.out.println("pruning");
                        root.pruneLevel();
                        --depth;
                    }

                    // walk the tree to depth, increasing the
                    // numPixels count for each node
                    Node node = root;
                    for (int level = 1; level <= depth; ++level) {
                        int id = (((red > node.midRed ? 1 : 0) << 0)
                            | ((green > node.midGreen ? 1 : 0) << 1)
                            | ((blue > node.midBlue ? 1 : 0) << 2) | ((alpha > node.midAlpha ? 1
                            : 0) << 3));
                        if (node.children[id] == null) {
                            new Node(node, id, level);
                        }
                        node = node.children[id];
                        node.numPixels += SHIFT[level];
                    }

                    ++node.unique;
                    node.totalRed += red;
                    node.totalGreen += green;
                    node.totalBlue += blue;
                    node.totalAlpha += alpha;
                } else if (!addTransparency) {
                    addTransparency = true;
                    numColors++;
                    firstColor = 1; // start at 1 as 0 will be the transparent
                                    // color
                }
            }
        }

        /*
         * reduction repeatedly prunes the tree until the number of nodes with
         * unique > 0 is less than or equal to the maximum number of colors
         * allowed in the output image.
         * 
         * When a node to be pruned has offspring, the pruning procedure invokes
         * itself recursively in order to prune the tree from the leaves upward.
         * The statistics of the node being pruned are always added to the
         * corresponding data in that node's parent. This retains the pruned
         * node's color characteristics for later averaging.
         */
        void reduction() {
            int threshold = 1;
            while (numColors > maxColors) {
                numColors = firstColor;
                threshold = root.reduce(threshold, Integer.MAX_VALUE);
            }
        }

        /**
         * The result of a closest color search.
         */
        static class Search {
            int distance;
            int colorIndex;
        }

        /*
         * Procedure assignment generates the output image from the pruned tree.
         * The output image consists of two parts: (1) A color map, which is an
         * array of color descriptions (RGB triples) for each color present in
         * the output image; (2) A pixel array, which represents each pixel as
         * an index into the color map array.
         * 
         * First, the assignment phase makes one pass over the pruned color
         * description tree to establish the image's color map. For each node
         * with n2 > 0, it divides Sr, Sg, and Sb by n2. This produces the mean
         * color of all pixels that classify no lower than this node. Each of
         * these colors becomes an entry in the color map.
         * 
         * Finally, the assignment phase reclassifies each pixel in the pruned
         * tree to identify the deepest node containing the pixel's color. The
         * pixel's value in the pixel array becomes the index of this node's
         * mean color in the color map.
         */
        BufferedImage assignment() {
            colorMap = new byte[4][numColors];

            if (addTransparency) {
                // if a transparency color is added, firstColor was set to 1,
                // so color 0 can be used for this
                colorMap[0][0] = 0;
                colorMap[1][0] = 0;
                colorMap[2][0] = 0;
                colorMap[3][0] = 0;
            }
            numColors = firstColor;
            root.mapColors();

            // determine bit depth for palette
            int depth;
            for (depth = 1; depth <= 8; depth++)
                if ((1 << depth) >= numColors)
                    break;

            // create the right color model, depending on transparency settings:
            IndexColorModel icm;
            if (alphaToBitmask) {
                if (addTransparency)
                    icm = new IndexColorModel(depth, numColors, colorMap[0],
                        colorMap[1], colorMap[2], 0);
                else
                    icm = new IndexColorModel(depth, numColors, colorMap[0],
                        colorMap[1], colorMap[2]);
            } else {
                icm = new IndexColorModel(depth, numColors, colorMap[0],
                    colorMap[1], colorMap[2], colorMap[3]);
            }
            // create the indexed BufferedImage:
            BufferedImage dest = new BufferedImage(source.getWidth(),
                source.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, icm);

            boolean firstOut = true;
            if (dither)
                new DiffusionFilterOp().filter(source, dest);
            else {
                Search search = new Search();
                // convert to indexed color
                byte[] dst = ((DataBufferByte) dest.getRaster().getDataBuffer()).getData();

                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    int red = (pixel >> 16) & 0xff;
                    int green = (pixel >> 8) & 0xff;
                    int blue = (pixel >> 0) & 0xff;
                    int alpha = (pixel >> 24) & 0xff;

                    if (alphaToBitmask)
                        alpha = alpha < 128 ? 0 : 0xff;
                    
                    // this is super weird: on some systems, transparent pixels are
                    // not calculated correctly if the following line is taken out.
                    // the bug is very strange, isn't related to the code (compiler error?)
                    // but doesn't allways happen. as soon as it does, though, it doesn't
                    // seem to want to go away.
                    // This happened at various times on my two different debian systems
                    // and i never found out how to really fix it. the following line seems to
                    // prevent it from happening, but i wonder wether there's a better way
                    // to fix it. 
                    // it looks as if the command forces alpha to take on correct values.
                    // Until now I only knew of effects like that in quantum mechanics...
                    if (i == 0) Integer.toString(alpha);

                    if (alpha == 0 && addTransparency) {
                        dst[i] = 0; // transparency color is at 0
                    } else {
                        // walk the tree to find the cube containing that color
                        Node node = root;
                        for (;;) {
                            int id = (((red > node.midRed ? 1 : 0) << 0)
                                | ((green > node.midGreen ? 1 : 0) << 1)
                                | ((blue > node.midBlue ? 1 : 0) << 2) | ((alpha > node.midAlpha ? 1
                                : 0) << 3));
                            if (node.children[id] == null) {
                                break;
                            }
                            node = node.children[id];
                        }

                        if (QUICK) {
                            // if QUICK is set, just use that
                            // node. Strictly speaking, this isn't
                            // necessarily best match.
                            dst[i] = (byte) node.colorIndex;
                        } else {
                            // Find the closest color.
                            search.distance = Integer.MAX_VALUE;
                            node.parent.closestColor(red, green, blue, alpha,
                                search);
                            dst[i] = (byte) search.colorIndex;
                        }
                    }
                }
            }
            return dest;
        }

        /**
         * A single Node in the tree.
         */
        static class Node {
            Cube cube;

            // parent node
            Node parent;

            // children nodes
            Node children[];
            int numChildren;

            // our index within our parent
            int id;
            // our level within the tree
            int level;
            // our color midpoint
            int midRed;
            int midGreen;
            int midBlue;
            int midAlpha;

            // the pixel count for this node and all children
            int numPixels;

            // the pixel count for this node
            int unique;
            // the sum of all pixels contained in this node
            int totalRed;
            int totalGreen;
            int totalBlue;
            int totalAlpha;

            // used to build the colorMap
            int colorIndex;

            Node(Cube cube) {
                this.cube = cube;
                this.parent = this;
                this.children = new Node[MAX_CHILDREN];
                this.id = 0;
                this.level = 0;

                this.numPixels = Integer.MAX_VALUE;

                this.midRed = (MAX_RGB + 1) >> 1;
                this.midGreen = (MAX_RGB + 1) >> 1;
                this.midBlue = (MAX_RGB + 1) >> 1;
                this.midAlpha = (MAX_RGB + 1) >> 1;
            }

            Node(Node parent, int id, int level) {
                this.cube = parent.cube;
                this.parent = parent;
                this.children = new Node[MAX_CHILDREN];
                this.id = id;
                this.level = level;

                // add to the cube
                ++cube.numNodes;
                if (level == cube.depth) {
                    ++cube.numColors;
                }

                // add to the parent
                ++parent.numChildren;
                parent.children[id] = this;

                // figure out our midpoint
                int bi = (1 << (MAX_TREE_DEPTH - level)) >> 1;
                midRed = parent.midRed + ((id & 1) > 0 ? bi : -bi);
                midGreen = parent.midGreen + ((id & 2) > 0 ? bi : -bi);
                midBlue = parent.midBlue + ((id & 4) > 0 ? bi : -bi);
                midAlpha = parent.midAlpha + ((id & 8) > 0 ? bi : -bi);
            }

            /**
             * Remove this children node, and make sure our parent absorbs our
             * pixel statistics.
             */
            void pruneChild() {
                --parent.numChildren;
                parent.unique += unique;
                parent.totalRed += totalRed;
                parent.totalGreen += totalGreen;
                parent.totalBlue += totalBlue;
                parent.totalAlpha += totalAlpha;
                parent.children[id] = null;
                --cube.numNodes;
                cube = null;
                parent = null;
            }

            /**
             * Prune the lowest layer of the tree.
             */
            void pruneLevel() {
                if (numChildren != 0) {
                    for (int id = 0; id < MAX_CHILDREN; id++) {
                        if (children[id] != null) {
                            children[id].pruneLevel();
                        }
                    }
                }
                if (level == cube.depth) {
                    pruneChild();
                }
            }

            /**
             * Remove any nodes that have fewer than threshold pixels. Also, as
             * long as we're walking the tree: - figure out the color with the
             * fewest pixels - recalculate the total number of colors in the
             * tree
             */
            int reduce(int threshold, int nextThreshold) {
                if (numChildren != 0) {
                    for (int id = 0; id < MAX_CHILDREN; id++) {
                        if (children[id] != null) {
                            nextThreshold = children[id].reduce(threshold,
                                nextThreshold);
                        }
                    }
                }
                if (numPixels <= threshold) {
                    pruneChild();
                } else {
                    if (unique != 0) {
                        cube.numColors++;
                    }
                    if (numPixels < nextThreshold) {
                        nextThreshold = numPixels;
                    }
                }
                return nextThreshold;
            }

            /*
             * mapColors traverses the color cube tree and notes each colorMap
             * entry. A colorMap entry is any node in the color cube tree where
             * the number of unique colors is not zero.
             */
            void mapColors() {
                if (numChildren != 0) {
                    for (int id = 0; id < MAX_CHILDREN; id++) {
                        if (children[id] != null) {
                            children[id].mapColors();
                        }
                    }
                }
                if (unique != 0) {
                    int add = unique >> 1;
                    cube.colorMap[0][cube.numColors] = (byte) ((totalRed + add) / unique);
                    cube.colorMap[1][cube.numColors] = (byte) ((totalGreen + add) / unique);
                    cube.colorMap[2][cube.numColors] = (byte) ((totalBlue + add) / unique);
                    cube.colorMap[3][cube.numColors] = (byte) ((totalAlpha + add) / unique);
                    colorIndex = cube.numColors++;
                }
            }

            /*
             * ClosestColor traverses the color cube tree at a particular node
             * and determines which colorMap entry best represents the input
             * color.
             */
            void closestColor(int red, int green, int blue, int alpha,
                Search search) {
                if (numChildren != 0) {
                    for (int id = 0; id < MAX_CHILDREN; id++) {
                        if (children[id] != null) {
                            children[id].closestColor(red, green, blue, alpha,
                                search);
                        }
                    }
                }

                if (unique != 0) {
                    int distance = distance(
                        cube.colorMap[0][colorIndex] & 0xff,
                        cube.colorMap[1][colorIndex] & 0xff,
                        cube.colorMap[2][colorIndex] & 0xff,
                        cube.colorMap[3][colorIndex] & 0xff, red, green, blue,
                        alpha);
                    if (distance < search.distance) {
                        search.distance = distance;
                        search.colorIndex = colorIndex;
                    }
                }
            }

            /**
             * Figure out the distance between this node and som color.
             */
            final static int distance(int r1, int g1, int b1, int a1, int r2,
                int g2, int b2, int a2) {
                int da = a1 - a2;
                int dr = r1 - r2;
                int dg = g1 - g2;
                int db = b1 - b2;

                return da * da + dr * dr + dg * dg + db * db;
//                return (SQUARES[r1 - r2 + MAX_RGB] +
//                     SQUARES[g1 - g2 + MAX_RGB] +
//                     SQUARES[b1 - b2 + MAX_RGB] +
//                     SQUARES[a1 - a2 + MAX_RGB]);
            }

            public String toString() {
                StringBuffer buf = new StringBuffer();
                if (parent == this) {
                    buf.append("root");
                } else {
                    buf.append("node");
                }
                buf.append(' ');
                buf.append(level);
                buf.append(" [");
                buf.append(midRed);
                buf.append(',');
                buf.append(midGreen);
                buf.append(',');
                buf.append(midBlue);
                buf.append(',');
                buf.append(midAlpha);
                buf.append(']');
                return new String(buf);
            }
        }
    }
}