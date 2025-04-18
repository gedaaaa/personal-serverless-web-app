import { writable } from 'svelte/store';
import memoService, {
  type Memo,
  type GetMemoListFilter,
} from '../_services/memo-service';
import _ from 'lodash';

interface MemoStoreState {
  memos: Memo[];
  error: string | undefined;
  filter: GetMemoListFilter;
  isFetchingList: boolean;
  hasMore: boolean;
  listCursor: string | undefined;
  itemLoadingStatus: Record<string, boolean>;
}

// --- Initial State ---
const initialState: MemoStoreState = {
  memos: [],
  error: undefined,
  filter: { isCompleted: false },
  isFetchingList: false,
  hasMore: true,
  listCursor: undefined,
  itemLoadingStatus: {},
};

// --- Store Creation ---
function createMemoStore() {
  const { subscribe, update } = writable<MemoStoreState>(initialState);

  // --- Actions ---
  const fetchMemos = async (limit: number = 10) => {
    let currentCursor: string | undefined;
    let currentFilter: GetMemoListFilter = { isCompleted: false };
    let isCurrentlyFetching = false;

    // Access current state values directly from the store
    update((state) => {
      currentCursor = state.listCursor;
      currentFilter = state.filter;
      isCurrentlyFetching = state.isFetchingList;

      // Avoid concurrent fetches
      if (isCurrentlyFetching) {
        return state; // No change needed
      }
      return { ...state, isFetchingList: true, error: undefined };
    });

    // If update decided not to fetch, exit early
    if (isCurrentlyFetching) {
      return;
    }

    try {
      const response = await memoService.getMemos(
        limit,
        currentCursor,
        currentFilter,
      );
      update((state) => {
        // Check if filter has changed since request started - if so, discard results
        if (JSON.stringify(state.filter) !== JSON.stringify(currentFilter)) {
          // Keep fetching true if a new fetch was likely triggered by applyFilter
          return state;
        }

        const newMemos = response.items ?? [];

        const fetchedMemos = currentCursor
          ? [...state.memos, ...newMemos]
          : newMemos;

        return {
          ...state,
          memos: fetchedMemos,
          listCursor: response.nextCursor,
          hasMore: response.hasMore,
          isFetchingList: false,
        };
      });
    } catch {
      update((state) => ({
        ...state,
        error: 'Failed to load memos. Please try again.',
        isFetchingList: false, // Ensure fetching state is reset on error
      }));
    }
  };

  const applyFilter = (newFilter: GetMemoListFilter) => {
    update((state) => ({
      ...state,
      filter: newFilter,
      memos: [], // Clear existing memos
      listCursor: undefined, // Reset cursor for new filter
      hasMore: true, // Assume there's data for the new filter
      isFetchingList: false, // Reset fetching state before triggering new fetch
      error: undefined,
    }));
    // Fetch immediately after applying the filter
    fetchMemos();
  };

  const updateMemoInList = (id: string, updatedData: Partial<Memo>) => {
    update((state) => {
      return {
        ...state,
        memos: _.chain(state.memos)
          .map((memo) => (memo.id === id ? { ...memo, ...updatedData } : memo))
          .filter(
            (memo) =>
              // If no filter is applied, show all memos
              _.isEmpty(state.filter) ||
              // If a filter is applied, only show memos that match the filter
              state.filter.isCompleted === memo.isCompleted,
          )
          .value(),
      };
    });
  };

  const removeMemoFromList = (id: string) => {
    update((state) => ({
      ...state,
      memos: state.memos.filter((memo) => memo.id !== id),
    }));
  };

  const setError = (message: string | undefined) => {
    update((state) => ({ ...state, error: message }));
    // Consider adding automatic clear timeout here if needed later
  };

  const setItemLoadingStatus = (id: string, isLoading: boolean) => {
    update((state) => ({
      ...state,
      itemLoadingStatus: { ...state.itemLoadingStatus, [id]: isLoading },
    }));
  };

  const tryFetchNextMemo = () => {
    update((state) => {
      if (!_.isNil(state.listCursor)) return state;
      return { ...state, listCursor: state.memos[state.memos.length - 1].id };
    });
    fetchMemos(1);
  };

  // --- Return Store API ---
  return {
    subscribe,
    fetchMemos,
    applyFilter,
    updateMemoInList,
    removeMemoFromList,
    setError,
    setItemLoadingStatus,
    tryFetchNextMemo,
  };
}

// --- Export Store Instance ---
const memoStore = createMemoStore();
export default memoStore;
