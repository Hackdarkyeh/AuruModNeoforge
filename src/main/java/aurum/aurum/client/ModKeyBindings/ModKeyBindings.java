package aurum.aurum.client.ModKeyBindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String CATEGORY = "key.categories.aurum";
    public static final KeyMapping TOGGLE_AURUM_BAR = new KeyMapping(
            "key.aurum.toggle_aurum_bar",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,  // Tecla por defecto: H
            CATEGORY
    );
}
