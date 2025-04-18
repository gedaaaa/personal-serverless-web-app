import memoService, {
  type Memo,
  type GetMemoListFilter,
} from '../_services/memo-service';
import _ from 'lodash';

export const store: {
  memos: Memo[];
  error: string | undefined;
  filter: GetMemoListFilter;
  isFetchingList: boolean;
  hasMore: boolean;
  listCursor: string | undefined;
  itemLoadingStatus: Record<string, boolean>;
} = $state({
  memos: [],
  error: undefined,
  filter: { isCompleted: false },
  isFetchingList: false,
  hasMore: true,
  listCursor: undefined,
  itemLoadingStatus: {},
});

/**
 * Fetches memos.
 * @param limit - The limit of the memos.
 */
export async function fetchMemos(limit: number = 10) {
  // Store values in local variables to use in closure
  const currentCursor = store.listCursor;
  const currentFilter = { ...store.filter };

  // Avoid concurrent fetches
  if (store.isFetchingList) {
    return;
  }

  store.isFetchingList = true;
  store.error = undefined;

  try {
    const response = await memoService.getMemos(
      limit,
      currentCursor,
      currentFilter,
    );

    // Check if filter has changed since request started - if so, discard results
    if (JSON.stringify(store.filter) !== JSON.stringify(currentFilter)) {
      // Keep fetching true if a new fetch was likely triggered by applyFilter
      return;
    }

    const newMemos = response.items ?? [];

    store.memos = currentCursor ? [...store.memos, ...newMemos] : newMemos;
    store.listCursor = response.nextCursor;
    store.hasMore = response.hasMore;
    store.isFetchingList = false;
  } catch {
    store.error = 'Failed to load memos. Please try again.';
    store.isFetchingList = false; // Ensure fetching state is reset on error
  }
}

/**
 * Applies a filter to the memos.
 * @param newFilter - The new filter.
 */
export function applyFilter(newFilter: GetMemoListFilter) {
  store.filter = newFilter;
  store.memos = []; // Clear existing memos
  store.listCursor = undefined; // Reset cursor for new filter
  store.hasMore = true; // Assume there's data for the new filter
  store.isFetchingList = false; // Reset fetching state before triggering new fetch
  store.error = undefined;

  // Fetch immediately after applying the filter
  fetchMemos();
}

/**
 * Updates a memo in the list.
 * @param id - The id of the memo.
 * @param updatedData - The updated data.
 */
export function updateMemoInList(id: string, updatedData: Partial<Memo>) {
  store.memos = _.chain(store.memos)
    .map((memo) => (memo.id === id ? { ...memo, ...updatedData } : memo))
    .filter(
      (memo) =>
        // If no filter is applied, show all memos
        _.isEmpty(store.filter) ||
        // If a filter is applied, only show memos that match the filter
        store.filter.isCompleted === memo.isCompleted,
    )
    .value();
}

/**
 * Removes a memo from the list.
 * @param id - The id of the memo.
 */
export function removeMemoFromList(id: string) {
  store.memos = store.memos.filter((memo) => memo.id !== id);
}

/**
 * Sets the error.
 * @param message - The message.
 */
export function setError(message: string | undefined) {
  store.error = message;
  // Consider adding automatic clear timeout here if needed later
}

/**
 * Sets the item loading status.
 * @param id - The id of the item.
 * @param isLoading - The loading status.
 */
export function setItemLoadingStatus(id: string, isLoading: boolean) {
  store.itemLoadingStatus = { ...store.itemLoadingStatus, [id]: isLoading };
}

/**
 * Tries to fetch the next memo.
 */
export async function tryFetchNextMemo() {
  const lastMemo = store.memos[store.memos.length - 1];
  store.listCursor = lastMemo ? lastMemo.id : undefined;
  await fetchMemos(1);
}
