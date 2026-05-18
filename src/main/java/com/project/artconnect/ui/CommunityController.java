package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class CommunityController {

    @FXML private TableView<CommunityMember> memberTable;
    @FXML private TableColumn<CommunityMember, String> nameColumn;
    @FXML private TableColumn<CommunityMember, String> emailColumn;
    @FXML private TableColumn<CommunityMember, String> cityColumn;

    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        showMemberDialog(null).ifPresent(member -> {
            try {
                communityService.createMember(member);
                refreshTable();
                showInfo("Succès", "Membre \"" + member.getName() + "\" ajouté.");
            } catch (Exception e) {
                showError("Erreur d'ajout", e.getMessage());
            }
        });
    }

    @FXML
    private void handleEdit() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un membre à modifier.");
            return;
        }
        showMemberDialog(selected).ifPresent(updated -> {
            try {
                communityService.updateMember(updated);
                refreshTable();
                showInfo("Succès", "Membre mis à jour.");
            } catch (Exception e) {
                showError("Erreur de modification", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélection requise", "Veuillez sélectionner un membre à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le membre \"" + selected.getName() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    communityService.deleteMember(selected.getName());
                    refreshTable();
                    showInfo("Succès", "Membre supprimé.");
                } catch (Exception e) {
                    showError("Erreur de suppression", e.getMessage());
                }
            }
        });
    }

    private Optional<CommunityMember> showMemberDialog(CommunityMember existing) {
        Dialog<CommunityMember> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Ajouter un membre" : "Modifier le membre");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField  = new TextField(existing != null ? existing.getName() : "");
        TextField emailField = new TextField(existing != null ? existing.getEmail() : "");
        TextField cityField  = new TextField(existing != null ? existing.getCity() : "");

        if (existing != null) nameField.setDisable(true); // name is PK

        grid.addRow(0, new Label("Nom *:"), nameField);
        grid.addRow(1, new Label("Email:"), emailField);
        grid.addRow(2, new Label("Ville:"), cityField);
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
                CommunityMember m = new CommunityMember();
                m.setName(existing != null ? existing.getName() : nameField.getText().trim());
                m.setEmail(emailField.getText().trim());
                m.setCity(cityField.getText().trim());
                return m;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void refreshTable() {
        try {
            memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
        } catch (Exception e) {
            memberTable.setItems(FXCollections.observableArrayList());
            showError("Erreur de chargement", "Impossible de charger les membres : " + e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }

    private void showInfo(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK){{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }
}
