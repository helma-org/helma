/**
  * macro rendering a skin
  * @param name name of skin
  */
function skin_macro(par) {
    if (par && par.name) {
        renderSkin(par.name);
    }
}


/**
  * Macro returning the actual date and time.
  */
function now_macro() {
    var date = new Date();
    return(date.format("dd.MM.yyyy, HH:mm'h' zzz"));
}
