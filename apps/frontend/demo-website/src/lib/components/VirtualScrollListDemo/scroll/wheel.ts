import { handleScroll } from './scroll';
import { state as listState } from '../store/list.svelte';

/**
 * Handles mouse wheel events for smooth scrolling.
 *
 * As the user scrolls, this manages both the visual translation
 * and the shifting of data through the ring buffer when boundaries are reached.
 */
export function handleWheel(event: WheelEvent) {
  const itemHeight = listState.itemHeight;
  event.preventDefault();

  // Use wheel event delta for smoother scrolling experience
  // Scale the wheel delta to an appropriate scrolling distance
  const scaleFactor = 0.5;
  const delta = event.deltaY * scaleFactor;

  handleScroll(
    delta > 0 ? Math.min(delta, itemHeight) : Math.max(delta, -itemHeight),
  );
}
