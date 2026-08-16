# Temp files

Where the bytes behind `Asset.uri` live, and how to get rid of them.

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

## Releasing individual assets

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
