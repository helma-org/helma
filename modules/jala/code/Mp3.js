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
 * @fileoverview Fields and methods of the jala.audio package.
 */

// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}

// Load java libraries
(function() {
   var jalaDir = getProperty("jala.dir", "modules/jala");
   // JavaMusicTag (org.farng.mp3.*)
   app.addRepository(jalaDir + "/lib/jid3lib-0.5.4.jar");
   // Mp3Info (de.ueberdosis.mp3info.*, required for parseDuration)
   app.addRepository(jalaDir + "/lib/id3-1.6.0d9.jar");
})();

// Resolve HelmaLib dependencies
app.addRepository("modules/helma/File.js");

/**
 * Constructs a new jala.Mp3 wrapper and
 * parses the header data of the MP3 file.
 * The standard fields for a tag are accessible
 * as properties of the new object.
 *
 * @class This is a class representing an MP3 file
 * providing methods to access its metadata.
 *
 * @param {String|File} file The mp3 file to be parsed, either as
 * path string or as any kind of file object
 *
 * @constructor
 */
jala.Mp3 = function(file) {

   // check and normalize file argument
   if (!file) {
      throw "jala.Mp3: missing argument";
   } else {
      file = new helma.File(file);
   }

   try {
      var clazz = java.lang.Class.forName("org.farng.mp3.MP3File",
                                          false, app.getClassLoader())
   } catch (e) {
      throw "jala.Mp3 requires jid3lib-0.5.4.jar"
            + " in lib/ext or modules/jala/lib directory "
            + "[http://javamusictag.sourceforge.net/]";
   }

   if (file.getLength() < 128) {
      throw "file too short to be an MP3 file (< 128 bytes)";
   }
   try {
      var mp3File = new Packages.org.farng.mp3.MP3File(file.getAbsolutePath());
   } catch (e) {
      throw "error parsing mp3 file: " + e.toString();
   }

   /**
    * Returns a helma.File reference to the wrapped file.
    * @type helma.File
    */
   this.getFile = function() {
      return file;
   };

   /**
    * Returns the underlying java object
    * @type org.farng.mp3.MP3File
    */
   this.getJavaObject = function() {
      return mp3File;
   };


   // map to remember tag objects
   var tagObjects = {};

   if (mp3File.hasID3v1Tag()) {
      tagObjects[jala.Mp3.Id3v1] = new jala.Mp3.Id3v1(this);
   }
   
   if (mp3File.hasID3v2Tag()) {
      tagObjects[jala.Mp3.Id3v2] = new jala.Mp3.Id3v2(this);
   }

   /**
    * This method creates a new tag object, attaches it
    * to the file (thereby replacing an existing tag of
    * this type) and returns it. Type is specified using 
    * the class name in jala.Mp3.*. If a second 
    * argument is provided, its values are copied into 
    * the new tag.
    *
    * @param {Object} tagClass
    * @param {Object} tagObject optional tag whose standard
    *        properties are copied to the new tag.
    * @type Object
    */
   this.createTag = function(tagClass, tagObject) {

      this.removeTag(tagClass);
      tagObjects[tagClass] = new tagClass(this);
      // we use zero as default value for empty track numbers.
      // this is the same behaviour as with winamp and tag&rename.
      tagObjects[tagClass].setTrackNumber("0");

      if (tagObject) {
         tagObjects[tagClass].copyFrom(tagObject);
      }
      return tagObjects[tagClass];
   };

   /**
    * Returns a tag object, type is specified using the class name
    * in jala.Mp3.*.
    * @type Object
    */
   this.getTag = function(tagClass) {
      return tagObjects[tagClass];
   };

   /**
    * Tells if the file contains a certain tag, type is specified
    * using the class name in jala.Mp3.*
    */
   this.hasTag = function(tagClass) {
      return (tagObjects[tagClass]) ? true : false;
   };


   // field to remember a v2 tag that has to be deleted from the file in save()
   var v2JavaTagToDelete = null;

   /**
    * Removes a tag from the file, type is specified using the
    * class name in jala.Mp3.*
    */
   this.removeTag = function(tagClass) {
      if (!tagObjects[tagClass]) {
         return;
      }

      // remember v2 tag here to explicitly delete it from
      // the audio file if save() is called ...
      // this is a workaround for a bug in JavaMusicTag!
      v2JavaTagToDelete = tagObjects[tagClass].getJavaObject();

      tagObjects[tagClass].removeFromAudio();
      tagObjects[tagClass] = null;
      return;
   };


   /**
    * Writes changed metadata back to the source file or to a new file. 
    * @param {String|helma.File} outFile (optional) save the modified file
    *                       to a different file
    * @returns true on success, false if the file contains tags that cannot be saved (Id3v2_2).
    * @type Boolean
    */
   this.save = function(outFile) {
      var tagOptions = Packages.org.farng.mp3.TagOptionSingleton.getInstance();
      // (robert) this appearently fixes the problem that Windows Media Player cannot play files
      // anymore if the size of an Id3v2 tag changed
      tagOptions.setId3v2PaddingCopyTag(false);
      // turn off saving of backup-files:
      tagOptions.setOriginalSavedAfterAdjustingID3v2Padding(false);
  
      if (v2JavaTagToDelete) {
         // this is a workaround for a bug in JavaMusicTag:
         // MP3File.save() just tries to delete an ID3v2_4 tag,
         // but omits 2_3 or 2_2 tags. To be on the safe side
         // we have to explicitly remove the deleted v2 tag.
         var raf = new java.io.RandomAccessFile(mp3File.getMp3file(), "rw");
         v2JavaTagToDelete["delete"](raf);
         v2JavaTagToDelete = null;
         raf.close();
      }

      if(tagObjects[jala.Mp3.Id3v2] && tagObjects[jala.Mp3.Id3v2].getSubtype() == 2) {
         app.log("Error in jala.Mp3#save: Can't save a tag of version Id3v2_2. Please remove the tag and add a new Id3v2 tag of sub type 3 or 4!");
         return false;
      }
   
      if(outFile) {
         var outFile = new helma.File(outFile);
         // MP3File.save(file) only saves the tags!
         // Thus, we make a hardcopy first.
         file.hardCopy(outFile);
      } else {
         outFile = file;
      }
      mp3File.save(outFile,
         Packages.org.farng.mp3.TagConstant.MP3_FILE_SAVE_OVERWRITE
      );
      return true;
   };



   // flag to remember if mp3 header has been read
   var mp3HeaderRead = false;   

   /**
    * Makes sure that the mp3 header is read only once
    * This takes a few milliseconds, so we only do it when a
    * function that depends on header data is called.
    * @private
    */
   this.readMp3Header = function() {
      if (!mp3HeaderRead) {
         mp3File.seekMP3Frame();
         mp3HeaderRead = true;
      }
      return;
   };


   /**   @type String   */
   this.album;

   /**   @type String   */
   this.artist;

   /**   @type String   */
   this.comment;

   /**   @type String   */
   this.genre;

   /**   @type String   */
   this.title;

   /**   @type String   */
   this.trackNumber;

   /**   @type String   */
   this.year;

   return this;
};

