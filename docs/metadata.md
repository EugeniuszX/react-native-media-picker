# Metadata

What `includeExif` and `stripMetadata` do, and what each of them costs.

Two independent options, both `false` by default and both photo-only:
`includeExif` reads metadata **out of the source** and hands it to you,
`stripMetadata` keeps metadata **out of the file** that is written. They do not
interact — asking for both is the ordinary way to keep the location of a photo
for yourself without shipping it inside the bytes you upload.

## `includeExif`

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
on iOS" note in the [`CameraOptions`](../README.md#cameraoptions) table.

## `stripMetadata`

`stripMetadata: true` removes the EXIF and the GPS from the photo written to
`uri`. There are two ways to get there, and which one an asset takes is worth
knowing, because only one of them is free:

- **Rewriting the container in place.** The already-compressed pixel data is
  copied across untouched, so nothing is decoded and no quality is lost — on iOS
  this was measured: the scrubbed JPEG's quantisation tables are byte-identical
  to the source's and its entropy-coded scan is byte-identical too, so **not one
  pixel changes**. The file gets slightly smaller, by roughly the size of the
  metadata that was removed. An auxiliary image stored beside the main one is
  carried across intact: this was measured on an HDR gain map, which comes back
  bit-for-bit. A depth map rides the same copy and should survive with it, but
  that was not measured.
- **Re-encoding.** Used where the container cannot be rewritten. The image is
  decoded and written afresh, so `fileSize` changes, `type` and `fileName` can
  change with it, and the pixels are no longer the source's. It also writes one
  image and nothing else: **auxiliary images stored beside the main one — an HDR
  gain map, a depth map — are lost.** On iOS that lands squarely on HEIC, which
  is the default iPhone capture format, so `stripMetadata: true` on a photo
  straight out of the camera comes back flat, without its HDR gain map and
  without its depth map. Nothing in the result says so.

The photo's **rendered orientation** survives either route, though by different
means. The in-place rewrite keeps the EXIF orientation tag — dropping it would
render a quarter-turned photo sideways, and the reported `width`/`height` already
assume it is there. A source carrying no orientation tag comes back with an
explicit `Orientation = 1`, which is what its absence already meant. The
re-encode bakes the rotation into the pixels instead and writes a file whose
EXIF holds only ImageIO's own basics — the orientation comes back as `1` — so
the image is simply upright. Either way `width`/`height` describe what you were
handed.

| Source | iOS | Android |
|---|---|---|
| JPEG | rewritten in place | rewritten in place |
| PNG | **re-encoded**, stays `image/png` | rewritten in place |
| HEIC | **re-encoded**, stays `image/heic` (`image/jpeg` where there is no HEIC encoder) | **re-encoded to `image/jpeg`** |
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
re-encoded too and normally stays `image/heic`, so `type` and `fileName` do not
move — `fileSize` and the pixels do. This mirrors what a resize already does to a
HEIC on Android — see [Format handling](formats.md). The exception is a
device with no HEIC encoder — the iOS Simulator, and older hardware: there the
encoder falls back to JPEG without announcing it, so `type` becomes `image/jpeg`
and `fileName`'s extension follows after all. It is the same fallback
[Format handling](formats.md) describes for a resize, and it applies for
the same reason: a file in the wrong container beats no strip at all.

**On iOS only a JPEG is rewritten in place; a PNG and a HEIC are re-encoded.**
This is narrower than it looks like it should be, and it is a measured result
rather than a policy. The one ImageIO call that copies the compressed data
unmodified strips a JPEG completely, but on a PNG it leaves every `tEXt` credit
where it is and *adds* an XMP packet rebuilt out of them, and on a HEIC it keeps
Artist, Copyright, DateTime, Software and the XMP. Neither is an acceptable
answer to `stripMetadata: true`, so both fall through to the re-encode. That
re-encode is clean by construction rather than by measurement: it hands a freshly
decoded bitmap to `UIImage.pngData()` or to an ImageIO HEIC destination with no
metadata attached, so there is no source container left for a tag to come out of.
The re-encode that was *measured* clean was a JPEG, written by a third writer
(`jpegData`). A PNG re-encode is not a quality loss in the JPEG sense — PNG
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
source. On iOS a JPEG carrying an IPTC block is still scrubbed losslessly, and
that one is measured: on a JPEG with a ten-dataset Photoshop `APP13` — by-line,
by-line title, city, country, headline, credit, caption, copyright — every
dataset is gone from the output, while the quantisation tables, Huffman tables
and entropy-coded scan come back byte-identical and 0 of 921 600 samples change.
What survives is an empty
`8BIM` `0x0404` resource: a 12-byte shell with a zero-length payload, carrying
nothing from the source. A PNG with text chunks is re-encoded on iOS too, for the
reason given above.

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

## Without `stripMetadata`

The default is unchanged from earlier versions and is worth stating next to the
above, because it is the case most photos hit: a photo returned **untouched**
keeps every byte of EXIF and GPS it arrived with, and a photo that was
**resized** loses all of it, since a re-encode writes fresh bytes and this
version does not copy metadata across one. So the metadata a caller ends up
shipping today depends on whether a resize happened. `stripMetadata: true` is
the way to stop it depending on that.
