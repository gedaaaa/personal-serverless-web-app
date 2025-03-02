declare module '@testing-library/svelte' {
  import type { ComponentType, SvelteComponent } from 'svelte';

  export interface RenderResult {
    container: HTMLElement;
    component: SvelteComponent;
    debug: (element?: HTMLElement) => void;
    unmount: () => void;
    rerender: (props: Record<string, unknown>) => void;
    findByText: (text: string | RegExp) => Promise<HTMLElement>;
    findAllByText: (text: string | RegExp) => Promise<HTMLElement[]>;
    findByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => Promise<HTMLElement>;
    findAllByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => Promise<HTMLElement[]>;
    findByLabelText: (text: string | RegExp) => Promise<HTMLElement>;
    findAllByLabelText: (text: string | RegExp) => Promise<HTMLElement[]>;
    findByPlaceholderText: (text: string | RegExp) => Promise<HTMLElement>;
    findAllByPlaceholderText: (text: string | RegExp) => Promise<HTMLElement[]>;
    findByTestId: (testId: string | RegExp) => Promise<HTMLElement>;
    findAllByTestId: (testId: string | RegExp) => Promise<HTMLElement[]>;
    getByText: (text: string | RegExp) => HTMLElement;
    getAllByText: (text: string | RegExp) => HTMLElement[];
    getByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement;
    getAllByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement[];
    getByLabelText: (text: string | RegExp) => HTMLElement;
    getAllByLabelText: (text: string | RegExp) => HTMLElement[];
    getByPlaceholderText: (text: string | RegExp) => HTMLElement;
    getAllByPlaceholderText: (text: string | RegExp) => HTMLElement[];
    getByTestId: (testId: string | RegExp) => HTMLElement;
    getAllByTestId: (testId: string | RegExp) => HTMLElement[];
    queryByText: (text: string | RegExp) => HTMLElement | null;
    queryAllByText: (text: string | RegExp) => HTMLElement[];
    queryByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement | null;
    queryAllByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement[];
    queryByLabelText: (text: string | RegExp) => HTMLElement | null;
    queryAllByLabelText: (text: string | RegExp) => HTMLElement[];
    queryByPlaceholderText: (text: string | RegExp) => HTMLElement | null;
    queryAllByPlaceholderText: (text: string | RegExp) => HTMLElement[];
    queryByTestId: (testId: string | RegExp) => HTMLElement | null;
    queryAllByTestId: (testId: string | RegExp) => HTMLElement[];
  }

  export function render<T extends SvelteComponent>(
    component: ComponentType<T>,
    options?: {
      props?: Record<string, unknown>;
      target?: HTMLElement;
    },
  ): RenderResult;

  export function cleanup(): void;

  export const screen: {
    debug: (element?: HTMLElement) => void;
    findByText: (text: string | RegExp) => Promise<HTMLElement>;
    findAllByText: (text: string | RegExp) => Promise<HTMLElement[]>;
    findByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => Promise<HTMLElement>;
    findAllByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => Promise<HTMLElement[]>;
    findByLabelText: (text: string | RegExp) => Promise<HTMLElement>;
    findAllByLabelText: (text: string | RegExp) => Promise<HTMLElement[]>;
    findByPlaceholderText: (text: string | RegExp) => Promise<HTMLElement>;
    findAllByPlaceholderText: (text: string | RegExp) => Promise<HTMLElement[]>;
    findByTestId: (testId: string | RegExp) => Promise<HTMLElement>;
    findAllByTestId: (testId: string | RegExp) => Promise<HTMLElement[]>;
    getByText: (text: string | RegExp) => HTMLElement;
    getAllByText: (text: string | RegExp) => HTMLElement[];
    getByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement;
    getAllByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement[];
    getByLabelText: (text: string | RegExp) => HTMLElement;
    getAllByLabelText: (text: string | RegExp) => HTMLElement[];
    getByPlaceholderText: (text: string | RegExp) => HTMLElement;
    getAllByPlaceholderText: (text: string | RegExp) => HTMLElement[];
    getByTestId: (testId: string | RegExp) => HTMLElement;
    getAllByTestId: (testId: string | RegExp) => HTMLElement[];
    queryByText: (text: string | RegExp) => HTMLElement | null;
    queryAllByText: (text: string | RegExp) => HTMLElement[];
    queryByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement | null;
    queryAllByRole: (
      role: string,
      options?: { name?: string | RegExp },
    ) => HTMLElement[];
    queryByLabelText: (text: string | RegExp) => HTMLElement | null;
    queryAllByLabelText: (text: string | RegExp) => HTMLElement[];
    queryByPlaceholderText: (text: string | RegExp) => HTMLElement | null;
    queryAllByPlaceholderText: (text: string | RegExp) => HTMLElement[];
    queryByTestId: (testId: string | RegExp) => HTMLElement | null;
    queryAllByTestId: (testId: string | RegExp) => HTMLElement[];
  };

  export interface FireEventFunction {
    (element: HTMLElement, event: Event): Promise<void>;
    click: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    submit: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    change: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    input: (
      element: HTMLElement,
      options?: { target?: { value: string } },
    ) => Promise<void>;
    focus: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    blur: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    keyDown: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    keyUp: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
    keyPress: (
      element: HTMLElement,
      options?: Record<string, unknown>,
    ) => Promise<void>;
  }

  export const fireEvent: FireEventFunction;
}
