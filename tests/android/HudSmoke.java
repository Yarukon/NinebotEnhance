import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import dev.ichinomiya.ninebotenhance.notification.DashboardHud;
import dev.ichinomiya.ninebotenhance.core.TireTelemetry;
import dev.ichinomiya.ninebotenhance.core.BatteryTelemetry;
import dev.ichinomiya.ninebotenhance.core.WidgetSettings;
import dev.ichinomiya.ninebotenhance.core.WidgetCondition;
import dev.ichinomiya.ninebotenhance.core.NotificationTimeline;
import dev.ichinomiya.ninebotenhance.core.RegisterProbe;
import dev.ichinomiya.ninebotenhance.core.RideState;
import dev.ichinomiya.ninebotenhance.core.SidebarLayout;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/** Runs the built APK's real Android Canvas renderer with synthetic data, without posting notifications. */
public final class HudSmoke {
    private static final int BACKGROUND=0xff343e46,ART=0xff3079c1,ACCENT=0xff7cd6a4;
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    static Bundle state(long cursor, Bundle... events) {
        Bundle phone=new Bundle();phone.putInt("battery",46);phone.putBoolean("charging",true);
        phone.putBoolean("wifi",true);phone.putBoolean("phone_permission",true);
        phone.putIntegerArrayList("slots",new ArrayList<>(Arrays.asList(1,2)));
        phone.putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4,3)));
        Bundle state=new Bundle();state.putString("epoch","smoke");state.putLong("cursor",cursor);
        state.putBoolean("enabled",true);state.putBundle("phone",phone);
        Bundle music=new Bundle();music.putBoolean("granted",true);music.putBoolean("active",true);music.putString("media_session","synthetic-player");
        music.putString("title","夜空中的星");music.putString("artist","示例歌手");music.putInt("state",3);music.putLong("position",65000);music.putLong("duration",240000);music.putFloat("speed",1);music.putLong("updated",100000);
        Bitmap art=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);art.eraseColor(ART);music.putParcelable("art",art);music.putLong("art_revision",1);state.putBundle("music",music);
        state.putParcelableArrayList("events",new ArrayList<>(Arrays.asList(events)));return state;
    }
    private static Bundle event(int seq,long now) {
        Bundle b=new Bundle();b.putString("key","same-app-same-notification-id");b.putLong("seq",seq);
        b.putLong("posted",now);b.putInt("duration",15000);b.putString("app","同一 App");
        b.putString("title","第 "+seq+" 条通知");b.putString("text","每条通知独立显示和计时");return b;
    }
    static TireTelemetry.Snapshot tireState(){
        TireTelemetry t=new TireTelemetry();t.select("synthetic-vehicle");
        t.update("synthetic-vehicle",true,2.4f,29f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        t.update("synthetic-vehicle",false,2.6f,30f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        return t.snapshot();
    }
    static BatteryTelemetry.Snapshot batteryState(){
        BatteryTelemetry b=new BatteryTelemetry();b.select("synthetic-vehicle");
        b.update("synthetic-vehicle",BatteryTelemetry.decode("rVoltage",new byte[]{0x3f,0x1c}),BatteryTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        return b.snapshot();
    }
    private static Bitmap frame(DashboardHud hud,long now) {
        Bitmap image=Bitmap.createBitmap(848,480,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image);canvas.drawColor(BACKGROUND);hud.draw(canvas,848,480,now);return image;
    }
    private static boolean solid(Bitmap image,int left,int top,int right,int bottom,int color){
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)if(image.getPixel(x,y)!=color)return false;
        return true;
    }
    private static boolean blocks(DashboardHud hud,float x,float y,long now){Bundle hit=hud.touch(x,y,848,480,now);return hit!=null&&"block".equals(hit.getString("command"));}
    private static boolean bright(Bitmap image,int left,int top,int right,int bottom){
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++){int p=image.getPixel(x,y);if(Color.red(p)>160&&Color.green(p)>160)return true;}
        return false;
    }
    private static Bitmap crop(Bitmap image,int left,int top,int right,int bottom){return Bitmap.createBitmap(image,left,top,right-left,bottom-top);}
    /** True if the box holds label/value/unit ink: a plain card surface is far darker than any text colour, and this skips the curve's own green. */
    private static boolean textInk(Bitmap image,int left,int right,int top,int bottom){
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++){
            int px=image.getPixel(x,y);
            if(Color.green(px)>Color.red(px)+40)continue;
            if(Color.red(px)+Color.green(px)+Color.blue(px)>270)return true;
        }
        return false;
    }
    /** Pixel span (inclusive) of a column that differs from the given colour, within a vertical search range; 0 if none found. */
    private static int span(Bitmap image,int x,int top,int bottom,int blank){
        int first=-1,last=-1;
        for(int y=top;y<bottom;y++)if(image.getPixel(x,y)!=blank){if(first<0)first=y;last=y;}
        return first<0?0:last-first+1;
    }
    /** Leftmost column in the box that differs from the given colour, scanning left to right; -1 if none found. */
    private static int leftInk(Bitmap image,int left,int right,int top,int bottom,int blank){
        for(int x=left;x<right;x++)for(int y=top;y<bottom;y++)if(image.getPixel(x,y)!=blank)return x;
        return -1;
    }
    /** Rightmost column in the box that differs from the given colour, scanning right to left; -1 if none found. */
    private static int rightInk(Bitmap image,int left,int right,int top,int bottom,int blank){
        for(int x=right-1;x>=left;x--)for(int y=top;y<bottom;y++)if(image.getPixel(x,y)!=blank)return x;
        return -1;
    }
    private static SidebarLayout.Box box(float left,float top,float right,float bottom){return new SidebarLayout.Box(left,top,right,bottom);}
    /** Same font as the HUD percentage, so the tests can locate the right-anchored phone elements. */
    private static float percentWidth(String percent){android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);p.setTextSize(14);p.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));return p.measureText(percent);}
    /** Same font setup as DashboardHud.measure(size,bold), so tests can predict tyre-row text widths instead of guessing them. */
    private static float measureText(String s,float size,boolean bold){android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.create("sans-serif",bold?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL));return p.measureText(s);}
    /** Mirrors DashboardHud.METRIC_CHART_GAP: the gap between the text column and the curve. */
    private static final int METRIC_CHART_GAP=8;
    /** Mirrors DashboardHud.metricColumn with the power card in watts: the widest value-and-unit template of the three chart cards. */
    private static float metricColumn(){
        float voltage=measureText("888.8",17,true)+4+measureText("V",11,false),speed=measureText("88.8",17,true)+4+measureText("km/h",11,false),power=measureText("-8888",17,true)+4+measureText("W",11,false);
        return Math.max(voltage,Math.max(speed,power));
    }
    /** Strongly green (curve line) pixels of a 48-high chart card and the leftmost column holding one; the fill is too faint to count. */
    private static int[] greens(Bitmap image,SidebarLayout.Box card){
        int count=0,first=-1,top=(int)card.top();
        for(int y=top+6;y<top+42;y++)for(int x=(int)card.left()+10;x<(int)card.right()-10;x++){int p=image.getPixel(x,y);if(Color.green(p)>Color.red(p)+40){count++;if(first<0||x<first)first=x;}}
        return new int[]{count,first};
    }
    /** `labelX` is the transform pivot; `barRight` and `unitX` are pre-scale columns; `scale` is 1 unless the row was squeezed. */
    private record TyreColumns(float labelX,float barRight,float scale,float unitX){
        int screenX(float value){return Math.round((value-labelX)*scale+labelX);}
    }
    /** Mirrors DashboardHud.drawTyrePair's column math (bar at a fixed column; temperature/℃ right-aligned to the
     * squeeze-adjusted inset), so tests can predict tyre-pair pixel columns instead of guessing a window. */
    private static TyreColumns tyreColumns(float left,float right,String frontPressure,String rearPressure,String frontTemperature,String rearTemperature){
        float labelW=Math.max(measureText("F",11,false),measureText("R",11,false));
        float pressW=Math.max(measureText(frontPressure,14,true),measureText(rearPressure,14,true));
        float tempW=Math.max(measureText(frontTemperature,14,true),measureText(rearTemperature,14,true));
        float labelX=left+8,barX=labelX+labelW+3+pressW+2,barW=measureText("bar",9,false),degW=measureText("℃",9,false);
        float available=right-left-16,needed=labelW+3+pressW+2+barW+4+tempW+1+degW;
        float scale=needed>available&&needed>0?available/needed:1,insetRight=labelX+Math.max(needed,available);
        return new TyreColumns(labelX,barX+barW,scale,insetRight-degW);
    }
    public static void main(String[] args) {
        try { run(args); } catch(Throwable error) { error.printStackTrace(System.out);System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        Looper.prepareMainLooper();
        // app_process does not get the font-map setup normally performed during application binding.
        android.graphics.Typeface.class.getDeclaredMethod("setSystemFontMap",android.os.SharedMemory.class)
                .invoke(null,new Object[]{null});
        NotificationContentSmoke.run();
        DashboardHud hud=new DashboardHud();hud.reset("test");hud.setWallClock(()->1789453350000L);
        hud.accept("test",state(0),100000);
        Bitmap missingTires=frame(hud,100000);hud.acceptTires(tireState());
        Bitmap missingBattery=frame(hud,100000);hud.acceptBattery(batteryState());
        Bitmap musicFrame=frame(hud,100000);
        var live=hud.stack(100000);int musicTop=(int)live.music().top(),artX=(int)live.music().left()+12+24,artLeft=(int)live.music().left()+12,musicMid=musicTop+32;
        int tyreTop=(int)live.tyres().top(),voltageTop=(int)live.voltage().top();
        check(live.music().right()==838&&live.music().left()==648&&live.music().height()==84,"a playing card keeps the full sidebar width and its 84-pixel height");
        check(live.phone().equals(box(648,420,740,468))&&live.tyres().equals(box(746,420,838,468))&&live.music().bottom()+6==live.phone().top()&&live.voltage().bottom()+6==live.music().top()&&live.voltage().right()==838,"phone status and tyres share the bottom row as two halves with music above them and the chart voltage on top, all in the right column");
        check(live.voltage().height()==48&&live.tyres().height()==48&&live.voltage().top()==276&&live.music().top()==330,"the chart card is 48 high like the paired row, the right column topping out at 276 below the instrument");
        check(live.voltage().left()==648&&live.voltage().width()==190&&live.tyres().width()==92&&live.tyres().right()==838,"the chart card spans the full sidebar width and the tyre half the right 92 pixels");
        check(bright(musicFrame,656,428,692,441)&&bright(musicFrame,656,446,674,462)&&bright(musicFrame,754,428,830,441)&&bright(musicFrame,754,447,830,461)&&solid(musicFrame,741,424,745,464,BACKGROUND)&&hud.touch(743,440,848,480,100000)==null,"both halves draw two rows (clock and battery over the SIMs, front over rear tyre) and the gap between them stays empty and untouchable");
        check(!musicFrame.sameAs(missingTires),"tyre line switches from unknown fields to actual measurements");
        check(!musicFrame.sameAs(missingBattery),"voltage row switches from its placeholder to the decoded reading");
        check(blocks(hud,820,tyreTop+14,100000)&&hud.touch(820,tyreTop-3,848,480,100000)==null&&blocks(hud,700,voltageTop+24,100000)&&hud.touch(700,voltageTop-3,848,480,100000)==null&&hud.touch(550,400,848,480,100000)==null,"tyre and voltage cards only consume touches inside their rectangles");
        check(hud.revision(104000)!=hud.revision(104001),"a voltage older than five read intervals expires and advances the output revision");
        check(!frame(hud,104000).sameAs(frame(hud,104001)),"an expired voltage is drawn as unknown");
        check(musicFrame.getPixel(artX,musicTop+24)==ART&&musicFrame.getPixel(420,musicTop+24)==BACKGROUND,"playing music shows unobscured album art and leaves the app area left of both columns alone");
        check(musicFrame.getPixel(700,236)==BACKGROUND&&musicFrame.getPixel(640,tyreTop+10)==BACKGROUND,"no overlay backing is drawn above the column and the paired row does not reach left of the column");
        check(hud.touch(600,300,848,480,100000)==null,"left app area remains outside hit targets");
        check(solid(musicFrame,700,musicTop+79,816,musicTop+81,musicFrame.getPixel(820,musicTop+52))&&hud.touch(700,417,848,480,100000)==null,"music ends immediately after the time labels without a volume row");
        check(solid(musicFrame,720,musicTop+50,825,musicTop+56,musicFrame.getPixel(820,musicTop+52)),"no textual playback status appears below the artist");
        check(blocks(hud,artX,musicTop+24,100000)&&hud.touch(420,musicTop+24,848,480,100000)==null,"album art only consumes touches inside the music card");
        check(blocks(hud,artLeft+5,musicTop+62,100000),"progress is display only");
        long rev=hud.revision(100000);check(hud.revision(101000)!=rev,"playing music advances the output revision without app redraws");
        Bundle paused=state(0);paused.getBundle("music").putInt("state",2);hud.accept("test",paused,100000);
        Bitmap pausedFrame=frame(hud,100000);
        check(pausedFrame.getPixel(artX-4,musicMid)==Color.WHITE&&pausedFrame.getPixel(artX+3,musicMid)==Color.WHITE&&pausedFrame.getPixel(artX-16,musicMid)==ART,"pause glyph overlays the centre of the original album art");
        check(pausedFrame.getPixel(artLeft+5,musicTop+62)==ACCENT,"paused track retains progress");
        // Auto-hide: the card opened at 100000 and closes five seconds later; being the lowest card, the ones above drop into its place.
        check(hud.stack(104999).music()!=null&&hud.stack(105000).music()==null&&hud.stack(105000).voltage().bottom()==414&&hud.stack(105000).voltage().right()==838,"the music card auto-hides five seconds after the last playback change and the cards above drop into its place");
        check(hud.animating(105100)&&!hud.animating(105400),"hiding fades the music card out and glides the other cards over a short animation");
        check(hud.revision(160000)==hud.revision(161000),"a hidden card, paused music and an empty chart do not cause perpetual redraw revisions");
        check(hud.revision(159000)!=hud.revision(159001),"tyre readings older than two read intervals invalidate an otherwise static frame");
        Bundle idle=state(0);Bundle idleMusic=new Bundle();idleMusic.putBoolean("granted",true);idle.putBundle("music",idleMusic);hud.accept("test",idle,170000);
        var idleStack=hud.stack(170000);Bitmap idleFrame=frame(hud,170400);
        check(idleStack.music()==null&&idleStack.tyres().bottom()==468&&idleStack.voltage().bottom()==414&&idleStack.voltage().right()==838&&hud.touch(820,417,848,480,170000)==null&&hud.touch(820,410,848,480,170000)!=null,"music without a track stays hidden and voltage sits right above the phone and tyre row");
        check(idleFrame.getPixel(640,414)==BACKGROUND&&hud.touch(640,414,848,480,170400)==null,"the app area left of the sidebar stays free");
        hud.accept("test",idle,173000);
        Bundle stopped=state(0);stopped.getBundle("music").putInt("state",1);hud.accept("test",stopped,173000);
        check(idleFrame.sameAs(frame(hud,173400)),"stopped session cannot leave stale track information visible");
        long T=180000;hud.accept("test",state(0),T);
        for(int i=1;i<=3;i++)hud.accept("test",state(i,event(i,T+i*500)),T+i*500);
        check(hud.summary(T+2000).contains("visible=3"),"same notification ID must produce three cards");
        check(blocks(hud,743,388,T+2000),"visible notification consumes taps above music");
        Bitmap image=frame(hud,T+2000);
        // The column change animates for 300 ms after the third card has fully entered; glyph checks use a settled frame.
        Bitmap settled=frame(hud,T+2400);
        int liftedPhoneTop=Math.round(hud.stack(T+2000).phone().top()),columnShift=Math.round(hud.stack(T+2000).music().right())-838;
        check(liftedPhoneTop==420-3*NotificationTimeline.STEP&&hud.stack(T+2000).phone().left()==443&&hud.stack(T+2000).tyres().right()==633&&columnShift==633-838,"three notifications lift the phone and tyre row by three slots, into the instrument zone, so it continues in the left column");
        check(settled.getPixel(artX+columnShift,musicTop+24-3*NotificationTimeline.STEP)==ART&&settled.getPixel(artX,musicTop+24)!=ART&&hud.stack(T+2000).music().right()==633,"three notifications move the entire music widget up three slots and into the left column");
        Bitmap comparison=Bitmap.createBitmap(848,960,Bitmap.Config.ARGB_8888);Canvas comparisonCanvas=new Canvas(comparison);comparisonCanvas.drawBitmap(musicFrame,0,0,null);comparisonCanvas.drawBitmap(settled,0,480,null);
        try(FileOutputStream out=new FileOutputStream(args[0])){comparison.compress(Bitmap.CompressFormat.PNG,100,out);}
        // Re-reading a mailbox response must not duplicate/restart cards or retire the oldest one.
        hud.accept("test",state(3,event(3,T+1500)),T+2000);
        Bitmap replay=frame(hud,T+2000);check(image.sameAs(replay),"mailbox replay changed the rendered queue");
        hud.accept("test",state(4,event(4,T+2100)),T+2100);
        check(hud.summary(T+2339).contains("visible=3"),"fourth post exceeded capacity during exit");
        check(hud.summary(T+2340).contains("visible=3"),"fourth post must enter after oldest exits");
        hud.accept("old-session",state(5,event(5,T+2400)),T+2400);
        check(hud.summary(T+2400).contains("visible=3"),"old session changed active cards");
        Bundle removal=new Bundle();removal.putString("key","same-app-same-notification-id");
        removal.putLong("seq",5);removal.putBoolean("removed",true);
        hud.accept("test",state(5,removal),T+2500);
        check(hud.summary(T+2740).contains("visible=0"),"source removal must remove all its cards");
        hud.reset("next");check(hud.summary(T+3000).contains("visible=0"),"session reset retained cards");
        Bundle narrow=state(0);narrow.putInt("notification_width",200);hud.accept("next",narrow,T+3000);
        narrow=state(1,event(1,T+3000));narrow.putInt("notification_width",200);hud.accept("next",narrow,T+3000);
        check(hud.touch(620,440,848,480,T+4000)==null&&blocks(hud,639,440,T+4000),"custom 200px notification remains right aligned with adjusted hit bounds");
        check(frame(hud,T+4400).getPixel(620,440)==BACKGROUND,"narrower notification frees the left app region once the lifted cards have settled");
        Bundle unauthorised=new Bundle();unauthorised.putString("epoch","revoked");unauthorised.putLong("cursor",0);hud.accept("next",unauthorised,T+4000);
        check(frame(hud,T+4400).getPixel(artX,musicTop+24)!=ART&&hud.stack(T+4400).music()==null,"revoking access clears cached artwork and hides the music card");
        hud.setWidgets(new WidgetSettings(0));hud.accept("next",state(8,event(8,T+4000)),T+4000);
        check(solid(frame(hud,T+4500),0,0,848,480,BACKGROUND),"disabling all widgets clears every HUD pixel once the cards have faded");
        check(hud.touch(740,450,848,480,T+4500)==null&&hud.touch(740,100,848,480,T+4500)==null,"hidden widgets do not intercept app touches");
        hud.setWidgets(new WidgetSettings(WidgetSettings.MUSIC|WidgetSettings.MUSIC_AUTO_HIDE));hud.accept("next",idle,T+5000);
        check(hud.stack(T+5000).music()==null&&hud.touch(820,450,848,480,T+5000)==null&&solid(frame(hud,T+5000),0,0,848,480,BACKGROUND),"music alone stays hidden while nothing plays");
        hud.accept("next",state(0),T+5000);
        check(hud.touch(820,450,848,480,T+5000)!=null&&hud.touch(820,380,848,480,T+5000)==null,"playing music alone packs against the lower edge without reserved phone or vehicle gaps");
        check(hud.stack(T+9999).music()!=null&&hud.stack(T+10000).music()==null,"the music card closes after the configured five seconds");
        Bundle nextTrack=state(0);nextTrack.getBundle("music").putString("title","另一首歌");hud.accept("next",nextTrack,T+11000);
        check(hud.stack(T+11000).music()!=null&&hud.stack(T+15999).music()!=null&&hud.stack(T+16000).music()==null,"a track change re-opens the card for another five seconds");
        Bundle sameTrack=state(0);sameTrack.getBundle("music").putString("title","另一首歌");sameTrack.getBundle("music").putLong("position",90000);hud.accept("next",sameTrack,T+17000);
        check(hud.stack(T+17000).music()==null,"a position update alone does not re-open the card");
        Bundle pausedTrack=state(0);pausedTrack.getBundle("music").putString("title","另一首歌");pausedTrack.getBundle("music").putInt("state",2);hud.accept("next",pausedTrack,T+18000);
        check(hud.stack(T+18000).music()!=null&&hud.stack(T+23000).music()==null,"pausing re-opens the card and it closes again");
        hud.setWidgets(new WidgetSettings(WidgetSettings.MUSIC));
        check(hud.stack(T+30000).music()!=null,"without auto-hide the card stays while a track exists");
        hud.setWidgets(WidgetSettings.DEFAULT);hud.accept("next",state(8,event(8,T+4000)),T+31000);
        check(hud.summary(T+31000).contains("visible=0"),"re-enabling notifications cannot replay posts received while hidden");
        hud.reset("sim");Bundle off=state(0);off.putBoolean("enabled",false);hud.accept("sim",off,300000);
        hud.simulate(300000);check(hud.summary(300100).contains("visible=1"),"a simulated notification appears even while notifications are disabled");
        hud.accept("sim",off,300300);check(hud.summary(300400).contains("visible=1")&&blocks(hud,830,440,300400),"later snapshots keep the simulated card until it expires");
        Bundle two=state(0);two.putInt("notification_limit",1);hud.accept("sim",two,301000);hud.simulate(301000);hud.simulate(301100);
        check(hud.summary(301100).contains("visible=1"),"the visible limit from the phone settings caps simulated cards too");
        Bundle brief=state(0);brief.putInt("notification_seconds",5);hud.accept("sim",brief,320000);hud.simulate(320000);
        check(hud.summary(324999).contains("visible=1")&&hud.summary(325000).contains("visible=0"),"simulated cards use the configured display time");
        hud.reset("vol");Bundle withVolume=state(0);Bundle volume=new Bundle();volume.putInt("level",7);volume.putInt("max",15);volume.putLong("seq",1);withVolume.putBundle("volume",volume);
        hud.accept("vol",withVolume,400000);
        check(frame(hud,400100).getPixel(30,370)==BACKGROUND,"the first volume snapshot of a session only records the level");
        volume.putInt("level",10);volume.putLong("seq",2);hud.accept("vol",withVolume,400500);
        check(frame(hud,400520).getPixel(30,370)==BACKGROUND&&hud.animating(400550),"the bar starts off screen to the left and slides in");
        Bitmap volumeFrame=frame(hud,400900);
        check(volumeFrame.getPixel(30,370)==ACCENT&&volumeFrame.getPixel(30,160)!=ACCENT&&volumeFrame.getPixel(30,160)!=BACKGROUND&&hud.touch(30,300,848,480,400900)==null,"a volume change shows a bar filled to the level above the dashboard speaker icon without blocking touches");
        volume.putInt("level",12);volume.putLong("seq",3);hud.accept("vol",withVolume,401000);
        check(frame(hud,401020).getPixel(30,370)==ACCENT&&!hud.animating(401300),"a further change while the bar is visible keeps it in place instead of sliding in again");
        check(frame(hud,403100).getPixel(30,370)==ACCENT&&frame(hud,403500).getPixel(30,370)==BACKGROUND,"the later change extends the hold and the bar then slides out to the left");
        hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.VOLUME,false));volume.putInt("level",14);volume.putLong("seq",4);hud.accept("vol",withVolume,404000);
        check(frame(hud,404400).getPixel(30,370)==BACKGROUND,"the volume widget switch hides the bar");
        hud.setWidgets(WidgetSettings.DEFAULT);
        hud.reset("chart");hud.accept("chart",state(0),500000);
        BatteryTelemetry series=new BatteryTelemetry();series.select("synthetic-vehicle");
        for(int i=0;i<20;i++){series.update("synthetic-vehicle",79.3f-(i%5)*0.3f,BatteryTelemetry.Source.BLUETOOTH,1789453320000L+i*1000L,480000+i*1000L);hud.acceptBattery(series.snapshot());}
        var chartStack=hud.stack(500000);Bitmap chartFrame=frame(hud,500000);int[] curve=greens(chartFrame,chartStack.voltage());
        // The chart starts right of the text column; the first sample (480000) sits a third into the 30 s window ending at 500000.
        float chartLeft=chartStack.voltage().left()+10+metricColumn()+8,chartRight=chartStack.voltage().right()-10;int firstSample=Math.round(chartLeft+(chartRight-chartLeft)/3);
        check(chartStack.voltage().height()==48&&curve[0]>40&&Math.abs(curve[1]-firstSample)<=2,"the voltage card draws the last 30 s of readings right of its text column, leaving the older left part empty; green="+curve[0]+" first="+curve[1]+" expected="+firstSample);
        check(hud.revision(500000)!=hud.revision(501000)&&hud.revision(501000)!=hud.revision(502000),"a live curve re-encodes once a second as it scrolls");
        hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.VOLTAGE_CHART,false));
        check(hud.stack(500000).voltage().height()==28&&hud.animating(500100)&&!hud.animating(500400),"switching the chart off shrinks the card to one row with a size animation");
        SidebarLayout.Box oneRow=hud.stack(500400).voltage();Bitmap rowFrame=frame(hud,500400);int rowBlank=rowFrame.getPixel((int)oneRow.left()+100,(int)oneRow.top()+14);
        int valueRight=rightInk(rowFrame,(int)oneRow.left()+100,(int)oneRow.right()-4,(int)oneRow.top()+6,(int)oneRow.top()+22,rowBlank);
        check(oneRow.width()==190&&Math.abs(valueRight-(oneRow.right()-10))<=2,"without the chart the full-width row right-aligns value and unit to the inner right edge, found "+valueRight);
        hud.reset("probe");hud.accept("probe",state(0),700000);
        java.util.List<RegisterProbe.Row> rows=new ArrayList<>();
        rows.add(new RegisterProbe.Row("rInfoBool2",new RegisterProbe.Value("0800",8,699000,699000,false,false)));
        rows.add(new RegisterProbe.Row("rStateBool",new RegisterProbe.Value("0100",1,690000,690000,true,false)));
        rows.add(new RegisterProbe.Row("rSpeed",null));
        hud.acceptProbe(rows);
        check(solid(frame(hud,700000),6,28,200,60,BACKGROUND),"the register table is not drawn while the probe switch is off");
        hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.REGISTER_PROBE,true));
        Bitmap probeFrame=frame(hud,700000);
        check(!solid(probeFrame,6,28,200,60,BACKGROUND)&&bright(probeFrame,10,32,120,60)&&hud.touch(100,45,848,480,700000)==null,"with the probe switch on the register table is drawn at the left without blocking touches");
        check(hud.revision(700000)!=hud.revision(701000),"the register table re-encodes once a second for its age labels");
        hud.setWidgets(WidgetSettings.DEFAULT);
        hud.reset("ride");hud.accept("ride",state(0),500000);hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.SPEED,true).with(WidgetSettings.POWER,true));
        for(int i=0;i<20;i++)hud.acceptRide(new RideState.Snapshot(120+(i%4)*15,200+(i%3)*80,480000+i*1000L,480000+i*1000L));
        // stack() at 500000 starts the fade-in of the freshly enabled cards; the frame is taken once it has finished.
        var rideStack=hud.stack(500000);Bitmap rideFrame=frame(hud,500400);int speedGreens=greens(rideFrame,rideStack.speed())[0];
        check(rideStack.speed().height()==48&&rideStack.power().height()==48&&rideStack.voltage().right()==838&&rideStack.voltage().top()==276&&rideStack.speed().equals(box(443,420,633,468))&&rideStack.power().bottom()+6==rideStack.speed().top()&&speedGreens>40&&blocks(hud,rideStack.power().left()+50,rideStack.power().top()+24,500400),"speed and power cards draw their own curves, continue past the instrument in the left column with power above speed, and consume touches; green="+speedGreens);
        int powerLeft=(int)rideStack.power().left(),powerTop=(int)rideStack.power().top(),textRight=powerLeft+10+Math.round(metricColumn());
        hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.SPEED,true).with(WidgetSettings.POWER,true).with(WidgetSettings.POWER_KW,true));
        check(crop(frame(hud,500400),powerLeft,powerTop,textRight,powerTop+48).sameAs(crop(rideFrame,powerLeft,powerTop,textRight,powerTop+48)),"below the 1000 W threshold the kW-above option leaves the power value identical to watts mode");
        hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.SPEED,true).with(WidgetSettings.POWER,true).with(WidgetSettings.POWER_KW_ALWAYS,true));
        check(!crop(rideFrame,powerLeft,powerTop,textRight,powerTop+48).sameAs(crop(frame(hud,500400),powerLeft,powerTop,textRight,powerTop+48)),"the kW option rewrites the power value in its text column");
        hud.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.SPEED,true).with(WidgetSettings.POWER,true));
        hud.acceptRide(new RideState.Snapshot(120,-12000,500400,500400));
        int gapLeft=textRight,gapRight=gapLeft+METRIC_CHART_GAP;
        check(!textInk(frame(hud,500400),gapLeft,gapRight,powerTop,powerTop+48),"a six-digit watts value is squeezed into its text column instead of overrunning the chart gap into the curve");
        hud.setWidgets(WidgetSettings.DEFAULT);
        hud.reset("hold");hud.accept("hold",state(0),800000);hud.acceptTires(tireState());
        check(hud.stack(800000).tyres().right()==838&&hud.stack(800000).phone().right()==740&&!hud.hillHold(800000),"without hill hold the cards keep the right edge");
        hud.acceptRide(new RideState.Snapshot(0,168,799900,799900));
        check(!hud.hillHold(800000)&&hud.stack(800000).tyres().right()==838,"the hold condition must last the minimum time before the cards move");
        hud.acceptRide(new RideState.Snapshot(0,168,801900,801900));hud.hillHold(802000);
        hud.acceptRide(new RideState.Snapshot(0,168,803900,803900));
        var held=hud.stack(804000);
        check(hud.hillHold(804000)&&held.phone().right()==470&&held.tyres().right()==568&&held.phone().bottom()==468&&held.music().right()==568&&held.music().bottom()==414&&held.voltage().right()==838&&held.voltage().bottom()==366&&hud.animating(804100),"after three seconds the covered phone and tyre row and the music card slide left while voltage restacks on the toast, animated");
        Bitmap heldFrame=frame(hud,804400);
        check(heldFrame.getPixel(700,455)==BACKGROUND&&hud.touch(700,455,848,480,804400)==null&&bright(heldFrame,386,447,410,462)&&bright(heldFrame,484,447,560,462)&&!bright(heldFrame,780,447,830,462),"the area under the toast is left empty and the text of both halves moves with the row");
        hud.accept("hold",state(1,event(1,804500)),804500);hud.acceptRide(new RideState.Snapshot(0,168,805900,805900));
        var heldLifted=hud.stack(806000);
        check(hud.hillHold(806000)&&heldLifted.phone().right()==470&&heldLifted.tyres().right()==568&&heldLifted.phone().bottom()==402&&heldLifted.voltage().right()==838&&heldLifted.voltage().bottom()==366&&blocks(hud,500,440,806000)&&hud.touch(700,440,848,480,806000)==null,"a notification and the phone and tyre row it lifts into the toast slide left together while the rest restack on the toast");
        check(hud.hillHold(809000)&&hud.hillHold(809500)&&!hud.hillHold(810000)&&hud.stack(810000).tyres().right()==838&&hud.stack(810000).tyres().bottom()==402,"stale readings end the dodge only after the minimum time, then the lifted column returns to the right");
        // Display conditions and the column order.
        hud.reset("cond");hud.accept("cond",state(0),900000);hud.acceptTires(tireState());
        hud.setWidgets(WidgetSettings.DEFAULT.withCondition(WidgetSettings.PHONE,new WidgetCondition(WidgetCondition.WHILE,0,5,WidgetCondition.SPEED,5,30,0,3000,20,100,0,100)));
        check(hud.stack(900000).phone()==null&&hud.stack(900000).music()!=null,"a speed condition hides the phone card while no speed reading exists");
        hud.acceptRide(new RideState.Snapshot(120,300,900500,900500));
        check(hud.stack(900500).phone()!=null&&hud.stack(900500).phone().bottom()==468&&hud.stack(900500).music().bottom()==414,"a speed inside the range shows the card again");
        hud.acceptRide(new RideState.Snapshot(400,300,901000,901000));
        check(hud.stack(901000).phone()==null&&hud.revision(901000)!=hud.revision(900500),"a speed outside the range hides it and changes the revision");
        TireTelemetry freshTyres=new TireTelemetry();freshTyres.select("synthetic-vehicle");freshTyres.update("synthetic-vehicle",true,2.4f,29f,TireTelemetry.Source.BLUETOOTH,1789453320000L,901400);freshTyres.update("synthetic-vehicle",false,2.6f,30f,TireTelemetry.Source.BLUETOOTH,1789453320000L,901400);hud.acceptTires(freshTyres.snapshot());
        hud.setWidgets(WidgetSettings.DEFAULT.withCondition(WidgetSettings.PHONE,new WidgetCondition(WidgetCondition.WHILE,0,5,WidgetCondition.TYRE_FRONT_PRESSURE|WidgetCondition.TYRE_REAR_TEMP,0,160,0,30000,20,90,0,100,20,30,12,35,-20,100,-20,100)));
        check(hud.stack(901500).phone()!=null,"a front pressure of 2.4 bar inside 2.0 to 3.0 with an open rear temperature range shows the card");
        hud.setWidgets(WidgetSettings.DEFAULT.withCondition(WidgetSettings.PHONE,new WidgetCondition(WidgetCondition.WHILE,0,5,WidgetCondition.TYRE_FRONT_PRESSURE,0,160,0,30000,20,90,0,100,25,30,12,35,-20,100,-20,100)));
        check(hud.stack(901500).phone()==null,"a front pressure below 2.5 bar hides it");
        hud.setWidgets(WidgetSettings.DEFAULT.withCondition(WidgetSettings.TYRES,WidgetCondition.onChange(WidgetCondition.VOLUME_CHANGE,2)));
        Bundle condVolume=state(0);Bundle level=new Bundle();level.putInt("level",5);level.putInt("max",15);level.putLong("seq",1);condVolume.putBundle("volume",level);hud.accept("cond",condVolume,902000);
        check(hud.stack(902000).tyres()==null&&hud.stack(902000).phone()!=null,"a change condition keeps the tyre card hidden until its trigger fires");
        level.putInt("level",6);level.putLong("seq",2);hud.accept("cond",condVolume,902500);
        check(hud.stack(902500).tyres()!=null&&hud.stack(904499).tyres()!=null&&hud.stack(904500).tyres()==null,"a volume change shows the tyre card for the configured two seconds");
        hud.setWidgets(WidgetSettings.DEFAULT.withOrder(java.util.List.of(WidgetSettings.PHONE,WidgetSettings.NOTIFICATIONS,WidgetSettings.MUSIC)));
        hud.accept("cond",state(1,event(1,905000)),905000);
        var ordered=hud.stack(906000);
        check(ordered.phone().bottom()==468&&ordered.tyres().bottom()==468&&ordered.notificationBottom()==414&&ordered.voltage().bottom()==348&&blocks(hud,700,450,906000)&&blocks(hud,700,400,906000)&&hud.touch(700,417,848,480,906000)==null,"a phone and tyre row ordered below the notifications stays at the bottom while the notification block and the cards above it move up, with matching touch areas");
        Bitmap orderedFrame=frame(hud,906000);
        check(orderedFrame.getPixel(700,466)!=BACKGROUND&&orderedFrame.getPixel(700,417)==BACKGROUND&&orderedFrame.getPixel(700,400)!=BACKGROUND,"the frame draws the phone and tyre row at the bottom, a gap, then the notification above it");
        hud.setWidgets(WidgetSettings.DEFAULT.withOrder(java.util.List.of(WidgetSettings.NOTIFICATIONS,WidgetSettings.PHONE_TYRES,WidgetSettings.MUSIC,WidgetSettings.COLUMN_DIVIDER,WidgetSettings.VOLTAGE)));
        var twoColumns=hud.stack(906000);
        check(twoColumns.voltage().right()==633&&twoColumns.voltage().bottom()==402&&twoColumns.phone().bottom()==402&&blocks(hud,600,395,906400)&&hud.touch(500,460,848,480,906400)==null&&frame(hud,906400).getPixel(600,395)!=BACKGROUND,"a card moved to the left column sits left of the right column, rises above the notification and is drawn and touchable there once its move has settled");
        hud.setWidgets(WidgetSettings.DEFAULT.withCondition(WidgetSettings.NOTIFICATIONS,new WidgetCondition(WidgetCondition.WHILE,0,5,WidgetCondition.PLAYING,0,100,0,3000,20,100,0,100)));
        Bundle pausedCond=state(1,event(1,905000));pausedCond.getBundle("music").putInt("state",2);hud.accept("cond",pausedCond,907000);
        check(hud.stack(907000).phone().bottom()==468&&hud.summary(907000).contains("visible=1")&&frame(hud,907000).getPixel(700,417)==BACKGROUND,"notifications gated on playing music are held back while it is paused: the card stays queued, nothing is drawn where it would sit and the column is not lifted");
        // Phone status alone (tyres off): today's single-row card, untouched by the shared row.
        DashboardHud single=new DashboardHud();single.reset("single");single.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.TYRES,false));
        single.accept("single",state(0),100000);single.acceptBattery(batteryState());
        var alone=single.stack(100000);Bitmap aloneFrame=frame(single,100000);
        float batteryX=838-10-percentWidth("46%")-6-27,wifiCenterX=batteryX-8-22+11;int wifiColumn=Math.round(wifiCenterX);
        check(alone.phone().equals(box(648,440,838,468))&&alone.tyres()==null&&alone.music().bottom()+6==alone.phone().top(),"with tyres off two SIMs fill the full sidebar width in one 28-pixel row, music right above");
        check(bright(aloneFrame,655,447,672,462)&&!bright(aloneFrame,649,447,655,462),"with two SIMs the first SIM starts at the left edge of the card");
        Bundle small=state(0);Bundle smallPhone=small.getBundle("phone");smallPhone.putBoolean("wifi",false);smallPhone.putIntegerArrayList("slots",new ArrayList<>(Arrays.asList(1)));smallPhone.putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4)));
        single.accept("single",small,101000);
        check(single.stack(101000).phone().left()>700&&single.animating(101100)&&frame(single,101400).getPixel(660,454)==BACKGROUND&&single.touch(660,454,848,480,101000)==null,"phone status shrinks without Wi-Fi and a second SIM, animating its width");
        // Two SIMs without Wi-Fi or mobile data: the slot is simply empty; with a mobile generation the slot shows its text.
        Bundle noSlot=state(0);noSlot.getBundle("phone").putBoolean("wifi",false);single.accept("single",noSlot,101500);
        check(!bright(frame(single,101900),wifiColumn-9,447,wifiColumn+10,462),"without Wi-Fi or mobile data nothing is drawn in the slot");
        Bundle mobile=state(0);mobile.getBundle("phone").putBoolean("wifi",false);mobile.getBundle("phone").putString("network","5G");single.accept("single",mobile,102000);
        check(single.stack(102000).phone().left()==alone.phone().left()&&bright(frame(single,102400),wifiColumn-9,447,wifiColumn+10,462),"without Wi-Fi the mobile network type takes the Wi-Fi slot at the same width");
        long S=110000;single.accept("single",state(0),S);
        for(int i=1;i<=3;i++)single.accept("single",state(i,event(i,S+i*500)),S+i*500);
        int singleTop=Math.round(single.stack(S+2000).phone().top()),singleShift=Math.round(single.stack(S+2000).phone().right())-838;
        check(singleTop==440-3*NotificationTimeline.STEP&&singleShift==633-838,"three notifications lift the phone status by three slots, into the instrument zone, so it continues in the left column");
        // At the centre of the Wi-Fi glyph the three arcs and dot must form four separated bands.
        Bitmap singleSettled=frame(single,S+2400);int bands=0;boolean previous=false;
        for(int y=singleTop+4;y<=singleTop+24;y++){
            // The glyph centre may sit between pixel columns, so three neighbouring columns are combined.
            boolean bright=false;for(int x=wifiColumn+singleShift-1;x<=wifiColumn+singleShift+1;x++){int color=singleSettled.getPixel(x,y);bright|=Color.red(color)>160&&Color.green(color)>160;}
            if(bright&&!previous)bands++;previous=bright;
        }
        check(bands==4,"Wi-Fi must have three arcs and a dot, found "+bands+" bands");
        // The shared row: the swap, one wheel switched off, and the clock.
        DashboardHud row=new DashboardHud();row.reset("row");row.setWallClock(()->1789453350000L);row.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.TYRES_LEFT,true));
        row.accept("row",state(0),100000);row.acceptTires(tireState());
        var swappedRow=row.stack(100000);Bitmap swapFrame=frame(row,100000);
        check(swappedRow.tyres().equals(box(648,420,740,468))&&swappedRow.phone().equals(box(746,420,838,468))&&bright(swapFrame,656,447,730,461)&&bright(swapFrame,754,446,772,462),"the swap draws the tyres in the left half and the phone in the right one");
        row.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.TYRE_REAR,false));row.stack(101000);Bitmap rearOff=frame(row,101400);
        TireTelemetry frontOnly=new TireTelemetry();frontOnly.select("synthetic-vehicle");frontOnly.update("synthetic-vehicle",true,2.4f,29f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        row.setWidgets(WidgetSettings.DEFAULT);row.acceptTires(frontOnly.snapshot());Bitmap rearMissing=frame(row,101400);
        check(row.stack(101400).tyres().equals(box(746,420,838,468))&&bright(rearOff,754,447,830,461)&&crop(rearOff,746,420,838,468).sameAs(crop(rearMissing,746,420,838,468)),"with one wheel switched off both tyre rows stay and the off wheel reads -- like a wheel without data");
        DashboardHud clock=new DashboardHud();clock.reset("clock");long[] wall={1789453320000L+10000};clock.setWallClock(()->wall[0]);
        Bundle still=state(0);still.getBundle("music").putInt("state",1);clock.accept("clock",still,600000);clock.acceptTires(tireState());
        long first=clock.revision(600000);wall[0]+=40000;long sameMinute=clock.revision(600000);Bitmap before=frame(clock,600000);
        wall[0]+=20000;long nextMinute=clock.revision(600000);Bitmap after=frame(clock,600000);
        check(!clock.animating(600000)&&first==sameMinute&&sameMinute!=nextMinute&&!crop(before,648,420,700,442).sameAs(crop(after,648,420,700,442)),"a static paired row re-encodes once a minute for its clock and only then");
        clock.setWidgets(WidgetSettings.DEFAULT.with(WidgetSettings.TYRES,false));clock.stack(600000);
        long unpaired=clock.revision(601000);wall[0]+=60000;
        check(unpaired==clock.revision(601000),"without the shared row no clock is drawn and a new minute does not re-encode");
        // The secondary (non-data) SIM draws uniform 3-pixel cells at y 37-40 below the primary bars, which end at y 36 (DashboardHud PAIR_SIM_*).
        DashboardHud pairPhone=new DashboardHud();pairPhone.reset("pairphone");pairPhone.accept("pairphone",state(0),100000);
        var pairStack=pairPhone.stack(100000);int phoneLeft=(int)pairStack.phone().left(),phoneTop=(int)pairStack.phone().top();
        Bitmap pairFrame=frame(pairPhone,100000);int blankSim=pairFrame.getPixel(phoneLeft+45,phoneTop+2);
        int bar0=Math.round(phoneLeft+8+1),bar1=Math.round(phoneLeft+8+4.5f+1),bar2=Math.round(phoneLeft+8+9f+1),bar3=Math.round(phoneLeft+8+13.5f+1);
        int h0=span(pairFrame,bar0,phoneTop+37,phoneTop+43,blankSim),h1=span(pairFrame,bar1,phoneTop+37,phoneTop+43,blankSim),h2=span(pairFrame,bar2,phoneTop+37,phoneTop+43,blankSim);
        check(h0>0&&h0<=4&&h0==h1&&h1==h2,"the secondary SIM draws uniform cells, found bar heights "+h0+","+h1+","+h2);
        // The primary row's own bars graduate (2 to 8 pixels up to y 36) and sit just above the secondary row, with a one-pixel gap; the scan starts below the clock.
        int p0=span(pairFrame,bar0,phoneTop+22,phoneTop+36,blankSim),p3=span(pairFrame,bar3,phoneTop+22,phoneTop+36,blankSim);
        check(p0>0&&p3>p0,"the primary row's bars increase in height, found "+p0+" then "+p3);
        check(!bright(pairFrame,bar3,phoneTop+36,bar3+1,phoneTop+37),"a small gap separates the primary bars from the secondary row");
        // The primary row follows the default data SIM, not array order: swapping which slot holds the data SIM (and its level)
        // with the other slot's level swapped the same way must draw the identical bars and cells, just relabelled.
        DashboardHud dataSim=new DashboardHud();dataSim.reset("dataSim");
        Bundle primaryFirst=state(0);primaryFirst.getBundle("phone").putInt("data_slot",1);primaryFirst.getBundle("phone").putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4,1)));
        dataSim.accept("dataSim",primaryFirst,100000);Bitmap firstFrame=frame(dataSim,100000);
        Bundle primarySecond=state(0);primarySecond.getBundle("phone").putInt("data_slot",2);primarySecond.getBundle("phone").putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(1,4)));
        dataSim.accept("dataSim",primarySecond,100000);Bitmap secondFrame=frame(dataSim,100000);
        var dataStack=dataSim.stack(100000);int dataLeft=(int)dataStack.phone().left(),dataTop=(int)dataStack.phone().top();
        check(crop(firstFrame,dataLeft,dataTop+22,dataLeft+25,dataTop+48).sameAs(crop(secondFrame,dataLeft,dataTop+22,dataLeft+25,dataTop+48)),"the taller primary bars follow the data SIM's level, not its position in slots");
        // The slot number is centred at x+4*4.5+6 = left+32, on the lower row (y 34), clear of the bars that end at left+24.5.
        check(bright(firstFrame,dataLeft+26,dataTop+27,dataLeft+40,dataTop+42)&&bright(secondFrame,dataLeft+26,dataTop+27,dataLeft+40,dataTop+42),"a single large slot number is drawn to the right of the bars");
        // Single SIM: no secondary row, so the primary bars (3 to 12 pixels up to y 40) fill the whole glyph height with no gap, taller than the paired primary.
        Bundle soloState=state(0);Bundle soloPhone=soloState.getBundle("phone");soloPhone.putIntegerArrayList("slots",new ArrayList<>(Arrays.asList(1)));soloPhone.putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4)));
        DashboardHud solo=new DashboardHud();solo.reset("solo");solo.accept("solo",soloState,100000);
        var soloStack=solo.stack(100000);int soloLeft=(int)soloStack.phone().left(),soloTop=(int)soloStack.phone().top();
        Bitmap soloFrame=frame(solo,100000);int soloBlank=soloFrame.getPixel(soloLeft+45,soloTop+2);
        int soloBar3=Math.round(soloLeft+8+13.5f+1);
        int soloSpan=span(soloFrame,soloBar3,soloTop+22,soloTop+46,soloBlank);
        check(soloSpan>p3,"a single SIM's bars must be taller than the paired primary's bars alone, found "+soloSpan+" vs "+p3);
        check(bright(soloFrame,soloBar3,soloTop+36,soloBar3+1,soloTop+37),"a single SIM fills the gap that the paired primary and secondary leave empty");
        // An unknown data-SIM signal (-1) must look different from a zero signal (0): the bars are the same all-grey shape in
        // both cases, but the number shows "?" instead of the slot digit.
        DashboardHud sig=new DashboardHud();sig.reset("sig");
        Bundle unknownData=state(0);unknownData.getBundle("phone").putInt("data_slot",2);unknownData.getBundle("phone").putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4,-1)));
        sig.accept("sig",unknownData,100000);Bitmap unknownFrame=frame(sig,100000);
        Bundle zeroData=state(0);zeroData.getBundle("phone").putInt("data_slot",2);zeroData.getBundle("phone").putIntegerArrayList("levels",new ArrayList<>(Arrays.asList(4,0)));
        sig.accept("sig",zeroData,100000);Bitmap zeroFrame=frame(sig,100000);
        var sigStack=sig.stack(100000);int sigLeft=(int)sigStack.phone().left(),sigTop=(int)sigStack.phone().top();
        check(!crop(unknownFrame,sigLeft,sigTop,sigLeft+92,sigTop+48).sameAs(crop(zeroFrame,sigLeft,sigTop,sigLeft+92,sigTop+48)),"an unknown data-SIM signal must be distinguishable from a zero signal in the pair");
        // Front and rear tyre rows right-align their ℃ at the same x regardless of how wide each temperature is, so
        // differently sized temperatures (front one digit, rear two) would expose a left-aligned regression; columns are
        // predicted with DashboardHud's own column math (TyreColumns) rather than a guessed pixel window.
        TireTelemetry alignTires=new TireTelemetry();alignTires.select("synthetic-vehicle");
        alignTires.update("synthetic-vehicle",true,2.4f,9f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        alignTires.update("synthetic-vehicle",false,2.6f,30f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        DashboardHud align=new DashboardHud();align.reset("align");align.acceptTires(alignTires.snapshot());align.accept("align",state(0),100000);
        var alignStack=align.stack(100000);float alignLeft=alignStack.tyres().left(),alignRight=alignStack.tyres().right();
        int tyresRight=(int)alignRight,tyresTop=(int)alignStack.tyres().top();
        Bitmap alignFrame=frame(align,100000);int blankTyre=alignFrame.getPixel(tyresRight-10,tyresTop+24);
        TyreColumns alignCols=tyreColumns(alignLeft,alignRight,"2.4","2.6","9","30");
        int barRightScreen=alignCols.screenX(alignCols.barRight());
        int frontSplit=alignCols.screenX(alignCols.unitX()-1-measureText("9",14,true)),rearSplit=alignCols.screenX(alignCols.unitX()-1-measureText("30",14,true));
        int topRight=rightInk(alignFrame,barRightScreen+1,tyresRight-1,tyresTop+5,tyresTop+23,blankTyre);
        int bottomRight=rightInk(alignFrame,barRightScreen+1,tyresRight-1,tyresTop+25,tyresTop+43,blankTyre);
        check(topRight>0&&Math.abs(topRight-bottomRight)<=1,"the front and rear rows right-align their ℃ at the same x, found "+topRight+" vs "+bottomRight);
        int topLeft=leftInk(alignFrame,frontSplit,tyresRight-1,tyresTop+5,tyresTop+23,blankTyre);
        int bottomLeft=leftInk(alignFrame,rearSplit,tyresRight-1,tyresTop+25,tyresTop+43,blankTyre);
        check(topLeft>0&&bottomLeft>0&&topLeft!=bottomLeft,"a left-aligned temperature would start both rows at the same x even with different widths; found "+topLeft+" vs "+bottomLeft);
        // Reviewer's overflow case: a wide front temperature forces the squeeze; bar and temperature must still not overlap,
        // and the squeezed ℃ right edge must still land on the card's real inner right edge (right-TYRE_INSET).
        TireTelemetry overflowTires=new TireTelemetry();overflowTires.select("synthetic-vehicle");
        overflowTires.update("synthetic-vehicle",true,2.4f,-15f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        overflowTires.update("synthetic-vehicle",false,2.6f,30f,TireTelemetry.Source.BLUETOOTH,1789453320000L,99000);
        DashboardHud overflow=new DashboardHud();overflow.reset("overflow");overflow.acceptTires(overflowTires.snapshot());overflow.accept("overflow",state(0),100000);
        var overflowStack=overflow.stack(100000);float overflowLeft=overflowStack.tyres().left(),overflowRight=overflowStack.tyres().right();
        int oTyresRight=(int)overflowRight,oTyresTop=(int)overflowStack.tyres().top();
        Bitmap overflowFrame=frame(overflow,100000);int oBlank=overflowFrame.getPixel(oTyresRight-10,oTyresTop+24);
        TyreColumns overflowCols=tyreColumns(overflowLeft,overflowRight,"2.4","2.6","-15","30");
        check(overflowCols.scale()<1,"this case must actually force the squeeze (reviewer's scenario), scale="+overflowCols.scale());
        int oFrontSplit=overflowCols.screenX(overflowCols.unitX()-1-measureText("-15",14,true)),oRearSplit=overflowCols.screenX(overflowCols.unitX()-1-measureText("30",14,true));
        int oTopBarRight=rightInk(overflowFrame,(int)overflowLeft,oFrontSplit,oTyresTop+5,oTyresTop+23,oBlank);
        int oTopTempLeft=leftInk(overflowFrame,oFrontSplit,oTyresRight-1,oTyresTop+5,oTyresTop+23,oBlank);
        int oBottomBarRight=rightInk(overflowFrame,(int)overflowLeft,oRearSplit,oTyresTop+25,oTyresTop+43,oBlank);
        int oBottomTempLeft=leftInk(overflowFrame,oRearSplit,oTyresRight-1,oTyresTop+25,oTyresTop+43,oBlank);
        check(oTopBarRight>0&&oTopTempLeft>0&&oTopBarRight<oTopTempLeft&&oBottomBarRight>0&&oBottomTempLeft>0&&oBottomBarRight<oBottomTempLeft,
            "bar and temperature must not overlap even when squeezed, found front bar/temp "+oTopBarRight+"/"+oTopTempLeft+", rear "+oBottomBarRight+"/"+oBottomTempLeft);
        int oTopRight=rightInk(overflowFrame,oFrontSplit,oTyresRight-1,oTyresTop+5,oTyresTop+23,oBlank);
        int oBottomRight=rightInk(overflowFrame,oRearSplit,oTyresRight-1,oTyresTop+25,oTyresTop+43,oBlank);
        check(Math.abs(oTopRight-(oTyresRight-8))<=2&&Math.abs(oBottomRight-(oTyresRight-8))<=2,"the squeezed ℃ right edge must land on the card's real inner right edge, found "+oTopRight+" and "+oBottomRight+" vs "+(oTyresRight-8));
        // Light dashboard theme: the same data on the light palette, appended below the dark comparison image.
        DashboardHud light=new DashboardHud();light.reset("light");light.setDark(false);light.acceptTires(tireState());light.acceptBattery(batteryState());
        light.accept("light",state(0),100000);light.simulate(100300);light.simulate(100350);light.stack(100400);light.stack(101000);
        Bitmap lightFrame=Bitmap.createBitmap(848,480,Bitmap.Config.ARGB_8888);Canvas lightCanvas=new Canvas(lightFrame);lightCanvas.drawColor(0xffe6eaee);light.draw(lightCanvas,848,480,101500);
        var lightStack=light.stack(101500);int lightSurface=lightFrame.getPixel((int)lightStack.music().right()-4,(int)lightStack.music().top()+32);
        check(!light.dark()&&Color.red(lightSurface)>230&&Color.green(lightSurface)>230&&Color.blue(lightSurface)>230,"light theme paints cards on a near-white surface, got #"+Integer.toHexString(lightSurface)+" box="+lightStack.music()+" notifications="+light.summary(101500));
        Bitmap darkImage=android.graphics.BitmapFactory.decodeFile(args[0]);Bitmap both=Bitmap.createBitmap(848,darkImage.getHeight()+480,Bitmap.Config.ARGB_8888);
        Canvas bothCanvas=new Canvas(both);bothCanvas.drawBitmap(darkImage,0,0,null);bothCanvas.drawBitmap(lightFrame,0,darkImage.getHeight(),null);
        try(FileOutputStream out=new FileOutputStream(args[0])){both.compress(Bitmap.CompressFormat.PNG,100,out);}
        System.out.println("PASS: "+checks+" Android HUD checks");
    }
}
