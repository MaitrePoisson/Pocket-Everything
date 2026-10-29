package fr.pocketeverything.gametest;

import fr.pocketeverything.PocketEverything;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = PocketEverything.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GameTestRegistration {

    private GameTestRegistration() {
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        event.register(CaptureGameTests.class);
        event.register(ItemGameTests.class);
        event.register(EventGameTests.class);
        event.register(VillagerGameTests.class);
        event.register(RobustnessGameTests.class);
        event.register(EdgeGameTests.class);
        event.register(PolishGameTests.class);
        event.register(ChestGameTests.class);
        event.register(ChestEdgeGameTests.class);
        event.register(StorageCarrierGameTests.class);
    }
}
