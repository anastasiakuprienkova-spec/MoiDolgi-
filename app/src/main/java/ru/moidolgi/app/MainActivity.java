
package ru.moidolgi.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    final int BG=0xFF0D0D14, CARD=0xFF1B1B29, FIELD=0xFF28283A;
    final int WHITE=0xFFF5F3FF, MUTED=0xFFA7A4BC, PURPLE=0xFFAA8BFF;
    final int GREEN=0xFF68DBAD, RED=0xFFFF929E;
    JSONArray debts = new JSONArray();
    LinearLayout page, items;
    EditText search;
    int filter=0;
    String query="";
    final Locale ru = new Locale("ru","RU");

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        load(); draw();
    }

    SharedPreferences prefs(){
        return getPreferences(MODE_PRIVATE);
    }

    void load(){
        try{
            debts=new JSONArray(prefs().getString("debts","[]"));
        }catch(Exception e){
            debts=new JSONArray();
        }
    }

    void save(){
        prefs().edit().putString("debts",debts.toString()).apply();
    }

    int dp(float n){
        return (int)(getResources().getDisplayMetrics().density*n+0.5f);
    }

    GradientDrawable shape(int color,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    GradientDrawable outline(int color){
        GradientDrawable g=shape(color,18);
        g.setStroke(dp(1),0xFF343347);
        return g;
    }

    TextView label(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }

    LinearLayout vertical(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(1);
        return l;
    }

    LinearLayout horizontal(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(0);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    void pad(View v,int a,int b,int c,int d){
        v.setPadding(dp(a),dp(b),dp(c),dp(d));
    }

    void space(LinearLayout parent,int h){
        View v=new View(this);
        parent.addView(v,new LinearLayout.LayoutParams(1,dp(h)));
    }

    String money(double value){
        return NumberFormat.getNumberInstance(ru)
            .format(Math.round(value*100)/100.0)+" ₽";
    }

    String today(){
        return new SimpleDateFormat("dd.MM.yyyy",ru).format(new Date());
    }

    double amount(JSONObject o){
        return Math.max(0,o.optDouble("amount",0));
    }

    double original(JSONObject o){
        return o.optDouble("original",amount(o));
    }

    boolean active(JSONObject o){
        return amount(o)>0.005;
    }

    String person(JSONObject o){
        return o.optString("name","Без имени");
    }

    TextView pill(String s,int color){
        TextView t=label(s,12,color,true);
        pad(t,12,8,12,8);
        t.setBackground(shape(FIELD,14));
        return t;
    }

    void draw(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        page=vertical();
        pad(page,20,26,20,32);
        scroll.addView(page);
        setContentView(scroll);

        TextView eyebrow=label("ЛИЧНЫЕ ФИНАНСЫ",11,PURPLE,true);
        eyebrow.setLetterSpacing(.18f);
        page.addView(eyebrow);
        space(page,8);

        page.addView(label("Мои долги",32,WHITE,true));
        space(page,20);

        LinearLayout summary=vertical();
        pad(summary,20,22,20,22);

        GradientDrawable gradient=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF3D286F,0xFF211B3E,0xFF181A2C}
        );
        gradient.setCornerRadius(dp(24));
        summary.setBackground(gradient);

        summary.addView(label("ЧИСТЫЙ БАЛАНС",12,0xFFD4C5FF,true));
        space(summary,9);

        double incoming=0,outgoing=0;

        for(int i=0;i<debts.length();i++){
            JSONObject o=debts.optJSONObject(i);
            if(o==null||!active(o))continue;
            if(o.optInt("mode",0)==0)
                incoming+=amount(o);
            else
                outgoing+=amount(o);
        }

        summary.addView(label(money(incoming-outgoing),31,WHITE,true));
        space(summary,21);

        LinearLayout metrics=horizontal();

        LinearLayout left=vertical();
        left.addView(label("↑ Мне должны",13,0xFFD4C5FF,false));
        space(left,5);
        left.addView(label(money(incoming),17,GREEN,true));

        LinearLayout right=vertical();
        right.addView(label("↓ Я должна",13,0xFFD4C5FF,false));
        space(right,5);
        right.addView(label(money(outgoing),17,RED,true));

        metrics.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        metrics.addView(right,new LinearLayout.LayoutParams(0,-2,1));
        summary.addView(metrics);
        page.addView(summary);
        space(page,20);

        TextView add=label("＋  Добавить долг",16,0xFF150F25,true);
        add.setGravity(Gravity.CENTER);
        pad(add,16,17,16,17);
        add.setBackground(shape(PURPLE,18));
        page.addView(add);
        add.setOnClickListener(v->editDebt(-1));
        space(page,27);

        page.addView(label("Записи",23,WHITE,true));
        space(page,14);

        search=new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("⌕  Поиск по имени");
        pad(search,16,10,16,10);
        search.setBackground(outline(CARD));
        search.setText(query);
        page.addView(search);

        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                query=s.toString();
                renderList();
            }
            public void afterTextChanged(Editable e){}
        });

        space(page,14);

        HorizontalScrollView hscroll=new HorizontalScrollView(this);
        hscroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs=horizontal();

        String[] names={"Все","Мне должны","Я должна","История"};

        for(int i=0;i<4;i++){
            final int n=i;
            TextView tab=pill(names[i],i==filter?0xFF160F26:WHITE);
            tab.setBackground(shape(i==filter?PURPLE:FIELD,15));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);
            lp.rightMargin=dp(8);
            tabs.addView(tab,lp);
            tab.setOnClickListener(v->{
                filter=n;
                draw();
            });
        }

        hscroll.addView(tabs);
        page.addView(hscroll);
        space(page,18);

        items=vertical();
        page.addView(items);
        renderList();
    }

    void renderList(){
        if(items==null)return;
        items.removeAllViews();
        int count=0;

        for(int i=debts.length()-1;i>=0;i--){
            JSONObject o=debts.optJSONObject(i);
            if(o==null)continue;

            boolean isActive=active(o);
            int mode=o.optInt("mode",0);

            if(filter==1&&(!isActive||mode!=0))continue;
            if(filter==2&&(!isActive||mode!=1))continue;
            if(filter==3&&isActive)continue;
            if(filter==0&&!isActive)continue;

            if(!person(o).toLowerCase(ru).contains(query.toLowerCase(ru)))
                continue;

            count++;
            final int index=i;

            LinearLayout card=vertical();
            pad(card,17,17,17,17);
            card.setBackground(outline(CARD));

            LinearLayout top=horizontal();
            LinearLayout nameBlock=vertical();

            nameBlock.addView(label(person(o),18,WHITE,true));
            space(nameBlock,5);
            nameBlock.addView(label(
                isActive?(mode==0?"Вам должны":"Вы должны"):"Погашено",
                12,isActive?MUTED:GREEN,false
            ));

            top.addView(nameBlock,new LinearLayout.LayoutParams(0,-2,1));

            TextView value=label(
                money(amount(o)),19,
                isActive?(mode==0?GREEN:RED):MUTED,true
            );

            top.addView(value);
            card.addView(top);

            String note=o.optString("note","");
            if(!note.isEmpty()){
                space(card,10);
                card.addView(label(note,13,MUTED,false));
            }

            String date=o.optString("date","");
            if(!date.isEmpty()){
                space(card,7);
                card.addView(label("Создано: "+date,11,MUTED,false));
            }

            JSONArray events=o.optJSONArray("history");

            if(events!=null && events.length()>0){
                space(card,10);

                TextView history=label(
                    "История операций  ·  "+events.length(),
                    12,PURPLE,true
                );

                card.addView(history);

                history.setOnClickListener(v->{
                    StringBuilder details=new StringBuilder();

                    for(int k=events.length()-1;k>=0;k--){
                        JSONObject ev=events.optJSONObject(k);
                        if(ev==null)continue;

                        details.append(ev.optString("date",""))
                            .append("  ·  ")
                            .append(ev.optString("type","Операция"));

                        if(ev.has("amount"))
                            details.append("  ")
                                .append(money(ev.optDouble("amount")));

                        if(ev.has("from"))
                            details.append("  ")
                                .append(money(ev.optDouble("from")))
                                .append(" → ")
                                .append(money(ev.optDouble("to")));

                        details.append("\n\n");
                    }

                    new AlertDialog.Builder(this)
                        .setTitle("История: "+person(o))
                        .setMessage(details.toString())
                        .setPositiveButton("Закрыть",null)
                        .show();
                });
            }

            space(card,13);
            LinearLayout actions=horizontal();

            if(isActive){
                TextView pay=pill("Погасить",PURPLE);
                actions.addView(pay);
                pay.setOnClickListener(v->pay(index));
            }

            TextView edit=pill("Изменить",WHITE);
            LinearLayout.LayoutParams elp=
                new LinearLayout.LayoutParams(-2,-2);
            elp.leftMargin=dp(8);

            actions.addView(edit,elp);
            edit.setOnClickListener(v->editDebt(index));
            card.addView(actions);

            items.addView(card);
            space(items,12);
        }

        if(count==0){
            TextView empty=label(
                filter==3?"Погашенных долгов пока нет":
                    "Здесь пока нет записей",
                15,MUTED,false
            );
            empty.setGravity(Gravity.CENTER);
            pad(empty,8,32,8,32);
            items.addView(empty);
        }
    }

    EditText field(String hint,String value,boolean numeric){
        EditText e=new EditText(this);
        e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);
        e.setTextSize(16);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);

        if(numeric)
            e.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL |
                InputType.TYPE_NUMBER_FLAG_SIGNED
            );

        pad(e,14,12,14,12);
        e.setBackground(outline(FIELD));
        return e;
    }

    double parse(String s)throws Exception{
        double x=Double.parseDouble(s.trim().replace(",","."));
        if(!Double.isFinite(x)||x<=0||x>1000000000000.0)
            throw new Exception();

        return Math.round(x*100.0)/100.0;
    }

    void error(String s){
        Toast.makeText(this,s,Toast.LENGTH_LONG).show();
    }

    void editDebt(int index){
        JSONObject old=index<0?null:debts.optJSONObject(index);
        if(index>=0&&old==null)return;

        LinearLayout form=vertical();
        pad(form,18,10,18,0);

        EditText name=field(
            "Имя",old==null?"":person(old),false
        );
        form.addView(name);
        space(form,10);

        EditText value=field(
            "Остаток долга, ₽",
            old==null?"":String.format(Locale.US,"%.2f",amount(old)),
            true
        );
        form.addView(value);
        space(form,10);

        EditText note=field(
            "Комментарий (необязательно)",
            old==null?"":old.optString("note",""),
            false
        );
        form.addView(note);
        space(form,10);

        Spinner direction=new Spinner(this);
        String[] opts={"Мне должны","Я должна"};

        ArrayAdapter<String> adapter=new ArrayAdapter<>(
            this,android.R.layout.simple_spinner_dropdown_item,opts
        );

        direction.setAdapter(adapter);
        direction.setSelection(
            old==null?(filter==2?1:0):old.optInt("mode",0)
        );
        form.addView(direction);

        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(index<0?"Новый долг":"Редактирование")
            .setView(form)
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Сохранить",null)
            .create();

        if(index>=0)
            dialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                "Удалить",
                (d,w)->confirmDelete(index)
            );

        dialog.setOnShowListener(v->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn->{
                    try{
                        String n=name.getText().toString().trim();
                        if(n.isEmpty()){
                            error("Введите имя");
                            return;
                        }

                        double a=parse(value.getText().toString());
                        JSONObject o=old==null?new JSONObject():old;
                        double previous=amount(o);

                        o.put("name",n);
                        o.put("amount",a);
                        o.put("mode",direction.getSelectedItemPosition());
                        o.put("note",note.getText().toString().trim());

                        if(old==null){
                            o.put("original",a);
                            o.put("date",today());
                            o.put("history",new JSONArray());
                            debts.put(o);
                        }else{
                            o.put("original",Math.max(a,original(o)));

                            if(a!=previous){
                                JSONArray history=o.optJSONArray("history");
                                if(history==null)history=new JSONArray();

                                JSONObject event=new JSONObject();
                                event.put("date",today());
                                event.put("type","Корректировка остатка");
                                event.put("from",previous);
                                event.put("to",a);

                                history.put(event);
                                o.put("history",history);
                            }
                        }

                        save();
                        dialog.dismiss();
                        draw();

                    }catch(Exception e){
                        error("Введите корректную положительную сумму");
                    }
                });
        });

        dialog.show();
    }

    void confirmDelete(int index){
        new AlertDialog.Builder(this)
            .setTitle("Удалить запись?")
            .setMessage(
                "Долг и его история будут удалены без возможности восстановления."
            )
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Удалить",(d,w)->{
                JSONArray next=new JSONArray();

                for(int i=0;i<debts.length();i++)
                    if(i!=index)next.put(debts.opt(i));

                debts=next;
                save();
                draw();
            }).show();
    }

    void pay(int index){
        JSONObject o=debts.optJSONObject(index);
        if(o==null)return;

        double remaining=amount(o);

        EditText input=field(
            "Сумма погашения",
            String.format(Locale.US,"%.2f",remaining),
            true
        );

        LinearLayout wrap=vertical();
        pad(wrap,18,12,18,0);
        wrap.addView(input);

        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("Погасить долг")
            .setMessage("Осталось: "+money(remaining))
            .setView(wrap)
            .setNegativeButton("Отмена",null)
            .setPositiveButton("Подтвердить",null)
            .create();

        dialog.setOnShowListener(v->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(btn->{
                    try{
                        double p=parse(input.getText().toString());

                        if(p>remaining){
                            error("Сумма больше остатка");
                            return;
                        }

                        double next=Math.round(
                            (remaining-p)*100.0
                        )/100.0;

                        o.put("amount",next);

                        JSONArray history=o.optJSONArray("history");
                        if(history==null)history=new JSONArray();

                        JSONObject event=new JSONObject();
                        event.put("date",today());
                        event.put("type","Погашение");
                        event.put("amount",p);

                        history.put(event);
                        o.put("history",history);

                        if(next==0)
                            o.put("closedDate",today());

                        save();
                        dialog.dismiss();
                        draw();

                    }catch(Exception e){
                        error("Проверьте сумму");
                    }
                })
        );

        dialog.show();
    }
}
