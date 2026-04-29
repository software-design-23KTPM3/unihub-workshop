FROM openresty/openresty:alpine-fat

RUN luarocks install lua-resty-jwt && \
    luarocks install lua-resty-http

COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

RUN mkdir -p /etc/nginx/lua
COPY rate_limit.lua /etc/nginx/lua/rate_limit.lua