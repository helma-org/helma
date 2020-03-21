/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2017 Daniel Ruthardt. All rights reserved.
 */

package helma.scripting.rhino;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.apache.commons.io.FilenameUtils;
import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Bridges the gap between CommonJS-style module loading and Node.js-style module loading.
 */
public class NodeModulesProvider extends UrlModuleSourceProvider {

    /**
     * Define the serialization UID.
     */
    private static final long serialVersionUID = 6858072487233136717L;

    /**
     * Delegates to the super constructor.
     */
    public NodeModulesProvider(Iterable<URI> privilegedUris, Iterable<URI> fallbackUris) {
        // do what would have been done anyways
        super(privilegedUris, fallbackUris);
    }

    /**
     * Do what Node.js's LOAD_AS_FILE(X) would do.
     * Case 4 ("If X.node is a file, load X.node as binary addon.  STOP") is currently not supported (for
     * quite obvious reasons). We might want to load JAR files in the future.
     *
     * @see https://nodejs.org/dist/latest-v9.x/docs/api/modules.html#modules_file_modules
     *
     * @param uri
     *  The file to load. FILE, FILE.js and FILE.json will be tried in this order.
     * @param base
     *  Not used, only forwarded to
     *  {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     * @param validator
     *  Not used, only forwarded to
     *  {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     * @return
     *  The module source or null, if the module was not found.
     *
     * @throws IOException
     *  See {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     * @throws URISyntaxException
     *  See {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     */
    private ModuleSource loadAsFile(URI uri, URI base, Object validator)
            throws IOException, URISyntaxException {
        // lets assume the module is a file
        File file = new File(uri);
        // check if the file exists and is a file
        if (file.exists() && file.isFile()) {
            // check if the file is a JSON file
            if (file.getAbsolutePath().toLowerCase().endsWith(".json")) { //$NON-NLS-1$
                // return a JSON module source
                return new JSONModuleSource(null, file.toURI(), base, validator);
            } else {
                // do what would have been done anyways
                return super.loadFromUri(uri, base, validator);
            }
        }

        // lets assume the module is a JS file
        file = new File(new File(uri).getPath() + ".js"); //$NON-NLS-1$
        // check if a file.js exists and is a file
        if (file.exists() && file.isFile()) {
            // do what would have been done anyways
            return super.loadFromUri(uri, base, validator);
        }

        // lets assume the module is a JSON file
        file = new File(new File(uri).getPath() + ".json"); //$NON-NLS-1$
        // check if a file.json exists and is a file
        if (file.exists() && file.isFile()) {
            // return a JSON module source
            return new JSONModuleSource(null, file.toURI(), base, validator);
        }

        // module not found
        return null;
    }

    /**
     * Do what Node.js's LOAD_AS_DIRECTORY(X) would do.
     *
     * @see https://nodejs.org/dist/latest-v9.x/docs/api/modules.html#modules_file_modules
     *
     * @param uri
     *  The directory to load.
     * @param base
     *  Not used, only forwarded to
     *  {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     * @param validator
     *  Not used, only forwarded to
     *  {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     * @return
     *  The module source or null, if the module was not found.
     *
     * @throws JsonSyntaxException
     *  Thrown for problems accessing the module's "package.json" file.
     * @throws IOException
     *  See {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     * @throws URISyntaxException
     *  See {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}.
     */
    private ModuleSource loadAsDirectory(URI uri, URI base, Object validator)
            throws JsonSyntaxException, IOException, URISyntaxException {
        // lets assume the module is a directory
        File directory = new File(uri);
        // check if the directory exists and is a directory
        if (directory.exists() && directory.isDirectory()) {
            // the module source
            ModuleSource moduleSource;

            // lets assume that there is a "package.json" file in the directory
            File packageFile = new File(directory, "package.json"); //$NON-NLS-1$

            // check if the there is a "package.json" file in the directory
            if (packageFile.exists() && packageFile.isFile()) {
                // parse the JSON file
                JsonObject json = new JsonParser()
                        .parse(new String(Files.readAllBytes(packageFile.toPath()))).getAsJsonObject();
                // check if the JSON file defines a main JS file
                if (json.has("main")) { //$NON-NLS-1$
                    // get the main JS file, removing the filename extension
                    String main = FilenameUtils.removeExtension(json.get("main").getAsString()); //$NON-NLS-1$

                    // load as file
                    moduleSource = this.loadAsFile(new File(directory, main).toURI(), base, validator);
                    // check if something was loaded
                    if (moduleSource != null) {
                        // return the loaded module source
                        return moduleSource;
                    }
                }
            }

            // load as index
            moduleSource = this.loadAsFile(new File(directory, "index").toURI(), base, validator); //$NON-NLS-1$
            // check if something was loaded
            if (moduleSource != null) {
                // return the loaded module source
                return moduleSource;
            }
        }

        // module not found
        return null;
    }

    /**
     * Do what Node.js's require(X) would do.
     *
     * Case 1 is not supported, you will have to use modules from npmjs.org, re-implementing the core
     * core module's functionality. We might want to use Nodeschnaps in the future.
     * Case 2 is not supported, paths are always treated as relative paths within the application's
     * "commonjs" directory.
     * Case 5 additionally tries {@link UrlModuleSourceProvider#loadSource(URI, URI, Object)}, even if it is
     * very unlikely that something, which hasn't been tried yet, will be done. One could say we are just
     * delegating throwing the error.
     *
     * @see https://nodejs.org/dist/latest-v9.x/docs/api/modules.html#modules_file_modules
     * @see https://github.com/killmag10/nodeschnaps
     */
    protected ModuleSource loadFromUri(URI uri, URI base, Object validator)
            throws IOException, URISyntaxException {
        // the module source
        ModuleSource moduleSource;

        // load as file
        moduleSource = this.loadAsFile(uri, base, validator);
        // check if something was loaded
        if (moduleSource != null) {
            // return the loaded module source
            return moduleSource;
        }

        // load as directory
        moduleSource = this.loadAsDirectory(uri, base, validator);
        // check if something was loaded
        if (moduleSource != null) {
            // return the loaded module source
            return moduleSource;
        }

        // do what would have been done anyways
        return super.loadFromUri(uri, base, validator);
    }

}
