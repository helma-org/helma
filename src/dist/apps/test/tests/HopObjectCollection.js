tests = [
    "testSize",
    "testMaxSize",
    "testAddSmall",
    "testAddLarge",
    "testRemoveSmall",
    "testRemoveLarge",
    "testUpdateSmall",
    "testUpdateLarge",
    "testListSmall",
    "testListLarge",
    "testOrderLarge",
    "testOrderSmall"
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

function testMaxSize() {
    assertEqual(ikea.range.size(), 100);
    assertEqual(helma.range.size(), 0);
    var person = ikea.range.get("Person Ikea 0150");
    assertNotNull(person);
    assertEqual(person, ikea.range.get(50));
    assertEqual(person, ikea.persons.get(150));
    assertEqual(50, ikea.range.indexOf(person));
    assertEqual(150, ikea.persons.indexOf(person));
}

function testAddSmall() {
    testAdd(helma, small);
}

function testAddLarge() {
    testAdd(ikea, large);
}

// test directly adding to a collection
function testAdd(org, size) {
    var person = new Person();
    person.name = "TestPerson";
    org.persons.add(person);
    assertEqual(org.persons.size(), size + 1);
    assertEqual(org.persons.indexOf(person), size);
    assertEqual(org.persons.contains(person), size);
    assertEqual(person.href(), org.persons.href() + "TestPerson/");
    // make sure the add has set the back-reference on the person object.
    // note that === comparison will return false if the
    // collection size exceeds the cache size.
    assertTrue(person.organisation == org);
}

function testRemoveSmall() {
    testRemove(helma, small);
}

function testRemoveLarge() {
    testRemove(ikea, large);
}

// test directly removing from a collection
function testRemove(org, size) {
    var person = org.persons.get(org.persons.size() - 1);
    person.remove();
    assertEqual(org.persons.size(), size);
    assertEqual(org.persons.indexOf(person), -1);
    assertEqual(org.persons.contains(person), -1);
}

function testUpdateSmall() {
    testUpdate(helma, small);
}

function testUpdateLarge() {
    testUpdate(ikea, large);
}

// test indirectly adding to and removing form a collection
function testUpdate(org, size) {
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
            assertEqual(list[i].name, "Person " + org.name + (start + i).format(" 0000"));
        }
    }
    iterate(org.persons.list(), 0, size);
    org.persons.invalidate();
    iterate(org.persons.list(), 0, size);
    org.persons.invalidate();
    iterate(org.persons.list(1, size - 2), 1, size - 2);
}

function testOrderLarge() {
    testOrder(ikea, ikea.persons.size() - 2);
    testOrder(ikea, Math.floor(ikea.persons.size() / 2));
    testOrder(ikea, 2);
}

function testOrderSmall() {
    testOrder(helma, helma.persons.size() - 1);
    testOrder(helma, 1);
    testOrder(helma, 0);
}

function testOrder(org, pos) {
    var person = new Person();
    person.name = "Person " + org.name + pos.format(" 0000") + "B";
    person.organisation = org;
    root.persons.add(person);
    res.commit();
    assertEqual(pos + 1, org.persons.indexOf(person));
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
        person.name = "Person " + name + i.format(" 0000");
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
