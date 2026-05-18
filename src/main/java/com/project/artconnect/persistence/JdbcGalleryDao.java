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

    @Override
    public Optional<Gallery> findById(Long id) {
        String sql = "SELECT Gallery_id, Gallery_name, Gallery_address, Rating FROM Gallery WHERE Gallery_id=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
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
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll galleries failed", e);
        }
        return result;
    }
}
