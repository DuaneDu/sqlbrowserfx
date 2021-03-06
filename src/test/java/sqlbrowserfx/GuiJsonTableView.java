package sqlbrowserfx;

import org.json.JSONArray;

import gr.sqlbrowserfx.nodes.tableviews.MapTableView;
import gr.sqlbrowserfx.utils.HTTPClient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class GuiJsonTableView extends Application{

	public static void main(String[] args) {
//		BasicConfigurator.configure();
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			primaryStage.setTitle("SqlBrowser");
			JSONArray jsonArray = new JSONArray(HTTPClient.GET("https://www.psantamouris.gr/get/customers"));
			MapTableView tableView = new MapTableView();
			tableView.setItemsLater(jsonArray);
			primaryStage.setScene(new Scene(new VBox(new TextField(), tableView)));
			primaryStage.show();
		} catch (Throwable e) {
			e.printStackTrace();
		}


	}
}
