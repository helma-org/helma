//
// Jala Project [http://opensvn.csie.org/traccgi/jala]
//
// Copyright 2004 ORF Online und Teletext GmbH
//
// Licensed under the Apache License, Version 2.0 (the ``License'');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an ``AS IS'' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Revision$
// $LastChangedBy$
// $LastChangedDate$
// $HeadURL$
//


/**
 * @fileoverview Fields and methods of the jala.ImageFilter class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Constructs a new ImageFilter object
 * @class  This class provides several image manipulating
 * methods. Most of this filter library is based on filters created
 * by Janne Kipinä for JAlbum. For more information have a look
 * at http://www.ratol.fi/~jakipina/java/
 * @param {Object} img Either
 <ul>
 <li>an instance of helma.image.ImageWrapper</li>
 <li>the path to the image file as String</li>
 <li>an instance of helma.File representing the image file</li>
 <li>an instance of java.io.File representing the image file</li>
 </ul>
 * @constructor
 */
jala.ImageFilter = function(img) {
   /**
    * The buffered image to work on
    * @type java.awt.image.BufferedImage
    * @private
    */
   var bi;

   /**
    * Perfoms a gaussian operation (unsharp masking or blurring)
    * on the image using the kernelFactory passed as argument
    * @param {Number} radius The radius
    * @param {Number} amount The amount
    * @param {Function} kernelFactory Factory method to call for building the kernel
    * @private
    */
   var gaussianOp = function(radius, amount, kernelFactory) {
      var DEFAULT_RADIUS = 2;
      var MINIMUM_RADIUS = 1;
      var MAXIMUM_RADIUS = 10;
      var DEFAULT_AMOUNT = 15;
      var MINIMUM_AMOUNT = 1;
      var MAXIMUM_AMOUNT = 100;
   
      // correct arguments if necessary
      if (isNaN(radius = Math.min(Math.max(radius, MINIMUM_RADIUS), MAXIMUM_RADIUS)))
         radius = DEFAULT_RADIUS;
      if (isNaN(amount = Math.min(Math.max(amount, MINIMUM_AMOUNT), MAXIMUM_AMOUNT)))
         amount = DEFAULT_AMOUNT;

      if ((bi.getWidth() < bi.getHeight()) && (radius > bi.getWidth())) {
         radius = bi.getWidth();
      } else if ((bi.getHeight() < bi.getWidth()) && (radius > bi.getHeight())) {
         radius = bi.getHeight();
      }
      
      var size = (radius * 2) + 1;
      var deviation = amount / 20;
      var elements = kernelFactory(size, deviation);
      var large = jala.ImageFilter.getEnlargedImageWithMirroring(bi, radius);
      var resultImg = new java.awt.image.BufferedImage(large.getWidth(), large.getHeight(), large.getType());
      var kernel = new java.awt.image.Kernel(size, size, elements);
      var cop = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
      cop.filter(large, resultImg);
      // replace the wrapped buffered image with the modified one
      bi = resultImg.getSubimage(radius, radius, bi.getWidth(), bi.getHeight());
      return;
   };
   
   /**
    * Sharpens the image using a plain sharpening kernel.
    * @param {Number} amount The amount of sharpening to apply
    */
   this.sharpen = function(amount) {
      var DEFAULT = 20;
      var MINIMUM = 1;
      var MAXIMUM = 100;
      // correct argument if necessary
      if (isNaN(Math.min(Math.max(amount, MINIMUM), MAXIMUM)))
         amount = DEFAULT;
      var sharpened = new java.awt.image.BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
      var kernel = new java.awt.image.Kernel(3, 3, jala.ImageFilter.getSharpeningKernel(amount));
      var cop = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
      cop.filter(bi, sharpened);
      bi = sharpened;
      return;
   };
   
   /**
    * Performs an unsharp mask operation on the image
    * @param {Number} radius The radius
    * @param {Number} amount The amount
    */
   this.unsharpMask = function(radius, amount) {
      gaussianOp(radius, amount, jala.ImageFilter.getUnsharpMaskKernel);
      return;
   };

   /**
    * Performs a gaussian blur operation on the image
    * @param {Number} radius The radius
    * @param {Number} amount The amount
    */
   this.gaussianBlur = function(radius, amount) {
      gaussianOp(radius, amount, jala.ImageFilter.getGaussianBlurKernel);
      return;
   };
   

   /**
    * Returns the image that has been worked on
    * @return An instance of helma.image.ImageWrapper
    * @type helma.image.ImageWrapper
    */
   this.getImage = function() {
      var generator = Packages.helma.image.ImageGenerator.getInstance();
      return new Packages.helma.image.ImageWrapper(bi,
                    bi.getWidth(),
                    bi.getHeight(),
                    generator);
   };

   /**
    * Returns the wrapped image as byte array, to use eg. in conjunction
    * with res.writeBinary()
    * @returns The wrapped image as byte array
    * @type byte[]
    */
   this.getBytes = function() {
      var outStream = new java.io.ByteArrayOutputStream();
      Packages.javax.imageio.ImageIO.write(bi, "jpeg", outStream);
      var bytes = outStream.toByteArray();
      outStream.close();
      return bytes;
   };

   /**
    * constructor body
    * @ignore
    */
   if (arguments.length == 0 || img == null) {
      throw "jala.ImageFilter: insufficient arguments";
   } else if (img instanceof Packages.helma.image.ImageWrapper) {
      bi = img.getBufferedImage();
   } else {
      if (typeof(img) == "string") {
         var inStream = new java.io.FileInputStream(new java.io.File(img));
      } else {
         var inStream = new java.io.FileInputStream(img);
      }
      var decoder =  Packages.com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(inStream);
      bi = decoder.decodeAsBufferedImage();
   }

   return this;
};

