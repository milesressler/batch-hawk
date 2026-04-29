import client from "./client";
import type { components } from "../api-types";

export type Product = components["schemas"]["ProductResponse"];
export type ProductPage = components["schemas"]["PageProductResponse"];

interface ListParams {
  roasterId?: string;
  name?: string;
  activeOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

const list = async (params?: ListParams): Promise<ProductPage> => {
  const { data, error } = await client.GET("/api/products", { params });
  if (error) throw error;
  return data!;
};

const getById = async (id: string): Promise<Product> => {
  const { data, error } = await client.GET("/api/products/{id}", {
    params: { path: { id } },
  });
  if (error) throw error;
  return data!;
};

export default { list, getById };
