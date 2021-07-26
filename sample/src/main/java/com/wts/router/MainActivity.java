package com.wts.router;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

@Route(scheme = "demo", host = "main", attach = "*")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

}