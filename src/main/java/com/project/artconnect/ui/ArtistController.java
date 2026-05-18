package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.util.Optional;

public class ArtistController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Discipline> disciplineFilter;
    @FXML private TableView<Artist> artistTable;
    @FXML private TableColumn<Artist, String> nameColumn;
    @FXML private TableColumn<Artist, String> cityColumn;
    @FXML private TableColumn<Artist, String> emailColumn;
    @FXML private TableColumn<Artist, Integer> yearColumn;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));

        try {
            disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        } catch (Exception e) {
            showError("Erreur de connexion", "Impossible de charger les disciplines : " + e.getMessage());
        }
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        try {
            String query = searchField.getText();
            Discipline d = disciplineFilter.getValue();
            String dName = (d != null) ? d.getName() : null;
            artistTable.setItems(FXCollections.observableArrayList(
                    artistService.searchArtists(query, dName, null)));
        } catch (Exception e) {
            showError("Erreur de recherche", e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showArtistDialog(null).ifPresent(artist -> {
            try {
                artistService.createArtist(artist);
                refreshTable();
                showInfo("Succès", "Artiste \"" + artist.getName() + "\" ajouté.");
            } catch (Exception e) {
                showError("Erreur d'ajout", e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un artiste à modifier.");
            return;
        }
        showArtistDialog(selected).ifPresent(updated -> {
            try {
                artistService.updateArtist(updated);
                refreshTable();
                showInfo("Succès", "Artiste mis à jour.");
            } catch (Exception e) {
                showError("Erreur de modification", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un artiste à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'artiste \"" + selected.getName() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    artistService.deleteArtist(selected.getName());
                    refreshTable();
                    showInfo("Succès", "Artiste supprimé.");
                } catch (Exception e) {
                    showError("Erreur de suppression", e.getMessage());
                }
            }
        });
    }

    private Optional<Artist> showArtistDialog(Artist existing) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter un artiste" : "Modifier l'artiste");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField   = new TextField(existing != null ? existing.getName() : "");
        TextField cityField   = new TextField(existing != null ? existing.getCity() : "");
        TextField emailField  = new TextField(existing != null ? existing.getContactEmail() : "");
        TextField yearField   = new TextField(existing != null && existing.getBirthYear() != null
                ? String.valueOf(existing.getBirthYear()) : "");
        TextField discField   = new TextField(existing != null && !existing.getDisciplines().isEmpty()
                ? existing.getDisciplines().get(0).getName() : "");

        if (existing != null) nameField.setDisable(true); // name is PK, can't change

        grid.addRow(0, new Label("Nom *:"), nameField);
        grid.addRow(1, new Label("Ville:"), cityField);
        grid.addRow(2, new Label("Email:"), emailField);
        grid.addRow(3, new Label("Année naissance:"), yearField);
        grid.addRow(4, new Label("Discipline:"), discField);

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
                Artist a = new Artist();
                a.setName(nameField.getText().trim());
                a.setCity(cityField.getText().trim().isEmpty() ? null : cityField.getText().trim());
                a.setContactEmail(emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                if (!yearField.getText().trim().isEmpty()) {
                    try { a.setBirthYear(Integer.parseInt(yearField.getText().trim())); }
                    catch (NumberFormatException ignored) {}
                }
                if (!discField.getText().trim().isEmpty()) {
                    a.getDisciplines().add(new Discipline(discField.getText().trim()));
                }
                return a;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        try {
            artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
        } catch (Exception e) {
            artistTable.setItems(FXCollections.observableArrayList());
            showError("Erreur de chargement", "Impossible de charger les artistes : " + e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
