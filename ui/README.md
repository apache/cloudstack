# CloudStack Primate

A progressive modern CloudStack Admin UI based on VueJS and Ant Design.

Install tools and dependencies:

    sudo apt-get install npm
    sudo npm i -g npm@next
    sudo npm i -g npm-check-updates
    ncu -u # optional: upgrade dependencies
    npm install

Build and run:

    npm run serve

Production Build:

    npm run build

Upgrade dependencies:


Run Tests:

    npm run test
    npm run lint
    npm run test:unit

Fix issues and vulnerabilities:

    npm audit

## History

The project was created by Rohit Yadav over several weekends during late 2018.
The base app layout was referenced from [Ant Design Pro
Vue](https://github.com/sendya/ant-design-pro-vue).

### Env and dependencies

- node
- webpack
- eslint
- @vue/cli ~3
- [ant-design-vue](https://github.com/vueComponent/ant-design-vue) - Ant Design Of Vue
- [vue-cropper](https://github.com/xyxiao001/vue-cropper) - Picture edit
- [@antv/g2](https://antv.alipay.com/zh-cn/index.html) - AntV G2
- [Viser-vue](https://viserjs.github.io/docs.html#/viser/guide/installation)  - Antv/G2 of Vue
- [Fontawesome](https://github.com/FortAwesome/vue-fontawesome)

### Other

- [Vue-cli3](https://cli.vuejs.org/guide/) used by the project.
- Disable Eslint (not recommended): remove `eslintConfig`  field in `package.json`

- Easy-Mock used by project，[easy-mock](https://www.easy-mock.com/)  Project API Data [DO NOT CHANGE THE INTERFACE](https://www.easy-mock.com/project/5b7bce071f130e5b7fe8cd7d)，If you want to modify, please fork [ANTD-PRO-Easy-Mock-API.zip](https://github.com/sendya/ant-design-pro-vue/files/2682711/ANTD-PRO-Easy-Mock-API.zip) and running to your server.

- Load on Demand: modify `/src/main.js` L7,  append `import './core/lazy_use'` code.

- Customize Theme:  `vue.config.js`
eg:
```ecmascript 6
  css: {
    loaderOptions: {
      less: {
        modifyVars: {
          /* Less variables, required modifyVars*/

          'primary-color': '#F5222D',
          'link-color': '#F5222D',
          'border-radius-base': '4px',
        },
        javascriptEnabled: true,
      }
    }
  }
```

### Docs

- [Router and Menu](https://github.com/sendya/ant-design-pro-vue/blob/master/src/router/README.md)
- [Table](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/table/README.md) [@Saraka](https://github.com/saraka-tsukai)
- [ANTD DefaultConfig](https://github.com/sendya/ant-design-pro-vue/blob/master/src/defaultSettings.js)
- [Frist page loading animate](https://github.com/sendya/ant-design-pro-vue/blob/master/docs/add-page-loading-animate.md)
- [Multi-Tabs feature/multi-tabs](https://github.com/sendya/ant-design-pro-vue/tree/feature/multi-tabs) [How to remove](https://github.com/sendya/ant-design-pro-vue/blob/master/docs/multi-tabs.md)
- [LoadOnDemand Demo feature/demand_load](https://github.com/sendya/ant-design-pro-vue/tree/feature/demand_load)
- [LoadOnDemand Docs](https://github.com/sendya/ant-design-pro-vue/blob/master/docs/load-on-demand.md)
- [i18n feature/lang](https://github.com/sendya/ant-design-pro-vue/tree/feature/lang)  Creator [@musnow](https://github.com/musnow)
- [Dependency analysis tool: analyzer](https://github.com/sendya/ant-design-pro-vue/blob/master/docs/webpack-bundle-analyzer.md)
- ANTD PRO Components:
  - Trend [Trend.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/Trend/index.md)
  - AvatarList [AvatarList.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/AvatarList/index.md)
  - CountDown [CountDown.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/CountDown/index.md)
  - Ellipsis [Ellipsis.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/Ellipsis/index.md)
  - NumberInfo [NumberInfo.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/NumberInfo/index.md)
  - FooterToolbar [FooterToolbar.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/FooterToolbar/index.md)
  - IconSelector [IconSelector.md](https://github.com/sendya/ant-design-pro-vue/blob/master/src/components/IconSelector/README.md) Creator: [@Saraka](https://github.com/saraka-tsukai)
