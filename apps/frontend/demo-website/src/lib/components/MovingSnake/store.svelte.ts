import {
  SNAKE_LENGTH,
  SECTION_LENGTH,
  SVG_VIEW_BOX_Y,
  SVG_VIEW_BOX_X,
} from './constants';
import type { Dot, SnakeDot } from './types';
import _ from 'lodash';

export const state: {
  snakeDots: SnakeDot[];
  bait: Dot;
  isAnimationRunning: boolean;
} = $state({
  snakeDots: _.range(SNAKE_LENGTH).map<SnakeDot>((idx) => ({
    id: idx,
    x: idx * SECTION_LENGTH,
    y: SVG_VIEW_BOX_Y + idx * SECTION_LENGTH,
  })),
  bait: {
    x: SVG_VIEW_BOX_X / 2,
    y: SVG_VIEW_BOX_Y / 2,
  },
  isAnimationRunning: false,
});