// define getter for standard fields:
try {
   jala.Mp3.prototype.__defineGetter__("album", function() {   return this.getField("album");   });
   jala.Mp3.prototype.__defineGetter__("artist", function() {   return this.getField("artist");   });
   jala.Mp3.prototype.__defineGetter__("comment", function() {   return this.getField("comment");   });
   jala.Mp3.prototype.__defineGetter__("genre", function() {   return this.getField("genre");   });
   jala.Mp3.prototype.__defineGetter__("title", function() {   return this.getField("title");   });
   jala.Mp3.prototype.__defineGetter__("trackNumber", function() {   return this.getField("trackNumber");   });
   jala.Mp3.prototype.__defineGetter__("year", function() {   return this.getField("year");   });
} catch (e) {
   // older helma versions can't handle __defineGetter__
}


/**
 * Array defining valid genres in ID3v1
 * @type Array
 * @final
 */
jala.Mp3.GENRES = ["Blues", "Classic Rock", "Country", "Dance", "Disco",
   "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other",
   "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative",
   "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient",
   "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical",
   "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise",
   "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop",
   "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial",
   "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy",
   "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle",
   "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave",
   "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka",
   "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock",
   "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic",
   "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock",
   "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus",
   "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera",
   "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove",
   "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad",
   "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo",
   "Acapella", "Euro-House", "Dance Hall"];


/**
 * Array defining mp3 modes.
 * @type Array
 * @final
 */
jala.Mp3.MODES = ["Stereo", "Joint stereo", "Dual channel", "Mono"];


/**
 * Array defining valid text encodings. Note: UTF-8 is valid for v2.4 only.
 * UTF-16 with BOM doesn't work with Winamp etc - use UTF-16BE instead!
 * The index position within the array defines the number used in the mp3 file.
 * @type Array
 * @final
 */
jala.Mp3.TEXT_ENCODINGS = ["ISO-8859-1", "UTF-16", "UTF-16BE", "UTF-8"];


/**
 * Array defining valid picture types. Note: Most image tagged files come with
 * one picture of picture type null!
 * The index position within the array defines the number used in the mp3 file.
 * @type Array
 * @final
 */
jala.Mp3.PICTURE_TYPES = ["Other", "32x32 pixels 'file icon' (PNG only)",
   "Other file icon", "Cover (front)", "Cover (back)", "Leaflet page",
   "Media (e.g. label side of CD)", "Lead artist/lead performer/soloist",
   "Artist/performer", "Conductor", "Band/Orchestra", "Composer",
   "Lyricist/text writer", "Recording Location", "During recording",
   "During performance", "Movie/video screen capture", "A bright coloured fish",
   "Illustration", "Band/artist logotype", "Publisher/Studio logotype"];


/**
 * Maps the name of the standard fields to frame ids in the different versions
 * of ID3v2.
 * @type Object
 * @private
 * @final
 */
jala.Mp3.FIELD_MAPPING = {
   "album":       ["", "", "TALB", "TALB", "TALB"],
   "artist":      ["", "", "TPE1", "TPE1", "TPE1"],
   "comment":     ["", "", "COMM", "COMM", "COMM"],
   "genre":       ["", "", "TCON", "TCON", "TCON"],
   "title":       ["", "", "TIT2", "TIT2", "TIT2"],
   "subtitle":    ["", "", "TIT3", "TIT3", "TIT3"],
   "trackNumber": ["", "", "TRCK", "TRCK", "TRCK"],
   "year":        ["", "", "TYER", "TYER", "TDRC"],
   "author":      ["", "", "TCOM", "TCOM", "TCOM"],
   "copyright":   ["", "", "TCOP", "TCOP", "TCOP"],
   "url":         ["", "", "WXXX", "WXXX", "WXXX"],
   "image":       ["", "", "APIC", "APIC", "APIC"]
};