/** @ignore */
jala.ImageFilter.prototype.toString = function() {
   return "[jala.ImageFilter]";
};

/**
 * Transforms an image into a bigger one while mirroring the edges
 * This method is used to apply the filtering up to the edges
 * of an image (otherwise the image would keep an unmodified
 * border).
 * @param {java.awt.image.BufferedImage} bi The buffered image to transform
 * @param {Number} size The size of the border area
 * @returns The transformed image
 * @type java.awt.image.BufferedImage
 * @private
 */
jala.ImageFilter.getEnlargedImageWithMirroring = function(bi, size) {

   var doFlip = function(bi, sx, sy, dist) {
      var out = new java.awt.image.BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
      var transform = java.awt.geom.AffineTransform.getScaleInstance(sx, sy);
      (sx < sy) ? transform.translate(-dist, 0) :  transform.translate(0, -dist);
      var atop = new java.awt.image.AffineTransformOp(transform,
          java.awt.image.AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
      out = atop["filter(java.awt.image.BufferedImage,java.awt.image.BufferedImage)"](bi, null);
      return out;
   }

   var doHorizontalFlip = function(bi) {
      return doFlip(bi, -1, 1, bi.getWidth());
   }

   var doVerticalFlip = function(bi) {
      return doFlip(bi, 1, -1, bi.getHeight());
   }

   var width = bi.getWidth() + 2 * size;
   var height = bi.getHeight() + 2 * size;
   var out = new java.awt.image.BufferedImage(width, height, bi.getType());
   var g = out.createGraphics();
   // due to method overloading exactly define the method to be called
   var func = "drawImage(java.awt.Image,int,int,java.awt.image.ImageObserver)";
   g[func](bi, size, size, null);
   
   var part;
   //top-left corner
   part = bi.getSubimage(0, 0, size, size);
   part = doHorizontalFlip(part);
   part = doVerticalFlip(part);
   g[func](part, 0, 0, null);
   //top-right corner
   part = bi.getSubimage(bi.getWidth()-size, 0, size, size);
   part = doHorizontalFlip(part);
   part = doVerticalFlip(part);
   g[func](part, width-size, 0, null);
   //bottom-left corner
   part = bi.getSubimage(0, bi.getHeight()-size, size, size);
   part = doHorizontalFlip(part);
   part = doVerticalFlip(part);
   g[func](part, 0, height-size, null);
   //bottom-right corner
   part = bi.getSubimage(bi.getWidth()-size, bi.getHeight()-size, size, size);
   part = doHorizontalFlip(part);
   part = doVerticalFlip(part);
   g[func](part, width-size, height-size, null);
   //left border
   part = bi.getSubimage(0, 0, size, bi.getHeight());
   part = doHorizontalFlip(part);
   g[func](part, 0, size, null);
   //right border
   part = bi.getSubimage(bi.getWidth()-size, 0, size, bi.getHeight());
   part = doHorizontalFlip(part);
   g[func](part, width-size, size, null);
   //top border
   part = bi.getSubimage(0, 0, bi.getWidth(), size);
   part = doVerticalFlip(part);
   g[func](part, size, 0, null);
   //bottom border
   part = bi.getSubimage(0, bi.getHeight()-size, bi.getWidth(), size);
   part = doVerticalFlip(part);
   g[func](part, size, height-size, null);
   return out;
};

/**
 * Factory method for a gaussian blur kernel
 * @returns The gaussian blur kernel
 * @param {Number} size The size of the kernel
 * @param {Number} deviation The deviation to use
 * @returns The gaussian blur kernel
 * @type float[]
 * @private
 */
jala.ImageFilter.getGaussianBlurKernel = function(size, deviation) {
   var nominator = 2 * deviation * deviation;
   var kernel = java.lang.reflect.Array.newInstance(java.lang.Float.TYPE, size*size);
   var center = (size - 1) / 2;
   var limit = size - 1;
   var xx, yy;
   var sum = 0;
   var value = 0;
   for (var y=0; y<size; y++) {
      for (var x=0; x<size; x++) {
         if ((y <= center) && (x <= center)) {
            if (x >= y) {
               //calculate new value
               xx = center - x;
               yy = center - y;
               value = Math.exp(-(xx*xx + yy*yy) / nominator);
               kernel[(y*size)+x] = value;
               sum += value;
            } else {
               //copy existing value
               value = kernel[(x*size)+y];
               kernel[(y*size)+x] = value;
               sum += value;
            }
         } else {
            xx = x;
            yy = y;
            if (yy > center)
               yy = limit - yy;
            if (xx > center)
               xx = limit - xx;
            value = kernel[(yy*size)+xx];
            kernel[(y*size)+x] = value;
            sum += value;
         }
      }
   }
   for (var i=0; i<kernel.length; i++) {
      kernel[i] = kernel[i] / sum;
   }
   return kernel;
};

/**
 * Factory method for an unsharp mask kernel
 * @param {Number} size The size of the kernel
 * @param {Number} deviation The deviation to use
 * @returns The unsharp mask kernel
 * @type float[]
 * @private
 */
jala.ImageFilter.getUnsharpMaskKernel = function(size, deviation) {
   var elements = jala.ImageFilter.getGaussianBlurKernel(size, deviation);
   var center = ((size * size) - 1) / 2;
   elements[center] = 0;
   var sum = 0;
   for (var i=0; i<elements.length; i++) {
      sum += elements[i];
      elements[i] = -elements[i];
   }
   elements[center] = sum + 1;
   return elements;
};

/**
 * Factory method for a sharpening kernel
 * @param {Number} amount The amount of sharpening to use
 * @return The sharpening kernel
 * @type float[]
 * @private
 */
jala.ImageFilter.getSharpeningKernel = function(amount) {
   var kernel = java.lang.reflect.Array.newInstance(java.lang.Float.TYPE, 9);
   var corner = 0;
   var side = amount / -50;
   var center = (side * -4.0) + (corner * -4.0) + 1.0;
   kernel[0] = kernel[2] = kernel[6] = kernel[8] = corner;
   kernel[1] = kernel[3] = kernel[5] = kernel[7] = side;
   kernel[4] = center;
   return kernel;
};

