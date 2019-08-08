# index.less
- src/styles/index.less imports all necessary rules for cloudstack

# ant .less structure node_modules/ant-design-vue/
## main .less entry points:

1. dist/antd.less
    - imports everthing with index.less + components.less
2. lib/style/index.less
    - themes/default.less
        - color/colors'
        - default theme @variables
    - core/index.less
        - includes base styles, motion rules and iconfont

# src/style/ explaination

- index.less includes ant styles, as well as all custom variables and rules

## folders:

1. variables
    - include all custom variables here
2. common
    - include all rules that reset styles, define global stuffs without classes at all
    - e.g. body {} p, ul, li {} h1, h2, h3 {}
3. ant-overwrite
    - any styles that overwrites the existin ant rules by any reason
    - e.g. classes like .ant-layout-header .anticon {}
4. frame
    - everything that belongs to the frame
    - e.g. header, footer, nav, sider, content (just the actual content frame, not every component in it)
5. layout
    - rules that modify the page at all if new layout class is set.
    - e.g. #html class="layout-ant-black"#
6. objects
    - repeatly used elements like buttons, inputs
7. components
    - complex elements like dropdown, forms, table, search (usualy include this to components/FooterToolbar/ folder)
