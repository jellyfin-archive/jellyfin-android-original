var gulp = require('gulp');
var gulpif = require('gulp-if');
var cleanCSS = require('gulp-clean-css');
var del = require('del');
var dom = require('gulp-dom');
var uglifyes = require('uglify-es');
var composer = require('gulp-uglify/composer');
var uglify = composer(uglifyes, console);

// Check the NODE_ENV environment variable
var isDev = process.env.NODE_ENV === 'development';
// Allow overriding of jellyfin-web directory
var WEB_DIR = process.env.JELLYFIN_WEB_DIR || 'node_modules/jellyfin-web/dist';
console.info('Using jellyfin-web from', WEB_DIR);

// Skip minification for development builds or minified files
var compress = !isDev && [
    '**/*',
    '!**/*min.*',
    '!**/*hls.js',
    // Temporarily exclude apiclient until updated
    '!bower_components/emby-apiclient/**/*.js'
];

var uglifyOptions = {
    compress: {
        drop_console: true
    }
};

var cleanOptions = {
    // Do not rebase relative urls
    // Otherwise asset urls are rewritten to be relative to the current src
    rebase: false
};

var paths = {
    assets: {
        src: [
            WEB_DIR + '/**/*',
            '!' + WEB_DIR + '/**/*.{js,css}',
            '!' + WEB_DIR + '/index.html'
        ],
        dest: 'www/'
    },
    index: {
        src: WEB_DIR + '/index.html',
        dest: 'www/'
    },
    scripts: {
        cordova: {
            src: 'src/cordova/**/*.js',
            dest: 'www/cordova/'
        },
        dashboard: {
            src: WEB_DIR + '/**/*.js',
            dest: 'www/'
        }
    },
    styles: {
        src: WEB_DIR + '/**/*.css',
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

// Add required tags to index.html
function modifyIndex() {
    return gulp.src(paths.index.src)
        .pipe(dom(function() {
            // inject CSP meta tag
            var meta = this.createElement('meta');
            meta.setAttribute('http-equiv', 'Content-Security-Policy');
            meta.setAttribute('content', 'default-src * \'self\' \'unsafe-inline\' \'unsafe-eval\' data: gap: file: filesystem: ws: wss:;');
            this.head.appendChild(meta);

            // inject appMode script
            var appMode = this.createElement('script');
            appMode.text = 'window.appMode=\'android\';';
            this.body.appendChild(appMode);

            // inject cordova.js
            var cordova = this.createElement('script');
            cordova.setAttribute('src', 'cordova.js');
            cordova.setAttribute('defer', '');
            this.body.appendChild(cordova);

            // inject apploader.js
            var apploader = this.createElement('script');
            apploader.setAttribute('src', 'scripts/apploader.js');
            apploader.setAttribute('defer', '');
            this.body.appendChild(apploader);

            return this;
        }))
        .pipe(gulp.dest(paths.index.dest))
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
        .pipe(gulpif(compress, cleanCSS(cleanOptions)))
        .pipe(gulp.dest(paths.styles.dest));
}

// Default build task
var build = gulp.series(
    clean,
    gulp.parallel(copy, modifyIndex, scripts, styles)
);

// Export tasks so they can be run individually
exports.clean = clean;
exports.copy = copy;
exports.modifyIndex = modifyIndex;
exports.scripts = scripts;
exports.styles = styles;
// Export default task
exports.default = build;
