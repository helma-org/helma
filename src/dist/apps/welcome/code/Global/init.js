function onStart(name) {
    if (!root.get('first')) {
        root.name = 'root';
        var firstNode = new HopObject();
        firstNode.name = 'first';
        root.add(firstNode)
        var secondNode = new HopObject();
        secondNode.name = 'second';
        firstNode.add(secondNode)
    }
}
