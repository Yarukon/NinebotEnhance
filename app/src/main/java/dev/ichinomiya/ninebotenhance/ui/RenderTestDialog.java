package dev.ichinomiya.ninebotenhance.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import dev.ichinomiya.ninebotenhance.client.FrameClient;
import dev.ichinomiya.ninebotenhance.core.BatteryTelemetry;
import dev.ichinomiya.ninebotenhance.core.BmsCard;
import dev.ichinomiya.ninebotenhance.core.BmsData;
import dev.ichinomiya.ninebotenhance.core.BmsSettings;
import dev.ichinomiya.ninebotenhance.core.BmsState;
import dev.ichinomiya.ninebotenhance.core.LampState;
import dev.ichinomiya.ninebotenhance.core.RideState;
import dev.ichinomiya.ninebotenhance.core.TireTelemetry;
import dev.ichinomiya.ninebotenhance.notification.BmsBundle;
import dev.ichinomiya.ninebotenhance.notification.DashboardHud;
import dev.ichinomiya.ninebotenhance.notification.DashboardOcclusion;

/**
 * Offline preview of the dashboard HUD fed synthetic data, so the cards can be checked without a vehicle. Owns a private
 * DashboardHud and its own tire/battery telemetry, refreshed once a second; never touches the running session's state.
 */
