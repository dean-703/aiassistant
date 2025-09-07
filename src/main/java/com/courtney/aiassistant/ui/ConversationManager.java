package com.courtney.aiassistant.ui;

import com.courtney.aiassistant.controller.ConversationController;
import com.courtney.aiassistant.model.Conversation;
import com.courtney.aiassistant.service.ConversationRepository;
import com.courtney.aiassistant.util.ErrorHandler;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ConversationManager {

    private final Stage stage = new Stage();
    private final ListView<Conversation> listView = new ListView<>();
    private final ConversationRepository repository;

    private Conversation chosen;

    public ConversationManager(Stage owner, ConversationController controller, ConversationRepository repository) {
        this.repository = repository;

        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Conversation Manager");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPrefColumnCount(40);
        searchField.setPromptText("Search conversations ...");

        HBox searchHbox = new HBox();
        searchHbox.setAlignment(Pos.CENTER);
        searchHbox.setPadding(new Insets(0, 10, 10, 10));
        searchHbox.getChildren().add(searchField);

        listView.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    String title = item.getTitle() == null || item.getTitle().isBlank()
                            ? "(untitled)" : item.getTitle();
                    String ts = item.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    setText(title + "  —  " + ts + "  (" + item.getMessages().size() + " messages)");
                }
            }
        });

        listView.setOnMouseClicked(event -> handleDoubleClick(event));

        Button open = new Button("Open");
        Button delete = new Button("Delete");
        Button cancel = new Button("Cancel");
        ToolBar tb = new ToolBar(open, delete, cancel);
        HBox hbox = new HBox(tb);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(10, 20, 0, 20));

        root.setTop(searchHbox);
        root.setCenter(listView);
        root.setBottom(hbox);

        Scene scene = new Scene(root, 640, 420);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);

        listView.requestFocus();

        refresh();

        open.setOnAction(e -> {
            chosen = listView.getSelectionModel().getSelectedItem();
            if (chosen != null) stage.close();
        });
        delete.setOnAction(e -> {
            Conversation sel = listView.getSelectionModel().getSelectedItem();
            if (sel == null) {
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete conversation");
            confirm.setHeaderText("Delete \"" + sel.getTitle() + "\"?");
            confirm.setContentText("This action cannot be undone.");

            ButtonType deleteType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(cancelType, deleteType);

            confirm.showAndWait().ifPresent(bt -> {
                if (bt == deleteType) {
                    try {
                        repository.delete(sel);
                        refresh();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Failed to delete: " + ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                }
            });
        });
        cancel.setOnAction(e -> stage.close());

        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterList(newValue));
    }

    private void filterList(String query) {
        try {
            List<Conversation> allConversations = repository.listAll();
            List<Conversation> filteredConversations = allConversations.stream()
                    .filter(conversation -> conversation.getTitle() != null && conversation.getTitle().toLowerCase().contains(query.toLowerCase()))
                    .toList();
            listView.setItems(FXCollections.observableArrayList(filteredConversations));
        } catch (Exception ex) {
            ErrorHandler.alert("Filter Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void refresh() {
        try {
            List<Conversation> items = repository.listAll();
            listView.setItems(FXCollections.observableArrayList(items));
        } catch (Exception ex) {
            ErrorHandler.alert("Load Error", ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // Method to handle double-click event
    private void handleDoubleClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            chosen = listView.getSelectionModel().getSelectedItem();
            if (chosen != null) {
                stage.close();
            }
        }
    }

    public Optional<Conversation> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(chosen);
    }
}
