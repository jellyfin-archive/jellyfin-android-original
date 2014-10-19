using MediaBrowser.Mobile.Api;
using MediaBrowser.Model.Logging;
using System;
using System.Text;

namespace MediaBrowser.Mobile.iOS.Api
{
    public class ConsoleLogger : ILogger
    {
        public void Debug(string message, params object[] paramList)
        {
            Log(LogSeverity.Debug, message, paramList);
        }

        public void Error(string message, params object[] paramList)
        {
            Log(LogSeverity.Error, message, paramList);
        }

        public void ErrorException(string message, Exception exception, params object[] paramList)
        {
            LogException(LogSeverity.Error, message, exception, paramList);
        }

        public void Fatal(string message, params object[] paramList)
        {
            Log(LogSeverity.Fatal, message, paramList);
        }

        public void FatalException(string message, Exception exception, params object[] paramList)
        {
            LogException(LogSeverity.Fatal, message, exception, paramList);
        }

        public void Info(string message, params object[] paramList)
        {
            Log(LogSeverity.Info, message, paramList);
        }

        public void LogMultiline(string message, LogSeverity severity, StringBuilder additionalContent)
        {
            additionalContent.Insert(0, message + Environment.NewLine);

            const char tabChar = '\t';

            var text = additionalContent.ToString()
                                        .Replace(Environment.NewLine, Environment.NewLine + tabChar)
                                        .TrimEnd(tabChar);

            if (text.EndsWith(Environment.NewLine))
            {
                text = text.Substring(0, text.LastIndexOf(Environment.NewLine, StringComparison.OrdinalIgnoreCase));
            }

            Log(severity, text);
        }

        private void LogException(LogSeverity severity, string message, Exception exception, params object[] paramList)
        {
            LogHelper.LogException(this, severity, message, exception, paramList);
        }

        public void Log(LogSeverity severity, string message, params object[] paramList)
        {
            message = string.Format("{0} {1}: {2}", DateTime.Now.ToLongTimeString(), severity, message);

            Log(message, paramList);
        }

        private void Log(string msg, params object[] paramList)
        {
            Console.WriteLine(msg, paramList);
        }

        public void Warn(string message, params object[] paramList)
        {
            Log(LogSeverity.Warn, message, paramList);
        }
    }
}