# Format handling

Which file type comes back for a photo, and when the bytes are passed through
rather than re-encoded.

With the default `format: 'original'` the source format is preserved:

- **No resize needed** (no `maxWidth`/`maxHeight`, or the image is already within
  bounds): the original bytes are returned unchanged. `quality` only applies when
  a resize forces a re-encode.
- **Resize needed:** the image is re-encoded in its source format — PNG stays PNG
  (transparency preserved), JPEG stays JPEG. **Exceptions:** HEIC stays HEIC on
  iOS but becomes `image/jpeg` on Android (no system HEIC encoder), and also falls
  back to `image/jpeg` on iOS Simulator / older devices without a HEIC encoder;
  WebP becomes `image/jpeg` on iOS (no system WebP encoder).
- **Animated images** (GIF, animated WebP): always returned untouched;
  `maxWidth`/`maxHeight`/`quality` are ignored so the animation survives.
- **Camera captures** are `image/jpeg` unless `format: 'png'` is requested. On iOS
  the capture arrives already decoded, so it is always re-encoded; on Android the
  capture file is passed through untouched when no resize is needed. With
  `format: 'png'` on Android the capture file is re-encoded even when no resize is
  needed.
- **Explicit `format: 'jpeg'` / `'png'`:** guarantees the output type. A source
  already in the requested format (and not animated) is passed through
  untouched — `quality` is not applied to it. Anything else is re-encoded even
  when no resize is needed. Animated images (GIF, animated WebP) are converted
  from their **first frame** — the animation is lost, and since it is lost
  anyway, `maxWidth`/`maxHeight` apply to that frame. `quality` still applies
  only when a re-encode happens, and is ignored for lossless PNG output.
  Converting a source with transparency (PNG/HEIC alpha) to `'jpeg'` composites
  it onto a black background — JPEG has no alpha channel; use `'png'` when
  transparency must survive. File types the library does not recognize (for
  example AVIF) are currently treated as JPEG, so `format: 'jpeg'` returns them
  unchanged rather than transcoding them; the guarantee covers the formats
  listed in the [`Asset`](../README.md#asset) section.

On iOS the system is willing to transcode a photo while exporting it from the
library, so a HEIC can arrive already turned into JPEG. With `format: 'original'`
the library asks for the stored representation instead, which is both faster and
what "original" promises — expect `image/heic` for HEIC photos where an earlier
version sometimes handed you `image/jpeg`. Ask for `format: 'jpeg'` when your
backend needs JPEG; the export is then left to the system, which is free to pick
whichever representation it can produce fastest.
