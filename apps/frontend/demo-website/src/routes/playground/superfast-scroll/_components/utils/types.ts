// Type definitions for the virtual scroll components
import type { DataItem, DataSource } from '../data/DataSource/DataSource';
import type { RingBufferDataWindowProvider } from '../data/VisibleItemsProvider';

// Scroll state interface
export interface ScrollState<T extends DataItem> {
  currentPosition: number | null;
  items: T[];
  totalCount: number;
  isAtStart: boolean;
  isAtEnd: boolean;
  translateY: number;
  domRingHead: number;
}

// Props for the main VirtualScrollList component
export interface VirtualScrollListProps<T extends DataItem> {
  provider: RingBufferDataWindowProvider<T> | null;
  dataSource: DataSource<T> | null;
  itemHeight?: number;
  visibleItemsCount?: number;
  jumpToPosition?: (position: number) => void;
}

// Configuration constants
// DOM Buffer items count - extra items to render beyond the visible viewport
// These provide a buffer so items can be ready before scrolling into view
export const VISUAL_BUFFER_ITEMS_COUNT = 2;

export const DEFAULT_ITEM_HEIGHT = 40;
export const DEFAULT_VISIBLE_ITEMS_COUNT = 10;
