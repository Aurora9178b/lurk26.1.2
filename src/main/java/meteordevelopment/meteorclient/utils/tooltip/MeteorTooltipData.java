package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public interface MeteorTooltipData extends TooltipComponent {
   ClientTooltipComponent getComponent();
}
