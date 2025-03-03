# 前端组件开发规范

本文档详细说明了我们项目中前端组件的开发规范和最佳实践。这些规范旨在提高代码质量、可维护性和团队协作效率。

## 目录结构

我们采用"目录即组件"的模式来组织前端组件，强调关注点分离和可维护性。

```
/routes/<route-path>/
├── +page.svelte                # 主页面组件
├── _components/                # 特定于此路由的组件（下划线前缀避免路由冲突）
│   ├── ComponentName/          # 每个组件有自己的目录
│   │   ├── index.svelte        # 主组件实现
│   │   ├── index.ts            # 公共API导出（可选）
│   │   ├── internal/           # 内部子组件
│   │   │   ├── SubComponent/   # 每个子组件有自己的目录
│   │   │   │   └── index.svelte
│   │   │   └── ...
│   │   └── __tests__/          # 此组件的测试
│   │       └── ComponentName.spec.ts
│   └── ...
├── _contexts/                  # 状态管理的上下文提供者
│   └── contextName.ts
└── _services/                  # API调用的服务层
    └── serviceName.ts
```

## 核心原则

### 1. 目录即组件

每个组件都有自己的目录，包含所有相关文件。这种方式有以下优势：

- **自包含**：组件的所有相关文件都在一个地方
- **可发现性**：容易找到与组件相关的所有资源
- **封装**：明确组件的边界
- **可扩展**：随着组件复杂度增加，可以轻松添加更多文件

### 2. 下划线前缀

使用 `_` 前缀标记非路由目录，防止 SvelteKit 将其视为路由：

```
/routes/console/users/
├── +page.svelte
├── _components/    # 不会被视为 /console/users/components 路由
├── _contexts/      # 不会被视为 /console/users/contexts 路由
└── _services/      # 不会被视为 /console/users/services 路由
```

### 3. 关注点分离

我们将代码按功能职责分为三个主要部分：

- **UI组件** (`_components/`)：负责渲染和用户交互
- **状态管理** (`_contexts/`)：负责数据状态和业务逻辑
- **API服务** (`_services/`)：负责与后端API通信

这种分离使代码更易于理解、测试和维护。

### 4. 组件隔离

组件应该只从自己的目录或显式导出的API导入：

- 组件不应该直接导入其他组件的内部实现
- 组件应该通过公共API（通常是 `index.ts`）暴露其功能

这种隔离确保了组件之间的松耦合，使重构和测试更容易。

### 5. 内部组件

只被父组件使用的子组件应放在 `internal/` 目录中：

```
ComponentName/
├── index.svelte
├── internal/           # 内部子组件
│   ├── Header/         # 只被ComponentName使用
│   ├── Footer/         # 只被ComponentName使用
│   └── ...
└── ...
```

这种方式明确了哪些组件是内部实现细节，不应被外部直接使用。

### 6. 测试

每个组件都应该有自己的测试，放在 `__tests__/` 目录中：

```
ComponentName/
├── index.svelte
├── __tests__/
│   ├── ComponentName.spec.ts    # 单元测试
│   └── ComponentName.e2e.ts     # 端到端测试（可选）
└── ...
```

## 状态管理

我们使用 Svelte 的上下文 API 进行状态管理，这是一种轻量级但强大的方式来共享状态和行为。

### 上下文创建

```typescript
// _contexts/userManagement.ts
import { writable, type Writable } from 'svelte/store';

// 定义上下文类型
export interface UserManagementContext {
  // 状态
  users: Writable<User[]>;
  // 方法
  loadUsers: () => Promise<void>;
}

// 创建上下文键
export const USER_MANAGEMENT_KEY = Symbol('userManagement');

// 创建上下文工厂函数
export function createUserManagementContext(): UserManagementContext {
  const users = writable<User[]>([]);

  async function loadUsers() {
    // 实现加载用户的逻辑
  }

  return {
    users,
    loadUsers,
  };
}
```

### 上下文提供

