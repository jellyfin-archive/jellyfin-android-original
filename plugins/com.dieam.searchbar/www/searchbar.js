// Find the cordova reference used in the currently running app (differs based on version)
var cordovaRef =  window.cordova || window.Cordova || window.PhoneGap;

// Define our SeachBar Object
// You can call the functions within the SearchBar at any time by calling SearchBar.FUNCTION_NAME();
var SearchBar = {


    // show() - Used to initially display the search bar. Automatically adds an offset for iOS7 devices.
    show: function() {
        cordovaRef.exec(null, null, 'SearchBar', 'show', []);
    },
    
    // hide() - Used to remove the currently displaying search bar. Automatically adds an offset for iOS7 devices.
    hide: function() {
        cordovaRef.exec(null, null, 'SearchBar', 'hide', []);
    },
    
    // showNavigation() - used to display the search bar embedded with a navigation bar containing one button.
    showNavigation: function() {
        cordovaRef.exec(null, null, 'SearchBar', 'showNavigation', []);
    },

    // hideNavigation() - Hides the search bar and its parent narvigation bar.
    hideNavigation: function() {
        cordovaRef.exec(null, null, 'SearchBar', 'showNavigation', []);
    },
    
    // Pushes another navigation item onto the stack. After being called the first time, a back button will appear. 
    push: function() {
        cordovaRef.exec(null, null, 'SearchBar', 'pushNavigation', []);
    },

    // Removes the top navigation item off of the stack. If the last item is removed, the back button will disappear.
    pop: function() {
        cordovaRef.exec(null, null, 'SearchBar', 'pushNavigation', []);
    }
    
}

module.exports = SearchBar;
