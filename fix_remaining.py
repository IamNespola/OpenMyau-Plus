import re
import os
import glob

# 1. CheadleClickGui
path = '/home/leviet/OpenMyau-Plus/src/main/java/myau/ui/impl/clickgui/cheadle/CheadleClickGui.java'
with open(path, 'r') as f: content = f.read()
# Replace .getColor(...) where it's expected to be Color with new Color(hud.getColor(...))
content = re.sub(r'(\.getColor\([^)]*\)\s*,\s*offset\.get\(\)\))', r'new Color(\1)', content)
with open(path, 'w') as f: f.write(content)

# 2. Modules returning Color
def change_method_return_type(filepath):
    with open(filepath, 'r') as f: content = f.read()
    # Change "public Color getColor(" to "public int getColor(" or similar if it's the method failing
    if 'public Color ' in content:
        content = content.replace('public Color get', 'public int get')
    if 'Color color =' in content and 'getColor(System' in content:
        content = content.replace('Color color =', 'int color =')
    if 'Color color =' in content and 'hud.getColor' in content:
        content = content.replace('Color color =', 'int color =')
    with open(filepath, 'w') as f: f.write(content)

modules_to_fix = [
    'BedESP', 'LagRange', 'Hotbar', 'Statistics', 'ESP2D', 'BedNuker', 'AntiFireball', 'TargetHUD'
]
for mod in modules_to_fix:
    change_method_return_type(f'/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/{mod}.java')

# TargetHUD specific
path = '/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/TargetHUD.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('int gradientLeft = hud != null ? hud.getColor(System.currentTimeMillis()).getRGB() : Color.WHITE.getRGB();', 'int gradientLeft = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE.getRGB();')
content = content.replace('int gradientRight = hud != null ? hud.getColor(System.currentTimeMillis() + 500).getRGB() : Color.WHITE.getRGB();', 'int gradientRight = hud != null ? hud.getColor(System.currentTimeMillis() + 500) : Color.WHITE.getRGB();')
content = content.replace('public Color getColor(', 'public int getColor(')
content = content.replace('hud != null ? hud.getColor(System.currentTimeMillis()) : -1', 'hud != null ? hud.getColor(System.currentTimeMillis()) : -1')
with open(path, 'w') as f: f.write(content)

# BlockOverlay
path = '/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/BlockOverlay.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('Color hudColor = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE;', 'int hudColor = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE.getRGB();')
content = content.replace('hudColor.getRed()', '(hudColor >> 16 & 0xFF)')
content = content.replace('hudColor.getGreen()', '(hudColor >> 8 & 0xFF)')
content = content.replace('hudColor.getBlue()', '(hudColor & 0xFF)')
with open(path, 'w') as f: f.write(content)

# NameTags
path = '/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/NameTags.java'
with open(path, 'r') as f: content = f.read()
content = content.replace('int friendColor = Myau.friendManager.getColor();', 'int friendColor = Myau.friendManager.getColor().getRGB();')
content = content.replace('int targetColor = Myau.targetManager.getColor();', 'int targetColor = Myau.targetManager.getColor().getRGB();')
with open(path, 'w') as f: f.write(content)

# Radar, ESP, Tracers, TargetStrafe
for mod in ['Radar', 'ESP', 'Tracers', 'TargetStrafe']:
    path = f'/home/leviet/OpenMyau-Plus/src/main/java/myau/module/modules/{mod}.java'
    with open(path, 'r') as f: content = f.read()
    content = content.replace('.getColor(System.currentTimeMillis()).getRGB()', '.getColor(System.currentTimeMillis())')
    with open(path, 'w') as f: f.write(content)

