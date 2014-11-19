using MediaBrowser.Mobile.Common.Localization;
using MediaBrowser.Mobile.Master;
using MediaBrowser.Mobile.Startup;
using MediaBrowser.Model.ApiClient;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Extensions
{
    public static class PageExtensions
    {
        public static void ShowGeneralErrorMessage(this Page page)
        {
            page.DisplayAlert("Error", "There was an error processing your request. Please try again later.", "Ok");
        }

        public static void ShowUnauthorizedMessage(this Page page)
        {
            page.DisplayAlert("Login Failure", "Invalid username or password. Please try again.", "Back");
        }
        
        public static async Task ProcessConnectionResult(this Page page, ConnectionResult result, MasterPage master)
        {
            // TODO: Move to view model?
            if (result.State == ConnectionState.ServerSelection)
            {
                if (result.Servers.Count == 0)
                {
                    await page.Navigation.PushAsync(new ServerEntryPage(master));
                }
                else
                {
                    await page.Navigation.PushAsync(new ServerSelectionPage(master));
                }
            }
            else if (result.State == ConnectionState.ConnectSignIn)
            {
                await page.Navigation.PushAsync(new ConnectPage(master));
            }
            else if (result.State == ConnectionState.ServerSignIn)
            {
                await page.Navigation.PushAsync(new ServerSignInPage(result.Servers[0], result.ApiClient, master));
            }
            else if (result.State == ConnectionState.SignedIn)
            {
                master.OnStartupFlowComplete();
            }
            else
            {
                throw new Exception("Unexpected Result.State");
            }
        }

        public static string GetLocalizedString(this Page page, string phrase)
        {
            var localization = Resolver.Resolve<ILocalizationManager>();

            return localization.GetLocalizedString(phrase);
        }
    }
}
