package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.utility.Debugger;
import keystrokesmod.utility.command.Command;

public class Debug extends Command {
    public Debug() {
        super("debug");
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length <= 1) {
            Raven.debug = !Raven.debug;
            chatWithPrefix("&7Debug " + (Raven.debug ? "&aenabled" : "&cdisabled") + "&7.");
        }
        else if (args.length == 2) {
            if (args[1].equals("mixin")) {
                Debugger.MIXIN = !Debugger.MIXIN;
                chatWithPrefix("&dMixin &7debug " + (Debugger.MIXIN ? "&aenabled" : "&cdisabled") + "&7.");
            }
            else if (args[1].equals("bg") || args[1].equals("background")) {
                Debugger.BACKGROUND = !Debugger.BACKGROUND;
                chatWithPrefix("&6Background &7debug " + (Debugger.BACKGROUND ? "&aenabled" : "&cdisabled") + "&7.");
            }
            else {
                syntaxError();
            }
        }
        else {
            syntaxError();
        }
    }
}
