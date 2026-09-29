# ---- build stage: compile the TypeScript/React app into static files --------------
FROM node:22-alpine AS build
WORKDIR /app
# Copy manifests first so `npm ci` is cached until dependencies actually change.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- runtime stage: nginx serves /dist and reverse-proxies the API to the gateway ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
# nginx:alpine's default CMD already runs nginx in the foreground.
