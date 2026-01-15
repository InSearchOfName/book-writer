package name.insearchof.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;

public class BookWriterEvents {
    public static final Event<WriteBook> WRITE_BOOK = EventFactory.createArrayBacked(
            WriteBook.class,
            listeners -> (player, title, content) -> {
                for (WriteBook listener : listeners) {
                    listener.onWriteBook(player, title, content);
                }
            }
    );

    @FunctionalInterface
    public interface WriteBook {
        void onWriteBook(PlayerEntity player, String title, String content);
    }
}