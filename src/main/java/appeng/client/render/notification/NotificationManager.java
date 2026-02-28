
package appeng.client.render.notification;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Original source copied from BeyondRealityCore and YamCore. All credits go to pauljoda for this code
 *
 * @author pauljoda
 *
 */
public class NotificationManager {

    private static GuiNotification guiNotification;

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.theWorld != null) {
            if (guiNotification == null) guiNotification = new GuiNotification(mc);
            guiNotification.updateNotificationWindow();
        }
    }

    public static GuiNotification getGuiNotification() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (guiNotification == null) guiNotification = new GuiNotification(mc);
        return guiNotification;
    }
}
