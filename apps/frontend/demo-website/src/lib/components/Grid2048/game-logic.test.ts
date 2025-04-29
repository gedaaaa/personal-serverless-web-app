import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  moveAllCellsTo,
  fillRandomEmptyCellInOppositeSide,
} from './game-logic';
import _ from 'lodash';

// Define Cell type if not easily importable
type Cell = {
  value: number;
  id: number;
};

// Define GridState type here
export type GridState = {
  grid: Cell[][];
  isMoving: boolean;
  isGameWon: boolean;
  increaseId: number;
};

// Mock the store module
vi.mock('./store.svelte', () => ({
  state: {
    grid: [] as Cell[][],
    increaseId: 1,
    isGameWon: false,
    isMoving: false,
  } as GridState, // Add type assertion here
}));

// Import the mocked state to control it
import { state as mockStateImport } from './store.svelte';
// Cast to the correct type
const mockState = mockStateImport as GridState;

// Helper to create a grid from simple values
function createGridFromValues(values: number[][]): Cell[][] {
  let idCounter = 1;
  return values.map((row) =>
    row.map((value) => ({
      value: value,
      id: value === 0 ? 0 : idCounter++, // Assign unique IDs, maybe 0 for empty cells? Check original logic. Let's start with 0 for empty.
    })),
  );
}

// Helper to get just the values from a grid for easier comparison
function getGridValues(grid: Cell[][]): number[][] {
  return grid.map((row) => row.map((cell) => cell.value));
}

// Reset state before each test
beforeEach(() => {
  mockState.grid = createGridFromValues([
    [0, 0, 0, 0],
    [0, 0, 0, 0],
    [0, 0, 0, 0],
    [0, 0, 0, 0],
  ]);
  mockState.increaseId = 100; // Start high to avoid collision with initial IDs
  mockState.isGameWon = false;
  mockState.isMoving = false;
  // Reset mocks specifically for lodash if needed later
  vi.resetAllMocks(); // Reset general mocks
});

// Placeholder for actual tests
describe('game-logic', () => {
  it('should have initial setup working', () => {
    expect(mockState.grid.length).toBe(4);
  });
});

