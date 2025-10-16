


export interface FlowModule {
    // @ts-ignore
    readonly modulePath: string;


    [functionName: string]: (...args: any[]) => any;
}

/**
 * Load a Flow module from a file
 *
 * @param modulePath - Path to the .flow file
 * @returns Module with callable functions
 *
 * @example
 * const mod = loadModule('math.flow');
 * const result = mod.add(10, 20);
 */
export function loadModule(modulePath: string): FlowModule;

/**
 * Compile Flow source code from a string
 *
 * @param source - Flow source code
 * @returns Module with callable functions
 *
 * @example
 * const mod = compile(`
 *   func add(a: int, b: int) -> int {
 *       return a + b;
 *   }
 * `);
 * const result = mod.add(10, 20);
 */
export function compile(source: string): FlowModule;

/**
 * Get Flow language version
 *
 * @returns Version string
 */
export function version(): string;

