package keystrokesmod.script.classes;

import keystrokesmod.utility.Utils;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

public class Message {
    public ChatComponentText component;

    public Message(String message) {
        this.component = new ChatComponentText(message);
    }

    public void appendStyle(String style, String action, String styleMessage, String message) {
        ChatStyle chatStyle = new ChatStyle();
        if (style.equals("HOVER")) {
            chatStyle.setChatHoverEvent(new HoverEvent(Utils.getEnum(HoverEvent.Action.class, action), new ChatComponentText(styleMessage)));
        }
        else if (style.equals("CLICK")) {
            chatStyle.setChatClickEvent(new ClickEvent(Utils.getEnum(ClickEvent.Action.class, action), styleMessage));
        }
        component.appendSibling(new ChatComponentText(message).setChatStyle(chatStyle));
    }

    public void append(String append) {
        component.appendSibling(new ChatComponentText(append));
    }
}