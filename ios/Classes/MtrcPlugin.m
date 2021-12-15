#import "MtrcPlugin.h"
#if __has_include(<mtrc/mtrc-Swift.h>)
#import <mtrc/mtrc-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "mtrc-Swift.h"
#endif

@implementation MtrcPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMtrcPlugin registerWithRegistrar:registrar];
}
@end
