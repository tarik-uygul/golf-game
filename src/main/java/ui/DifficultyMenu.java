package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class DifficultyMenu {

    private VBox layout;

    public DifficultyMenu(GolfApp app) {

        // title
        Text title = new Text("SELECT DIFFICULTY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 38));
        title.setFill(Color.WHITE);

        Text subtitle = new Text("Choose your difficulty");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.color(1, 1, 1, 0.70));

        VBox titleBlock = new VBox(6, title, subtitle);
        titleBlock.setAlignment(Pos.CENTER);

        // difficulty buttons
        Button easyBtn = makeDifficultyButton(
                "Easy", "description of easy course",
                "#27ae60", "#2ecc71");

        Button mediumBtn = makeDifficultyButton(
                "Medium", "description of medium course",
                "#d4a017", "#f1c40f");

        Button hardBtn = makeDifficultyButton(
                "Hard", "description of hard course",
                "#c0392b", "#e74c3c");

        easyBtn.setOnAction(e -> app.startGame("Easy"));
        mediumBtn.setOnAction(e -> app.startGame("Medium"));
        hardBtn.setOnAction(e -> app.startGame("Hard"));

        // back button
        Button backBtn = new Button("Back");
        backBtn.setPrefSize(150, 45);
        backBtn.setFocusTraversable(false);

        String backBase = flatStyle("#666666");
        String backHover = flatStyle("#777777");
        backBtn.setStyle(backBase);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backHover));
        backBtn.setOnMouseExited (e -> backBtn.setStyle(backBase));
        backBtn.setOnAction(e -> app.showMainMenu());

        // layout
        layout = new VBox(22);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.getChildren().addAll(titleBlock, easyBtn, mediumBtn, hardBtn, backBtn);
        layout.setBackground(new Background(new BackgroundFill(Color.web("#1a5c2a"), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // create button with hover effect
    private Button makeDifficultyButton(String label, String description, String baseColor, String hoverColor) {
        Text labelText = new Text(label);
        labelText.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        labelText.setFill(Color.WHITE);

        Text descText = new Text(description);
        descText.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        descText.setFill(Color.color(1, 1, 1, 0.80));

        VBox content = new VBox(3, labelText, descText);
        content.setAlignment(Pos.CENTER);

        Button btn = new Button();
        btn.setGraphic(content);
        btn.setPrefSize(300, 80);
        btn.setFocusTraversable(false);

        String base = flatStyle(baseColor);
        String hover = flatStyle(hoverColor);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited (e -> btn.setStyle(base));
        return btn;
    }

    // default string style
    private String flatStyle(String bgColor) {
        return "-fx-background-color: " + bgColor + ";" +
               "-fx-background-radius: 15;" +
               "-fx-cursor: hand;";
    }

    public VBox getLayout() { return layout; }
}