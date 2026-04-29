import client from "./client";
import type { components } from "../api-types";

export type Roaster = components["schemas"]["RoasterResponse"];
export type RoasterPage = components["schemas"]["PageRoasterResponse"];

interface ListParams {
  name?: string;
  activeOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

const list = async (params?: ListParams): Promise<RoasterPage> => {
  const { data, error } = await client.GET("/api/roasters", { params });
  if (error) throw error;
  return data!;
};

const getById = async (id: string): Promise<Roaster> => {
  const { data, error } = await client.GET("/api/roasters/{id}", {
    params: { path: { id } },
  });
  if (error) throw error;
  return data!;
};

export default { list, getById };
