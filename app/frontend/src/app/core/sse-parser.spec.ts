import { describe, expect, it } from 'vitest';
import { loadSseRecording } from '../../testing/recording-loader';
import { SseEvent, SseParser } from './sse-parser';

function parseChunks(chunks: string[]): SseEvent[] {
  const parser = new SseParser();
  const events: SseEvent[] = [];
  for (const chunk of chunks) {
    events.push(...parser.push(chunk));
  }
  events.push(...parser.flush());
  return events;
}

describe('SseParser', () => {
  it('parses a single event', () => {
    expect(parseChunks(['event: delta\ndata: hi\n\n'])).toEqual([{ event: 'delta', data: 'hi' }]);
  });

  it('parses two events delivered in one chunk', () => {
    expect(parseChunks(['event: a\ndata: 1\n\nevent: b\ndata: 2\n\n'])).toEqual([
      { event: 'a', data: '1' },
      { event: 'b', data: '2' },
    ]);
  });

  it('accepts CRLF line endings', () => {
    expect(parseChunks(['event: a\r\ndata: x\r\n\r\n'])).toEqual([{ event: 'a', data: 'x' }]);
  });

  it('accepts a lone CR as line ending', () => {
    expect(parseChunks(['event: a\rdata: x\r\r'])).toEqual([{ event: 'a', data: 'x' }]);
  });

  it('treats a CR at the end of a chunk followed by LF in the next chunk as one line break', () => {
    const parser = new SseParser();
    expect(parser.push('data: a\r')).toEqual([]);
    // The LF completes the CRLF pair, so it must not terminate the (still open) event.
    expect(parser.push('\n')).toEqual([]);
    expect(parser.push('\n')).toEqual([{ event: 'message', data: 'a' }]);
  });

  it('ignores comment lines, including heartbeats between events', () => {
    expect(parseChunks([': keep-alive\n\n'])).toEqual([]);
    expect(parseChunks(['data: x\n: keep-alive\n\n'])).toEqual([{ event: 'message', data: 'x' }]);
  });

  it('joins multi-line data with LF', () => {
    expect(parseChunks(['data: a\ndata: b\n\n'])).toEqual([{ event: 'message', data: 'a\nb' }]);
  });

  it('accepts a data field without a space after the colon', () => {
    expect(parseChunks(['data:x\n\n'])).toEqual([{ event: 'message', data: 'x' }]);
  });

  it('dispatches an empty data field as an empty string', () => {
    expect(parseChunks(['data:\n\n'])).toEqual([{ event: 'message', data: '' }]);
  });

  it('treats a line without a colon as a field with an empty value', () => {
    expect(parseChunks(['data\n\n'])).toEqual([{ event: 'message', data: '' }]);
  });

  it('reassembles a line split across chunks', () => {
    expect(parseChunks(['event: del', 'ta\ndata: {"text":"Dzień"}\n\n'])).toEqual([
      { event: 'delta', data: '{"text":"Dzień"}' },
    ]);
  });

  it('defaults the event name to message and resets it after each dispatch', () => {
    expect(parseChunks(['event: a\ndata: 1\n\ndata: 2\n\n'])).toEqual([
      { event: 'a', data: '1' },
      { event: 'message', data: '2' },
    ]);
  });

  it('ignores unknown fields such as id and retry', () => {
    expect(parseChunks(['id: 5\nretry: 1000\ndata: x\n\n'])).toEqual([{ event: 'message', data: 'x' }]);
  });

  it('parses the recorded decision update the same whole and one character at a time', async () => {
    const recording = await loadSseRecording('chat-decision-update');
    const expected = parseChunks([recording.text]);

    expect(expected).toHaveLength(35);
    expect(expected[0]).toEqual({ event: 'decision', data: '{"category":"ESCALATED"}' });
    expect(expected.at(-1)).toEqual({ event: 'done', data: '{"category":"ESCALATED"}' });
    expect(parseChunks(Array.from(recording.text))).toEqual(expected);
  });

  it('parses the recorded decision update identically when UTF-8 bytes are split at every offset', async () => {
    const recording = await loadSseRecording('chat-decision-update');
    const expected = parseChunks([recording.text]);
    const bytes = new TextEncoder().encode(recording.text);
    for (let cut = 1; cut < bytes.length; cut++) {
      const decoder = new TextDecoder('utf-8');
      const parser = new SseParser();
      const events = [
        ...parser.push(decoder.decode(bytes.subarray(0, cut), { stream: true })),
        ...parser.push(decoder.decode(bytes.subarray(cut), { stream: true })),
        ...parser.push(decoder.decode()),
        ...parser.flush(),
      ];
      expect(events, `split at byte ${cut}`).toEqual(expected);
    }
  });

  it('does not emit an unterminated last event on flush', () => {
    const parser = new SseParser();
    expect(parser.push('data: x\n')).toEqual([]);
    expect(parser.flush()).toEqual([]);
  });

  it('emits only the terminated events before an unterminated tail', () => {
    const parser = new SseParser();
    expect(parser.push('data: x\n\ndata: y\n')).toEqual([{ event: 'message', data: 'x' }]);
    expect(parser.flush()).toEqual([]);
  });
});
