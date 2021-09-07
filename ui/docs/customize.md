# UI customization
Use a `public/config.json` (or `dist/config.json` after build) file for customizing theme, logos,...

## Images
Change the image of the logo, login banner, error page, etc.
```json
{
  "logo": "assets/logo.svg",
  "banner": "assets/banner.svg",
  "error": {
    "404": "assets/404.png",
    "403": "assets/403.png",
    "500": "assets/500.png"
  }
}
```

- `logo` changes the logo top-left side image.
- `banner` changes the login banner image.
- `error.404` change the image of error Page not found.
- `error.403` change the image of error Forbidden.
- `error.500` change the image of error Internal Server Error.

## Theme
Customize themes like colors, border color, etc.
```json
{
  "theme": {
    "@primary-color": "#1890ff",
    "@success-color": "#52c41a",
    "@processing-color": "#1890ff",
    "@warning-color": "#faad14",
    "@error-color": "#f5222d",
    "@font-size-base": "14px",
    "@heading-color": "rgba(0, 0, 0, 0.85)",
    "@text-color": "rgba(0, 0, 0, 0.65)",
    "@text-color-secondary": "rgba(0, 0, 0, 0.45)",
    "@disabled-color": "rgba(0, 0, 0, 0.25)",
    "@border-color-base": "#d9d9d9",
    "@logo-width": "256px",
    "@logo-height": "64px",
    "@banner-width": "700px",
    "@banner-height": "110px",
    "@error-width": "256px",
    "@error-height": "256px"
  }
}
```

- `@logo-background-color` changes the logo background color.
- `@project-nav-background-color` changes the navigation menu background color of the project .
- `@project-nav-text-color` changes the navigation menu background color of the project view.
- `@navigation-background-color` changes the navigation menu background color.
- `@navigation-text-color` changes the navigation text color.
- `@primary-color` change the major background color of the page (background button, icon hover, etc).
- `@link-color` changes the link color.
- `@link-hover-color` changes the link hover color.
- `@loading-color` changes the message loading color and page loading bar at the top page.
- `@success-color` change success state color.
- `@processing-color` change processing state color. Exp: progress status.
- `@warning-color` change warning state color.
- `@error-color` change error state color.
- `@heading-color` change table header color.
- `@text-color` change in major text color.
- `@text-color-secondary` change of secondary text color (breadcrumb icon).
- `@disabled-color` change disable state color (disabled button, switch, etc).
- `@border-color-base` change in major border color.
- `@logo-width` change the width of the logo top-left side.
- `@logo-height` change the height of the logo top-left side.
- `@banner-width` changes the width of the login banner.
- `@banner-height` changes the height of the login banner.
- `@error-width` changes the width of the error image.
- `@error-height` changes the height of the error image.

Assorted primary theme colours:

- Blue: #1890FF
- Red: #F5222D
- Yellow: #FAAD14
- Cyan: #13C2C2
- Green: #52C41A
- Purple: #722ED1

Also, to add other properties, we can add new properties into `theme.config.js` based on the Ant Design Vue Less variable.
Refer: https://www.antdv.com/docs/vue/customize-theme/#Ant-Design-Vue-Less-variables
