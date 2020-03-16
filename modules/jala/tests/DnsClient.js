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


var dnsClient = new jala.DnsClient("68.12.16.25");
var result;

/**
 * Testing default mode (A records)
 */
var testAQuery = function() {
   result = dnsClient.query("nomatic.org");
   assertEqual(result.length, 1);
   assertEqual(result[0].ipAddress, "213.129.249.34");
   return;
};

/**
 * Testing SOA record queries
 */
var testSoaQuery = function() {
   result = dnsClient.query("nomatic.org", jala.DnsClient.TYPE_SOA);
   assertEqual(result.length, 1);
   assertEqual(result[0].email, "hostmaster.nomatic.org");
   return;
};

/**
 * Testing MX record queries
 */
var testMxQuery = function() {
   result = dnsClient.query("nomatic.org", jala.DnsClient.TYPE_MX);
   assertEqual(result.length, 1);
   assertEqual(result[0].mx, "grace.nomatic.org");
   return;
};

/**
 * Testing NS record queries
 */
var testNsQuery = function() {
   result = dnsClient.query("nomatic.org", jala.DnsClient.TYPE_NS);
   assertEqual(result.length, 3);
   // can't test single records as their order changes unpredictably
   return;
};
