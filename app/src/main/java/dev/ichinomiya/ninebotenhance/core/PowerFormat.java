package dev.ichinomiya.ninebotenhance.core;

/** Power card text: whole watts, or kilowatts with one decimal; the 1000 W threshold is judged on the magnitude. */
public final class PowerFormat {
    public static final int WATTS=0,KW_ABOVE=1,KW_ALWAYS=2;
    public static boolean kilowatts(int watts,int mode){return mode==KW_ALWAYS||mode==KW_ABOVE&&Math.abs(watts)>=1000;}
    /** Kilowatts round half away from zero to 100 W and never read "-0.0". */
    public static String value(int watts,int mode){
        if(!kilowatts(watts,mode))return String.valueOf(watts);
        long tenths=Math.round(Math.abs(watts)/100.0);
        return (watts<0&&tenths>0?"-":"")+tenths/10+"."+tenths%10;
    }
    public static String unit(int watts,int mode){return kilowatts(watts,mode)?"kW":"W";}
    private PowerFormat(){}
}
