using MediaBrowser.Model.Logging;
using System;
using System.Text;

namespace MediaBrowser.Mobile.Api
{
    public static class LogHelper
    {
        public static void LogException(ILogger logger, LogSeverity severity, string message, Exception exception, params object[] paramList)
        {
            message = LogHelper.FormatMessage(message, paramList).Replace(Environment.NewLine, ". ");

            var messageText = LogHelper.GetLogMessage(exception);

            logger.LogMultiline(message, severity, messageText);
        }

        private static string FormatMessage(string message, params object[] paramList)
        {
            if (paramList != null)
            {
                for (var i = 0; i < paramList.Length; i++)
                {
                    message = message.Replace("{" + i + "}", paramList[i].ToString());
                }
            }

            return message;
        }

        /// <summary>
        /// Gets the log message.
        /// </summary>
        /// <param name="exception">The exception.</param>
        /// <returns>StringBuilder.</returns>
        private static StringBuilder GetLogMessage(Exception exception)
        {
            var messageText = new StringBuilder();

            messageText.AppendLine(exception.Message);

            messageText.AppendLine(exception.GetType().FullName);

            LogExceptionData(messageText, exception);

            messageText.AppendLine(exception.StackTrace ?? "No Stack Trace Available");

            // Log the InnerExceptions, if any
            AppendInnerExceptions(messageText, exception);

            messageText.AppendLine(string.Empty);

            return messageText;
        }

        /// <summary>
        /// Appends the inner exceptions.
        /// </summary>
        /// <param name="messageText">The message text.</param>
        /// <param name="e">The e.</param>
        private static void AppendInnerExceptions(StringBuilder messageText, Exception e)
        {
            var aggregate = e as AggregateException;

            if (aggregate != null && aggregate.InnerExceptions != null)
            {
                foreach (var ex in aggregate.InnerExceptions)
                {
                    AppendInnerException(messageText, ex);
                    AppendInnerExceptions(messageText, ex);
                }
            }

            else if (e.InnerException != null)
            {
                AppendInnerException(messageText, e.InnerException);
                AppendInnerExceptions(messageText, e.InnerException);
            }
        }

        /// <summary>
        /// Appends the inner exception.
        /// </summary>
        /// <param name="messageText">The message text.</param>
        /// <param name="e">The e.</param>
        private static void AppendInnerException(StringBuilder messageText, Exception e)
        {
            messageText.AppendLine("InnerException: " + e.GetType().FullName);
            messageText.AppendLine(e.Message);

            LogExceptionData(messageText, e);

            if (e.StackTrace != null)
            {
                messageText.AppendLine(e.StackTrace);
            }
        }

        /// <summary>
        /// Logs the exception data.
        /// </summary>
        /// <param name="messageText">The message text.</param>
        /// <param name="e">The e.</param>
        private static void LogExceptionData(StringBuilder messageText, Exception e)
        {
            foreach (var key in e.Data.Keys)
            {
                messageText.AppendLine(key + ": " + e.Data[key]);
            }
        }
    }
}
