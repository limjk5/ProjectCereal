package com.example.loginpage;

/**
 * @author saran
 * @date 20/2/2023
 */


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.loginpage.databinding.ActivityMessageBinding;
import com.example.loginpage.utility.CustomMessageViewHolderFactory;
import com.example.loginpage.utility.Database;
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Normal;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.Mode.Thread;
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel.State.NavigateUp;

import java.util.HashMap;

import io.getstream.chat.android.client.ChatClient;
import io.getstream.chat.android.client.channel.ChannelClient;
import io.getstream.chat.android.client.models.Message;
import io.getstream.chat.android.client.models.Reaction;
import io.getstream.chat.android.ui.message.input.viewmodel.MessageInputViewModelBinding;
import io.getstream.chat.android.ui.message.list.header.MessageListHeaderView;
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel;
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModelBinding;
import io.getstream.chat.android.ui.message.list.viewmodel.MessageListViewModelBinding;
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory;

public class ChannelActivity extends AppCompatActivity {

    private final static String CID_KEY = "shk4bq5vqttmrfush2e98d9d83n7bz5cwj8ws4dtxe9xby3nw8hgsr5vjmr4qcms";
    private static ChannelClient classChannel;
    private static Database mDatabase;
    private ChatClient messageClient;
    private String currentCID;
    public ChannelActivity(){
        super(R.layout.activity_message);
    }
    public static Intent newIntent(Context context, ChannelClient channel, Database database) {
        classChannel = channel;
        mDatabase = database;
        final Intent intent = new Intent(context, ChannelActivity.class);
        intent.putExtra(CID_KEY, channel.getCid());
        return intent;
    }
    public String getCurrentCID(){return currentCID;}
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Step 0 - inflate binding
        ActivityMessageBinding binding = ActivityMessageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        String cid = getIntent().getStringExtra(CID_KEY);
        System.out.println("printing cid"+cid);
        if (cid == null) {
            throw new IllegalStateException("Specifying a channel id is required when starting ChannelActivity");
        }
        // Step 1 - Create three separate ViewModels for the views so it's easy
        //          to customize them individually
        ViewModelProvider.Factory factory = new MessageListViewModelFactory.Builder()
                .cid(cid)
                .build();
        ViewModelProvider provider = new ViewModelProvider(this, factory);

        MessageListHeaderViewModel messageListHeaderViewModel = provider.get(MessageListHeaderViewModel.class);
        MessageListViewModel messageListViewModel = provider.get(MessageListViewModel.class);
        MessageInputViewModel messageInputViewModel = provider.get(MessageInputViewModel.class);

        // TODO set custom Imgur attachment factory
        //this one bro
        /*binding.messageListView.setMessageReactionHandler((message,reactionType) ->{
            Reaction reaction = new Reaction();
            reaction.setMessageId("message-id");
            reaction.setType("like");
            reaction.setScore(5);
            boolean enforceUnique = false;
            classChannel.sendReaction(reaction, enforceUnique).enqueue(result -> {
                if (result.isSuccess()) {
                    Reaction sentReaction = result.data();
                } else {
                    System.out.println("failed reaction");
                }
            });
        });*/

        // Step 2 - Bind the view and ViewModels, they are loosely coupled so it's easy to customize
        MessageListHeaderViewModelBinding.bind(messageListHeaderViewModel, binding.messageListHeaderView, this);
        MessageListViewModelBinding.bind(messageListViewModel, binding.messageListView, this, true);
        MessageInputViewModelBinding.bind(messageInputViewModel, binding.messageInputView, this);
        /*binding.messageListView.setMessageClickListener((result) ->{
            Log.d("hello","clicked");
            System.out.println("Message clicked");
            System.out.println(result);
        });*/
        // Step 3 - Let both MessageListHeaderView and MessageInputView know when we open a thread
        messageListViewModel.getMode().observe(this, mode -> {
            if (mode instanceof Thread) {
                Message parentMessage = ((Thread) mode).getParentMessage();
                messageListHeaderViewModel.setActiveThread(parentMessage);
                messageInputViewModel.setActiveThread(parentMessage);
            } else if (mode instanceof Normal) {
                messageListHeaderViewModel.resetThread();
                messageInputViewModel.resetThread();
            }
        });


