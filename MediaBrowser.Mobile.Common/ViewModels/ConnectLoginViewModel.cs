using MediaBrowser.Model.ApiClient;
using System.Threading;
using System.Threading.Tasks;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Common.ViewModels
{
    public class ConnectLoginViewModel : BaseViewModel
    {
        public async Task<ConnectionResult> Login(string username, string password)
        {
            var connectionManager = Resolver.Resolve<IConnectionManager>();

            IsBusy = true;

            try
            {
                var result = await connectionManager.Connect(CancellationToken.None);

                if (result.State == ConnectionState.ConnectSignIn)
                {
                    result.State = ConnectionState.ServerSelection;
                }

                return result;
            }
            finally
            {
                IsBusy = false;
            }
        }
    }
}
