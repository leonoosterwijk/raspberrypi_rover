package leon.roverremote;

import android.util.Log;

import com.loopj.android.http.*;
/**
 * Created by leon on 12/18/16.
 */

public class RoverRestClient {
    //private static final String BASE_URL = "http://192.168.0.6:5000/rover/api/v1.0/";
    private static final String API_PATH = "/rover/api/v1.0/";
    private static final String ROVER_PORT = "5000";
    private static final String ROVER_ADDRESS = "192.168.0.6";
    private static String BASE_URL = "http://" + ROVER_ADDRESS + ":" + ROVER_PORT + API_PATH;

    public static void resetBaseUrl(String address, String port){
        BASE_URL = "http://" + address + ":" + port + API_PATH;
        Log.d("RoverRestClient","Setting url to:" + BASE_URL);
    }

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.setTimeout(30*1000);
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.setTimeout(30*1000);
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
