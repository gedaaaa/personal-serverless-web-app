const API_BASE_URL = import.meta.env.VITE_PUBLIC_API_BASE_URL || 'http://localhost:3000';

interface ApiError {
    status: number;
    message: string;
    details?: unknown;
}

export class ApiClient {
    private readonly baseUrl: string;

    constructor(baseUrl = API_BASE_URL) {
        this.baseUrl = baseUrl;
    }

    async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
        const url = `${this.baseUrl}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers,
        };

        try {
            const response = await fetch(url, { ...options, headers });

            if (!response.ok) {
                const error: ApiError = {
                    status: response.status,
                    message: response.statusText,
                };
                throw error;
            }

            return await response.json() as Promise<T>;
        } catch (error) {
            console.error('API request failed:', error);
            throw error;
        }
    }

    get<T>(endpoint: string) {
        return this.request<T>(endpoint, { method: 'GET' });
    }

    post<T>(endpoint: string, body: unknown) {
        return this.request<T>(endpoint, {
            method: 'POST',
            body: JSON.stringify(body),
        });
    }

    // TODO: PUT DELETE
}