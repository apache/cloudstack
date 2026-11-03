<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

# index.less
- src/styles/index.less imports all necessary rules for cloudstack

# ant .less structure node_modules/ant-design-vue/
## main .less entry points:

1. dist/antd.less
    - imports everything with index.less + components.less
2. lib/style/index.less
    - themes/default.less
        - color/colors'
        - default theme @variables
    - core/index.less
        - includes base styles, motion rules and iconfont

# src/style/ explanation

- index.less includes ant styles, as well as all custom variables and rules

## folders:

1. variables
    - include all custom variables here
2. common
    - include all rules that reset styles, define global stuffs without classes at all
    - e.g. body {} p, ul, li {} h1, h2, h3 {}
3. ant-overwrite
    - any styles that overwrites the existing ant rules by any reason
    - e.g. classes like .ant-layout-header .anticon {}
4. frame
    - everything that belongs to the frame
    - e.g. header, footer, nav, sider, content (just the actual content frame, not every component in it)
5. layout
    - rules that modify the page at all if new layout class is set.
    - e.g. #html class="layout-ant-black"#
6. objects
    - repeatedly used elements like buttons, inputs
7. components
    - complex elements like dropdown, forms, table, search (usually include this to components/FooterToolbar/ folder)

# The "/deep/" combinator
- use the /deep/ combinator (or in other versions ">>>") helps us to exclude "scoped" rules into global
- e.g. <style scoped> .a .b .c {}</style> will scope a generated data ID like .a .b .c[data-abcde] {}
- but  <style scoped> .a /deep/ .b .c {} </style> will scope .a[data-abcde] .b .c {}
- so everything after deep will be outside the defined scope
- watch this article for technical information. https://vue-loader.vuejs.org/guide/scoped-css.html#child-component-root-elements
