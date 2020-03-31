var gulp = require('gulp');
var gulpif = require('gulp-if');
var del = require('del');
var dom = require('gulp-dom');
var terser = require('terser');
var composer = require('gulp-uglify/composer');
var tersify = composer(terser, console);

// Check the NODE_ENV environment variable
var isDev = process.env.NODE_ENV === 'development';

// Allow overriding of jellyfin-web directory
var WEB_DIR = process.env.JELLYFIN_WEB_DIR || 'node_modules/jellyfin-web/dist';
console.info('using jellyfin-web from', WEB_DIR);

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

var paths = {
    assets: {
        src: [
            WEB_DIR + '/**/*',
            '!' + WEB_DIR + '/index.html'
        ],
        dest: 'www/'
    },
    index: {
        src: WEB_DIR + '/index.html',
        dest: 'www/'
    },
    scripts: {
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
            meta.setAttribute('content', 'default-src * \'self\' \'unsafe-inline\' \'unsafe-eval\' blob: data: gap: file: filesystem: ws: wss:;');
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
function scripts() {
    return gulp.src(paths.scripts.src)
        .pipe(gulpif(compress, tersify(uglifyOptions)))
        .pipe(gulp.dest(paths.scripts.dest));
}

// Default build task
var build = gulp.series(
    clean,
    gulp.parallel(copy, modifyIndex, scripts)
);

// Export tasks so they can be run individually
exports.clean = clean;
exports.copy = copy;
exports.modifyIndex = modifyIndex;
exports.scripts = scripts;
// Export default task
exports.default = build;
