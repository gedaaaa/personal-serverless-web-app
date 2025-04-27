import { CELL_MOVE_DURATION } from './constants';
import { state } from './store.svelte';
import _ from 'lodash';

type Direction = 'up' | 'down' | 'left' | 'right';

/**
 * Fills a random empty cell in the opposite side of moving direction
 * @param direction - The direction to fill the cell in
 */
export async function fillRandomEmptyCellInOppositeSide(direction: Direction) {
  const size = 4;
  // decide which side we should fill
  let itemsCordinatesOnSide: { row: number; col: number }[] = [];
  if (direction === 'up' || direction === 'down') {
    const entranceRow = direction === 'up' ? size - 1 : 0;
    itemsCordinatesOnSide = _.chain(state.grid[entranceRow])
      .map((_unused, col) => ({
        row: entranceRow,
        col,
      }))
      .value();
  } else {
    const entranceCol = direction === 'left' ? size - 1 : 0;
    itemsCordinatesOnSide = _.chain(state.grid)
      .map((row) => row[entranceCol])
      .map((_unused, row) => ({
        row,
        col: entranceCol,
      }))
      .value();
  }

  // get all empty cells on the side
  const emptyCells = itemsCordinatesOnSide.filter(
    ({ row, col }) => state.grid[row][col].value === 0,
  );

  //  get a random empty cell
  const randomEmptyCell = _.sample(emptyCells);

  // if there is an empty cell, fill it and try to move this cell towards direction until it is close to a non-empty cell
  if (randomEmptyCell) {
    // fill the empty cell with a 2
    const nextId = state.increaseId++;
    state.grid[randomEmptyCell.row][randomEmptyCell.col] = {
      value: 2,
      id: nextId,
    };

    // wait for a while to make it visually noticed by user
    await wait(CELL_MOVE_DURATION / 8);

    // get the next position of the cell
    function getNextPosition(cordinates: { row: number; col: number }) {
      switch (direction) {
        case 'up':
          return cordinates.row - 1 >= 0
            ? { row: cordinates.row - 1, col: cordinates.col }
            : null;
        case 'down':
          return cordinates.row + 1 < size
            ? { row: cordinates.row + 1, col: cordinates.col }
            : null;
        case 'left':
          return cordinates.col - 1 >= 0
            ? { row: cordinates.row, col: cordinates.col - 1 }
            : null;
        case 'right':
          return cordinates.col + 1 < size
            ? { row: cordinates.row, col: cordinates.col + 1 }
            : null;
      }
    }
    let nextPosition = getNextPosition(randomEmptyCell);

    // if there is a next position, fill the next position with the cell and try to move this cell towards direction until it is close to a non-empty cell
    let toFill = randomEmptyCell;
    while (
      nextPosition &&
      state.grid[nextPosition.row][nextPosition.col].value === 0
    ) {
      toFill = nextPosition;
      nextPosition = getNextPosition(nextPosition);
    }

    // fill the original position with a 0
    state.grid[randomEmptyCell.row][randomEmptyCell.col] = {
      value: 0,
      id: state.increaseId++,
    };
    // fill next empty cell with a 2
    state.grid[toFill.row][toFill.col] = { value: 2, id: nextId };
  }
}

/**
 * Moves all cells to a direction, merge if cells have the same value
 * @param direction - The direction to move the cells to
 * @returns - Whether the cells were moved
 */
