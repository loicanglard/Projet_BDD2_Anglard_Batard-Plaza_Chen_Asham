package com.project.artconnect.persistence;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcExhibitionDao implements ExhibitionDao {

    private Exhibition mapRow(ResultSet rs) throws SQLException {
        Exhibition e = new Exhibition();
        e.setTitle(rs.getString("Exhibition_Title"));
        Date exhibDate = rs.getDate("Exhibition_Date");
        if (exhibDate != null) {
            e.setStartDate(exhibDate.toLocalDate());
        }
        e.setTheme(rs.getString("Exhibition_Theme"));

        Gallery gallery = new Gallery();
        gallery.setName(rs.getString("Gallery_name"));
        gallery.setAddress(rs.getString("Gallery_address"));
        gallery.setRating(rs.getInt("Rating"));
        e.setGallery(gallery);
        return e;
    }

    private String baseQuery() {
        return "SELECT e.Exhibition_id, e.Exhibition_Title, e.Exhibition_Date, e.Exhibition_Theme, " +
               "g.Gallery_name, g.Gallery_address, g.Rating " +
               "FROM Exhibition e " +
               "JOIN Gallery g ON e.Gallery_id = g.Gallery_id";
    }

    private int nextId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Exhibition_id), 0) + 1 FROM Exhibition")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int galleryIdByName(Connection conn, String galleryName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT Gallery_id FROM Gallery WHERE Gallery_name=?")) {
            ps.setString(1, galleryName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Gallery not found: " + galleryName);
    }

    @Override
    public List<Exhibition> findAll() {
        List<Exhibition> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(baseQuery());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll exhibitions failed", e);
        }
        return result;
    }

    @Override
    public List<Exhibition> findByGalleryName(String galleryName) {
        List<Exhibition> result = new ArrayList<>();
        String sql = baseQuery() + " WHERE g.Gallery_name=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, galleryName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByGalleryName exhibitions failed", e);
        }
        return result;
    }

    @Override
    public void save(Exhibition exhibition) {
        String sql = "INSERT INTO Exhibition(Exhibition_id, Exhibition_Title, Exhibition_Date, Exhibition_Theme, Gallery_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nextId(conn));
            ps.setString(2, exhibition.getTitle());
            ps.setDate(3, (exhibition.getStartDate() != null) ? Date.valueOf(exhibition.getStartDate()) : null);
            ps.setString(4, exhibition.getTheme());
            String galleryName = (exhibition.getGallery() != null) ? exhibition.getGallery().getName() : null;
            ps.setInt(5, galleryIdByName(conn, galleryName));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save exhibition failed", e);
        }
    }

    @Override
    public void update(Exhibition exhibition) {
        String sql = "UPDATE Exhibition SET Exhibition_Date=?, Exhibition_Theme=?, Gallery_id=? WHERE Exhibition_Title=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, (exhibition.getStartDate() != null) ? Date.valueOf(exhibition.getStartDate()) : null);
            ps.setString(2, exhibition.getTheme());
            String galleryName = (exhibition.getGallery() != null) ? exhibition.getGallery().getName() : null;
            ps.setInt(3, galleryIdByName(conn, galleryName));
            ps.setString(4, exhibition.getTitle());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update exhibition failed", e);
        }
    }

    @Override
    public void delete(String title) {
        String sqlFeatures = "DELETE FROM Features WHERE Exhibition_id = (SELECT Exhibition_id FROM Exhibition WHERE Exhibition_Title=?)";
        String sqlExhibition = "DELETE FROM Exhibition WHERE Exhibition_Title=?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlFeatures)) {
                    ps.setString(1, title);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlExhibition)) {
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
            throw new RuntimeException("delete exhibition failed", e);
        }
    }
}
