FROM node:20-alpine AS build
WORKDIR /app
# package-lock.json е дел од копирањето: npm ci гради од точните верзии
# од lock фајлот, па имиџот е ист денес и за шест месеци
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
