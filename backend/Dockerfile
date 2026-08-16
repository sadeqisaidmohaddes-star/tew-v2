# Two stages so the shipped image carries no compiler and no dev dependencies.
# The target is a shared VPS also running Postgres and whisper.cpp — memory
# spent on a toolchain is memory the ASR pass does not get.

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY tsconfig.json ./
COPY src ./src
COPY test ./test
RUN npm run build

FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production

COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/dist ./dist
COPY migrations ./migrations

# Run unprivileged. The node image already provides this user.
USER node

EXPOSE 8080

# The app refuses to start in production without DATABASE_URL and with the
# stub token verifier — see src/server.ts and src/auth/verifier.ts. Both are
# deliberate: failing to boot is better than silently accepting any token or
# losing every memo on restart.
CMD ["node", "dist/src/server.js"]
