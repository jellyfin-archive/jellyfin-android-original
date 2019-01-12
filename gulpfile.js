/**
 * gulpfile.js
 * Part of the Jellyfin project (https://jellyfin.media)
 *
 *    This Source Code Form is subject to the terms of the Mozilla Public
 *    License, v. 2.0. If a copy of the MPL was not distributed with this file,
 *    You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 *    All copyright belongs to the Jellyfin contributors; a full list can
 *    be found in the file CONTRIBUTORS.md
 *
 *    Alternatively, the contents of this file may be used under the terms
 *    of the GNU General Public License Version 2 or later, as described below:
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
