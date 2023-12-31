package com.example.loginpage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.loginpage.constants.Environment;
import com.example.loginpage.utility.BundleDeliveryMan;
import com.example.loginpage.utility.LoadingDialogFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.api.models.FilterObject;
import io.getstream.chat.android.client.api.models.QueryChannelsRequest;
import io.getstream.chat.android.client.api.models.querysort.QuerySortByField;
import io.getstream.chat.android.client.channel.ChannelClient;
import io.getstream.chat.android.client.models.Channel;
import io.getstream.chat.android.client.models.Filters;
import io.getstream.chat.android.client.models.User;

/**
 * @author saran
 * @date 10/2/2023
 */

public class HomePage extends AppCompatActivity {
   private final ChatClient client = ChatClient.instance();
   private EditText RoomCode;
   private String LIVESTREAM;
   private final BundleDeliveryMan mDeliveryMan = BundleDeliveryMan.getInstance();
   private String api_key;
   private String userToken;
   private String uid;
   SharedPreferences mSharedPreferences;
   public static final String USER_TOKEN = "userToken";
   public static final String API_KEY = "api_key";
   public static final String UID = "uid";
   private final String sharedPrefFile = "com.example.loginpage.homepage";

   public LoadingDialogFragment loadingDialogFragment = new LoadingDialogFragment();


   public HomePage() throws MalformedURLException {
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {

      Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
      {
         @Override
         public void uncaughtException (@NonNull Thread thread, @NonNull Throwable e)
         {
            handleUncaughtException (thread, e);
         }
      });


      super.onCreate(savedInstanceState);
      setContentView(R.layout.homepage);

      Bundle dataBundle = getIntent().getExtras();
      Button createRoomButton = findViewById(R.id.createRoom);
      Button submit = findViewById(R.id.roomSubmit);
      Button settingsButton = findViewById(R.id.settingsButton);
      Button logOut=findViewById(R.id.logOut);
      // getting saved instance data in case of abrupt finishing of activity
      if(savedInstanceState != null){
         mSharedPreferences = getSharedPreferences(sharedPrefFile,MODE_PRIVATE);
         userToken = mSharedPreferences.getString(USER_TOKEN,"");
         uid = mSharedPreferences.getString(UID,"");
         api_key = mSharedPreferences.getString(API_KEY,Environment.API_KEY);
      } else {
         userToken = dataBundle.getString(USER_TOKEN);
         uid = dataBundle.getString(UID);
         api_key = dataBundle.getString(API_KEY);
      }

      LIVESTREAM = getString(R.string.livestreamChannelType);

      createRoomButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            registerUser(uid,userToken);

         }
      });

      submit.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {

            registerUser_another(uid,userToken);
         }
      });
      settingsButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (!loadingDialogFragment.isAdded()) {
               loadingDialogFragment.show(getSupportFragmentManager(), "loader");
            }
            Intent intentSettings = new Intent(HomePage.this,SettingActivity.class);
            Bundle settingsPageBundle = mDeliveryMan.SettingsPageBundle(uid);
            intentSettings.putExtras(settingsPageBundle);
            startActivity(intentSettings);
         }
      });

      logOut.setOnClickListener(new View.OnClickListener(){
         @Override
         public void onClick(View view) {
            if (!loadingDialogFragment.isAdded()) {
               loadingDialogFragment.show(getSupportFragmentManager(), "loader");
            }
            Intent intentLogOut = new Intent(HomePage.this,MainActivity.class);
            startActivity(intentLogOut);

         }
      });
   }

   //we handle exceptions here
   public void handleUncaughtException (Thread thread, Throwable e)
   {
      Toast.makeText(getApplicationContext(),"An unexpected error has occurred.",Toast.LENGTH_LONG).show();
   }
   private void registerUser(String uid, String userToken){
      User streamUser = new User();
      streamUser.setId(uid);
      client.connectUser(
              streamUser,userToken
      ).enqueue(connectionResult->{
         if(connectionResult.isError()) {
            Log.e("HomePage","Error connecting to client."+connectionResult.error());
         } else {
            String createRoomCode=String.valueOf(randomInteger());
            startChannel(createRoomCode);
            Log.i("HomePage","successfully created a room with code:"+createRoomCode);
         }
              }
      );
   }
   private void registerUser_another(String uid, String userToken){
      User streamUser = new User();
      streamUser.setId(uid);
      client.connectUser(
              streamUser,userToken
      ).enqueue(connectionResult->{
                 if(connectionResult.isError()) {
                    Log.e("HomePage","Error connecting to client!" + connectionResult.error());
                 } else {

                    RoomCode = findViewById(R.id.roomCode);
                    String roomCode = RoomCode.getText().toString();
                    String channelId = "messageRoom"+roomCode;
                    FilterObject filter = Filters.and(
                            Filters.eq("type", "livestream"),
                            Filters.in("id", Arrays.asList(channelId))
                    );

                    int offset = 0;
                    int limit = 10;
                    QuerySortByField<Channel> sort = QuerySortByField.descByName("last_message_at");
                    int messageLimit = 0;
                    int memberLimit = 0;

                    QueryChannelsRequest request = new QueryChannelsRequest(filter, offset, limit, sort, messageLimit, memberLimit)
                            .withWatch()
                            .withState();

                    client.queryChannels(request).enqueue(result -> {
                       if (result.isSuccess()) {
                          List<Channel> channels = result.data();
                          Log.i("HomePage","Channels printed: " + channels);
                          if(channels.size()==0){
                             Log.w("TAG", "channel does not exist");
                             Toast.makeText(HomePage.this, "channel does not exist",
                                     Toast.LENGTH_SHORT).show();
                          }
                          if(channels.size()!=0){
                             startChannel(roomCode);
                          }
                       } else {
                          Log.i("HomePage", String.valueOf(result));
                       }
                    });

                 }
              }
      );
   }

   private void startChannel(String createRoomCode){
      try{
         if (!loadingDialogFragment.isAdded()) {
            loadingDialogFragment.show(getSupportFragmentManager(), "loader");
         }
         String channelId = "messageRoom"+createRoomCode;
         ExecutorService executorService = Executors.newSingleThreadExecutor();
         final Handler handler = new Handler(Looper.getMainLooper());
         // task to run on a separate thread as internet connection might fluctuate
         executorService.execute(new Runnable() {
            @Override
            public void run() {
               ChannelClient channelClient = client.channel(LIVESTREAM, channelId);
               channelClient.watch().execute();
               handler.post(new Runnable() {
                  @Override
                  public void run() {
                     startActivity(QuestionActivity.newIntent(HomePage.this,channelClient));
                  }
               });
               executorService.shutdown();
            }
         });

         Log.i("HomePage","Channel started successfully");

      } catch (Exception e){
         Log.e("HomePage","Unable to start channel on HomePage: " + e);
      }

   }


   //method to create a random 4 digit number for room creating purposes
   private int randomInteger(){
      Random rand = new Random();
      int randomNumber = rand.nextInt(9000) + 1000;
      return randomNumber;

   }

   @Override
   protected void onPause() {
      super.onPause();
      try{
         SharedPreferences.Editor preferencesEditor = mSharedPreferences.edit();
         Bundle data = mDeliveryMan.HomePageBundle(uid);
         preferencesEditor.putString(API_KEY, data.getString(API_KEY));
         preferencesEditor.putString(USER_TOKEN,data.getString(USER_TOKEN));
         preferencesEditor.putString(UID,data.getString(UID));
         preferencesEditor.apply();
      }catch (Exception e){
         Log.e("HomePage: ","Error saving preferences in homepage onPause" + e);
      }

   }

}