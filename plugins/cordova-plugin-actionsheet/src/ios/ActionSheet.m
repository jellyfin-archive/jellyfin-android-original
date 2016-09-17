#import "ActionSheet.h"

@implementation ActionSheet

#pragma mark - Cordova interface methods
- (void) show:(CDVInvokedUrlCommand*)command {
  self.callbackId = command.callbackId;
  NSDictionary* options = command.arguments[0];

  NSString *title  = options[@"title"] ?: nil;
  NSString *subtitle  = options[@"subtitle"] ?: nil;
  NSArray *buttons = options[@"buttonLabels"];
  NSArray *position = options[@"position"] ?: nil;
  NSString *addCancelButtonWithLabel = options[@"addCancelButtonWithLabel"] ?: nil;
  NSString *addDestructiveButtonWithLabel = options[@"addDestructiveButtonWithLabel"] ?: nil;
  BOOL destructiveButtonLast = [options[@"destructiveButtonLast"] isEqual:[NSNumber numberWithBool:YES]];

  [self.commandDelegate runInBackground:^{

    if ([UIAlertController class]) {

      self.alertController = [UIAlertController alertControllerWithTitle:[self encodeString:title]
                                                                 message:[self encodeString:subtitle]
                                                          preferredStyle:UIAlertControllerStyleActionSheet];

      if (addDestructiveButtonWithLabel != nil && !destructiveButtonLast) {
        [self addDestructiveButton:addDestructiveButtonWithLabel toAlertController:self.alertController asLastItem:destructiveButtonLast relativeToButtons:buttons];
      }

      for(int i = 0; i < [buttons count]; i++) {
        UIAlertAction *action = [UIAlertAction actionWithTitle:[self encodeString:buttons[i]]
                                                               style:UIAlertActionStyleDefault
                                                             handler:^(UIAlertAction *action) {
                                                               int buttonIndex;
                                                               for(int i = 0; i < [buttons count]; i++) {
                                                                 if ([action.title isEqualToString:buttons[i]]) {
                                                                   buttonIndex = i+1;
                                                                   if (addDestructiveButtonWithLabel != nil && !destructiveButtonLast) {
                                                                     buttonIndex++;
                                                                   }
                                                                   [self respondWithButtonIndex:buttonIndex];
                                                                   break;
                                                                 }
                                                               }
                                                             }];
        [self.alertController addAction:action];
      }

      if (addDestructiveButtonWithLabel != nil && destructiveButtonLast) {
        [self addDestructiveButton:addDestructiveButtonWithLabel toAlertController:self.alertController asLastItem:destructiveButtonLast relativeToButtons:buttons];
      }

      if (addCancelButtonWithLabel != nil) {
        UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:addCancelButtonWithLabel
                                                               style:UIAlertActionStyleCancel
                                                             handler:^(UIAlertAction *action) {
                                                               [self respondWithButtonIndex:(int)buttons.count +(addDestructiveButtonWithLabel == nil ? 1 : 2)];
                                                             }];
        [self.alertController addAction:cancelAction];
      }

      dispatch_async(dispatch_get_main_queue(), ^{
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
          [self.alertController setModalPresentationStyle:UIModalPresentationPopover];
          UIPopoverPresentationController *popPresenter = [self.alertController popoverPresentationController];
          popPresenter.sourceView = self.webView.superview;
          if (position == nil) {
            NSLog(@"Because the 'postion' param is not set, on iPad the popup is shown in the top left corner.");
          }
          popPresenter.sourceRect = [self getPopupRectFromIPadPopupCoordinates:(position != nil ? position : @[@40, @20])];
        }
        [self.viewController presentViewController:self.alertController animated:YES completion:nil];
      });

    } else {

      self.actionSheet = [[UIActionSheet alloc] initWithTitle:[self encodeString:title]
                                                delegate:self
                                       cancelButtonTitle:nil
                                  destructiveButtonTitle:[self encodeString:addDestructiveButtonWithLabel]
                                       otherButtonTitles:nil];

      for(int i = 0; i < [buttons count]; i++) {
        [self.actionSheet addButtonWithTitle:[self encodeString:buttons[i]]];
      }

      if (addCancelButtonWithLabel != nil) {
        [self.actionSheet addButtonWithTitle:[self encodeString:addCancelButtonWithLabel]];
        self.actionSheet.cancelButtonIndex = [buttons count]+(addDestructiveButtonWithLabel == nil ? 0 : 1);
      }

      dispatch_async(dispatch_get_main_queue(), ^{
        if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
          if (position!= nil) {
            CGRect rect = [self getPopupRectFromIPadPopupCoordinates:position];
            [self.actionSheet showFromRect:rect inView:self.webView.superview animated:YES];
          } else {
            [self.actionSheet showInView:self.webView.superview];
          }
        } else {
          // In this case the device is an iPhone/iPod Touch.
          [self.actionSheet showInView:self.webView.superview];
        }
      });
    }
  }];
}

- (void) addDestructiveButton:(NSString*)title toAlertController:(UIAlertController*)alertController asLastItem:(BOOL)isLast relativeToButtons:(NSArray*)buttons {

  [alertController addAction:[UIAlertAction actionWithTitle:title
                                                      style:UIAlertActionStyleDestructive
                                                    handler:^(UIAlertAction *action) {
                                                      if (isLast) {
                                                        [self respondWithButtonIndex:1 + (int)buttons.count];
                                                      } else {
                                                        [self respondWithButtonIndex:1];
                                                      }
                                                    }]];
}

- (void) respondWithButtonIndex:(int)index {
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                       messageAsInt:index];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
}

- (void) hide:(CDVInvokedUrlCommand*)command {
  dispatch_async(dispatch_get_main_queue(), ^{
    if ([UIAlertController class]) {
      [self.alertController dismissViewControllerAnimated:YES completion:nil];
      [self respondWithButtonIndex:-1];
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else {
      // dismissing with -2 because it's +1'd by didDismissWithButtonIndex below and we want it to report -1
      [self.actionSheet dismissWithClickedButtonIndex:-2 animated:true];
      CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
  });
}

#pragma mark - helper methods
- (NSString*) encodeString:(NSString*)input {
  if (input == nil) {
    return nil;
  } else {
    return input;
  }
}

- (CGRect)getPopupRectFromIPadPopupCoordinates:(NSArray*)comps {
  CGRect rect = CGRectZero;
  if ([comps count] == 2) {
    rect = CGRectMake([[comps objectAtIndex:0] integerValue], [[comps objectAtIndex:1] integerValue], 0, 0);
  } else if ([comps count] == 4) {
    rect = CGRectMake([[comps objectAtIndex:0] integerValue], [[comps objectAtIndex:1] integerValue], [[comps objectAtIndex:2] integerValue], [[comps objectAtIndex:3] integerValue]);
  }
  return rect;
}

#pragma mark - UIActionSheetDelegate methods
- (void)actionSheet:(UIActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex {

  // ActionSheet button index is 0-based, but other Cordova plugins are 1-based (prompt, confirm)
  buttonIndex++;

  [self respondWithButtonIndex:(int)buttonIndex];
}

@end
