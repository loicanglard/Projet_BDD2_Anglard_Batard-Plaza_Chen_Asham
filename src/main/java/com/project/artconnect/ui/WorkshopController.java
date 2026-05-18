package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class WorkshopController {

    @FXML private TableView<Workshop> workshopTable;
    @FXML private TableColumn<Workshop, String> titleColumn;
    @FXML private TableColumn<Workshop, String> dateColumn;
    @FXML private TableColumn<Workshop, String> instructorColumn;
    @FXML private TableColumn<Workshop, Double> priceColumn;
    @FXML private TableColumn<Workshop, String> levelColumn;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtistService artistService     = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDate() != null ? cellData.getValue().getDate().format(DT_FMT) : ""));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null ? cellData.getValue().getInstructor().getName() : "Unknown"));
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        List<Artist> artists = loadArtists();
        showWorkshopDialog(null, artists).ifPresent(workshop -> {
            try {
                workshopService.createWorkshop(workshop);
                refreshTable();
                showInfo("Succès", "Atelier \"" + workshop.getTitle() + "\" ajouté.");
            } catch (Exception e) {
                showError("Erreur d'ajout", e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un atelier à modifier.");
            return;
        }
        List<Artist> artists = loadArtists();
        showWorkshopDialog(selected, artists).ifPresent(updated -> {
            try {
                workshopService.updateWorkshop(updated);
                refreshTable();
                showInfo("Succès", "Atelier mis à jour.");
            } catch (Exception e) {
                showError("Erreur de modification", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un atelier à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'atelier \"" + selected.getTitle() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    workshopService.deleteWorkshop(selected.getTitle());
                    refreshTable();
                    showInfo("Succès", "Atelier supprimé.");
                } catch (Exception e) {
                    showError("Erreur de suppression", e.getMessage());
                }
            }
        });
    }

    private Optional<Workshop> showWorkshopDialog(Workshop existing, List<Artist> artists) {
        Dialog<Workshop> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter un atelier" : "Modifier l'atelier");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField titleField = new TextField(existing != null ? existing.getTitle() : "");
        TextField dateField  = new TextField(existing != null && existing.getDate() != null
                ? existing.getDate().format(DT_FMT) : "2025-01-01 10:00");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "0.0");
        ComboBox<String> levelBox = new ComboBox<>(FXCollections.observableArrayList("beginner", "intermediate", "advanced"));
        levelBox.setValue(existing != null && existing.getLevel() != null ? existing.getLevel() : "beginner");

        ComboBox<Artist> artistBox = new ComboBox<>(FXCollections.observableArrayList(artists));
        artistBox.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Artist a) { return a != null ? a.getName() : ""; }
            public Artist fromString(String s) { return null; }
        });
        if (existing != null && existing.getInstructor() != null) {
            artists.stream().filter(a -> a.getName().equals(existing.getInstructor().getName()))
                    .findFirst().ifPresent(artistBox::setValue);
        }

        if (existing != null) titleField.setDisable(true);

        grid.addRow(0, new Label("Titre *:"), titleField);
        grid.addRow(1, new Label("Date (yyyy-MM-dd HH:mm):"), dateField);
        grid.addRow(2, new Label("Prix (€):"), priceField);
        grid.addRow(3, new Label("Niveau:"), levelBox);
        grid.addRow(4, new Label("Instructeur:"), artistBox);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (titleField.getText().trim().isEmpty()) return null;
                Workshop w = new Workshop();
                w.setTitle(titleField.getText().trim());
                try { w.setDate(LocalDateTime.parse(dateField.getText().trim(), DT_FMT)); }
                catch (DateTimeParseException ignored) {}
                try { w.setPrice(Double.parseDouble(priceField.getText().trim())); }
                catch (NumberFormatException ignored) {}
                w.setLevel(levelBox.getValue());
                w.setInstructor(artistBox.getValue());
                return w;
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
            workshopTable.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
        } catch (Exception e) {
            workshopTable.setItems(FXCollections.observableArrayList());
            showError("Erreur de chargement", "Impossible de charger les ateliers : " + e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }

    private void showInfo(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }
}
