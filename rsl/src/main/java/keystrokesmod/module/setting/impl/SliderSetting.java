package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.event.PostSetSliderEvent;
import keystrokesmod.module.setting.Setting;
import net.minecraftforge.common.MinecraftForge;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderSetting extends Setting {
    private String settingName;
    private String[] options = null;
    private double defaultValue;
    private double max;
    private double min;
    private double intervals;
    public boolean isString;
    private String suffix = "";
    public boolean canBeDisabled;
    public GroupSetting groupSetting;

    public SliderSetting(GroupSetting groupSetting, String settingName, double defaultValue, double min, double max, double intervals) {
        super(settingName);
        this.groupSetting = groupSetting;
        this.settingName = settingName;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isString = false;
    }

    public SliderSetting(String settingName, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, defaultValue, min, max, intervals);
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this(groupSetting, settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(GroupSetting group, String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(group, settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, String suffix, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, int defaultValue, String[] options) {
        super(settingName);
        this.groupSetting = groupSetting;
        this.settingName = settingName;
        this.options = options;
        this.defaultValue = defaultValue;
        this.min = 0;
        this.max = options.length - 1;
        this.intervals = 1;
        this.isString = true;
    }

    public SliderSetting(String settingName, int defaultValue, String[] options) {
        this((GroupSetting) null, settingName, defaultValue, options);
    }

    public SliderSetting(String settingName, String suffix, int defaultValue, String[] options) {
        this((GroupSetting) null, settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, int defaultValue, String[] options) {
        this(groupSetting, settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String[] getOptions() {
        return options;
    }

    public String getName() {
        return this.settingName;
    }

    public double getInput() {
        return roundToInterval(this.defaultValue, 4);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public double setValue(double newValue) {
        newValue = correctValue(newValue, this.min, this.max);
        newValue = (double) Math.round(newValue * (1.0D / this.intervals)) / (1.0D / this.intervals);
        return this.defaultValue = newValue;
    }

    public void setValueWithEvent(double newValue) {
        double prev = this.defaultValue;
        MinecraftForge.EVENT_BUS.post(new PostSetSliderEvent(prev, this.setValue(newValue)));
    }

    public void setValueRaw(double n) {
        this.defaultValue = n;
    }

    public void setValueRawWithEvent(double n) {
        double prev = this.defaultValue;
        this.defaultValue = n;
        MinecraftForge.EVENT_BUS.post(new PostSetSliderEvent(prev, n));
    }

    public static double correctValue(double v, double i, double a) {
        v = Math.max(i, v);
        v = Math.min(a, v);
        return v;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public static double roundToInterval(double v, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
        if (data.has(getName()) && data.get(getName()).isJsonPrimitive()) {
            double newValue = defaultValue;
            try {
                newValue = data.getAsJsonPrimitive(getName()).getAsDouble();
            }
            catch (Exception e) {

            }
            if (newValue == -1) {
                setValueRaw(newValue);
                return;
            }
            setValue(newValue);
        }
    }
}
