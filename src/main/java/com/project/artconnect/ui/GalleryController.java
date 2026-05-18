package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class GalleryController {

    @FXML private TableView<Gallery> galleryTable;
    @FXML private TableColumn<Gallery, String> nameColumn;
    @FXML private TableColumn<Gallery, String> addressColumn;
    @FXML private TableColumn<Gallery, Double> ratingColumn;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        addressColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAddress()));
        ratingColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getRating()).asObject());
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showGalleryDialog(null).ifPresent(gallery -> {
            try {
                galleryService.createGallery(gallery);
                refreshTable();
                showInfo("Succès", "Galerie \"" + gallery.getName() + "\" ajoutée.");
            } catch (Exception e) {
                showError("Erreur d'ajout", e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Gallery selected = galleryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner une galerie à modifier.");
            return;
        }
        showGalleryDialog(selected).ifPresent(updated -> {
            try {
                galleryService.updateGallery(updated);
                refreshTable();
                showInfo("Succès", "Galerie mise à jour.");
            } catch (Exception e) {
                showError("Erreur de modification", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Gallery selected = galleryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner une galerie à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la galerie \"" + selected.getName() + "\" (et ses expositions) ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    galleryService.deleteGallery(selected.getName());
                    refreshTable();
                    showInfo("Succès", "Galerie supprimée.");
                } catch (Exception e) {
                    showError("Erreur de suppression", e.getMessage());
                }
            }
        });
    }

    private Optional<Gallery> showGalleryDialog(Gallery existing) {
        Dialog<Gallery> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter une galerie" : "Modifier la galerie");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField    = new TextField(existing != null ? existing.getName() : "");
        TextField addressField = new TextField(existing != null ? existing.getAddress() : "");
        TextField ratingField  = new TextField(existing != null ? String.valueOf(existing.getRating()) : "0.0");

        if (existing != null) nameField.setDisable(true); // name is PK

        grid.addRow(0, new Label("Nom *:"), nameField);
        grid.addRow(1, new Label("Adresse:"), addressField);
        grid.addRow(2, new Label("Note (0-5):"), ratingField);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (nameField.getText().trim().isEmpty()) {
                showError("Champ requis", "Le nom est obligatoire.");
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Gallery g = new Gallery();
                g.setName(existing != null ? existing.getName() : nameField.getText().trim());
                g.setAddress(addressField.getText().trim());
                try { g.setRating(Double.parseDouble(ratingField.getText().trim())); }
                catch (NumberFormatException ignored) {}
                return g;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        try {
            galleryTable.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));
        } catch (Exception e) {
            galleryTable.setItems(FXCollections.observableArrayList());
            showError("Erreur de chargement", "Impossible de charger les galeries : " + e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }

    private void showInfo(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }
}
