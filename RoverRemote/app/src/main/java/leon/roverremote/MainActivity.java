package leon.roverremote;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.concurrent.Semaphore;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    public class MovementHandler extends AsyncHttpResponseHandler {

        public Button b;

        public MovementHandler(Button button) {
            b = button;
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            ((TextView) MainActivity.this.findViewById(R.id.lastCmdOutput)).setText(new String(responseBody));
            if (b != null)
                b.setClickable(true);
            getDistance();
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            error.printStackTrace();
            if (b != null)
                b.setClickable(true);
            TextView t = ((TextView) MainActivity.this.findViewById(R.id.lastCmdOutput));
            if (t != null)
                t.setText(new String(responseBody));
        }
    }

    private AsyncHttpResponseHandler sonarHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            ((TextView) MainActivity.this.findViewById(R.id.sonarText)).setText(new String(responseBody));
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            error.printStackTrace();
            TextView t = ((TextView) MainActivity.this.findViewById(R.id.lastCmdOutput));
            if (t != null)
                t.setText(new String(responseBody));
        }
    };

    private AsyncHttpResponseHandler camHandler = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            //opts.outHeight = 400;
            Bitmap bmp = BitmapFactory.decodeByteArray(responseBody,0,responseBody.length,opts);
            ((ImageView) MainActivity.this.findViewById(R.id.camView)).setImageBitmap(bmp);
            available.release();

        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            error.printStackTrace();
            TextView t = ((TextView) MainActivity.this.findViewById(R.id.lastCmdOutput));
            if (t != null)
                t.setText(new String(responseBody));
            available.release();

        }
    };

    private final Semaphore available = new Semaphore(1, true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String address = sharedPref.getString(SettingsActivity.ROVER_ADDRESS, "192.168.0.6");
        String port = sharedPref.getString(SettingsActivity.ROVER_PORT, "5000");
        RoverRestClient.resetBaseUrl(address,port);

        getDistance();
        startCamLoop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopCamLoop();
    }

    private void stopCamLoop() {
        camThreadshouldTerminate = true;
    }

    Boolean camThreadshouldTerminate = false;
    Runnable camRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            while(!camThreadshouldTerminate){
                try {
                    available.acquire();
                    getCam();
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            camThreadshouldTerminate = false;
        }
    };

    private void startCamLoop() {
        new Thread(camRefreshRunnable).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void goForward(View view) {
        Button b = (Button) findViewById(R.id.fwd_button);
        b.setClickable(false);
        RoverRestClient.get("rc/move/fwd/1", null, new MovementHandler(b));
    }

    public void goBack(View view) {
        Button b = (Button) findViewById(R.id.back_button);
        b.setClickable(false);
        RoverRestClient.get("rc/move/back/1", null, new MovementHandler(b));
    }

    public void turnLeft(View view) {
        Button b = (Button) findViewById(R.id.left_button);
        b.setClickable(false);
        RoverRestClient.get("rc/turn/left/90", null, new MovementHandler(b));

    }

    public void turnRight(View view) {
        Button b = (Button) findViewById(R.id.right_button);
        b.setClickable(false);
        RoverRestClient.get("rc/turn/right/90", null, new MovementHandler(b));
    }

    public void identify(View view) {
        Button b = (Button) findViewById(R.id.identify_button);
        b.setClickable(false);
        RoverRestClient.get("see/what", null, new MovementHandler(b));
        getDistance();
    }

    public void showConfig(View view) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }



    public void getDistance() {
        RoverRestClient.get("sonar/ping", null, sonarHandler);
    }

    private void getCam() {
        RoverRestClient.get("cam", null, camHandler);
    }


}