public final class RenderTestDialog {
    private static final String KEY="render-test";
    public static void show(Activity activity,FrameClient frames,View reference){
        MirrorUi theme=new MirrorUi(activity,reference);
        DashboardHud hud=new DashboardHud();hud.reset(KEY);
        hud.setWidgets(frames.widgetSettings());hud.setDark(frames.dashboardDark());hud.setBmsLayout(frames.bmsLayout());
        hud.setOcclusions(frames.dashboardLayout().referenceOcclusions());
        TireTelemetry tires=new TireTelemetry();tires.select(KEY);
        BatteryTelemetry battery=new BatteryTelemetry();battery.select(KEY);
        int frameWidth=frames.frameWidth(),frameHeight=frames.frameHeight();boolean halfScreen=frames.halfScreen();
        int background=frames.cachedSettings().background(frames.dashboardDark());
        Preview preview=new Preview(activity,hud,tires,battery,background,frameWidth,frameHeight,halfScreen);
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        int pad=MirrorUi.dp(activity,20);content.setPadding(pad,MirrorUi.dp(activity,8),pad,MirrorUi.dp(activity,8));
        content.addView(preview,new LinearLayout.LayoutParams(-1,-2));
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        TextView title=new TextView(activity);title.setText("测试渲染");title.setTextSize(20);title.setTextColor(theme.text);title.setPadding(pad,pad,pad,pad/2);
        AlertDialog dialog=new AlertDialog.Builder(activity).setCustomTitle(title).setView(scroll).setNegativeButton("关闭",null).setPositiveButton("通知",null).create();
        dialog.show();dialog.getWindow().setBackgroundDrawable(theme.background(activity,theme.surface,22,false));
        dialog.getButton(-1).setTextColor(theme.accent);dialog.getButton(-2).setTextColor(theme.accent);
        dialog.getButton(-1).setOnClickListener(v->{hud.simulate(SystemClock.elapsedRealtime());preview.invalidate();});
        dialog.setOnDismissListener(d->preview.stop());
    }
    /** The frame at the dialog's width, scaled from the reference frame size; re-fed every second while attached so nothing expires. */
    private static final class Preview extends View{
        private final DashboardHud hud;private final TireTelemetry tires;private final BatteryTelemetry battery;
        private final int background,frameWidth,frameHeight;private final boolean halfScreen;
        private final Handler handler=new Handler(Looper.getMainLooper());private final Runnable tick=this::pulse;
        private long started;
        Preview(Context context,DashboardHud hud,TireTelemetry tires,BatteryTelemetry battery,int background,int frameWidth,int frameHeight,boolean halfScreen){
            super(context);this.hud=hud;this.tires=tires;this.battery=battery;this.background=background;
            this.frameWidth=Math.max(160,frameWidth);this.frameHeight=Math.max(160,frameHeight);this.halfScreen=halfScreen;
        }
        @Override protected void onMeasure(int widthSpec,int heightSpec){
            int width=MeasureSpec.getSize(widthSpec);setMeasuredDimension(width,Math.round(width*(float)frameHeight/frameWidth));
        }
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();started=SystemClock.elapsedRealtime();handler.post(tick);}
        @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();stop();}
        @Override protected void onWindowVisibilityChanged(int visibility){
            super.onWindowVisibilityChanged(visibility);
            if(visibility==VISIBLE){if(!handler.hasCallbacks(tick))handler.post(tick);}else stop();
        }
        void stop(){handler.removeCallbacks(tick);}
        private void pulse(){feed(SystemClock.elapsedRealtime());invalidate();handler.postDelayed(tick,1000);}
        private void feed(long now){
            long elapsed=now-started;
            Bundle phone=new Bundle();phone.putInt("battery",76);phone.putBoolean("charging",false);phone.putBoolean("wifi",true);phone.putBoolean("phone_permission",true);
            phone.putIntegerArrayList("slots",new ArrayList<>(Arrays.asList(1,2)));phone.putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4,2)));phone.putInt("data_slot",2);
            Bundle music=new Bundle();music.putBoolean("granted",true);music.putBoolean("active",true);music.putString("media_session",KEY);
            music.putInt("state",3);music.putString("title","测试渲染");music.putString("artist","Ninebot Enhance");
            music.putLong("duration",200000);music.putLong("position",elapsed%200000);music.putLong("updated",now);music.putFloat("speed",1f);
            Bundle lamp=new Bundle();lamp.putInt("phase",LampState.READY);lamp.putInt("position",60);lamp.putInt("speed",0);lamp.putInt("low",0);lamp.putInt("high",100);lamp.putInt("percent",60);
            BmsData sample=BmsCard.SAMPLE;
            BmsData fresh=new BmsData(sample.name(),sample.mos(),sample.cells(),sample.capacityAh(),sample.remainingAh(),sample.volts(),sample.amps(),sample.watts(),sample.soc(),
                    sample.temps(),sample.maxCellMv(),sample.minCellMv(),sample.avgCellMv(),sample.diffMv(),sample.cycleAh(),sample.cycles(),sample.cellMv(),now);
            Bundle state=new Bundle();state.putString("epoch",KEY);state.putLong("cursor",0);state.putBoolean("enabled",false);
            state.putBundle("phone",phone);state.putBundle("music",music);state.putBundle("lamp",lamp);
            state.putBundle("bms",BmsBundle.write(new BmsState(BmsState.READY,fresh,""),BmsSettings.DEFAULT_POLL_MS));
            hud.accept(KEY,state,now);
            tires.update(KEY,true,2.4f,29f,TireTelemetry.Source.BLUETOOTH,now,now);tires.update(KEY,false,2.6f,30f,TireTelemetry.Source.BLUETOOTH,now,now);
            hud.acceptTires(tires.snapshot());
            battery.update(KEY,79.3f+0.3f*(float)Math.sin(elapsed/1000.0),BatteryTelemetry.Source.BLUETOOTH,now,now);
            hud.acceptBattery(battery.snapshot());
            hud.acceptRide(new RideState.Snapshot(320,850,now,now));
        }
        @Override protected void onDraw(Canvas canvas){
            int save=canvas.save();float scale=getWidth()/(float)frameWidth;canvas.scale(scale,scale);
            canvas.drawColor(background);
            long now=SystemClock.elapsedRealtime();hud.draw(canvas,frameWidth,frameHeight,now);
            if(!halfScreen)DashboardOcclusion.draw(canvas,frameWidth,frameHeight,hud.hillHold(now),hud.occlusions());
            canvas.restoreToCount(save);
            if(hud.animating(now))postInvalidateOnAnimation();
        }
    }
    private RenderTestDialog(){}
}
