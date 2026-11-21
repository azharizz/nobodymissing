/** Code-point string order. Same as Kotlin's String.compareTo, so web and Android sort identically. */
export const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
