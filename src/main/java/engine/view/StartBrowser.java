package engine.view;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class StartBrowser {

    public static void startBrowser(String url) {

        String os = System.getProperty("os.name").toLowerCase(); // получаем имя операционной системы
        Runtime rt = Runtime.getRuntime();
        try {
            if (os.contains("win")) {
                // не поддерживаются ссылки формата "page.html#someTag"
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url); // если windows, открываем url через командную строку
            } else if (os.contains("mac")) {
                rt.exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                // nix системы
                String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"};
                // Формируем строку с вызовом всем браузеров через логическое ИЛИ в shell консоли
                // "browser0 "URI" || browser1 "URI" ||..."
                StringBuilder cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++)
                    cmd.append(i == 0 ? "" : " || ").append(browsers[i]).append(" \"").append(url).append("\" ");
                rt.exec(new String[]{"sh", "-c", cmd.toString()});
            } else {
                return;
            }
        } catch (Exception e) {
            // игнорируем все ошибки
        }
    }

    public static void openBrowser(String url) throws URISyntaxException {
        Desktop desktop;
        try {
            desktop = Desktop.getDesktop();
        } catch (Exception ex) {
            System.err.println("Класс Desktop не поддерживается.");
            return;
        }
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            System.err.println("BROWSE: операция не поддерживается..");
            return;
        }
        // если все ок пытаемся открыть ссылку
        try {
            desktop.browse(new URL(url).toURI());
        } catch (IOException ex) {
            System.err.println("Failed to browse. " + ex.getLocalizedMessage());
        }
    }

}
