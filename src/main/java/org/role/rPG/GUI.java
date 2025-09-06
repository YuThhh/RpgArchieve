package org.role.rPG;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class GUI implements InventoryHolder, Listener {

    private final Inventory inv;

    public GUI() {
        // InventoryHolder 인터페이스의 메서드인 getInventory()만 @Override를 사용합니다.
        Component titleComponent = Component.text("메뉴", NamedTextColor.BLUE);
        inv = Bukkit.createInventory(this, 54, titleComponent);
        initializeItems();
    }

    private void initializeItems() {
        // inv.setItem(슬롯, 아이템);
        // 슬롯 번호는 0부터 시작
        ItemStack grayGlassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta grayMeta = grayGlassPane.getItemMeta();
        grayMeta.displayName(Component.text(" ", NamedTextColor.GRAY));
        grayMeta.setHideTooltip(true);
        grayGlassPane.setItemMeta(grayMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, grayGlassPane);
        }

        ItemStack profile = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta headMeta = profile.getItemMeta();
        headMeta.displayName(Component.text("프로필", NamedTextColor.YELLOW));
        profile.setItemMeta(headMeta);

        ItemStack title = new ItemStack(Material.END_CRYSTAL);
        ItemMeta titleMeta = title.getItemMeta();
        titleMeta.displayName(Component.text("칭호", NamedTextColor.GREEN));
        title.setItemMeta(titleMeta);

        ItemStack quest = new ItemStack(Material.NETHER_STAR);
        ItemMeta questMeta = quest.getItemMeta();
        questMeta.displayName(Component.text("퀘스트", NamedTextColor.BLUE));
        quest.setItemMeta(questMeta);

        ItemStack level = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = level.getItemMeta();
        levelMeta.displayName(Component.text("숙련도", NamedTextColor.RED));
        level.setItemMeta(levelMeta);

        ItemStack craft = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta craftMeta = craft.getItemMeta();
        craftMeta.displayName(Component.text("제작대", NamedTextColor.DARK_GREEN));
        craft.setItemMeta(craftMeta);

        ItemStack storage = new ItemStack(Material.CHEST);
        ItemMeta storageMeta = storage.getItemMeta();
        storageMeta.displayName(Component.text("창고", NamedTextColor.GREEN));
        storage.setItemMeta(storageMeta);

        ItemStack guide = new ItemStack(Material.BOOK);
        ItemMeta guidMeta = guide.getItemMeta();
        guidMeta.displayName(Component.text("도감", NamedTextColor.DARK_PURPLE));
        guide.setItemMeta(guidMeta);

        ItemStack issue = new ItemStack(Material.OAK_SIGN);
        ItemMeta issueMeta = issue.getItemMeta();
        issueMeta.displayName(Component.text("건의사항", NamedTextColor.DARK_GREEN));
        issue.setItemMeta(issueMeta);

        ItemStack bug = new ItemStack(Material.REDSTONE);
        ItemMeta bugMeta = bug.getItemMeta();
        bugMeta.displayName(Component.text("버그 제보", NamedTextColor.DARK_RED));
        bug.setItemMeta(bugMeta);

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("뒤로가기", NamedTextColor.RED));
        back.setItemMeta(backMeta);

        inv.setItem(13, profile);
        inv.setItem(20,title);
        inv.setItem(21,quest);
        inv.setItem(22,level);
        inv.setItem(23,craft);
        inv.setItem(24,storage);
        inv.setItem(31,guide);
        inv.setItem(45,issue);
        inv.setItem(46,bug);
        inv.setItem(53,back);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public void openInventory(final Player p) {
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 클릭된 인벤토리가 현재 GUI 클래스의 인스턴스인지 확인
        if (event.getInventory().getHolder() instanceof GUI) {
            // 이벤트 취소하여 아이템 이동 방지
            event.setCancelled(true);

            // Shift 클릭으로 플레이어 인벤토리에서 GUI로 아이템이 들어오는 것도 방지
            if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
                event.setCancelled(true);
            }
        }
    }
}
