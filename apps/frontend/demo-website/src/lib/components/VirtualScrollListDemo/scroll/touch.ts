import { handleScroll } from './scroll';

let isTouching = false;
let touchStartY = 0;
let lastTouchY = 0;

/**
 * Manages touch start interaction for mobile scrolling.
 */
export function handleTouchStart(event: TouchEvent) {
  if (event.touches.length === 1) {
    isTouching = true;
    touchStartY = event.touches[0].clientY;
    lastTouchY = touchStartY;
  }
}

/**
 * Processes touch movement to enable smooth scrolling on mobile devices.
 */
export function handleTouchMove(event: TouchEvent) {
  if (!isTouching || event.touches.length !== 1) return;

  const currentTouchY = event.touches[0].clientY;
  const deltaY = lastTouchY - currentTouchY;
  lastTouchY = currentTouchY;

  // Apply sensitivity scaling for touch scrolling
  const scaleFactor = 1;
  const scaledDelta = deltaY * scaleFactor;

  handleScroll(scaledDelta);
}

/**
 * Handles the end of touch interaction.
 */
export function handleTouchEnd() {
  isTouching = false;
}
