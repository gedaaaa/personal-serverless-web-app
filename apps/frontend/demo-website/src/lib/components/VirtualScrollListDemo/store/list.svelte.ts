import type { DataItem } from '../data/DataSource/DataSource';
import type { DataWindowProvider } from '../data/DataWindowProvider';

const VISUAL_BUFFER_ITEMS_COUNT = 2;
const DEFAULT_ITEM_HEIGHT = 40;
const DEFAULT_VISIBLE_ITEMS_COUNT = 10;

export const state = $state<{
  dataWindowProvider: DataWindowProvider<DataItem> | null;
  dataWindowSize: number;
  visibleItemsCount: number;
  itemHeight: number;
  dataWindowItems: DataItem[];
}>({
  dataWindowProvider: null,
  dataWindowSize: 0,
  visibleItemsCount: DEFAULT_VISIBLE_ITEMS_COUNT,
  itemHeight: DEFAULT_ITEM_HEIGHT,
  dataWindowItems: [],
});

export const setVisibleItemsCount = (visibleItemsCount: number) => {
  state.visibleItemsCount = visibleItemsCount;
  state.dataWindowSize = visibleItemsCount + VISUAL_BUFFER_ITEMS_COUNT;
};

export const setItemHeight = (itemHeight: number) => {
  state.itemHeight = itemHeight;
};
