# Batch Hawk — Web

React + TypeScript + Vite frontend for Batch Hawk.

## Setup

```bash
npm install
```

## Development

```bash
npm run dev     # starts Vite dev server at http://localhost:5173
npm run build   # type-check + production build
npm run lint
```

Set `VITE_API_BASE_URL` in a `.env.local` file to point at the API:

```
VITE_API_BASE_URL=http://localhost:8080
```

## API Types

TypeScript types for the API are generated from the OpenAPI spec. The generated
file `src/api-types.ts` is committed — do not edit it manually.

To regenerate after backend API changes:

```bash
# Preferred: boots testcontainers, generates spec, then generates types
./gradlew :api:generateClientTypes

# Quick alternative if the backend is already running locally
npm run fetch:spec
```

See `src/services/` for the typed API client and per-resource service files.
