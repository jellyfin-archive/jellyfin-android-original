var del = require('del');
var gulp = require('gulp');

// Check the NODE_ENV environment variable
var isDev = process.env.NODE_ENV === 'development';

// Web client to bundle with cordova
var webSource = isDev ? '../jellyfin-web/dist' : 'src/jellyfin-web';

var cleanOptions = {
    // Do not rebase relative urls
    // Otherwise asset urls are rewritten to be relative to the current src
    rebase: false
};

var paths = {
    web: {
        src: webSource + '/**/*',
        dest: 'www/'
    },
    plugins: {
        src: 'src/cordova/**/*.js',
        dest: 'www/cordova/'
    }
};

// Clean the www directory
function clean() {
    return del([
        'www',
        'platforms/android/assets/www'
    ]);
}

// Copy web client
function copyWeb() {
    return gulp.src(paths.web.src)
        .pipe(gulp.dest(paths.web.dest));
}

// Copy web plugins
function copyPlugins() {
    return gulp.src(paths.plugins.src)
        .pipe(gulp.dest(paths.plugins.dest));
}

// Export tasks so they can be run individually
exports.clean = clean;
exports.copyWeb = copyWeb;
exports.copyPlugins = copyPlugins;

// Default build task
var build = gulp.series(clean, copyWeb, copyPlugins);

// Export default task
exports.default = build;
