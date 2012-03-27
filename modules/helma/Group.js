/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Group.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * @fileoverview A JavaScript library wrapping 
 * Packages.helma.extensions.helmagroups
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/helma/Group.js')
 */

// Define the global namespace if not existing
if (!global.helma) {
    global.helma = {};
}

/**
 * Constructs a new helma.Group Object.
 * @class This is what is retrieved through groups.get(groupName),
 * wrapping the root object of each group tree.
 * @param {FIXME} javaGroup FIXME
 * @constructor
 */
helma.Group = function(javaGroup) {
    // private variable containing the wrapper object
    var groupRoot = new helma.Group.GroupObject(javaGroup.getRoot());

    /**
     * @returns the wrapped java object Group
     */
    this.getJavaObject = function() {
        return javaGroup;
    };

    /**
     * sets a key/value pair on the group's root, 
     * wraps the function of the wrapper object
     */
    this.set = function(key, val, sendMode) {
        groupRoot.set(key, val, sendMode);
        return;
    };

    /**
     * removes a key from the group's root, 
     * wraps the function of the root GroupObject
     */
    this.remove = function(key, sendMode) {
        return groupRoot.remove(key, sendMode);
    };

    /**
     * retrieves a key from the group's root, 
     * wraps the function of the root GroupObject
     */
    this.get = function(key) {
        return groupRoot.get(key);
    };

    /**
     * @see helma.Group.GroupObject.listChildren
     */   
    this.listChildren = function() {
        return groupRoot.listChildren();
    };

    /**
     * @see helma.Group.GroupObject.listProperties
     */   
    this.listProperties = function() {
        return groupRoot.listProperties();
    };
   
    /**
     * @see helma.Group.GroupObject.countChildren
     */   
    this.countChildren = function() {
        return groupRoot.countChildren();
    };

    /**
     * @see helma.Group.GroupObject.countProperties
     */   
    this.countProperties = function() {
        return groupRoot.countProperties();
    };

    /**
     * calls a function in all connected applications 
     * (to be specific: in all registered localClients).
     * @param method name of the method in xmlrpc-style: test 
     * is called as root.test(), stories.137.render
     * is called as root.stories.get("137").render() etc etc.
     * @param argArr array of arguments to the remote method
     * @param sendMode as defined for helma.Group.GroupObject
     * @returns array of result objects
     */
    this.callFunction = function(method, argArr, sendMode) {
        groups.checkWriteAccess(javaGroup);
        if (sendMode == null) {
            sendMode = helma.Group.GroupObject.DEFAULT_GET;
        }
        var argVec = new java.util.Vector();
        for (var i=0; i<argArr.length; i++) {
            argVec.add(argArr[i]);
        }
        var resVec = javaGroup.execute(method, argVec, sendMode, 
                                       javaGroup.DEFAULT_EXECUTE_TIMEOUT);
        var resArr = [];
        for (var i=0; i<resVec.size(); i++) {
            resArr[i] = resVec.get(i);
        }
        return resArr;
     };
   
     this.toString = function() {
         return javaGroup.toString();
     };
   
     return this;
};

/**
 * Constructs a new helma.Group.GroupObject. 
 * @class This class wraps the java GroupObject
 * and provides several methods for retrieving and manipulating properties.
 * @param {Object} Instance of helma.extensions.helmagroups.GroupObject
 * @constructor
 */
