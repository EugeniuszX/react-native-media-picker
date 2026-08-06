#import "ReactNativeMediaPicker.h"

#if __has_include("ReactNativeMediaPicker-Swift.h")
#import "ReactNativeMediaPicker-Swift.h"
#else
#import <ReactNativeMediaPicker/ReactNativeMediaPicker-Swift.h>
#endif

static NSDictionary *RNMediaPickerBuildResponse(NSArray<NSDictionary<NSString *, id> *> *assets,
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
    PickerCoordinator *_coordinator;
}

- (instancetype)init
{
    if (self = [super init]) {
        _coordinator = [PickerCoordinator new];
    }
    return self;
}

- (void)launchImageLibrary:(JS::NativeReactNativeMediaPicker::NativeLibraryOptions &)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
    [_coordinator launchImageLibraryWithSelectionLimit:(NSInteger)options.selectionLimit()
                                              maxWidth:(NSInteger)options.maxWidth()
                                             maxHeight:(NSInteger)options.maxHeight()
                                               quality:options.quality()
                                         includeBase64:options.includeBase64()
                                            completion:^(NSArray<NSDictionary<NSString *, id> *> *assets,
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
    [_coordinator launchCameraWithCameraType:options.cameraType()
                                    maxWidth:(NSInteger)options.maxWidth()
                                   maxHeight:(NSInteger)options.maxHeight()
                                     quality:options.quality()
                               includeBase64:options.includeBase64()
                                  completion:^(NSArray<NSDictionary<NSString *, id> *> *assets,
                                               BOOL didCancel,
                                               NSString *errorCode,
                                               NSString *errorMessage) {
        resolve(RNMediaPickerBuildResponse(assets, didCancel, errorCode, errorMessage));
    }];
}

- (void)cleanTempFiles:(RCTPromiseResolveBlock)resolve
                reject:(RCTPromiseRejectBlock)reject
{
    [_coordinator cleanTempFiles];
    resolve(nil);
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
