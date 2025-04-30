import type { DataItem, DataSource } from '../data/DataSource/DataSource';

export const state: {
  dataSource: DataSource<DataItem> | null;
  jumpTargetPosition: number;
} = $state({
  dataSource: null,
  jumpTargetPosition: -1,
});

export const setDataSource = (dataSource: DataSource<DataItem>) => {
  state.dataSource = dataSource;
};

export const setJumpTargetPosition = (jumpTargetPosition: number) => {
  state.jumpTargetPosition = jumpTargetPosition;
};
