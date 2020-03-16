tests = [
   "testSimpleParent",
   "testFallbackParent",
   "testMountpoints"
];


var org;
var person1;
var person2;

function setup() {
   org = new Organisation();
   org.name = "Helma";
   root.organisations.add(org);

   person1 = new Person();
   person1.name = "Hannes";
   person1.organisation = org;
   root.persons.add(person1);

   person2 = new Person();
   person2.name = "Michi";
   root.persons.add(person2);
}

function testSimpleParent() {
   assertEqual(org.href(), root.organisations.href() + org.name + "/");
   assertEqual(root.organisations, org._parent);
   assertEqual(root, org._parent._parent);
}

function testFallbackParent() {
   assertEqual(person1.href(), person1.organisation.persons.href() + person1.name + "/");
   assertEqual(person1.organisation.persons, person1._parent);

   assertEqual(person2.href(), root.persons.href() + person2._id + "/");
   assertEqual(root.persons, person2._parent);
}

function testMountpoints() {
   assertEqual(root.someMountpoint._prototype, "SomeMountpoint");
   assertEqual(root.someMountpoint._parent, root);
   assertEqual(root.someMountpoint.href(), root.href() + "someMountpoint/");

   assertEqual(org.someMountpoint._prototype, "SomeMountpoint");
   assertEqual(org.someMountpoint._parent, org);
   // FIXME: Helma-Bug ? mountpoints are converted to lower case ?
   assertEqual(org.someMountpoint.href(), org.href() + "someMountpoint/");
}

function cleanup() {
   org.remove();
   person1.remove();
   person2.remove();
}
