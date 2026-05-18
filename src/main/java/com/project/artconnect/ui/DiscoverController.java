package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscoverController {

    @FXML private FlowPane discoverPane;

    private final GalleryService galleryService   = ServiceProvider.getGalleryService();
    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();

    @FXML
    public void initialize() {
        discoverPane.getChildren().clear();

        List<Exhibition> featuredExhibitions = safeLoadExhibitions();
        List<Workshop> workshops             = safeLoadWorkshops();

        if (featuredExhibitions.isEmpty() && workshops.isEmpty()) {
            Label empty = new Label("Aucune donnée disponible. Vérifiez la connexion à la base de données.");
            empty.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
            discoverPane.getChildren().add(empty);
            return;
        }

        featuredExhibitions.stream().limit(3).forEach(this::addExhibitionCard);
        workshops.stream().limit(3).forEach(this::addWorkshopCard);
    }

    private List<Exhibition> safeLoadExhibitions() {
        try {
            List<Exhibition> result = new ArrayList<>();
            for (Gallery g : galleryService.getAllGalleries()) {
                result.addAll(g.getExhibitions());
                if (result.size() >= 6) break;
            }
            return result;
        } catch (Exception e) {
            System.err.println("DiscoverController: could not load exhibitions - " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Workshop> safeLoadWorkshops() {
        try {
            return workshopService.getAllWorkshops();
        } catch (Exception e) {
            System.err.println("DiscoverController: could not load workshops - " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void addExhibitionCard(Exhibition e) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefWidth(250);

        String title   = e.getTitle()   != null ? e.getTitle()   : "(sans titre)";
        String theme   = e.getTheme()   != null ? e.getTheme()   : "-";
        String gallery = (e.getGallery() != null && e.getGallery().getName() != null)
                ? e.getGallery().getName() : "Inconnue";

        Label badge = new Label("EXPOSITION");
        badge.setStyle("-fx-font-size: 10; -fx-text-fill: #1565c0;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        card.getChildren().addAll(
                badge,
                titleLabel,
                new Label("Thème : " + theme),
                new Label("Galerie : " + gallery)
        );
        discoverPane.getChildren().add(card);
    }

    private void addWorkshopCard(Workshop w) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f1f8e9; -fx-border-color: #4caf50; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefWidth(250);

        String title      = w.getTitle() != null ? w.getTitle() : "(sans titre)";
        String instructor = (w.getInstructor() != null && w.getInstructor().getName() != null)
                ? w.getInstructor().getName() : "Inconnu";
        String level      = w.getLevel() != null ? w.getLevel() : "-";

        Label badge = new Label("ATELIER");
        badge.setStyle("-fx-font-size: 10; -fx-text-fill: #2e7d32;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        card.getChildren().addAll(
                badge,
                titleLabel,
                new Label("Instructeur : " + instructor),
                new Label("Niveau : " + level),
                new Label("Prix : " + w.getPrice() + " €")
        );
        discoverPane.getChildren().add(card);
    }
}
