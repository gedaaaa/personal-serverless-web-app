import { tick } from 'svelte';
import type { DataItem } from '../data/DataSource/DataSource';
import type { VisibleItemsProvider } from '../data/VisibleItemsProvider';

/**
 * 统一的滚动处理函数参数接口
 */
export interface ScrollParams<T extends DataItem> {
  translateY: number;
  itemHeight: number;
  provider: VisibleItemsProvider<T> | null;
  visualHead: number;
  listItemsCount: number;
  items: T[];
}

/**
 * 滚动处理结果接口
 */
export interface ScrollResult<T extends DataItem> {
  translateY: number;
  visualHead: number;
  items: T[];
  success: boolean;
}

/**
 * 统一处理滚动逻辑
 * @param delta 滚动增量（正值表示向下滚动，负值表示向上滚动）
 * @param params 滚动参数
 */
export function handleScroll<T extends DataItem>(
  delta: number,
  params: ScrollParams<T>,
): ScrollResult<T> {
  const {
    translateY,
    itemHeight,
    provider,
    visualHead,
    listItemsCount,
    items,
  } = params;

  if (!provider) {
    return {
      translateY,
      visualHead,
      items,
      success: false,
    };
  }

  // 计算新的translateY
  const newTranslateY = translateY - delta;

  // 处理向下滚动（delta > 0）
  if (delta > 0) {
    // 到达下边界（-2*itemHeight），尝试获取下一个项目
    if (newTranslateY <= -2 * itemHeight) {
      // 使用provider.moveForward尝试获取下一个项目
      const nextItem = provider.moveForward();

      // 如果成功获取到下一个项目
      if (nextItem) {
        // 更新visualHead
        const newVisualHead = (visualHead + 1) % listItemsCount;

        // 计算需要更新的元素索引
        const targetIndex =
          (newVisualHead + listItemsCount - 1) % listItemsCount;

        // 创建新的items数组
        const newItems = [...items];
        newItems[targetIndex] = nextItem;

        return {
          translateY: -itemHeight, // 重置到-itemHeight
          visualHead: newVisualHead,
          items: newItems,
          success: true,
        };
      } else {
        // 如果没有下一个项目（到达列表末尾）
        return {
          translateY: -2 * itemHeight, // 保持在边界位置
          visualHead,
          items,
          success: false,
        };
      }
    }
  }
  // 处理向上滚动（delta < 0）
  else if (delta < 0) {
    // 到达上边界（0），尝试获取前一个项目
    if (newTranslateY >= 0) {
      // 使用provider.moveBackward尝试获取前一个项目
      const prevItem = provider.moveBackward();

      // 如果成功获取到前一个项目
      if (prevItem) {
        // 更新visualHead
        const newVisualHead =
          (visualHead - 1 + listItemsCount) % listItemsCount;

        // 创建新的items数组
        const newItems = [...items];
        newItems[newVisualHead] = prevItem;

        return {
          translateY: -itemHeight, // 重置到-itemHeight
          visualHead: newVisualHead,
          items: newItems,
          success: true,
        };
      } else {
        // 如果没有前一个项目（已经是列表开头）
        return {
          translateY: 0, // 保持在边界位置
          visualHead,
          items,
          success: false,
        };
      }
    }
  }

  // 正常滚动范围内
  return {
    translateY: newTranslateY,
    visualHead,
    items,
    success: true,
  };
}
