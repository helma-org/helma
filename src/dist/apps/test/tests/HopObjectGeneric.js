tests = [
    'testSize',
    'testContent',
    'testGroupContent',
    'testRemove',
    'testAdd'
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

function testContent() {
    var list = org.generic.list();
    assertEqual(list.length, size);
    for (var i = 0; i < list.length; i++) {
        assertEqual(list[i].name, "GenericPerson " + i.format("00"));
    }
}

function testGroupContent() {
    var list = org.groupedGeneric.list();
    assertEqual(list.length, size);
    for (var i = 0; i < list.length; i++) {
        assertEqual(list[i].groupname, "GenericPerson " + i.format("00"));
        assertEqual(list[i].size(), 1);
        assertEqual(list[i].get(0).name, "GenericPerson " + i.format("00"));
    }
}

function testRemove() {
    var person = org.generic.get(size/2);
    org.generic.removeChild(person);
    assertEqual(org.generic.size(), size - 1);
    res.rollback();
    // note: removeChild does not remove the node, nor does it
    // unset the constraints between parent and child, so after a rollback
    // the object is back in place. While this behaviour is disputable,
    // until this is so we test for it.
    assertEqual(org.generic.size(), size);
}

function testAdd() {
    var person = new Person();
    org.generic.add(person);
    assertEqual(org.generic.size(), size + 1);
    assertEqual(org.groupedGeneric.size(), size);
    res.commit();
    // note: even after commit the grouped collection must not grow
    // since we added a person without a name
    assertEqual(org.generic.size(), size + 1);
    assertEqual(org.groupedGeneric.size(), size);
}

function cleanup() {
    var persons = org.generic.list();
    for each (var person in persons) {
        person.remove();
    }
    org.remove();
}
