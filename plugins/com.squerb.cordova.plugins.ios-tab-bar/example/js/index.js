/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
  // Application Constructor
  initialize: function() {
      this.bindEvents();
  },
  // Bind Event Listeners
  //
  // Bind any events that are required on startup. Common events are:
  // 'load', 'deviceready', 'offline', and 'online'.
  bindEvents: function() {
    document.addEventListener('deviceready', this.onDeviceReady, false);

    document.getElementById("showTabBar").onclick = this.showTabBar;
    document.getElementById("showTabBarItems").onclick = this.showTabBarItems;
    document.getElementById("hideTabBar").onclick = this.hideTabBar;
    document.getElementById("selectSearch").onclick = this.selectSearch;
  },
  setUpTabBar: function() {
    // Use system defined items for this demo
    // If an image is passed, label is not used
    var items = [
      {name: 'Featured',  label: 'label', image: 'tabButton:Featured',  options: {} },
      {name: 'Search',    label: 'label', image: 'tabButton:Search',    options: {} },
      {name: 'History',   label: 'label', image: 'tabButton:History',   options: {} },
      {name: 'Bookmarks', label: 'label', image: 'tabButton:Bookmarks', options: {badge: '3'} }
    ];

    var fn = function(name) {
      var msg = "Selected " + name;
      console.log(msg);
      alert(msg);
    };

    for (var i=0; i < items.length; i++) {
      var item = items[i];
      var options = item.options;
      // set the function to invoke when the item is selected
      options.onSelect = fn;
      TabBar.createItem(item.name, item.label, item.image, item.options);
    };

    TabBar.showItems();
    TabBar.show();
  },
  showTabBar: function() {
    TabBar.show();
  },
  showTabBarNamedItems: function() {
    var names = ['Featured', 'Bookmarks'];
    TabBar.showNamedItems(names, {animate: 'YES'});
  },
  hideTabBar: function() {
    TabBar.hide();
  },
  selectSearch: function() {
    TabBar.selectItem('Search');
  },
  // deviceready Event Handler
  //
  // The scope of 'this' is the event. In order to call the 'receivedEvent'
  // function, we must explicitly call 'app.receivedEvent(...);'
  onDeviceReady: function() {
      app.receivedEvent('deviceready');
      app.setUpTabBar();
  },
  // Update DOM on a Received Event
  receivedEvent: function(id) {
      var parentElement = document.getElementById(id);
      var listeningElement = parentElement.querySelector('.listening');
      var receivedElement = parentElement.querySelector('.received');

      listeningElement.setAttribute('style', 'display:none;');
      receivedElement.setAttribute('style', 'display:block;');

      console.log('Received Event: ' + id);
  }
};

app.initialize();
