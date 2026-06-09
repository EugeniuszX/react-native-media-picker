#import "ReactNativeMediaPicker.h"

#if __has_include("ReactNativeMediaPicker-Swift.h")
#import "ReactNativeMediaPicker-Swift.h"
#else
#import <ReactNativeMediaPicker/ReactNativeMediaPicker-Swift.h>
#endif

static NSDictionary *RNMediaPickerBuildResponse(NSArray<NSDictionary *> *assets,
                                                BOOL didCancel,
                                                NSString *errorCode,
                                                NSString *errorMessage) {
  NSMutableDictionary *response = [NSMutableDictionary dictionary];
  response[@"didCancel"] = @(didCancel);
  if (assets != nil) {
    response[@"assets"] = assets;
  }
  if (errorCode != nil) {
    response[@"errorCode"] = errorCode;
  }
  if (errorMessage != nil) {
    response[@"errorMessage"] = errorMessage;
  }
  return response;
}

@implementation ReactNativeMediaPicker {
    MediaPickerImpl *_impl;
}

- (instancetype)init
{
    if (self = [super init]) {
        _impl = [MediaPickerImpl new];
    }
    return self;
}

- (void)launchImageLibrary:(JS::NativeReactNativeMediaPicker::NativeLibraryOptions &)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
    NSInteger selectionLimit = (NSInteger)options.selectionLimit();
    double maxWidth = options.maxWidth();
    double maxHeight = options.maxHeight();
    double quality = options.quality();
    BOOL includeBase64 = options.includeBase64();

    [_impl launchImageLibrary:selectionLimit
                     maxWidth:maxWidth
                    maxHeight:maxHeight
                      quality:quality
                includeBase64:includeBase64
                   completion:^(NSArray<NSDictionary *> *assets,
                                BOOL didCancel,
                                NSString *errorCode,
                                NSString *errorMessage) {
        resolve(RNMediaPickerBuildResponse(assets, didCancel, errorCode, errorMessage));
    }];
}

- (void)launchCamera:(JS::NativeReactNativeMediaPicker::NativeCameraOptions &)options
             resolve:(RCTPromiseResolveBlock)resolve
              reject:(RCTPromiseRejectBlock)reject
{
    NSString *cameraType = options.cameraType();
    double maxWidth = options.maxWidth();
    double maxHeight = options.maxHeight();
    double quality = options.quality();
    BOOL includeBase64 = options.includeBase64();

    [_impl launchCamera:cameraType
               maxWidth:maxWidth
              maxHeight:maxHeight
                quality:quality
          includeBase64:includeBase64
             completion:^(NSArray<NSDictionary *> *assets,
                          BOOL didCancel,
                          NSString *errorCode,
                          NSString *errorMessage) {
        resolve(RNMediaPickerBuildResponse(assets, didCancel, errorCode, errorMessage));
    }];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeReactNativeMediaPickerSpecJSI>(params);
}

+ (NSString *)moduleName
{
  return @"ReactNativeMediaPicker";
}

@end
