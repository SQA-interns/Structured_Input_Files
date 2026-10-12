FROM node:24.13.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN npm ci --ignore-scripts --no-audit --no-fund
COPY index.html tsconfig.json vite.config.ts vitest.config.ts playwright.config.ts ./
COPY src ./src
COPY tests ./tests
COPY e2e ./e2e
RUN npm run build

FROM nginx:1.30.5-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist /usr/share/nginx/html
RUN mkdir -p /tmp/nginx && chown -R nginx:nginx /tmp/nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