helma.Group.GroupObject = function(javaGroupObject) {
    var helmagroups = Packages.helma.extensions.helmagroups;

    if (!javaGroupObject) {
        var javaGroupObject = new helmagroups.GroupObject();
    }

    /**
     * private method that returns true if the group
     * is writable
     * @returns Boolean
     */
    var checkWriteAccess = function() {
        if (javaGroupObject.getState() == helmagroups.GroupObject.REPLICATED) {
            groups.checkWriteAccess(javaGroupObject.getGroup());
        }
        return true;
    };

    /**
     * Checks if the key passed as argument is a path
     * (either an Array or a String that contains separator characters)
     * @returns Boolean
     */
    var keyIsPath = function(key) {
        var separator = helmagroups.GroupObject.SEPARATOR;
        if ((key instanceof Array) || key.indexOf(separator) != -1) {
            return true;
        }
        return false;
    };
   
    /**
     * Returns the last element if the key passed as argument is a path.
     * @returns Boolean
     */
    var getLastKeyElement = function(key) {
        var separator = helmagroups.GroupObject.SEPARATOR;
        if (!(key instanceof Array) && key.indexOf(separator) != -1) {
            if (key.charAt(key.length-1)==separator) {
                key = key.substring(0, key.length-1);
            }
            key = key.split(separator);
        }
        if (key instanceof Array) {
            return key[key.length-1];
        }
        return null;
    };
 
 
    /**
     * if key is a path, walks through the path and returns the lowest GroupObject.
     * if tree ends somewhere in the path, function returns null.
     * @returns null or GroupObject
     */
    var walkPath = function(obj, key) {
        var separator = helmagroups.GroupObject.SEPARATOR;
        if (!(key instanceof Array) && key.indexOf(separator) != -1) {
            if (key.charAt(key.length-1)==separator) {
                key = key.substring(0, key.length-1);
            }
            key = key.split(separator);
        }
        if (key instanceof Array) {
            // loop down until end of array
            for (var i=0; i<key.length-1; i++) {
                var nextObj = obj.get(key[i]);
                if (nextObj == null || !(nextObj instanceof helma.Group.GroupObject)) {
                    return null;
                }
                obj = nextObj;
            }
            return obj;
        }
    };
 
 
    /**
     * if key is a path, walks through the path and returns the lowest GroupObject.
     * if tree ends somewhere in the path, function creates the missing GroupObjects.
     * @returns helma.Group.GroupObject
     */
    var createPath = function(obj, key) {
        var separator = helmagroups.GroupObject.SEPARATOR;
        if (!(key instanceof Array) && key.indexOf(separator) != -1) {
            if (key.charAt(key.length-1)==separator) {
                key = key.substring(0, key.length-1);
            }
            key = key.split(separator);
        }
        if (key instanceof Array) {
            // loop down until end of array
            for (var i=0; i<key.length-1; i++) {
                var nextObj = obj.get(key[i]);
                if (nextObj == null || !(nextObj instanceof helma.Group.GroupObject)) {
                    nextObj = new helma.Group.GroupObject();
                    obj.set(key[i], nextObj);
                }
                obj = nextObj;
            }
            return obj;
        }
    };
 
 
    /**
     * Returns the wrapped java GroupObject.
     * @return Instance of helma.extensions.helmagroups.GroupObject;
     * @type helma.extensions.helmagroups.GroupObject
     */
    this.getJavaObject = function() {
        return javaGroupObject;
    };
 
 
    /**
     * Sets a property or a child GroupObject in this instance.
     * The Key may be a String, an Array or a String with separator characters ("/").
     * In the latter two cases the argument is considered a path and
     * all GroupObjects along this path are created if necessary.
     * @param {Object} key Either
     * <ul>
     * <li>a String</li>
     * <li>a String containing slashes</li>
     * <li>an Array containing String keys</li>
     * </ul>
     * @param {Number} The value to set the property to.
     * @param {Object} The mode to use when committing the change to
     * the helma.Group
     */
    this.set = function(key, val, sendMode) {
        if (!key) {
            throw "helma.Group.GroupObject.set(): key can't be null";
        }
        checkWriteAccess();
        // check content type of value:
        var ok = false;
        if (val == null)
            ok = true;
        else if (typeof(val) == "string")
            ok = true;
        else if (typeof(val) == "number")
            ok = true;
        else if (typeof(val) == "boolean")
            ok = true;
        else if (val instanceof Date)
            ok = true;
        else if (val instanceof helma.Group.GroupObject)
            ok = true;
        if (ok == false) {   
            throw "only primitive values, Date and helma.Group.GroupObject allowed in helma.Group.GroupObject.set()";
        }
        if (sendMode == null) {
            sendMode = helma.Group.GroupObject.DEFAULT_GET;
        }
        
        if (keyIsPath(key)) {
            var obj = createPath(this, key);
            if (obj != null) {
                obj.set(getLastKeyElement(key), val, sendMode);
            }
        } else {
            // set a property/child of this object
            if (val == null) {
                // null values aren't permitted in the group,
                // setting a property to null is the same as deleting it
                this.remove(key, sendMode);
            } else if (val instanceof helma.Group.GroupObject) {
                // replicate helma.Group.GroupObject
                javaGroupObject.put(key, val.getJavaObject(), sendMode);
            } else {
                // put the primitive property (or maybe replicate,
                // decision's up to helma.Group.GroupObject)
                if (val instanceof Date) {
                    // convert javascript dates to java dates
                    val = new java.util.Date(val.getTime());
                }
                javaGroupObject.put(key, val, sendMode);
            }
        }
        return;
    };
 
 
 
    /**
     * Removes a property or a child GroupObject from this instance.
     * The Key may be a String, an Array or a String with separator characters ("/").
     * In the latter two cases the argument is considered a path and
     * the function walks down that path to find the GroupObject and
     * deletes it.
     * @param {Object} key Either
     * <ul>
     * <li>a String</li>
     * <li>a String containing slashes</li>
     * <li>an Array containing String keys</li>
     * </ul>
     * @param {Number} The mode to use when committing the change to
     * the helma.Group
     */
    this.remove = function(key, sendMode) {
        checkWriteAccess();
        if (sendMode == null) {
            sendMode = helma.Group.GroupObject.DEFAULT_GET;
        }
        if (keyIsPath(key)) {
            var obj = walkPath(this, key);
            if (obj != null) {
                obj.remove(getLastKeyElement(key));
            }
        } else {
            javaGroupObject.remove(key, sendMode);
        }
        return;
    };
 
 
    /**
     * Returns either a property or a child GroupObject from
     * this GroupObject instance. The key passed as argument
     * may be a String, an Array containing Strings or a
     * String containing separator characters ("/"). In the latter
     * two cases the argument is considered a path and
     * the function walks down that path to find the requested
     * GroupObject.
     * @param {Object} key Either
     * <ul>
     * <li>a String</li>
     * <li>a String containing slashes</li>
     * <li>an Array containing String keys</li>
     * </ul>
     * @return Depending on the argument either the appropriate property
     * value or a helma.Group.GroupObject
     * @type Object
     */
    this.get = function(key) {
        if (key == null) {
            return null;
        }
        if (keyIsPath(key)) {
            var obj = walkPath(this, key);
            if (obj != null) {
                return obj.get(getLastKeyElement(key));
            } else {
                return null;
            }
        } else if (javaGroupObject.hasProperty(key)) {
           // we got a primitive property
           var val = javaGroupObject.getProperty(key);
           if (val instanceof java.util.Date) {
               // convert java dates to javascript dates
               val = new Date(val);
           }
           return val;
        } else if (javaGroupObject.hasChild(key)) {
            // we got a child object
            return new helma.Group.GroupObject(javaGroupObject.getChild(key));
        }
        return null;
    };
 
 
    /**
     * Gets a property from this GroupObject. The key passed as argument
     * is always considered a property even if it contains a slash.
     * This is actually a workaround for the fact that other
     * instances of the group not using the javascript extension aren't forbidden
     * to add properties containing a slash in the property's name.
     * So, using this extension we can at least read the property.
     * @param {String} key The name of the property to return
     * @returns The value of the property
     * @type Object
     */
    this.getProperty = function(key) {
        if (key == null) {
            return null;
        } else if (javaGroupObject.hasProperty(key)) {
            // we got a primitive property
            var val = javaGroupObject.getProperty(key);
            if (val instanceof java.util.Date) {
                // convert java dates to javascript dates
                val = new Date(val);
            }
            return val;
        }
        return null;
    }
    
 
    /**
     * Exchanges this GroupObject with the one passed
     * as argument. This is done by exchanging the wrapped
     * instance of helma.extensions.helmagroups.GroupObject
     * @param {GroupObject} The GroupObject to use
     * @returns The GroupObject with the exchanged wrapped java object
     * @type GroupObject
     */
    this.wrap = function(newGroupObject) {
        checkWriteAccess();
        if (javaGroupObject.getState() != helmagroups.GroupObject.REPLICATED) {
            throw "helma.Group.GroupObject.wrap() may only be called on replicated GroupObjects";
        }
        if (newGroupObject == null || !(newGroupObject instanceof helma.Group.GroupObject)) {
            throw "helma.Group.GroupObject.wrap() requires a helma.Group.GroupObject as an argument";
        }
        javaGroupObject.wrap(newGroupObject.getJavaObject());
        return this;
    };
 
    /**
     * Clones this GroupObject and returns it.
     * This method should be considered if many properties
     * of a GroupObject must be set or modified since every
     * change to an already replicated GroupObject will
     * result in immediate network traffic. Using unwrap
     * one can modify several properties and then commit
     * the GroupObject at once using {@link #wrap).
     * @returns A clone of this GroupObject
     * @type GroupObject
     */
    this.unwrap = function() {
        var javaGroupObjectClone = javaGroupObject.clone();
        javaGroupObjectClone.setChildren(new java.util.Hashtable());
        javaGroupObjectClone.setState(helmagroups.GroupObject.LOCAL);
        javaGroupObjectClone.setPath(null);
        return new helma.Group.GroupObject(javaGroupObjectClone);
    };
 
    /**
     * Converts this GroupObject into a vanilla Object
     * @returns An Object containing all properties of this GroupObject
     * @type Object
     */
    this.toJSObject = function() {
        var key;
        var obj = {};
        var e = javaGroupObject.properties();
        while(e.hasMoreElements()) {
            obj[key = e.nextElement()] = javaGroupObject.getProperty(key);
        }
        return obj;
    };
   
    /**
     * Returns an Array containing all child GroupObjects
     * @returns An Array containing GroupObjects
     * @type Array
     */
    this.listChildren = function() {
        var arr = [];
        var e = javaGroupObject.children();
        while(e.hasMoreElements()) {
            arr.push(e.nextElement());
        }
        return arr;
    };
 
    /**
     * Returns an Array containing all property
     * names of this GroupObject instance
     * @returns An Array containing property names
     * @type Array
     */
    this.listProperties = function() {
        var arr = [];
        var e = javaGroupObject.properties();
        while(e.hasMoreElements()) {
            arr.push(e.nextElement());
        }
        return arr;
    };
  
    /**
     * Returns the number of child GroupObjects
     * @returns The number of child GroupObjects of this
     * helma.Group.GroupObject instance
     * @type Number
     */
    this.countChildren = function() {
        var ht = javaGroupObject.getChildren();
        if (ht == null) {
            return 0;
        } else {
            return ht.size();
        }
    };
 
    /**
     * Returns the number of properties of this GroupObject
     * @return The number of properties
     * @type Number
     */
    this.countProperties = function() {
        var ht = javaGroupObject.getProperties();
        return (ht == null) ? 0 : ht.size();
    };
 
    /**
     * Returns true if the GroupObject is <em>not</em> replicated
     * @returns True if this GroupObject is still local
     * @type Boolean
     */
    this.isLocal = function() {
        return (javaGroupObject.getState()
                == helmagroups.GroupObject.LOCAL);
    };
 
    /** @ignore */
    this.toString = function() {
        return javaGroupObject.toString();
    };
 
    return this;
};

