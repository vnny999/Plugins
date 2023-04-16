package net.runelite.client.plugins.oneclickchins;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.plugins.oneclickchins.BoxTrap.State;
import net.runelite.client.util.Text;

public class BoxTrap
{
    @Getter
    private final WorldPoint worldLocation;
    @Getter
    private TileItem tileItem;
    @Getter
    @Setter
    private GameObject gameObject;
    @Getter
    private Instant placedOn;
    @Getter
    @Setter
    private net.runelite.client.plugins.oneclickchins.BoxTrap.State state;

    BoxTrap(GameObject gameObject)
    {
        this.state = net.runelite.client.plugins.oneclickchins.BoxTrap.State.OPEN;
        this.placedOn = Instant.now();
        this.worldLocation = gameObject.getWorldLocation();
        this.gameObject = gameObject;
    }

    BoxTrap(ItemSpawned itemSpawned)
    {
        this.state = State.ITEM;
        this.placedOn = Instant.now();
        this.worldLocation = itemSpawned.getTile().getWorldLocation();
        this.tileItem = itemSpawned.getItem();
    }

    enum State
    {
        /**
         * A laid out trap.
         */
        OPEN,
        /**
         * A trap that is empty.
         */
        EMPTY,
        /**
         * A trap that caught something.
         */
        FULL,

        /**
         * A trap that is an item in the inventory.
         */
        ITEM
    }

    public int getTrapTimeRemaining()
    {
        Duration duration = Duration.between(placedOn, Instant.now());
        return 60 - duration.toSecondsPart();
    }

    /**
     * Resets the time value when the trap was placed.
     */
    public void resetTimer()
    {
        placedOn = Instant.now();
    }

    /**
     * Saves the location of the trap for the given player.
     *
     * @param playerName The name of the player.
     * @param trapMap The map of player trap locations.
     */
    public void saveLocation(String playerName, Map<String, List<BoxTrap>> trapMap)
    {
        List<BoxTrap> playerTraps = trapMap.getOrDefault(Text.standardize(playerName), new ArrayList<>());
        playerTraps.removeIf(trap -> trap.getWorldLocation().equals(worldLocation));
        playerTraps.add(this);
        trapMap.put(Text.standardize(playerName), playerTraps);
    }

    /**
     * Attempts to load the location of the trap for the given player.
     * If no trap is found at the saved location, attempts to place a trap from the player's inventory.
     *
     * @param playerName The name of the player.
     * @param trapMap The map of player trap locations.
     * @return Whether the trap was successfully loaded or placed.
     */
    public boolean loadLocation(String playerName, Map<String, List<BoxTrap>> trapMap)
    {
        List<BoxTrap> playerTraps = trapMap.getOrDefault(Text.standardize(playerName), new ArrayList<>());
        for (BoxTrap trap : playerTraps)
        {
            if (trap.getWorldLocation().equals(worldLocation))
            {
                gameObject = trap.getGameObject();
                tileItem = trap.getTileItem();
                state = trap.getState();
                placedOn = trap.getPlacedOn();
                return true;
            }
        }

        Item[] traps = ClientUtils.getItems(item -> item.getName().equals("Box trap"));
        if (traps.length > 0)
        {
            Item trapItem = traps[0];
            trapItem.interact("Lay");
            return true;
        }

        return false;
    }
}
