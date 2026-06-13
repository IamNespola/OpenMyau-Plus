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
   SLIDE,
   SWING,
   OLD,
   PUSH,
   DASH,
   SLASH,
   SCALE,
   SWONK,
   STELLA,
   SMALL,
   EDIT,
   RHYS,
   STAB,
   FLOAT,
   REMIX,
   XIV,
   WINTER,
   YAMATO,
   SLIDE_SWING,
   SMALL_PUSH,
   REVERSE,
   INVENT,
   LEAKED,
   AQUA,
   ASTRO,
   FADEAWAY,
   ASTOLFO,
   ASTOLFO_SPIN,
   MOON,
   MOON_PUSH,
   SMOOTH,
   TAP1,
   TAP2,
   SIGMA3,
   SIGMA4,
   MYAU_1_8,
   MYAU_SLIDE,
   MYAU_SWANK,
   MYAU_SWANG,
   MYAU_AVATAR,
   MYAU_JIGSAW;

   public static AnimationMode fromJsonValue(String value) {
      try {
         return valueOf(value.toUpperCase());
      } catch (NullPointerException | IllegalArgumentException var2) {
         return VANILLA;
      }
   }
}
