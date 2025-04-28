export class ApiError<T> extends Error {
  constructor(
    public status: number,
    public message: string,
    public data: T,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}