```svelte
<!-- +page.svelte -->
<script lang="ts">
  import { setContext } from 'svelte';
  import {
    createUserManagementContext,
    USER_MANAGEMENT_KEY,
  } from './_contexts/userManagement';

  // 创建并设置上下文
  const userManagementContext = createUserManagementContext();
  setContext(USER_MANAGEMENT_KEY, userManagementContext);
</script>
```

### 上下文使用

```svelte
<!-- _components/UserList/index.svelte -->
<script lang="ts">
  import { getContext } from 'svelte';
  import { USER_MANAGEMENT_KEY } from '../../_contexts/userManagement';
  import type { UserManagementContext } from '../../_contexts/userManagement';

  // 获取上下文
  const { users, loadUsers } =
    getContext<UserManagementContext>(USER_MANAGEMENT_KEY);
</script>
```

## 导入约定

### 1. 外部导入

从组件的公共API导入：

```typescript
// 正确
import UserList from '../UserList';

// 错误 - 不要直接导入其他组件的内部实现
import UserList from '../UserList/index.svelte';
```

### 2. 内部导入

直接从文件导入：

```typescript
// 从内部子组件导入
import UserListItem from './internal/UserListItem/index.svelte';
```

### 3. 上下文导入

从上下文文件导入：

```typescript
import {
  USER_MANAGEMENT_KEY,
  type UserManagementContext,
} from '../../_contexts/userManagement';
```

## 组件API设计

### 1. 属性 (Props)

在组件脚本顶部定义属性：

```svelte
<script lang="ts">
  // 必需属性
  export let user: User;

  // 可选属性（带默认值）
  export let isEditable = false;
  export let variant: 'default' | 'compact' = 'default';
</script>
```

### 2. 事件

使用 Svelte 的事件分发机制：

```svelte
<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  // 定义事件类型
  const dispatch = createEventDispatcher<{
    edit: { userId: string };
    delete: { userId: string };
  }>();

  export let user: User;

  function handleEdit() {
    dispatch('edit', { userId: user.id });
  }

  function handleDelete() {
    dispatch('delete', { userId: user.id });
  }
</script>

<button on:click={handleEdit}>编辑</button>
<button on:click={handleDelete}>删除</button>
```

### 3. 插槽

使用命名插槽实现灵活的组件组合：

```svelte
<!-- Card.svelte -->
<div class="card">
  <div class="card-header">
    <slot name="header">默认标题</slot>
  </div>
  <div class="card-body">
    <slot><!-- 默认内容 --></slot>
  </div>
  <div class="card-footer">
    <slot name="footer"></slot>
  </div>
</div>
```

使用插槽：

```svelte
<Card>
  <svelte:fragment slot="header">自定义标题</svelte:fragment>

  主要内容

  <svelte:fragment slot="footer">
    <button>确认</button>
    <button>取消</button>
  </svelte:fragment>
</Card>
```

## 实际示例

以下是用户管理页面的完整结构示例：

```
/routes/console/users/
├── +page.svelte                # 主用户管理页面
├── _components/                # 用户特定组件
│   ├── UserList/               # 用户列表组件
│   │   ├── index.svelte        # 主实现
│   │   ├── index.ts            # 导出
│   │   ├── internal/           # 内部组件
│   │   │   ├── UserListItem/   # 列表项组件
│   │   │   │   ├── index.svelte
│   │   │   │   └── __tests__/
│   │   │   │       └── UserListItem.spec.ts
│   │   │   └── UserListHeader/
│   │   │       ├── index.svelte
│   │   │       └── __tests__/
│   │   │           └── UserListHeader.spec.ts
│   │   └── __tests__/
│   │       └── UserList.spec.ts
│   ├── UserEditModal/          # 编辑用户模态框
│   │   ├── index.svelte
│   │   ├── index.ts
│   │   ├── internal/
│   │   │   └── EditForm/       # 子组件
│   │   │       ├── index.svelte
│   │   │       └── __tests__/
│   │   │           └── EditForm.spec.ts
│   │   └── __tests__/
│   │       └── UserEditModal.spec.ts
│   └── UserDeleteModal/        # 删除确认模态框
│       ├── index.svelte
│       ├── index.ts
│       └── __tests__/
│           └── UserDeleteModal.spec.ts
├── _contexts/
│   └── userManagement.ts       # 用户管理上下文
└── _services/
    ├── userService.ts          # 用户API服务
    └── __tests__/
        └── userService.spec.ts
```

