using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MediaBrowser.Mobile.Startup;
using MediaBrowser.Model.ApiClient;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Extensions
{
    public static class PageExtensions
    {
        public static void ShowGeneralErrorMessage(this Page page)
        {
            page.DisplayAlert("Error", "There was an error processing your request. Please try again later.", "Ok");
        }

        public static async Task ProcessConnectionResult(this Page page, ConnectionResult result)
        {
            // TODO: Move to view model?
            if (result.State == ConnectionState.ServerSelection)
            {
                await page.Navigation.PushAsync(new ServerSelectionPage());
            }
            else if (result.State == ConnectionState.ServerSignIn)
            {
            }
            else if (result.State == ConnectionState.SignedIn)
            {
            }
            else
            {
            }
        }

    }
}
