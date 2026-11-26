import { describe, expect, it } from 'vitest';
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

// TODO(T26): replace this hand-written spec-grammar fixture with a recorded backend SSE stream.
const FIXTURE =
  ': heartbeat\n\n' +
  'event: decision\ndata: {"category":"ESCALATED"}\n\n' +
  'event: delta\ndata: {"text":"Zaktualizowana decyzja: Przekazane do specjalisty\\n"}\n\n' +
  'event: delta\ndata: {"text":"Zażółć gęślą jaźń 😀"}\n\n' +
  'event: done\ndata: {"category":"ESCALATED"}\n\n';

const FIXTURE_EVENTS: SseEvent[] = [
  { event: 'decision', data: '{"category":"ESCALATED"}' },
  { event: 'delta', data: '{"text":"Zaktualizowana decyzja: Przekazane do specjalisty\\n"}' },
  { event: 'delta', data: '{"text":"Zażółć gęślą jaźń 😀"}' },
  { event: 'done', data: '{"category":"ESCALATED"}' },
];

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

  it('parses the fixture the same whole and one character at a time', () => {
    expect(parseChunks([FIXTURE])).toEqual(FIXTURE_EVENTS);
    expect(parseChunks(Array.from(FIXTURE))).toEqual(FIXTURE_EVENTS);
  });

  it('parses the fixture identically when UTF-8 bytes are split at every offset', () => {
    const bytes = new TextEncoder().encode(FIXTURE);
    for (let cut = 1; cut < bytes.length; cut++) {
      const decoder = new TextDecoder('utf-8');
      const parser = new SseParser();
      const events = [
        ...parser.push(decoder.decode(bytes.subarray(0, cut), { stream: true })),
        ...parser.push(decoder.decode(bytes.subarray(cut), { stream: true })),
        ...parser.push(decoder.decode()),
        ...parser.flush(),
      ];
      expect(events, `split at byte ${cut}`).toEqual(FIXTURE_EVENTS);
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
