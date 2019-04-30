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

const pathRoot = process.cwd();
const pathCss = pathRoot + '/../';
const pathSass = pathRoot + '/scss/';
const filesSass = pathRoot + '/scss/*.scss';


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
      lintSass,
      buildSass('expanded')
    )
  );
};


gulp.task('default',
  gulp.series(
    lintSass,
    buildSass('expanded'),
    gulp.parallel(
      watchSass
    )
  )
);