/**
 * Helper method to copy the standard fields from one tag
 * to another
 * @param {Object} src  object with setter methods for fields album, artist,
 *                      comment, title, trackNumber, genre and year.
 * @param {Object} dest object with getter methods for fields album, artist,
 *                      comment, title, trackNumber, genre and year.
 * @returns changed object
 * @type Object
 * @private
 */
jala.Mp3.copyFields = function(src, dest) {
   dest.setAlbum(src.getAlbum());
   dest.setArtist(src.getArtist());
   dest.setComment(src.getComment());
   dest.setTitle(src.getTitle());
   dest.setTrackNumber(src.getTrackNumber());
   dest.setGenre(src.getGenre());
   dest.setYear(src.getYear());
   return dest;
};


/**
 * Helper function to handle arguments that may either be a
 * number or an object that matches a value in an array.
 * In the first case the number itself is returned, in the latter
 * case the index position within the array is returned.
 * @param {Number|Object} arg argument as number or object
 * @param {Array} values Array of objects.
 * @returns The number the argument represents
 * @type Number
 * @private
 */
jala.Mp3.normalizeArg = function(arg, values, defaultValue) {
   if (arg == null) {
      return defaultValue;
   } else if (!isNaN(arg)) {
      return parseInt(arg);
   } else {
      var idx = values.indexOf(arg);
      if (idx > 0) {
         return idx;
      }
   }
   return null;
};


/**
 * The audio length of the file in seconds at best estimate
 * from the file info (method returns immediately).
 * This method calculates based on the bitrate. Therefore it
 * has to produce wrong results for files encoded with variable
 * bitrate (vbr). For these files parseDuration() can be used.
 * @returns length in seconds
 * @type Number
 * @see #parseDuration
 */
jala.Mp3.prototype.getDuration = function() {
   var bitrate = this.getBitRate();
   if (bitrate != 0) {
      return Math.round(this.getSize() / (bitrate * 1000 / 8));
   }
   return 0;
};


/**
 * Parses the audio file to extract the precise duration of the audio.
 * The upside is that it works fine for files with variable bitrates.
 * The downside is that this action may take a few seconds depending on
 * the size of the audio file.
 * @returns length in seconds
 * @type Number
 * @see #getDuration
 */
jala.Mp3.prototype.parseDuration = function() {
   try {
      Packages.de.ueberdosis.util.OutputCtr.setLevel(0);    // turn off debug output
      var reader = Packages.de.ueberdosis.mp3info.ID3Reader(this.getFile().getAbsolutePath());
      var tag = reader.getExtendedID3Tag();
      return tag.getRuntime();
   } catch (e) {
      throw "jala.Mp3#parseDuration requires id3-1.6.0d9.jar"
      + " in lib/ext or modules/jala/lib directory "
      + "[http://sourceforge.net/projects/mp3info/]";
   }
};


/**
 * Returns the file size in bytes.
 * @type Number
 */
jala.Mp3.prototype.getSize = function() {
   return this.getFile().getLength();
};


/**
 * Returns the bit rate the file was encoded with.
 * @type Number
 */
jala.Mp3.prototype.getBitRate = function() {
   this.readMp3Header()
   return this.getJavaObject().getBitRate();
};


/**
 * Returns the channel mode the file was encoded with.
 * @type String
 */
jala.Mp3.prototype.getChannelMode = function() {
   this.readMp3Header()
   return jala.Mp3.MODES[this.getJavaObject().getMode()];
};


/**
 * Returns the frequency the file was encoded with.
 * @type Number
 */
jala.Mp3.prototype.getFrequency = function() {
   this.readMp3Header()
   return this.getJavaObject().getFrequency();
};


/**
 * Returns true if the file is (or seems to be) encoded with
 * variable bit rate. FIXME: The current implementation returned
 * true for all test files.
 * @type Boolean
 */
jala.Mp3.prototype.isVariableBitRate = function() {
   this.readMp3Header()
   return this.getJavaObject().isVariableBitRate();
};


/**
 * Returns the information for a field from the tags: At first the ID3v2
 * tag is checked. If it isn't present or doesn't contain the field,
 * the ID3v1 tag is checked.
 * @type {String}
 * @private
 */
jala.Mp3.prototype.getField = function(fieldName) {
   var funcName = "get" + fieldName.charAt(0).toUpperCase() + fieldName.substring(1);
   var tag, value;
   var getValue = function() {
      if (tag[funcName] != null && tag[funcName] instanceof Function) {
         return tag[funcName]();
      }
      return null;
   };

   if ((tag = this.getV2Tag()) != null && (value = getValue()) != null) {
      return value;
   }
   if ((tag = this.getV1Tag()) != null && (value = getValue()) != null) {
      return value;
   }
   return null;
};

/**
 * Sets the value of the field with the given name to the value specified,
 * in both ID3v1 and ID3v2 tags, but only if the appropriate setter method
 * exists.
 * @param {String} fieldName The name of the field to set
 * @param {String} value The value of the field
 * @private
 */
