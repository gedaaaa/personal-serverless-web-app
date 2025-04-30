import { state as scrollState } from '../store/scroll.svelte';
import { state as listState } from '../store/list.svelte';

/**
 * Processes scroll events and manages the virtual scroll viewport's state.
 * Handles both upward and downward scrolling with boundary detection.
 *
 * The DOM ring buffer is updated when scrolling reaches boundaries,
 * repositioning elements and loading new data as needed.
 *
 * @param delta The scroll amount (positive for downward, negative for upward)
 */
export function handleScroll(delta: number) {
  const translateY = scrollState.listTranslateY;
  const itemHeight = listState.itemHeight;
  const provider = listState.dataWindowProvider;
  const visualHead = scrollState.domRingHead;
  const listItemsCount = listState.dataWindowSize;
  const items = listState.dataWindowItems;

  console.log('handleScroll', delta, translateY, visualHead, listItemsCount);

  if (!provider) {
    scrollState.listTranslateY = translateY;
    scrollState.domRingHead = visualHead;
    listState.dataWindowItems = items;
    return;
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
        // Update DOM ring head position
        const newDomRingHead = (visualHead + 1) % listItemsCount;

        // Calculate the index of the element to update
        const targetIndex =
          (newDomRingHead + listItemsCount - 1) % listItemsCount;

        // Create a new items array with the updated item
        const newItems = [...items];
        newItems[targetIndex] = nextItem;

        scrollState.listTranslateY = -itemHeight;
        scrollState.domRingHead = newDomRingHead;
        listState.dataWindowItems = newItems;
        return;
      } else {
        // If there's no next item (end of the list)
        scrollState.listTranslateY = -2 * itemHeight;
        scrollState.domRingHead = visualHead;
        listState.dataWindowItems = items;
        return;
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
        // Update DOM ring head position
        const newDomRingHead =
          (visualHead - 1 + listItemsCount) % listItemsCount;

        // Create a new items array with the updated item
        const newItems = [...items];
        newItems[newDomRingHead] = prevItem;

        scrollState.listTranslateY = -itemHeight;
        scrollState.domRingHead = newDomRingHead;
        listState.dataWindowItems = newItems;
        return;
      } else {
        // If there's no previous item (beginning of the list)
        scrollState.listTranslateY = 0;
        scrollState.domRingHead = visualHead;
        listState.dataWindowItems = items;
        return;
      }
    }
  }

  // Normal scrolling (within boundaries)
  scrollState.listTranslateY = newTranslateY;
  scrollState.domRingHead = visualHead;
  listState.dataWindowItems = items;
  return;
}
