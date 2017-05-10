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
                .title("Thanks for joining QuickAttend")
                .description("We've made attendance tracking a pleasant experience")
                .image(R.drawable.filled_clipboard_icon)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Ditch the pen and paper")
                .description("You'll love our new system")
                .image(R.drawable.filled_pen_icon)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                //.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Stay in touch")
                .description("Notify students of changes or cancellations")
                .image(R.drawable.filled_email_icon)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                //.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Enjoy the freedom")
                .description("Export all of your info easily")
                .image(R.drawable.filled_csv_file)
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                //.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build());

        addSlide(new SimpleSlide.Builder()
                .title("Ready to start?")
                .description("Let's make your account!")
                .background(R.color.colorPrimary)
                .backgroundDark(R.color.colorPrimaryDark)
                .scrollable(false)
                //.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .build());
    }
}