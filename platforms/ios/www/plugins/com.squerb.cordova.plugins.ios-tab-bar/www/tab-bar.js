cordova.define("com.squerb.cordova.plugins.ios-tab-bar.TabBar", function(require, exports, module) { // JS ::::::::

/*
 //  This code is adapted from the work of:
 //  Created by Michael Nachbaur on 13/04/09.
 //  Copyright 2009 Decaf Ninja Software. All rights reserved.
 //  MIT licensed
 */

// This is installed as a <js-module /> so it doesn't have a cordova.define wrapper
var exec = require( 'cordova/exec' );

/**
 * This class exposes mobile phone interface controls to JavaScript, such as
 * native tab and tool bars, etc.
 * @constructor
 */
function TabBar() {
    var self = this;
    this.serviceName = 'TabBar';

    this.tabBarTag = 0;
    this.toolBarIndexes = 0;
    this.tabBarItems = [];
    this.nameTags = {};

    this.tabBarCallbacks = {};
    this.selectedTabBarItem = null;

    exec( function ( tab ) { self.itemSelected( tab ); }, function () {}, this.serviceName, 'bindListener', [] );
}

/**
 * Show a tab bar.  The tab bar has to be created first.
 * @param {Object} [options] Options indicating how the tab bar should be shown:
 * - \c height integer indicating the height of the tab bar (default: \c 49)
 * - \c position specifies whether the tab bar will be placed at the \c top or \c bottom of the screen (default: \c bottom)
 */
TabBar.prototype.show = function ( options ) {
    if ( !options ) options = {'position': 'bottom'};
    exec( null, null, this.serviceName, "showTabBar", [ options ] );
};

/**
 * Hide a tab bar.  The tab bar has to be created first.
 */
TabBar.prototype.hide = function ( animate ) {
    if ( animate == undefined || animate == null ) animate = true;
    exec( null, null, this.serviceName, "hideTabBar", [ { animate: animate } ] );
};

/**
 * Create a new tab bar item for use on a previously created tab bar.  Use ::showTabBarItems to show the new item on the tab bar.
 *
 * If the supplied image name is one of the labels listed below, then this method will construct a tab button
 * using the standard system buttons.  Note that if you use one of the system images, that the \c title you supply will be ignored.
 *
 * <b>Tab Buttons</b>
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
 * @param {String} name internal name to refer to this tab by
 * @param {String} [label] title text to show on the tab, or null if no text should be shown
 * @param {String} [image] image filename or internal identifier to show, or null if no image should be shown
 * @param {Object} [options] Options for customizing the individual tab item
 *  - \c badge value to display in the optional circular badge on the item; if null or unspecified, the badge will be hidden
 */
               TabBar.prototype.createItem = function ( name, label, image, onselect, options ) {
               var tag = this.tabBarTag++;
               if ( onselect ) {
               this.tabBarCallbacks[tag] = {'onSelect': onselect, 'name': name};
               }
               this.tabBarItems.push(name);
               this.nameTags[name] = tag;
               exec( null, null, this.serviceName, "createTabBarItem", [ name, label, image, tag, options ] );
               };

/**
 * Update an existing tab bar item to change its badge value.
 * @param {String} name internal name used to represent this item when it was created
 * @param {Object} options Options for customizing the individual tab item
 *  - \c badge value to display in the optional circular badge on the item; if null or unspecified, the badge will be hidden
 */
TabBar.prototype.updateItem = function ( name, options ) {
    if ( !options ) options = {};
    exec( null, null, this.serviceName, "updateTabBarItem", [ name, options ] );
};

/**
 * Show previously created items on the tab bar
 * @param {Object} [options] dictionary of options, notable options including:
 *  - \c animate indicates that the items should animate onto the tab bar
 * @see createTabBarItem
 * @see createTabBar
 */
TabBar.prototype.showItems = function (options) {
  exec( null, null, this.serviceName, "showTabBarItems", [this.tabBarItems, options] );
};

/**
 * Show specific items on the tab bar
 * @param {[String]} names array of names to display
 * @param {Object} options Options for customizing the individual tab item
 *  - \c badge value to display in the optional circular badge on the item; if null or unspecified, the badge will be hidden
 */
TabBar.prototype.showNamedItems = function (names, options) {
  exec( null, null, this.serviceName, "showTabBarItems", [names, options]);
};

/**
 * Function to detect currently selected tab bar item
 * @see createTabBarItem
 * @see showTabBarItems
 */
TabBar.prototype.getSelectedItem = function () {
    return this.selectedTabBarItem;
};

/**
 * Manually select an individual tab bar item, or nil for deselecting a currently selected tab bar item.
 * @param {String} tabName the name of the tab to select, or null if all tabs should be deselected
 * @see createTabBarItem
 * @see showTabBarItems
 */
TabBar.prototype.selectItem = function ( tab ) {
    exec( null, null, this.serviceName, "selectTabBarItem", [ tab ] );
    this.itemSelected(this.nameTags[tab]);
};

/**
 * Function called when a tab bar item has been selected.
 * @param {Number} tag the tag number for the item that has been selected
 */
TabBar.prototype.itemSelected = function ( tag ) {
    this.selectedTabBarItem = tag;
    if ( typeof(this.tabBarCallbacks[tag].onSelect) == 'function' )
        this.tabBarCallbacks[tag].onSelect( this.tabBarCallbacks[tag].name );
};

module.exports = new TabBar();

});
