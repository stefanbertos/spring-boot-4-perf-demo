FROM node:22-alpine AS build
WORKDIR /app

# Copy workspace root and both packages for npm workspaces
COPY package.json ./
COPY perf-ui-components/package.json perf-ui-components/
COPY perf-ui/package.json perf-ui/
RUN npm install

# Build component library first
COPY perf-ui-components/ perf-ui-components/
RUN npm -w perf-ui-components run build

# Build the UI
COPY perf-ui/ perf-ui/
RUN npm -w perf-ui run build

FROM caddy:2-alpine
COPY --from=build /app/perf-ui/dist /srv
COPY perf-ui/Caddyfile /etc/caddy/Caddyfile
EXPOSE 80
