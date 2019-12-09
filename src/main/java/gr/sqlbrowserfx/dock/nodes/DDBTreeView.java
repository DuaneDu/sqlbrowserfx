package gr.sqlbrowserfx.dock.nodes;

import org.dockfx.DockNode;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.nodes.DBTreeView;
import gr.sqlbrowserfx.nodes.sqlPane.SqlPane;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class DDBTreeView extends DBTreeView implements Dockable {

	private DockNode thisDockNode = null;
	private Menu openInSqlPaneMenu;
	
	public DDBTreeView(String dbPath, SqlConnector sqlConnector) {
		super(dbPath, sqlConnector);
		this.setOnContextMenuRequested(menuRequestedEvent -> {
			openInSqlPaneMenu.getItems().clear();
			int i = 1;
			for (SqlPane sqlPane : SqlBrowserFXAppManager.getActiveSqlPanes()) {
				MenuItem item = new MenuItem("SqlPane " + i++);
				item.setOnAction(action2 -> {
					sqlPane.createTableViewWithData(this.getSelectionModel().getSelectedItem().getValue());
				});
				openInSqlPaneMenu.getItems().add(item);
			}
			this.getContextMenu().show(this, menuRequestedEvent.getScreenX(),menuRequestedEvent.getScreenY());
		});
	}

	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Structure", JavaFXUtils.icon("/res/structure.png"));
		}
		return thisDockNode;
	}
	
	@Override
	public ContextMenu createContextMenu() {
		ContextMenu menu = super.createContextMenu();
		openInSqlPaneMenu = new Menu("Open in..." , JavaFXUtils.icon("/res/openTab.png"));
		openInSqlPaneMenu.setOnMenuValidation(action -> {
			openInSqlPaneMenu.getItems().clear();
			for (DSqlPane sqlPane : SqlBrowserFXAppManager.getActiveSqlPanes()) {
				MenuItem item = new MenuItem(sqlPane.asDockNode().getTitle());
				item.setOnAction(action2 -> {
					sqlPane.createTableViewWithData(this.getSelectionModel().getSelectedItem().getValue());
				});
				openInSqlPaneMenu.getItems().add(item);
			}
		});
		menu.getItems().add(menu.getItems().size()-1, openInSqlPaneMenu);
		return menu;
		
	}

}
