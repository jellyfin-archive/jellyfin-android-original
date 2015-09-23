/*
 *  CDVTabBar.h
 *
 *
 *  Created by Jesse MacFadyen on 10-02-03.
 *  MIT Licensed
 *
 *  Originally this code was developed my Michael Nachbaur
 *  Formerly -> PhoneGap :: UIControls.h
 *  Created by Michael Nachbaur on 13/04/09.
 *  Copyright 2009 Decaf Ninja Software. All rights reserved.
 */
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <UIKit/UITabBar.h>
#import <UIKit/UIToolbar.h>
#import <Cordova/CDVPlugin.h>

@interface CDVTabBar : CDVPlugin <UITabBarDelegate> {
	CGRect	originalWebViewBounds;
	
  UITabBar* tabBar;
	NSMutableDictionary* tabBarItems;
}

#pragma mark - Properties

@property (nonatomic, copy) NSString* callbackId;
@property (nonatomic, copy) NSString* listenerCallbackId;

#pragma mark - Listener
- (void)bindListener:(CDVInvokedUrlCommand*)command;

#pragma mark - Tab bar methods
/* Tab Bar methods
 */
- (void)createTabBar:(CDVInvokedUrlCommand*)command;
- (void)showTabBar:(CDVInvokedUrlCommand*)command;
- (void)hideTabBar:(CDVInvokedUrlCommand*)command;
- (void)showTabBarItems:(CDVInvokedUrlCommand*)command;
- (void)createTabBarItem:(CDVInvokedUrlCommand*)command;
- (void)updateTabBarItem:(CDVInvokedUrlCommand*)command;
- (void)selectTabBarItem:(CDVInvokedUrlCommand*)command;

@end
