export default interface PagedResponse<T> {
  items?: T[];
  nextCursor?: string;
  hasMore: boolean;
}
