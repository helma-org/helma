function anotherexample_action() {
    res.write('Hello again, this is the action \
        defined in ./apps/welcome/code/Root/example.js');
}

function pullLink_macro(params) {
    switch (params.part) {
        case 'text' : return '/example_using_macro';
        case 'url'  : return this.href('example_using_macro');;
    }
}
