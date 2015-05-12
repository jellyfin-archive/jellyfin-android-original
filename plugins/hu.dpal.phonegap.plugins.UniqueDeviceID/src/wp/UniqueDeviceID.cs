using System;
using Windows.Phone.System.Analytics;
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;

namespace WPCordovaClassLib.Cordova.Commands
{
    public class UniqueDeviceID : BaseCommand
    {
        public void get(string options)
        {

            string uuid = Windows.Phone.System.Analytics.HostInformation.PublisherHostId;
            uuid = uuid.Substring(0, 32);

            DispatchCommandResult(new PluginResult(PluginResult.Status.OK, uuid));

        }
    }
}