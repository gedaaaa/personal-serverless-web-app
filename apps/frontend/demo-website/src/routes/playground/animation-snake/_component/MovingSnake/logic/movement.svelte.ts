import {
  MAX_ANGLE_BODY_CURVE_LIMIT,
  MAX_ANGLE_CHANGE_PER_FRAME,
  MAX_ANGLE_CHANGE_WHEN_NON_REACHABLE,
  MOVING_LENGTH_PER_FRAME,
  POSSIBLE_NON_REACHABLE_DISTANCE,
  SECTION_LENGTH,
  STEPS_TO_1_CIRCLE_AT_NON_REACHABLE_DISTANCE,
} from '../constants';
import { MAX_SECTION_RADIAN } from '../constants';
import { state } from '../store.svelte';
import type { DirectionVector } from '../types';
import { getNarrowedDirectionVector } from '../util';
import { eatBait } from './bait.svelte';

let bodyCurveLimit = MAX_SECTION_RADIAN;
let animationId: number | null = null;
/**
 * Maintain the body curve limit
 * The body curve limit is the maximum angle that the snake body can bend.
 * If the curve limit is too high, we reset it to π (180 degrees)
 * If the curve limit is too low, we reset it to MAX_SECTION_RADIAN
 * Otherwise, we decrease the curve limit by 0.05% (this function will be called multiple times per frame)
 */
function maintainBodyCurveLimit() {
  if (bodyCurveLimit > Math.PI) {
    bodyCurveLimit = Math.PI;
  } else if (bodyCurveLimit <= MAX_SECTION_RADIAN) {
    bodyCurveLimit = MAX_SECTION_RADIAN;
  } else {
    bodyCurveLimit *= 0.995;
  }
}

/**
 * Constraint the distance between the anchorDot and the movingDot
 * @param anchorDotIndex
 * @param movingDotIndex
 */
function constraintDistance(anchorDotIndex: number, movingDotIndex: number) {
  // maintain the body curve limit
  const anchorDot = state.snakeDots[anchorDotIndex];
  const movingDot = state.snakeDots[movingDotIndex];
  // distance from movingDot dot to anchor dot
  const distance = Math.sqrt(
    (anchorDot.x - movingDot.x) ** 2 + (anchorDot.y - movingDot.y) ** 2,
  );

  // calculate normalized direction vector from anchorDot to movingDot
  const directionVector: DirectionVector = {
    x: (movingDot.x - anchorDot.x) / distance,
    y: (movingDot.y - anchorDot.y) / distance,
  };

  // check if the direction of the section is too curved
  const previousSectionDirectionVector: DirectionVector = {
    x:
      anchorDotIndex > 0
        ? (anchorDot.x - state.snakeDots[anchorDotIndex - 1].x) / SECTION_LENGTH
        : -movingDirection.x,
    y:
      anchorDotIndex > 0
        ? (anchorDot.y - state.snakeDots[anchorDotIndex - 1].y) / SECTION_LENGTH
        : -movingDirection.y,
  };

  const narrowedDirectionVector = getNarrowedDirectionVector(
    previousSectionDirectionVector,
    directionVector,
    bodyCurveLimit,
  );

  // put the movingDot at exactly SCETION_LENGTH distance from anchorDot
  const targetX = anchorDot.x + narrowedDirectionVector.x * SECTION_LENGTH;
  const targetY = anchorDot.y + narrowedDirectionVector.y * SECTION_LENGTH;

  const isSignificantlyChanged =
    Math.abs(targetX - movingDot.x) > 0.7 ||
    Math.abs(targetY - movingDot.y) > 0.7;

  if (isSignificantlyChanged) {
    state.snakeDots[movingDotIndex] = {
      id: movingDot.id,
      x: targetX,
      y: targetY,
    };
  }

  return isSignificantlyChanged;
}

let movingDirection: DirectionVector = { x: 0, y: -1 };
let circlingSteps = 0;
let turnLimitation = MAX_ANGLE_CHANGE_PER_FRAME;

function animationLoop() {
  // Check running state *inside* the loop in case it was stopped externally
  if (!state.isAnimationRunning) {
    animationId = null; // Ensure ID is cleared
    return;
  }
  headTorwardsbait();
  animationId = requestAnimationFrame(animationLoop); // Schedule next frame
}

