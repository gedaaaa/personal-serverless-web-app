<script lang="ts">
  import _ from 'lodash';
  import { tick } from 'svelte';
  import { flip } from 'svelte/animate';

  const cellMoveDuration = 200;

  let isMoving = $state(false);
  const initialGrid = [
    [
      { value: 0, id: 0 },
      { value: 0, id: 1 },
      { value: 0, id: 2 },
      { value: 0, id: 3 },
    ],
    [
      { value: 0, id: 4 },
      { value: 0, id: 5 },
      { value: 0, id: 6 },
      { value: 0, id: 7 },
    ],
    [
      { value: 0, id: 8 },
      { value: 0, id: 9 },
      { value: 0, id: 10 },
      { value: 0, id: 11 },
    ],
    [
      { value: 0, id: 12 },
      { value: 0, id: 13 },
      { value: 0, id: 14 },
      { value: 2, id: 15 },
    ],
  ];

  let grid: { value: number; id: number }[][] = $state(initialGrid);

  function getFlatGrid() {
    return grid.flatMap((row) => row);
  }

  let increaseId = 16;

  function getColorClass(value: number) {
    switch (value) {
      case 0:
        return 'bg-gray-200 text-transparent border-transparent';
      case 2:
        return 'bg-blue-200 text-blue-400';
      case 4:
        return 'bg-green-200 text-green-400';
      case 8:
        return 'bg-yellow-200 text-yellow-400';
      case 16:
        return 'bg-orange-200 text-orange-400';
      case 32:
        return 'bg-red-200 text-red-400';
      case 64:
        return 'bg-purple-200 text-purple-400';
      case 128:
        return 'bg-pink-200 text-pink-400';
      case 256:
        return 'bg-brown-200 text-brown-400';
      case 512:
        return 'bg-gray-200 text-gray-400';
      case 1024:
        return 'bg-gray-400 text-gray-600';
      case 2048:
        return 'bg-gray-600 text-gray-100';
      default:
        return 'bg-gray-200 text-gray-400';
    }
  }

  async function fillFromBottom() {
    const entranceRow = 3;
    const entranceColCandidates = grid[entranceRow]
      .map((cell, col) => ({ cell, col }))
      .filter(({ cell }) => cell.value == 0)
      .map(({ col }) => col);

    // intial state: fill random empty cell in bottom row with 2.
    const entranceCol = _.sample(entranceColCandidates);
    if (_.isNumber(entranceCol)) {
      const nextId = increaseId++;
      grid[entranceRow][entranceCol] = { value: 2, id: nextId };
      await tick();

      let finalRow = entranceRow;
      while (finalRow > 0) {
        if (grid[finalRow - 1][entranceCol].value === 0) {
          finalRow--;
        } else {
          break;
        }
      }

      grid[entranceRow][entranceCol] = { value: 0, id: increaseId++ };
      grid[finalRow][entranceCol] = { value: 2, id: nextId };
    }
  }

  function calculateMoveUp() {
    let moved = false;
    for (let col = 0; col < 4; col++) {
      let lastExamedRow = 0;
      for (let row = 0; row < 4; row++) {
        let currentValue = grid[row][col].value;
        examing: for (
          let examingRow = lastExamedRow + 1;
          examingRow < 4;
          examingRow++
        ) {
          lastExamedRow = examingRow;
          let examingValue = grid[examingRow][col].value;
          if (examingValue === 0) {
            continue examing;
          }
          if (currentValue === examingValue) {
            currentValue = currentValue * 2;
            grid[row][col] = {
              value: currentValue,
              id: grid[examingRow][col].id,
            };
            grid[examingRow][col] = { value: 0, id: increaseId++ };
            moved = true;
            break examing;
          }
          if (currentValue === 0) {
            currentValue = examingValue;
            grid[row][col] = {
              value: currentValue,
              id: grid[examingRow][col].id,
            };
            grid[examingRow][col] = { value: 0, id: increaseId++ };
            moved = true;
            continue examing;
          }

          const examingCellId = grid[examingRow][col].id;

          grid[examingRow][col] = { value: 0, id: increaseId++ };
          grid[row + 1][col] = {
            value: examingValue,
            id: examingCellId,
          };
          moved = true;
          break examing;
        }
      }
    }
    return moved;
  }

  async function moveUp() {
    if (isMoving) {
      return;
    }
    isMoving = true;
    const moved = calculateMoveUp();
    if (moved) {
      await waitForCellMove();
      await fillFromBottom();
    }
    isMoving = false;
  }

  async function fillFromTop() {
    const entranceRow = 0;
    const entranceColCandidates = grid[entranceRow]
      .map((cell, col) => ({ cell, col }))
      .filter(({ cell }) => cell.value == 0)
      .map(({ col }) => col);
    const entranceCol = _.sample(entranceColCandidates);
    if (_.isNumber(entranceCol)) {
      const nextId = increaseId++;
      grid[entranceRow][entranceCol] = { value: 2, id: nextId };
      await tick();

      let finalRow = entranceRow;
      while (finalRow < 3) {
        if (grid[finalRow + 1][entranceCol].value === 0) {
          finalRow++;
        } else {
          break;
        }
      }

      grid[entranceRow][entranceCol] = { value: 0, id: increaseId++ };
      grid[finalRow][entranceCol] = { value: 2, id: nextId };
    }
  }

  function calculateMoveDown() {
    let moved = false;
    for (let col = 0; col < 4; col++) {
      let lastExamedRow = 3;
      for (let row = 3; row >= 0; row--) {
        let currentValue = grid[row][col].value;
        examing: for (
          let examingRow = lastExamedRow - 1;
          examingRow >= 0;
          examingRow--
        ) {
          lastExamedRow--;
          let examingValue = grid[examingRow][col].value;
          if (examingValue === 0) {
            continue examing;
          }
          if (currentValue === examingValue) {
            currentValue = currentValue * 2;
            grid[row][col] = {
              value: currentValue,
              id: grid[examingRow][col].id,
            };
            grid[examingRow][col] = { value: 0, id: increaseId++ };
            moved = true;
            break examing;
          }
          if (currentValue === 0) {
            currentValue = examingValue;
            grid[row][col] = {
              value: currentValue,
              id: grid[examingRow][col].id,
            };
            grid[examingRow][col] = { value: 0, id: increaseId++ };
            moved = true;
            continue examing;
          }

          const examingCellId = grid[examingRow][col].id;

          grid[examingRow][col] = { value: 0, id: increaseId++ };
          grid[row - 1][col] = {
            value: examingValue,
            id: examingCellId,
          };
          moved = true;
          break examing;
        }
      }
    }
    return moved;
  }

  async function moveDown() {
    if (isMoving) {
      return;
    }
    isMoving = true;
    const moved = calculateMoveDown();
    if (moved) {
      await waitForCellMove();
      await fillFromTop();
    }
    isMoving = false;
  }

  async function fillFromRight() {
    const entranceCol = 3;
    const entranceRowCandidates = grid
      .map((row) => row[entranceCol])
      .map((cell, row) => ({ cell, row }))
      .filter(({ cell }) => cell.value == 0)
      .map(({ row }) => row);

    const entranceRow = _.sample(entranceRowCandidates);

    if (_.isNumber(entranceRow)) {
      const nextId = increaseId++;
      grid[entranceRow][entranceCol] = { value: 2, id: nextId };
      await tick();

      let finalCol = entranceCol;
      while (finalCol > 0) {
        if (grid[entranceRow][finalCol - 1].value === 0) {
          finalCol--;
        } else {
          break;
        }
      }

      grid[entranceRow][entranceCol] = { value: 0, id: increaseId++ };
      grid[entranceRow][finalCol] = { value: 2, id: nextId };
    }
  }

  function calculateMoveLeft() {
    let moved = false;
    for (let row = 0; row < 4; row++) {
      let lastExamedCol = 0;
      for (let col = 0; col < 4; col++) {
        let currentValue = grid[row][col].value;
        examing: for (
          let examingCol = lastExamedCol + 1;
          examingCol < 4;
          examingCol++
        ) {
          lastExamedCol = examingCol;
          let examingValue = grid[row][examingCol].value;
          if (examingValue === 0) {
            continue examing;
          }
          if (currentValue === examingValue) {
            currentValue = currentValue * 2;
            grid[row][col] = {
              value: currentValue,
              id: grid[row][examingCol].id,
            };
            grid[row][examingCol] = { value: 0, id: increaseId++ };
            moved = true;
            break examing;
          }
          if (currentValue === 0) {
            currentValue = examingValue;
            grid[row][col] = {
              value: currentValue,
              id: grid[row][examingCol].id,
            };
            grid[row][examingCol] = { value: 0, id: increaseId++ };
            moved = true;
            continue examing;
          }

          const examingCellId = grid[row][examingCol].id;

          grid[row][examingCol] = { value: 0, id: increaseId++ };
          grid[row][col + 1] = {
            value: examingValue,
            id: examingCellId,
          };
          moved = true;
          break examing;
        }
      }
    }
    return moved;
  }

  async function moveLeft() {
    if (isMoving) {
      return;
    }
    isMoving = true;
    const moved = calculateMoveLeft();
    if (moved) {
      await waitForCellMove();
      await fillFromRight();
    }
    isMoving = false;
  }

  async function fillFromLeft() {
    const entranceCol = 0;
    const entranceRowCandidates = grid
      .map((row) => row[entranceCol])
      .map((cell, row) => ({ cell, row }))
      .filter(({ cell }) => cell.value == 0)
      .map(({ row }) => row);
    const entranceRow = _.sample(entranceRowCandidates);
    if (_.isNumber(entranceRow)) {
      const nextId = increaseId++;
      grid[entranceRow][entranceCol] = { value: 2, id: nextId };
      await tick();

      if (_.isNumber(entranceRow)) {
        let finalCol = entranceCol;
        while (finalCol < 3) {
          if (grid[entranceRow][finalCol + 1].value === 0) {
            finalCol++;
          } else {
            break;
          }
        }

        grid[entranceRow][entranceCol] = { value: 0, id: increaseId++ };
        grid[entranceRow][finalCol] = { value: 2, id: nextId };
      }
    }
  }

  function calculateMoveRight() {
    let moved = false;
    for (let row = 0; row < 4; row++) {
      let lastExamedCol = 3;
      for (let col = 3; col >= 0; col--) {
        let currentValue = grid[row][col].value;
        examing: for (
          let examingCol = lastExamedCol - 1;
          examingCol >= 0;
          examingCol--
        ) {
          lastExamedCol = examingCol;
          let examingValue = grid[row][examingCol].value;
          if (examingValue === 0) {
            continue examing;
          }
          if (currentValue === examingValue) {
            currentValue = currentValue * 2;
            grid[row][col] = {
              value: currentValue,
              id: grid[row][examingCol].id,
            };
            grid[row][examingCol] = { value: 0, id: increaseId++ };
            moved = true;
            break examing;
          }
          if (currentValue === 0) {
            currentValue = examingValue;
            grid[row][col] = {
              value: currentValue,
              id: grid[row][examingCol].id,
            };
            grid[row][examingCol] = { value: 0, id: increaseId++ };
            moved = true;
            continue examing;
          }

          const examingCellId = grid[row][examingCol].id;

          grid[row][examingCol] = { value: 0, id: increaseId++ };
          grid[row][col - 1] = {
            value: examingValue,
            id: examingCellId,
          };
          moved = true;
          break examing;
        }
      }
    }
    return moved;
  }

  async function moveRight() {
    if (isMoving) {
      return;
    }
    isMoving = true;
    const moved = calculateMoveRight();
    if (moved) {
      await waitForCellMove();
      await fillFromLeft();
    }
    isMoving = false;
  }

  function isGameWon() {
    return grid.some((row) => row.some((cell) => cell.value === 2048));
  }

  function reset() {
    grid = initialGrid;
    increaseId = 16;
  }

  async function waitForCellMove() {
    await new Promise((resolve) => setTimeout(resolve, cellMoveDuration));
  }
