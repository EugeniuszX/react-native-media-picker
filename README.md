# @eugeniuszx/react-native-media-picker

[![npm version](https://img.shields.io/npm/v/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![npm downloads](https://img.shields.io/npm/dm/@eugeniuszx/react-native-media-picker.svg)](https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker)
[![license](https://img.shields.io/npm/l/@eugeniuszx/react-native-media-picker.svg)](./LICENSE)

Cross-platform media picker for React Native (New Architecture). Picks photos and
videos from the gallery, and captures photos or records video with the camera.
Gallery access requires **no runtime permissions** on iOS (PHPicker) or Android
(Photo Picker).

📦 **npm:** https://www.npmjs.com/package/@eugeniuszx/react-native-media-picker

> Requires React Native 0.76+ with the New Architecture enabled.

## Install

```sh
npm install @eugeniuszx/react-native-media-picker
cd ios && pod install
```

## Usage

```ts
import { launchImageLibrary } from '@eugeniuszx/react-native-media-picker';

const result = await launchImageLibrary({
  selectionLimit: 1, // 0 = unlimited
  mediaType: 'photo', // 'photo' | 'video' | 'mixed'
  maxWidth: 640,
  maxHeight: 640,
  quality: 0.8, // 0..1
  includeBase64: false,
});

if (!result.didCancel && !result.errorCode) {
  // `assets` is `Asset[]` here, not `Asset[] | undefined`.
  console.log(result.assets[0]?.uri);
}
```

## `LibraryOptions`

| Option | Type | Default | Notes |
|---|---|---|---|
| `selectionLimit` | `number` | `1` | `0` = unlimited (see note below) |
| `mediaType` | `'photo' \| 'video' \| 'mixed'` | `'photo'` | What the picker offers; videos are returned as-is (see [Video assets](#video-assets)) |
| `maxWidth` | `number` | `0` | `0` = no resize; ignored for video assets |
| `maxHeight` | `number` | `0` | `0` = no resize; ignored for video assets |
| `quality` | `number` | `1` | Re-encode quality 0..1 (JPEG/WebP/HEIC); ignored for lossless PNG, for video assets, and for animated images (unless an explicit `format` forces a first-frame re-encode) |
| `format` | `'original' \| 'jpeg' \| 'png'` | `'original'` | Guarantee the output file type; `'original'` preserves the source format (see below); ignored for video assets |
| `includeBase64` | `boolean` | `false` | adds `base64` to each asset; ignored for video assets |
| `includeThumbnail` | `boolean` | `false` | adds `thumbnailUri` to each **video** asset (see [Video thumbnails](#video-thumbnails)); ignored for photos |
| `includeExif` | `boolean` | `false` | adds `exif` to each **photo** asset, read from the source (see [Metadata](#metadata)); ignored for video assets |
| `stripMetadata` | `boolean` | `false` | removes EXIF/GPS from each **photo** written (see [Metadata](#metadata)); ignored for video assets |

Videos can be picked with `mediaType: 'video'` (or alongside photos with
`'mixed'`) — see [Video assets](#video-assets).

On Android, "unlimited" is capped by the system Photo Picker:
`selectionLimit: 0` requests `MediaStore.getPickImagesMaxLimit()`, and any larger
explicit limit is clamped to it. iOS has no such cap.

On iOS, `assets` follows the order the user tapped the items in. On Android the
order is the one the system picker reports.

## `Asset`

Each picked item resolves to: `uri` (a `file://` path to a temp file), `type`
(the output mime, matching the file actually written — `image/jpeg`,
`image/png`, `image/heic`, `image/gif`, or `image/webp` for photos, and
`video/mp4`, `video/quicktime`, `video/webm` or `video/3gpp` for videos; see
[Format handling](#format-handling) and [Video assets](#video-assets) for how
it is derived), `fileName` (extension matches `type`), `fileSize`, `width`,
`height`, `duration` (seconds, video assets only — absent for photos),
`base64` (only when `includeBase64` is true, photos only),
`thumbnailUri`/`thumbnailWidth`/`thumbnailHeight` (only when
`includeThumbnail` is true, video assets only — see
[Video thumbnails](#video-thumbnails)), and `exif` (only when `includeExif` is
true, photos only — see [Metadata](#metadata)).

`uri` and `type` are the only fields declared non-optional. `fileName`,
`fileSize`, `width` and `height` are typed `?:`, so TypeScript makes you narrow
them. Both native modules do populate all four today, but `width` and `height`
are reported as `0` when the image or video metadata cannot be read — treat `0`
as "unknown", not as a real dimension.

`fileName` is the name the item carries in the gallery, carrying the extension of
the file actually written — `IMG_4821.HEIC` picked with `format: 'jpeg'` comes
back as `IMG_4821.jpg`. It is a label for uploads and UI, never a path: the bytes
always live at `uri`, under a generated name. It arrives sanitized (path
separators, control characters and leading dots are dropped, and the name is
capped at 100 characters plus the extension), and when nothing usable survives —
or the system reports no name at all, which is always the case for **camera
captures** — it falls back to the temp file's own `media_picker_<uuid>.<ext>`.
Two picked items can carry the same `fileName`, so use `uri` as the identity.

`width` and `height` are the dimensions **as displayed** — the EXIF orientation
is already applied. For an image whose orientation is a quarter turn they are
therefore swapped relative to the stored pixel buffer when the original bytes are
passed through untouched; when a resize forces a re-encode the rotation is baked
into the output, so buffer and reported size agree. For videos the rotation
metadata of the video track is applied the same way — see
[Video assets](#video-assets).

### Format handling

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
  listed in the [`Asset`](#asset) section.

On iOS the system is willing to transcode a photo while exporting it from the
library, so a HEIC can arrive already turned into JPEG. With `format: 'original'`
the library asks for the stored representation instead, which is both faster and
what "original" promises — expect `image/heic` for HEIC photos where an earlier
version sometimes handed you `image/jpeg`. Ask for `format: 'jpeg'` when your
backend needs JPEG; the export is then left to the system, which is free to pick
whichever representation it can produce fastest.

### Video assets

Videos are always passed through untouched: the file the system hands over is
copied byte-for-byte into a temp file and returned as-is. `maxWidth`,
`maxHeight`, `quality`, `format` and `includeBase64` are ignored for video
assets — the library never transcodes, and base64 for videos is deliberately
unsupported. On iOS the copied bytes are the representation PHPicker exports
from the photo library, and the system may re-encode the asset while producing
it, so they are not guaranteed to be identical to the original file in the
library.

Each video asset carries `duration` (seconds), `width`/`height` (displayed
axes — rotation metadata is applied; `0` means the metadata could not be
read) and `fileSize`. A failure to read metadata does not fail the asset:
`duration` is then simply absent, and `width`/`height` come back as `0`.

`type` is derived from the mime the system reports for the picked item:
`video/mp4`, `video/quicktime`, `video/webm` or `video/3gpp`, and `fileName`'s
extension matches it (`mp4`, `mov`, `webm`, `3gp`). That is also why a mime the
library does not recognize — or a missing one — is labeled `video/mp4`; the
bytes are still passed through untouched, exactly as with unrecognized image
types under [Format handling](#format-handling). iOS only ever reports
`video/mp4` or `video/quicktime`; `video/webm` and `video/3gpp` come from
Android.

Platform notes:

- **iOS:** videos stored in iCloud are downloaded by the system during the
  pick; large videos can take a while and there is no progress reporting. The
  pick stays in flight for the whole download — and, since only one pick may run
  at a time, new `launchImageLibrary`/`launchCamera` calls are rejected until it
  finishes. Slo-mo videos arrive as regular movies. A Live Photo is returned as
  its still image (also under `mediaType: 'mixed'`) — the motion part is not
  extracted.
- **Android:** on devices without the system Photo Picker the fallback
  `ACTION_GET_CONTENT` chooser is used, same as for photos. The provider's mime
  type is trusted when it reports one; when it reports none or a generic one,
  the library falls back to sniffing the file header (ISO-BMFF / Matroska) to
  tell a video from an image. With `mediaType: 'mixed'` that chooser is opened
  with type `*/*` (images and videos are only a hint, passed as
  `EXTRA_MIME_TYPES`), so it may also offer non-media files — a non-media pick
  fails to process and is dropped like any other failed item.

Temp-file handling is identical to photos: the same `rn-media-picker`
directory, the same 24-hour sweep, and `cleanTempFiles()` deletes video temp
files too.

### Video thumbnails

`includeThumbnail: true` writes a poster frame for every **video** asset and
adds `thumbnailUri`, `thumbnailWidth` and `thumbnailHeight` to it:

```ts
const result = await launchImageLibrary({
  mediaType: 'video',
  includeThumbnail: true,
});

// <Image source={{ uri: result.assets?.[0]?.thumbnailUri }} />
```

The thumbnail is a **JPEG** taken from the first renderable frame, fitted
inside 512×512 (never scaled up), with the video's rotation metadata already
applied. Its size is not configurable — `maxWidth`/`maxHeight`/`quality`/
`format` describe the asset, not its preview. Photo assets never get one, and
neither do camera photo captures — but a video recorded with
[`launchCamera`](#camera) does, since it is a video asset like any other.

The thumbnail is a temp file in the same `rn-media-picker` directory, so it is
covered by the 24-hour sweep and by `cleanTempFiles()`. Passing the asset to
[`releaseAssets`](#releasing-individual-assets) releases the thumbnail with it.

Generating a thumbnail never fails an asset: if the frame cannot be decoded,
the asset comes back without the three `thumbnail*` fields. Always narrow
`thumbnailUri` before using it.

## Metadata

Two independent options, both `false` by default and both photo-only:
`includeExif` reads metadata **out of the source** and hands it to you,
`stripMetadata` keeps metadata **out of the file** that is written. They do not
interact — asking for both is the ordinary way to keep the location of a photo
for yourself without shipping it inside the bytes you upload.

### `includeExif`

`includeExif: true` adds an `exif` object to every photo asset. Video assets
never get one and the option is ignored for them.

| Field | Type | Meaning |
|---|---|---|
| `dateTimeOriginal` | `string` | When the shot was taken, ISO 8601 **without an offset** (`2026-08-14T15:29:03`). EXIF carries no timezone, so none is invented; a malformed or all-zero timestamp is dropped rather than half-parsed |
| `latitude` | `number` | Signed decimal degrees — negative south of the equator |
| `longitude` | `number` | Signed decimal degrees — negative west of Greenwich |
| `altitude` | `number` | Metres, negative below sea level |
| `make` | `string` | Camera manufacturer |
| `model` | `string` | Camera model |
| `orientation` | `number` | The EXIF orientation value, `1`–`8` |
| `iso` | `number` | Sensitivity, as in `400` |
| `fNumber` | `number` | Aperture, as in `1.8` |
| `exposureTime` | `number` | Seconds — `0.008` for 1/125 |
| `focalLength` | `number` | Millimetres |

**Every one of the eleven is optional**, and so is `exif` itself: a source with
none of them — a screenshot, a re-saved PNG — arrives without the object at all
rather than with an empty one. Narrow both levels.

```ts
const result = await launchImageLibrary({ selectionLimit: 1, includeExif: true });
const takenAt = result.assets?.[0]?.exif?.dateTimeOriginal;
```

`exif` is read from the **source**, not from the file that is written. So it
still arrives when `stripMetadata` removed those values from the output, and
when a resize dropped them — the read happens before either.

`exif.orientation` describes the source too. After a resize the rotation is
baked into the pixels and the written file is upright, while the field still
reports the value the original carried. `width`/`height` are the authority on
the file you were handed; read `orientation` as a fact about where the photo
came from, not as an instruction for rendering it.

For a **camera capture on iOS** the values come from the metadata dictionary the
system picker hands over rather than from a file: the capture arrives already
decoded and is always re-encoded, so the file that is written carries no EXIF of
its own.

**On Android a capture is the other way round**, and the difference points the
opposite way for privacy, so it is worth being explicit. A capture there goes
through the ordinary photo path: `exif` is read from the file the camera app
wrote, and — with no resize asked for and `format: 'original'` — that file is
copied through **verbatim**, so whatever EXIF and GPS the camera app recorded is
still inside the asset you get back. `stripMetadata: true` is what removes it,
and for Android captures it does real work; that is the converse of the "no-op
on iOS" note in the [`CameraOptions`](#cameraoptions) table.

### `stripMetadata`

`stripMetadata: true` removes the EXIF and the GPS from the photo written to
`uri`. There are two ways to get there, and which one an asset takes is worth
knowing, because only one of them is free:

- **Rewriting the container in place.** The already-compressed pixel data is
  copied across untouched, so nothing is decoded and no quality is lost — on iOS
  this was measured: the scrubbed JPEG's quantisation tables are byte-identical
  to the source's and its entropy-coded scan is byte-identical too, so **not one
  pixel changes**. The file gets slightly smaller, by roughly the size of the
  metadata that was removed. Auxiliary images stored beside the main one — an
  HDR gain map, a depth map — are carried across intact.
- **Re-encoding.** Used where the container cannot be rewritten. The image is
  decoded and written afresh, so `fileSize` changes, `type` and `fileName` can
  change with it, and the pixels are no longer the source's.

The photo's **rendered orientation** survives either route, though by different
means. The in-place rewrite keeps the EXIF orientation tag — dropping it would
render a quarter-turned photo sideways, and the reported `width`/`height` already
assume it is there. The re-encode bakes the rotation into the pixels instead and
writes a file with no EXIF at all, so there is no orientation tag left to read
back; the image is simply upright. Either way `width`/`height` describe what you
were handed.

| Source | iOS | Android |
|---|---|---|
| JPEG | rewritten in place | rewritten in place |
| PNG | **re-encoded**, stays `image/png` | rewritten in place |
| HEIC | **re-encoded**, stays `image/heic` | **re-encoded to `image/jpeg`** |
| WebP, static | **re-encoded to `image/jpeg`** (no WebP encoder on iOS) | rewritten in place |
| WebP, animated | **passthrough — not honoured** | rewritten in place, frames intact |
| GIF (animated or not) | **no-op** | **no-op** |

Beyond the table, the cases that surprise people:

**GIF is a no-op, animated or not.** A GIF has no EXIF or GPS container, so
there is nothing to remove. Re-encoding one anyway would flatten a static GIF
into a JPEG and composite its transparency onto black, to strip metadata it
never carried. Both platforms leave GIFs exactly as they arrived.

**An animated WebP on iOS is a genuine passthrough.** iOS ships no WebP encoder
at all, so the container cannot be rewritten, and a re-encode would leave one
frame of an animation. The frames win: on iOS this is the input where
`stripMetadata: true` is not applied at all, so the file comes back as it
arrived and may still carry everything it did. A **static**
WebP takes the re-encode instead and does honour the option, coming back as
`image/jpeg`. Android rewrites both in place and honours the option for each.

**A HEIC changes container on Android.** `ExifInterface` can read HEIC but not
write it, so a strip means a re-encode to JPEG: `type` becomes `image/jpeg`,
`fileName`'s extension follows, and `fileSize` moves. On iOS the same photo is
re-encoded too but stays `image/heic`, so `type` and `fileName` do not move —
`fileSize` and the pixels do. This mirrors what a resize already does to a HEIC
on Android — see [Format handling](#format-handling).

**On iOS only a JPEG is rewritten in place; a PNG and a HEIC are re-encoded.**
This is narrower than it looks like it should be, and it is a measured result
rather than a policy. The one ImageIO call that copies the compressed data
unmodified strips a JPEG completely, but on a PNG it leaves every `tEXt` credit
where it is and *adds* an XMP packet rebuilt out of them, and on a HEIC it keeps
Artist, Copyright, DateTime, Software and the XMP. Neither is an acceptable
answer to `stripMetadata: true`, so both fall through to the re-encode, which
comes back clean. A PNG re-encode is not a quality loss in the JPEG sense — PNG
is lossless — but it does pass the image through an 8-bit sRGB context, so a
16-bit or wide-gamut PNG is converted. A HEIC re-encode is lossy and respects
the `quality` you set.

**An APNG asked to strip is flattened to its first frame on iOS.** iOS treats an
APNG as a still PNG — the animation check covers GIF and animated WebP only — so
the `PNG` row above applies to it, and that row is now a re-encode, which encodes
the single frame it decoded. The animation is lost, and nothing in the result
says so. Without `stripMetadata` an APNG is returned untouched and keeps every
frame; asking for a resize flattens it too, for the same reason. This is a known
limitation rather than a decision — the fix belongs in the shared format
detection and is not in this release. Android's rewrite works the other way
round: it replaces the EXIF segment and copies the rest of the container across,
so an APNG keeps its frames there.

**A source carrying an XMP packet is re-encoded rather than rewritten.** XMP is
a second, parallel copy of the metadata — it can hold `exif:GPSLatitude` and
`tiff:Model` of its own — and neither platform's in-place writer removes it
reliably: on iOS a source that already carries a packet was measured to keep it,
and Android's `saveAttributes` replaces only the EXIF segment and copies every
other one verbatim. Rather than rest the guarantee on that, both platforms
decline the rewrite and re-encode. Photos that went through Lightroom, Google
Photos or a similar pipeline commonly carry a packet. The consequence is worth stating plainly: **a JPEG
asked for with `format: 'original'` and no resize comes back materially larger**
than the untouched passthrough the caller expected. At the default `quality: 1`
nothing is thrown away, which is exactly why the file grows. A `quality` you set
yourself **does** apply to this re-encode, so `quality: 0.6` on a source you
expected to be passed through untouched genuinely re-compresses it.

**Two more channels take the re-encode on Android**, because `saveAttributes`
copies them across untouched: an IPTC block (JPEG `APP13`, written by Photoshop
and the newsroom tooling that follows it — creator, city, contact, copyright)
and PNG text chunks (`tEXt`/`zTXt`/`iTXt` — Author, Comment, Software). A source
carrying either is re-encoded, same as an XMP one, and comes back a different
size — usually larger, but a PNG re-encoded because of a `tEXt` chunk goes back
out as PNG at Android's own deflate settings and can land either side of the
source. On iOS a JPEG carrying an IPTC block is still scrubbed losslessly — the
block is removed by the in-place rewrite along with everything else. A PNG with
text chunks is re-encoded on iOS too, for the reason given above.

The one place that leaves an asset with nowhere to go is an **animated WebP on
Android that also carries residue**: it cannot be scrubbed cleanly and it cannot
be re-encoded without losing its frames, so the frames win and the file comes
back as it arrived — the same unhonoured outcome iOS has for every animated
WebP.

**JPEG `COM` comment segments survive a scrub on Android.** A `COM` segment has
no identifier to detect and no defined semantics; in practice it holds encoder
signatures (`"Created with GIMP"` and the like) rather than anything about the
photographer. On iOS a `COM` is **removed** — measured on a JPEG with one
injected after the `SOI`, which came back without it and byte-for-byte the same
size as the same JPEG scrubbed without one. That is the writer's doing rather
than a check the library performs, so it is a measurement, not a guarantee.

**And a scrub can leave IFD1 behind on Android.** `ExifInterface` skips the
thumbnail IFD when the file reports no thumbnail, while its writer emits every
non-empty IFD, so a JPEG whose IFD1 holds tags but whose thumbnail pointer is
absent or zero-length keeps those tags. Narrow, and never GPS — but it is why
this section says the EXIF and GPS *are removed* rather than promising a file
that provably carries nothing at all.

**A scrub that fails falls through to a re-encode** on both platforms, rather
than returning a half-stripped file: a strip that cannot be done losslessly is
done destructively instead of being abandoned. The animated WebP above is the
one input skipped entirely.

### Without `stripMetadata`

The default is unchanged from earlier versions and is worth stating next to the
above, because it is the case most photos hit: a photo returned **untouched**
keeps every byte of EXIF and GPS it arrived with, and a photo that was
**resized** loses all of it, since a re-encode writes fresh bytes and this
version does not copy metadata across one. So the metadata a caller ends up
shipping today depends on whether a resize happened. `stripMetadata: true` is
the way to stop it depending on that.

## Camera

```ts
import { launchCamera } from '@eugeniuszx/react-native-media-picker';

const result = await launchCamera({
  cameraType: 'back', // 'back' | 'front'
  maxWidth: 1280,
  maxHeight: 1280,
  quality: 0.8,
  includeBase64: false,
});
```

### `CameraOptions`

| Option | Type | Default | Notes |
|---|---|---|---|
| `cameraType` | `'back' \| 'front'` | `'back'` | Honored on iOS; **best-effort on Android** (the system camera app may ignore it) |
| `mediaType` | `'photo' \| 'video'` | `'photo'` | `'video'` records a movie instead of capturing a still (see [Recording video](#recording-video)) |
| `maxWidth` | `number` | `0` | `0` = no resize |
| `maxHeight` | `number` | `0` | `0` = no resize |
| `quality` | `number` | `1` | JPEG quality 0..1. Applies whenever the capture is re-encoded — always on iOS; on Android only when a resize is needed; ignored when `format: 'png'` (PNG is lossless) |
| `format` | `'original' \| 'jpeg' \| 'png'` | `'original'` | Output type of the capture; `'original'` = JPEG, as before |
| `includeBase64` | `boolean` | `false` | adds `base64` to the captured asset |
| `includeExif` | `boolean` | `false` | photos only (see [Metadata](#metadata)) |
| `stripMetadata` | `boolean` | `false` | photos only; **a no-op on iOS**, where a capture is always re-encoded and therefore already carries no metadata |
| `maxDuration` | `number` | `0` | Recording limit in seconds. `0` leaves the **platform's own** limit, which is 10 minutes on iOS and whatever the camera app defaults to on Android — not "unlimited". **Best-effort on Android** |
| `videoQuality` | `'low' \| 'medium' \| 'high'` | `'high'` | Honored on iOS; **best-effort on Android** (`EXTRA_VIDEO_QUALITY` carries only low/high, so `'medium'` is sent as high, and most camera apps ignore it anyway) |
| `includeThumbnail` | `boolean` | `false` | adds `thumbnailUri` to a recorded **video** (see [Video thumbnails](#video-thumbnails)); ignored for photos |

### Recording video

`mediaType: 'video'` opens the same system camera in movie mode and returns a
recorded movie instead of a still:

```ts
const result = await launchCamera({
  mediaType: 'video',
  maxDuration: 30,
  videoQuality: 'medium',
  includeThumbnail: true,
});
```

The result is the same shape as a video picked from the library — `duration`,
displayed `width`/`height`, `fileSize`, and `thumbnailUri` when you ask for it —
so [Video assets](#video-assets) describes it in full. This library never
transcodes the recording — it copies out what the camera wrote, which is where
`videoQuality` had its say — and `fileName` is the generated
`media_picker_<uuid>.<ext>`, since a fresh recording has no name in the gallery.
`type` is `video/quicktime` on iOS
and `video/mp4` on Android — though a camera app that ignores the output file
and hands back a content uri of its own can report a different video mime, which
is passed through exactly as a picked one would be.

The photo options do not apply to a recording and are ignored for it:
`maxWidth`, `maxHeight`, `quality`, `format`, `includeBase64`, `includeExif` and
`stripMetadata`. `videoQuality` and `maxDuration` take their place, with the
caveats in the table above — `cameraType` carries the same best-effort caveat on
Android it always has.

`maxDuration: 0` does not mean "record until the user stops". It means the
library sets no limit of its own and leaves the platform's: a hard 10 minutes on
iOS, and on Android whatever the installed camera app decides.

### Camera permissions

- **iOS:** add `NSCameraUsageDescription` to your app's `Info.plist`. iOS shows the permission prompt automatically; the app crashes at launch of the camera if the key is missing. If the user denies access, `launchCamera` resolves `{ didCancel: false, errorCode: 'permission' }`.
- **Android:** if your app declares `android.permission.CAMERA` in its manifest, this library requests it at runtime before opening the camera (a denial resolves `{ didCancel: false, errorCode: 'permission' }`). If your app does **not** declare `CAMERA`, the system camera app is launched without any runtime permission.

> **Recording video on iOS also needs `NSMicrophoneUsageDescription`.** The
> system camera controller starts audio capture as soon as it opens in movie
> mode, and iOS terminates the process when the key is missing — exactly as it
> does without `NSCameraUsageDescription`, and before any code of yours runs.
> Add both keys if you use `mediaType: 'video'`, or let the
> [config plugin](#expo) do it.

Android needs no equivalent. `ACTION_VIDEO_CAPTURE` hands the recording to a
separate camera app, which holds its own microphone permission; this library
never records audio in your process and never asks for `RECORD_AUDIO`.

`launchCamera` handles all of this on its own. Reach for the two calls below when
you want to explain yourself before the system prompt appears, or to gate a
camera button on the current status:

```ts
import {
  getCameraPermissionStatus,
  requestCameraPermission,
} from '@eugeniuszx/react-native-media-picker';

if ((await getCameraPermissionStatus()) === 'blocked') {
  // Only Settings can undo this — send the user there instead of asking again.
} else {
  const status = await requestCameraPermission();
}
```

`getCameraPermissionStatus` never prompts. `requestCameraPermission` shows the
system prompt only when it can still be answered (`'not_determined'` or
`'denied'`) and resolves the status the user leaves it in; every other status is
returned as-is, without a dialog. Neither call rejects, and neither is needed to
use `launchCamera`.

`CameraPermissionStatus` is exported from the package:

| Status | Meaning |
|---|---|
| `'granted'` | Capture is allowed |
| `'not_determined'` | Never asked — asking will show the prompt |
| `'denied'` | Refused, but asking again still shows the prompt. **Android only** |
| `'blocked'` | Refused for good (or restricted by policy); only Settings can change it. iOS reports every refusal here, since it allows one ask per install |
| `'not_required'` | The app does not declare `android.permission.CAMERA`, so no runtime permission is involved. **Android only** |
| `'unavailable'` | The device has no camera |

Two details worth knowing on Android: distinguishing `'denied'` from `'blocked'`
relies on this library remembering that it has asked, so a status read after the
app's data is cleared reports `'not_determined'` again; and a status read while no
activity is in the foreground reports `'blocked'` rather than `'denied'` for a
refused permission, because the rationale flag cannot be read from the background.
`'unavailable'` covers missing camera hardware — a device that has a camera but no
app able to handle the capture intent still reports its real permission status
here and surfaces as `errorCode: 'camera_unavailable'` from `launchCamera`.

## Response & error handling

`launchImageLibrary` and `launchCamera` **never reject** — they always resolve a `PickerResponse`:

- **Success:** `{ didCancel: false, assets: Asset[] }`
- **Cancelled:** `{ didCancel: true }`
- **Error:** `{ didCancel: false, errorCode, errorMessage }`. `errorCode` is the
  union
  `ErrorCode = 'permission' | 'camera_unavailable' | 'busy' | 'others'`,
  exported from the package:
  - `'permission'` — camera permission denied or restricted
  - `'camera_unavailable'` — the device has no camera; or (iOS) it has one that
    cannot record movies and `mediaType: 'video'` was asked for, which is what
    the simulator reports; or (Android) no installed app can handle the capture
    intent
  - `'busy'` — **another pick from this library is already in flight**, and
    nothing else
  - `'others'` — everything else (no activity or view controller to present from,
    load/decode/encode failure)

`PickerResponse` is a discriminated union, so those three cases narrow: check
`didCancel`, then `errorCode`, and `assets` is `Asset[]` on what is left.

```ts
const result = await launchImageLibrary();

if (result.didCancel) return;
if (result.errorCode) {
  console.warn(result.errorCode, result.errorMessage);
  return;
}
console.log(result.assets.length); // Asset[], no `?.` needed
```

Reading `result.assets` or `result.errorMessage` on an un-narrowed value still
compiles exactly as it did before the union.

When several items are picked and only some of them fail to load, the successful
ones are returned; in that case `errorCode` is set only if **every** item failed.

Only one pick may be in flight at a time. A `launchImageLibrary` or
`launchCamera` call made while another is still running resolves immediately with
`errorCode: 'busy'` and `errorMessage: 'Already waiting for a pick.'` — the
running pick is left untouched. A double-tapped button is the usual way to see
it, and it is the code to match on when you want to ignore that quietly rather
than surface an error.

`'busy'` is narrow on purpose: it means *this library's own pick is already
running*, not "the picker is unavailable right now". Presenting on top of a
modal your app put up fails a separate check and still resolves `'others'`
(`'A view controller is already being presented'` on iOS), because that is your
UI in the way, not our pick.

## Temp files

Every picked or captured asset is written to a temp file inside a dedicated
`rn-media-picker` directory (in `NSTemporaryDirectory()` on iOS, in the app's
`cacheDir` on Android). `Asset.uri` points at that file.

The library sweeps files older than 24 hours when the native module initializes,
so leftovers from previous runs do not accumulate. Copy or upload what you need,
then release the rest explicitly:

```ts
import { cleanTempFiles } from '@eugeniuszx/react-native-media-picker';

await cleanTempFiles();
```

`cleanTempFiles` deletes every temp file this library has produced, so it
invalidates every `uri` handed out earlier — call it once you are done with them.
It never rejects, and resolves once the sweep has finished with the number of
files it deleted.

**Do not call it while a pick is in flight.** The sweep empties the whole
directory, including files the running pick is still using. On Android it
deletes the file the camera app is writing into, and the capture then resolves
`{ didCancel: true }` as though the user had backed out; on either platform it
can delete a just-written asset before its `uri` reaches you. Call it after the
`launchImageLibrary` / `launchCamera` promise has settled.

### Releasing individual assets

`cleanTempFiles()` is all-or-nothing. When you keep some assets and are done
with others — the user deselected one, an upload finished, a preview was
dismissed — release just those:

```ts
import { releaseAssets } from '@eugeniuszx/react-native-media-picker';

await releaseAssets(result.assets ?? []); // whole batch
await releaseAssets(asset); // one asset
await releaseAssets(asset.uri); // one uri
```

It accepts an asset, a `uri` string, or an array mixing both. Passing an asset
also releases its `thumbnailUri`, which a bare `uri` string would leave behind.
Duplicates and empty entries are dropped, and an empty list never reaches the
native module — it resolves `0` right away.

Only files inside the library's own `rn-media-picker` directory can be deleted:
a `uri` pointing anywhere else — or one this library did not hand out — is
ignored rather than acted on. Non-`file://` uris are ignored too.

Like `cleanTempFiles`, it never rejects, and resolves with the number of files
deleted once they are gone. A `uri` it declines to touch simply is not counted.
The same warning applies: do not call it while a pick is in flight.

Upgrading from 0.2.x: temp files written by earlier versions went straight into
the cache/temp directory rather than the `rn-media-picker` subdirectory, so
neither `cleanTempFiles` nor the 24-hour sweep touches them. They are left to the
OS to reclaim.

## Permissions

Gallery picking needs **no** permissions. Camera capture may require permissions — see
[Camera permissions](#camera-permissions) above.

## Expo

The library is not available in Expo Go (it ships native code), but it works in
any project using `expo prebuild` / a development build. A config plugin is
included so you do not have to edit `Info.plist` by hand:

```json
{
  "expo": {
    "plugins": [
      [
        "@eugeniuszx/react-native-media-picker",
        {
          "cameraPermission": "Let $(PRODUCT_NAME) take photos for your profile."
        }
      ]
    ]
  }
}
```

| Prop | Type | Default | Effect |
|---|---|---|---|
| `cameraPermission` | `string \| false` | a generic sentence | Text for `NSCameraUsageDescription`. `false` leaves `Info.plist` alone — use it if you only pick from the gallery, or set the key yourself |
| `microphonePermission` | `string \| false` | a generic sentence | Text for `NSMicrophoneUsageDescription`, required to record video on iOS — see [Camera permissions](#camera-permissions). `false` leaves `Info.plist` alone |
| `enableAndroidCameraPermission` | `boolean` | `false` | Adds `android.permission.CAMERA` to the manifest |

Both permission keys are written by default, since a missing
`NSMicrophoneUsageDescription` terminates the app when the camera opens in video
mode. Pass `false` for either one to keep it out of `Info.plist`.

Leave `enableAndroidCameraPermission` off unless you need the permission for
something else: declaring `CAMERA` is what makes this library ask for it at
runtime, and without it the system camera app needs no permission at all — see
[Camera permissions](#camera-permissions).

The plugin adds no runtime dependency; it resolves `expo/config-plugins` from
your app, and non-Expo projects never load it.

## Testing

Jest cannot load a TurboModule, so mock the package. A ready-made mock ships
with it:

```ts
jest.mock('@eugeniuszx/react-native-media-picker', () =>
  require('@eugeniuszx/react-native-media-picker/jest/mock')
);
```

Every entry point becomes a `jest.fn()`: `launchImageLibrary` and `launchCamera`
resolve `{ didCancel: true }`, `cleanTempFiles` and `releaseAssets` resolve `0`,
and `getCameraPermissionStatus`/`requestCameraPermission` resolve `'granted'`. So
stage a result per test:

```ts
import { launchImageLibrary } from '@eugeniuszx/react-native-media-picker';

(launchImageLibrary as jest.Mock).mockResolvedValueOnce({
  didCancel: false,
  assets: [{ uri: 'file:///tmp/a.jpg', type: 'image/jpeg' }],
});
```

The mock covers the entry points only — the pure option helpers
(`normalizeLibraryOptions`, `collectReleasableUris`) are not re-exported,
since they need no mocking.

## Upgrading from 1.4.x

Everything new is opt-in, and no runtime behaviour changed for code that does
not ask for it. Two things can break a build, both in the type layer.

- **`PickerResponse` is now a discriminated union.** Reading `assets`,
  `errorCode` or `errorMessage` on an un-narrowed value compiles exactly as it
  did, so code that only *consumes* a response is unaffected; the gain is that
  `if (didCancel) … if (errorCode) …` now narrows `assets` to `Asset[]` instead
  of `Asset[] | undefined`.

  **Code that *constructs* the type is the half that breaks** — a typed test
  mock, a fixture in your own test utilities, a helper declared to return
  `PickerResponse`. `{ didCancel: false }` with no `assets` and no `errorCode`
  matches no member of the union and no longer compiles. Say which case you
  meant:

  ```ts
  import type { Asset, PickerResponse } from '@eugeniuszx/react-native-media-picker';

  const cancelled: PickerResponse = { didCancel: true };
  const picked = (assets: Asset[]): PickerResponse => ({ didCancel: false, assets });
  const failed: PickerResponse = {
    didCancel: false,
    errorCode: 'busy',
    errorMessage: 'Already waiting for a pick.',
  };
  ```

  The [Jest mock](#testing) that ships with the package is untyped, so
  `mockResolvedValueOnce` on it is unaffected either way.

- **`ErrorCode` gained `'busy'`**, returned where `'others'` used to be for
  `'Already waiting for a pick.'`. The message itself is unchanged, and no other
  code moved. An exhaustive `switch` over `ErrorCode` needs a new arm — the one
  break the compiler will point at.

  Less visibly, **code comparing `errorCode === 'others'` as a catch-all** — the
  "something went wrong" branch, the fallback toast — stops matching the
  double-tap case, which was probably the most common thing reaching it.
  TypeScript says nothing, because that comparison is still valid. Handle
  `'busy'` deliberately: see [Response & error handling](#response--error-handling)
  for what it covers and, just as importantly, what it does not.

New, all opt-in:

- `mediaType: 'video'` for `launchCamera`, with `maxDuration`, `videoQuality`
  and `includeThumbnail` — see [Recording video](#recording-video).
- `includeExif` and `stripMetadata` for photos — see [Metadata](#metadata).

One thing to add before you ship the first of those: **recording video on iOS
requires `NSMicrophoneUsageDescription` in `Info.plist`**, or the app is
terminated the moment the camera opens. The Expo config plugin writes the key
for you now; a bare React Native app has to add it by hand. See
[Camera permissions](#camera-permissions).

## Upgrading from 1.3.x

Everything new is additive, but four behaviours changed. None of them alters a
type, so TypeScript will not point them out for you.

- **`Asset.fileName` is now the name from the gallery**, not the temp file's
  generated name — `IMG_4821.jpg` instead of `media_picker_<uuid>.jpg`. The
  extension still matches `type`, and `uri` is unchanged. Code that reconstructed
  a path from `fileName` was already relying on something it should not; use
  `uri`. Camera captures keep the generated name.
- **On iOS, picked assets now come back in tap order** rather than in library
  order. If you were re-sorting them yourself to get that, you can stop.
- **On iOS with `format: 'original'`, HEIC photos now stay HEIC more often.**
  Earlier the system was free to hand over a JPEG it transcoded itself; the
  library now asks for the stored representation. Pass `format: 'jpeg'` if you
  need JPEG — see [Format handling](#format-handling).
- **`cleanTempFiles()` and `releaseAssets()` resolve `Promise<number>`** — the
  number of files deleted — and only once the files are actually gone, instead of
  as soon as the sweep was scheduled. Awaiting them is now meaningful; code that
  ignored the result keeps working.

New: [`getCameraPermissionStatus()` and `requestCameraPermission()`](#camera-permissions).
`peerDependencies` now state the versions the library has always needed
(`react-native >= 0.76`, `react >= 18.2`), so a mismatched install warns instead
of failing at runtime.

## Migrating from 0.2.x

- `LibraryOptions.mediaType` is back (it was removed because it was never
  read): `'photo' | 'video' | 'mixed'`, default `'photo'`. `Asset.duration`
  is populated for video assets.
- `PickerResponse.errorCode` is now the `ErrorCode` union instead of `string`.
  `ErrorCode` is exported from the package root.
- New: `cleanTempFiles()`. Assets now live in a dedicated `rn-media-picker`
  subdirectory of the temp/cache directory, and files older than 24 hours are
  swept when the native module initializes — see [Temp files](#temp-files).
- On Android, a multi-image pick where some items fail now returns the
  successful ones instead of failing the whole batch, matching iOS.
- On Android, `maxWidth`/`maxHeight` are now honoured for images whose EXIF
  orientation is a quarter turn. Previously the bounds were tested against the
  stored pixel buffer rather than the displayed axes, so such an image could skip
  the resize entirely and come back **larger than the bound you asked for** — a
  3000×4000 buffer that displays as 4000×3000 was returned untouched at 4000px
  wide under `maxWidth: 3500`. (Reported `width`/`height` were never wrong; they
  always described the file that was returned.) Those images are now re-encoded
  where they previously were not, so for them `type` can change (HEIC becomes
  `image/jpeg` on Android), `fileSize` changes, and `quality` starts applying.
- On iOS, a `launchImageLibrary` call rejected because another pick is in flight
  now reports `errorMessage: 'Already waiting for a pick.'` instead of
  `'Already waiting for an image pick.'`. Both platforms and both entry points
  now use the one string, so code matching on the old message fails silently —
  match on `errorCode` instead, which is `'busy'` as of 1.5.0 and was `'others'`
  before it.
- On Android, the single-pick-in-flight window now extends through image
  processing instead of ending the moment the picker closes. A pick started while
  the previous batch is still decoding resolves `errorCode: 'busy'` with
  `'Already waiting for a pick.'` rather than running concurrently — matching
  iOS. Chaining the next pick off the previous promise is unaffected.

## License

MIT
