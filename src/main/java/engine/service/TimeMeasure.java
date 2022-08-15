package engine.service;
        

public class TimeMeasure {
    private static long startTime;

    public static void setStartTime() {
        startTime = System.currentTimeMillis();
    }

    public static long getStartTime() {
        return startTime;
    }
    public static long getExperienceTime() {
        return System.currentTimeMillis() - startTime;
    }

    public static String getNormalizedTime(long timeMillis) {
        if (timeMillis >= 3_600_000) {
            return (timeMillis / 3_600_000) + " ч. " +  ((timeMillis % 3_600_000) / 60_000) + " мин.";
       }
        if (timeMillis >= 60_000) {
            return (timeMillis / 60_000) + "." + (timeMillis % 60_000) / 1_000 + " мин.";
        } if (timeMillis >= 1_000) {
            return (timeMillis / 1_000) + "." + (timeMillis % 1_000)  + " сек.";
        } else
            return timeMillis  + " м.сек.";
    }
}
