using Android.App;
using Android.Content.PM;
using Android.OS;
using MediaBrowser.ApiInteraction;
using MediaBrowser.Mobile.Api;
using MediaBrowser.Mobile.Common.ApiClient;
using MediaBrowser.Mobile.Common.Localization;
using MediaBrowser.Mobile.Common.Networking;
using MediaBrowser.Mobile.Droid.Api;
using MediaBrowser.Mobile.Droid.Localization;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Entities;
using MediaBrowser.Model.Logging;
using MediaBrowser.Model.Serialization;
using MediaBrowser.Model.Session;
using Refractored.Xam.Settings;
using Refractored.Xam.Settings.Abstractions;
using System.Collections.Generic;
using System.Linq;
using Xamarin.Forms.Labs;
using Xamarin.Forms.Labs.Droid;
using Xamarin.Forms.Labs.Services;
using IDevice = Xamarin.Forms.Labs.IDevice;

namespace MediaBrowser.Mobile.Droid
{
    [Activity(Label = "MediaBrowser.Mobile", MainLauncher = true, ConfigurationChanges = ConfigChanges.ScreenSize | ConfigChanges.Orientation)]
    public class MainActivity : XFormsApplicationDroid
    {
        protected override void OnCreate(Bundle bundle)
        {
            base.OnCreate(bundle);

            ConfigureResolver();

            Xamarin.Forms.Forms.Init(this, bundle);

            SetPage(App.GetMainPage());
        }

        private void ConfigureResolver()
        {
            var resolverContainer = new SimpleContainer();

            resolverContainer

                // registers IDevice as a Func<IResolver, IDevice> where 
                // the Func returns a singleton
                // of WindowsPhoneDevice
                    .Register<IDevice>(t => AndroidDevice.CurrentDevice)

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
            
            resolverContainer.Register(GetLocalizationManager());
            resolverContainer.Register(GetConnectionManager());
        }

        private ILocalizationManager GetLocalizationManager()
        {
            return new LocalizationManager(GetJsonSerializer());
        }

        private IConnectionManager GetConnectionManager()
        {
            var jsonSerializer = GetJsonSerializer();

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

        private IJsonSerializer GetJsonSerializer()
        {
            return new NewtonsoftJsonSerializer();
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

