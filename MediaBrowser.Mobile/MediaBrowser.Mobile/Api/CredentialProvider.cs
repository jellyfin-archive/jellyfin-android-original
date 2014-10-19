using MediaBrowser.ApiInteraction;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Serialization;
using Refractored.Xam.Settings.Abstractions;
using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Api
{
    public class CredentialProvider : ICredentialProvider
    {
        private readonly IJsonSerializer _json;
        private readonly ISettings _settings;

        public CredentialProvider(ISettings settings, IJsonSerializer json)
        {
            _json = json;
            _settings = settings;
        }

        public Task<ServerCredentials> GetServerCredentials()
        {
            var val = _settings.GetValueOrDefault<string>("servers");

            ServerCredentials credentials;

            if (string.IsNullOrWhiteSpace(val))
            {
                credentials = new ServerCredentials();
            }
            else
            {
                credentials = _json.DeserializeFromString<ServerCredentials>(val);
            }

            return Task.FromResult(credentials);
        }

        public Task SaveServerCredentials(ServerCredentials configuration)
        {
            var json = _json.SerializeToString(configuration);

            _settings.AddOrUpdateValue("servers", json);
            _settings.Save();

            return Task.FromResult(true);
        }
    }
}
