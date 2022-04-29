package com.wts.router;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

@Route(scheme = Router.SCHEME_WTS, host = "main",
        paramKey = {"a", "b"}, paramTyped = {int.class, boolean.class})
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout layout = new FrameLayout(this);
        Button button = new Button(this);
        button.setText(String.format("Hello World!a=%s,b=%s",
                getIntent().getIntExtra("a", 0),
                getIntent().getFloatExtra("b", 0.0f)));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        layout.addView(button, params);
        setContentView(layout);
        button.setOnClickListener(v -> {
//            IRoute route = Router.getInstance().makeRoute(MainActivity.class, null)[0];
//            Router.getInstance().open(MainActivity.this, route);
            Router.getInstance().open(MainActivity.this, "wts://main?a=1.0&b=2.1");
        });
    }

}