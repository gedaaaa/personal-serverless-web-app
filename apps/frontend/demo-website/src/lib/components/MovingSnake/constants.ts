// Snake constants
export const SECTION_LENGTH = 30;
export const SNAKE_LENGTH = 20;

// View port constants
export const SVG_VIEW_BOX_X = 1000;
export const SVG_VIEW_BOX_Y = 1000;

// Movement constants
export const MOVING_LENGTH_PER_FRAME = 4;
export const MAX_ANGLE_CHANGE_PER_FRAME = (30 / 1024) * Math.PI;
export const MAX_SECTION_RADIAN = (250 / 1024) * Math.PI;
export const POSSIBLE_NON_REACHABLE_DISTANCE =
  MOVING_LENGTH_PER_FRAME / (2 * Math.sin(MAX_ANGLE_CHANGE_PER_FRAME / 2));
export const MAX_ANGLE_CHANGE_WHEN_NON_REACHABLE = Math.PI / 3;
export const MAX_ANGLE_BODY_CURVE_LIMIT = Math.PI * (2 / 3);
export const STEPS_TO_1_CIRCLE_AT_NON_REACHABLE_DISTANCE =
  Math.PI / Math.sin(MAX_ANGLE_CHANGE_PER_FRAME / 2);

// Bait constants
export const ZIGZAG_AMPLITUDE = 60;
export const ZIGZAG_WAVELENGTH = 400;
export const ZIGZAG_POOL_SIZE = 10;
