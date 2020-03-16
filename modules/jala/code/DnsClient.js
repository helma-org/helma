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
 * @fileoverview Fields and methods of the jala.DnsClient class.
 */


// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * Jala dependencies
 */
app.addRepository(getProperty("jala.dir", "modules/jala") + 
                  "/lib/javadns.jar");

/**
 * Constructs a new DnsClient object.
 * @class This is a wrapper around the Dns Client by wonderly.org
 * providing methods for querying Dns servers. For more information
 * about the Java DNS client visit
 * <a href="https://javadns.dev.java.net/">https://javadns.dev.java.net/</a>.
 * Please mind that the nameserver specified must accept queries on port
 * 53 TCP (the Java DNS client used doesn't support UDP nameserver queries),
 * and that reverse lookups are not supported.
 * @param {String} nameServer IP-Address or FQDN of nameserver to query
 * @constructor
 */
jala.DnsClient = function(nameServer) {
   /**
    * Contains the IP Adress/FQDN of the name server to query.
    * @type String
    */
   this.nameServer = nameServer;

   if (!this.nameServer) {
      throw "jala.DnsClient: missing nameserver argument";
   } else {
      // test if required javadns library is available
      try {
         var clazz = java.lang.Class.forName("org.wonderly.net.dns.Query",
                                             false, app.getClassLoader())
      } catch (e) {
         throw "jala.DnsClient requires JavaDNS.jar"
               + " in lib/ext or application directory "
               + "[https://javadns.dev.java.net/]";
      }
   }

   return this;
};

/** @ignore */
jala.DnsClient.PKG = Packages.org.wonderly.net.dns;

/**
 * The "A" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_A = jala.DnsClient.PKG.Question.TYPE_A;

/**
 * The "CNAME" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_CNAME = jala.DnsClient.PKG.Question.TYPE_CNAME;

/**
 * The "MX" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_MX = jala.DnsClient.PKG.Question.TYPE_MX;

/**
 * The "NS" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_NS = jala.DnsClient.PKG.Question.TYPE_NS;

/**
 * The "PTR" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_PTR = jala.DnsClient.PKG.Question.TYPE_PTR;

/**
 * The "SOA" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_SOA = jala.DnsClient.PKG.Question.TYPE_SOA;

/**
 * The "TXT" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_TXT = jala.DnsClient.PKG.Question.TYPE_TXT;

/**
 * The "WKS" record/query type.
 * @type Number
 * @final
 */
jala.DnsClient.TYPE_WKS = jala.DnsClient.PKG.Question.TYPE_WKS;

/**
 * Queries the nameserver for a specific domain
 * and the given type of record.
 * @param {String} dName The domain name to query for
 * @param {Number} queryType The type of records to retrieve
 * @returns The records retrieved from the nameserver
 * @type org.wonderly.net.dns.RR
 */
jala.DnsClient.prototype.query = function(dName, queryType) {
   if (dName == null) {
      throw new Error("no domain-name to query for");
   }
   if (queryType == null) {
      queryType = jala.DnsClient.TYPE_A;
   }
   // construct the question for querying the nameserver
   var question = new jala.DnsClient.PKG.Question(dName,
                  queryType,
                  jala.DnsClient.PKG.Question.CLASS_IN);
   // construct the query
   var query = new jala.DnsClient.PKG.Query(question);
   // run the query
   query.runQuery(this.nameServer);
   // wrap the records received in instances of jala.DnsClient.Record
   var answers = query.getAnswers();
   var arr = [];
   for (var i=0;i<answers.length;i++) {
      arr[i] = new jala.DnsClient.Record(answers[i]);
   }
   return arr;
};

/**
 * Convenience method to query for the MX-records
 * of the domain passed as argument.
 * @param {String} dName The domain name to query for
 * @returns The records retrieved from the nameserver
 * @type org.wonderly.net.dns.RR
 */
jala.DnsClient.prototype.queryMailHost = function (dName) {
   return this.query(dName, this.TYPE_MX);
};


/** @ignore */
jala.DnsClient.toString = function() {
   return "[jala.DnsClient]";
};


/** @ignore */
jala.DnsClient.prototype.toString = function() {
   return "[jala.DnsClient (" + this.nameServer + ")]";
};

/**
 * Constructs a new instance of jala.DnsClient.Record.
 * @class Instances of this class wrap record data as received
 * from the nameserver.
 * @param {org.wonderly.net.dns.RR} data The data as received from
 * the nameserver
 * @returns A newly constructed Record instance
 * @constructor
 */
jala.DnsClient.Record = function(data) {
   /**
    * The type of the nameserver record represented by this Answer instance.
    * @type Number
    * @see #TYPE_A
    * @see #TYPE_CNAME
    * @see #TYPE_HINFO
    * @see #TYPE_MX
    * @see #TYPE_NS
    * @see #TYPE_PTR
    * @see #TYPE_SOA
    * @see #TYPE_TXT
    * @see #TYPE_WKS
    */
   this.type = data.getType();

   /**
    * The name of the host. This will only be set for records
    * of type A, AAAA and NS.
    * @type String
    * @see #TYPE_A
    * @see #TYPE_AAAA
    * @see #TYPE_NS
    */
   this.host = null;

   /**
    * The IP address of the host. This will only be set for records
    * of type A and AAAA
    * @type String
    * @see #TYPE_A
    * @see #TYPE_AAAA
    */
   this.ipAddress = null;

   /**
    * The CNAME of this record. This will only be set for records
    * of type CNAME
    * @type String
    * @see #TYPE_CNAME
    */
   this.cname = null;

   /**
    * The name of the mail exchanging server. This is only set for
    * records of type MX
    * @type String
    * @see #TYPE_MX
    */
   this.mx = null;

   /**
    * The email address responsible for a name server. This property
    * will only be set for records of type SOA
    * @type String
    * @see #TYPE_SOA
    */
   this.email = null;

   /**
    * Descriptive text as received from the nameserver. This is only
    * set for records of type TXT
    * @type String
    * @see #TYPE_TXT
    */
   this.text = null;

   /**
    * Returns the wrapped nameserver record data
    * @returns The wrapped data
    * @type org.wonderly.net.dns.RR
    */
   this.getData = function() {
      return data;
   };

   /**
    * Main constructor body
    */
   switch (data.getClass()) {
      case jala.DnsClient.PKG.ARR:
      case jala.DnsClient.PKG.AAAARR:
         this.host = data.getHost();
         this.ipAddress = data.getIPAddress();
         break;
      case jala.DnsClient.PKG.NSRR:
         this.host = data.getHost();
         break;
      case jala.DnsClient.PKG.CNAMERR:
         this.cname = data.getCName();
         break;
      case jala.DnsClient.PKG.MXRR:
         this.mx = data.getExchanger();
         break;
      case jala.DnsClient.PKG.SOARR:
         this.host = data.getNSHost();
         this.email = data.getResponsibleEmail();
         break;
      case jala.DnsClient.PKG.TXTRR:
         this.text = data.getText();
         break;
      default:
         break;
   }

   return this;
};

/** @ignore */
jala.DnsClient.Record.prototype.toString = function() {
   return "[jala.DnsClient.Record]";
};
