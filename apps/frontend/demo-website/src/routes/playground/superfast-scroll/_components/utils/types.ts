// Type definitions for the virtual scroll components
import type { DataItem, DataSource } from '../data/DataSource/DataSource';
import type { RingBufferVisibleItemsProvider } from '../data/VisibleItemsProvider';

// Scroll state interface
export interface ScrollState<T extends DataItem> {
  currentPosition: number | null;
  items: T[];
  totalCount: number;
  isAtStart: boolean;
  isAtEnd: boolean;
  translateY: number;
  virtualRingHead: number;
}

// Props for the main VirtualScrollList component
export interface VirtualScrollListProps<T extends DataItem> {
  provider: RingBufferVisibleItemsProvider<T> | null;
  dataSource: DataSource<T> | null;
  itemHeight?: number;
  visibleItemsCount?: number;
  jumpToPosition?: (position: number) => void;
}

// Configuration constants
export const BUFFER_ITEMS_COUNT = 2;
export const DEFAULT_ITEM_HEIGHT = 40;
export const DEFAULT_VISIBLE_ITEMS_COUNT = 10;
