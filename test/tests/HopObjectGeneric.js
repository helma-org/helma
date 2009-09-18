tests = [
    'testSize'
];

var org;
var size = 16;

function setup() {
    org = new Organisation();
    org.name = "GenericOrg";
    var i = 0, person;

    function addPerson(collection) {
        person = new Person();
        person.name = "GenericPerson " + i.format("00");
        collection.add(person);
        i++;
    }

    // add first half to both collections of transient parent ...
    while (i < 4)
        addPerson(org.generic);
    while (i < 8)
        addPerson(org.groupedGeneric);
    root.organisations.add(org);
    // ... second half to both collections of persistent parent.
    while (i < 12)
        addPerson(org.generic);
    while (i < size)
        addPerson(org.groupedGeneric);

    res.commit();
}

function testSize() {
    assertEqual(org.generic.size(), size);
    org.invalidate();
    assertEqual(org.generic.size(), size);
}

function cleanup() {
    var persons = org.generic.list();
    for each (var person in persons) {
        person.remove();
    }
    org.remove();
}
