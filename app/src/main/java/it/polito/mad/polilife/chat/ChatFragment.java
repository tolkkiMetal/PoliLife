package it.polito.mad.polilife.chat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.polilife.R;
import it.polito.mad.polilife.db.classes.Student;

/**
 * Created by luigi on 01/01/16.
 */
public class ChatFragment extends Fragment {

    private static final int NEW_CHAT_REQUEST_CODE = 1;

    public static ChatFragment newInstance(){
        return new ChatFragment();
    }

    private Student mUser = (Student) ParseUser.getCurrentUser();

    private PubnubChatManager mChatManager;

    private ListView mConversations;
    private List<String> mSubscriptions;
    private int mSelected = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String UUID = mUser.getUsername();
        mChatManager = new PubnubChatManager(UUID);
        mChatManager.setChatListener(new PubnubChatManager.ChatListener() {
            @Override
            void onSubscribedToChannel(final String channel) {
                if (!mSubscriptions.contains(channel)) {
                    mUser.addChannel(channel);
                    mUser.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null){
                                Log.e("Parse", "error: "+e.getMessage());
                                return;
                            }
                            mSubscriptions.add(channel);
                            ((BaseAdapter) mConversations.getAdapter()).notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_home, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Show dialog");
                final CharSequence[] choiceList = {"One to one", "Group"};
                DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent i = new Intent(getActivity(), CreateChatActivity.class);
                                i.putExtra("mode",mSelected);
                                startActivityForResult(i, NEW_CHAT_REQUEST_CODE);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                dialog.dismiss();
                                break;
                            default:
                                mSelected = which;
                                break;
                        }
                    }
                };
                builder.setSingleChoiceItems(choiceList, mSelected, onClick)
                        .setCancelable(false)
                        .setPositiveButton("OK", onClick)
                        .setNegativeButton("Cancel", onClick);
                final AlertDialog alert = builder.create();
                alert.show();
            }
        });
        mSubscriptions = mUser.getChannels() != null ? mUser.getChannels() : new ArrayList<String>();
        mConversations = (ListView) view.findViewById(R.id.conversations_list);
        mConversations.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mSubscriptions.size();
            }

            @Override
            public String getItem(int position) {
                return mSubscriptions.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(
                            R.layout.layout_chat_friends_item, parent, false);
                }
                final String item = getItem(position);
                ((TextView) convertView.findViewById(R.id.friend_name)).setText(item);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), MessagingActivity.class);
                        intent.putExtra("UUID", mChatManager.getUUID());
                        intent.putExtra("CHANNEL", item);
                        startActivity(intent);
                    }
                });
                return convertView;
            }
        });
        mChatManager.init(mSubscriptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_CHAT_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                switch(data.getIntExtra("mode", -1)){
                    case CreateChatActivity.ONE_TO_ONE:
                        String otherUUID = data.getStringExtra("params");
                        mChatManager.newOneToOneChat(otherUUID);
                        break;
                    case CreateChatActivity.ONE_TO_MANY:
                        List<String> UUIDs = data.getStringArrayListExtra("params");
                        mChatManager.newGroupChat("a", UUIDs);
                        break;
                }
            }
        }
    }
}