jala.Mp3.prototype.setField = function(fieldName, value) {
   if (value != null) {
      var funcName = "set" + fieldName.charAt(0).toUpperCase() + fieldName.substring(1);
      var setValue = function(tag) {
         if (tag[funcName] != null && tag[funcName] instanceof Function) {
            tag[funcName](value);
         }
         return;
      };
   
      setValue(this.getV2Tag() || this.createV2Tag());
      setValue(this.getV1Tag() || this.createV1Tag());
   }
   return;
};


/**
 * If the file doesn't contain an ID3v1 tag, this method
 * creates a new ID3v1 tag object, attaches it to the file
 * and returns it. If a second argument is provided, its
 * values are copied into the new tag.
 *
 * @param {Object} tagObject optional tag whose standard
 *        properties are copied to the new tag.
 * @type jala.Mp3.Id3v1
 */
jala.Mp3.prototype.createV1Tag = function(tagObject) {
   return this.createTag(jala.Mp3.Id3v1, tagObject);
};


/**
 * If the file doesn't contain an ID3v2 tag, this method
 * creates a new ID3v2 tag object, attaches it to the file
 * and returns it. If a second argument is provided, its
 * values are copied into the new tag.
 *
 * @param {Object} tagObject optional tag whose standard
 *        properties are copied to the new tag.
 * @type jala.Mp3.Id3v2
 */
jala.Mp3.prototype.createV2Tag = function(tagObject) {
   return this.createTag(jala.Mp3.Id3v2, tagObject);
};


/**
 * @type jala.Mp3.Id3v1
 */
jala.Mp3.prototype.getV1Tag = function() {
   return this.getTag(jala.Mp3.Id3v1);
};


/**
 * @type jala.Mp3.Id3v2
 */
jala.Mp3.prototype.getV2Tag = function() {
   return this.getTag(jala.Mp3.Id3v2);
};


/**
 * Returns true if the file contains a ID3v1 tag.
 * @type Boolean
 */
jala.Mp3.prototype.hasV1Tag = function() {
   return this.hasTag(jala.Mp3.Id3v1);
};


/**
 * Returns true if the file contains a ID3v2 tag.
 * @type Boolean
 */
jala.Mp3.prototype.hasV2Tag = function() {
   return this.hasTag(jala.Mp3.Id3v2);
};


/**
 * Removes the ID3v1 tag from the file.
 */
jala.Mp3.prototype.removeV1Tag = function() {
   this.removeTag(jala.Mp3.Id3v1);
};


/**
 * Removes the ID3v2 tag from the file.
 */
jala.Mp3.prototype.removeV2Tag = function() {
   return this.removeTag(jala.Mp3.Id3v2);
};


/** @ignore */
jala.Mp3.prototype.toString = function() {
   return "[jala.Mp3 " + this.getFile() + "]";
};

/**
 * Returns a plain JavaScript object containing the values of
 * all fields stored in either the Id3 V1 or V2 tag
 * @returns An object containing the values of all fields
 */
jala.Mp3.prototype.getMetadata = function() {
   var result = {};
   // generic metadata values
   result.size = this.getSize();
   result.isVariableBitRate = this.isVariableBitRate();
   result.bitrate = this.getBitRate();
   result.frequency = this.getFrequency();
   result.channelMode = this.getChannelMode();
   result.duration = this.parseDuration();
   // Id3 tag values
   var fields = [
      "title",
      "subtitle",
      "author",
      "url",
      "trackNumber",
      "year",
      "album",
      "artist",
      "comment",
      "genre",
      "copyright",
   ];
   var fieldName;
   for (var i=0; i<fields.length; i++) {
      fieldName = fields[i];
      result[fieldName] = this.getField(fieldName);
   }
   return result;
};


/**
 * Stores the metadata passed as argument in the ID2 v1 and v2 tags
 * of the wrapped MP3 file.
 * @param {Object} metadata An object containing the fields to set
 * and their values.
 */
jala.Mp3.prototype.setMetadata = function(metadata) {
   for (var propName in metadata) {
      this.setField(propName, metadata[propName]);
   }
   return;
};


/**
 * Constructs a new Id3v1 tag from an Mp3 file
 * @param {jala.Mp3} mp3File
 * @class This class represents an Id3v1 tag.
 * @constructor
 */
jala.Mp3.Id3v1 = function(audioObj) {

   var tag = audioObj.getJavaObject().getID3v1Tag();
   if (!tag) {
      tag = new Packages.org.farng.mp3.id3.ID3v1_1();
      audioObj.getJavaObject().setID3v1Tag(tag);
   }

   /**
    * Returns the wrapper for the underlying audio file.
    * @type jala.Mp3
    */
   this.getAudio = function() {
      return audioObj;
   };

   /**
    * Returns the java representation of the tag,
    * class depends on the actual library used.
    * @type org.farng.mp3.id3.AbstractID3v1
    */
   this.getJavaObject = function() {
      return tag;
   };   

   /**
    * Removes the tag from the audio file and
    * nulls out the wrapper.
    * @private
    */
   this.removeFromAudio = function() {
      audioObj.getJavaObject()["setID3v1Tag(org.farng.mp3.id3.ID3v1)"](null);
      tag = null;
      audioObj = null;
      return;
   };

   return this;
};


