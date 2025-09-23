package org.role.rPG.UI;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * 모든 커스텀 GUI의 이벤트를 중앙에서 관리하는 리스너 클래스입니다.
 * 플러그인 활성화 시 이 클래스만 리스너로 등록하면 됩니다.
 */
public class GUI_manager implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        // 클릭한 인벤토리가 우리가 만든 BaseUI를 상속받았는지 확인
        if (holder instanceof BaseUI ui) {
            // BaseUI의 handleClick 메서드를 호출하여 실제 처리를 위임
            ui.handleClick(event);
        }
    }

    // 창고(Storage_UI)처럼 인벤토리를 닫을 때 저장 로직이 필요한 경우를 위해 추가
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof Storage_UI) {
            ((Storage_UI) holder).handleClose(event);
        }

        // Reforge_UI처럼 아이템을 돌려줘야 하는 경우
        if (holder instanceof Reforge_UI) {
            ((Reforge_UI) holder).handleClose(event);
        }

        if (holder instanceof Profile_UI) {
            ((Profile_UI) holder).cancelRefreshTask();
        }
    }
}