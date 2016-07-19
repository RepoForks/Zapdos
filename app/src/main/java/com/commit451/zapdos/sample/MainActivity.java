package com.commit451.zapdos.sample;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.commit451.zapdos.Zapdos;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener{

    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private static final String FILE_NAME = "some_file_name.txt";
    private EditText mMessage;

    private GoogleApiClient mGoogleApiClient;
    private SampleDrive mSampleDrive;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        loadMessage();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, connectionResult.getErrorCode(), 0).show();
            return;
        }
        try {
            connectionResult.startResolutionForResult(MainActivity.this, REQUEST_RESOLVE_ERROR);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Zapdos zapdos = new Zapdos.Builder(mGoogleApiClient)
                .baseScope(Drive.SCOPE_APPFOLDER)
                .addConverterFactory(CustomConverterFactory.create(mGoogleApiClient))
                .build();
        mSampleDrive = zapdos.create(SampleDrive.class);

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message(mMessage.getText().toString());
                mSampleDrive.writeMessage(FILE_NAME, message)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Message>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                                e.printStackTrace();
                            }

                            @Override
                            public void onNext(Message message) {
                                Toast.makeText(MainActivity.this, "Message saved", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        mMessage = (EditText) findViewById(R.id.message);
        mGoogleApiClient.connect();
    }

    private void loadMessage() {
        mSampleDrive.getMessage(FILE_NAME)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Message>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Message message) {
                        mMessage.setText(message.text);
                    }
                });
    }
}
