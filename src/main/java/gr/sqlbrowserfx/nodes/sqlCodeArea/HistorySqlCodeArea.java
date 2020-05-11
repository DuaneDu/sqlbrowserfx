package gr.sqlbrowserfx.nodes.sqlCodeArea;

import gr.bashfx.SearchAndReplacePopOver;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class HistorySqlCodeArea extends SqlCodeArea {

	public HistorySqlCodeArea() {
		super();
		searchAndReplacePopOver = new SearchAndReplacePopOver(this, false);
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();

		MenuItem menuItemCopy = new MenuItem("Copy", JavaFXUtils.icon("/res/copy.png"));
		menuItemCopy.setOnAction(event -> this.copy());
		
		MenuItem menuItemSearchAndReplace = new MenuItem("Search...", JavaFXUtils.icon("/res/magnify.png"));
		menuItemSearchAndReplace.setOnAction(action -> this.showSearchAndReplacePopup());

		menu.getItems().addAll(menuItemCopy, menuItemSearchAndReplace);
		return menu;
	}
}
