using MediaBrowser.Mobile.Home;
using MediaBrowser.Mobile.Startup;
using MediaBrowser.Model.ApiClient;
using System.Threading.Tasks;
using Xamarin.Forms;

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
        
        public static async Task ProcessConnectionResult(this Page page, ConnectionResult result, MasterDetailPage master)
        {
            // TODO: Move to view model?
            if (result.State == ConnectionState.ServerSelection)
            {
                await page.Navigation.PushAsync(new ServerSelectionPage());
            }
            else if (result.State == ConnectionState.ServerSignIn)
            {
                await page.Navigation.PushAsync(new ServerSignInPage(result.Servers[0], result.ApiClient, master));
            }
            else if (result.State == ConnectionState.SignedIn)
            {
                master.Detail = new NavigationPage(new HomePage());
            }
            else
            {
            }
        }
    }
}
