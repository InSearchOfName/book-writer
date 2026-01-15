package name.insearchof.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

public class InventoryUtils {
    private InventoryUtils() {
        // Utility class
    }

    public static int countWritableBooks(PlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).getItem() == Items.WRITABLE_BOOK) {
                count++;
            }
        }
        return count;
    }
}
