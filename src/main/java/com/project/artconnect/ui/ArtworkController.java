package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

public class ArtworkController {

    @FXML private TableView<Artwork> artworkTable;
    @FXML private TableColumn<Artwork, String> titleColumn;
    @FXML private TableColumn<Artwork, String> typeColumn;
    @FXML private TableColumn<Artwork, Double> priceColumn;
    @FXML private TableColumn<Artwork, String> statusColumn;
    @FXML private TableColumn<Artwork, String> artistColumn;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService artistService   = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().toString() : ""));
        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null ? cellData.getValue().getArtist().getName() : "Unknown"));
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        List<Artist> artists = loadArtists();
        showArtworkDialog(null, artists).ifPresent(artwork -> {
            try {
                artworkService.createArtwork(artwork);
                refreshTable();
                showInfo("Succès", "Œuvre \"" + artwork.getTitle() + "\" ajoutée.");
            } catch (Exception e) {
                showError("Erreur d'ajout", e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner une œuvre à modifier.");
            return;
        }
        List<Artist> artists = loadArtists();
        showArtworkDialog(selected, artists).ifPresent(updated -> {
            try {
                artworkService.updateArtwork(updated);
                refreshTable();
                showInfo("Succès", "Œuvre mise à jour.");
            } catch (Exception e) {
                showError("Erreur de modification", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner une œuvre à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + selected.getTitle() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    artworkService.deleteArtwork(selected.getTitle());
                    refreshTable();
                    showInfo("Succès", "Œuvre supprimée.");
                } catch (Exception e) {
                    showError("Erreur de suppression", e.getMessage());
                }
            }
        });
    }

    private Optional<Artwork> showArtworkDialog(Artwork existing, List<Artist> artists) {
        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter une œuvre" : "Modifier l'œuvre");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        TextField typeField  = new TextField(existing != null ? existing.getType() : "");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "0.0");

        ComboBox<Artwork.Status> statusBox = new ComboBox<>(
                FXCollections.observableArrayList(Artwork.Status.values()));
        statusBox.setValue(existing != null ? existing.getStatus() : Artwork.Status.FOR_SALE);

        ComboBox<Artist> artistBox = new ComboBox<>(FXCollections.observableArrayList(artists));
        if (existing != null && existing.getArtist() != null) {
            artists.stream().filter(a -> a.getName().equals(existing.getArtist().getName()))
                    .findFirst().ifPresent(artistBox::setValue);
        }
        artistBox.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Artist a) { return a != null ? a.getName() : ""; }
            public Artist fromString(String s) { return null; }
        });

        if (existing != null) titleField.setDisable(true); // title is PK

        grid.addRow(0, new Label("Titre *:"), titleField);
        grid.addRow(1, new Label("Type:"), typeField);
        grid.addRow(2, new Label("Prix (€):"), priceField);
        grid.addRow(3, new Label("Statut:"), statusBox);
        grid.addRow(4, new Label("Artiste:"), artistBox);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (titleField.getText().trim().isEmpty()) {
                showError("Champ requis", "Le titre est obligatoire.");
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Artwork a = new Artwork();
                a.setTitle(titleField.getText().trim());
                a.setType(typeField.getText().trim());
                try { a.setPrice(Double.parseDouble(priceField.getText().trim())); }
                catch (NumberFormatException ignored) {}
                a.setStatus(statusBox.getValue() != null ? statusBox.getValue() : Artwork.Status.FOR_SALE);
                a.setArtist(artistBox.getValue());
                return a;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private List<Artist> loadArtists() {
        try { return artistService.getAllArtists(); }
        catch (Exception e) { return List.of(); }
    }

    private void refreshTable() {
        try {
            artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
        } catch (Exception e) {
            artworkTable.setItems(FXCollections.observableArrayList());
            showError("Erreur de chargement", "Impossible de charger les œuvres : " + e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }

    private void showInfo(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }
}