export function manageSnakeMovementLoop() {
  $effect(() => {
    if (state.isAnimationRunning) {
      if (animationId === null) {
        // start the animation loop if no loop is running
        animationId = requestAnimationFrame(animationLoop);
      }
    } else {
      if (animationId !== null) {
        animationId = null;
      }
    }

    return () => {
      if (animationId !== null) {
        cancelAnimationFrame(animationId);
        animationId = null;
      }
    };
  });
}
/**
 * Move the snake head towards the bait
 */
function headTorwardsbait() {
  maintainBodyCurveLimit();
  const { x: targetX, y: targetY } = state.bait;

  // calculate the direction vector from snake head to the bait
  const dx = targetX - state.snakeDots[0].x;
  const dy = targetY - state.snakeDots[0].y;

  const distance = Math.sqrt(dx * dx + dy * dy);

  const tryDirectionVector: DirectionVector = {
    x: dx / distance,
    y: dy / distance,
  };

  // if snake is very close to target, move directly to it
  if (distance <= MOVING_LENGTH_PER_FRAME) {
    const narrowedDirectionVector = getNarrowedDirectionVector(
      movingDirection,
      tryDirectionVector,
      MAX_ANGLE_CHANGE_WHEN_NON_REACHABLE,
    );
    // reset circling steps
    circlingSteps = 0;
    // update moving direction
    movingDirection = narrowedDirectionVector;
    // update snake head position
    state.snakeDots[0] = {
      id: state.snakeDots[0].id,
      x: state.snakeDots[0].x + movingDirection.x * MOVING_LENGTH_PER_FRAME,
      y: state.snakeDots[0].y + movingDirection.y * MOVING_LENGTH_PER_FRAME,
    };

    eatBait();

    turnLimitation = MAX_ANGLE_CHANGE_PER_FRAME;
  }
  // if the snake is close to the bait, but not too close, start recording circling steps
  // if the snake is circling, it will try to turn much faster and the snake body will be able to bend more
  else if (distance <= POSSIBLE_NON_REACHABLE_DISTANCE) {
    // increase circling steps
    circlingSteps++;

    let narrowedDirectionVector = getNarrowedDirectionVector(
      movingDirection,
      tryDirectionVector,
      turnLimitation,
    );

    // check if the snake is circling
    const isCircling =
      circlingSteps >= STEPS_TO_1_CIRCLE_AT_NON_REACHABLE_DISTANCE / 3 &&
      (narrowedDirectionVector.x !== movingDirection.x ||
        narrowedDirectionVector.y !== movingDirection.y);

    if (isCircling) {
      turnLimitation = Math.min(
        turnLimitation * 1.1,
        MAX_ANGLE_CHANGE_WHEN_NON_REACHABLE,
      );
      bodyCurveLimit = Math.min(
        MAX_ANGLE_BODY_CURVE_LIMIT,
        bodyCurveLimit * 1.1,
      );
      narrowedDirectionVector = getNarrowedDirectionVector(
        movingDirection,
        tryDirectionVector,
        turnLimitation,
      );
    }

    // update moving direction
    movingDirection = narrowedDirectionVector;
    // update snake head position
    state.snakeDots[0] = {
      id: state.snakeDots[0].id,
      x: state.snakeDots[0].x + movingDirection.x * MOVING_LENGTH_PER_FRAME,
      y: state.snakeDots[0].y + movingDirection.y * MOVING_LENGTH_PER_FRAME,
    };
  } else {
    // update circling steps if the snake is already circling
    circlingSteps = circlingSteps === 0 ? circlingSteps : circlingSteps + 1;
    const narrowedDirectionVector = getNarrowedDirectionVector(
      movingDirection,
      tryDirectionVector,
      MAX_ANGLE_CHANGE_PER_FRAME,
    );

    // move snake head by fixed distance towards target
    state.snakeDots[0] = {
      id: state.snakeDots[0].id,
      x:
        state.snakeDots[0].x +
        narrowedDirectionVector.x * MOVING_LENGTH_PER_FRAME,
      y:
        state.snakeDots[0].y +
        narrowedDirectionVector.y * MOVING_LENGTH_PER_FRAME,
    };

    movingDirection = narrowedDirectionVector;
  }

  // maintain the body dots (base on current head position)
  maintainSnakeBodyDots();
}

/**
 * Calculate the position of the snake dots
 */
function maintainSnakeBodyDots() {
  for (let i = 1; i < state.snakeDots.length; i++) {
    if (!constraintDistance(i - 1, i)) {
      break;
    }
  }
}
