import { tick } from 'svelte';
import type { DataItem } from '../data/DataSource/DataSource';
import type { VisibleItemsProvider } from '../data/VisibleItemsProvider';

/**
 * Parameters required for scroll handling operations.
 */
export interface ScrollParams<T extends DataItem> {
  translateY: number;
  itemHeight: number;
  provider: VisibleItemsProvider<T> | null;
  visualHead: number;
  listItemsCount: number;
  items: T[];
}

/**
 * The result of a scroll handling operation.
 */
export interface ScrollResult<T extends DataItem> {
  translateY: number;
  visualHead: number;
  items: T[];
  success: boolean;
}

/**
 * Processes scroll events and manages the virtual scroll viewport's state.
 * Handles both upward and downward scrolling with boundary detection.
 *
 * @param delta The scroll amount (positive for downward, negative for upward)
 * @param params Current scroll state parameters
 */
export function handleScroll<T extends DataItem>(
  delta: number,
  params: ScrollParams<T>,
): ScrollResult<T> {
  const {
    translateY,
    itemHeight,
    provider,
    visualHead,
    listItemsCount,
    items,
  } = params;

  if (!provider) {
    return {
      translateY,
      visualHead,
      items,
      success: false,
    };
  }

  // Calculate the new translateY position
  const newTranslateY = translateY - delta;

  // Handle downward scrolling (delta > 0)
  if (delta > 0) {
    // Check if we've reached the lower boundary (-2*itemHeight)
    if (newTranslateY <= -2 * itemHeight) {
      // Try to get the next item using provider.moveForward
      const nextItem = provider.moveForward();

      // If we successfully retrieved the next item
      if (nextItem) {
        // Update visualHead position
        const newVisualHead = (visualHead + 1) % listItemsCount;

        // Calculate the index of the element to update
        const targetIndex =
          (newVisualHead + listItemsCount - 1) % listItemsCount;

        // Create a new items array with the updated item
        const newItems = [...items];
        newItems[targetIndex] = nextItem;

        return {
          translateY: -itemHeight, // Reset to -itemHeight
          visualHead: newVisualHead,
          items: newItems,
          success: true,
        };
      } else {
        // If there's no next item (end of the list)
        return {
          translateY: -2 * itemHeight, // Stay at boundary position
          visualHead,
          items,
          success: false,
        };
      }
    }
  }
  // Handle upward scrolling (delta < 0)
  else if (delta < 0) {
    // Check if we've reached the upper boundary (0)
    if (newTranslateY >= 0) {
      // Try to get the previous item using provider.moveBackward
      const prevItem = provider.moveBackward();

      // If we successfully retrieved the previous item
      if (prevItem) {
        // Update visualHead position
        const newVisualHead =
          (visualHead - 1 + listItemsCount) % listItemsCount;

        // Create a new items array with the updated item
        const newItems = [...items];
        newItems[newVisualHead] = prevItem;

        return {
          translateY: -itemHeight, // Reset to -itemHeight
          visualHead: newVisualHead,
          items: newItems,
          success: true,
        };
      } else {
        // If there's no previous item (beginning of the list)
        return {
          translateY: 0, // Stay at boundary position
          visualHead,
          items,
          success: false,
        };
      }
    }
  }

  // Normal scrolling (within boundaries)
  return {
    translateY: newTranslateY,
    visualHead,
    items,
    success: true,
  };
}