/**
 * Copies standard fields from another tag.
 * @param {Object} src object with getter methods for fields album, artist,
 *                     comment, title, trackNumber, genre and year.
 */
jala.Mp3.Id3v1.prototype.copyFrom = function(tag) {
   jala.Mp3.copyFields(tag, this);
   return;
};


/**
 * Returns the album information of the tag.
 * @returns string containing album name
 * @type String
 */
jala.Mp3.Id3v1.prototype.getAlbum = function() {
   return this.getJavaObject().getAlbumTitle();
};


/**
 * Returns the artist information of the tag.
 * @returns string containing artist name
 * @type String
 */
jala.Mp3.Id3v1.prototype.getArtist = function() {
   return this.getJavaObject().getLeadArtist();
};


/**
 * Returns the comment information of the tag.
 * @returns string containing comment
 * @type String
 */
jala.Mp3.Id3v1.prototype.getComment = function() {
   return this.getJavaObject().getSongComment();
};


/**
 * Returns the title information of the tag.
 * @returns string containing title
 * @type String
 */
jala.Mp3.Id3v1.prototype.getTitle = function() {
   return this.getJavaObject().getSongTitle();
};


/**
 * Returns the track number information of the tag.
 * @returns string representing track number or null 
 * if tag doesn't contain a track number.
 * @type String
 */
jala.Mp3.Id3v1.prototype.getTrackNumber = function() {
   try {
      return this.getJavaObject().getTrackNumberOnAlbum();
   } catch (e) {
      // track number only exists in Id3v1.1, for
      // Id3v1 getTrackNumberOnAlbum throws an exception.
      return null;
   }
};


/**
 * Returns the genre information of the tag.
 * @returns string containing genre name
 * @type String
 */
jala.Mp3.Id3v1.prototype.getGenre = function() {
   var genre = this.getJavaObject().getGenre();
   return jala.Mp3.GENRES[genre];
};


/**
 * Returns the year information of the tag.
 * @returns string representing year
 * @type String
 */
jala.Mp3.Id3v1.prototype.getYear = function() {
   return this.getJavaObject().getYearReleased();
};


/**
 * This method could be used to retrieve an arbitrary field
 * of the underlying tag. For Id3v1 tags all information
 * is available through getter and setter methods, so this
 * implementation always returns null.
 * @param {String} id
 * @returns null
 */
jala.Mp3.Id3v1.prototype.getTextContent = function(id) {
   return null;
}


/**
 * Sets the album information.
 * @param {String} album
 */
jala.Mp3.Id3v1.prototype.setAlbum = function(album) {
   this.getJavaObject().setAlbumTitle(album);
   return;
};


/**
 * Sets the artist information.
 * @param {String} artist
 */
jala.Mp3.Id3v1.prototype.setArtist = function(artist) {
   this.getJavaObject().setLeadArtist(artist);
   return;
};


/**
 * Sets the comment
 * @param {String} comment
 */
jala.Mp3.Id3v1.prototype.setComment = function(comment) {
   this.getJavaObject().setSongComment(comment);
   return;
};


/**
 * Sets the title information
 * @param {String} title
 */
jala.Mp3.Id3v1.prototype.setTitle = function(title) {
   this.getJavaObject().setSongTitle(title);
   return;
};


/**
 * Sets the track number information.
 * @param {Number} trackNumber
 */
jala.Mp3.Id3v1.prototype.setTrackNumber = function(trackNumber) {
   if (!trackNumber || isNaN(trackNumber)) {
      // default value for empty track numbers in v1 is zero.
      trackNumber = 0;
   }
   this.getJavaObject().setTrackNumberOnAlbum(trackNumber);
   return;
};


/**
 * Sets the genre information. A list of genre names that are valid
 * for ID3v1 tags is located in jala.Mp3.GENRES.
 * @param {String} genre
 */
jala.Mp3.Id3v1.prototype.setGenre = function(genre) {
   var genreByte = new java.lang.Long(jala.Mp3.GENRES.indexOf(genre));
   this.getJavaObject().setSongGenre(genreByte);
   return;
};


/**
 * Sets the year information.
 * @param {Number} year
 */
jala.Mp3.Id3v1.prototype.setYear = function(year) {
   this.getJavaObject().setYearReleased(year);
   return;
};


/**
 * This method could be used to set an arbitrary field
 * of the underlying tag. For Id3v1 tags all information
 * is available through getter and setter methods, so this
 * implementation does nothing.
 * @param {String} id
 * @param {String} value
 */
jala.Mp3.Id3v1.prototype.setTextContent = function(id, val)  {
};


/** @ignore */
jala.Mp3.Id3v1.toString = function() {
   return "[jala.Mp3.Id3v1]";
};

/** @ignore */
jala.Mp3.Id3v1.prototype.toString = jala.Mp3.Id3v1.toString;






/**
 * Constructs a new Id3v2 tag from an Mp3 file
 * @param {jala.Mp3} mp3File
 * @class This class represents an Id3v2 tag.
 * @constructor
 */
