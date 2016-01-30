#import "SearchBar.h"

@implementation SearchBar

@synthesize nativeSearchBar, nativeNavigationBar;

#pragma mark - Constants

#define RGB(r, g, b) [UIColor colorWithRed:(float)r / 255.0 green:(float)g / 255.0 blue:(float)b / 255.0 alpha:1.0]

/* Duration of slide animations. */
static double const ANIMATION_DURATION = 0.4;

/* Determines if Cordova WebView should "shrink" underneith the SearchBar.
 * Set to NO to overlay SearchBar atop the WebView. */
static BOOL const RESIZE_WEBVIEW = NO;

/* Determines if we should offset the top of the view when device is at or above iOS7.
 * This will allow the status bar to properly display. Set to NO to use fullscreen. */
static BOOL const OFFSET_IOS7 = YES;

/* Determines if user is allowed to hit the "Search" button if they have NOT entered data.
 * Set to NO to ensure user has entered at least one character before search. */
static BOOL const ALLOW_EMPTY_SEARCH = YES;
    
static int const BACK_BUTTON_WIDTH = 50;

# pragma mark - Initialization

-(CDVPlugin*) initWithWebView:(UIWebView*)theWebView {
    isShowing = false;
    return (SearchBar*)[super initWithWebView:theWebView];
}

-(void)setupSearchBar:(BOOL)asNavigation {
    float barHeight = 44.0; // 44.0 is the standard height in px for iOS UISearchBar.
    // Setting y-offset to -height since we will be sliding-in the view, or none if embedding within a UINavigationbar.
    nativeSearchBar = [[UISearchBar alloc] initWithFrame:CGRectMake(0, asNavigation ? 0 : -barHeight, self.webView.superview.bounds.size.width, barHeight)];
    nativeSearchBar.delegate = self;
    nativeSearchBar.barTintColor = RGB(28, 28, 28);
    nativeSearchBar.showsCancelButton = true;
    
    if (ALLOW_EMPTY_SEARCH) {
        [self enableEmptySearch];
    }
}
    
-(void)setupNavigation {
    nativeNavigationBar = [[UINavigationBar alloc] initWithFrame:CGRectMake(0, 20, self.webView.superview.bounds.size.width, 44)];
    [nativeNavigationBar setBarTintColor: RGB(28, 28, 28)];
    [nativeNavigationBar setTintColor: [UIColor colorWithRed:0.6 green:0.6 blue:0.6 alpha:1]];
    [nativeNavigationBar setTranslucent: NO];
    [nativeNavigationBar addSubview:nativeSearchBar];

    [self.webView.superview addSubview: nativeNavigationBar];
}
    
-(UINavigationItem*)makeNavigationItem {
    UINavigationItem* nativeNavigationItem = [[UINavigationItem alloc] initWithTitle:@""];
    UIBarButtonItem* buttonItem = [[UIBarButtonItem alloc] initWithTitle:@"Back" style:UIBarButtonItemStyleBordered target:self action:@selector(popNavigation:)];
    [nativeNavigationItem setLeftBarButtonItem: buttonItem];
    return nativeNavigationItem;
}
    
-(void)enableEmptySearch {
    UITextField* searchField = [self getTextField];
    if (searchField) {
        searchField.enablesReturnKeyAutomatically = NO;
    }
}
    


# pragma mark - Bridge Methods

-(void)show:(CDVInvokedUrlCommand *)command {
    if (isShowing) {
        return;
    }
    
    if (nativeSearchBar == nil) {
        [self setupSearchBar:NO];
    }
    
    isShowing = true;
    
    [self.webView.superview addSubview:nativeSearchBar];
    [self slideDown: nativeSearchBar];
    [nativeSearchBar becomeFirstResponder];
}

-(void)hide:(CDVInvokedUrlCommand *)command {
    [self hideInternal];
}

-(void)hideInternal {
    if (!isShowing || nativeSearchBar == nil) {
        return;
    }
    
    isShowing = false;
    [nativeSearchBar resignFirstResponder];
    [self slideUp: nativeSearchBar];
    [self writeJavascript:[NSString stringWithFormat:@"cordova.fireDocumentEvent('searchClosed', {})"]];
}
    
-(void)showNavigation:(CDVInvokedUrlCommand *)command {
    if (isShowing) {
        return;
    }
    
    if (nativeNavigationBar == nil) {
        [self setupSearchBar:YES];
        [self setupNavigation];
    }
    
    isShowing = true;
}
    
-(void)hideNavigation:(CDVInvokedUrlCommand *)command {
    if (!isShowing || nativeSearchBar == nil) {
        return;
    }
    
    isShowing = false;
    [nativeSearchBar resignFirstResponder];
    [self slideUp: nativeSearchBar];
}
    
-(void)pushNavigation:(CDVInvokedUrlCommand *)command {
    [nativeNavigationBar pushNavigationItem:[self makeNavigationItem] animated:NO];
    
    if (nativeNavigationBar.items.count == 1) {
        [self slideOver:nativeSearchBar withOffsetX:BACK_BUTTON_WIDTH];
    }
}
    
