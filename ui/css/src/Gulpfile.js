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