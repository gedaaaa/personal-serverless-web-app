import type { DataSource, Direction, DataItem } from './DataSource';

import { SkipListDataSource } from './SkipListDataSource/SkipListDataSource';

export class DelayedDataSource<T extends DataItem> implements DataSource<T> {
  private delegate: DataSource<T>;
  private delayMs: number;

  constructor(delay: number = 200) {
    this.delegate = new SkipListDataSource<T>();
    this.delayMs = delay;
  }

  insert(item: T): boolean {
    return this.delegate.insert(item);
  }

  async delete(id: number): Promise<boolean> {
    await this.delay();
    return this.delegate.delete(id);
  }

  async getRangeFromId(
    startId: number,
    count: number,
    direction: Direction,
  ): Promise<T[]> {
    await this.delay();
    return this.delegate.getRangeFromId(startId, count, direction);
  }

  async getTotalCount(): Promise<number> {
    await this.delay();
    return this.delegate.getTotalCount();
  }

  private async delay(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, this.delayMs));
  }
}
