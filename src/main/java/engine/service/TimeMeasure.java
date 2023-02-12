package engine.service;

import java.util.concurrent.ConcurrentHashMap;

import static engine.view.UIElement.showMessage;

public class TimeMeasure {
    private static final ConcurrentHashMap<String, Long> timers = new ConcurrentHashMap<>();
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
            return (timeMillis / 3_600_000) + " ч. " +  ((timeMillis % 3_600_000) / 60_000) + " мин.";}
        if (timeMillis >= 60_000) {
            return (timeMillis / 60_000) + "." + (timeMillis % 60_000) / 1_000 + " мин.";
        } if (timeMillis >= 1_000) {
            return (timeMillis / 1_000) + "." + (timeMillis % 1_000)  + " сек.";
        } else
            return timeMillis  + " м.сек.";
    }
    public static String getStringExperienceTime() {
        return getNormalizedTime(System.currentTimeMillis() - startTime);
    }

    public static void startTimer(String timerName) {
        timers.put(timerName, System.currentTimeMillis());
    }

    public static String getTimerValue(String timerName) {
        if (timers.contains(timerName))
           return getNormalizedTime(timers.get(timerName).longValue());
        else
            return "Таймер " + timerName + " не найден";
    }
    public static void removeTimer(String timerName) {
        timers.remove(timerName);
    }
    public static void timeSpentNotification(String text) {
        showMessage(text + TimeMeasure.getStringExperienceTime());
    }
}
