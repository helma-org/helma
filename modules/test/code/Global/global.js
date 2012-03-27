function onStart() {
   root.date = new Date();
   root.string = "root";
}

function hello_macro(param) {
    return "hello";
}

function echo_macro(param, arg) {
   return arg || param.what;
}

function isDate_filter(arg) {
   return arg instanceof Date;
}

function isRootDate_filter(arg) {
    return (arg instanceof Date) && 
        arg.getTime() == root.date.getTime();
}

function isResponseDate_filter(arg) {
    return (arg instanceof Date) && arg == res.data.date;
}

function makeLongString() {
    var b = new java.lang.StringBuffer();
    for (var i = 1; i <= 10000; i++) {
        b.append(i.toString()).append(" ");
    }
    return b.toString();
}
