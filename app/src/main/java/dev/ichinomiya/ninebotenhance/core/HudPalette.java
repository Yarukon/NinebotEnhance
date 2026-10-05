package dev.ichinomiya.ninebotenhance.core;

/**
 * Colours of the module's dashboard cards for the two dashboard themes. The vehicle dashboard paints its own status bar and
 * instrument card light or dark according to the TFT day/night flag ({@link DashboardTheme}); the cards follow the same choice.
 */
public record HudPalette(boolean dark,int surface,int border,int text,int label,int unit,int accent,int chartFill,int remaining,int track,int divider,
                         int iconBox,int iconGlyph,int artBox,int artGlyph,int pauseBackdrop,int pauseGlyph,int body,int dim,int icon,int signalOff,
                         int batteryLow,int batteryCharging,int bolt,int boltOutline,int percent,int probePending,int levelLow){
    public static final HudPalette DARK=new HudPalette(true,0xf5172027,0x7a52636c,0xfff4f8fa,0xffa6b6c2,0xff8fa1ad,0xff7cd6a4,0x337cd6a4,0x887cd6a4,0xff40515b,0x5552636c,
            0xff526776,0xffffffff,0xff31434e,0xff9fb9c6,0x99000000,0xffffffff,0xffcfdae2,0xff9db1bf,0xffe0eaf1,0xff4b5a66,
            0xffffbc72,0xffb6f681,0xffffffff,0xff172027,0xffe8f0f5,0xffffc857,0xffff5a5f);
    public static final HudPalette LIGHT=new HudPalette(false,0xf5f7f9fb,0x66a7b3bd,0xff16212b,0xff5b6c79,0xff75858f,0xff1f9a5c,0x331f9a5c,0x881f9a5c,0xffd3dbe1,0x55a7b3bd,
            0xff9fb0bd,0xffffffff,0xffdde4ea,0xff6f8090,0x99ffffff,0xff16212b,0xff3b4a56,0xff6b7b87,0xff2a3843,0xffc6cfd6,
            0xffd97b1f,0xff2e9e4f,0xffffffff,0xff16212b,0xff16212b,0xffc98a00,0xffe5484d);
    public static HudPalette of(boolean dark){return dark?DARK:LIGHT;}
}
