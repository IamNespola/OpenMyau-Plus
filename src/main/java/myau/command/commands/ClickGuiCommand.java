package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.module.modules.render.ClickGUIModule;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ClickGuiCommand extends Command {

    public ClickGuiCommand() {
        super(new ArrayList<>(Arrays.asList("clickgui", "gui")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ClickGUIModule guiModule = (ClickGUIModule) Myau.moduleManager.getModule("ClickGUI");

        if (guiModule == null) {
            ChatUtil.sendFormatted(String.format("%sClickGUI module not found!", Myau.clientName));
            return;
        }

        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&oopen&r/&osave&r/&oscale&r/&ocorner&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
            return;
        }

        String subCommand = args.get(1).toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "open":
                guiModule.toggle();
                ChatUtil.sendFormatted(String.format("%sClickGUI %s&r", Myau.clientName, guiModule.isEnabled() ? "&aopened" : "&cclosed"));
                break;
            case "save":
                guiModule.saveGuiState.setValue(true);
                ChatUtil.sendFormatted(String.format("%sClickGUI state saving %s&r", Myau.clientName, guiModule.saveGuiState.getValue() ? "&aenabled" : "&cdisabled"));
                break;
            case "scale":
                if (args.size() < 3) {
                    ChatUtil.sendFormatted(String.format("%sUsage: .%s scale <&oscale_factor&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
                    return;
                }
                try {
                    float scale = Float.parseFloat(args.get(2));
                    guiModule.windowWidth.setValue((int) (600 * scale));
                    guiModule.windowHeight.setValue((int) (400 * scale));
                    ChatUtil.sendFormatted(String.format("%sClickGUI scale set to &o%s&r", Myau.clientName, scale));
                } catch (NumberFormatException e) {
                    ChatUtil.sendFormatted(String.format("%sInvalid scale value (&o%s&r)&r", Myau.clientName, args.get(2)));
                }
                break;
            case "corner":
                if (args.size() < 3) {
                    ChatUtil.sendFormatted(String.format("%sUsage: .%s corner <&oradius&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
                    return;
                }
                try {
                    float radius = Float.parseFloat(args.get(2));
                    guiModule.cornerRadius.setValue(radius);
                    ChatUtil.sendFormatted(String.format("%sClickGUI corner radius set to &o%s&r", Myau.clientName, radius));
                } catch (NumberFormatException e) {
                    ChatUtil.sendFormatted(String.format("%sInvalid corner radius value (&o%s&r)&r", Myau.clientName, args.get(2)));
                }
                break;
            default:
                ChatUtil.sendFormatted(String.format("%sInvalid argument (&o%s&r)&r", Myau.clientName, args.get(1)));
                break;
        }
    }
}
