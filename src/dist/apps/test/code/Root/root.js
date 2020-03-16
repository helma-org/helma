function main_action() {
    res.redirect(root.href("jala.test"));
}

function hello_action() {
    res.contentType = "text/plain";
    res.write("Hello");
}

function throwerror_action() {
    throw Error();
}

function notfound_action() {
    res.write("Not found");
}

function redirect_action() {
    res.redirect(this.href("hello"));
}

function error_action() {
    res.write("Error");
}

function long_action() {
    res.write(makeLongString());
}

function macro_macro(param) {
    // change suffix
    if (param.suffix) param.suffix = ".";
    return this.string;
}

