import os
import glob
import re

files = glob.glob('/home/leviet/OpenMyau-Plus/src/main/java/myau/**/*.java', recursive=True)

for filepath in files:
    with open(filepath, 'r') as f:
        content = f.read()

    original = content
    
    # 1. Fix .getRGB() on getColor
    content = re.sub(r'(\.getColor\([^)]*\))\.getRGB\(\)', r'\1', content)
    content = re.sub(r'(\.getColor\([^)]*,[^)]*\))\.getRGB\(\)', r'\1', content)

    # 2. Fix Color variable assignments
    content = re.sub(r'Color\s+color\s*=\s*\(\(HUD\)', r'int color = ((HUD)', content)
    content = re.sub(r'Color\s+c1\s*=\s*hud\.getColor', r'int c1 = hud.getColor', content)
    content = re.sub(r'Color\s+c2\s*=\s*hud\.getColor', r'int c2 = hud.getColor', content)
    content = re.sub(r'Color\s+hudColor\s*=\s*hud\s*!=\s*null\s*\?\s*hud\.getColor\(System\.currentTimeMillis\(\)\)\s*:\s*Color\.WHITE;', r'int hudColor = hud != null ? hud.getColor(System.currentTimeMillis()) : Color.WHITE.getRGB();', content)
    content = re.sub(r'Color\s+themeColor\s*=\s*hud\.getColor', r'int themeColor = hud.getColor', content)

    # 3. Fix TargetHUD getColor returns
    content = content.replace('hud != null ? hud.getColor(System.currentTimeMillis()) : new Color(-1)', 'hud != null ? hud.getColor(System.currentTimeMillis()) : -1')
    content = content.replace('hud != null ? hud.getColor(System.currentTimeMillis(), offset) : new Color(0, 150, 255)', 'hud != null ? hud.getColor(System.currentTimeMillis(), offset) : new Color(0, 150, 255).getRGB()')

    # 4. Fix method return types if they return Color but actually return int now
    if 'public Color ' in content and 'hud.getColor' in content:
        # We need to manually fix these, so we'll leave them to compile errors or attempt generic replace
        # Look for methods returning HUD color in Modules
        content = re.sub(r'public Color get\w+\([^)]*\)\s*\{[^}]*return\s+[^;]*\.getColor\([^;]*;\s*\}', lambda m: m.group(0).replace('public Color', 'public int'), content)

    if original != content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Fixed {filepath}")

