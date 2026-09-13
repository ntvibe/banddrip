import android.content.Intent;
import android.os.Looper;
import android.os.Parcelable;
import java.lang.reflect.Method;

/** One-time ADB launcher for Mi Fitness's own installer. No BLE client or APK patch. */
public final class BandDripNativeInstaller {
    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("Expected Mi Fitness APK class path");
            Looper.prepareMainLooper();
            ClassLoader loader = new dalvik.system.PathClassLoader(args[0], ClassLoader.getSystemClassLoader());
            Class<?> builderType = loader.loadClass("com.xiaomi.fitness.baseui.common.FragmentParams$b");
            Object builder = builderType.getDeclaredConstructor().newInstance();
            Class<?> fragment = loader.loadClass("com.xiaomi.xms.wearable.ui.debug.ThirdAppDebugFragment");
            builderType.getMethod("e", Class.class).invoke(builder, fragment);
            Object params = builderType.getMethod("b").invoke(builder);
            Intent intent = new Intent();
            intent.setClassName("com.xiaomi.wearable", "com.xiaomi.fitness.baseui.common.CommonBaseActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("fragment_param", (Parcelable) params);
            Class<?> amType = Class.forName("android.app.ActivityManager");
            Method getter = amType.getDeclaredMethod("getService");
            getter.setAccessible(true);
            Object manager = getter.invoke(null);
            for (Method method : manager.getClass().getMethods()) {
                Class<?>[] types = method.getParameterTypes();
                if (!method.getName().equals("startActivity") || types.length != 10
                        || types[1] != String.class || types[2] != Intent.class) continue;
                Object[] values = new Object[types.length];
                for (int i = 0; i < types.length; i++) {
                    if (types[i] == int.class) values[i] = 0;
                    else if (types[i] == Intent.class) values[i] = intent;
                    else if (i == 1) values[i] = "com.android.shell";
                }
                Object result = method.invoke(manager, values);
                if (!(result instanceof Integer) || ((Integer) result) < 0)
                    throw new IllegalStateException("Activity launch rejected: " + result);
                System.out.println("Native installer launch requested; verify the phone screen. No band install attempted.");
                return;
            }
            throw new IllegalStateException("Unsupported Android ActivityManager signature");
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }
}
