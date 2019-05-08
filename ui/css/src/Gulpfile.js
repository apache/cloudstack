/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

'use strict';

const Fiber = require('fibers');
const gulp = require('gulp');
const sass = require('gulp-sass');
const sassLint = require('gulp-sass-lint');
const sourcemaps = require('gulp-sourcemaps');
const autoprefixer = require('gulp-autoprefixer');
const shell = require('gulp-shell');

const pathRoot = process.cwd();
const pathCss = pathRoot + '/../';
const pathSass = pathRoot + '/scss/';
const filesSass = pathRoot + '/scss/*.scss';
const browserVersions = [
  "last 1 versions",
  "last 20 firefox versions",
  "last 20 chrome versions",
  "last 5 opera versions",
  "ie >= 9",
  "last 5 edge versions",
  "safari >= 9",
  "last 3 ios versions",
  "last 5 android versions",
  "last 5 ie_mob versions",
  "last 5 and_chr versions"
];


gulp.task('lintSassFix',
  shell.task('npm run fix')
);

const buildSass = (style) => {
  const buildSass = () => { // function and name is required here for gulp-task naming-process
    return gulp.src(filesSass)
      .pipe(sourcemaps.init())
      .pipe(
        sass({
          fiber: Fiber,
          outputStyle: style
        })
        .on('error', sass.logError))
      .pipe(autoprefixer({
        browsers: browserVersions, //todo remove all current prefix rules from css
        cascade: false // prefix indentation in one line?
      }))
      .pipe(sourcemaps.write('./src/sourcemaps'))
      .pipe(gulp.dest(pathCss));
  }

  return buildSass;
};

const lintSass = () => {
  return gulp.src(pathSass + '**/*.scss')
    .pipe(sassLint())
    .pipe(sassLint.format());
};

const watchSass = () => {
  gulp.watch(pathSass + '**/*.scss',
    gulp.series(
      'lintSassFix',
      lintSass,
      buildSass('expanded')
    )
  );
};


gulp.task('default',
  gulp.series(
    'lintSassFix',
    lintSass,
    buildSass('expanded'),
    gulp.parallel(
      watchSass
    )
  )
);