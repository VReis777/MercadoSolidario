# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ARG VITE_ENABLE_VISUAL_PREVIEW=false
ARG VITE_API_URL=
ENV VITE_ENABLE_VISUAL_PREVIEW=$VITE_ENABLE_VISUAL_PREVIEW
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
