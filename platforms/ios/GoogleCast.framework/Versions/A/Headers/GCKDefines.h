// Copyright 2014 Google Inc.

#import <Availability.h>

#define GCK_EXPORT __attribute__((visibility ("default")))

#ifdef __cplusplus
#define GCK_EXTERN extern "C" GCK_EXPORT
#else
#define GCK_EXTERN extern GCK_EXPORT
#endif
