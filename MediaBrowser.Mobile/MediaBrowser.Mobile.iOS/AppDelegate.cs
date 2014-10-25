using MediaBrowser.ApiInteraction;
using MediaBrowser.Mobile.Api;
using MediaBrowser.Mobile.Common.ApiClient;
using MediaBrowser.Mobile.Common.Networking;
using MediaBrowser.Mobile.iOS.Api;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Entities;
using MediaBrowser.Model.Logging;
using MediaBrowser.Model.Session;
using MonoTouch.Foundation;
using MonoTouch.UIKit;
using Refractored.Xam.Settings;
using Refractored.Xam.Settings.Abstractions;
using System.Collections.Generic;
using System.Linq;
using Xamarin.Forms;
using Xamarin.Forms.Labs;
using Xamarin.Forms.Labs.iOS;
using Xamarin.Forms.Labs.Services;
using Device = MediaBrowser.Mobile.Common.ApiClient.Device;
using IDevice = Xamarin.Forms.Labs.IDevice;

namespace MediaBrowser.Mobile.iOS
{
    // The UIApplicationDelegate for the application. This class is responsible for launching the 
    // User Interface of the application, as well as listening (and optionally responding) to 
    // application events from iOS.
    [Register("AppDelegate")]
    public partial class AppDelegate : XFormsApplicationDelegate
    {
        // class-level declarations
        UIWindow window;

        //
        // This method is invoked when the application has loaded and is ready to run. In this 
        // method you should instantiate the window, load the UI into it and then make the window
        // visible.
        //
        // You have 17 seconds to return from this method, or iOS will terminate your application.
        //
        public override bool FinishedLaunching(UIApplication app, NSDictionary options)
        {
            Forms.Init();

            ConfigureResolver();

            window = new UIWindow(UIScreen.MainScreen.Bounds);

            window.RootViewController = App.GetMainPage().CreateViewController();

            window.MakeKeyAndVisible();

            return true;
        }

        private void ConfigureResolver()
        {
            var resolverContainer = new SimpleContainer();

            resolverContainer

                // registers IDevice as a Func<IResolver, IDevice> where 
                // the Func returns a singleton
                // of WindowsPhoneDevice
                    .Register<IDevice>(t => AppleDevice.CurrentDevice)

                // registers IDisplay as a Func<IResolver, IDisplay> where 
                // the Func uses the IResolver instance passed to it to 
                // first resolve IDevice and then return its Display property
                    .Register<IDisplay>(t => t.Resolve<IDevice>().Display)

                // register IJsonSerializer as type of ServiceStack JSON serializer
                //    .Register<IJsonSerializer, Services.Serialization.ServiceStackV3.JsonSerializer>()

                // registers IXFormsApp to an instance of XFormsAppWP 
                // created previously with 
                // var app = new XFormsAppWP();
                //     .Register<IXFormsApp>(this)

                // register ISimpleCache to Func<IResolver, ISimpleCache> 
                // returning a new instance of SQLiteSimpleCache
                // with one of the constructor parameters being resolved by 
                // IResolver to previously registered JSON serializer
                /*   .Register<ISimpleCache>(t => new SQLiteSimpleCache(new SQLite.Net.Platform.WindowsPhone8.SQLitePlatformWP8(),
                       new SQLite.Net.SQLiteConnectionString(pathToDatabase, true), t.Resolve<IJsonSerializer>()))*/;

            resolverContainer.RegisterSingle<ISettings, Settings>();
            
            Resolver.SetResolver(resolverContainer.GetResolver());

            resolverContainer.Register(GetConnectionManager());
        }

        private IConnectionManager GetConnectionManager()
        {
            var jsonSerializer = new NewtonsoftJsonSerializer();

            var device = Resolver.Resolve<IDevice>();
            var settings = Resolver.Resolve<ISettings>();
            var wakeonlan = new WakeOnLan();

            var manager = new ConnectionManager(
                new ConsoleLogger(),
                new CredentialProvider(settings, jsonSerializer),
                new NetworkConnection(device, wakeonlan),
                new ServerLocator(jsonSerializer, UdpClientFactory),
                "Media Browser Mobile",
                GetType().Assembly.GetName().Version.ToString(),
                new Device(device),
                GetCapabilities(),
                new CryptoProvider(),
                WebSocketFactory);

            manager.JsonSerializer = jsonSerializer;

            return manager;
        }

        private ClientCapabilities GetCapabilities()
        {
            return new ClientCapabilities
            {
                SupportsContentUploading = true,
                SupportsMediaControl = true,
                PlayableMediaTypes = new List<string>
                 {
                     MediaType.Audio,
                     MediaType.Video,
                     MediaType.Photo
                 },

                SupportedCommands = new List<GeneralCommandType>
                {
                    GeneralCommandType.DisplayContent,
                    GeneralCommandType.DisplayMessage,
                    GeneralCommandType.GoHome,
                    GeneralCommandType.GoToSearch,
                    GeneralCommandType.GoToSettings,
                    GeneralCommandType.Mute,
                    GeneralCommandType.SetAudioStreamIndex,
                    GeneralCommandType.SetSubtitleStreamIndex,
                    GeneralCommandType.SetVolume,
                    GeneralCommandType.TakeScreenshot,
                    GeneralCommandType.ToggleContextMenu,
                    GeneralCommandType.ToggleFullscreen,
                    GeneralCommandType.ToggleMute,
                    GeneralCommandType.ToggleOsd,
                    GeneralCommandType.Unmute,
                    GeneralCommandType.VolumeDown,
                    GeneralCommandType.VolumeUp

                }.Select(i => i.ToString()).ToList()
            };
        }

        private IClientWebSocket WebSocketFactory()
        {
            return new ClientWebSocket();
        }

        private IUdpClient UdpClientFactory()
        {
            return new UdpService();
        }
    }
}
