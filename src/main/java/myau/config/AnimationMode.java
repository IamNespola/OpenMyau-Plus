package myau.config;

/**
 * Animation modes for item rendering
 * Original logic by syuto/animations-1.6, integrated into Uzi
 */
public enum AnimationMode {
   VANILLA,
   EXHIBITION,
   ETB,
   SIGMA,
   DORTWARE,
   PLAIN,
   SPIN,
   AVATAR,
   SWONG,
   SWANG,
   SWANK,
   STYLES,
   NUDGE,
   PUNCH,
   JIGSAW,
   SLIDE;

   public static AnimationMode fromJsonValue(String value) {
      try {
         return valueOf(value.toUpperCase());
      } catch (NullPointerException | IllegalArgumentException var2) {
         return VANILLA;
      }
   }
}