jala.Mp3.Id3v2 = function(audioObj) {

   var tag = audioObj.getJavaObject().getID3v2Tag();
   if (!tag) {
      tag = new Packages.org.farng.mp3.id3.ID3v2_3();
      audioObj.getJavaObject().setID3v2Tag(tag);
   }

   /**
    * Returns the wrapper for the underlying audio file.
    * @type jala.Mp3
    */
   this.getAudio = function() {
      return audioObj;
   };

   /**
    * returns the java representation of the tag,
    * class depends on the actual library used.
    * @type org.farng.mp3.id3.AbstractID3v2
    */
   this.getJavaObject = function() {
      return tag;
   };
   
   /**
    * Removes the tag from the audio file and
    * nulls out the wrapper.
    */
   this.removeFromAudio = function() {
      audioObj.getJavaObject()["setID3v2Tag(org.farng.mp3.id3.AbstractID3v2)"](null);
      tag = null;
      audioObj = null;
      return;
   };


   // default encoding = ISO 8859-1
   var textEncoding = new java.lang.Long(0);

   /**
    * sets the text encoding used when creating new frames
    * (the encoding type of old frames can't be changed with
    * JavaMusicTag)
    * @param {Number|String} encType the new encoding type
    *       as number or string
    * @see jala.Mp3#TEXT_ENCODINGS
    */
   this.setTextEncoding = function(encType) {
      textEncoding = normalizeArg(encType, jala.Mp3.TEXT_ENCODINGS, new java.lang.Long(0));
      return;
   };

   /**
    * Returns the text encoding used when setting values.
    * @returns The text encoding
    * @type Number
    */
   this.getTextEncoding = function() {
      return textEncoding;
   };
   
   return this;
};


/**
 * Copies standard fields from another tag.
 * @param {Object} src object with getter methods for fields album, artist,
 *                     comment, title, trackNumber, genre and year.
 */
jala.Mp3.Id3v2.prototype.copyFrom = function(tag) {
   jala.Mp3.copyFields(tag, this);
};


/**
 * Helper method that constructs an identifier string from the
 * arguments array in which the arguments are separated by a 
 * character of the value 0 and then returns the frame for this
 * identifier string.
 * @param {String} idStr frame id (or for standard fields the 
 *         name from jala.Mp3.FIELD_MAPPING can be used)
 * @returns frame object
 * @type org.farng.mp3.id3.AbstractID3v2
 * @private
 */
jala.Mp3.Id3v2.prototype.getFrame = function(idStr) {
   var id = idStr;
   if (jala.Mp3.FIELD_MAPPING[idStr]) {
      id = jala.Mp3.FIELD_MAPPING[idStr][this.getSubtype()];
   }
   for (var i=1; i<arguments.length; i++) {
      id += java.lang.Character(0) + arguments[i];
   }
   return this.getJavaObject().getFrame(id);
};


/**
 * Encodes a string using the given encoding.
 * @param {String} str string to encode
 * @param {String} encoding encoding to use
 * @returns decoded string
 * @type String
 * @private
 */
jala.Mp3.Id3v2.prototype.encodeText = function(str, encoding) {
   if (!isNaN(encoding)) {
      // if encoding is the byte value -> get the correct encoding string from constant
      encoding = jala.Mp3.TEXT_ENCODINGS[encoding];
   }
   return new java.lang.String(new java.lang.String(str).getBytes(encoding));
};


/**
 * Decodes a string using the given encoding.
 * @param {String} str string to decode
 * @param {String} encoding encoding to use
 * @returns decoded string
 * @type String
 * @private
 */
jala.Mp3.Id3v2.prototype.decodeText = function(str, encoding) {
   if (!isNaN(encoding)) {
      // if encoding is the byte value -> get the correct encoding string from constant
      encoding = jala.Mp3.TEXT_ENCODINGS[encoding]
   }
   var rawStr = new java.lang.String(str);
   return "" + new java.lang.String(rawStr.getBytes(), encoding);
};


/**
 * This method can be used to retrieve an arbitrary text frame
 * of the underlying tag. For the list of valid identifiers
 * and their meaning see http://www.id3.org/
 * The identifiers vary across the sub versions of id3v2 tags,
 * use getSubtype to make sure you use the correct version.
 * @param {String} id Frame identifier according to Id3v2 specification
 *                   or shortcut as defined in jala.Mp3.FIELD_MAPPING.
 * @returns String contained in the frame
 * @type String
 * @see #getSubtype
 */
jala.Mp3.Id3v2.prototype.getTextContent = function(idStr) {
   var id = idStr;
   if (jala.Mp3.FIELD_MAPPING[idStr]) {
      id = jala.Mp3.FIELD_MAPPING[idStr][this.getSubtype()];
   }
   var frame = this.getJavaObject().getFrame(id);
   if (frame) {
      var body = frame.getBody();
      if (!(body instanceof Packages.org.farng.mp3.id3.FrameBodyUnsupported)) {
         return this.decodeText(body.getText(), body.getObject("Text Encoding"));
      }
   }
   return null;
}


/**
 * This method can be used to set an arbitrary field
 * of the underlying tag. For the list of valid identifiers
 * and their meaning see http://www.id3.org/
 * The identifiers vary across the sub versions of id3v2 tags,
 * use getSubtype to make sure you use the correct version.
 * @param {String} id Frame identifier according to Id3v2 specification
 * @param {String} value
 * @type String
 * @see #getSubtype
 */
