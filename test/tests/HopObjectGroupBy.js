tests = [
   "testGroupByAddRemoveCommit",
   "testGroupByAddRemoveNoCommit",
   "testGroupOrder",
   "testGroupTransient"
];

// todo: run with different sizes
var size = 1234;

function setup() {
    for (var i = 0; i < size; i++) {
        var org = new Organisation();
        org.name = "Organisation " + i;
        org.country = "C" + i;
        root.organisations.add(org);
    }
    res.commit();
}

function testGroupByAddRemoveCommit() {
   var countryCount = root.organisationsByCountry.count();
   var org = new Organisation();
   org.country = "AT" + Math.random();
   org.name = "Helma" + Math.random();
   root.organisations.add(org);
   res.commit(); // Commit Transaction

   var country = root.organisationsByCountry.get(org.country);
   assertEqual(countryCount + 1, root.organisationsByCountry.count());
   assertNotNull(country);
   assertEqual(country._prototype, "Country");
   assertEqual(country._id, org.country);

   org.remove();
   res.commit(); // Commit Transaction

   assertNull(root.organisationsByCountry.get(org.country));
   assertEqual(countryCount, root.organisationsByCountry.count());
}

function testGroupByAddRemoveNoCommit() {

   var countryCount = root.organisationsByCountry.count();
   var org = new Organisation();
   org.country = "AT" + Math.random();
   org.name = "Helma" + Math.random();
   root.organisations.add(org);
   root.organisationsByCountry.add(org);

   // FIXME HELMABUG: count does not get incremented immediately
   assertEqual(countryCount + 1, root.organisationsByCountry.count());

   var country = root.organisationsByCountry.get(org.country);
   assertNotNull(country);
   assertEqual(country._prototype, "Country");
   assertEqual(country._id, org.country);
   assertEqual(country.count(), 1);
   assertEqual(country.get(0), org);

   root.organisationsByCountry.removeChild(org);
   org.remove();

   // FIXME HELMABUG: country is still accessible at this point
   // similar to http://helma.org/bugs/show_bug.cgi?id=551
   assertNull(root.organisationsByCountry.get(org.country));
   
   assertEqual(countryCount, root.organisationsByCountry.count());
}

function testGroupOrder() {

   var org1 = new Organisation();
   org1.country = "AT" + Math.random();
   org1.name = "Helma" + Math.random();
   root.organisations.add(org1);

   var org2 = new Organisation();
   org2.country = "CH" + Math.random();
   org2.name = "Helma" + Math.random();
   root.organisations.add(org2);

   var org3 = new Organisation();
   org3.country = "DE" + Math.random();
   org3.name = "Helma" + Math.random();
   root.organisations.add(org3);

   var org4 = new Organisation();
   org4.country = org3.country;
   org4.name = "Helma" + Math.random();
   root.organisations.add(org4);

   res.commit();

   // make sure that countries and organisations are sorted in decreasing order (as specified in type.properties)
   var countries = root.organisationsByCountry.list();
   for (var i=0; i<root.organisationsByCountry.count(); i++) {
      if (i>0) assertTrue(root.organisationsByCountry.get(i-1)._id >= root.organisationsByCountry.get(i)._id)
      for (var j=0; j<root.organisationsByCountry.get(i); j++) {
         if (j>0) assertTrue(root.organisationsByCountry.get(i).get(j-1)._id >= root.organisationsByCountry.get(i).get(j)._id)
      }
   }

   org1.remove();
   org2.remove();
   org3.remove();
   org4.remove();
}

function testGroupTransient() {

   var temp = new Root();
   var countryCount = temp.organisationsByCountry.count();
   var org = new Organisation();
   org.country = "AT" + Math.random();
   org.name = "Helma" + Math.random();
   temp.organisationsByCountry.add(org);

   var country = temp.organisationsByCountry.get(org.country);
   assertEqual(countryCount + 1, temp.organisationsByCountry.count());
   assertNotNull(country);
   assertEqual(country._prototype, "Country");
   assertEqual(country.groupname, org.country);
   
   // These don't work as org uses the parent from type.properties
   // which is root.organisations. Not sure if this is a bug or not.
   // assertEqual(country, org._parent);
   // org.remove();
   country.removeChild(org);

   assertNull(root.organisationsByCountry.get(org.country));
   assertEqual(countryCount, temp.organisationsByCountry.count());
}

function cleanup() {
    var orgs = root.organisations.list();
    for each (var org in orgs) {
        org.remove();
    }
}