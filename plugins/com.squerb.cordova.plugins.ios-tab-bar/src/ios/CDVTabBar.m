/*
 *  CDVTabBar
 *
 *  Updated By Squerb
 *
 *  Based on:
 *
 *  NativeControls
 *  Created by Jesse MacFadyen on 10-02-03.
 *  MIT Licensed
 *
 *  Originally this code was developed my Michael Nachbaur
 *  Formerly -> PhoneGap :: UIControls.h
 *  Created by Michael Nachbaur on 13/04/09.
 *  Copyright 2009 Decaf Ninja Software. All rights reserved.
 */
#import "CDVTabBar.h"

#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)

@implementation CDVTabBar

@synthesize webView;
@synthesize callbackId = _callbackId;
@synthesize listenerCallbackId = _listenerCallbackId;

- (void)pluginInitialize
{
  tabBarItems = [[NSMutableDictionary alloc] initWithCapacity:5];
  originalWebViewBounds = self.webView.bounds;
}

#pragma mark - Listener
/**
 * Bind listener for didSelectItem.
 */
-(void)bindListener:(CDVInvokedUrlCommand*)command
{
  self.listenerCallbackId = command.callbackId;

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [pluginResult setKeepCallbackAsBool:true];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

#pragma mark - TabBar

/**
 * Create a native tab bar at either the top or the bottom of the display.
 * @brief creates a tab bar
 * @param arguments unused
 * @param options unused
 */
- (void)createTabBar:(CDVInvokedUrlCommand*)command
{
	tabBar = [UITabBar new];
	tabBar.autoresizingMask = UIViewAutoresizingFlexibleBottomMargin | UIViewAutoresizingFlexibleTopMargin | UIViewAutoresizingFlexibleWidth;
	[tabBar sizeToFit];
	tabBar.delegate = self;

  if ( SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")) {
    tabBar.barStyle = UIBarStyleBlack;
    tabBar.translucent = NO;
    tabBar.barTintColor = [UIColor colorWithRed:0.122 green:0.122 blue:0.122 alpha:1]; /*#1f1f1f*/
    tabBar.tintColor = [UIColor colorWithRed:1 green:1 blue:1 alpha:1]; /*#ffffff*/
  } else {
    // Pre iOS 7
    tabBar.opaque = YES;
  }
  
	tabBar.multipleTouchEnabled   = NO;
	tabBar.autoresizesSubviews    = YES;
	tabBar.hidden                 = YES;
	tabBar.userInteractionEnabled = YES;

  self.webView.superview.autoresizesSubviews = YES;

	/* Styling hints REF UIInterface.h

	 tabBar.alpha = 0.5;
	 tabBar.tintColor = [UIColor colorWithRed:1.000 green:0.000 blue:0.000 alpha:1.000];

	 */

	[self.webView.superview addSubview:tabBar];

  if (command) {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
  }
}

/**
 * Show the tab bar after its been created.
 * @brief show the tab bar
 * @param arguments unused
 * @param options used to indicate options for where and how the tab bar should be placed
 * - \c height integer indicating the height of the tab bar (default: \c 49)
 * - \c position specifies whether the tab bar will be placed at the \c top or \c bottom of the screen (default: \c bottom)
 */
- (void)showTabBar:(CDVInvokedUrlCommand*)command
{

  NSDictionary *options = [command.arguments objectAtIndex:0];

  if (!tabBar) {
    [self createTabBar:nil];
  }
  
  // if we are calling this again when its shown, reset
  if (!tabBar.hidden) {
    return;
  }

  CGFloat height = 0.0f;
  BOOL atBottom = YES;

  //	CGRect offsetRect = [ [UIApplication sharedApplication] statusBarFrame];

  if (options)
  {
    height   = [[options objectForKey:@"height"] floatValue];
    atBottom = [[options objectForKey:@"position"] isEqualToString:@"bottom"];
  }
  if (height == 0)
  {
    height = 49.0f;
    atBottom = YES;
  }

  tabBar.hidden = NO;
  CGRect webViewBounds = originalWebViewBounds;
  CGRect tabBarBounds;

	NSNotification* notif = [NSNotification notificationWithName:@"CDVLayoutSubviewAdded" object:tabBar];
	[[NSNotificationQueue defaultQueue] enqueueNotification:notif postingStyle: NSPostASAP];

  if (atBottom)
  {
    tabBarBounds = CGRectMake(
                              webViewBounds.origin.x,
                              webViewBounds.origin.y + webViewBounds.size.height - height,
                              webViewBounds.size.width,
                              height
                              );
    webViewBounds = CGRectMake(
                               webViewBounds.origin.x,
                               webViewBounds.origin.y,
                               webViewBounds.size.width,
                               webViewBounds.size.height - height
                               );
  }
  else
  {
    tabBarBounds = CGRectMake(
                              webViewBounds.origin.x,
                              webViewBounds.origin.y,
                              webViewBounds.size.width,
                              height
                              );
    webViewBounds = CGRectMake(
                               webViewBounds.origin.x,
                               webViewBounds.origin.y + height,
                               webViewBounds.size.width,
                               webViewBounds.size.height - height
                               );
  }

  [tabBar setFrame:tabBarBounds];


  [self.webView setFrame:webViewBounds];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Hide the tab bar
 * @brief hide the tab bar
 * @param arguments unused
 * @param options unused
 */
- (void)hideTabBar:(CDVInvokedUrlCommand*)command
{
  if (!tabBar) {
    [self createTabBar:nil];
  }
	tabBar.hidden = YES;

  NSNotification* notif = [NSNotification notificationWithName:@"CDVLayoutSubviewRemoved" object:tabBar];
  [[NSNotificationQueue defaultQueue] enqueueNotification:notif postingStyle: NSPostASAP];

  CGRect webViewBounds = originalWebViewBounds;
  if ( SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")) {
    webViewBounds.origin.y += 20;
  }

  [self.webView setFrame:webViewBounds];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Create a new tab bar item for use on a previously created tab bar.  Use ::showTabBarItems to show the new item on the tab bar.
 *
 * If the supplied image name is one of the labels listed below, then this method will construct a tab button
 * using the standard system buttons.  Note that if you use one of the system images, that the \c title you supply will be ignored.
 * - <b>Tab Buttons</b>
 *   - tabButton:More
 *   - tabButton:Favorites
 *   - tabButton:Featured
 *   - tabButton:TopRated
 *   - tabButton:Recents
 *   - tabButton:Contacts
 *   - tabButton:History
 *   - tabButton:Bookmarks
 *   - tabButton:Search
 *   - tabButton:Downloads
 *   - tabButton:MostRecent
 *   - tabButton:MostViewed
 * @brief create a tab bar item
 * @param arguments Parameters used to create the tab bar
 *  -# \c name internal name to refer to this tab by
 *  -# \c title title text to show on the tab, or null if no text should be shown
 *  -# \c image image filename or internal identifier to show, or null if now image should be shown
 *  -# \c tag unique number to be used as an internal reference to this button
 * @param options Options for customizing the individual tab item
 *  - \c badge value to display in the optional circular badge on the item; if nil or unspecified, the badge will be hidden
 */
- (void)createTabBarItem:(CDVInvokedUrlCommand*)command
{

  NSArray *arguments = command.arguments;
  NSDictionary *options = arguments.count > 4 ? [arguments objectAtIndex:4]: nil;

  if (!tabBar) {
    [self createTabBar:nil];
  }

  NSString  *name      = [arguments objectAtIndex:0];
  NSString  *title     = [arguments objectAtIndex:1];
  NSString  *imageName = [arguments objectAtIndex:2];
  int tag              = [[arguments objectAtIndex:3] intValue];
  
  UITabBarItem *item = nil;
  if ([imageName length] > 0) {
    NSInteger systemItem = -1;
    if ([imageName isEqualToString:@"tabButton:More"])       systemItem = UITabBarSystemItemMore;
    if ([imageName isEqualToString:@"tabButton:Favorites"])  systemItem = UITabBarSystemItemFavorites;
    if ([imageName isEqualToString:@"tabButton:Featured"])   systemItem = UITabBarSystemItemFeatured;
    if ([imageName isEqualToString:@"tabButton:TopRated"])   systemItem = UITabBarSystemItemTopRated;
    if ([imageName isEqualToString:@"tabButton:Recents"])    systemItem = UITabBarSystemItemRecents;
    if ([imageName isEqualToString:@"tabButton:Contacts"])   systemItem = UITabBarSystemItemContacts;
    if ([imageName isEqualToString:@"tabButton:History"])    systemItem = UITabBarSystemItemHistory;
    if ([imageName isEqualToString:@"tabButton:Bookmarks"])  systemItem = UITabBarSystemItemBookmarks;
    if ([imageName isEqualToString:@"tabButton:Search"])     systemItem = UITabBarSystemItemSearch;
    if ([imageName isEqualToString:@"tabButton:Downloads"])  systemItem = UITabBarSystemItemDownloads;
    if ([imageName isEqualToString:@"tabButton:MostRecent"]) systemItem = UITabBarSystemItemMostRecent;
    if ([imageName isEqualToString:@"tabButton:MostViewed"]) systemItem = UITabBarSystemItemMostViewed;
    if (systemItem != -1)
      item = [[UITabBarItem alloc] initWithTabBarSystemItem:systemItem tag:tag];
  }

  if (!item) {
    item = [[UITabBarItem alloc] initWithTitle:title image:[UIImage imageNamed:imageName] tag:tag];
  }

  // Set badge if needed
  if ([options objectForKey:@"badge"])
    item.badgeValue = [options objectForKey:@"badge"];

  [tabBarItems setObject:item forKey:name];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Update an existing tab bar item to change its badge value.
 * @brief update the badge value on an existing tab bar item
 * @param arguments Parameters used to identify the tab bar item to update
 *  -# \c name internal name used to represent this item when it was created
 * @param options Options for customizing the individual tab item
 *  - \c badge value to display in the optional circular badge on the item; if nil or unspecified, the badge will be hidden
 */
- (void)updateTabBarItem:(CDVInvokedUrlCommand*)command
{

  NSArray *arguments = command.arguments;
  NSDictionary *options = [arguments objectAtIndex:1];

  if (!tabBar)
    [self createTabBar:nil];

  NSString  *name = [arguments objectAtIndex:0];
  UITabBarItem *item = [tabBarItems objectForKey:name];
  if (item)
    item.badgeValue = [options objectForKey:@"badge"];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


/**
 * Show previously created items on the tab bar
 * @brief show a list of tab bar items
 * @param arguments the item names to be shown
 * @param options dictionary of options, notable options including:
 *  - \c animate indicates that the items should animate onto the tab bar
 * @see createTabBarItem
 * @see createTabBar
 */
- (void)showTabBarItems:(CDVInvokedUrlCommand*)command
{

  NSArray *arguments = command.arguments;
  NSArray *names = [arguments objectAtIndex:0];
  NSDictionary *options = arguments.count > 1 ? [arguments objectAtIndex:1]: nil;
  
  if (!tabBar) {
    [self createTabBar:nil];
  }

  int count = (int) names.count;
  NSMutableArray *items = [[NSMutableArray alloc] initWithCapacity:count];
  for (int i = 0; i < count; i++) {
    NSString *itemName = [names objectAtIndex:i];
    UITabBarItem *item = [tabBarItems objectForKey:itemName];
    if (item) {
      [items addObject:item];
    }
  }

  BOOL animateItems = NO;
  if (![options isKindOfClass:[NSNull class]] && [options objectForKey:@"animate"]) {
    animateItems = [(NSString*)[options objectForKey:@"animate"] boolValue];
  }
  [tabBar setItems:items animated:animateItems];

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/**
 * Manually select an individual tab bar item, or nil for deselecting a currently selected tab bar item.
 * @brief manually select a tab bar item
 * @param arguments the name of the tab bar item to select
 * @see createTabBarItem
 * @see showTabBarItems
 */
- (void)selectTabBarItem:(CDVInvokedUrlCommand*)command
{
  NSArray *arguments = command.arguments;

  if (!tabBar)
    [self createTabBar:nil];

  NSString *itemName = [arguments objectAtIndex:0];
  UITabBarItem *item = [tabBarItems objectForKey:itemName];
  if (item)
    tabBar.selectedItem = item;
  else
    tabBar.selectedItem = nil;

  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)tabBar:(UITabBar *)tabBar didSelectItem:(UITabBarItem *)item
{
	// Create Plugin Result
	CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:item.tag];
  [pluginResult setKeepCallbackAsBool:true];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:self.listenerCallbackId];
}

@end
