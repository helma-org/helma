tests = [
   "testEquality",
   "testSimpleMapping",
   "testSimpleCollection",
   "testObjectReference",
   "testCollectionForReference"
];

function setup() {
}

function testEquality() {
    var person = new Person();
    root.persons.add(person);
    res.commit();
    var id = person._id;
    app.clearCache();
    var person2 = root.persons.get(id);
    assertNotNull(person2);
    assertTrue(person !== person2);
    assertTrue(person._id === person2._id);
    assertTrue(person == person2);
}

function testSimpleMapping() {

   var data = {
      name: "Oliver Stone",
      dateOfBirth: new Date(1946, 8, 15, 6, 0, 0),
      height: 182
   };
   var person = new Person();
   person.name = data.name;
   person.dateOfBirth = data.dateOfBirth;
   person.height = data.height;
   root.persons.add(person);
   var personId = person._id;

   res.commit(); // Commit Transaction
   app.clearCache(); // Clear cache so that object is refetched

   var person = Person.getById(personId);
   assertNotNull(person);
   assertEqual(person._prototype, "Person");
   assertEqual(person._id, personId);
   assertEqual(person.name, data.name);
   assertEqual(person.height, data.height);
   assertEqual(person.dateOfBirth.valueOf(), data.dateOfBirth.valueOf());
   res.commit(); 

   person.remove();
}

function testSimpleCollection() {

   var data = {
      name: "Helma"
   };
   var orgCount = root.organisations.count();
   var org = new Organisation();
   org.name = data.name;
   root.organisations.add(org);
   var orgId = org._id;

   assertEqual(orgCount + 1, root.organisations.count());
   assertNotEqual(root.organisations.indexOf(org), -1);

   // fetch Object via position
   assertEqual(root.organisations.get(root.organisations.indexOf(org)), org);

   // fetch Object via accessname
   assertEqual(root.organisations.get(data.name), org);

   // fetch Object via id
   assertEqual(root.organisations.getById(orgId), org);

   // test list
   assertEqual(root.organisations.count(), root.organisations.list().length);

   org.remove();

   assertEqual(orgCount, root.organisations.count());
   assertEqual(root.organisations.indexOf(org), -1);
   assertNull(root.organisations.get(data.name));
   assertNull(root.organisations.getById(orgId));

}


function testObjectReference() {

   var org = new Organisation();
   org.name = "Helma";
   root.organisations.add(org);
   var orgId = org._id;

   var person = new Person();
   person.name = "Hannes";
   person.organisation = org;
   root.persons.add(person);
   var personId = person._id;

   res.commit(); // Commit Transaction
   app.clearCache(); // Clear cache so that object is refetched

   var org = Organisation.getById(orgId);
   var person = Person.getById(personId);
   assertEqual(person.organisation, org);

   org.remove();
   res.commit(); // Commit Transaction

   assertNull(person.organisation);

   person.remove();
}


function testCollectionForReference() {

   var org = new Organisation();
   org.name = "Helma";
   root.organisations.add(org);
   var orgId = org._id;
   var personCount = org.persons.count();

   var person = new Person();
   person.name = "Hannes";
   person.organisation = org;
   root.persons.add(person);
   org.persons.add(person);

   assertEqual(personCount + 1, org.persons.count());
   assertNotEqual(org.persons.indexOf(person), -1);

   org.persons.removeChild(person);
   person.remove();

   assertEqual(personCount, org.persons.count());
   assertEqual(org.persons.indexOf(person), -1);

   org.remove();
}


function cleanup() {
}
