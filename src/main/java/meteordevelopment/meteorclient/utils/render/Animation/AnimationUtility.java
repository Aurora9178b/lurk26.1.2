package meteordevelopment.meteorclient.utils.render.Animation;

import meteordevelopment.meteorclient.utils.math.FrameRateCounter;
import meteordevelopment.meteorclient.utils.math.MathUtility;

public class AnimationUtility {
   public static float deltaTime() {
      return FrameRateCounter.INSTANCE.getFps() > 5 ? 1.0F / FrameRateCounter.INSTANCE.getFps() : 0.016F;
   }

   public static float fast(float end, float start, float multiple) {
      float clampedDelta = MathUtility.clamp(deltaTime() * multiple, 0.0F, 1.0F);
      return (1.0F - clampedDelta) * end + clampedDelta * start;
   }

   public static float ease(double start, double end, float multiple) {
      float clampedDelta = MathUtility.clamp(deltaTime() * multiple, 0.0F, 1.0F);
      return (float)(start + (end - start) * clampedDelta);
   }

   public static float ease(float start, float end, float multiple) {
      float clampedDelta = MathUtility.clamp(deltaTime() * multiple, 0.0F, 1.0F);
      return start + (end - start) * clampedDelta;
   }

   public static float wave(float start, float end, float speed) {
      float t = MathUtility.clamp(deltaTime() * speed, 0.0F, 1.0F);
      float overshootAmount = 0.1F;
      float overshootEnd = end + (end - start) * overshootAmount;
      return t < 0.7F ? start + (overshootEnd - start) * (t / 0.7F) : overshootEnd + (end - overshootEnd) * ((t - 0.7F) / 0.3F);
   }

   public static float ease2(float start, float end, float multiple) {
      float clampedDelta = MathUtility.clamp(deltaTime() * multiple, 0.0F, 1.0F);
      float result = start + (end - start) * clampedDelta;
      return start < end ? MathUtility.clamp(result, start, end) : MathUtility.clamp(result, end, start);
   }

   public static float interpolate(float startValue, float endValue, float progress) {
      return startValue + (endValue - startValue) * progress;
   }

   private float easeIn(float startValue, float endValue, float progress) {
      progress *= progress;
      return startValue + (endValue - startValue) * progress;
   }

   private float easeOut(float startValue, float endValue, float progress) {
      progress = 1.0F - (1.0F - progress) * (1.0F - progress);
      return startValue + (endValue - startValue) * progress;
   }
}
