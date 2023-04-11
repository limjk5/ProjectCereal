package com.example.loginpage.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import com.example.loginpage.R;
import com.example.loginpage.utility.Database;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.util.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.channel.ChannelClient;
import io.getstream.chat.android.client.models.Attachment;
import io.getstream.chat.android.client.models.Message;
import io.getstream.chat.android.ui.message.input.MessageInputView;
import kotlin.Pair;


/**
 * @author saran
 * @date 31/3/2023
 */
public class CustomMessageSend extends ConstraintLayout implements MessageInputView.MessageSendHandler{
   private final Database mDatabase = Database.getInstance();
   public static ChannelClient classChannel;
   private boolean allowTaPermission = false;
   private boolean allowStudentPermission = false;

   public CustomMessageSend(@NonNull Context context) {
      super(context);
      setListeners(context);
   }

   public CustomMessageSend(@NonNull Context context, @Nullable AttributeSet attrs) {
      super(context, attrs);
      setListeners(context);
   }

   public CustomMessageSend(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
      setListeners(context);
   }

   public CustomMessageSend(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
      super(context, attrs, defStyleAttr, defStyleRes);
      setListeners(context);
   }
   private void setListeners(Context context){
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View view = inflater.inflate(R.layout.custom_question_input, this, true);
      EditText inputField = view.findViewById(R.id.inputField);
      ImageButton sendButton = view.findViewById(R.id.sendButton);
      ToggleButton TAToggleButton = view.findViewById(R.id.TAToggleButton);
      ToggleButton StudentToggleButton = view.findViewById(R.id.studentToggleButton);
      sendButton.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            String messageString = inputField.getText().toString();
            String checkMessage = messageString.trim();
            if(!checkMessage.equals("")){
               sendMessage(messageString,null);
               resetState(inputField,StudentToggleButton,TAToggleButton);
            }
         }
      });
      TAToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            allowTaPermission = b;
         }
      });
      StudentToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            allowStudentPermission = b;
         }
      });

   }

   @Override
   public void sendMessage(@NonNull String s, @Nullable Message message) {
      ChatClient client = ChatClient.instance();
      Message message1 = construct_message(s,client);
      mDatabase.sendMessage(classChannel.getChannelId(),message1).onSuccessTask(new SuccessContinuation<Void, Object>() {
         @NonNull
         @Override
         public Task<Object> then(Void unused) throws Exception {
            Log.d("CustomMessageSend","Sent message to database with ID:"+message1.getId());
            classChannel.sendMessage(message1).enqueue(result -> {
               if(result.isSuccess()){
                  Log.e("CustomMessageSend","Message with text: "+ message1.getText()+" was sent successfully");
               } else {
                  Log.e("CustomMessageSend","Error sending message with text " + message1.getText() + result);
               }
            });
            return null;
         }
      });

   }

   private Message construct_message(String s,ChatClient client){
      Message message = new Message();
      message.setId(random_id());
      message.setText(s);
      message.setCid(classChannel.getCid());
      message.setUser(Objects.requireNonNull(client.getCurrentUser()));
      HashMap<String,Object> extraData = new HashMap<>();
      extraData.put("vote_count",0);
      extraData.put("RC",0);
      extraData.put("channel_id",classChannel.getChannelId());
      extraData.put("allow_ta",allowTaPermission? "true" : "false");
      extraData.put("allow_student",allowStudentPermission? "true" : "false");
      extraData.put("profApproved","false");
      extraData.put("taApproved","false");
      extraData.put("studentApproved","false");

      message.setExtraData(extraData);
      return message;
   }
   private String random_id(){
      String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
              + "0123456789"
              + "abcdefghijklmnopqrstuvxyz";
      StringBuilder sb = new StringBuilder(8);
      for (int i = 0; i < 8; i++) {
         int index
                 = (int)(AlphaNumericString.length()
                 * Math.random());
         sb.append(AlphaNumericString
                 .charAt(index));
      }
      return sb.toString();
   }
   private void resetState(EditText editText, ToggleButton studentButton, ToggleButton TAButton){
      editText.setText("");
      studentButton.setChecked(false);
      TAButton.setChecked(false);
   }
   @Override
   public void dismissReply() {

   }

   @Override
   public void editMessage(@NonNull Message message, @NonNull String s) {

   }



   @Override
   public void sendMessageWithAttachments(@NonNull String s, @NonNull List<? extends Pair<? extends File, String>> list, @Nullable Message message) {
      Log.d("CustomMessageSend","Sending attachments");
   }

   @Override
   public void sendMessageWithCustomAttachments(@NonNull String s, @NonNull List<Attachment> list, @Nullable Message message) {
      Log.d("CustomMessageSend","Sending custom attachments");
   }

   @Override
   public void sendToThread(@NonNull Message message, @NonNull String s, boolean b) {

   }

   @Override
   public void sendToThreadWithAttachments(@NonNull Message message, @NonNull String s, boolean b, @NonNull List<? extends Pair<? extends File, String>> list) {

   }

   @Override
   public void sendToThreadWithCustomAttachments(@NonNull Message message, @NonNull String s, boolean b, @NonNull List<Attachment> list) {

   }

}