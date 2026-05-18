use bdd2_project;
CREATE TABLE Member_(
   Member_id INT,
   Member_name VARCHAR(50) NOT NULL,
   Member_email VARCHAR(50) NOT NULL,
   Member_City VARCHAR(50) NOT NULL,
   PRIMARY KEY(Member_id),
   UNIQUE(Member_email)
);

CREATE TABLE Artist(
   Artist_id INT,
   Artist_name VARCHAR(50) NOT NULL,
   Artist_Birth_Year DATE NOT NULL,
   Artist_Discipline VARCHAR(50) NOT NULL,
   Artist_email VARCHAR(50) NOT NULL,
   Artist_city VARCHAR(50) NOT NULL,
   PRIMARY KEY(Artist_id),
   UNIQUE(Artist_email)
);

CREATE TABLE Artwork(
   Artwork_id INT,
   Artwork_Title VARCHAR(50) NOT NULL,
   Price DECIMAL(15,2) NOT NULL,
   Type VARCHAR(50) NOT NULL,
   Status VARCHAR(50) NOT NULL,
   PRIMARY KEY(Artwork_id)
);

CREATE TABLE Gallery(
   Gallery_id INT,
   Gallery_name VARCHAR(50) NOT NULL,
   Gallery_address VARCHAR(50) NOT NULL,
   Rating INT NOT NULL,
   PRIMARY KEY(Gallery_id),
   UNIQUE(Gallery_name),
   UNIQUE(Gallery_address)
);

CREATE TABLE Workshop(
   Workshop_id INT,
   Workshop_Title VARCHAR(50) NOT NULL,
   Workshop_Date DATETIME NOT NULL,
   Workshop_Price DECIMAL(15,2) NOT NULL,
   Workshop_Level VARCHAR(50) NOT NULL,
   Artist_id INT,
   PRIMARY KEY(Workshop_id),
   FOREIGN KEY(Artist_id) REFERENCES Artist(Artist_id)
);

CREATE TABLE Exhibition(
   Exhibition_id INT,
   Exhibition_Title VARCHAR(50) NOT NULL,
   Exhibition_Date DATE NOT NULL,
   Exhibition_Theme VARCHAR(50) NOT NULL,
   Gallery_id INT NOT NULL,
   PRIMARY KEY(Exhibition_id),
   UNIQUE(Exhibition_Title),
   FOREIGN KEY(Gallery_id) REFERENCES Gallery(Gallery_id)
);

CREATE TABLE Participates(
   Member_id INT,
   Workshop_id INT,
   PRIMARY KEY(Member_id, Workshop_id),
   FOREIGN KEY(Member_id) REFERENCES Member_(Member_id),
   FOREIGN KEY(Workshop_id) REFERENCES Workshop(Workshop_id)
);

CREATE TABLE CreatedBy(
   Artist_id INT,
   Artwork_id INT,
   PRIMARY KEY(Artist_id, Artwork_id),
   FOREIGN KEY(Artist_id) REFERENCES Artist(Artist_id),
   FOREIGN KEY(Artwork_id) REFERENCES Artwork(Artwork_id)
);

CREATE TABLE Features(
   Artwork_id INT,
   Exhibition_id INT,
   PRIMARY KEY(Artwork_id, Exhibition_id),
   FOREIGN KEY(Artwork_id) REFERENCES Artwork(Artwork_id),
   FOREIGN KEY(Exhibition_id) REFERENCES Exhibition(Exhibition_id)
);

-- =========================
-- MEMBERS
-- =========================
INSERT INTO Member_ VALUES
(1, 'Alice Martin', 'alice@email.com', 'Paris'),
(2, 'Lucas Bernard', 'lucas@email.com', 'Lyon'),
(3, 'Sophie Dubois', 'sophie@email.com', 'Marseille'),
(4, 'Emma Leroy', 'emma@email.com', 'Toulouse'),
(5, 'Hugo Petit', 'hugo@email.com', 'Nice');

-- =========================
-- ARTISTS
-- =========================
INSERT INTO Artist VALUES
(1, 'Jean Dupont', '1985-06-15', 'Peinture', 'jean@email.com', 'Paris'),
(2, 'Clara Moreau', '1990-09-22', 'Sculpture', 'clara@email.com', 'Lille'),
(3, 'Nicolas Petit', '1978-03-10', 'Photographie', 'nicolas@email.com', 'Bordeaux'),
(4, 'Sarah Nguyen', '1988-12-05', 'Peinture', 'sarah@email.com', 'Paris');

