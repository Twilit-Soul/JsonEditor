package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Making this to give me a super easy way to view json resource files at a glance, and modify/save them.
 */
public class Main extends Application {
	@SuppressWarnings("FieldCanBeLocal") //I'd like to use this to resize the UI at some point.
	private static final int WIDTH = 1000, HEIGHT = 775;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
		Parent root = loader.load();
		Controller controller = (Controller)loader.getController();
		controller.setStage(primaryStage);
		primaryStage.setTitle("Json Editor");
		primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
		primaryStage.setResizable(false);
		primaryStage.show();
	}
}
