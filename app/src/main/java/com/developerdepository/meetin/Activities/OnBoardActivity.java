package com.developerdepository.meetin.Activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.ViewPager;

import com.developerdepository.meetin.Adapters.OnBoardingSliderAdapter;
import com.developerdepository.meetin.R;

import maes.tech.intentanim.CustomIntent;

public class OnBoardActivity extends AppCompatActivity {

    //View Variables
    private ViewPager sliderViewPager;
    private LinearLayout indicatorsLayout;
    private TextView getStartedBtn;
    private ConstraintLayout nextBtn;
    private CardView nextBtnContainer;

    //Other Variables
    private OnBoardingSliderAdapter onBoardingSliderAdapter;
    private TextView[] indicators;
    int CURRENT_POSITION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboard);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setActionOnViews();

        //Call Adapter
        onBoardingSliderAdapter = new OnBoardingSliderAdapter(OnBoardActivity.this);
        sliderViewPager.setAdapter(onBoardingSliderAdapter);
        sliderViewPager.addOnPageChangeListener(onPageChangeListener);

        createIndicatorsLayout(0);
    }

    private void initViews() {
        //Initialize Views
        sliderViewPager = findViewById(R.id.slider_view_pager);
        indicatorsLayout = findViewById(R.id.indicators_layout);
        getStartedBtn = findViewById(R.id.get_started_btn);
        nextBtn = findViewById(R.id.next_btn);
        nextBtnContainer = findViewById(R.id.next_btn_container);
    }

    private void setActionOnViews() {
        //Set Action On Views

        getStartedBtn.setOnClickListener(v -> {
            startActivity(new Intent(OnBoardActivity.this, WelcomeActivity.class));
            CustomIntent.customType(OnBoardActivity.this, "fadein-to-fadeout");
            finish();
        });

        nextBtn.setOnClickListener(v -> sliderViewPager.setCurrentItem(CURRENT_POSITION + 1));
    }

    private void createIndicatorsLayout(int position) {
        //Set Dashes
        indicators = new TextView[4];
        indicatorsLayout.removeAllViews();

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new TextView(OnBoardActivity.this);
            indicators[i].setText(Html.fromHtml("&#183;"));
            indicators[i].setTextSize(40);
            indicators[i].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            indicators[i].setTextColor(getColor(R.color.colorInactive));

            indicatorsLayout.addView(indicators[i]);
        }

        if (indicators.length > 0) {
            indicators[position].setText(Html.fromHtml("&#183;"));
            indicators[position].setTextSize(40);
            indicators[position].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            indicators[position].setTextColor(getColor(R.color.colorPrimary));
        }
    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            createIndicatorsLayout(position);
            CURRENT_POSITION = position;
            if (position == 0) {
                nextBtnContainer.setVisibility(View.VISIBLE);
                nextBtn.setEnabled(true);
            } else if (position == 1) {
                nextBtnContainer.setVisibility(View.VISIBLE);
                nextBtn.setEnabled(true);
            } else if (position == 2) {
                nextBtnContainer.setVisibility(View.VISIBLE);
                nextBtn.setEnabled(true);
            } else if (position == 3) {
                nextBtnContainer.setVisibility(View.INVISIBLE);
                nextBtn.setEnabled(false);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}