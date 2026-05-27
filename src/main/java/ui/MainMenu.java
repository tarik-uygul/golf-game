package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class MainMenu {

    private VBox layout;

    public MainMenu(GolfApp app) {

        // title
        Text icon = new Text("⛳");
        icon.setFont(Font.font("Arial", 56));

        Text title = new Text("CRAZY PUTTING");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        title.setFill(Color.WHITE);

        VBox titleBlock = new VBox(6, icon, title);
        titleBlock.setAlignment(Pos.CENTER);

        // buttons
        Button playButton = makeMenuButton("Play",   "#2e8b57", "#3cb371");
        Button createButton = makeMenuButton("Create", "#2e8b57", "#3cb371");

        playButton.setOnAction(e -> app.difficultyMenu());
        createButton.setOnAction(e -> { /* redirect to creation mode */ });

        // layout
        layout = new VBox(28);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.getChildren().addAll(titleBlock, playButton, createButton);
        layout.setBackground(new Background(new BackgroundFill(Color.web("#1a5c2a"), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // create button with hover effect
    private Button makeMenuButton(String label, String baseColor, String hoverColor) {
        Button btn = new Button(label);
        btn.setPrefSize(260, 65);
        btn.setFocusTraversable(false);

        String base = baseStyle(baseColor);
        String hover = baseStyle(hoverColor);

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited (e -> btn.setStyle(base));
        return btn;
    }

    // default string style
    private String baseStyle(String bgColor) {
        return "-fx-background-color: " + bgColor + ";" +
               "-fx-text-fill: white;" +
               "-fx-font-size: 22px;" +
               "-fx-font-weight: bold;" +
               "-fx-background-radius: 15;";
    }

    public VBox getLayout() { return layout; }
}