## 代码示例

### 主页面 (+page.svelte)

```svelte
<script lang="ts">
  import { onMount, setContext } from 'svelte';
  import {
    createUserManagementContext,
    USER_MANAGEMENT_KEY,
  } from './_contexts/userManagement';
  import UserList from './_components/UserList';
  import UserEditModal from './_components/UserEditModal';
  import UserDeleteModal from './_components/UserDeleteModal';

  // 创建并设置上下文
  const userManagementContext = createUserManagementContext();
  setContext(USER_MANAGEMENT_KEY, userManagementContext);

  // 初始加载用户数据
  onMount(() => {
    userManagementContext.loadUsers(10);
  });
</script>

<div>
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-medium text-gray-800">用户管理</h1>
  </div>

  <!-- 显示全局错误 -->
  {#if $userManagementContext.error}
    <div class="mb-4 rounded-md bg-red-50 p-4 text-red-600">
      {$userManagementContext.error}
    </div>
  {/if}

  <UserList />
  <UserEditModal />
  <UserDeleteModal />
</div>
```

### 用户列表组件 (UserList/index.svelte)

```svelte
<script lang="ts">
  import { getContext } from 'svelte';
  import { USER_MANAGEMENT_KEY } from '../../_contexts/userManagement';
  import type { UserManagementContext } from '../../_contexts/userManagement';
  import UserListItem from './internal/UserListItem/index.svelte';

  // 获取上下文
  const { users, nextCursor, hasMore, loading, loadUsers } =
    getContext<UserManagementContext>(USER_MANAGEMENT_KEY);

  // 加载更多用户
  function loadMore() {
    if ($hasMore && $nextCursor) {
      loadUsers(10, $nextCursor);
    }
  }
</script>

<!-- 用户列表UI -->
<div class="overflow-x-auto rounded-lg bg-white shadow">
  <table class="min-w-full divide-y divide-gray-200">
    <thead class="bg-gray-50">
      <tr>
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
          >用户名</th
        >
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
          >邮箱</th
        >
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
          >姓名</th
        >
        <th
          scope="col"
          class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500"
          >角色</th
        >
        <th
          scope="col"
          class="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500"
          >操作</th
        >
      </tr>
    </thead>
    <tbody class="divide-y divide-gray-200 bg-white">
      {#if $loading && $users.length === 0}
        <tr>
          <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
            >加载中...</td
          >
        </tr>
      {:else if $users.length === 0}
        <tr>
          <td colspan="5" class="px-6 py-4 text-center text-sm text-gray-500"
            >暂无用户数据</td
          >
        </tr>
      {:else}
        {#each $users as user (user.id)}
          <UserListItem {user} />
        {/each}
      {/if}
    </tbody>
  </table>

  {#if $hasMore}
    <div class="flex justify-center border-t border-gray-200 px-4 py-3 sm:px-6">
      <button
        on:click={loadMore}
        disabled={$loading}
        class="inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:opacity-50"
      >
        {$loading ? '加载中...' : '加载更多'}
      </button>
    </div>
  {/if}
</div>
```

## 总结

遵循这些组件开发规范将带来以下好处：

1. **一致性**：所有组件遵循相同的结构和约定
2. **可维护性**：清晰的结构使代码更易于理解和修改
3. **可测试性**：组件和逻辑的分离使测试更容易
4. **可扩展性**：结构设计考虑了未来的扩展需求
5. **协作效率**：团队成员可以更容易地理解和贡献代码

这些规范不是一成不变的，随着项目的发展和团队的反馈，我们会不断完善和改进这些规范。
