package myau.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class CFontRenderer extends CFont {
    protected CFont.CharData[] boldChars = new CFont.CharData[256];
    protected CFont.CharData[] italicChars = new CFont.CharData[256];
    protected CFont.CharData[] boldItalicChars = new CFont.CharData[256];

    public int FONT_HEIGHT = 9;

    private final int[] colorCode = new int[32];

    private boolean useMCustomFont = false;

    public CFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
        setupMinecraftColorcodes();
    }

    String nameFontTTF;

    public CFontRenderer(String NameFontTTF, float size, int fonttype, boolean antiAlias, boolean fractionalMetrics) {
        super(getFontFromTTF(new ResourceLocation("myau/font/" + NameFontTTF+".ttf"), size,fonttype), antiAlias, fractionalMetrics);
        this.nameFontTTF = NameFontTTF;
        this.useMCustomFont = NameFontTTF.equalsIgnoreCase("mc");
        setupMinecraftColorcodes();
    }

    public String getNameFontTTF() {
        return this.nameFontTTF;
    }

    public float drawString(String text, float x, float y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, color, false);
        }
        return drawString(text, x, y, color, false);
    }

    public float drawString(String text, double x, double y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) x, (float) y, color, false);
        }
        return drawString(text, x, y, color, false);
    }

    public float drawStringWithShadow(String text, float x, float y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, color, true);
        }
        float shadowWidth = drawString(text, x + 1.0f, y + 1.0f, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, false));
    }

    public float drawStringWithShadow(String text, double x, double y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) x, (float) y, color, true);
        }
        float shadowWidth = drawString(text, x + 1.0f, y + 1.0f, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, false));
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        if (useMCustomFont) {
            int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x - width / 2f, y, color, false);
        }
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public float drawCenteredString(String text, double x, double y, int color) {
        if (useMCustomFont) {
            int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) (x - width / 2f), (float) y, color, false);
        }
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        if (useMCustomFont) {
            int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x - width / 2f, y, color, true);
        }
        float shadowWidth = drawString(text, x - getStringWidth(text) / 2 + 0.45D, y + 0.5D, color, true);
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public void drawStringWithOutline(String text, double x, double y, int color) {
        drawString(text, x - .5, y, 0x000000);
        drawString(text, x + .5, y, 0x000000);
        drawString(text, x, y - .5, 0x000000);
        drawString(text, x, y + .5, 0x000000);
        drawString(text, x, y, color);
    }

    public void drawCenteredStringWithOutline(String text, double x, double y, int color) {
        drawCenteredString(text, x - .5, y, 0x000000);
        drawCenteredString(text, x + .5, y, 0x000000);
        drawCenteredString(text, x, y - .5, 0x000000);
        drawCenteredString(text, x, y + .5, 0x000000);
        drawCenteredString(text, x, y, color);
    }

    public float drawCenteredStringWithShadow(String text, double x, double y, int color) {
        float shadowWidth = drawString(text, x - getStringWidth(text) / 2 + 0.45D, y + 0.5D, color, true);
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public float drawString(String text, double x, double y, int color, boolean shadow) {
        Minecraft mc = Minecraft.getMinecraft();
        x -= 1;

        if (text == null) {
            return 0.0F;
        }

        if (color == 553648127) {
            color = 16777215;
        }

        if ((color & 0xFC000000) == 0) {
            color |= -16777216;
        }

        if (shadow) {
            color = (color & 0xFCFCFC) >> 2 | color & new Color(20, 20, 20, 200).getRGB();
        }

        CFont.CharData[] currentData = this.charData;
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        boolean randomCase = false;
        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;
        boolean render = true;
        x *= 2.0D;
        y = (y - 2.0D) * 2.0D;

        if (render) {
            GL11.glPushMatrix();
            GlStateManager.scale(0.5D, 0.5D, 0.5D);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
            int size = text.length();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(tex.getGlTextureId());

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getGlTextureId());

            for (int i = 0; i < size; i++) {
                char character = text.charAt(i);
                if ((String.valueOf(character).equals("\247")) && (i < size)) {
                    int colorIndex = 21;

                    try {
                        colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (colorIndex < 16) {
                        bold = false;
                        italic = false;
                        randomCase = false;
                        underline = false;
                        strikethrough = false;
                        GlStateManager.bindTexture(tex.getGlTextureId());
                        currentData = this.charData;

                        if ((colorIndex < 0) || (colorIndex > 15)) {
                            colorIndex = 15;
                        }

                        if (shadow) {
                            colorIndex += 16;
                        }

                        int colorcode = this.colorCode[colorIndex];
                        GlStateManager.color((colorcode >> 16 & 0xFF) / 255.0F, (colorcode >> 8 & 0xFF) / 255.0F, (colorcode & 0xFF) / 255.0F, alpha);
                    } else if (colorIndex == 16) {
                        randomCase = true;
                    } else if (colorIndex == 17) {
                        bold = true;

                        if (italic) {
                            currentData = this.charData;
                        } else {
                            currentData = this.charData;
                        }
                    } else if (colorIndex == 18) {
                        strikethrough = true;
                    } else if (colorIndex == 19) {
                        underline = true;
                    } else if (colorIndex == 20) {
                        italic = true;

                        if (bold) {
                            currentData = this.charData;
                        } else {
                            currentData = this.charData;
                        }
                    } else if (colorIndex == 21) {
                        bold = false;
                        italic = false;
                        randomCase = false;
                        underline = false;
                        strikethrough = false;
                        GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
                        GlStateManager.bindTexture(tex.getGlTextureId());
                        currentData = this.charData;
                    }

                    i++;
                } else if ((character < currentData.length) && (character >= 0)) {
                    GL11.glBegin(GL11.GL_TRIANGLES);
                    drawChar(currentData, character, (float) x, (float) y);
                    GL11.glEnd();

                    if (strikethrough) {
                        drawLine(x, y + currentData[character].height / 2, x + currentData[character].width - 8.0D, y + currentData[character].height / 2, 1.0F);
                    }

                    if (underline) {
                        drawLine(x, y + currentData[character].height - 2.0D, x + currentData[character].width - 8.0D, y + currentData[character].height - 2.0D, 1.0F);
                    }

                    x += currentData[character].width - 8 + this.charOffset;
                }
            }

            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
            GL11.glPopMatrix();
        }

        return (float) x / 2.0F;
    }

    public int getStringWidth(String text) {
        if (text == null) {
            return 0;
        }
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }
        int width = 0;
        CFont.CharData[] currentData = this.charData;
        boolean bold = false;
        boolean italic = false;
        int size = text.length();

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);

            if ((String.valueOf(character).equals("\247")) && (i < size)) {
                int colorIndex = "0123456789abcdefklmnor".indexOf(character);

                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                } else if (colorIndex == 17) {
                    bold = true;

                    if (italic) {
                        currentData = this.boldItalicChars;
                    } else {
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 20) {
                    italic = true;

                    if (bold) {
                        currentData = this.boldItalicChars;
                    } else {
                        currentData = this.italicChars;
                    }
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                }

                i++;
            } else if ((character < currentData.length) && (character >= 0)) {
                width += currentData[character].width - 8 + this.charOffset;
            }
        }

        return width / 2;
    }

    public int getStringWidthCust(String text) {
        if (text == null) {
            return 0;
        }

        int width = 0;
        CFont.CharData[] currentData = this.charData;
        boolean bold = false;
        boolean italic = false;
        int size = text.length();

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);

            if ((String.valueOf(character).equals("�")) && (i < size)) {
                int colorIndex = "0123456789abcdefklmnor".indexOf(character);

                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                } else if (colorIndex == 17) {
                    bold = true;

                    if (italic) {
                        currentData = this.boldItalicChars;
                    } else {
                        currentData = this.boldChars;
                    }
                } else if (colorIndex == 20) {
                    italic = true;

                    if (bold) {
                        currentData = this.boldItalicChars;
                    } else {
                        currentData = this.italicChars;
                    }
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                }

                i++;
            } else if ((character < currentData.length) && (character >= 0)) {
                width += currentData[character].width - 8 + this.charOffset;
            }
        }

        return (width - this.charOffset) / 2;
    }

    public void setFont(Font font) {
        super.setFont(font);
    }

    public void setAntiAlias(boolean antiAlias) {
        super.setAntiAlias(antiAlias);
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        super.setFractionalMetrics(fractionalMetrics);
    }

    protected DynamicTexture texBold;
    protected DynamicTexture texItalic;
    protected DynamicTexture texItalicBold;

    private void drawLine(double x, double y, double x1, double y1, float width) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x1, y1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public List<String> wrapWords(String text, double width) {
        List finalWords = new ArrayList();

        if (getStringWidth(text) > width) {
            String[] words = text.split(" ");
            String currentWord = "";
            char lastColorCode = 65535;

            for (String word : words) {
                for (int i = 0; i < word.toCharArray().length; i++) {
                    char c = word.toCharArray()[i];

                    if ((String.valueOf(c).equals("�")) && (i < word.toCharArray().length - 1)) {
                        lastColorCode = word.toCharArray()[(i + 1)];
                    }
                }

                if (getStringWidth(currentWord + word + " ") < width) {
                    currentWord = currentWord + word + " ";
                } else {
                    finalWords.add(currentWord);
                    currentWord = "" + lastColorCode + word + " ";
                }
            }

            if (currentWord.length() > 0) if (getStringWidth(currentWord) < width) {
                finalWords.add("" + lastColorCode + currentWord + " ");
                currentWord = "";
            } else {
                for (String s : formatString(currentWord, width)) {
                    finalWords.add(s);
                }
            }
        } else {
            finalWords.add(text);
        }

        return finalWords;
    }

    public List<String> formatString(String string, double width) {
        List finalWords = new ArrayList();
        String currentWord = "";
        char lastColorCode = 65535;
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if ((String.valueOf(c).equals("�")) && (i < chars.length - 1))
            {
                lastColorCode = chars[(i + 1)];
            }

            if (getStringWidth(currentWord + c) < width) {
                currentWord = currentWord + c;
            } else {
                finalWords.add(currentWord);
                currentWord = "" + lastColorCode + String.valueOf(c);
            }
        }

        if (currentWord.length() > 0) {
            finalWords.add(currentWord);
        }

        return finalWords;
    }

    private void setupMinecraftColorcodes() {
        for (int index = 0; index < 32; index++) {
            int noClue = (index >> 3 & 0x1) * 85;
            int red = (index >> 2 & 0x1) * 170 + noClue;
            int green = (index >> 1 & 0x1) * 170 + noClue;
            int blue = (index >> 0 & 0x1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }

            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
    }

    public static Font getFontFromTTF(ResourceLocation fontLocation, float fontSize, int fontType) {
        Font output = null;
        try {
            output = Font.createFont(fontType, Minecraft.getMinecraft().getResourceManager().getResource(fontLocation).getInputStream());
            output = output.deriveFont(fontSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Default", fontType, (int) fontSize);
        }
        return output;
    }

    public float getMiddleOfBox(float height) {
        return height / 2f - getHeight() / 2f;
    }

}