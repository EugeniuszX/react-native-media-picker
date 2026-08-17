# Changelog

## [1.5.1](https://github-melber17/EugeniuszX/react-native-media-picker/compare/v1.5.0...v1.5.1) (2026-08-17)


### Bug Fixes

* change media picker provider ([c21c61e](https://github-melber17/EugeniuszX/react-native-media-picker/commit/c21c61e53ef7f4ad1142788cb8180248fdb5b778))

# [1.5.0](https://github-melber17/EugeniuszX/react-native-media-picker/compare/v1.4.0...v1.5.0) (2026-08-16)


### Bug Fixes

* decline iptc and png text residue before scrubbing ([bdebef9](https://github-melber17/EugeniuszX/react-native-media-picker/commit/bdebef9762646f6bc7cb761d4651d26e0a227599))
* decline scrubbing xmp sources and keep png rendering chunks ([a8e6b56](https://github-melber17/EugeniuszX/react-native-media-picker/commit/a8e6b564859992d8dcf42862f6ce32d40e32138e))
* decline scrubbing xmp sources on android ([3719c45](https://github-melber17/EugeniuszX/react-native-media-picker/commit/3719c455ba93382e6db536a7ec35d3958bccd023))
* default the ios scrub orientation to 1 so the metadata object is never empty ([5f29c1b](https://github-melber17/EugeniuszX/react-native-media-picker/commit/5f29c1b935ba94aae1e722993ea098943db5c1f1))
* match the png xmp keyword on ios and delete the recorded capture file ([d5cf112](https://github-melber17/EugeniuszX/react-native-media-picker/commit/d5cf112331dbba48b928c09da9d74f23bb770696))
* merge the example app.json usage strings into the generated info.plist ([a9413e1](https://github-melber17/EugeniuszX/react-native-media-picker/commit/a9413e149995225ca1658740812f2f6a57945a4e))
* reject non-ascii digits and non-finite coordinates ([8498db9](https://github-melber17/EugeniuszX/react-native-media-picker/commit/8498db9d1803c5ce3cdbf69e35d3c4c1c24a98a4))
* require the movie media type before recording video on ios ([4326f15](https://github-melber17/EugeniuszX/react-native-media-picker/commit/4326f15995f7a7a71f6868b829e1faa50fb6fc69))
* scrub metadata with CopyImageSource so the jpeg is left byte-identical ([19d8054](https://github-melber17/EugeniuszX/react-native-media-picker/commit/19d80544703a355c9e71b343ed92e37b5a2e7bc2))
* settle the android video pick when the thumbnail runs out of memory ([cac4b26](https://github-melber17/EugeniuszX/react-native-media-picker/commit/cac4b2653ff25ad039ccc5118abf23c593bcc3d7))
* strip every exif tag and scrub animated webp ([eeefa69](https://github-melber17/EugeniuszX/react-native-media-picker/commit/eeefa6949f434e118e8153a77dd6c6a2283e7e3d))
* strip png text chunks and xmp on ios scrub ([518c4e9](https://github-melber17/EugeniuszX/react-native-media-picker/commit/518c4e996ab82c228ffd66d5bb1b065183495cbe))


### Features

* add exif to the native asset spec ([5c072b5](https://github-melber17/EugeniuszX/react-native-media-picker/commit/5c072b5e20941767be9d4ed4885b4b2b86433c7f))
* add metadata and camera video options to the js layer ([ed2e4a3](https://github-melber17/EugeniuszX/react-native-media-picker/commit/ed2e4a30f00aa986beb8c506f62d656745772f73))
* add pure exif date and gps coordinate parsers ([9d6b5d1](https://github-melber17/EugeniuszX/react-native-media-picker/commit/9d6b5d1b4716073e7c3a66a95308f90023016b9c))
* add the metadata plan that decides between skip scrub and reencode ([f9982b8](https://github-melber17/EugeniuszX/react-native-media-picker/commit/f9982b82f2b7e03b98066078f52e714326519b39))
* narrow PickerResponse into a discriminated union and add the busy error code ([9e4c140](https://github-melber17/EugeniuszX/react-native-media-picker/commit/9e4c140fbc50a47c1c62d17bc69c0dda8bc12339))
* read exif and strip metadata on android ([fb920d7](https://github-melber17/EugeniuszX/react-native-media-picker/commit/fb920d7036854b5447b2306cf98719385bd2e1c2))
* read exif and strip metadata on ios ([6684b3a](https://github-melber17/EugeniuszX/react-native-media-picker/commit/6684b3a6c3e5b7311e017ea63db00ef705221867))
* record video with the camera on android ([fb2e55a](https://github-melber17/EugeniuszX/react-native-media-picker/commit/fb2e55ab15bcea5d2c5a836f0bbbc0de9b2f6078))
* record video with the camera on ios ([98c40ae](https://github-melber17/EugeniuszX/react-native-media-picker/commit/98c40ae7bc7723fc78d74c3fb9d177abddd3b1ea))
* report an in-flight pick as the busy error code ([03ed58e](https://github-melber17/EugeniuszX/react-native-media-picker/commit/03ed58e85bcb951ffc4fc3237c8d30f06e4ef1a6))

# [1.4.0](https://github-melber17/EugeniuszX/react-native-media-picker/compare/v1.3.0...v1.4.0) (2026-08-14)


### Bug Fixes

* resolve the pick when the ios picker is swiped away ([39591d7](https://github-melber17/EugeniuszX/react-native-media-picker/commit/39591d7ab2b99b380d2c4797a6156677bba6f594))
* settle the temp file promises exactly once on android ([872a4a7](https://github-melber17/EugeniuszX/react-native-media-picker/commit/872a4a7ed225d8a7787b9ade10e7092e5a580547))


### Features

* expose the camera permission status and an explicit request ([b8e8c5e](https://github-melber17/EugeniuszX/react-native-media-picker/commit/b8e8c5ebb6133b99846297c9d914aedb3a007a44))
* keep the tap order and the stored representation on the iOS picker ([d977629](https://github-melber17/EugeniuszX/react-native-media-picker/commit/d977629e09fd9ffa157df3a8e5f5fd30ecdfd123))
* report the original file name of picked assets ([8046c0b](https://github-melber17/EugeniuszX/react-native-media-picker/commit/8046c0bcfc2abfc218e8539d76bd0a4aae80e26f))
* resolve cleanTempFiles and releaseAssets with the number of files removed ([17c14f2](https://github-melber17/EugeniuszX/react-native-media-picker/commit/17c14f2bcdd82e54d7c0f0854771f2ba7d3bc930))

# [1.3.0](https://github-melber17/EugeniuszX/react-native-media-picker/compare/v1.2.0...v1.3.0) (2026-08-14)


### Features

* add an Expo config plugin ([32925c9](https://github-melber17/EugeniuszX/react-native-media-picker/commit/32925c99d858fc98949009233cfb30b0e3cbeb44))
* generate a thumbnail for picked videos ([f59e47a](https://github-melber17/EugeniuszX/react-native-media-picker/commit/f59e47a01f0bdf48d18fecde1dbcff5170e0de78))
* release individual assets with releaseAssets ([82bc5de](https://github-melber17/EugeniuszX/react-native-media-picker/commit/82bc5ded422c9113b88c7982beaea2684ba79a90))
* ship a Jest mock entry point ([c284628](https://github-melber17/EugeniuszX/react-native-media-picker/commit/c284628ba64f2c0e38ea58098f1321a529269ed9))
