// AUTO-GENERATED — run `npm run generate:api` to regenerate from live backend.
// Do not edit manually; changes will be overwritten.

export interface components {
  schemas: {
    RoasterResponse: {
      id: string;
      name: string;
      websiteUrl: string | null;
      emailListUrl: string | null;
      active: boolean;
      moderationStatus: "PENDING" | "APPROVED" | "REJECTED";
      createdAt: string;
      updatedAt: string;
    };
    ProductResponse: {
      id: string;
      roasterId: string;
      roasterName: string;
      name: string;
      roastLevel: string | null;
      productType: string | null;
      originCountry: string | null;
      originRegion: string | null;
      process: string | null;
      brewMethods: string[];
      flavorProfile: string[];
      decaf: boolean;
      availabilityType: string | null;
      description: string | null;
      active: boolean;
      createdAt: string;
      updatedAt: string;
    };
    PageRoasterResponse: {
      content: components["schemas"]["RoasterResponse"][];
      totalElements: number;
      totalPages: number;
      size: number;
      number: number;
      first: boolean;
      last: boolean;
      empty: boolean;
    };
    PageProductResponse: {
      content: components["schemas"]["ProductResponse"][];
      totalElements: number;
      totalPages: number;
      size: number;
      number: number;
      first: boolean;
      last: boolean;
      empty: boolean;
    };
  };
}

export interface paths {
  "/api/roasters": {
    get: {
      parameters: {
        query?: {
          name?: string;
          activeOnly?: boolean;
          page?: number;
          size?: number;
          sort?: string;
        };
      };
      responses: {
        200: {
          content: {
            "application/json": components["schemas"]["PageRoasterResponse"];
          };
        };
      };
    };
  };
  "/api/roasters/{id}": {
    get: {
      parameters: {
        path: { id: string };
      };
      responses: {
        200: {
          content: {
            "application/json": components["schemas"]["RoasterResponse"];
          };
        };
      };
    };
  };
  "/api/products": {
    get: {
      parameters: {
        query?: {
          roasterId?: string;
          name?: string;
          activeOnly?: boolean;
          page?: number;
          size?: number;
          sort?: string;
        };
      };
      responses: {
        200: {
          content: {
            "application/json": components["schemas"]["PageProductResponse"];
          };
        };
      };
    };
  };
  "/api/products/{id}": {
    get: {
      parameters: {
        path: { id: string };
      };
      responses: {
        200: {
          content: {
            "application/json": components["schemas"]["ProductResponse"];
          };
        };
      };
    };
  };
}
