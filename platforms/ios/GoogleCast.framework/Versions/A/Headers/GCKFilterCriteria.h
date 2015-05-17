// Copyright 2013 Google Inc.

#import "GCKDefines.h"

/**
 * Filter criteria for applications.
 *
 * @ingroup Discovery
 */
GCK_EXPORT
@interface GCKFilterCriteria : NSObject <NSCopying, NSCoding>

/**
 * Criteria for an application which is available to be launched on a device. The application does
 * not need to be currently running.
 *
 * @param applicationID The application ID. Must be non-nil.
 */
+ (instancetype)criteriaForAvailableApplicationWithID:(NSString *)applicationID;

/**
 * Criteria for an application which is currently running on the device and supports all of
 * the given namespaces, optionally also with a particular application ID.
 *
 * @param applicationID The application ID. Optional; may be nil, in which case only the namespace
 * will be used.
 * @param supportedNamespaces An array of namespace strings. Must be non-nil.
 *
 * @deprecated This method is deprecated. Filtering by running application ID is not supported by
 * the SDK. Calling this method will ignore the applicationID parameter and simply delegate to
 * @link #criteriaForRunningApplicationWithSupportedNamespaces: @endlink.
 */
+ (instancetype)criteriaForRunningApplicationWithID:(NSString *)applicationID
                                supportedNamespaces:(NSArray *)supportedNamespaces;

/**
 * Criteria for an application which is currently running on the device and supports all of
 * the given namespaces.
 *
 * @param supportedNamespaces An array of namespace strings. Must be non-nil.
 */
+ (instancetype)criteriaForRunningApplicationWithSupportedNamespaces:(NSArray *)supportedNamespaces;

@end
