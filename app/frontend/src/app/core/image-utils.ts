/**
 * Photo validation and thumbnail helpers (ADR-003 §3.6, AC-09, AC-10).
 * The file name and MIME type are never trusted: the kind is sniffed from the magic bytes.
 */

export type ImageKind = 'jpeg' | 'png' | 'webp';

export const MAX_PHOTO_BYTES = 10 * 1024 * 1024;

const SIGNATURE_BYTES = 12;

const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
const RIFF = [0x52, 0x49, 0x46, 0x46];
const WEBP = [0x57, 0x45, 0x42, 0x50];

function startsWith(bytes: Uint8Array, signature: number[], offset = 0): boolean {
  if (bytes.length < offset + signature.length) {
    return false;
  }
  return signature.every((value, index) => bytes[offset + index] === value);
}

export function detectImageKind(bytes: Uint8Array): ImageKind | null {
  if (startsWith(bytes, [0xff, 0xd8, 0xff])) {
    return 'jpeg';
  }
  if (startsWith(bytes, PNG_SIGNATURE)) {
    return 'png';
  }
  // RIFF (4 bytes) + file size (4 bytes, arbitrary) + "WEBP" at offset 8.
  if (startsWith(bytes, RIFF) && startsWith(bytes, WEBP, 8)) {
    return 'webp';
  }
  return null;
}

export async function detectFileImageKind(file: Blob): Promise<ImageKind | null> {
  const head = new Uint8Array(await file.slice(0, SIGNATURE_BYTES).arrayBuffer());
  return detectImageKind(head);
}

export function isWithinSizeLimit(file: { size: number }): boolean {
  return file.size <= MAX_PHOTO_BYTES;
}

export async function createThumbnailDataUrl(file: Blob, maxSide = 320, quality = 0.7): Promise<string> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  try {
    const longest = Math.max(bitmap.width, bitmap.height);
    const scale = Math.min(1, maxSide / longest);
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('Canvas 2D context is not available');
    }
    context.drawImage(bitmap, 0, 0, width, height);
    return canvas.toDataURL('image/jpeg', quality);
  } finally {
    bitmap.close();
  }
}
