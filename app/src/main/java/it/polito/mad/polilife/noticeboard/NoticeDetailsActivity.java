package it.polito.mad.polilife.noticeboard;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.ParseUser;

import it.polito.mad.polilife.R;
import it.polito.mad.polilife.db.DBCallbacks;
import it.polito.mad.polilife.db.PoliLifeDB;
import it.polito.mad.polilife.db.classes.Notice;
import it.polito.mad.polilife.db.classes.Student;

public class NoticeDetailsActivity extends AppCompatActivity
        implements DBCallbacks.SingleFetchCallback<Notice> {

    private Notice mNotice;
    private Toolbar mToolbar;
    private ProgressBar mWait;
    private TextView mTitleView, mDescriptionView;
    private boolean loaded = false;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice_details);
        mWait = (ProgressBar) findViewById(R.id.wait);
        mTitleView = (TextView) findViewById(R.id.title_text_view);
        mDescriptionView = (TextView) findViewById(R.id.description_text_view);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        String id = getIntent().getStringExtra("data");

        PoliLifeDB.retreiveObject(id, Notice.class, this);

        if (savedInstanceState != null){
            loaded = savedInstanceState.getBoolean("load");
            isFavorite = savedInstanceState.getBoolean("flag");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null){
            outState.putBoolean("load", loaded);
            outState.putBoolean("flag", isFavorite);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null){
            loaded = savedInstanceState.getBoolean("load");
            isFavorite = savedInstanceState.getBoolean("flag");
        }
    }



    @Override
    public void onFetchSuccess(Notice result) {
        mWait.setVisibility(View.INVISIBLE);
        loaded = true;
        isFavorite = true;

        mNotice = result;
        update();
    }

    @Override
    public void onFetchError(Exception exception) {
        mWait.setVisibility(View.INVISIBLE);
        loaded = true;
        isFavorite = false;
    }

    private void update(){
        mTitleView.setText(mNotice.getTitle() != null ? mNotice.getTitle() : "No title");
        mDescriptionView.setText(mNotice.getDescription() != null ? mNotice.getDescription() : "No description");

        ViewGroup details = (ViewGroup) findViewById(R.id.details_layout);
        details.removeAllViews();
        if (mNotice.getPrice() > 0){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getPrice()+"");
            details.addView(item);
        }
        if (mNotice.getLocationName() != null){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getLocationName());
            details.addView(item);
        }
        if (mNotice.getPublishedAt() != null){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getPublishedAt().toString());
            details.addView(item);
        }
        if (mNotice.getPropertyType() != null){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getPropertyType());
            details.addView(item);
        }
        if (mNotice.getContractType() != null){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getContractType());
            details.addView(item);
        }
        if (mNotice.getSize() > 0){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getSize()+"");
            details.addView(item);
        }
        if (mNotice.getAvailableFrom() != null){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getAvailableFrom().toString());
            details.addView(item);
        }
        if (mNotice.getTags() != null && !mNotice.getTags().isEmpty()){
            View item = LayoutInflater.from(this).inflate(R.layout.layout_material_list_item, null, false);
            ((ImageView) item.findViewById(R.id.rowIcon)).setImageResource(R.drawable.ic_mail);
            ((TextView) item.findViewById(R.id.rowText)).setText(mNotice.getTags().toString());
            details.addView(item);
        }

        View[] advViews = {
                findViewById(R.id.adv_name_layout),
                findViewById(R.id.adv_mail_layout),
                findViewById(R.id.adv_phone_layout)
        };
        int[] icons = {
                R.drawable.ic_user,
                R.drawable.ic_mail,
                R.drawable.ic_call_grey600_24dp
        };
        ParseUser owner = mNotice.getOwner();
        String fname = owner.getString(Student.FIRST_NAME);
        String lname = owner.getString(Student.LAST_NAME);
        String name = (fname != null && lname != null) ? fname+" "+lname : "null";
        String phone = owner.getString(Student.CONTACT_PHONE);
        String[] values = {
                name,
                owner.getEmail() != null ? owner.getEmail() : "null",
                phone != null ? phone : "null"
        };
        for (int i=0;i<advViews.length;i++){
            ((ImageView) advViews[i].findViewById(R.id.rowIcon)).setImageResource(icons[i]);
            ((TextView) advViews[i].findViewById(R.id.rowText)).setText(values[i]);
        }
    }

}