-- =========================
-- ARTWORKS
-- =========================
INSERT INTO Artwork VALUES
(1, 'Coucher de Soleil', 1200.00, 'Peinture', 'Disponible'),
(2, 'Forme Moderne', 2500.00, 'Sculpture', 'Vendu'),
(3, 'Instant Urbain', 800.00, 'Photographie', 'Disponible'),
(4, 'Reflets d\'été', 1500.00, 'Peinture', 'Disponible'),
(5, 'Structure Brisée', 3000.00, 'Sculpture', 'Disponible'),
(6, 'Noir et Lumière', 950.00, 'Photographie', 'Vendu');

-- =========================
-- GALLERIES
-- =========================
INSERT INTO Gallery VALUES
(1, 'Galerie Lumière', '10 rue de Rivoli, Paris', 5),
(2, 'Art Space Lyon', '25 rue République, Lyon', 4),
(3, 'Modern Art Center', '5 avenue Prado, Marseille', 5);

-- =========================
-- WORKSHOPS
-- =========================
INSERT INTO Workshop VALUES
(1, 'Initiation peinture', '2026-05-10 10:00:00', 50.00, 'Débutant', 1),
(2, 'Sculpture avancée', '2026-06-15 14:00:00', 120.00, 'Avancé', 2),
(3, 'Photo urbaine', '2026-05-20 09:00:00', 80.00, 'Intermédiaire', 3),
(4, 'Peinture abstraite', '2026-06-01 11:00:00', 70.00, 'Intermédiaire', 4);

-- =========================
-- EXHIBITIONS
-- =========================
INSERT INTO Exhibition VALUES
(1, 'Couleurs et Émotions', '2026-07-01', 'Peinture contemporaine', 1),
(2, 'Formes et Matières', '2026-08-15', 'Sculpture moderne', 2),
(3, 'Regards Urbains', '2026-09-10', 'Photographie', 3);

-- =========================
-- PARTICIPATIONS (cas croisés)
-- =========================
INSERT INTO Participates VALUES
(1, 1),
(1, 3),  -- Alice participe à plusieurs workshops
(2, 1),
(2, 2),  -- Lucas aussi
(3, 3),
(4, 4),
(5, 2),
(5, 3);  -- Hugo participe à deux disciplines différentes

-- =========================
-- CREATED BY (1 artiste -> plusieurs œuvres)
-- =========================
INSERT INTO CreatedBy VALUES
(1, 1),
(1, 4),  -- Jean a fait plusieurs peintures
(2, 2),
(2, 5),  -- Clara a fait plusieurs sculptures
(3, 3),
(3, 6),  -- Nicolas plusieurs photos
(4, 4);  -- Sarah partage une œuvre avec Jean (cas intéressant)

-- =========================
-- FEATURES (œuvres dans plusieurs expositions)
-- =========================
INSERT INTO Features VALUES
(1, 1),
(4, 1),
(4, 2),  -- même œuvre dans 2 expos
(2, 2),
(5, 2),
(3, 3),
(6, 3),
(3, 1);  -- une photo aussi exposée dans expo peinture (cas mixte)

-- ==========================================================
-- ÉTAPE 3 : OBJETS AVANCÉS (VUES ET INDEX)
-- ==========================================================

-- ----------------------------------------------------------
-- 1. CRÉATION DES VUES (VIEWS)
-- ----------------------------------------------------------

-- A. Vue de Sécurité : Masquer l'email personnel des artistes
-- Objectif : Permettre l'affichage public sans exposer de données sensibles.
CREATE VIEW View_Artist_Public AS
SELECT Artist_name, Artist_Birth_Year, Artist_Discipline, Artist_city
FROM Artist;

-- B. Vue de Simplification : Catalogue complet des œuvres avec leurs auteurs
-- Objectif : Éviter les jointures complexes (Artwork + CreatedBy + Artist) pour les requêtes fréquentes.
CREATE VIEW View_Artwork_Details AS
SELECT 
    a.Artwork_Title, 
    a.Price, 
    a.Type, 
    a.Status, 
    art.Artist_name
FROM Artwork a
JOIN CreatedBy cb ON a.Artwork_id = cb.Artwork_id
JOIN Artist art ON cb.Artist_id = art.Artist_id;

-- C. Vue de Gestion : Planning des Workshops et affluence
-- Objectif : Afficher les ateliers avec le nom de l'artiste et le nombre d'inscrits.
CREATE VIEW View_Workshop_Schedule AS
SELECT 
    w.Workshop_Title, 
    w.Workshop_Date, 
    w.Workshop_Level, 
    art.Artist_name, 
    COUNT(p.Member_id) AS Total_Participants
FROM Workshop w
JOIN Artist art ON w.Artist_id = art.Artist_id
LEFT JOIN Participates p ON w.Workshop_id = p.Workshop_id
GROUP BY w.Workshop_id, w.Workshop_Title, w.Workshop_Date, w.Workshop_Level, art.Artist_name;


