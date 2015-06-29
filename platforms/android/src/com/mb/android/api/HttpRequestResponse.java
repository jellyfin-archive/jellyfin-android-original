package com.mb.android.api;

import com.mb.android.webviews.IWebView;

import java.util.Map;

import mediabrowser.apiinteraction.Response;
import mediabrowser.model.net.HttpException;
import mediabrowser.model.serialization.IJsonSerializer;

/**
 * Created by Luke on 6/27/2015.
 */
public class HttpRequestResponse extends Response<String> {

    private IJsonSerializer jsonSerializer;
    private IWebView webView;
    private String requestId;

    public HttpRequestResponse(IJsonSerializer jsonSerializer, IWebView webView, String requestId) {
        this.jsonSerializer = jsonSerializer;
        this.webView = webView;
        this.requestId = requestId;
    }

    @Override
    public void onResponse(String response){

        if (response == null || response.length() == 0){
            response = "null";
        }

        String js = String.format("AndroidAjax.onResponse('%s', %s, %s);", requestId, 200, response);
        RespondToWebView(js);
    }

    @Override
    public void onError(Exception ex){

        String jsResponse;

        String statusCodeString = "null";

        if (ex instanceof HttpException){
            HttpException httpError = (HttpException)ex;

            HttpRequestWebViewResponse response = new HttpRequestWebViewResponse();

            if (httpError.getIsTimedOut()){

            }
            else{

                if (httpError.getStatusCode() != null) {
                    response.setStatusCode(httpError.getStatusCode());
                    statusCodeString = String.valueOf(httpError.getStatusCode());
                }

                response.setResponseHeaders(httpError.getHeaders());
            }

            String responseJson = jsonSerializer.SerializeToString(response);

            jsResponse = String.format("AndroidAjax.onError('%s', %s, %s);", requestId, statusCodeString, responseJson);
        }
        else{
            HttpRequestWebViewResponse response = new HttpRequestWebViewResponse();
            String responseJson = jsonSerializer.SerializeToString(response);

            jsResponse = String.format("AndroidAjax.onError('%s', %s, %s);", requestId, statusCodeString, responseJson);
        }

        RespondToWebView(jsResponse);
    }

    private class HttpRequestWebViewResponse{

        private Integer StatusCode;
        public Integer getStatusCode() {
            return StatusCode;
        }
        public void setStatusCode(Integer statusCode){
            StatusCode = statusCode;
        }

        private Map<String,String> ResponseHeaders;
        public Map<String,String> getResponseHeaders() {
            return ResponseHeaders;
        }
        public void setResponseHeaders(Map<String,String> responseHeaders){
            ResponseHeaders = responseHeaders;
        }
    }

    private void RespondToWebView(final String js) {

        //logger.Info("Sending url to webView: %s", js);
        webView.sendJavaScript(js);
    }
}
