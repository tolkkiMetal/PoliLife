package it.polito.mad.polilife.chat;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.polilife.R;

public class MessagingActivity extends AppCompatActivity {

    private String mChannel;
    private ListView mMessagesListView;
    private EditText mMsgEditText;
    private PubnubChatManager mChatManager = PubnubChatManager.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);
        String UUID = getIntent().getStringExtra("UUID");
        mChannel = getIntent().getStringExtra("CHANNEL");
        //mChatManager = new PubnubChatManager(UUID);
        List<String> l = new ArrayList<>();
        l.add(mChannel);
        mChatManager.init(l);

        mMsgEditText = (EditText) findViewById(R.id.msg);
        mMessagesListView = (ListView) findViewById(R.id.messages_list);
        final ChatMessagesBaseAdapter adapter = new ChatMessagesBaseAdapter(this, mChatManager.getUUID());
        mMessagesListView.setAdapter(adapter);
        mChatManager.setChatListener(new PubnubChatManager.ChatListener() {
            @Override
            public void onTextMessageReceived(String channel, ChatMessage message) {
                Log.d("PUBNUB", "Received msg: "+message.toString());
                adapter.addMessage(message);
            }

            @Override
            void onTextMessageSent(String channel, ChatMessage message) {
                Log.d("PUBNUB", "Sent msg: "+message.toString());
                adapter.addMessage(message);
                mMsgEditText.setText("");
            }

            @Override
            public void onSubscribedToChannel(String channel) {

            }

            @Override
            public void onHistory(String channel, List<ChatMessage> messages) {
                adapter.setData(messages);
            }
        });

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = mMsgEditText.getText().toString();
                mChatManager.sendMessage(mChannel, msg);
            }
        });
        mChatManager.history(mChannel, 30);
    }

    @Override
    protected void onPause() {
        mChatManager.unsubscribe(mChannel);
        super.onPause();
    }
}