jala.Mp3.Id3v2.prototype.setTextContent = function(idStr, val)  {
   var id = idStr;
   if (jala.Mp3.FIELD_MAPPING[idStr]) {
      id = jala.Mp3.FIELD_MAPPING[idStr][this.getSubtype()];
   }
   var frame = this.getJavaObject().getFrame(id);
   if (frame) {
      var body = frame.getBody();
      // frame already exists, use its encoding:
      body.setText(this.encodeText(val, body.getObject("Text Encoding")));
   } else {
      // new frame is created, use our own encoding:
      var body = new Packages.org.farng.mp3.id3["FrameBody" + id](
         this.getTextEncoding(), this.encodeText(val, this.getTextEncoding())
      );
      this.getJavaObject().setFrame(this.createFrameObject(body));
   }
   return;
};


/**
 * Creates a new frame object that fits to the tag version.
 * @param {org.farng.mp3.id3.AbstractID3v2FrameBody} body frame body object
 * @returns new frame object
 * @type org.farng.mp3.id.ID3v2_2
 * @private
 */
jala.Mp3.Id3v2.prototype.createFrameObject = function(body) {
   var subtype = this.getSubtype();
   if (subtype == 2) {
      return new Packages.org.farng.mp3.id3.ID3v2_2Frame(body);
   } else if (subtype == 3) {
      return new Packages.org.farng.mp3.id3.ID3v2_3Frame(body);
   } else if (subtype == 4 || subtype == 0) {
      return new Packages.org.farng.mp3.id3.ID3v2_4Frame(body);
   }
   return null;
};


/**
 * Returns the version number of this id3v2 (values 2 to 4 for id3v2.2 to id3v2.4)
 * @returns The version number of this Id3v2 tag
 * @type Number
 */
jala.Mp3.Id3v2.prototype.getSubtype = function() {
   // AbstractID3v2#getRevision() only works for newly constructed tag objects,
   // but not for tag objects that have been read from a file.
   // so we make a class comparison to find out the subtype:
   var obj = this.getJavaObject();
   if (obj instanceof Packages.org.farng.mp3.id3.ID3v2_4) {
      return 4;
   } else if (obj instanceof Packages.org.farng.mp3.id3.ID3v2_3) {
      return 3;
   } else if (obj instanceof Packages.org.farng.mp3.id3.ID3v2_2) {
      return 2;
   }
   return 0;
};


/**
 * Returns the album information of the tag.
 * @returns string containing album name
 * @type String
 */
jala.Mp3.Id3v2.prototype.getAlbum = function() {
   return this.getTextContent("album");
};


/**
 * Returns the artist information of the tag.
 * @returns string containing artist name
 * @type String
 */
jala.Mp3.Id3v2.prototype.getArtist = function() {
   return this.getTextContent("artist");
};


/**
 * Returns the comment information of the tag.
 * @returns string containing comment
 * @type String
 */
jala.Mp3.Id3v2.prototype.getComment = function() {
   var frame = this.getFrame("comment", "eng", "");
   if (frame) {
      var str = frame.getBody().getText();
      return this.decodeText(str, frame.getBody().getObject("Text Encoding"));
   }
   return null;
};


/**
 * Returns the title information of the tag.
 * @returns string containing title
 * @type String
 */
jala.Mp3.Id3v2.prototype.getTitle = function() {
   return this.getTextContent("title");
};


/**
 * Returns the subtitle information of the tag.
 * @returns string containing subtitle
 * @type String
 */
jala.Mp3.Id3v2.prototype.getSubtitle = function() {
   return this.getTextContent("subtitle");
};


/**
 * Returns the track number information of the tag.
 * @returns string representing track number
 * @type String
 */
jala.Mp3.Id3v2.prototype.getTrackNumber = function() {
   return this.getTextContent("trackNumber");
};


/**
 * Returns the genre information of the tag.
 * @returns string containing genre name
 * @type String
 */
jala.Mp3.Id3v2.prototype.getGenre = function() {
   return this.getTextContent("genre");
};


/**
 * Returns the year information of the tag.
 * @returns string representing year
 * @type String
 */
jala.Mp3.Id3v2.prototype.getYear = function() {
   return this.getTextContent("year");
};


/**
 * Returns the author information of the tag.
 * @returns string containing author information
 * @type String
 */
jala.Mp3.Id3v2.prototype.getAuthor = function() {
   return this.getTextContent("author");
};


/**
 * Returns the copyright information of the tag.
 * @returns The copyright information of the tag
 * @type String
 */
jala.Mp3.Id3v2.prototype.getCopyright = function() {
   return this.getTextContent("copyright");
};


/**
 * Returns the Url stored in this tag
 * @returns The url stored in this tag
 * @type String
 */
jala.Mp3.Id3v2.prototype.getUrl = function() {
   var frame = this.getFrame("url", "");
   if (frame) {
      return frame.getBody().getUrlLink();
   }
   return null;
};


/**
 * Sets the album information.
 * @param {String} album
 */
jala.Mp3.Id3v2.prototype.setAlbum = function(album) {
   this.setTextContent("album", album);
   return;
};


/**
 * Sets the artist information.
 * @param {String} artist
 */
jala.Mp3.Id3v2.prototype.setArtist = function(artist) {
   this.setTextContent("artist", artist);
   return;
};


/**
 * Sets the comment
 * @param {String} comment
 */
