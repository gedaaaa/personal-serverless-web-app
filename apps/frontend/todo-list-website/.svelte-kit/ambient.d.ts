
// this file is generated — do not edit it


/// <reference types="@sveltejs/kit" />

/**
 * Environment variables [loaded by Vite](https://vitejs.dev/guide/env-and-mode.html#env-files) from `.env` files and `process.env`. Like [`$env/dynamic/private`](https://svelte.dev/docs/kit/$env-dynamic-private), this module cannot be imported into client-side code. This module only includes variables that _do not_ begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) _and do_ start with [`config.kit.env.privatePrefix`](https://svelte.dev/docs/kit/configuration#env) (if configured).
 * 
 * _Unlike_ [`$env/dynamic/private`](https://svelte.dev/docs/kit/$env-dynamic-private), the values exported from this module are statically injected into your bundle at build time, enabling optimisations like dead code elimination.
 * 
 * ```ts
 * import { API_KEY } from '$env/static/private';
 * ```
 * 
 * Note that all environment variables referenced in your code should be declared (for example in an `.env` file), even if they don't have a value until the app is deployed:
 * 
 * ```
 * MY_FEATURE_FLAG=""
 * ```
 * 
 * You can override `.env` values from the command line like so:
 * 
 * ```bash
 * MY_FEATURE_FLAG="enabled" npm run dev
 * ```
 */
declare module '$env/static/private' {
	export const NX_CLI_SET: string;
	export const NVM_INC: string;
	export const SST_SERVER: string;
	export const NX_LOAD_DOT_ENV_FILES: string;
	export const NVM_CD_FLAGS: string;
	export const SHELL: string;
	export const PULUMI_NODEJS_ORGANIZATION: string;
	export const TERM: string;
	export const SST_AWS_SESSION_TOKEN: string;
	export const HOMEBREW_REPOSITORY: string;
	export const TMPDIR: string;
	export const PULUMI_SKIP_UPDATE_CHECK: string;
	export const PULUMI_NODEJS_MONITOR: string;
	export const NODE_OPTIONS: string;
	export const PULUMI_NODEJS_STACK: string;
	export const NX_TASK_TARGET_TARGET: string;
	export const NVM_DIR: string;
	export const PULUMI_NODEJS_ENGINE: string;
	export const USER: string;
	export const SST_AWS_ACCESS_KEY_ID: string;
	export const COMMAND_MODE: string;
	export const NX_TASK_HASH: string;
	export const SSH_AUTH_SOCK: string;
	export const __CF_USER_TEXT_ENCODING: string;
	export const SST_AWS_SECRET_ACCESS_KEY: string;
	export const SST: string;
	export const PULUMI_CONFIG_PASSPHRASE: string;
	export const NX_TASK_TARGET_PROJECT: string;
	export const PATH: string;
	export const PULUMI_NODEJS_PARALLEL: string;
	export const NX_WORKSPACE_ROOT: string;
	export const IJ_RESTARTER_LOG: string;
	export const __CFBundleIdentifier: string;
	export const npm_command: string;
	export const PWD: string;
	export const PULUMI_NODEJS_PROJECT: string;
	export const PULUMI_CONFIG: string;
	export const PULUMI_NODEJS_SYNC: string;
	export const NODE_PATH: string;
	export const PULUMI_DEBUG_COMMANDS: string;
	export const NX_VERBOSE_LOGGING: string;
	export const XPC_FLAGS: string;
	export const SST_AWS_REGION: string;
	export const PULUMI_PROJECT: string;
	export const FORCE_COLOR: string;
	export const XPC_SERVICE_NAME: string;
	export const HOME: string;
	export const PULUMI_CONFIG_SECRET_KEYS: string;
	export const NX_TASK_TARGET_CONFIGURATION: string;
	export const SHLVL: string;
	export const HOMEBREW_PREFIX: string;
	export const LOGNAME: string;
	export const PNPM_PACKAGE_NAME: string;
	export const LC_CTYPE: string;
	export const LERNA_PACKAGE_NAME: string;
	export const NVM_BIN: string;
	export const PULUMI_HOME: string;
	export const npm_config_user_agent: string;
	export const SST_RESOURCE_App: string;
	export const INFOPATH: string;
	export const HOMEBREW_CELLAR: string;
	export const PULUMI_BACKEND_URL: string;
	export const TOOLBOX_VERSION: string;
	export const PULUMI_NODEJS_TYPESCRIPT: string;
	export const NODE_ENV: string;
}

/**
 * Similar to [`$env/static/private`](https://svelte.dev/docs/kit/$env-static-private), except that it only includes environment variables that begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) (which defaults to `PUBLIC_`), and can therefore safely be exposed to client-side code.
 * 
 * Values are replaced statically at build time.
 * 
 * ```ts
 * import { PUBLIC_BASE_URL } from '$env/static/public';
 * ```
 */
