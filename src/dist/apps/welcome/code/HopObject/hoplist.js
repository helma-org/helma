function hoplist(){
    var list = '';
    for (var subnode in this.list()) {
        list += '<li><a href="'+ this.list()[subnode].href()
             +'">'+ this.list()[subnode].name +'</a></li>';
    }
    return '<ul>'+ list +'</ul>';
}