jala.Mp3.Id3v2.prototype.setComment = function(comment) {
   // comment (COMM) isn't a text frame. it supports the getText()
   // method but its constructor has a different signature.
   var frame = this.getFrame("comment", "eng", "");
   if (frame) {
      frame.getBody().setText(this.encodeText(comment, frame.getBody().getObject("Text Encoding")));
   } else {
      var body = new Packages.org.farng.mp3.id3.FrameBodyCOMM(
         this.getTextEncoding(), "eng", "", this.encodeText(comment, this.getTextEncoding())
      );
      this.getJavaObject().setFrame(this.createFrameObject(body));
   }
   return;
};


/**
 * Sets the title information
 * @param {String} title
 */
jala.Mp3.Id3v2.prototype.setTitle = function(title) {
   this.setTextContent("title", title);
   return;
};


/**
 * Sets the subtitle information
 * @param {String} title
 */
jala.Mp3.Id3v2.prototype.setSubtitle = function(title) {
   this.setTextContent("subtitle", title);
   return;
};


/**
 * Sets the track number information.
 * @param {Number} trackNumber
 */
jala.Mp3.Id3v2.prototype.setTrackNumber = function(trackNumber) {
   this.setTextContent("trackNumber", trackNumber);
   return;
};


/**
 * Sets the genre information. A list of genre names that are compatible
 * with ID3v1 tags is located in jala.Mp3.GENRES.
 * @param {String} genre
 */
jala.Mp3.Id3v2.prototype.setGenre = function(genre) {
   this.setTextContent("genre", genre);
   return;
};


/**
 * Sets the year information.
 * @param {Number} year
 */
jala.Mp3.Id3v2.prototype.setYear = function(year) {
   this.setTextContent("year", year);
   return;
};


/**
 * Sets the author information in this tag
 * @param {String} author The author information to set
 */
jala.Mp3.Id3v2.prototype.setAuthor = function(author) {
   this.setTextContent("author", author);
   return;
};


/**
 * Sets the copyright information in this tag
 * @param {String} copyright The copyright information to set
 */
jala.Mp3.Id3v2.prototype.setCopyright = function(copyright) {
   this.setTextContent("copyright", copyright);
   return;
};


/**
 * Stores the Url passed as argument in this tag.
 * @param {String} url The url to store in this tag
 * @param {String} desc An optiona description of the Url
 */
jala.Mp3.Id3v2.prototype.setUrl = function(url, desc) {
   var frame = this.getFrame("url", "");
   if (frame) {
      frame.getBody().setUrlLink(url);
   } else {
      var body = new Packages.org.farng.mp3.id3.FrameBodyWXXX(
         this.getTextEncoding(), desc, url
      );
      this.getJavaObject().setFrame(this.createFrameObject(body));
   }
   return;
};


/**
 * Extracts the image from the tag
 * @param {String} pictureType number describing picture type
 *                (default is 3, describing a front cover).
 * @returns image as mime object
 * @type helma.util.MimePart
 */
jala.Mp3.Id3v2.prototype.getImage = function(pictureType) {
   // FIXME: maybe add description to arguments of getFrame?
   // more testing needed...
   pictureType = jala.Mp3.normalizeArg(pictureType,
      jala.Mp3.PICTURE_TYPES, 3);

   var frame = this.getFrame("image", new java.lang.Character(pictureType));
   if (frame) {
      var body = frame.getBody();
      var mimeType = body.getObject("MIME Type");
      var imageType = mimeType.substring(6);
      var imageName = this.getAudio().getFile().getName().replace(/\.[^\.]+$/i, "") + "." + imageType;
      return new Packages.helma.util.MimePart(
         imageName,
         body.getObject("Picture Data"),
         mimeType
      );
   }
   return null;
};


/**
 * adds an image to the file.
 * @param {Number} pictureType number determining picture type
 * @param {String} mimeType mime type of image
 * @param {Array} byteArray image binary data
 * @param {String} desc optional description
 * @see jala.Mp3
 */
jala.Mp3.Id3v2.prototype.setImage = function(pictureType, mimeType, byteArray) {
   pictureType = jala.Mp3.normalizeArg(pictureType,
      jala.Mp3.PICTURE_TYPES, 3);

   var frame = this.getFrame("image", new java.lang.Character(pictureType));
   if (frame) {
      if (mimeType && byteArray) {
         // set new image data
         frame.getBody().setObject("MIME Type", mimeType);
         frame.getBody().setObject("Picture Data", byteArray);
      }
   } else {
      // add new image to tag
      var body = new Packages.org.farng.mp3.id3.FrameBodyAPIC(
         this.getTextEncoding(),
         mimeType,
         new java.lang.Long(pictureType),
         new java.lang.Character(pictureType),
         byteArray
      );
      this.getJavaObject().setFrame(this.createFrameObject(body));
   }
   return;
};


/** @ignore */
jala.Mp3.Id3v2.prototype.debug = function() {
   return "<pre>" + this.getJavaObject().toString() + "</pre>";
};


/** @ignore */
jala.Mp3.Id3v2.toString = function() {
   return "[jala.Mp3.Id3v2]";
};

/** @ignore */
jala.Mp3.Id3v2.prototype.toString = jala.Mp3.Id3v2.toString;


// FIXME: report bug in JavaMusicTag:
// if you delete a v2 tag and call save() JMT calls the delete method of an ID3v2_4 tag.
// this way a 2_2 or 2_3 tag in the file isn't found and not deleted.
// Mp3.save() has a workaround for this.

