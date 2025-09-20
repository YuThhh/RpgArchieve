// 파일 경로: org/role/rPG/MagicListener.java (예시)
package org.role.rPG.Magic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.role.rPG.Item.ItemManager;
import org.role.rPG.Player.StatManager;
import org.role.rPG.RPG;
// Bukkit 추가

import java.util.List;
import java.util.Objects;
// PlayerProfile 추가
// PlayerTextures 추가
// URL 추가


public class MagicListener implements Listener {

    private final RPG plugin;
    private final ItemManager itemManager;
    private final StatManager statManager;
    private final SpellManager spellManager;

    public MagicListener(RPG plugin, ItemManager itemManager, StatManager statManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.statManager = statManager;
        this.spellManager = new SpellManager(plugin, statManager);
    }

    @EventHandler
    public void onPlayerUseMagicBook(PlayerInteractEvent event) {
        // 1. 우클릭 액션인지 확인
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // 2. 손에 든 아이템이 커스텀 아이템인지, 그리고 'test_magicbook'인지 확인
        if (itemManager.isNotCustomItem(itemInHand)) {
            return;
        }

        if (!itemInHand.hasItemMeta() || !itemInHand.getItemMeta().hasLore()) {
            return;
        }

        // 2. 로어를 한 줄씩 반복하여 확인합니다.
        List<Component> lore = itemInHand.getItemMeta().lore();

        for (Component lineComponent : Objects.requireNonNull(lore)) {
            // 각 줄(Component)을 순수 텍스트(String)로 변환합니다.
            String plainLine = PlainTextComponentSerializer.plainText().serialize(lineComponent);

            // 로어 형식을 "마법: [마법이름]" 으로 통일하면 좋습니다.
            if (plainLine.contains("마법:")) {
                // 3. 투사체 발사!
                String spellName = plainLine.substring(plainLine.indexOf(":") + 1).trim();
                Spell spell = spellManager.getSpell(spellName);

                if (spell != null) {
                    // 마나 소모, 발사 소리 등 공통 로직은 여기서 처리

                    // TODO: 마나 소모 로직을 여기에 추가할 수 있습니다.
//         double currentMana = statManager.getFinalStat(player.getUniqueId(), "CURRENT_MANA");
//         if (currentMana < 20) {
//             player.sendMessage("마나가 부족합니다!");
//             return;
//         }
//         statManager.updatePlayerCurrentMana(player.getUniqueId(), currentMana - 20);

                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 1.7f);

                    // 해당 마법의 고유 로직 실행
                    spell.cast(player);

                    break; // 마법을 하나 찾아서 시전했으면 종료
                }
            }
        }
    }
}