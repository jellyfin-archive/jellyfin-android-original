#!/usr/bin/env node

/*jshint latedef:nofunc, node:true*/

// Modules
var fs          = require('fs');
var path        = require('path');
var UglifyJS    = require('uglify-js');
var CleanCSS    = require('clean-css');
var ngAnnotate  = require('ng-annotate');
var Imagemin    = require('imagemin');

// Process
var rootDir      = process.argv[2];
var platformPath = path.join(rootDir, 'platforms');
var platforms    = process.env.CORDOVA_PLATFORMS.split(',');
var cliCommand   = process.env.CORDOVA_CMDLINE;

// Hook configuration
var configFilePath        = path.join(rootDir, 'hooks/uglify-config.json');
var hookConfig            = JSON.parse(fs.readFileSync(configFilePath));
var isRelease             = hookConfig.alwaysRun || (cliCommand.indexOf('--release') > -1);
var recursiveFolderSearch = hookConfig.recursiveFolderSearch; // set this to false to manually indicate the folders to process
var foldersToProcess      = hookConfig.foldersToProcess; // add other www folders in here if needed (ex. js/controllers)
var cssMinifier           = new CleanCSS(hookConfig.cleanCssOptions);
var minifyImage           = new MinifyImage(hookConfig.imageminOptions);

// Exit
if (!isRelease) {
    return;
}

// Run uglifier
run();

/**
 * Run compression for all specified platforms.
 * @return {undefined}
 */
function run() {
    platforms.forEach(function(platform) {
        var wwwPath;

        switch (platform) {
            case 'android':
                wwwPath = path.join(platformPath, platform, 'assets', 'www');
                break;

            case 'ios':
            case 'browser':
            case 'wp8':
                wwwPath = path.join(platformPath, platform, 'www');
                break;

            default:
                console.log('this hook only supports android, ios, wp8, and browser currently');
                return;
        }

        processFolders(wwwPath);
    });
}

/**
 * Processes defined folders.
 * @param  {string} wwwPath - Path to www directory
 * @return {undefined}
 */
function processFolders(wwwPath) {
    processFiles(wwwPath);
}

/**
 * Processes files in directories.
 * @param  {string} dir - Directory path
 * @return {undefined}
 */
function processFiles(dir) {
    fs.readdir(dir, function (err, list) {
        if (err) {
            console.log('processFiles err: ' + err);

            return;
        }

        list.forEach(function(file) {
            file = path.join(dir, file);

            fs.stat(file, function(err, stat) {
                if (stat.isFile()) {
                    compress(file);

                    return;
                }

                if (recursiveFolderSearch && stat.isDirectory()) {
                    processFiles(file);

                    return;
                }
            });
        });
    });
}

/**
 * Compresses file.
 * @param  {string} file - File path
 * @return {undefined}
 */
function compress(file) {
    var ext = path.extname(file),
        res,
        source,
        result;

    if (file.toLowerCase().indexOf(".min.") != -1 || file.toLowerCase().indexOf("hls.js") != -1) {
		console.log('Skipping minification for ' + file);
        return;
	}
	
    switch (ext) {
        case '.js':
            console.log('uglifying js file ' + file);

            res = ngAnnotate(String(fs.readFileSync(file)), { add: true });
            result = UglifyJS.minify(res.src, hookConfig.uglifyJsOptions);
            fs.writeFileSync(file, result.code, 'utf8'); // overwrite the original unminified file
            break;

        case '.css':
            console.log('minifying css file ' + file);

            source = fs.readFileSync(file, 'utf8');
            result = cssMinifier.minify(source);
            fs.writeFileSync(file, result, 'utf8'); // overwrite the original unminified file
            break;

        default:
            console.log('encountered a ' + ext + ' file, not compressing it');
            break;
    }
}

/**
 * Constructor
 * @param {object} config - The hook config of image
 * @return {object} - MinifyImage instance
 */
function MinifyImage(config) {
    this.config = config || {};
    this.JPEG = 'JPEG';
    this.PNG = 'PNG';
    this.GIF = 'GIF';
    this.SVG = 'SVG';
    this.minify = minify;

    var that = this;

    /**
     * @param {string} file   - File path
     * @param {string} format - Image format
     * @return {undefined}
     * {@link https://github.com/imagemin/imagemin imagemin}
     */
    function minify(file, format) {
        switch (format) {
            case that.JPEG:
                new Imagemin()
                    .src(file)
                    .dest(path.dirname(file))
                    .use(Imagemin.jpegtran(that.config.jpeg))
                    .run(errorHandler);
                break;

            case that.PNG:
                new Imagemin()
                    .src(file)
                    .dest(path.dirname(file))
                    .use(Imagemin.optipng(that.config.png))
                    .run(errorHandler);
                break;

            case that.GIF:
                new Imagemin()
                    .src(file)
                    .dest(path.dirname(file))
                    .use(Imagemin.gifsicle(that.config.gif))
                    .run(errorHandler);
                break;

            case that.SVG:
                new Imagemin()
                    .src(file)
                    .dest(path.dirname(file))
                    .use(Imagemin.svgo(that.config.svg))
                    .run(errorHandler);
                break;

            default:
                console.log('encountered a ' + format + ' image, not compressing it');
                break;
        }

        // Error handler
        function errorHandler(err) {
            if (!err) {
                return;
            }

            console.error('Fail to minify image ' + file + ': ' + err);
        }
    }
}
