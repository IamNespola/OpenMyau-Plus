import re

path = '/home/leviet/OpenMyau-Plus/src/main/java/myau/ui/impl/clickgui/cheadle/CheadleClickGui.java'
with open(path, 'r') as f: content = f.read()

# Fix 760
content = content.replace('((HUD) Myau.moduleManager.modules.get(HUD.class))new Color(.getColor(System.currentTimeMillis(), offset.get())).getRGB()', '((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis(), offset.get())')

# Fix Color declarations
content = content.replace('Color accentColor = ((HUD) Myau.moduleManager.modules.get(HUD.class))\n                        new Color(.getColor(System.currentTimeMillis(), offset.get()));', 'int accentColorInt = ((HUD) Myau.moduleManager.modules.get(HUD.class))\n                        .getColor(System.currentTimeMillis(), offset.get());\n                Color accentColor = new Color(accentColorInt, true);')

content = content.replace('Color accentColor = ((HUD) Myau.moduleManager.modules.get(HUD.class))\n                    new Color(.getColor(System.currentTimeMillis(), offset.get()));', 'int accentColorInt = ((HUD) Myau.moduleManager.modules.get(HUD.class))\n                    .getColor(System.currentTimeMillis(), offset.get());\n            Color accentColor = new Color(accentColorInt, true);')


with open(path, 'w') as f: f.write(content)

