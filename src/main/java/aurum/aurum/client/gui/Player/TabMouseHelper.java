package aurum.aurum.client.gui.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class TabMouseHelper {
    private static double savedX = -1;
    private static double savedY = -1;

    private static Field xposField;
    private static Field yposField;

    static {
        try {
            // En 1.21.1 con mapeos oficiales de Mojang, los campos son 'xpos' y 'ypos'
            xposField = MouseHandler.class.getDeclaredField("xpos");
            yposField = MouseHandler.class.getDeclaredField("ypos");
            xposField.setAccessible(true);
            yposField.setAccessible(true);
        } catch (Exception e) {
            xposField = null;
            yposField = null;
        }
    }

    /** Guarda la posición del cursor en píxeles de la ventana */
    public static void savePosition() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.mouseHandler != null) {
            savedX = mc.mouseHandler.xpos(); // <-- CORREGIDO: xpos()
            savedY = mc.mouseHandler.ypos(); // <-- CORREGIDO: ypos()
        }
    }

    /** Restaura el cursor tanto en GLFW como en las variables internas de Minecraft */
    public static void restorePosition() {
        if (savedX >= 0 && savedY >= 0) {
            Minecraft mc = Minecraft.getInstance();
            long window = mc.getWindow().getWindow();

            // 1. Mover el cursor del sistema operativo (GLFW)
            GLFW.glfwSetCursorPos(window, savedX, savedY);

            // 2. Forzar la actualización inmediata de las coordenadas internas del MouseHandler
            if (xposField != null && yposField != null) {
                try {
                    xposField.set(mc.mouseHandler, savedX);
                    yposField.set(mc.mouseHandler, savedY);
                } catch (Exception ignored) {}
            }

            savedX = -1;
            savedY = -1;
        }
    }

    public static boolean hasSavedPosition() {
        return savedX >= 0 && savedY >= 0;
    }
}