FROM node:22-alpine

WORKDIR /app

ARG APP_NAME=rider-web
ARG APP_PORT=3000

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    APP_NAME=${APP_NAME}

COPY package.json ./package.json
COPY tsconfig.base.json ./tsconfig.base.json
COPY apps ./apps
COPY packages ./packages

RUN npm install
RUN npm run build --workspace ${APP_NAME}

EXPOSE ${APP_PORT}

CMD ["sh", "-c", "npm run start --workspace ${APP_NAME}"]
