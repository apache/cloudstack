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
const AntDesignThemePlugin = require('antd-theme-webpack-plugin')

function resolve (dir) {
  return path.join(__dirname, dir)
}

const options = {
  stylesDir: resolve('./src/style'),
  antDir: resolve('./node_modules/ant-design-vue'),
  varFile: resolve('./src/style/vars.less'),
  themeVariables: [
    '@logo-background-color',
    '@project-nav-background-color',
    '@project-nav-text-color',
    '@navigation-background-color',
    '@navigation-text-color',
    '@primary-color',
    '@link-color',
    '@link-hover-color',
    '@loading-color',
    '@success-color',
    '@warning-color',
    '@processing-color',
    '@error-color',
    '@heading-color',
    '@text-color',
    '@text-color-secondary',
    '@disabled-color',
    '@border-color-base',
    '@border-radius-base',
    '@box-shadow-base'
  ],
  indexFileName: 'index.html',
  publicPath: '.',
  generateOnce: false
}

const createThemeColorReplacerPlugin = () => new AntDesignThemePlugin(options)

module.exports = createThemeColorReplacerPlugin
