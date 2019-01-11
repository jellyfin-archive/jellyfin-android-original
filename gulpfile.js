var gulp = require('gulp');
var gulpif = require('gulp-if');
var cleanCSS = require('gulp-clean-css');
var del = require('del');
var uglify = require('gulp-uglify');

// Check the NODE_ENV environment variable
var isDev = process.env.NODE_ENV === 'development';

// Skip minification for development builds or minified files
var compress = !isDev && [
    '**/*',
    '!**/*min.*',
    '!**/*hls.js'
];

var uglifyOptions = {
    compress: {
        drop_console: true
    }
};

var paths = {
    assets: {
        src: [
            'src/jellyfin-web/src/**/*',
            '!src/jellyfin-web/src/**/*.{js,css}'
        ],
        dest: 'www/'
    },
    scripts: {
        cordova: {
            src: 'src/cordova/**/*.js',
            dest: 'www/cordova/'
        },
        dashboard: {
            src: 'src/jellyfin-web/src/**/*.js',
            dest: 'www/'
        }
    },
    styles: {
        src: 'src/jellyfin-web/src/**/*.css',
        dest: 'www/'
    }
};

// Clean the www directory
function clean() {
    return del([
        'www',
        'platforms/android/assets/www'
    ]);
}

// Copy unmodified assets
function copy() {
    return gulp.src(paths.assets.src)
        .pipe(gulp.dest(paths.assets.dest));
}

// Uglify cordova scripts
function cordovaScripts() {
    return gulp.src(paths.scripts.cordova.src)
        .pipe(gulpif(compress, uglify(uglifyOptions)))
        .pipe(gulp.dest(paths.scripts.cordova.dest));
}
cordovaScripts.displayName = 'scripts:cordova';

// Uglify dashboard-ui scripts
function dashboardScripts() {
    return gulp.src(paths.scripts.dashboard.src)
        .pipe(gulpif(compress, uglify(uglifyOptions)))
        .pipe(gulp.dest(paths.scripts.dashboard.dest));
}
dashboardScripts.displayName = 'scripts:dashboard';

// Uglify scripts
var scripts = gulp.parallel(cordovaScripts, dashboardScripts);

// Uglify stylesheets
function styles() {
    return gulp.src(paths.styles.src)
        .pipe(gulpif(compress, cleanCSS()))
        .pipe(gulp.dest(paths.styles.dest));
}

// Default build task
var build = gulp.series(
    clean,
    gulp.parallel(copy, scripts, styles)
);

// Export tasks so they can be run individually
exports.clean = clean;
exports.copy = copy;
exports.scripts = scripts;
exports.styles = styles;
// Export default task
exports.default = build;
