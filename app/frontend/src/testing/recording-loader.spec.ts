import { describe, expect, it } from 'vitest';
import { loadJsonRecording, loadRawBody, loadSseRecording } from './recording-loader';

describe('recording-loader', () => {
  it('loads a JSON recording with status, headers and body', async () => {
    const rec = await loadJsonRecording<{ signature: string }>('assessment-return-ok');
    expect(rec.status).toBe(200);
    expect(rec.headers['content-type']).toContain('application/json');
    expect(typeof rec.body.signature).toBe('string');
  });

  it('loads the exact raw body matching the JSON body', async () => {
    const rec = await loadJsonRecording('problem-429');
    expect(JSON.parse(await loadRawBody('problem-429'))).toEqual(rec.body);
  });

  it('loads an SSE recording with headers', async () => {
    const rec = await loadSseRecording('chat-plain');
    expect(rec.headers['content-type']).toBe('text/event-stream');
    expect(rec.text).toContain('event:done');
  });
});
