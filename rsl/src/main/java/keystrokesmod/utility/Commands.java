package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.profile.Profile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean f = true;
    private static final List<Integer> cs = Arrays.asList((new Color(170, 107, 148, 50)).getRGB(), (new Color(122, 158, 134, 50)).getRGB(), (new Color(16, 16, 16, 50)).getRGB(), (new Color(64, 114, 148, 50)).getRGB());
    private static int ccs = 0;
    private static int lccs = -1;
    public static List<String> rs = new ArrayList();
    private static final String invSyn = "&cInvalid syntax.";
    private static final String invCom = "&cInvalid command.";

    public static void rCMD(String c) {
        if (!c.isEmpty()) {
            String cm = c.toLowerCase();
            boolean hasArgs = c.contains(" ");
            String[] args = hasArgs ? c.split(" ") : null;
            String n;
            if (cm.startsWith("ping")) {
                Ping.checkPing(false);
            }
            else if (cm.startsWith("clear")) {
                rs.clear();
            }
            else if (cm.startsWith("hide")) {
                if (!hasArgs || args.length != 2) {
                    print(invSyn, 1);
                    return;
                }

                for (Module module : Raven.getModuleManager().getModules()) {
                    String name = module.getName().toLowerCase().replace(" ", "");
                    if (name.equals(args[1].toLowerCase())) {
                        module.setHidden(true);
                        print("&a" + module.getName() + " is now hidden in HUD", 1);
                    }
                }
            }
            else if (cm.startsWith("show")) {
                if (!hasArgs) {
                    print(invSyn, 1);
                    return;
                }

                if (args.length != 2) {
                    print(invSyn, 1);
                    return;
                }

                for (Module module : Raven.getModuleManager().getModules()) {
                    String name = module.getName().toLowerCase().replace(" ", "");
                    if (name.equals(args[1].toLowerCase())) {
                        module.setHidden(false);
                        print("&a" + module.getName() + " is now visible in HUD", 1);
                    }
                }
            }
            else if (cm.startsWith("friend") || cm.startsWith("f")) {
                if (!hasArgs) {
                    print(invSyn, 1);
                    return;
                }

                if (args.length != 2) {
                    print(invSyn, 1);
                    return;
                }

                if (args[1].equals("clear")) {
                    Utils.friends.clear();
                    print("&aFriends cleared.", 1);
                    return;
                }

                boolean added = Utils.addFriend(args[1]);
                if (added) {
                    print("&aAdded friend: " + args[1], 1);
                } else {
                    print("&aRemoved friend: " + args[1], 1);
                    Utils.removeFriend(args[1]);
                }
            }
            else if (cm.startsWith("enemy") || cm.startsWith("e")) {
                if (!hasArgs) {
                    print(invSyn, 1);
                    return;
                }

                if (args.length != 2) {
                    print(invSyn, 1);
                    return;
                }

                if (args[1].equals("clear")) {
                    Utils.enemies.clear();
                    print("&aEnemies cleared.", 1);
                    return;
                }

                boolean added = Utils.addEnemy(args[1]);
                if (!added) {
                    print("&aRemoved enemy: " + args[1], 1);
                    Utils.removeEnemy(args[1]);
                }
                else {
                    print("&aAdded enemy: " + args[1], 1);
                }
            }
            else if (cm.startsWith("Debug".toLowerCase())) {
                Raven.debug = !Raven.debug;
                print("Debug " + (Raven.debug ? "enabled" : "disabled") + ".", 1);
            }
            else if (cm.startsWith("profiles") || cm.startsWith("p")) {
                if (!hasArgs) {
                    print("&aAvailable profiles:", 1);
                    if (Raven.profileManager.profiles.isEmpty()) {
                        print("None", 0);
                        return;
                    }
                    for (int i = 0; i < Raven.profileManager.profiles.size(); ++i) {
                        print(i + 1 + ". " + Raven.profileManager.profiles.get(i).getName(), 0);
                    }
                }
                else if (args.length > 1) {
                    if (args[1].equals("save") || args[1].equals("s")) {
                        if (args.length != 3) {
                            print(invSyn, 1);
                            return;
                        }
                        String name = args[2];
                        if (name.length() < 2 || name.length() > 10) {
                            print("&cInvalid name.", 1);
                            return;
                        }
                        Raven.profileManager.saveProfile(new Profile(name, 0));
                        print("&aSaved profile:", 1);
                        print(name, 0);
                        Raven.profileManager.loadProfiles();
                    }
                    else if (args[1].equals("load") || args[1].equals("l")) {
                        if (args.length != 3) {
                            print(invSyn, 1);
                            return;
                        }
                        String name = args[2];
                        for (Profile profile : Raven.profileManager.profiles) {
                            if (profile.getName().equals(name)) {
                                Raven.profileManager.loadProfile(profile.getName());
                                print("&aLoaded profile:", 1);
                                print(name, 0);
                                if (Settings.sendMessage.isToggled()) {
                                    Utils.sendMessage("&7Enabled profile: &b" + name);
                                }
                                return;
                            }
                        }
                        print("&cInvalid profile.", 1);
                    }
                    else if (args[1].equals("remove") || args[1].equals("r")) {
                        if (args.length != 3) {
                            print(invSyn, 1);
                            return;
                        }
                        String name = args[2];
                        for (Profile profile : Raven.profileManager.profiles) {
                            if (profile.getName().equals(name)) {
                                Raven.profileManager.deleteProfile(profile.getName());
                                print("&aRemoved profile:", 1);
                                print(name, 0);
                                Raven.profileManager.loadProfiles();
                                return;
                            }
                        }
                        print("&cInvalid profile.", 1);
                    }
                }
            } else if (!cm.startsWith("help") && !cm.startsWith("?")) {
                if (cm.startsWith("shoutout")) {
                    print("&eCelebrities:", 1);
                    print("- hevex", 0);
                    print("- jc", 0);
                    print("- mood", 0);
                    print("- charlotte", 0);
                } else {
                    print(invCom + " (" + (cm.length() > 5 ? cm.substring(0, 5) + "..." : cm) + ")", 1);
                }
            } else {
                print("&eAvailable commands:", 1);
                print("2 friend/enemy [name/clear]", 0);
                print("5 ping", 0);
                print("6 hide/show [module]", 0);
                print("&eProfiles:", 0);
                print("1 profiles", 0);
                print("2 profiles save [profile]", 0);
                print("3 profiles load [profile]", 0);
                print("4 profiles remove [profile]", 0);
            }

        }
    }

    public static void print(String m, int t) {
        if (t == 1 || t == 2) {
            rs.add("");
        }

        rs.add(m);
        if (t == 2 || t == 3) {
            rs.add("");
        }
    }

    public static void rc(FontRenderer fr, int h, int w, double s) {
        int x = w - 195;
        int y = h - 130;
        int sY = h - 345;
        int sH = 230;
        GL11.glEnable(3089);
        double mw = w * s;
        GL11.glScissor(0, (int) (mc.displayHeight - (sY + sH) * s), (int) (mw - (mw < 2 ? 0 : 2)), (int) (sH * s - 2));
        RenderUtils.db(1000, 1000, ccs);
        rss(fr, rs, x, y);
        GL11.glDisable(3089);
    }

    private static void rss(FontRenderer fr, List<String> rs, int x, int y) {
        if (f) {
            f = false;
            print("Welcome,", 0);
            print("Use \"help\" for help.", 0);
        }

        if (!rs.isEmpty()) {
            for (int i = rs.size() - 1; i >= 0; --i) {
                String s = rs.get(i);
                int c = -1;
                if (s.contains("&a")) {
                    s = s.replace("&a", "");
                    c = Color.green.getRGB();
                } else if (s.contains("&c")) {
                    s = s.replace("&c", "");
                    c = Color.red.getRGB();
                } else if (s.contains("&e")) {
                    s = s.replace("&e", "");
                    c = Color.yellow.getRGB();
                }

                fr.drawString(s, x, y, c);
                y -= fr.FONT_HEIGHT + 5;
            }

        }
    }

    public static void setccs() {
        int val = Utils.getRandom().nextInt(cs.size());
        if (val == lccs) {
            val += val == 3 ? -3 : 1;
        }

        lccs = val;
        ccs = cs.get(val);
    }

    public static void onDisable() {
        Ping.reset(false);
    }
}