function moveAllCellsTo(direction: Direction) {
  // decide if the direction is vertical or horizontal
  const isVertical = direction === 'up' || direction === 'down';
  // decide if the direction is reverse
  const isReverse = direction === 'down' || direction === 'right';

  // flag to check if any cell was moved
  let moved = false;

  // loop through the grid, this index can be used as col or row based on the direction
  for (let outerIndex = 0; outerIndex < 4; outerIndex++) {
    // get the last examed cell cordinates, we use last examed cell to optimized the time complexity to be O(n) for each line.
    let lastExamedCellCordinates = {
      row: isVertical ? (isReverse ? 3 : 0) : outerIndex,
      col: isVertical ? outerIndex : isReverse ? 3 : 0,
    };
    // loop through the grid, this index will be used as the other axis of the cell
    for (
      let innerIndex = isReverse ? 3 : 0;
      isReverse ? innerIndex >= 0 : innerIndex < 4;
      isReverse ? innerIndex-- : innerIndex++
    ) {
      // update the last examed cell cordinates because we are going to check the cell next to current cell
      lastExamedCellCordinates = {
        row: isVertical
          ? isReverse
            ? lastExamedCellCordinates.row - 1
            : lastExamedCellCordinates.row + 1
          : outerIndex,
        col: isVertical
          ? outerIndex
          : isReverse
            ? lastExamedCellCordinates.col - 1
            : lastExamedCellCordinates.col + 1,
      };

      //   get the current cell cordinates
      const currentCellCordinates = isVertical
        ? { row: innerIndex, col: outerIndex }
        : { row: outerIndex, col: innerIndex };
      //   get the value of the current cell
      let currentCellValue =
        state.grid[currentCellCordinates.row][currentCellCordinates.col].value;

      // check each cell in the direction from current cell
      checkCell: for (
        let checkCellIndex = isVertical
          ? lastExamedCellCordinates.row
          : lastExamedCellCordinates.col;
        isReverse ? checkCellIndex >= 0 : checkCellIndex < 4;
        isReverse ? checkCellIndex-- : checkCellIndex++
      ) {
        // get the cell we are going to check
        const checkCellCordinates = isVertical
          ? { row: checkCellIndex, col: outerIndex }
          : { row: outerIndex, col: checkCellIndex };
        const checkCell =
          state.grid[checkCellCordinates.row][checkCellCordinates.col];
        const checkCellValue = checkCell.value;

        // if the cell we are going to check is empty, skip it
        if (checkCellValue === 0) {
          continue checkCell;
        }

        // if the current cell and the cell we are going to check have the same value, merge them
        if (currentCellValue === checkCellValue) {
          currentCellValue = currentCellValue * 2;
          state.grid[currentCellCordinates.row][currentCellCordinates.col] = {
            value: currentCellValue,
            id: state.grid[checkCellCordinates.row][checkCellCordinates.col].id,
          };
          state.grid[checkCellCordinates.row][checkCellCordinates.col] = {
            value: 0,
            id: state.increaseId++,
          };
          if (currentCellValue >= 2048) {
            state.isGameWon = true;
          }
          moved = true;
          break checkCell;
        }

        // if the current cell is empty, move the cell we are checking to the current cell
        if (currentCellValue === 0) {
          currentCellValue = checkCellValue;
          state.grid[currentCellCordinates.row][currentCellCordinates.col] = {
            value: checkCellValue,
            id: state.grid[checkCellCordinates.row][checkCellCordinates.col].id,
          };
          state.grid[checkCellCordinates.row][checkCellCordinates.col] = {
            value: 0,
            id: state.increaseId++,
          };
          moved = true;

          continue checkCell;
        }

        // helper function to get the next cell cordinates
        function getNextCellCordinates(cordinates: {
          row: number;
          col: number;
        }) {
          return {
            row: cordinates.row + (isVertical ? (isReverse ? -1 : 1) : 0),
            col: cordinates.col + (isVertical ? 0 : isReverse ? -1 : 1),
          };
        }

        const nextCellCordinates = getNextCellCordinates(currentCellCordinates);

        // If the check cell is not empty and is the next cell to current cell, we should move nothing
        if (
          nextCellCordinates.row === checkCellCordinates.row &&
          nextCellCordinates.col === checkCellCordinates.col
        ) {
          break checkCell;
        }

        // if the current cell and the cell we are checking are not empty and not the same value,
        // so we move the cell we are checking to be next to current cell
        const lastExamedCellId =
          state.grid[checkCellCordinates.row][checkCellCordinates.col].id;

        // fill the cell we are checking with a 0
        state.grid[checkCellCordinates.row][checkCellCordinates.col] = {
          value: 0,
          id: state.increaseId++,
        };

        // fill the cell next to current cell with the value of the cell we are checking
        state.grid[nextCellCordinates.row][nextCellCordinates.col] = {
          value: checkCellValue,
          id: lastExamedCellId,
        };
        moved = true;
        break checkCell;
      }
    }
  }
  return moved;
}

/**
 * Moves all cells to a direction, merge if cells have the same value
 * @param direction - The direction to move the cells to
 * @returns - Whether the cells were moved
 */
export async function move(direction: Direction) {
  if (state.isMoving) {
    return;
  }
  state.isMoving = true;
  const moved = moveAllCellsTo(direction);
  if (moved) {
    await wait();
    await fillRandomEmptyCellInOppositeSide(direction);
  }
  state.isMoving = false;
}

/**
 * Waits for a duration
 * @param duration - The duration to wait for
 */
async function wait(duration = CELL_MOVE_DURATION) {
  await new Promise((resolve) => setTimeout(resolve, duration));
}
