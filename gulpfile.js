var gulp = require('gulp');
var gulpif = require('gulp-if');
var del = require('del');
var dom = require('gulp-dom');

// Allow jellyfin-web directory override
var WEB_DIR = process.env.JELLYFIN_WEB_DIR || 'node_modules/jellyfin-web/dist';
console.info('using jellyfin-web from', WEB_DIR);

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
function index() {
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

function scripts() {
    return gulp.src(paths.scripts.src)
        .pipe(gulp.dest(paths.scripts.dest));
}

exports.clean = clean;
exports.copy = copy;
exports.index = index;
exports.scripts = scripts;

exports.default = gulp.series(clean, gulp.parallel(copy, index, scripts));
