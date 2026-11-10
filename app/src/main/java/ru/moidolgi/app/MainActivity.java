
package ru.moidolgi.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText("Мои долги");
        text.setTextSize(28);
        text.setGravity(Gravity.CENTER);

        setContentView(text);
    }
}
