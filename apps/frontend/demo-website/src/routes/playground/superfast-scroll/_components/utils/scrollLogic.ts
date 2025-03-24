import { tick } from 'svelte';
import type { DataItem } from '../data/DataSource/DataSource';
import type { VisibleItemsProvider } from '../data/VisibleItemsProvider';

/**
 * Handles wheel event scrolling logic for virtual scroll
 * @param event The wheel event
 * @param params Configuration and state parameters
 * @returns Updated state values
 */
export function handleWheelScroll<T extends DataItem>(
  event: WheelEvent,
  params: {
    translateY: number;
    currentPosition: number | null;
    isAtStart: boolean;
    isAtEnd: boolean;
    itemHeight: number;
    provider: VisibleItemsProvider<T> | null;
    tempVirtualRingHead: number;
  },
) {
  const {
    translateY,
    currentPosition,
    isAtStart,
    isAtEnd,
    itemHeight,
    provider,
    tempVirtualRingHead,
  } = params;

  // Prevent default scrolling behavior
  event.preventDefault();

  // Calculate new translateY
  const newTranslateY = translateY - event.deltaY;
  let updatedTranslateY = translateY;
  let updatedPosition = currentPosition;
  let updatedTempVirtualRingHead = tempVirtualRingHead;

  if (event.deltaY > 0) {
    // Scrolling down
    if (isAtEnd) {
      // If we're at the end of the list, limit translateY
      updatedTranslateY = -2 * itemHeight;
      updatedTempVirtualRingHead = 0;
    } else if (newTranslateY < -2 * itemHeight) {
      // We've scrolled down 2 items, reset to 1 item offset
      updatedTranslateY = -itemHeight;

      // Move to next item if not at end
      updatedPosition = Math.min(999999, (currentPosition || 0) + 1);
      updatedTempVirtualRingHead = tempVirtualRingHead + 1;
    } else {
      updatedTranslateY = newTranslateY;
    }
  } else if (event.deltaY < 0) {
    // Scrolling up
    if (isAtStart) {
      // If we're at the start of the list, reset translateY to 0
      updatedTranslateY = 0;
      updatedTempVirtualRingHead = 0;
    } else if (newTranslateY > 0) {
      // We've scrolled up to the top buffer, reset to 1 item offset
      updatedTranslateY = -itemHeight;

      // Move to previous item if not at start
      updatedPosition = Math.max(0, (currentPosition || 0) - 1);
      updatedTempVirtualRingHead = tempVirtualRingHead - 1;
    } else {
      updatedTranslateY = newTranslateY;
    }
  }

  return {
    translateY: updatedTranslateY,
    currentPosition: updatedPosition,
    tempVirtualRingHead: updatedTempVirtualRingHead,
  };
}

/**
 * Handles touch events for scrolling logic
 * @param deltaY The vertical distance moved
 * @param params Configuration and state parameters
 * @returns Updated state values
 */
export function handleTouchScroll<T extends DataItem>(
  deltaY: number,
  params: {
    translateY: number;
    currentPosition: number | null;
    isAtStart: boolean;
    isAtEnd: boolean;
    itemHeight: number;
    provider: VisibleItemsProvider<T> | null;
    tempVirtualRingHead: number;
  },
) {
  const {
    translateY,
    currentPosition,
    isAtStart,
    isAtEnd,
    itemHeight,
    provider,
    tempVirtualRingHead,
  } = params;

  // Calculate new translateY
  const newTranslateY = translateY - deltaY;
  let updatedTranslateY = translateY;
  let updatedPosition = currentPosition;
  let updatedTempVirtualRingHead = tempVirtualRingHead;

  if (deltaY > 0) {
    // Scrolling down (finger moving up)
    if (isAtEnd) {
      // If we're at the end of the list, limit translateY
      updatedTranslateY = -2 * itemHeight;
      updatedTempVirtualRingHead = 0;
    } else if (newTranslateY < -2 * itemHeight) {
      // Move to next item if not at end
      updatedPosition = Math.min(999999, (currentPosition || 0) + 1);

      updatedTempVirtualRingHead = tempVirtualRingHead + 1;

      // We've scrolled down 2 items, reset to 1 item offset
      updatedTranslateY = -itemHeight;
    } else {
      updatedTranslateY = newTranslateY;
    }
  } else if (deltaY < 0) {
    // Scrolling up (finger moving down)
    if (isAtStart) {
      // If we're at the start of the list, reset translateY to 0
      updatedTranslateY = 0;
      updatedTempVirtualRingHead = 0;
    } else if (newTranslateY > 0) {
      // We've scrolled up to the top buffer, reset to 1 item offset

      // Move to previous item if not at start
      updatedPosition = Math.max(0, (currentPosition || 0) - 1);
      updatedTempVirtualRingHead = tempVirtualRingHead - 1;
      updatedTranslateY = -itemHeight;
    } else {
      updatedTranslateY = newTranslateY;
    }
  }

  return {
    translateY: updatedTranslateY,
    currentPosition: updatedPosition,
    tempVirtualRingHead: updatedTempVirtualRingHead,
  };
}

/**
 * Creates a jump to position handler
 * @returns A function to jump to a specific position
 */
export function createJumpToPositionHandler(
  setCurrentPosition: (position: number) => void,
  setTranslateY: (translateY: number) => void,
) {
  return function jumpToPosition(position: number): void {
    setCurrentPosition(position);
    setTranslateY(0); // Reset translateY when jumping to position
  };
}
