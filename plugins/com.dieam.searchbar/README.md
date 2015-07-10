cordova-search-bar
==================

A Cordova plugin to display a native SearchBar and send the search text back to the Cordova WebView.
For **iOS**, this means a simple usage of the UISearchBar. For **Android**, this is an EditText with a colored border and added search icon (uses internal Android resource, so icon appearance may very).

Installation
------------

Follow the **Cordova guide** on plugin installation
http://cordova.apache.org/blog/releases/2013/07/23/cordova-3  
  
Use <code>cordova.searchbar</code> to call the native methods (eg. <code>cordova.searchbar.show()</code>).

**or**

**Install Manually**  
1. Copy the SearchBar.h and SearchBar.m files into your XCode project <code>/Plugins</code> directory.  
2. Copy searchbar.js into your XCode project <code>/www</code> directory (can place anywhere in this directory, eg. <code>www/js</code>).  
3. Add the following to the config.xml in your root XCode project directory:
        ```  
        <feature name="SearchBar">  
           <param name="ios-package" value="SearchBar" /> 
        </feature>
       ```  
4. Import searchbar.js wherever needed in your HTML (path being wherever you placed the file in step 2).  
5. Use the <code>SearchBar</code> variable to call the defined methods (Manual install only. Plugman install maps this variable to <code>cordova.searchbar</code>).


Methods
-------

<code>show()</code>  
Slides-in the native UISearchBar onto the top of the screen.

<code>hide()</code>  
Slides-out the native UISearchBar.

<code>showNavigation()</code>  
*iOS Only*  
Slides-in the native UISearchBar within UINavigationBar. Will look the same as ````show()```` until ````push()```` is called (see below), which will add a native "Back" button to the left of the UISearchBar. DO NOT USE CONCURRENTLY WITH ````show()````/````hide()````.

<code>hideNavigation()</code>  
*iOS Only*  
Slides-out the native UISearchBar & UINavigationBar. DO NOT USE CONCURRENTLY WITH ````show()````/````hide()````.

<code>push()</code>  
*iOS Only*  
Pushes another navigation item onto the stack. Call after navigating to a new page in the WebView. After being called the first time, a back button will appear. If the back button is clicked, the Cordova WebView wil perform go back a page.
DO NOT USE CONCURRENTLY WITH ````show()````/````hide()````.

<code>pop()</code>  
*iOS Only*  
Removes the top navigation item off of the stack. If the last item is removed, the back button will disappear.
DO NOT USE CONCURRENTLY WITH ````show()````/````hide()````.

Handling User Input
-------------------

When a user enters a search term into the UISearchBar and presses the native "Search" button, the native code looks to fire a JavaScript event named **searchEvent**. 
The native code will send an Object with one key/value pair of key "text" (value is the search String entered by the user) to the function associated with this event.  
**You must add a JavaScript event listener with the name 'searchEvent' in order to retrieve the search value.**  
  
Example:

````
function mySearchEventFunction(data) {
    // Your logic here
    alert("You searched: " + data.text);  
}  
document.addEventListener('searchEvent', mySearchEventFunction, true);
````

Customization
-------------
**iOS**  
You may customize the native UISearchBar functionality in any way by editing the SearchBar.h and SearchBar.m files.  
SearchBar.m comes with the following constants allowing for quick and easy customization.  

<code>double ANIMATION_DURATION</code>  
Duration of the slide animations. Increase the value to slow the animation.  

<code>BOOL RESIZE_WEBVIEW</code>  
Determines if Cordova WebView should "shrink" underneith the SearchBar.  
Default is <code>YES</code>, Set to <code>NO</code> to overlay SearchBar atop the WebView.  

<code>BOOL OFFSET_IOS7</code>  
Determines if we should offset the top of the view when device is at or above iOS7.  
Default is <code>YES</code>, Set to <code>NO</code> to use fullscreen.  

<code>BOOL ALLOW_EMPTY_SEARCH</code>  
Determines if user is allowed to hit the "Search" button if they have NOT entered data.  
Default is <code>YES</code>, Set to <code>NO</code> to ensure user has entered at least one character before search.  


**Android**  
You may customize the native EditText functionality in any way by editing the SearchBar.java Class.  