-- ----------------------------------------------------------
-- 2. CRÉATION DES INDEX (INDEXES)
-- ----------------------------------------------------------

-- Optimisation des recherches chronologiques (Expositions et Ateliers)
-- Justification : Améliore la vitesse du tri par date pour les calendriers.
CREATE INDEX idx_exhibition_date ON Exhibition(Exhibition_Date);
CREATE INDEX idx_workshop_date ON Workshop(Workshop_Date);

-- Optimisation des filtres par catégorie
-- Justification : Accélère les recherches par discipline artistique ou type d'œuvre.
CREATE INDEX idx_artist_discipline ON Artist(Artist_Discipline);
CREATE INDEX idx_artwork_type ON Artwork(Type);

-- Optimisation du statut commercial
-- Justification : Indispensable pour filtrer rapidement les œuvres encore "Disponibles".
CREATE INDEX idx_artwork_status ON Artwork(Status);
-- =============================================
-- ÉTAPE 3.3 : TRIGGER ET PROCÉDURE
-- =============================================

-- 1. Trigger pour empêcher les prix invalides
DELIMITER //
CREATE TRIGGER before_artwork_update
BEFORE UPDATE ON Artwork
FOR EACH ROW
BEGIN
    IF NEW.Price <= 0 THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Le prix d''une oeuvre doit être supérieur à 0';
    END IF;
END //
DELIMITER ;

-- 2. Procédure pour ajouter un membre facilement
DELIMITER //
CREATE PROCEDURE AddNewMember(
    IN p_id INT,
    IN p_name VARCHAR(50),
    IN p_email VARCHAR(50),
    IN p_city VARCHAR(50)
)
BEGIN
    INSERT INTO Member_(Member_id, Member_name, Member_email, Member_City)
    VALUES (p_id, p_name, p_email, p_city);
END //
DELIMITER ;
-- ==========================================================
-- ÉTAPE 4 : VALIDATION ET EXPLOITATION DE LA BASE
-- ==========================================================

-- ----------------------------------------------------------
-- 1. ANALYSE ET STATISTIQUES (Requêtes complexes)
-- ----------------------------------------------------------

-- A. Chiffre d'affaires potentiel par discipline (uniquement œuvres disponibles)
-- Objectif : Connaître la valeur financière du stock actuel.
SELECT Type, SUM(Price) AS Valeur_Stock_Disponible
FROM Artwork
WHERE Status = 'Disponible'
GROUP BY Type;

-- B. Liste détaillée des inscriptions aux Workshops
-- Objectif : Afficher le nom du membre, le titre du workshop et l'artiste animateur.
SELECT 
    m.Member_name AS Nom_Membre, 
    w.Workshop_Title AS Titre_Atelier, 
    art.Artist_name AS Nom_Artiste
FROM Member_ m
JOIN Participates p ON m.Member_id = p.Member_id
JOIN Workshop w ON p.Workshop_id = w.Workshop_id
JOIN Artist art ON w.Artist_id = art.Artist_id;

-- C. Recherche croisée : Œuvres exposées dans un thème spécifique
-- Objectif : Lister les œuvres présentées dans l'exposition "Couleurs et Émotions".
SELECT a.Artwork_Title, a.Price, a.Type
FROM Artwork a
JOIN Features f ON a.Artwork_id = f.Artwork_id
JOIN Exhibition e ON f.Exhibition_id = e.Exhibition_id
WHERE e.Exhibition_Title = 'Couleurs et Émotions';

-- ----------------------------------------------------------
-- 2. VALIDATION DES OBJETS AVANCÉS (Tests)
-- ----------------------------------------------------------

-- A. Test de la Procédure Stockée : Inscription d'un nouveau membre
-- On utilise la procédure créée à l'étape 3.
CALL AddNewMember(6, 'Thomas Durand', 'thomas@email.com', 'Lyon');

-- Vérification de l'insertion
SELECT * FROM Member_ WHERE Member_id = 6;

-- B. Test du Trigger de sécurité : Tentative d'insertion d'un prix invalide
-- Cette requête doit déclencher une erreur 'Le prix d''une oeuvre doit être supérieur à 0'
-- Décommentez la ligne suivante pour tester l'erreur :
-- UPDATE Artwork SET Price = -50.00 WHERE Artwork_id = 1;

-- ----------------------------------------------------------
-- 3. CONSULTATION DES VUES DE GESTION
-- ----------------------------------------------------------

-- Afficher l'état du planning et le nombre d'inscrits par atelier
SELECT * FROM View_Workshop_Schedule;