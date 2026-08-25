import android.content.Intent;
import android.os.Looper;
import android.os.Parcelable;

/**
 * Tiny shell-side launcher used by BandDrip's optional Shizuku installer path.
 *
 * It is compiled to launch.dex during CI and bundled inside the Android APK.
 * The code intentionally does only one thing: open Mi Fitness's hidden
 * ThirdAppDebugFragment. UI interaction remains a separate, fail-safe step.
 */
public class LaunchFragment {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("ERROR: Mi Fitness APK path missing");
                return;
            }
            Looper.prepareMainLooper();
            String apkPath = args[0];
            dalvik.system.PathClassLoader cl = new dalvik.system.PathClassLoader(
                apkPath,
                ClassLoader.getSystemClassLoader()
            );

            Class<?> builderClass = cl.loadClass("com.xiaomi.fitness.baseui.common.FragmentParams$b");
            Object builder = builderClass.newInstance();
            Class<?> fragmentClass = cl.loadClass("com.xiaomi.xms.wearable.ui.debug.ThirdAppDebugFragment");
            builderClass.getMethod("e", Class.class).invoke(builder, fragmentClass);
            Object fragmentParams = builderClass.getMethod("b").invoke(builder);

            Intent intent = new Intent();
            intent.setClassName(
                "com.xiaomi.wearable",
                "com.xiaomi.fitness.baseui.common.CommonBaseActivity"
            );
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("fragment_param", (Parcelable) fragmentParams);

            Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
            java.lang.reflect.Method getService = activityManagerClass.getDeclaredMethod("getService");
            getService.setAccessible(true);
            Object activityManager = getService.invoke(null);

            for (java.lang.reflect.Method method : activityManager.getClass().getMethods()) {
                if (method.getName().equals("startActivity") && method.getParameterTypes().length == 10) {
                    Object[] callArgs = new Object[10];
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    for (int i = 0; i < callArgs.length; i++) {
                        if (parameterTypes[i] == int.class) {
                            callArgs[i] = 0;
                        } else if (parameterTypes[i] == Intent.class) {
                            callArgs[i] = intent;
                        } else if (parameterTypes[i] == String.class && i == 1) {
                            callArgs[i] = "com.android.shell";
                        } else {
                            callArgs[i] = null;
                        }
                    }
                    method.invoke(activityManager, callArgs);
                    System.out.println("SUCCESS");
                    return;
                }
            }
            System.err.println("ERROR: compatible ActivityManager.startActivity overload not found");
        } catch (Throwable error) {
            System.err.println("ERROR: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            error.printStackTrace();
        }
    }
}
