if (!global.helma) {
    global.helma = {};
}

/**
 * Checks shell access, renders the shell skin and evaluates submitted shell commands and scripts
 */
helma.shell = function(realm) {
    if (req.data.done) {
        helma.invalidate('shell');
        helma.invalidate('sqlshell');
        helma.invalidate('inspector');
        res.redirect(this.href());
    }
    if (!helma.auth('shell')) 
        res.abort();
    res.data.fontface = 'Trebuchet MS, Verdana, sans-serif';
    res.data.href = this.href();
    res.data.commands = encodeForm(req.data.commands);
    var evalcode = req.data.command || req.data.commands;
    if (!evalcode && helma.Inspector) {
        if (!session.data.inspectorAuthenticated) 
            session.data.inspectorAuthenticated = true;
        evalcode = '(new helma.Inspector(this)).action();"<hr>";';
    }
    if (evalcode) {
        try {
            var startTime = new Date();
            var evalResult = eval(evalcode);	
            var stopTime = new Date();
            res.write(evalResult);
            if (req.data.commands) {
                res.write('<br /><br />')
                res.write('<span style="font-family:'+res.data.fontface+'; color:grey; font-size:11px">')
                res.write('Script evaluated in ' + (stopTime.getTime() - startTime.getTime()) +' milliseconds.');
                res.write('</span><hr />');
            } else if (!req.data.command) {
                res.write('<hr />');
            }
        } catch ( e ) {
            res.write('<span class="error">');
            if ( e.javaException ) {
                var s = new java.io.StringWriter();
                e.javaException.printStackTrace( new java.io.PrintWriter( s ) );
                res.write( s.toString() );
            } else {
                res.write( format( e + '<br />' + e.fileName + ', lineNumber = ' + e.lineNumber ) );
            }
            res.write('</span>');
            if (req.data.commands) res.write('<hr />');
        }
    }
    if (!req.data.command) renderSkin('helma.shell');
}
helma.dontEnum('shell');

/**
 * Checks shell access, renders the shell skin and evaluates submitted shell commands and scripts
 */
HopObject.prototype[ (app.properties['shellAction'] || 'shell') +'_action' ] = helma.shell;
