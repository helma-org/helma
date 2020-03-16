if (!global.helma) {
    global.helma = {};
}

/**
 * Performs basic admin level access checking for the specifed realm
 * @param String realm for which access should be checked and bootstrapped
 * @return true if access id verified, otherwise renders login form with bootstrapping instructions
 */
helma.auth = function(realm) {

    // helper function, checks if the client host matches an allowed host pattern,
    // hostnames are converted, wildcards are only allowed in ip-addresses
    var hostIsAllowed = function() {
        if (!getProperty(realm+'AccessAllowed'))
            return true;
        else if (getProperty(realm+'AccessAllowed') == 'false')
            return false;
        var filter = new Packages.helma.util.InetAddressFilter();
        var str = getProperty(realm+'AccessAllowed');
        if (str != null && str != "") {
            var arr = str.split(",");
            for (var i in arr) {
                str = new java.lang.String(arr[i]);
                try {
                    filter.addAddress(str.trim());
                } catch (a) {
                    try {
                        str = java.net.InetAddress.getByName(str.trim()).getHostAddress();
                        filter.addAddress(str);
                    } catch (b) {
                        app.log("error using address " + arr[i] + ": " + b);
                    }
                }
            }
        }
        return filter.matches(java.net.InetAddress.getByName(req.data.http_remotehost));
    }

    // Check if current session is authenticated for this realm
    if (session.data[realm+'Authenticated'] && hostIsAllowed())
        return true;

    // Otherwise, guide to properly configure access authentication for this realm
    res.data.fontface = 'Trebuchet MS, Verdana, sans-serif';
    res.data.href = path[path.length-1].href(req.action);
    var pw = getProperty('adminAccess');
    var param = {};
    var accessAllowed = true;
    if (req.data.username && req.data.password) {
        if (pw && hostIsAllowed()) {
            if (pw == Packages.helma.util.MD5Encoder.encode(req.data.username + "-" + req.data.password)) {
                session.data[realm+'Authenticated'] = true;
                res.redirect(res.data.href);
            } else {
                param.message = 'Sorry, wrong password!';
            }
        } else {
            param.message = 'Currently, '+ realm + ' access is not allowed!<br />';
            if (!pw) param.message += '\
                The adminAccess property is not set.<br />\
                Before proceeding, add the following line to your app.properties or server.properties file:\
                <br /><br />adminAccess='
                + Packages.helma.util.MD5Encoder.encode(req.data.username + "-" + req.data.password);
            else param.message += 'The '+ realm +'AccessAllowed property does not match your host.<br />\
                Before proceeding, remove this property from your app.properties or server.properties file \
                or include your host as follows:<br /><br />'
                + realm +'AccessAllowed=' + req.data.http_remotehost;
        }
    }
    res.data.header = 'Authentication for '+ realm +' access';
    renderSkin('helma.auth.login', param);
    return false;
}
helma.dontEnum('auth');

/**
 * Invalidates a previously authenticated realm
 * @param String realm for which an authentication should be invalidated
 * @return true if an authenticated realm was invalidated, otherwise false
 */
helma.invalidate = function(realm) {
    if (session.data[realm+'Authenticated']) {
        delete session.data[realm+'Authenticated'];
        return true;
    }
    else {
        return false;
    }
}
helma.dontEnum('invalidate');
