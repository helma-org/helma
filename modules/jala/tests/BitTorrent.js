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
 * A simple test of jala.BitTorrent.
 * FIXME: Needs resolution of issue #33
 */
var testBitTorrent = function() {
   var size = 1024 * 1024; // 1 meg
   var file = new java.io.File(jala.Test.getTestFile("1meg"));
   var fos = new java.io.FileOutputStream(file, false);
   var channel = fos.getChannel();
   var iterations = 0;
   while (channel.size() < size) {
      channel.write(java.nio.ByteBuffer.allocate(1024));
   }
   channel.close();
   fos.close();

   var torrent = new jala.BitTorrent(file);
   // Testing against file generated with BitTorrent.app (OS X)
   torrent.set("announce", "http://tracker.orf.at/announce");
   // S3 defines a multitracker list with a single tracker item
   //torrent.set("announce-list", [["http://tracker.amazonaws.com:6969/announce"]]);
   torrent.setCreationDate(new Date(2007, 1, 26, 14, 46, 44));
   torrent.save();
   file["delete"]();

   try {
      var torrentFile = torrent.getTorrentFile();
      var refFile = new helma.File(jala.Test.getTestFile("1meg.reference.torrent"));
      assertEqual(torrentFile.readAll().trim(), refFile.readAll().trim());
   } catch (x) {
      throw(x);
   } finally {
      torrentFile.remove();
   }
   return;
};
