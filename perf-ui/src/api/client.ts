export class ApiError extends Error {
  readonly status: number;
  readonly statusText: string;
  readonly body: unknown;

  constructor(status: number, statusText: string, body: unknown) {
    super(`${status} ${statusText}`);
    this.name = 'ApiError';
    this.status = status;
    this.statusText = statusText;
    this.body = body;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let body: unknown;
    try {
      body = await response.json();
    } catch {
      body = await response.text();
    }
    throw new ApiError(response.status, response.statusText, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export async function get<T>(url: string): Promise<T> {
  const response = await fetch(url);
  return handleResponse<T>(response);
}

export async function post<T>(
  url: string,
  body?: unknown,
  contentType = 'application/json',
): Promise<T> {
  const headers: Record<string, string> = {};
  let serializedBody: string | undefined;

  if (body !== undefined) {
    headers['Content-Type'] = contentType;
    serializedBody = contentType === 'application/json' ? JSON.stringify(body) : String(body);
  }

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: serializedBody,
  });
  return handleResponse<T>(response);
}
