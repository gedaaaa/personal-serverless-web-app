import { INITIAL_GRID, INCREASEMENT_ID_START } from './constants';

export const state = $state({
  grid: INITIAL_GRID,
  isMoving: false,
  isGameWon: false,
  increaseId: INCREASEMENT_ID_START,
});

export const reset = () => {
  state.grid = INITIAL_GRID;
  state.isMoving = false;
  state.isGameWon = false;
  state.increaseId = INCREASEMENT_ID_START;
};
