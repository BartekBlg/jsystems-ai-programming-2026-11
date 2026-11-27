/**
 * Incremental Server-Sent Events parser following the WHATWG grammar (ADR-003 §3.5, TAC-003-03).
 * Operates on already-decoded text; the caller is responsible for UTF-8 decoding.
 */

export interface SseEvent {
  event: string;
  data: string;
}

export class SseParser {
  private line = '';
  private skipLf = false;
  private eventName = '';
  private dataBuffer = '';

  /** Feed decoded text; returns the events completed by this chunk. */
  push(chunk: string): SseEvent[] {
    const events: SseEvent[] = [];
    for (const ch of chunk) {
      if (this.skipLf) {
        this.skipLf = false;
        if (ch === '\n') {
          continue;
        }
      }
      if (ch === '\r') {
        this.endLine(events);
        this.skipLf = true;
      } else if (ch === '\n') {
        this.endLine(events);
      } else {
        this.line += ch;
      }
    }
    return events;
  }

  /**
   * Call at end of stream. Per the spec an event that was not terminated by a blank line is discarded,
   * and every blank-line-terminated event has already been returned by push().
   */
  flush(): SseEvent[] {
    this.line = '';
    this.skipLf = false;
    this.eventName = '';
    this.dataBuffer = '';
    return [];
  }

  private endLine(events: SseEvent[]): void {
    const line = this.line;
    this.line = '';

    if (line === '') {
      const event = this.dispatch();
      if (event) {
        events.push(event);
      }
      return;
    }
    if (line.startsWith(':')) {
      return;
    }

    const colon = line.indexOf(':');
    const field = colon === -1 ? line : line.slice(0, colon);
    let value = colon === -1 ? '' : line.slice(colon + 1);
    if (value.startsWith(' ')) {
      value = value.slice(1);
    }

    if (field === 'event') {
      this.eventName = value;
    } else if (field === 'data') {
      this.dataBuffer += value + '\n';
    }
  }

  private dispatch(): SseEvent | null {
    const data = this.dataBuffer;
    const event = this.eventName || 'message';
    this.eventName = '';
    this.dataBuffer = '';
    if (data === '') {
      return null;
    }
    return { event, data: data.slice(0, -1) };
  }
}
