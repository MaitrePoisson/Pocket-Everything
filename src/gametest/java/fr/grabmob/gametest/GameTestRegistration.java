package fr.grabmob.gametest;

import fr.grabmob.GrabMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = GrabMob.MOD_ID)
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
    }
}
