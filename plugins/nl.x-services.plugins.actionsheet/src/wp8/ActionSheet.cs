using Microsoft.Phone.Tasks;
using Microsoft.Phone.Controls;
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;
using System.Runtime.Serialization;
using System;
using System.Windows;
using System.Windows.Media;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using WPCordovaClassLib;

namespace Cordova.Extension.Commands
{
    public class ActionSheet : BaseCommand
    {

        [DataContract]
        public class ActionSheetOptions
        {
            [DataMember(IsRequired = false, Name = "buttonLabels")]
            public string[] buttonLabels { get; set; }

            [DataMember(IsRequired = false, Name = "title")]
            public string title { get; set; }

            [DataMember(IsRequired = false, Name = "addCancelButtonWithLabel")]
            public string addCancelButtonWithLabel { get; set; }

            [DataMember(IsRequired = false, Name = "addDestructiveButtonWithLabel")]
            public string addDestructiveButtonWithLabel { get; set; }

            [DataMember(IsRequired = false, Name = "winphoneEnableCancelButton")]
            public bool winphoneEnableCancelButton { get; set; }
        }

        private ActionSheetOptions actionSheetOptions = null;

        private Popup popup = new Popup();

        private Brush darkBrush = new SolidColorBrush(Color.FromArgb(250, 40, 40, 40));

        public void show(string options)
        {
            try
            {
                String jsonOptions = JsonHelper.Deserialize<string[]>(options)[0];
                actionSheetOptions = JsonHelper.Deserialize<ActionSheetOptions>(jsonOptions);
            }
            catch (Exception)
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
                return;
            }

            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                // attach a backbutton listener to the view and dim it a bit
                CordovaView cView = getCordovaView();
                cView.Browser.Dispatcher.BeginInvoke(() =>
                {
                    cView.Browser.InvokeScript("execScript", "document.addEventListener('backbutton', window.plugins.actionsheet.hide, false)");
                    cView.Browser.Opacity = 0.5d;
                });

                Border border = new Border();
                border.Width = Application.Current.Host.Content.ActualWidth;
                border.Background = darkBrush;
                border.Padding = new Thickness(10, 10, 10, 10);


                // container for the buttons
                StackPanel panel = new StackPanel();
                panel.HorizontalAlignment = HorizontalAlignment.Stretch;
                panel.VerticalAlignment = VerticalAlignment.Center;
                panel.Width = Application.Current.Host.Content.ActualWidth - 60;


                // title
                if (actionSheetOptions.title != null)
                {
                    TextBlock textblock1 = new TextBlock();
                    textblock1.Text = actionSheetOptions.title;
                    textblock1.TextWrapping = TextWrapping.Wrap;
                    textblock1.Margin = new Thickness(20, 10, 20, 0); // left, top, right, bottom
                    textblock1.FontSize = 22;
                    textblock1.Foreground = new SolidColorBrush(Colors.White);
                    panel.Children.Add(textblock1);
                }

                int buttonIndex = 1;

                // desctructive button
                if (actionSheetOptions.addDestructiveButtonWithLabel != null)
                {
                    Button button = new Button();
                    button.TabIndex = buttonIndex++;
                    button.Content = actionSheetOptions.addDestructiveButtonWithLabel;
                    button.Background = darkBrush; // new SolidColorBrush(Colors.White);
                    button.Foreground = new SolidColorBrush(Color.FromArgb(255, 255, 69, 0));
                    button.Padding = new Thickness(10);
                    button.Margin = new Thickness(5);
                    button.Click += new RoutedEventHandler(buttonClickListener);
                    panel.Children.Add(button);
                }


                // regular buttons
                if (actionSheetOptions.buttonLabels != null)
                {
                    foreach (String buttonLabel in actionSheetOptions.buttonLabels)
                    {
                        Button button = new Button();
                        button.TabIndex = buttonIndex++;
                        button.Content = buttonLabel;
                        button.Background = darkBrush;
                        button.Foreground = new SolidColorBrush(Colors.White);
                        button.Padding = new Thickness(10);
                        button.Margin = new Thickness(5);
                        button.Click += new RoutedEventHandler(buttonClickListener);
                        panel.Children.Add(button);
                    }
                }

                // cancel button
                if (actionSheetOptions.winphoneEnableCancelButton && actionSheetOptions.addCancelButtonWithLabel != null)
                {
                    Button button = new Button();
                    button.HorizontalAlignment = HorizontalAlignment.Left;
                    button.TabIndex = buttonIndex++;
                    button.Content = actionSheetOptions.addCancelButtonWithLabel;
                    button.Padding = new Thickness(50, 10, 50, 10);
                    button.Margin = new Thickness(5, 0, 20, 5);
                    button.FontSize = 17;
                    button.Background = darkBrush;
                    button.Foreground = new SolidColorBrush(Colors.White);

                    button.Click += new RoutedEventHandler(buttonClickListener);
                    panel.Children.Add(button);

                }

                border.Child = panel;
                popup.Child = border;

                // Set where the popup will show up on the screen.
                popup.VerticalOffset = 30;

                // Open the popup.
                popup.IsOpen = true;
            });
        }

        void buttonClickListener(object sender, RoutedEventArgs e)
        {
            // Close the popup
            hide(null);

            // Get the clicked button index
            Button button = (Button)sender;
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK, button.TabIndex));
        }

        public void hide(string options)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                // remove the backbutton listener from the view and undim it
                CordovaView cView = getCordovaView();
                getCordovaView().Browser.Dispatcher.BeginInvoke(() =>
                {
                    cView.Browser.InvokeScript("execScript", "document.removeEventListener('backbutton', window.plugins.actionsheet.hide, false)");
                    cView.Browser.Opacity = 1d;
                });

                if (popup.IsOpen)
                {
                    popup.IsOpen = false;
                }
            });
        }

        // note: needs to be invoked from within Deployment.Current.Dispatcher.BeginInvoke ..
        private CordovaView getCordovaView()
        {
            PhoneApplicationFrame frame = (PhoneApplicationFrame)Application.Current.RootVisual;
            PhoneApplicationPage page = (PhoneApplicationPage)frame.Content;
            return (CordovaView)page.FindName("CordovaView");
        }
    }
}
