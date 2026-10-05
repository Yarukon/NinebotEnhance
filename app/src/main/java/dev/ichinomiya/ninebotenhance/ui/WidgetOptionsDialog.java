package dev.ichinomiya.ninebotenhance.ui;

import android.app.*;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import dev.ichinomiya.ninebotenhance.client.FrameClient;
import dev.ichinomiya.ninebotenhance.core.WidgetSettings;
import java.util.List;

/** One dialog shape for the per-widget options: check boxes bound to setting bits, radio choices between bit patterns, plus sliders; saved into the shared widget settings. Labels only, no explanatory copy. */
public final class WidgetOptionsDialog {
    public record Option(String label,int flag){}
    /** One of several bit patterns; the shown pick is the last whose bits are all set, saving clears every pattern's bits first. */
    public record Choice(String label,List<String> names,List<Integer> masks){}
    public interface Describe{String of(int value);}
    public record Slider(String label,int min,int max,int value,Describe describe){}
    public interface Apply{WidgetSettings apply(WidgetSettings current,int mask,int[] sliders);}
    private static final Describe SECONDS=v->v+" 秒";
    public static void pair(Activity activity,FrameClient frames,View reference){
        show(activity,frames,reference,"手机 + 胎压",
                List.of(new Option("前胎压力与温度",WidgetSettings.TYRE_FRONT),new Option("后胎压力与温度",WidgetSettings.TYRE_REAR),new Option("左右互换",WidgetSettings.TYRES_LEFT)),
                List.of(),(current,mask,values)->current.withMask(mask));
    }
    public static void voltage(Activity activity,FrameClient frames,View reference){
        WidgetSettings s=frames.widgetSettings();
        show(activity,frames,reference,"电压",List.of(new Option("曲线图",WidgetSettings.VOLTAGE_CHART)),
                List.of(new Slider("曲线时长",WidgetSettings.MIN_CHART_SECONDS,WidgetSettings.MAX_CHART_SECONDS,s.chartSeconds(),SECONDS)),
                (current,mask,values)->current.withMask(mask).chart(values[0]));
    }
    public static void speed(Activity activity,FrameClient frames,View reference){
        WidgetSettings s=frames.widgetSettings();
        show(activity,frames,reference,"速度",List.of(new Option("曲线图",WidgetSettings.SPEED_CHART)),
                List.of(new Slider("曲线时长",WidgetSettings.MIN_CHART_SECONDS,WidgetSettings.MAX_CHART_SECONDS,s.speedChartSeconds(),SECONDS)),
                (current,mask,values)->current.withMask(mask).speedChart(values[0]));
    }
    public static void power(Activity activity,FrameClient frames,View reference){
        WidgetSettings s=frames.widgetSettings();
        show(activity,frames,reference,"功率",List.of(new Option("曲线图",WidgetSettings.POWER_CHART)),
                List.of(new Choice("kW 显示",List.of("关闭","1000 W 及以上","始终"),List.of(0,WidgetSettings.POWER_KW,WidgetSettings.POWER_KW_ALWAYS))),
                List.of(new Slider("曲线时长",WidgetSettings.MIN_CHART_SECONDS,WidgetSettings.MAX_CHART_SECONDS,s.powerChartSeconds(),SECONDS)),
                (current,mask,values)->current.withMask(mask).powerChart(values[0]));
    }
    public static void hold(Activity activity,FrameClient frames,View reference){
        WidgetSettings s=frames.widgetSettings();
        show(activity,frames,reference,"驻车避让",List.of(),
                List.of(new Slider("最小功率",WidgetSettings.MIN_HOLD_POWER,WidgetSettings.MAX_HOLD_POWER,s.holdPowerMin(),v->v+" W"),
                        new Slider("最大功率",WidgetSettings.MIN_HOLD_POWER_MAX,WidgetSettings.MAX_HOLD_POWER_MAX,s.holdPowerMax(),v->v+" W"),
                        new Slider("速度阈值",WidgetSettings.MIN_HOLD_SPEED,WidgetSettings.MAX_HOLD_SPEED,s.holdSpeedMax(),v->v+" km/h"),
                        new Slider("最短持续",WidgetSettings.MIN_HOLD_SECONDS,WidgetSettings.MAX_HOLD_SECONDS,s.holdSeconds(),SECONDS)),
                (current,mask,values)->current.withMask(mask).holdRange(values[0],values[1],values[2],values[3]));
    }
    public static void show(Activity activity,FrameClient frames,View reference,String heading,List<Option> options,List<Slider> sliders,Apply apply){show(activity,frames,reference,heading,options,List.of(),sliders,apply);}
    public static void show(Activity activity,FrameClient frames,View reference,String heading,List<Option> options,List<Choice> choices,List<Slider> sliders,Apply apply){
        MirrorUi theme=new MirrorUi(activity,reference);WidgetSettings settings=frames.widgetSettings();
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        int pad=MirrorUi.dp(activity,20),gap=MirrorUi.dp(activity,8);content.setPadding(pad,gap,pad,gap);
        if(!options.isEmpty())content.addView(caption(activity,theme,"选项"));
        CheckBox[] checks=new CheckBox[options.size()];
        for(int i=0;i<options.size();i++){
            CheckBox box=new CheckBox(activity);checks[i]=box;box.setText(options.get(i).label());box.setTextColor(theme.text);box.setTextSize(15);
            box.setButtonTintList(ColorStateList.valueOf(theme.accent));box.setPadding(0,gap,0,gap);box.setChecked(settings.enabled(options.get(i).flag()));
            content.addView(box,new LinearLayout.LayoutParams(-1,-2));
        }
        RadioButton[][] picks=new RadioButton[choices.size()][];
        for(int i=0;i<choices.size();i++){
            Choice choice=choices.get(i);content.addView(caption(activity,theme,choice.label()));
            RadioGroup group=new RadioGroup(activity);picks[i]=new RadioButton[choice.names().size()];int shown=0;
            for(int j=0;j<picks[i].length;j++){
                RadioButton b=new RadioButton(activity);b.setId(View.generateViewId());b.setText(choice.names().get(j));b.setTextColor(theme.text);b.setTextSize(15);
                b.setButtonTintList(ColorStateList.valueOf(theme.accent));b.setPadding(0,gap,0,gap);group.addView(b);picks[i][j]=b;
                int bits=choice.masks().get(j);if((settings.mask()&bits)==bits)shown=j;
            }
            picks[i][shown].setChecked(true);content.addView(group);
        }
        SeekBar[] bars=new SeekBar[sliders.size()];
        for(int i=0;i<sliders.size();i++){Slider s=sliders.get(i);bars[i]=slider(activity,theme,content,s.label(),s.min(),s.max(),s.value(),s.describe());}
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        TextView title=new TextView(activity);title.setText(heading);title.setTextSize(20);title.setTextColor(theme.text);title.setPadding(pad,pad,pad,pad/2);
        AlertDialog dialog=new AlertDialog.Builder(activity).setCustomTitle(title).setView(scroll).setNegativeButton("关闭",null)
                .setPositiveButton("保存",(d,w)->{
                    WidgetSettings current=frames.widgetSettings();int mask=current.mask();
                    for(int i=0;i<checks.length;i++)mask=checks[i].isChecked()?mask|options.get(i).flag():mask&~options.get(i).flag();
                    for(int i=0;i<picks.length;i++){for(int bits:choices.get(i).masks())mask&=~bits;for(int j=0;j<picks[i].length;j++)if(picks[i][j].isChecked())mask|=choices.get(i).masks().get(j);}
                    int[] values=new int[bars.length];for(int i=0;i<bars.length;i++)values[i]=bars[i].getProgress();
                    frames.saveWidgetSettings(apply.apply(current,mask,values));
                }).create();
        dialog.show();dialog.getWindow().setBackgroundDrawable(theme.background(activity,theme.surface,22,false));
        dialog.getButton(-1).setTextColor(theme.accent);dialog.getButton(-2).setTextColor(theme.accent);
    }
    static SeekBar slider(Activity activity,MirrorUi theme,LinearLayout parent,String label,int min,int max,int value,Describe describe){
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(caption(activity,theme,label),new LinearLayout.LayoutParams(0,-2,1));
        TextView shown=new TextView(activity);shown.setTextColor(theme.text);shown.setTextSize(13);shown.setText(describe.of(value));header.addView(shown);
        LinearLayout.LayoutParams headerParams=new LinearLayout.LayoutParams(-1,-2);headerParams.topMargin=MirrorUi.dp(activity,8);parent.addView(header,headerParams);
        SeekBar bar=new SeekBar(activity);bar.setMin(min);bar.setMax(max);bar.setProgress(Math.max(min,Math.min(max,value)));bar.setContentDescription(label);
        bar.setProgressTintList(ColorStateList.valueOf(theme.accent));bar.setThumbTintList(ColorStateList.valueOf(theme.accent));bar.setProgressBackgroundTintList(ColorStateList.valueOf(theme.input));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean fromUser){shown.setText(describe.of(progress));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
        parent.addView(bar,new LinearLayout.LayoutParams(-1,MirrorUi.dp(activity,44)));
        return bar;
    }
    static TextView caption(Activity activity,MirrorUi theme,String text){TextView v=new TextView(activity);v.setText(text);v.setTextColor(theme.secondary);v.setTextSize(13);v.setPadding(0,MirrorUi.dp(activity,6),0,MirrorUi.dp(activity,2));return v;}
    private WidgetOptionsDialog(){}
}
