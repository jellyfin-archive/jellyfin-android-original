var gulp = require('gulp');
var cleanCSS = require('gulp-clean-css');
var del = require('del');
var dom = require('gulp-dom');
var uglify = require('gulp-uglify');

var paths = {
    assets: {
        src: [
            'src/dashboard-ui/**/*',
            '!src/dashboard-ui/**/*.{js,css}'
        ],
        dest: 'www/'
    },
    scripts: {
        cordova: {
            src: 'src/cordova/**/*.js',
            dest: 'www/cordova/'
        },
        dashboard: {
            src: 'src/dashboard-ui/**/*.js',
            dest: 'www/'
        }
    },
    styles: {
        src: 'src/dashboard-ui/**/*.css',
        dest: 'www/'
    }
};

function clean() {
    return del([ 'www' ]);
}

function copy() {
    return gulp.src(paths.assets.src)
        .pipe(gulp.dest(paths.assets.dest))
}

function cordovaScripts() {
    return gulp.src(paths.scripts.cordova.src)
        .pipe(uglify())
        .pipe(gulp.dest(paths.scripts.cordova.dest));
}

function dashboardScripts() {
    return gulp.src(paths.scripts.dashboard.src)
        .pipe(uglify())
        .pipe(gulp.dest(paths.scripts.dashboard.dest));
}

var scripts = gulp.parallel(cordovaScripts, dashboardScripts);

function styles() {
    return gulp.src(paths.styles.src)
        .pipe(cleanCSS())
        .pipe(gulp.dest(paths.styles.dest));
}

var build = gulp.series(
    clean,
    gulp.parallel(copy, scripts, styles)
);

exports.clean = clean;
exports.copy = copy;
exports.scripts = scripts;
exports.styles = styles;

exports.default = build;
