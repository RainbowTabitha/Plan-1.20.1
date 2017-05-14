package main.java.com.djrapitops.plan.data.listeners;

import java.util.Date;
import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.data.cache.DataCacheHandler;
import main.java.com.djrapitops.plan.data.handling.info.DeathInfo;
import main.java.com.djrapitops.plan.data.handling.info.KillInfo;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Event Listener for EntityDeathEvents.
 *
 * @author Rsl1122
 */
public class PlanDeathEventListener implements Listener {

    private final Plan plugin;
    private final DataCacheHandler handler;

    /**
     * Class Constructor.
     *
     * @param plugin Current instance of Plan
     */
    public PlanDeathEventListener(Plan plugin) {
        this.plugin = plugin;
        this.handler = plugin.getHandler();
    }

    /**
     * Command use listener.
     *
     * @param event Fired event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        long time = new Date().getTime();
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        boolean killerIsPlayer = killer != null;
        if (killerIsPlayer) {
            Material itemInHand;
            try {
                itemInHand = killer.getInventory().getItemInMainHand().getType();
            } catch (Throwable e) {
                try {
                    itemInHand = killer.getInventory().getItemInHand().getType();
                } catch (Throwable e2) {
                    itemInHand = Material.AIR;
                }
            }
            handler.addToPool(new KillInfo(killer.getUniqueId(), time, dead, itemInHand.name()));
        }
        if (dead instanceof Player) {
            handler.addToPool(new DeathInfo(((Player) dead).getUniqueId()));
        }
    }
}
