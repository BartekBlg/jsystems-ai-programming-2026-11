import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LABELS } from '../core/labels';
import { PhotoPickerComponent } from './photo-picker';

const MB = 1024 * 1024;
const JPEG_HEADER = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01];
const GIF_HEADER = [0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00];

function imageFile(
  name: string,
  header: number[],
  size = header.length,
  type = 'image/jpeg',
): File {
  const bytes = new Uint8Array(size);
  bytes.set(header);
  return new File([bytes], name, { type });
}

describe('PhotoPickerComponent', () => {
  let fixture: ComponentFixture<PhotoPickerComponent>;
  let component: PhotoPickerComponent;
  let urlCounter: number;
  let onChange: ReturnType<typeof vi.fn<(value: File | null) => void>>;

  beforeEach(async () => {
    urlCounter = 0;
    globalThis.URL.createObjectURL = vi.fn(() => `blob:preview-${++urlCounter}`);
    globalThis.URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({ imports: [PhotoPickerComponent] }).compileComponents();
    fixture = TestBed.createComponent(PhotoPickerComponent);
    component = fixture.componentInstance;
    onChange = vi.fn<(value: File | null) => void>();
    component.registerOnChange(onChange);
    component.registerOnTouched(() => {});
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function el(selector: string): HTMLElement {
    return fixture.nativeElement.querySelector(selector) as HTMLElement;
  }

  /** Identity check: jsdom File objects are deep-equal to each other, so toHaveBeenCalledWith(file) is not strict enough. */
  function lastValue(): unknown {
    return onChange.mock.lastCall?.[0];
  }

  function chooseFile(file: File): void {
    const input = el('input[type="file"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [file], configurable: true });
    input.dispatchEvent(new Event('change'));
  }

  it('shows the visible label, drop hint and a real choose-photo button', () => {
    expect(el('.label')?.textContent?.trim()).toBe(LABELS.form.photo);
    expect(el('button[type="button"]')?.textContent?.trim()).toBe(LABELS.form.choosePhoto);
    expect(fixture.nativeElement.textContent).toContain(LABELS.form.dropHint);
    expect(el('input[type="file"]')?.getAttribute('accept')).toBe(
      'image/jpeg,image/png,image/webp',
    );
  });

  it('clicking the choose-photo button opens the hidden file input', () => {
    const input = el('input[type="file"]') as HTMLInputElement;
    const clickSpy = vi.spyOn(input, 'click');
    el('button[type="button"]')!.click();
    expect(clickSpy).toHaveBeenCalledTimes(1);
  });

  it('prevents default on dragover and takes the first dropped file', async () => {
    const zone = el('.drop-zone')!;
    const over = new Event('dragover', { cancelable: true });
    zone.dispatchEvent(over);
    expect(over.defaultPrevented).toBe(true);

    const file = imageFile('drop.jpg', JPEG_HEADER);
    const drop = new Event('drop', { cancelable: true });
    Object.defineProperty(drop, 'dataTransfer', { value: { files: [file] } });
    zone.dispatchEvent(drop);
    await vi.waitFor(() => expect(lastValue()).toBe(file));
  });

  it('accepts a file of exactly 10 MB with a JPEG header', async () => {
    const file = imageFile('max.jpg', JPEG_HEADER, 10 * MB);
    chooseFile(file);
    await vi.waitFor(() => expect(lastValue()).toBe(file));
  });

  it('rejects a file of 10 MB + 1 byte with photoTooLarge and keeps no value', async () => {
    const errors: string[] = [];
    component.fileError.subscribe((e) => errors.push(e));
    chooseFile(imageFile('big.jpg', JPEG_HEADER, 10 * MB + 1));
    await vi.waitFor(() => expect(errors).toEqual(['photoTooLarge']));
    expect(onChange).toHaveBeenLastCalledWith(null);
    expect(el('img')).toBeNull();
  });

  it('rejects a GIF named .jpg with photoUnsupported', async () => {
    const errors: string[] = [];
    component.fileError.subscribe((e) => errors.push(e));
    chooseFile(imageFile('fake.jpg', GIF_HEADER, GIF_HEADER.length, 'image/gif'));
    await vi.waitFor(() => expect(errors).toEqual(['photoUnsupported']));
    expect(onChange).toHaveBeenLastCalledWith(null);
  });

  it('accepts a JPEG named .png (kind is sniffed from magic bytes)', async () => {
    const file = imageFile('photo.png', JPEG_HEADER);
    chooseFile(file);
    await vi.waitFor(() => expect(lastValue()).toBe(file));
  });

  it('shows preview, file name and size in Polish format after a valid choice', async () => {
    chooseFile(imageFile('telefon.jpg', JPEG_HEADER, Math.round(1.2 * MB)));
    await vi.waitFor(() => expect(el('img')).not.toBeNull());
    expect(el('img')!.getAttribute('src')).toBe('blob:preview-1');
    expect(el('img')!.getAttribute('alt')).toBe(LABELS.form.photo);
    expect(fixture.nativeElement.textContent).toContain('telefon.jpg');
    expect(fixture.nativeElement.textContent).toContain('1,2 MB');
  });

  it('formats sizes below 1 MB in KB', async () => {
    chooseFile(imageFile('maly.jpg', JPEG_HEADER, 850 * 1024));
    await vi.waitFor(() => expect(fixture.nativeElement.textContent).toContain('850 KB'));
  });

  it('replaces the first photo with the second and revokes the old object URL', async () => {
    const first = imageFile('a.jpg', JPEG_HEADER);
    const second = imageFile('b.jpg', JPEG_HEADER);
    chooseFile(first);
    await vi.waitFor(() => expect(el('img')).not.toBeNull());
    chooseFile(second);
    await vi.waitFor(() => expect(lastValue()).toBe(second));
    fixture.detectChanges();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-1');
    expect(el('img')!.getAttribute('src')).toBe('blob:preview-2');
  });

  it('remove button clears the value and the preview', async () => {
    chooseFile(imageFile('a.jpg', JPEG_HEADER));
    await vi.waitFor(() => expect(el('img')).not.toBeNull());
    const remove = Array.from(fixture.nativeElement.querySelectorAll('button')).find(
      (b) => (b as HTMLElement).textContent?.trim() === LABELS.form.removePhoto,
    ) as HTMLButtonElement;
    remove.click();
    fixture.detectChanges();
    expect(onChange).toHaveBeenLastCalledWith(null);
    expect(el('img')).toBeNull();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-1');
  });

  it('writeValue shows a prefilled photo and revokes its URL on destroy', () => {
    const file = imageFile('pre.jpg', JPEG_HEADER);
    component.writeValue(file);
    fixture.detectChanges();
    expect(el('img')!.getAttribute('src')).toBe('blob:preview-1');
    fixture.destroy();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview-1');
  });

  it('renders the hint input below the picker when it is set', () => {
    fixture.componentRef.setInput('hint', 'Pokaż całe urządzenie.');
    fixture.detectChanges();
    expect(el('.hint')?.textContent?.trim()).toBe('Pokaż całe urządzenie.');
  });
});
