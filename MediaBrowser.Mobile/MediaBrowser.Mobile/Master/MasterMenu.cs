using System.Threading.Tasks;
using MediaBrowser.Mobile.Common.ViewModels;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Master
{
    public class MasterMenu : ContentPage
    {
        private bool _needsRefresh;
        private SessionViewModel _viewModel;

        public MasterMenu(SessionViewModel viewModel)
        {
            Title = "Media Browser";

            BindingContext = viewModel;

            this.SetBinding(TitleProperty, SessionViewModel.UsernamePropertyName);

            _needsRefresh = true;
            _viewModel = viewModel;
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();

            if (_needsRefresh)
            {
                await RefreshContent();
            }
        }

        private async Task RefreshContent()
        {
            
        }
    }
}
