package myau.util.client;

import myau.module.modules.MouseRawInput;
import net.minecraft.util.MouseHelper;

public class RawMouseHelper extends MouseHelper {
    @Override
    public void mouseXYChange() {
        int rawDeltaX = MouseRawInput.consumeDeltaX();
        int rawDeltaY = MouseRawInput.consumeDeltaY();

        if (rawDeltaX == 0 && rawDeltaY == 0) {
            super.mouseXYChange();
            return;
        }

        this.deltaX = rawDeltaX;
        this.deltaY = -rawDeltaY;
    }
}