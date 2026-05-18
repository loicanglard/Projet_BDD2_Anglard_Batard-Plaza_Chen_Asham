package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcArtworkDao implements ArtworkDao {

    private Artwork.Status mapStatus(String dbStatus) {
        if ("Vendu".equalsIgnoreCase(dbStatus)) return Artwork.Status.SOLD;
        return Artwork.Status.FOR_SALE;
    }

    private String mapStatusToDb(Artwork.Status status) {
        if (status == Artwork.Status.SOLD) return "Vendu";
        return "Disponible";
    }

    private Artwork mapRow(ResultSet rs) throws SQLException {
        Artwork a = new Artwork();
        a.setTitle(rs.getString("Artwork_Title"));
        a.setPrice(rs.getDouble("Price"));
        a.setType(rs.getString("Type"));
        a.setStatus(mapStatus(rs.getString("Status")));

        String artistName = rs.getString("Artist_name");
        if (artistName != null) {
            Artist artist = new Artist();
            artist.setName(artistName);
            try {
                artist.setContactEmail(rs.getString("Artist_email"));
                artist.setCity(rs.getString("Artist_city"));
                Date birthDate = rs.getDate("Artist_Birth_Year");
                if (birthDate != null) {
                    artist.setBirthYear(birthDate.toLocalDate().getYear());
                }
                String discipline = rs.getString("Artist_Discipline");
                if (discipline != null && !discipline.isEmpty()) {
                    artist.getDisciplines().add(new Discipline(discipline));
                }
            } catch (SQLException ignored) {
                // artist join columns may not always be present
            }
            a.setArtist(artist);
        }
        return a;
    }

    private int nextId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Artwork_id), 0) + 1 FROM Artwork")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Override
    public List<Artwork> findAll() {
        List<Artwork> result = new ArrayList<>();
        String sql = "SELECT a.Artwork_id, a.Artwork_Title, a.Price, a.Type, a.Status, " +
                     "art.Artist_name, art.Artist_email, art.Artist_city, art.Artist_Birth_Year, art.Artist_Discipline " +
                     "FROM Artwork a " +
                     "LEFT JOIN CreatedBy cb ON a.Artwork_id = cb.Artwork_id " +
                     "LEFT JOIN Artist art ON cb.Artist_id = art.Artist_id";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll artworks failed", e);
        }
        return result;
    }

    @Override
    public void save(Artwork artwork) {
        String sqlArtwork = "INSERT INTO Artwork(Artwork_id, Artwork_Title, Price, Type, Status) VALUES (?, ?, ?, ?, ?)";
        String sqlCreatedBy = "INSERT INTO CreatedBy(Artist_id, Artwork_id) " +
                              "SELECT Artist_id, ? FROM Artist WHERE Artist_name=?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int artworkId;
                try (PreparedStatement ps = conn.prepareStatement(sqlArtwork)) {
                    artworkId = nextId(conn);
                    ps.setInt(1, artworkId);
                    ps.setString(2, artwork.getTitle());
                    ps.setDouble(3, artwork.getPrice());
                    ps.setString(4, artwork.getType());
                    ps.setString(5, mapStatusToDb(artwork.getStatus()));
                    ps.executeUpdate();
                }
                if (artwork.getArtist() != null && artwork.getArtist().getName() != null) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlCreatedBy)) {
                        ps.setInt(1, artworkId);
                        ps.setString(2, artwork.getArtist().getName());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("save artwork failed", e);
        }
    }

    @Override
    public void update(Artwork artwork) {
        String sql = "UPDATE Artwork SET Price=?, Type=?, Status=? WHERE Artwork_Title=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, artwork.getPrice());
            ps.setString(2, artwork.getType());
            ps.setString(3, mapStatusToDb(artwork.getStatus()));
            ps.setString(4, artwork.getTitle());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update artwork failed", e);
        }
    }

    @Override
    public void delete(String title) {
        String sqlFeatures   = "DELETE FROM Features WHERE Artwork_id = (SELECT Artwork_id FROM Artwork WHERE Artwork_Title=?)";
        String sqlCreatedBy  = "DELETE FROM CreatedBy WHERE Artwork_id = (SELECT Artwork_id FROM Artwork WHERE Artwork_Title=?)";
        String sqlArtwork    = "DELETE FROM Artwork WHERE Artwork_Title=?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlFeatures)) {
                    ps.setString(1, title);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlCreatedBy)) {
                    ps.setString(1, title);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlArtwork)) {
                    ps.setString(1, title);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("delete artwork failed", e);
        }
    }

    @Override
    public List<Artwork> findByArtistName(String artistName) {
        List<Artwork> result = new ArrayList<>();
        String sql = "SELECT a.Artwork_id, a.Artwork_Title, a.Price, a.Type, a.Status, " +
                     "art.Artist_name, art.Artist_email, art.Artist_city, art.Artist_Birth_Year, art.Artist_Discipline " +
                     "FROM Artwork a " +
                     "JOIN CreatedBy cb ON a.Artwork_id = cb.Artwork_id " +
                     "JOIN Artist art ON cb.Artist_id = art.Artist_id " +
                     "WHERE art.Artist_name=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artistName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByArtistName artworks failed", e);
        }
        return result;
    }
}
