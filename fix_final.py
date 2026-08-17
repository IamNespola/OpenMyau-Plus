import re
import os

def fix_file(path, replacements):
    with open(path, 'r') as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, 'w') as f:
        f.write(content)

# BedESP
fix_file('/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/BedESP.java', [
    ('color.getRed(), color.getGreen(), color.getBlue()', '(color >> 16 & 0xFF), (color >> 8 & 0xFF), (color & 0xFF)'),
    ('color.getRed(),\n                                color.getGreen(),\n                                color.getBlue()', '(color >> 16 & 0xFF),\n                                (color >> 8 & 0xFF),\n                                (color & 0xFF)')
])

# LagRange
fix_file('/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/LagRange.java', [
    ('int color = new Color(-1);', 'int color = -1;'),
    ('color = TeamUtil.getTeamColor(mc.thePlayer, 1.0F);', 'color = TeamUtil.getTeamColor(mc.thePlayer, 1.0F).getRGB();'),
    ('color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());', 'color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());'),
    ('RenderUtil.drawFilledBox(aabb, color.getRed(), color.getGreen(), color.getBlue());', 'RenderUtil.drawFilledBox(aabb, (color >> 16 & 0xFF), (color >> 8 & 0xFF), (color & 0xFF));')
])

# AntiFireball
fix_file('/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/AntiFireball.java', [
    ('int color = new Color(-1);', 'int color = -1;'),
    ('color = new Color(16733525);', 'color = 16733525;'),
    ('color = new Color(5635925);', 'color = 5635925;'),
    ('RenderUtil.drawEntityBox(this.target, color.getRed(), color.getGreen(), color.getBlue());', 'RenderUtil.drawEntityBox(this.target, (color >> 16 & 0xFF), (color >> 8 & 0xFF), (color & 0xFF));')
])

# BedNuker
fix_file('/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/BedNuker.java', [
    ('int r = color.getRed();', 'int r = (color >> 16 & 0xFF);'),
    ('int g = color.getBlue();', 'int g = (color & 0xFF);'),
    ('int b = color.getGreen();', 'int b = (color >> 8 & 0xFF);')
])

# Hotbar
fix_file('/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/Hotbar.java', [
    ('c1 = hud.getColor(currentTime);', 'c1 = hud.getColor(currentTime);'),
    ('c2 = hud.getColor(currentTime + 500);', 'c2 = hud.getColor(currentTime + 500);')
])

# The modules that have "public Color getColor(" remaining
def change_method(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    content = content.replace('public Color getColor', 'public int getColor')
    with open(filepath, 'w') as f:
        f.write(content)

for mod in ['Statistics', 'TargetHUD', 'ESP2D', 'BedNuker', 'LagRange', 'BedESP']:
    change_method(f'/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/{mod}.java')

