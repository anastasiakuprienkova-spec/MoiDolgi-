
package ru.moidolgi.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {
    LinearLayout root, list;
    TextView total;
    JSONArray debts = new JSONArray();
    int mode = 0;

    int blue = Color.rgb(55, 75, 180);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        load();
        show();
    }

    void load() {
        try {
            String s = getPreferences(0)
                .getString("debts", "[]");
            debts = new JSONArray(s);
        } catch(Exception e) {
            debts = new JSONArray();
        }
    }

    void save() {
        getPreferences(0).edit()
            .putString("debts", debts.toString())
            .apply();
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(12, 18, 12, 18);
        return t;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    void show() {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24, 30, 24, 12);
        root.setBackgroundColor(Color.rgb(248,249,253));

        TextView title = text("Мои долги", 30);
        title.setTextColor(blue);
        root.addView(title);

        total = text("", 22);
        root.addView(total);

        LinearLayout tabs = new LinearLayout(this);
        Button a = button("Мне должны");
        Button b = button("Я должна");
        tabs.addView(a, new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(b, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(tabs);

        a.setOnClickListener(v -> {
            mode = 0;
            show();
        });
        b.setOnClickListener(v -> {
            mode = 1;
            show();
        });

        Button add = button("+ Добавить долг");
        root.addView(add);
        add.setOnClickListener(v -> addDebt());

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(1);
        scroll.addView(list);
        root.addView(scroll);

        setContentView(root);
        refresh();
    }

    void refresh() {
        list.removeAllViews();
        double sum = 0;

        for(int i=0; i<debts.length(); i++) {
            JSONObject d = debts.optJSONObject(i);
            if(d == null || d.optInt("mode") != mode)
                continue;

            double amount = d.optDouble("amount");
            sum += amount;

            final int index = i;
            String name = d.optString("name");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(1);
            card.setPadding(16,12,16,12);
            card.setBackgroundColor(Color.WHITE);

            card.addView(text(name + " — " +
                String.format("%.2f ₽", amount), 20));

            Button pay = button("Вернули / погасить");
            card.addView(pay);
            pay.setOnClickListener(v -> payDebt(index));

            list.addView(card);

            View space = new View(this);
            list.addView(space,
                new LinearLayout.LayoutParams(1,12));
        }

        total.setText((mode == 0 ?
            "Мне должны: " : "Я должна: ") +
            String.format("%.2f ₽", sum));
    }

    void addDebt() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        box.setPadding(35,15,35,0);

        EditText name = new EditText(this);
        name.setHint("Имя");
        box.addView(name);

        EditText amount = new EditText(this);
        amount.setHint("Сумма в рублях");
        amount.setInputType(8194);
        box.addView(amount);

        new AlertDialog.Builder(this)
            .setTitle("Новый долг")
            .setView(box)
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Сохранить",(d,w)->{
                try {
                    String n = name.getText().toString().trim();
                    double x = Double.parseDouble(
                        amount.getText().toString()
                        .replace(",","."));

                    if(n.isEmpty() || x <= 0 ||
                       !Double.isFinite(x)) {
                        Toast.makeText(this,
                            "Проверьте имя и сумму",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    JSONObject obj = new JSONObject();
                    obj.put("name",n);
                    obj.put("amount",x);
                    obj.put("mode",mode);
                    debts.put(obj);
                    save();
                    refresh();
                } catch(Exception e) {
                    Toast.makeText(this,
                        "Неверная сумма",
                        Toast.LENGTH_SHORT).show();
                }
            }).show();
    }

    void payDebt(int index) {
        JSONObject obj = debts.optJSONObject(index);
        if(obj == null) return;

        EditText input = new EditText(this);
        input.setHint("Сумма погашения");
        input.setInputType(8194);
        input.setText(String.format(
            java.util.Locale.US, "%.2f",
            obj.optDouble("amount")));

        new AlertDialog.Builder(this)
            .setTitle("Погашение долга")
            .setView(input)
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Погасить",(d,w)->{
                try {
                    double paid = Double.parseDouble(
                        input.getText().toString()
                        .replace(",","."));
                    double old = obj.optDouble("amount");

                    if(paid <= 0 || paid > old ||
                       !Double.isFinite(paid)) {
                        Toast.makeText(this,
                            "Проверьте сумму",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double remaining = Math.round(
                        (old-paid)*100.0)/100.0;

                    if(remaining <= 0) {
                        JSONArray updated = new JSONArray();
                        for(int j=0;j<debts.length();j++)
                            if(j != index)
                                updated.put(debts.opt(j));
                        debts = updated;
                    } else {
                        obj.put("amount",remaining);
                    }
                    save();
                    refresh();
                } catch(Exception e) {
                    Toast.makeText(this,
                        "Неверная сумма",
                        Toast.LENGTH_SHORT).show();
                }
            }).show();
    }
}
