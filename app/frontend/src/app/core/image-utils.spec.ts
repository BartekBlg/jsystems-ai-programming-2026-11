import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  MAX_PHOTO_BYTES,
  createThumbnailDataUrl,
  detectFileImageKind,
  detectImageKind,
  isWithinSizeLimit,
} from './image-utils';

const JPEG = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10];
const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d];
const WEBP = [0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50];
const GIF = [0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00];
const RIFF_WAVE = [0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45];

describe('detectImageKind', () => {
  it('detects JPEG by FF D8 FF', () => {
    expect(detectImageKind(new Uint8Array(JPEG))).toBe('jpeg');
  });

  it('detects PNG by its 8-byte signature', () => {
    expect(detectImageKind(new Uint8Array(PNG))).toBe('png');
  });

  it('detects WebP by RIFF....WEBP', () => {
    expect(detectImageKind(new Uint8Array(WEBP))).toBe('webp');
  });

  it('returns null for GIF', () => {
    expect(detectImageKind(new Uint8Array(GIF))).toBeNull();
  });

  it('returns null for RIFF container that is not WEBP', () => {
    expect(detectImageKind(new Uint8Array(RIFF_WAVE))).toBeNull();
  });

  it('returns null for fewer bytes than the signature needs', () => {
    expect(detectImageKind(new Uint8Array([0xff, 0xd8, 0xff]).subarray(0, 2))).toBeNull();
    expect(detectImageKind(new Uint8Array([0x89, 0x50, 0x4e]))).toBeNull();
  });

  it('returns null for 3 unrelated bytes', () => {
    expect(detectImageKind(new Uint8Array([0x01, 0x02, 0x03]))).toBeNull();
  });

  it('returns null for empty input', () => {
    expect(detectImageKind(new Uint8Array([]))).toBeNull();
  });
});

describe('detectFileImageKind', () => {
  it('ignores the file name and MIME type and sniffs the content', async () => {
    const file = new File([new Uint8Array(JPEG)], 'photo.png', { type: 'image/png' });
    await expect(detectFileImageKind(file)).resolves.toBe('jpeg');
  });

  it('returns null for GIF content even when named and typed as JPEG', async () => {
    const file = new File([new Uint8Array(GIF)], 'x.jpg', { type: 'image/jpeg' });
    await expect(detectFileImageKind(file)).resolves.toBeNull();
  });
});

describe('isWithinSizeLimit', () => {
  it('accepts exactly 10 MB', () => {
    expect(MAX_PHOTO_BYTES).toBe(10485760);
    expect(isWithinSizeLimit({ size: 10485760 })).toBe(true);
  });

  it('rejects one byte above 10 MB', () => {
    expect(isWithinSizeLimit({ size: 10485761 })).toBe(false);
  });
});

describe('createThumbnailDataUrl', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  function stubImage(width: number, height: number) {
    const close = vi.fn();
    const createImageBitmap = vi.fn().mockResolvedValue({ width, height, close });
    vi.stubGlobal('createImageBitmap', createImageBitmap);

    const drawImage = vi.fn();
    const context = { drawImage } as unknown as CanvasRenderingContext2D;
    const toDataURL = vi
      .spyOn(HTMLCanvasElement.prototype, 'toDataURL')
      .mockReturnValue('data:image/jpeg;base64,AAAA');
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(context as never);
    const file = new File([new Uint8Array(JPEG)], 'photo.jpg', { type: 'image/jpeg' });
    return { createImageBitmap, close, drawImage, toDataURL, file };
  }

  it('scales the longer side down to maxSide and keeps the aspect ratio', async () => {
    const { createImageBitmap, file, toDataURL } = stubImage(1600, 1200);
    const widthSpy = vi.spyOn(HTMLCanvasElement.prototype, 'width', 'set');
    const heightSpy = vi.spyOn(HTMLCanvasElement.prototype, 'height', 'set');

    const url = await createThumbnailDataUrl(file);

    expect(createImageBitmap).toHaveBeenCalledWith(file, { imageOrientation: 'from-image' });
    expect(widthSpy).toHaveBeenLastCalledWith(320);
    expect(heightSpy).toHaveBeenLastCalledWith(240);
    expect(toDataURL).toHaveBeenCalledWith('image/jpeg', 0.7);
    expect(url).toBe('data:image/jpeg;base64,AAAA');
  });

  it('never upscales a small image', async () => {
    const { file } = stubImage(200, 100);
    const widthSpy = vi.spyOn(HTMLCanvasElement.prototype, 'width', 'set');
    const heightSpy = vi.spyOn(HTMLCanvasElement.prototype, 'height', 'set');

    await createThumbnailDataUrl(file);

    expect(widthSpy).toHaveBeenLastCalledWith(200);
    expect(heightSpy).toHaveBeenLastCalledWith(100);
  });

  it('closes the decoded bitmap after drawing', async () => {
    const { close, file } = stubImage(1600, 1200);
    await createThumbnailDataUrl(file);
    expect(close).toHaveBeenCalled();
  });
});
