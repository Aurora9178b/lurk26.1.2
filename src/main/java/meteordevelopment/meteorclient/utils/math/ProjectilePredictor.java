package meteordevelopment.meteorclient.utils.math;

import meteordevelopment.meteorclient.utils.rotation.Rotation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ProjectilePredictor {
   private static final int ITERATIONS = 8;
   private static final double MIN_SPEED = 1.0E-4;
   private static final double MAX_TRAVEL_TICKS = 80.0;

   private ProjectilePredictor() {
   }

   public static ProjectilePredictor.Prediction predict(Vec3 origin, Vec3 targetPos, Vec3 targetVelocity, double projectileSpeed, double gravity) {
      if (origin != null && targetPos != null && !(projectileSpeed <= 1.0E-4)) {
         Vec3 velocity = targetVelocity == null ? Vec3.ZERO : targetVelocity;
         double travelTicks = initialTravelTicks(origin, targetPos, velocity, projectileSpeed);
         Rotation rotation = null;
         boolean ballistic = false;

         for (int i = 0; i < 8; i++) {
            Vec3 aimPoint = targetPos.add(velocity.scale(travelTicks));
            ProjectilePredictor.BallisticSolution solution = solveBallistic(origin, aimPoint, projectileSpeed, gravity);
            rotation = solution.rotation();
            ballistic = solution.valid();
            if (rotation == null) {
               return null;
            }

            double nextTicks = travelTicks(origin, aimPoint, projectileSpeed, rotation, gravity);
            if (Math.abs(nextTicks - travelTicks) < 0.05) {
               travelTicks = nextTicks;
               break;
            }

            travelTicks = Mth.clamp(nextTicks, 0.0, 80.0);
         }

         Vec3 var17 = targetPos.add(velocity.scale(travelTicks));
         ProjectilePredictor.BallisticSolution finalSolution = solveBallistic(origin, var17, projectileSpeed, gravity);
         if (finalSolution.rotation() != null) {
            rotation = finalSolution.rotation();
            ballistic = finalSolution.valid();
         }

         return new ProjectilePredictor.Prediction(var17, rotation, travelTicks, ballistic);
      } else {
         return null;
      }
   }

   private static double initialTravelTicks(Vec3 origin, Vec3 targetPos, Vec3 targetVelocity, double projectileSpeed) {
      Vec3 relative = targetPos.subtract(origin);
      Vec3 horizontalRelative = new Vec3(relative.x, 0.0, relative.z);
      Vec3 horizontalVelocity = new Vec3(targetVelocity.x, 0.0, targetVelocity.z);
      double a = horizontalVelocity.lengthSqr() - projectileSpeed * projectileSpeed;
      double b = 2.0 * horizontalRelative.dot(horizontalVelocity);
      double c = horizontalRelative.lengthSqr();
      double t = solvePositiveTime(a, b, c);
      if (!Double.isFinite(t)) {
         t = horizontalRelative.length() / Math.max(projectileSpeed, 1.0E-4);
      }

      return Mth.clamp(t, 0.0, 80.0);
   }

   private static double solvePositiveTime(double a, double b, double c) {
      if (Math.abs(a) < 1.0E-7) {
         if (Math.abs(b) < 1.0E-7) {
            return Double.NaN;
         }

         double t = -c / b;
         return t > 0.0 ? t : Double.NaN;
      } else {
         double discriminant = b * b - 4.0 * a * c;
         if (discriminant < 0.0) {
            return Double.NaN;
         } else {
            double sqrt = Math.sqrt(discriminant);
            double t1 = (-b - sqrt) / (2.0 * a);
            double t2 = (-b + sqrt) / (2.0 * a);
            if (t1 > 0.0 && t2 > 0.0) {
               return Math.min(t1, t2);
            } else if (t1 > 0.0) {
               return t1;
            } else {
               return t2 > 0.0 ? t2 : Double.NaN;
            }
         }
      }
   }

   private static double travelTicks(Vec3 origin, Vec3 aimPoint, double projectileSpeed, Rotation rotation, double gravity) {
      if (gravity <= 0.0) {
         return origin.distanceTo(aimPoint) / Math.max(projectileSpeed, 1.0E-4);
      }

      double horizontal = horizontalDistance(origin, aimPoint);
      double horizontalSpeed = projectileSpeed * Math.max(Math.cos(Math.toRadians(rotation.getPitch())), 0.05);
      return horizontal / Math.max(horizontalSpeed, 1.0E-4);
   }

   private static ProjectilePredictor.BallisticSolution solveBallistic(Vec3 origin, Vec3 aimPoint, double projectileSpeed, double gravity) {
      double dx = aimPoint.x - origin.x;
      double dy = aimPoint.y - origin.y;
      double dz = aimPoint.z - origin.z;
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      if (horizontal < 1.0E-4) {
         return new ProjectilePredictor.BallisticSolution(null, false);
      } else {
         float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
         if (gravity <= 0.0) {
            float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
            return new ProjectilePredictor.BallisticSolution(new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F)), true);
         } else {
            double speedSq = projectileSpeed * projectileSpeed;
            double discriminant = speedSq * speedSq - gravity * (gravity * horizontal * horizontal + 2.0 * dy * speedSq);
            if (discriminant < 0.0) {
               float directPitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
               return new ProjectilePredictor.BallisticSolution(new Rotation(yaw, Mth.clamp(directPitch, -90.0F, 90.0F)), false);
            } else {
               double pitch = -Math.toDegrees(Math.atan((speedSq - Math.sqrt(discriminant)) / (gravity * horizontal)));
               return new ProjectilePredictor.BallisticSolution(new Rotation(yaw, Mth.clamp((float)pitch, -90.0F, 90.0F)), true);
            }
         }
      }
   }

   private static double horizontalDistance(Vec3 a, Vec3 b) {
      double x = b.x - a.x;
      double z = b.z - a.z;
      return Math.sqrt(x * x + z * z);
   }

   private record BallisticSolution(Rotation rotation, boolean valid) {
   }

   public record Prediction(Vec3 aimPoint, Rotation rotation, double travelTicks, boolean ballistic) {
   }
}
