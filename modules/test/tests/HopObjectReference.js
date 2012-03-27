tests = [
   "testForward",
   "testBackward",
];

function setup() {
    var org = new Organisation();
    var person = new Person();
    org.name = "Acme Hovercraft";
    person.name = "Murray Feather";
    person.organisation = org;
    org.person = person;
    person.persist();
    res.commit();
}

function testForward() {
    app.clearCache();
    person = root.persons.get(0);
    org = root.organisations.get(0);
    assertEqual(person.organisation, org);
    assertEqual(person.organisation.name, org.name);
    assertEqual("Acme Hovercraft", org.name);
}

function testBackward() {
    app.clearCache();
    var person = root.persons.get(0);
    var org = root.organisations.get(0);
    assertEqual(org.person, person);
    assertEqual(org.person.name, person.name);
    assertEqual("Murray Feather", person.name);    
}

function cleanup() {
    var person = root.persons.get(0);
    var org = root.organisations.get(0);
    org.remove();
    person.remove();
}
