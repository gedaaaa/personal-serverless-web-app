import { move } from './game-logic';

let touchStartX = 0;
let touchStartY = 0;
const SWIPE_THRESHOLD = 50; // Minimum distance for a swipe

export function handleKeyDown(event: KeyboardEvent) {
  switch (event.key) {
    case 'ArrowUp':
    case 'w':
    case 'W':
      move('up');
      break;
    case 'ArrowDown':
    case 's':
    case 'S':
      move('down');
      break;
    case 'ArrowLeft':
    case 'a':
    case 'A':
      move('left');
      break;
    case 'ArrowRight':
    case 'd':
    case 'D':
      move('right');
      break;
  }
}

export function handleTouchStart(event: TouchEvent) {
  touchStartX = event.touches[0].clientX;
  touchStartY = event.touches[0].clientY;
}

export function handleTouchEnd(event: TouchEvent) {
  if (!touchStartX || !touchStartY) {
    return;
  }

  const touchEndX = event.changedTouches[0].clientX;
  const touchEndY = event.changedTouches[0].clientY;

  const deltaX = touchEndX - touchStartX;
  const deltaY = touchEndY - touchStartY;

  // Reset touch start points for the next touch
  touchStartX = 0;
  touchStartY = 0;

  if (
    Math.abs(deltaX) > SWIPE_THRESHOLD ||
    Math.abs(deltaY) > SWIPE_THRESHOLD
  ) {
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      // Horizontal swipe
      if (deltaX > 0) {
        move('right');
      } else {
        move('left');
      }
    } else {
      // Vertical swipe
      if (deltaY > 0) {
        move('down');
      } else {
        move('up');
      }
    }
  }
}