        // Step 4 - Let the message input know when we are editing a message
        // TODO: Add message filtering
        //binding.messageInputView.setSendMessageHandler(new CustomMessageSend(classChannel,mDatabase));

        binding.messageListView.setRepliesEnabled(true);
        binding.messageListView.setThreadsEnabled(true);
        binding.messageListView.setEditMessageEnabled(true);
        binding.messageListView.setMessageEditHandler(messageInputViewModel::postMessageToEdit);

        binding.messageListView.setMessageReplyHandler((parent,message)-> {
            System.out.println(message);
            System.out.println(parent);
            System.out.println("Break point");
            // TODO Logic handling for replies
        });
        binding.messageListView.setThreadStartHandler((result)->{
            System.out.println("THREAD STARTED!");
            System.out.println(result);
        });





        binding.messageInputView.setOnSendButtonClickListener(()->{
//            List<Message> messages = classChannel.watch().execute().data().getMessages();
//            for(Message m: messages){
//                System.out.println("message on send listener " + m.getText());
//            }
        });
        // Step 5 - Handle navigate up state
        messageListViewModel.getState().observe(this, state -> {
            if (state instanceof NavigateUp) {
                finish();
            }
        });

        // Step 6 - Handle back button behaviour correctly when you're in a thread
        MessageListHeaderView.OnClickListener backHandler = () -> {
            Intent int1 = new Intent(ChannelActivity.this,HomePage.class);
            Bundle b = new Bundle();
            b.putString("username","Admin");
            int1.putExtras(b);
            startActivity(int1);
            messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed.INSTANCE);
        };
        binding.messageListHeaderView.setBackButtonClickListener(backHandler);
        // Customised View Model for Messages
        binding.messageListView.setMessageViewHolderFactory(new CustomMessageViewHolderFactory(mDatabase));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backHandler.onClick();

            }
        });
    }
    /*private void startChannel(){
        try{
            ChannelClient channelClient = messageClient.channel("messaging", "message_room");
//                 Map<String, Object> extraData = new HashMap<>();
//                 extraData.put("name","");
//                 List<String> memberIds = new LinkedList<>();
            startActivity(ChannelActivity.newIntent(this,channelClient,mDatabase));
            System.out.println(" Channel started successfully ");
        } catch (Exception e){
            System.out.println("Unable to start channel on HomePage: " + e);
        }

    }*/
    private static Message messageConstructor(String s, int id){
        Message newMessage = new Message();
        newMessage.setText(s);
        newMessage.setId(Integer.toString(id));
        newMessage.setCid(classChannel.getCid());
        HashMap<String,Object> extraData = new HashMap<>();
        extraData.put("current_votes",(double)0.0);
        newMessage.setExtraData(extraData);
        return newMessage;
    }
//    private static void printMessageList(){
//        List<Message> messages = getLatestChannel().watch().execute().data().getMessages();
//        System.out.println(messages.toArray().length + " messages in the channel, total_message_count " + total_message_count);
//        for (Message m: messages){
//            System.out.println(m.getId() + " message id in channel ");
//            System.out.println(m.getExtraData() + " in channel " + classChannel.getId());
//        }
//    }
    private static ChannelClient getLatestChannel(){
        return ChatClient.instance().channel(classChannel.getCid());
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) { //saving instance
        // Save the user's current game state.
        savedInstanceState.putString(getIntent().getStringExtra(CID_KEY),currentCID);
        // Always call the superclass so it can save the view hierarchy state.
        System.out.println("saved");
        super.onSaveInstanceState(savedInstanceState);
    }
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy.
        super.onRestoreInstanceState(savedInstanceState);
        // Restore state members from saved instance.
        System.out.println("restored");
        currentCID = savedInstanceState.getString(getIntent().getStringExtra(CID_KEY));
    }
}
