// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

const path = require('path')
const webpack = require('webpack')
const fs = require('fs')
const packageJson = fs.readFileSync('./package.json')
const version = JSON.parse(packageJson).version || 'main'
const createThemeColorReplacerPlugin = require('./theme.config')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')

function resolve (dir) {
  return path.join(__dirname, dir)
}

// vue.config.js
const vueConfig = {
  publicPath: './',
  /*
    Vue-cli3:
    Crashed when using Webpack `import()` #2463
    https://github.com/vuejs/vue-cli/issues/2463

   */
  /*
  pages: {
    index: {
      entry: 'src/main.js',
      chunks: ['chunk-vendors', 'chunk-common', 'index']
    }
  },
  */
  configureWebpack: {
    plugins: [
      // Ignore all locale files of moment.js
      new webpack.IgnorePlugin(/^\.\/locale$/, /moment$/),
      new webpack.IgnorePlugin(/@antv\/g2/),
      new webpack.DefinePlugin({
        'process.env': {
          PACKAGE_VERSION: '"' + version + '"'
        }
      })
    ],
    optimization: {
      minimizer: [
        new UglifyJsPlugin({
          cache: true,
          parallel: true,
          uglifyOptions: {
            compress: false,
            ecma: 6,
            mangle: true
          },
          sourceMap: true
        })
      ],
      splitChunks: {
        cacheGroups: {
          commons: {
            test: /[\\/]node_modules[\\/]/,
            name: 'vendors',
            chunks: 'all'
          }
        }
      }
    }
  },

  chainWebpack: (config) => {
    config.resolve.alias
      .set('@public', resolve('public'))
      .set('@$', resolve('src'))
      .set('@api', resolve('src/api'))
      .set('@assets', resolve('src/assets'))
      .set('@comp', resolve('src/components'))
      .set('@views', resolve('src/views'))
      .set('@layout', resolve('src/layout'))
      .set('@static', resolve('src/static'))

    // do not emit errors as a warning
    config.module.rule('eslint').use('eslint-loader').tap(
      opts => ({ ...opts, emitWarning: false })
    )

    const svgRule = config.module.rule('svg')
    svgRule.uses.clear()

    svgRule
      .use('vue-loader')
      .loader('vue-loader')
      .end()
      .use('vue-svg-loader')
      .loader('vue-svg-loader')

    /* svgRule.oneOf('inline')
      .resourceQuery(/inline/)
      .use('vue-svg-loader')
      .loader('vue-svg-loader')
      .end()
      .end()
      .oneOf('external')
      .use('file-loader')
      .loader('file-loader')
      .options({
        name: 'assets/[name].[hash:8].[ext]'
      })
    */
  },

  css: {
    loaderOptions: {
      less: {
        modifyVars: {
          // https://ant.design/docs/spec/colors
          // https://vue.ant.design/docs/vue/customize-theme/
          'root-entry-name': 'default'
        },
        javascriptEnabled: true
      }
    }
  },

  devServer: {
    port: 5050,
    proxy: {
      '/client': {
        target: process.env.CS_URL || 'http://localhost:8080',
        secure: false,
        ws: false,
        changeOrigin: true,
        proxyTimeout: 10 * 60 * 1000 // 10 minutes
      }
    },
    https: process.env.HTTPS_KEY ? {
      key: process.env.HTTPS_KEY ? fs.readFileSync(process.env.HTTPS_KEY) : undefined,
      cert: process.env.HTTPS_CERT ? fs.readFileSync(process.env.HTTPS_CERT) : undefined,
      ca: process.env.HTTPS_CA ? fs.readFileSync(process.env.HTTPS_CA) : undefined,
      dhparam: process.env.HTTPS_DHPARAM ? fs.readFileSync(process.env.HTTPS_DHPARAM) : undefined
    } : false,
    public: process.env.PUBLIC_HOST || undefined,
    allowedHosts: process.env.ALLOWED_HOSTS ? JSON.parse(process.env.ALLOWED_HOSTS) : undefined
  },

  lintOnSave: undefined,

  // babel-loader no-ignore node_modules/*
  transpileDependencies: [],

  pluginOptions: {
    i18n: {
      locale: 'en',
      fallbackLocale: 'en',
      localeDir: 'locales',
      enableInSFC: true
    }
  }
}

vueConfig.configureWebpack.plugins.push(createThemeColorReplacerPlugin())

module.exports = vueConfig
