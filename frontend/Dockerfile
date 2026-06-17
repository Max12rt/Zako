FROM node:20-alpine AS build
WORKDIR /app
ENV NODE_ENV=development
RUN npm config set strict-ssl false
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN node_modules/.bin/ng build --configuration production

FROM nginx:1.27-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
