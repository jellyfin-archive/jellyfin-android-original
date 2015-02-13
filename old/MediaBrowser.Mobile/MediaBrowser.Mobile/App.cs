using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using MediaBrowser.Mobile.Home;
using MediaBrowser.Mobile.Master;
using MediaBrowser.Mobile.Startup;
using MediaBrowser.Model.ApiClient;
using Xamarin.Forms;

namespace MediaBrowser.Mobile
{
    public class App
    {
        public static Page GetMainPage()
        {
            return new MasterPage();
        }
    }
}
