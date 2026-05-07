import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "../api-types";

const client = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? "",
});

let getToken: (() => Promise<string>) | null = null;

export const setTokenGetter = (fn: (() => Promise<string>) | null) => {
  getToken = fn;
};

const authMiddleware: Middleware = {
  async onRequest({ request }) {
    if (getToken) {
      const token = await getToken().catch(() => null);
      if (token) request.headers.set("Authorization", `Bearer ${token}`);
    }
    return request;
  },
};

client.use(authMiddleware);

export default client;
