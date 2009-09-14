tests = [
    "testSize",
    "testAddRemoveSmall",
    "testAddRemoveLarge",
    "testListSmall",
    "testListLarge"
];

var helma, ikea;
var small = 3, large = 1234;

function setup() {
    ikea = makeOrg('Ikea', large);
    helma = makeOrg('Helma', small);
}

function testSize() {
    assertEqual(ikea.persons.size(), large);
    assertEqual(helma.persons.size(), small);
}

function testAddRemoveSmall(org) {
    testAddRemove(helma, small);
}

function testAddRemoveLarge(org) {
    testAddRemove(ikea, large);
}

function testAddRemove(org, size) {
    var person = new Person();
    person.name = "TestPerson";
    person.organisation = org;
    person.persist();
    res.commit();
    assertEqual(org.persons.size(), size + 1);
    org.persons.prefetchChildren();
    assertEqual(org.persons.indexOf(person), size);
    assertEqual(org.persons.contains(person), size);
    assertEqual(person.href(), org.persons.href() + "TestPerson/");
    person.remove();
    res.commit();
    assertEqual(org.persons.size(), size);
    assertEqual(org.persons.indexOf(person), -1);
    assertEqual(org.persons.contains(person), -1);
}

function testListSmall() {
    testList(helma, small);
}

function testListLarge() {
    testList(ikea, large);
}

function testList(org, size) {
    function iterate(list, start, length) {
        assertEqual(list.length, length);
        for (var i = 0; i < length; i++) {
            assertEqual(list[i].name, "Person " + org.name + " " + (start + i).format("0000"));
        }
    }
    iterate(org.persons.list(), 0, size);
    org.persons.invalidate();
    iterate(org.persons.list(), 0, size);
    org.persons.invalidate();
    iterate(org.persons.list(1, size - 2), 1, size - 2);
}

function cleanup() {
    var persons = root.persons.list();
    for each (var person in persons) {
        person.remove();
    }
    ikea.remove();
    helma.remove();
}

function makeOrg(name, size) {
    var org = new Organisation();
    org.name = name;
    root.organisations.add(org);

    for (var i = 0; i < size; i++) {
        var person = new Person();
        person.name = "Person " + name + " " + i.format("0000");
        person.organisation = org;
        root.persons.add(person);
    }
    res.commit();
    return org;
}

// debugging helpers
function dumpDataChange(message) {
    res.debug(message + ": ");
    dumpDataChangeFor("Person");
    dumpDataChangeFor("Organisation");
}

function dumpDataChangeFor(name) {
    res.debug(name + ": " + app.__app__.getDbMapping(name).getLastDataChange());
}