/**
 * Static properties of GroupObject constructor function.
 * These values determine if and for how many confirmation of the
 * group members this instance waits after a modification.
 * These values are passed through to org.jgroups.blocks.GroupRequest,
 * for further comments see the sourcecode of that class
 */
// wait just for the first response
helma.Group.GroupObject.GET_FIRST = 1;
// wait until all members have responded
helma.Group.GroupObject.GET_ALL = 2;
// wait for majority (50% + 1) to respond
helma.Group.GroupObject.GET_MAJORITY = 3;
// wait for majority of all members (may block!)
helma.Group.GroupObject.GET_ABS_MAJORITY = 4;
// don't wait for any response (fire & forget)
helma.Group.GroupObject.GET_NONE = 6;
// default: wait for all responses
helma.Group.GroupObject.DEFAULT_GET = helma.Group.GroupObject.GET_ALL;

/**
 * This is mounted as "groups". 
 * @class The root for all groups started in this application
 * @constructor
 */
helma.Group.Manager = function() {
    var helmagroups = Packages.helma.extensions.helmagroups;
    var extension = helmagroups.GroupExtension.self;

    if (extension == null) {
        throw("helma.Group.Manager requires the HelmaGroups Extension \
               located in lib/ext or the application's top-level directory \
               [http://adele.helma.org/download/helma/contrib/helmagroups/]");
    }
    
    
    /**
     * get a java object Group for a groupname.
     * object is fetched regardless of connection status
     * @returns null if group is not defined
     */
    var getJavaGroup = function(name) {
        return extension.checkAppLink(app.name).get(name);
    };
    
    
    /**
     * visible to scripting env: get a group, wrapped as a javascript helma.Group object.
     * the group must be defined in app.properties: group.nameXX = <configfile>
     * and can then be accessed like this group.get("nameXX")
     * @returns null if group is not defined or not connected
     */
    this.get = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null || javaGroup.isConnected() == false) {
            return null;
        } else {
            return new helma.Group(javaGroup);
        }
    };
    
    
    /**
     * checks for write access to a group according to app.properties
     * group.nameXX.writable must be true so that this function returns
     * @param nameOrJGroup can be the name of a group or a java Group itself
     * @throws an error if group is not writable
     */
    this.checkWriteAccess = function(nameOrJGroup) {
        var extension = helmagroups.GroupExtension.self;
        var jAppLink = extension.checkAppLink(app.name);
        if (nameOrJGroup instanceof helmagroups.Group) {
            // arg was a Group
            var javaGroup = nameOrJGroup;
        } else {
            // arg was the name of the group
            var javaGroup = jAppLink.get(nameOrJGroup);
        }
        if (javaGroup != null && jAppLink.isWritable(javaGroup) == false) {
            throw("tried to access write-protected group");
        }
        return true;
    };
    
    
    /**
     * try to connect a group
     * @returns false if group is not found
     */
    this.connect = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        javaGroup.connect();
        return true;
    };
    
    
    /**
     * try to disconnect from a group
     * @returns false if group is not found
     */
    this.disconnect = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        javaGroup.disconnect();
        return true;
    };
    
    
    /**
     * try to disconnect and connect again to a group
     * @returns false if group is not found
     */
    this.reconnect = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        javaGroup.reconnect();
        return true;
    };
    
    
    /**
     * try to reset a group (if application may write in group).
     * all instances of the group empty their cache.
     * @returns false if group is not found
     */
    this.reset = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        groups.checkWriteAccess(javaGroup);
        javaGroup.reset();
        return true;
    };
    
    
    /**
     * try to destroy a group (if application may write in group).
     * all other instances of the group disconnect
     * @returns false if group is not found
     */
    this.destroy = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        groups.checkWriteAccess(javaGroup);
        javaGroup.destroy();
        return true;
    };
    
    
    /**
     * try to restart a group (if application may write in group).
     * all other instances of the group disconnect and reconnect - each app after a different pause
     * so that they don't all come up at the same moment
     * @returns false if group is not found
     */
    this.restart = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        groups.checkWriteAccess(javaGroup);
        javaGroup.restart();
        return true;
    };
    
    
    /**
     * list the members of this group (ie instances of Group, one helma server is one instance)
     * @returns array of strings, false if group is not found
     */
    this.listMembers = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        var addrArr = javaGroup.info.listMembers();
        var arr = [];
        for (var i=0; i<addrArr.length; i++) {
            arr[arr.length] = helmagroups.Config.addressToString(addrArr[i]);
        }
        return arr;
    };
    
    
    /**
     * lists the members applications of this group (may be more than one per instance but also none)
     * @returns array of strings, false if group is not found
     */
    this.listMemberApps = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        var appsArr = javaGroup.info.listMemberApps();
        var arr = [];
        for (var i=0; i<appsArr.length; i++) {
            arr[arr.length] = appsArr[i];
        }
        return arr;
    };
    
    
    /**
     * dumps the keys of the group to a string
     * @returns string, notice if group is not found
     */
    this.getContent = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return "[not connected]";
        }
        return javaGroup.info.print();
    };
    
    
    /**
     * dumps the keys and the content of the group to a string
     * @returns string, notice if group is not found
     */
    this.getFullContent = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return "[not connected]";
        }
        return javaGroup.info.printFull();
    };
    
    
    /**
     * dumps the config of the jgroups stack to a string
     * @returns string, false if group is not found
     */
    this.getConfig = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        return javaGroup.info.printStack(false);
    };
    
    
    /**
     * dumps the config of the jgroups stack including all properties to a string
     * @returns string, false if group is not found
     */
    this.getFullConfig = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        return javaGroup.info.printStack(true);
    };
    
    
    /**
     * returns the connection identifier of the Group instance (localname + multicast-target)
     * @returns string, false if group is not found
     */
    this.getConnection = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        return javaGroup.info.getConnection();
    };
    
    
    /**
     * returns true/false if the group is connected
     */
    this.isConnected = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        return javaGroup.isConnected();
    };
    
    
    /**
     * returns the total number of groupobjects in this group
     */
    this.size = function(name) {
        var javaGroup = getJavaGroup(name);
        if (javaGroup == null) {
            return false;
        }
        return javaGroup.size();
    };
    
    
    /**
     * returns the total number of groupobjects in this group
     */
    this.count = function(name) {
        return this.size(name);
    };
    
    this.toString = function() {
        return "[helma.Group.Manager]";
    };
    
    return this;
}

// Instantiate helma.Group.Manager as "groups" variable
var groups = new helma.Group.Manager();
