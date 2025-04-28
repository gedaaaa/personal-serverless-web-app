/**
 * Hashes a string using SHA-256
 * @param input The string to hash
 * @returns The hashed string
 */
export async function sha256(input: string): Promise<string> {
  // Encode the password to Uint8Array
  const encoder = new TextEncoder();
  const data = encoder.encode(input);

  // Compute the SHA-256 hash
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);

  // Convert ArrayBuffer to Uint8Array
  const hashArray = new Uint8Array(hashBuffer);

  // Convert bytes to hex string
  return Array.from(hashArray)
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('');
}
