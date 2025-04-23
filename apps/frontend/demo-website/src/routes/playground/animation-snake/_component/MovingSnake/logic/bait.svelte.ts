import { SVG_VIEW_BOX_X, SVG_VIEW_BOX_Y } from '../constants';
import { state } from '../store.svelte';
import { type Dot } from '../types';
import {
  ZIGZAG_AMPLITUDE,
  ZIGZAG_WAVELENGTH,
  ZIGZAG_POOL_SIZE,
} from '../constants';
let placeBaitLoopTimeout: ReturnType<typeof setTimeout> | null = null;

let baitIndex = 0;
let baitPool: Dot[] = [];

/**
 * Generate bait pool for the snake to move towards
 * Make the baits look like a zigzag line so the snake will move like a real snake
 */
function generateZigZagBaits() {
  baitIndex = 0;
  baitPool = [];
  const startDot = state.snakeDots[0] as Dot;

  const directionDot = {
    x: Math.random() * SVG_VIEW_BOX_X,
    y: Math.random() * SVG_VIEW_BOX_Y,
  };

  // Direction vector from start dot to direction dot
  const directionVector = {
    x: directionDot.x - startDot.x,
    y: directionDot.y - startDot.y,
  };

  // Normalize direction vector
  const length = Math.sqrt(directionVector.x ** 2 + directionVector.y ** 2);
  const dirX = directionVector.x / length;
  const dirY = directionVector.y / length;

  // Use fixed wavelength as the baseline length

  // Calculate the perpendicular vector
  const perpDx = -dirY;
  const perpDy = dirX;

  const step = ZIGZAG_WAVELENGTH / 2;

  for (let i = 0; i < ZIGZAG_POOL_SIZE; i++) {
    const t = (i + 1) * step;
    const baseX = startDot.x + dirX * t;
    const baseY = startDot.y + dirY * t;

    // Determine the direction of the zigzag
    const direction = i % 2 === 0 ? 1 : -1;
    const offset = direction * ZIGZAG_AMPLITUDE;

    const dot = {
      x: baseX + perpDx * offset,
      y: baseY + perpDy * offset,
    };
    baitPool.push(dot);
  }
}

/**
 * Place bait on the screen
 * If the previous bait is out of the view, we generate a new one that leads the snake back to center area
 */
function placeBait() {
  const isPreviousBaitInViewBox =
    state.bait.x < SVG_VIEW_BOX_X &&
    state.bait.x > 0 &&
    state.bait.y < SVG_VIEW_BOX_Y &&
    state.bait.y > 0;

  // If previous bait is out of the view,
  // we generate a new one that leads the snake back to center area
  if (!isPreviousBaitInViewBox) {
    baitIndex = 0;
    baitPool = [
      {
        x: Math.min(
          SVG_VIEW_BOX_X * 0.8,
          Math.max(SVG_VIEW_BOX_X * 0.2, state.bait.x),
        ),
        y: Math.min(
          SVG_VIEW_BOX_Y * 0.8,
          Math.max(SVG_VIEW_BOX_Y * 0.2, state.bait.y),
        ),
      },
    ];
  }

  const shouldPutBaitNearSnakeHead = Math.random() < 0.1;
  // small chance to put bait near the snake head, to make the move pattern more interesting
  if (shouldPutBaitNearSnakeHead) {
    const headDot = state.snakeDots[0];
    state.bait = {
      x: headDot.x + (Math.random() * 200 - 100),
      y: headDot.y + (Math.random() * 200 - 100),
    };
  } else {
    // use next bait in the pool
    baitIndex++;
    if (baitIndex >= baitPool.length) {
      // if no more bait in the pool, generate new ones
      generateZigZagBaits();
    }
    state.bait = baitPool[baitIndex];
  }
}

/**
 * Start the loop of placing bait
 */
function placeBaitLoop() {
  if (!state.isAnimationRunning) {
    placeBaitLoopTimeout = null;
    return;
  }

  placeBait();

  // Schedule the *next* call to this function
  placeBaitLoopTimeout = setTimeout(placeBaitLoop, 10 * 1000);
}

/**
 * Manage the timer of placing bait
 */
export function managePlaceBaitTimer() {
  $effect(() => {
    if (state.isAnimationRunning) {
      if (placeBaitLoopTimeout === null) {
        setTimeout(placeBaitLoop, 0); // Start the cycle
      }
    } else {
      if (placeBaitLoopTimeout !== null) {
        clearTimeout(placeBaitLoopTimeout);
        placeBaitLoopTimeout = null;
      }
    }
    return () => {
      if (placeBaitLoopTimeout !== null) {
        clearTimeout(placeBaitLoopTimeout);
        placeBaitLoopTimeout = null;
      }
    };
  });
}

/**
 * When bait is eaten, put a new bait immediately,
 * which means we need to clear current running timer to avoid multiple bait reset,
 * and start a new loop.
 */
export function eatBait() {
  if (state.isAnimationRunning) {
    if (placeBaitLoopTimeout !== null) {
      clearTimeout(placeBaitLoopTimeout);
    }
    placeBaitLoopTimeout = setTimeout(placeBaitLoop, 0);
  }
  // do nothing if game is not running
}
