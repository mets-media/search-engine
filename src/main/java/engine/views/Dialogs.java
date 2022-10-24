package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import engine.entity.Site;
import lombok.Getter;

import java.util.Optional;

public class Dialogs {
    private static Boolean result;

    public static boolean getDialodResult() {
        return result;
    }
    public static void showConfirmDialog(String message, String confirmCaption, String cancelCaption) {
        boolean result = false;
        Dialog dialog = new Dialog();
        Button confirm = new Button(confirmCaption);
        Button cancel = new Button(cancelCaption);

        dialog.add(message);
        dialog.add(confirm);
        dialog.add(cancel);
        confirm.addClickListener(clickEvent -> {

            dialog.close();
            Notification notification = new Notification(confirmCaption, 500);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
            Dialogs.result = true;
        });
        cancel.addClickListener(clickEvent -> {
            dialog.close();
            Notification notification = new Notification(cancelCaption, 500);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();

            Dialogs.result = false;
        });
        dialog.open();
    }
}
