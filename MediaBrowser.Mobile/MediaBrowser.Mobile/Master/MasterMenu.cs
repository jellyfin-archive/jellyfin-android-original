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

            _needsRefresh = true;
            _viewModel = viewModel;

            Content = new Label {Text = "Menu"};
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
