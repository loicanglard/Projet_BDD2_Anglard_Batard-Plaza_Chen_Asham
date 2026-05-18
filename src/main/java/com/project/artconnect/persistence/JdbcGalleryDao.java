package com.project.artconnect.persistence;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcGalleryDao implements GalleryDao {

    private Gallery mapRow(ResultSet rs) throws SQLException {
        Gallery g = new Gallery();
        g.setName(rs.getString("Gallery_name"));
        g.setAddress(rs.getString("Gallery_address"));
        g.setRating(rs.getInt("Rating"));
        return g;
    }

    private int nextId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Gallery_id), 0) + 1 FROM Gallery")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Override
    public Optional<Gallery> findById(Long id) {
        String sql = "SELECT Gallery_id, Gallery_name, Gallery_address, Rating FROM Gallery WHERE Gallery_id=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById gallery failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Gallery> findAll() {
        List<Gallery> result = new ArrayList<>();
        String sql = "SELECT Gallery_id, Gallery_name, Gallery_address, Rating FROM Gallery";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll galleries failed", e);
        }
        return result;
    }

    @Override
    public void save(Gallery gallery) {
        String sql = "INSERT INTO Gallery(Gallery_id, Gallery_name, Gallery_address, Rating) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nextId(conn));
            ps.setString(2, gallery.getName());
            ps.setString(3, gallery.getAddress());
            ps.setDouble(4, gallery.getRating());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save gallery failed", e);
        }
    }

    @Override
    public void update(Gallery gallery) {
        String sql = "UPDATE Gallery SET Gallery_address=?, Rating=? WHERE Gallery_name=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, gallery.getAddress());
            ps.setDouble(2, gallery.getRating());
            ps.setString(3, gallery.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update gallery failed", e);
        }
    }

    @Override
    public void delete(String name) {
        // Delete exhibitions first (FK constraint)
        String sqlExhib = "DELETE FROM Exhibition WHERE Gallery_id = (SELECT Gallery_id FROM Gallery WHERE Gallery_name=?)";
        String sqlGallery = "DELETE FROM Gallery WHERE Gallery_name=?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlExhib)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlGallery)) {
                    ps.setString(1, name);
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
            throw new RuntimeException("delete gallery failed", e);
        }
    }
}