</script>

<div class="flex h-full w-full flex-col items-center justify-center">
  <div class="flex flex-row items-center justify-center gap-4">
    {#if isGameWon()}
      YOU WIN!
    {/if}
    <button
      disabled={isMoving}
      class="rounded-md bg-blue-500 px-4 py-2 text-white"
      onclick={reset}
    >
      Reset
    </button>
  </div>

  <div
    class="my-4 grid grid-cols-4 gap-4 rounded-md border-2 border-gray-200 bg-gray-100 p-4"
  >
    {#each getFlatGrid() as cell (cell.id)}
      {@const colorClass = getColorClass(cell.value)}
      <div
        animate:flip={{ duration: cellMoveDuration }}
        class="flex h-10 w-10 items-center justify-center rounded-md {colorClass}"
      >
        <div>{cell.value}</div>
      </div>
    {/each}
  </div>

  <button
    disabled={isMoving}
    class="mt-4 rounded-md bg-blue-500 px-4 py-2 text-white"
    onclick={moveUp}
  >
    Move Up
  </button>
  <button
    disabled={isMoving}
    class="mt-4 rounded-md bg-blue-500 px-4 py-2 text-white"
    onclick={moveDown}
  >
    Move Down
  </button>
  <button
    disabled={isMoving}
    class="mt-4 rounded-md bg-blue-500 px-4 py-2 text-white"
    onclick={moveLeft}
  >
    Move Left
  </button>
  <button
    disabled={isMoving}
    class="mt-4 rounded-md bg-blue-500 px-4 py-2 text-white"
    onclick={moveRight}
  >
    Move Right
  </button>
</div>