-(void)popNavigation:(CDVInvokedUrlCommand *)command {
    [self.webView goBack];
    [nativeNavigationBar popNavigationItemAnimated:NO];
    if (nativeNavigationBar.items.count == 0) {
        [self slideOver:nativeSearchBar withOffsetX:-BACK_BUTTON_WIDTH];
    }
}

#pragma mark - Animations

/* Slides the SearchBar into view from the top. Assumes SearchBar is already part of the drawn view. */
-(void)slideDown:(UIView *)view {
    [UIView animateWithDuration:ANIMATION_DURATION
                     animations:^ {
                         [view setFrame:CGRectOffset([view frame], 0, view.frame.size.height + [self getOffset])];
                         if (RESIZE_WEBVIEW) [self shrinkWebView: view];
                     }];
}

/* Slides the SearchBar up and out of view. Removes the SearchBar from the superview after the animation. */
-(void)slideUp:(UIView *)view {
    [UIView animateWithDuration:ANIMATION_DURATION
                     animations:^ {
                         [view setFrame:CGRectOffset([view frame], 0, -(view.frame.size.height + [self getOffset]))];
                         if (RESIZE_WEBVIEW) [self restoreWebView];
                     }
                     completion:^(BOOL finished) {
                         [view removeFromSuperview];
                     }];
}
    
-(void)slideOver:(UIView*)view withOffsetX:(CGFloat)dx {
    CGRect rect = view.frame;
    [UIView animateWithDuration:ANIMATION_DURATION
                     animations:^ {
                         [view setFrame:CGRectMake(rect.origin.x + dx, rect.origin.y, view.frame.size.width - dx, rect.size.height)];
                     }];
}
    
#pragma mark - Helper Methods

/* Adjusts the height of the WebView to account for the SearchBar. */
-(void)shrinkWebView:(UIView *)underneithView {
    defaultWebViewFrame = self.webView.frame;
    
    CGRect newFrame = CGRectMake(defaultWebViewFrame.origin.x,
                                 underneithView.frame.size.height + [self getOffset],
                                 defaultWebViewFrame.size.width,
                                 defaultWebViewFrame.size.height - nativeSearchBar.frame.size.height);
    
    [self.webView setFrame:newFrame];
}

/* Restores the height of the WebView to its pre-SearchBar state. */
-(void)restoreWebView {
    [self.webView setFrame:defaultWebViewFrame];
}

/* Returns the y-axis offset needed for SearchBar based on OS version. */
-(CGFloat)getOffset {
    if (!OFFSET_IOS7) return 0.0;
    
    if ([self isiOS7]) {
        return 20.0; // Amount of offset required for versions at or above iOS 7.0
    }
    return 0.0;
}

    
// Returns YES if device is at or above iOS7.
-(BOOL)isiOS7 {
    NSString *sysVersion = [[UIDevice currentDevice] systemVersion];
    if ([sysVersion compare:@"7.0" options:NSNumericSearch] != NSOrderedAscending) {
        return YES;
    }
    return NO;
}

// Returns the UITextField associated with this search bar.
-(UITextField *)getTextField {
    if ([self isiOS7]) {
        // iOS7 requires iterating into an extra level of subviews.
        return [self getTextFieldiOS7];
    }
        
    for (UIView *subview in nativeSearchBar.subviews) {
        if ([subview isKindOfClass:[UITextField class]]) {
            return (UITextField *) subview;
        }
    }
    return nil;
}

// Returns the UITextField associated with this search bar. (iOS7+ devices)
-(UITextField *)getTextFieldiOS7 {
    for (UIView *subView in nativeSearchBar.subviews) {
        for (UIView *subSubView in subView.subviews) {
            if ([subSubView isKindOfClass:[UITextField class]]) {
                return (UITextField *) subSubView;
            }
        }
    }
    return nil;
}

#pragma mark - UISearchBar Delegate Methods

- (void)searchBarCancelButtonClicked:(UISearchBar *)searchBar {
    searchBar.text=@"";
    [searchBar resignFirstResponder];
    [self hideInternal];
}

-(void)searchBarSearchButtonClicked:(UISearchBar *)searchBar {
    [self search];
    [searchBar resignFirstResponder];
}

-(void)searchBar:(UISearchBar *)searchBar textDidChange:(NSString *)searchText
{
    SEL fetchSearchResults = @selector(search); //call the search function
    [[self class] cancelPreviousPerformRequestsWithTarget:self selector:fetchSearchResults object:nil];
    [self performSelector:fetchSearchResults withObject:nil afterDelay:1];
    
}

-(void)search{
    [self writeJavascript:[NSString stringWithFormat:@"cordova.fireDocumentEvent('searchEvent', {text:'%@'})", nativeSearchBar.text]];
}

@end