declare module '$env/static/public' {
	
}

/**
 * This module provides access to runtime environment variables, as defined by the platform you're running on. For example if you're using [`adapter-node`](https://github.com/sveltejs/kit/tree/main/packages/adapter-node) (or running [`vite preview`](https://svelte.dev/docs/kit/cli)), this is equivalent to `process.env`. This module only includes variables that _do not_ begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) _and do_ start with [`config.kit.env.privatePrefix`](https://svelte.dev/docs/kit/configuration#env) (if configured).
 * 
 * This module cannot be imported into client-side code.
 * 
 * Dynamic environment variables cannot be used during prerendering.
 * 
 * ```ts
 * import { env } from '$env/dynamic/private';
 * console.log(env.DEPLOYMENT_SPECIFIC_VARIABLE);
 * ```
 * 
 * > In `dev`, `$env/dynamic` always includes environment variables from `.env`. In `prod`, this behavior will depend on your adapter.
 */
declare module '$env/dynamic/private' {
	export const env: {
		NX_CLI_SET: string;
		NVM_INC: string;
		SST_SERVER: string;
		NX_LOAD_DOT_ENV_FILES: string;
		NVM_CD_FLAGS: string;
		SHELL: string;
		PULUMI_NODEJS_ORGANIZATION: string;
		TERM: string;
		SST_AWS_SESSION_TOKEN: string;
		HOMEBREW_REPOSITORY: string;
		TMPDIR: string;
		PULUMI_SKIP_UPDATE_CHECK: string;
		PULUMI_NODEJS_MONITOR: string;
		NODE_OPTIONS: string;
		PULUMI_NODEJS_STACK: string;
		NX_TASK_TARGET_TARGET: string;
		NVM_DIR: string;
		PULUMI_NODEJS_ENGINE: string;
		USER: string;
		SST_AWS_ACCESS_KEY_ID: string;
		COMMAND_MODE: string;
		NX_TASK_HASH: string;
		SSH_AUTH_SOCK: string;
		__CF_USER_TEXT_ENCODING: string;
		SST_AWS_SECRET_ACCESS_KEY: string;
		SST: string;
		PULUMI_CONFIG_PASSPHRASE: string;
		NX_TASK_TARGET_PROJECT: string;
		PATH: string;
		PULUMI_NODEJS_PARALLEL: string;
		NX_WORKSPACE_ROOT: string;
		IJ_RESTARTER_LOG: string;
		__CFBundleIdentifier: string;
		npm_command: string;
		PWD: string;
		PULUMI_NODEJS_PROJECT: string;
		PULUMI_CONFIG: string;
		PULUMI_NODEJS_SYNC: string;
		NODE_PATH: string;
		PULUMI_DEBUG_COMMANDS: string;
		NX_VERBOSE_LOGGING: string;
		XPC_FLAGS: string;
		SST_AWS_REGION: string;
		PULUMI_PROJECT: string;
		FORCE_COLOR: string;
		XPC_SERVICE_NAME: string;
		HOME: string;
		PULUMI_CONFIG_SECRET_KEYS: string;
		NX_TASK_TARGET_CONFIGURATION: string;
		SHLVL: string;
		HOMEBREW_PREFIX: string;
		LOGNAME: string;
		PNPM_PACKAGE_NAME: string;
		LC_CTYPE: string;
		LERNA_PACKAGE_NAME: string;
		NVM_BIN: string;
		PULUMI_HOME: string;
		npm_config_user_agent: string;
		SST_RESOURCE_App: string;
		INFOPATH: string;
		HOMEBREW_CELLAR: string;
		PULUMI_BACKEND_URL: string;
		TOOLBOX_VERSION: string;
		PULUMI_NODEJS_TYPESCRIPT: string;
		NODE_ENV: string;
		[key: `PUBLIC_${string}`]: undefined;
		[key: `${string}`]: string | undefined;
	}
}

/**
 * Similar to [`$env/dynamic/private`](https://svelte.dev/docs/kit/$env-dynamic-private), but only includes variables that begin with [`config.kit.env.publicPrefix`](https://svelte.dev/docs/kit/configuration#env) (which defaults to `PUBLIC_`), and can therefore safely be exposed to client-side code.
 * 
 * Note that public dynamic environment variables must all be sent from the server to the client, causing larger network requests — when possible, use `$env/static/public` instead.
 * 
 * Dynamic environment variables cannot be used during prerendering.
 * 
 * ```ts
 * import { env } from '$env/dynamic/public';
 * console.log(env.PUBLIC_DEPLOYMENT_SPECIFIC_VARIABLE);
 * ```
 */
declare module '$env/dynamic/public' {
	export const env: {
		[key: `PUBLIC_${string}`]: string | undefined;
	}
}
