# Multiple Management Server
Use file `public/config.json` (or `dist/config.json` after build) to configure the settings which allow Cloudstack to support multiple servers.

## Setting `config.json`
```
"servers": [
    {
      "name": "server01",
      "apiHost": "/server01",
      "apiBase": "/client/api"
    },
    {
      "name": "server02",
      "apiHost": "",
      "apiBase": "/client/api"
    }
  ],
"multipleServer": true
```
- `multipleServer` configure to allow Cloudstack to support multiple servers.
- `servers` list of servers to which Cloudstack can connect.

## Nginx config
Example: Use following settings in `config.json` so that user's GUI can work with different servers (to be put into /etc/nginx/conf.d/default/conf or similar):

```
server {
    listen          80;
    server_name     localhost;

    location / {
        # /src/ui/dist contains the built UI webpack
        root        /src/ui/dist;
        index       index.html;
    }

    # for apiHost of server01 located in config.json
    location /server01/client/ {
        rewrite ^/server01/(.*)$ /$1 break;
        # server's actual URI
        proxy_pass   https://server01.your.domain;
    }

    # for apiHost of server02 located in config.json
    location /client/ {
        # server's actual URI
        proxy_pass   https://server02.your.domain;
    }
}
```
