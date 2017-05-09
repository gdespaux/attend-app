package com.quickattend.quickattend.activity;

import android.Manifest;
import android.os.Bundle;

import com.heinrichreimersoftware.materialintro.app.IntroActivity;
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide;
import com.quickattend.quickattend.R;

public class MainIntroActivity extends IntroActivity {
    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // Add slides, edit configuration...
        addSlide(new SimpleSlide.Builder()
                .title("First Intro Slide")
                .description("It's really rather exquisite")
                //.image(R.drawable.image_1)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Second Intro Slide")
                .description("Isn't this pleasant?")
                //.image(R.drawable.image_1)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build());
    }
}