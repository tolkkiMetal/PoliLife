package it.polito.mad.polilife;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Toast;

import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import it.polito.mad.polilife.chat.ChatFragment;
import it.polito.mad.polilife.db.DBCallbacks;
import it.polito.mad.polilife.db.PoliLifeDB;
import it.polito.mad.polilife.db.classes.Job;
import it.polito.mad.polilife.db.classes.Student;
import it.polito.mad.polilife.db.push.JSONFactory;
import it.polito.mad.polilife.db.push.PushBroadcastReceiver;
import it.polito.mad.polilife.didactical.DidacticalHomeFragment;
import it.polito.mad.polilife.news.NewsFragment;
import it.polito.mad.polilife.noticeboard.NoticeBoardActivity;
import it.polito.mad.polilife.noticeboard.NoticeboardHomeFragment;
import it.polito.mad.polilife.placement.PositionsListFragment;
import it.polito.mad.polilife.placement.JobPlacementFragment;
import it.polito.mad.polilife.profile.ProfileFragment;

public class HomeActivity extends AppCompatActivity
        implements JobPlacementFragment.Listener, ProfileFragment.EditModeChangeListener {

    private static final int PROFILE = 0;
    private static final int DIDACTICS = 1;
    private static final int NOTICEBOARD = 2;
    private static final int JOBPLACEMENT = 3;
    private static final int CHAT = 4;
    private static final int NEWS = 5;

    private Student user = (Student) ParseUser.getCurrentUser();

    private Fragment[] pages = {
            ProfileFragment.newInstance(),
            DidacticalHomeFragment.newInstance(),
            NoticeboardHomeFragment.newInstance(),
            JobPlacementFragment.newInstance(),
            ChatFragment.newInstance(),
            NewsFragment.newInstance()
    };

    private PoliLifeNavigationDrawer mNavigationDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolbar;
    private String[] mTitles;

    private int mCurrentFeature = NEWS ;
    private int currentAlpha = 255;
    private int i;

    private boolean mProfileEditMode = false;

    //if bcast is sent when this activity is still resumed
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("json")){
                try{
                    JSONObject obj = new JSONObject(intent.getStringExtra("json"));
                    if (JSONFactory.isChatMessage(obj)) mCurrentFeature = CHAT;
                    else if (JSONFactory.isDidacticalMessage(obj)) mCurrentFeature = DIDACTICS;
                    else if (JSONFactory.isJobMessage(obj)) mCurrentFeature = JOBPLACEMENT;
                    if (pages[mCurrentFeature] instanceof PushListener) {
                        ((PushListener) pages[mCurrentFeature]).onPushReceived(obj);
                    }
                    showPage(mCurrentFeature);
                }catch(JSONException e){}
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //if bcast is sent when this activity was paused
        if (getIntent().hasExtra("json")){
            try{
                JSONObject obj = new JSONObject(getIntent().getStringExtra("json"));
                if (JSONFactory.isChatMessage(obj)) mCurrentFeature = CHAT;
                else if (JSONFactory.isDidacticalMessage(obj)) mCurrentFeature = DIDACTICS;
                else if (JSONFactory.isJobMessage(obj)) mCurrentFeature = JOBPLACEMENT;
                if (pages[mCurrentFeature] instanceof PushListener) {
                    ((PushListener) pages[mCurrentFeature]).onPushReceived(obj);
                }
                showPage(mCurrentFeature);
            }catch(JSONException e){}
        }

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.getBackground().setAlpha(255);

        mTitles = getResources().getStringArray(R.array.student_drawer_titles);
        setUpNavigationDrawer();

        mNavigationDrawer.setOnItemClickListener(new PoliLifeNavigationDrawer.SimpleOnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                final Context context = HomeActivity.this;
                mCurrentFeature = position;
                switch (position) {
                    default:
                        showPage(position);
                        break;
                }
            }

            @Override
            public void onFooterClick(View view) {
                PoliLifeDB.logOut(new DBCallbacks.UserLogoutCallback() {
                    @Override
                    public void onLogoutSuccess() {
                        Intent toHomePage = new Intent(HomeActivity.this, MainActivity.class);
                        startActivity(toHomePage);
                        finish();
                    }

                    @Override
                    public void onLogoutError(Exception exception) {
                        Toast.makeText(HomeActivity.this, exception.getMessage(), Toast.LENGTH_SHORT);
                        Intent toHomePage = new Intent(HomeActivity.this, MainActivity.class);
                        startActivity(toHomePage);
                        finish();
                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(PushBroadcastReceiver.HOME_ACTIVITY_INTENT_ACTION);
        registerReceiver(mIntentReceiver, filter);
        showPage(mCurrentFeature);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mIntentReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int id = R.menu.menu_empty;
        switch(mCurrentFeature){
            case NEWS:
                id = R.menu.menu_home;
                break;
            case PROFILE:
                id = mProfileEditMode ? R.menu.menu_edited_profile : R.menu.menu_editable_profile;
                break;
        }
        getMenuInflater().inflate(id, menu);
        getSupportActionBar().setTitle(mTitles[mCurrentFeature]);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_didactics:
                showPage(DIDACTICS);
                break;
            case R.id.action_chat:
                showPage(CHAT);
                break;
            case R.id.action_edit_profile:
            case R.id.action_confirm_profile:
                ProfileFragment profile = (ProfileFragment) pages[PROFILE];
                mProfileEditMode = !mProfileEditMode;
                profile.switchToEditMode(mProfileEditMode);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditModeChanged() {
        supportInvalidateOptionsMenu();
    }

    private void setUpNavigationDrawer() {
        TypedArray ar = getResources().obtainTypedArray(R.array.student_drawer_icons);
        int[] icons = new int[ar.length()];
        for (int i = 0; i < mTitles.length; i++) {
            icons[i] = ar.getResourceId(i, 0);
        }
        ar.recycle();

        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mNavigationDrawer = new PoliLifeNavigationDrawer(
                mTitles, icons,
                mDrawerLayout
        );

        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                (Toolbar) findViewById(R.id.toolbar),
                R.string.drawer_open,
                R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View v) {
                if (i == 1) {
                    mToolbar.getBackground().setAlpha(currentAlpha);
                } else {
                    mToolbar.getBackground().setAlpha(255);
                }
                super.onDrawerClosed(v);
                invalidateOptionsMenu();
                syncState();
                //showPage(mCurrentFeature);
            }

            @Override
            public void onDrawerOpened(View v) {
                if (Build.VERSION.SDK_INT >= 19) {
                    currentAlpha = mToolbar.getBackground().getAlpha();
                }
                mToolbar.getBackground().setAlpha(255);
                super.onDrawerOpened(v);
                invalidateOptionsMenu();
                syncState();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String complete = firstName + " " + lastName;
        String mail = user.getEmail();
        if (user.getPhoto() != null) {
            try {
                mNavigationDrawer.setUserData(complete, mail,
                        Utility.getBitmap(user.getPhoto().getData()));
            } catch (Exception e) {
                mNavigationDrawer.setUserData(complete, mail, R.drawable.student_icon);
            }
        } else {
            mNavigationDrawer.setUserData(complete, mail, R.drawable.student_icon);
        }

    }


    private void showPage(int position){
        FragmentManager frgManager = getSupportFragmentManager();
        frgManager.beginTransaction().replace(R.id.container, pages[position]).commit();
        mCurrentFeature = position;
        supportInvalidateOptionsMenu();
        if (mNavigationDrawer != null){
            mNavigationDrawer.close();
        }
    }

    @Override
    public void onSelectAppliedJobs() {
        FragmentManager frgManager = getSupportFragmentManager();
        frgManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .addToBackStack(null)
                .replace(R.id.container, PositionsListFragment.newInstance("applied"))
                .commit();
    }

    @Override
    public void onSelectSavedJobs() {
        FragmentManager frgManager = getSupportFragmentManager();
        frgManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                .addToBackStack(null)
                .replace(R.id.container, PositionsListFragment.newInstance("saved"))
                .commit();
    }

    @Override
    public void onProfileSelected() {
        showPage(PROFILE);
    }

}
