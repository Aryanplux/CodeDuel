FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ENV VITE_API_URL=/api
ENV VITE_WS_URL=/ws
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf/nginx.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80