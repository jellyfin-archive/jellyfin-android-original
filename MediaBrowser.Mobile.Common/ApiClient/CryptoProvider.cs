using MediaBrowser.ApiInteraction.Cryptography;
using PCLCrypto;

namespace MediaBrowser.Mobile.Api
{
    public class CryptoProvider : ICryptographyProvider
    {
        public byte[] CreateMD5(byte[] value)
        {
            var hasher = WinRTCrypto.HashAlgorithmProvider.OpenAlgorithm(HashAlgorithm.Md5);
            return hasher.HashData(value);
        }

        public byte[] CreateSha1(byte[] value)
        {
            var hasher = WinRTCrypto.HashAlgorithmProvider.OpenAlgorithm(HashAlgorithm.Sha1);
            return hasher.HashData(value);
        }
    }
}
