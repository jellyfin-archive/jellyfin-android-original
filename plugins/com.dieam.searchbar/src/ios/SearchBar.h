#import <Cordova/CDVPlugin.h>

@interface SearchBar : CDVPlugin <UISearchBarDelegate> {
    CGRect defaultWebViewFrame; // Original frame taken up by WebView
    BOOL isShowing; // YES when the SearchBar is currently being displayed.
}

@property (nonatomic, strong) UISearchBar *nativeSearchBar;
@property (nonatomic, strong) UINavigationBar *nativeNavigationBar;

@end