describe('moveAllCellsTo', () => {
  describe('direction: up', () => {
    it('should not move if column is already packed upwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(false);
      expect(getGridValues(mockState.grid)).toEqual([
        [2, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should not move if column is empty', () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(false);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should move cells upwards into empty spaces', () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [2, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should merge adjacent cells upwards', () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [2, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should merge cells with a gap upwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should perform multiple merges upwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [2, 0, 0, 0],
        [4, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [4, 0, 0, 0],
        [8, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should handle merging and moving complex cases upwards', () => {
      mockState.grid = createGridFromValues([
        [0, 2, 0, 4],
        [2, 2, 0, 4],
        [2, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [4, 4, 0, 8],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should move a non-first element upwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [2, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should merge a non-first element upwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [4, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [2, 0, 0, 0],
        [8, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should set isGameWon when 2048 is reached', () => {
      mockState.grid = createGridFromValues([
        [1024, 0, 0, 0],
        [1024, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('up');
      expect(moved).toBe(true);
      expect(mockState.isGameWon).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [2048, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });
  });

  describe('direction: down', () => {
    it('should not move if column is already packed downwards', () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('down');
      expect(moved).toBe(false);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
    });

    it('should move cells downwards into empty spaces', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('down');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
    });

    it('should merge adjacent cells downwards', () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [2, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('down');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
    });

    it('should merge cells with a gap downwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 0, 0],
        [0, 0, 0, 0],
        [2, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('down');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 0, 0],
      ]);
    });

    it('should perform multiple merges downwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 4, 0],
        [2, 0, 4, 0],
        [4, 0, 8, 0],
        [4, 0, 8, 0],
      ]);
      const moved = moveAllCellsTo('down');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 8, 0],
        [8, 0, 16, 0],
      ]);
    });

    it('should handle merging and moving complex cases downwards', () => {
      mockState.grid = createGridFromValues([
        [4, 0, 0, 0],
        [2, 0, 2, 0],
        [2, 0, 2, 0],
        [0, 0, 4, 0],
      ]);
      const moved = moveAllCellsTo('down');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [4, 0, 4, 0],
        [4, 0, 4, 0],
      ]);
    });
  });

  describe('direction: left', () => {
    it('should not move if row is already packed leftwards', () => {
      mockState.grid = createGridFromValues([
        [2, 4, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('left');
      expect(moved).toBe(false);
      expect(getGridValues(mockState.grid)[0]).toEqual([2, 4, 0, 0]);
    });

    it('should move cells leftwards into empty spaces', () => {
      mockState.grid = createGridFromValues([
        [0, 2, 0, 4],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('left');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([2, 4, 0, 0]);
    });

    it('should merge adjacent cells leftwards', () => {
      mockState.grid = createGridFromValues([
        [0, 2, 2, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('left');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([4, 0, 0, 0]);
    });

    it('should merge cells with a gap leftwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 2, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('left');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([4, 0, 0, 0]);
    });

    it('should perform multiple merges leftwards', () => {
      mockState.grid = createGridFromValues([
        [2, 2, 4, 4],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('left');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([4, 8, 0, 0]);
    });

    it('should handle merging and moving complex cases leftwards', () => {
      mockState.grid = createGridFromValues([
        [0, 2, 2, 4],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('left');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([4, 4, 0, 0]);
    });
  });

  describe('direction: right', () => {
    it('should not move if row is already packed rightwards', () => {
      mockState.grid = createGridFromValues([
        [0, 0, 2, 4],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('right');
      expect(moved).toBe(false);
      expect(getGridValues(mockState.grid)[0]).toEqual([0, 0, 2, 4]);
    });

    it('should move cells rightwards into empty spaces', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 4, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('right');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([0, 0, 2, 4]);
    });

    it('should merge adjacent cells rightwards', () => {
      mockState.grid = createGridFromValues([
        [0, 2, 2, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('right');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([0, 0, 0, 4]);
    });

    it('should merge cells with a gap rightwards', () => {
      mockState.grid = createGridFromValues([
        [2, 0, 2, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('right');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([0, 0, 0, 4]);
    });

    it('should perform multiple merges rightwards', () => {
      mockState.grid = createGridFromValues([
        [2, 2, 4, 4],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('right');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([0, 0, 4, 8]);
    });

    it('should handle merging and moving complex cases rightwards', () => {
      mockState.grid = createGridFromValues([
        [4, 2, 2, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const moved = moveAllCellsTo('right');
      expect(moved).toBe(true);
      expect(getGridValues(mockState.grid)[0]).toEqual([0, 0, 4, 4]);
    });
  });
});

describe('fillRandomEmptyCellInOppositeSide', () => {
  let sampleSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.useFakeTimers();
    // Mock _.sample to control which cell is chosen
    // Ensure lodash is imported if not already
    sampleSpy = vi.spyOn(_, 'sample');
  });

  afterEach(() => {
    vi.restoreAllMocks(); // Restores original _.sample
    vi.useRealTimers();
  });

  describe('direction: up (fills bottom row)', () => {
    it('should fill a random empty cell in the bottom row and move it up to the top', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 3, col: 1 };
      sampleSpy.mockReturnValue(targetCell);
      const initialIncreaseId = mockState.increaseId;

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('up');

      // Check intermediate state (cell filled at bottom - should happen synchronously before await wait)
      expect(getGridValues(mockState.grid)[3][1]).toBe(2);
      const intermediateIncreaseId = mockState.increaseId;
      expect(intermediateIncreaseId).toBeGreaterThan(initialIncreaseId);

      // Now advance timer to resolve the internal wait()
      await vi.advanceTimersToNextTimerAsync();
      // Now await the function completion
      await fillPromise;

      // Check final state (cell moved up)
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 2, 0, 0], // Moved to top
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0], // Original spot now empty
      ]);
      expect(mockState.increaseId).toBeGreaterThan(intermediateIncreaseId);
    });

    it('should not fill if the bottom row is full', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [2, 4, 8, 16],
      ]);
      const initialGrid = getGridValues(mockState.grid);
      sampleSpy.mockReturnValue(undefined); // No empty cell found
      const initialIncreaseId = mockState.increaseId;

      await fillRandomEmptyCellInOppositeSide('up');
      // No timer started if no cell is filled
      // await vi.advanceTimersToNextTimerAsync(); // This would hang if no timer exists

      expect(getGridValues(mockState.grid)).toEqual(initialGrid);
      expect(sampleSpy).toHaveBeenCalled();
      expect(mockState.increaseId).toBe(initialIncreaseId); // ID should not change
    });

    it('should move the new cell up until it hits an obstacle', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 4, 0, 0], // Obstacle
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 3, col: 1 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('up');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[3][1]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 4, 0, 0],
        [0, 2, 0, 0], // Stopped below obstacle
        [0, 0, 0, 0],
      ]);
    });
  });

  describe('direction: down (fills top row)', () => {
    it('should fill a random empty cell in the top row and move it down to the bottom', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 0, col: 2 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('down');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[0][2]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0], // Original spot now empty
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 2, 0], // Moved to bottom
      ]);
    });

    it('should not fill if the top row is full', async () => {
      mockState.grid = createGridFromValues([
        [2, 4, 8, 16],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const initialGrid = getGridValues(mockState.grid);
      sampleSpy.mockReturnValue(undefined);
      await fillRandomEmptyCellInOppositeSide('down');
      expect(getGridValues(mockState.grid)).toEqual(initialGrid);
    });

    it('should move the new cell down until it hits an obstacle', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 4, 0], // Obstacle
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 0, col: 2 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('down');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[0][2]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 2, 0], // Stopped above obstacle
        [0, 0, 4, 0],
        [0, 0, 0, 0],
      ]);
    });
  });

  describe('direction: left (fills right column)', () => {
    it('should fill a random empty cell in the right column and move it left', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 1, col: 3 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('left');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[1][3]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [2, 0, 0, 0], // Moved left
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });

    it('should move the new cell left until it hits an obstacle', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 4, 0, 0], // Obstacle
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 1, col: 3 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('left');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[1][3]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 4, 2, 0], // Stopped right of obstacle
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });
  });

  describe('direction: right (fills left column)', () => {
    it('should fill a random empty cell in the left column and move it right', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 2, col: 0 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('right');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[2][0]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 0, 0, 0],
        [0, 0, 0, 2], // Moved right
        [0, 0, 0, 0],
      ]);
    });

    it('should move the new cell right until it hits an obstacle', async () => {
      mockState.grid = createGridFromValues([
        [0, 0, 0, 0],
        [0, 0, 4, 0], // Obstacle
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
      const targetCell = { row: 1, col: 0 };
      sampleSpy.mockReturnValue(targetCell);

      // Call function but don't await immediately
      const fillPromise = fillRandomEmptyCellInOppositeSide('right');

      // Check intermediate state
      expect(getGridValues(mockState.grid)[1][0]).toBe(2);

      // Advance timer and await completion
      await vi.advanceTimersToNextTimerAsync();
      await fillPromise;

      // Check final state
      expect(getGridValues(mockState.grid)).toEqual([
        [0, 0, 0, 0],
        [0, 2, 4, 0], // Stopped left of obstacle
        [0, 0, 0, 0],
        [0, 0, 0, 0],
      ]);
    });
  });
});
