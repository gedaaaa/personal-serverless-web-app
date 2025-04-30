import { resetScroll } from '../store/scroll.svelte';
import { state as listState } from '../store/list.svelte';
import { state as dataState } from '../store/data.svelte';

let lastVersion: number | null = null;

export default function registerJumpReactive() {
  // jump to target position when jump target position is set
  $effect(() => {
    if (dataState.jumpTargetPosition >= 0) {
      listState.dataWindowProvider?.setDataWindowPosition(
        dataState.jumpTargetPosition,
      );
    }
  });

  // update data window items when data window provider version changes (async update for jump result)
  $effect(() => {
    const version = listState.dataWindowProvider?.version ?? null;
    if (version !== null && version !== lastVersion) {
      listState.dataWindowItems =
        listState.dataWindowProvider?.getDataWindowItems() ?? [];
      resetScroll();
      lastVersion = version;
    }
  });
}
