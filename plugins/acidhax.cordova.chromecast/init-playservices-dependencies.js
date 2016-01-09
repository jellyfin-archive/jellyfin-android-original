#!/usr/bin/env node

// Adds Google PlayServices dependencies to the current Cordova project
// This is done by turning (a copy of) Android SDK libraries AppCompat, MediaRouter and PlayServices into
// project libraries and linking them to the current project.
// Requires the Android SDK to be installed locally including ANDROID_HOME environment variable to be set.

var fs = require('fs');
var path = require('path');
var exec = require('child_process').exec;

var androidHome = process.env.ANDROID_HOME;
if( !androidHome ) {
    //throw new Error("Environment variable ANDROID_HOME is not set to Android SDK directory");
}
else {
    console.log("Found Android SDK at "+androidHome);
}


/**
 * Copies an entire directory into another
 */
var copyRecursiveSync = function(src, dest) {
    var exists = fs.existsSync(src);
    var stats = exists && fs.statSync(src);
    var isDirectory = exists && stats.isDirectory();
    if (exists && isDirectory) {
        fs.mkdirSync(dest);
        fs.readdirSync(src).forEach(function(childItemName) {
            copyRecursiveSync(path.join(src, childItemName), path.join(dest, childItemName));
        });
    } else {
        fs.createReadStream( src ).pipe( fs.createWriteStream( dest ) );
    }
};

/**
 * Executes an (external) command
 */
var execCommand = function(command, callback) {

    console.log("Executing "+command+" ...");
    var p = exec(command, function(error, stdout, stderr) {

        if(!!stdout ) {
            console.log("Exec: "+stdout);
        }
        if(!!stderr ) {
            console.error("Exec: "+stderr);
        }
        if(!!error ) {
            throw new Error("Exec: "+error);
        }
    });
    p.on("close", function(code) {
        if( code !== 0 ) {
            throw new Error("Error executing "+command+": "+code);
        }
        console.info("Executed "+command);
        if( !!callback ) {
            callback();
        }
    });
};

/**
 * Turns a project into an android "library project"
 * @param path The location of the project
 */
var prepareLibraryProject = function(path, callback) {

    execCommand(androidHome+"/tools/android update lib-project -p "+path+" -t \"android-21\"", function() {
        execCommand("ant clean -f "+path+"/build.xml", function() {
            execCommand("ant release -f "+path+"/build.xml", function() {

                console.info("Turned "+path+" into a library project");
                if(!!callback ) {
                    callback();
                }
            });
        });
    });
};

/**
 * Adds a library reference to a "library project"
 * @param libraryPath path The location of the project
 * @param referencePaths The relative locations of the references
 */
var addLibraryReference = function(libraryPath, referencePaths, callback) {

    var projectProperties = libraryPath+"/project.properties";
    console.info("Adding references "+referencePaths+" to "+projectProperties);

    fs.readFile(projectProperties, 'utf8', function (err,data) {
        if (err) {
            throw new Error("Error reading "+projectProperties);
        }

        // find the next available reference index
        var referenceIndex = 1;
        while( data.indexOf('android.library.reference.'+referenceIndex) > -1 ) {
            referenceIndex++;
        }

        // compute the entries to be appended
        var appends = "";
        for( var i=0; i<referencePaths.length; i++ ) {
            appends += "\n\randroid.library.reference."+(i+referenceIndex)+"="+referencePaths[i];
        }

        // append
        fs.appendFile(projectProperties, appends, function (err) {
            console.info("Added references to "+projectProperties+": "+appends);
            if( !!callback ) {
                callback();
            }
        });
    });
};


// -------------------------------


// copy libraries AppCompat, Mediarouter and PlayServices from Android SDK to local project

var appCompatLib    = './platforms/android/libs/AppCompatLib';
var mediaRouterLib  = './platforms/android/libs/MediarouterLib';
var playServicesLib = './platforms/android/libs/PlayServicesLib';

// HACK: avoid that the logic is executed for every plugin. TODO find something better.
if (fs.existsSync(appCompatLib)) {
    return;
}

//copyRecursiveSync(androidHome+"/extras/android/support/v7/appcompat/", appCompatLib+"/");
//copyRecursiveSync(androidHome+"/extras/android/support/v7/mediarouter/", mediaRouterLib+'/');
//copyRecursiveSync(androidHome+"/extras/google/google_play_services/libproject/google-play-services_lib/", playServicesLib+'/');

//// --- turn AppCompatLib into a library project
//prepareLibraryProject(appCompatLib, function() {
//    // --- turn MediarouterLib into a library project (after adjusting dependencies)
//    addLibraryReference(mediaRouterLib, ['../AppCompatLib'], function() {
//        prepareLibraryProject(mediaRouterLib, function() {
//            // --- turn PlayServicesLib into a library project
//            prepareLibraryProject(playServicesLib, function() {
//                // add all three libraries to current project
//                addLibraryReference("./platforms/android", ['libs/AppCompatLib','libs/MediarouterLib','libs/PlayServicesLib'], function() {

//                    console.info("Plugin installed");
//                });
//            });
//        });
//    });
//});




