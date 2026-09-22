package com.daimajia.slider.library;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An empty host for the tests. A SliderLayout needs a window, a themed Context and a real
 * lifecycle owner before any of its behaviour means anything, so every test puts its views in
 * here rather than building them against the application context.
 */
public class SliderTestActivity extends AppCompatActivity {

    public FrameLayout root;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        root.setId(android.R.id.content + 1);
        setContentView(root);
    }
}
