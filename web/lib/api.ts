// Calls the real Rota API (docs/contracts/openapi/rota.yaml). No mock data anywhere.
// The pure helpers (resolveApiBase, actionPath, errorMessage) are separate from fetch so they
// can be unit-tested.

import type { ApiErrorBody, Roster } from "./types";

export const DEFAULT_API_BASE = "http://localhost:8080";

export type DemoAction = "solve-mini" | "solve-tight" | "repair-sick";

export const ACTIONS: { action: DemoAction; label: string }[] = [
  { action: "solve-mini", label: "Solve mini" },
  { action: "solve-tight", label: "Solve tight" },
  { action: "repair-sick", label: "Sick repair" },
];

/** Configured base URL without trailing slash; blank or missing means the default. */
export function resolveApiBase(configured: string | undefined): string {
  const value = configured?.trim();
  return (value ? value : DEFAULT_API_BASE).replace(/\/+$/, "");
}

/** NEXT_PUBLIC_API_BASE is inlined by Next.js at build time (must be referenced literally). */
export function apiBase(): string {
  return resolveApiBase(process.env.NEXT_PUBLIC_API_BASE);
}

export function actionPath(action: DemoAction): string {
  switch (action) {
    case "solve-mini":
      return "/api/rosters/solve?fixture=mini";
    case "solve-tight":
      return "/api/rosters/solve?fixture=tight";
    case "repair-sick":
      return "/api/rosters/repair?fixture=sick";
  }
}

function isApiErrorBody(body: unknown): body is ApiErrorBody {
  return (
    typeof body === "object" &&
    body !== null &&
    typeof (body as ApiErrorBody).error === "string" &&
    typeof (body as ApiErrorBody).message === "string"
  );
}

/** Readable message for a non-2xx response: the API's {error, message} if present. */
export function errorMessage(status: number, body: unknown): string {
  if (isApiErrorBody(body)) {
    return `${status} ${body.error}: ${body.message}`;
  }
  return `HTTP ${status}`;
}

export class ApiRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ApiRequestError";
  }
}

/** POSTs the action to the API and returns the stored roster. Throws ApiRequestError. */
export async function runAction(action: DemoAction, signal?: AbortSignal): Promise<Roster> {
  const base = apiBase();
  let response: Response;
  try {
    response = await fetch(base + actionPath(action), {
      method: "POST",
      headers: { Accept: "application/json" },
      signal,
    });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === "AbortError") {
      throw cause;
    }
    throw new ApiRequestError(
      `Cannot reach the API at ${base}. Is it running (scripts/run-api.ps1)? ` +
        "If it is, check that it allows this page's origin (rota.cors.allowed-origins).",
    );
  }
  let body: unknown = null;
  try {
    body = await response.json();
  } catch {
    body = null;
  }
  if (!response.ok) {
    throw new ApiRequestError(errorMessage(response.status, body));
  }
  if (body === null || typeof body !== "object") {
    throw new ApiRequestError(`HTTP ${response.status}: response is not JSON`);
  }
  return body as Roster;
}
