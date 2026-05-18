package com.project.artconnect.persistence;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcWorkshopDao implements WorkshopDao {

    private Workshop mapRow(ResultSet rs) throws SQLException {
        Workshop w = new Workshop();
        w.setTitle(rs.getString("Workshop_Title"));
        Timestamp ts = rs.getTimestamp("Workshop_Date");
        if (ts != null) w.setDate(ts.toLocalDateTime());
        w.setPrice(rs.getDouble("Workshop_Price"));
        w.setLevel(rs.getString("Workshop_Level"));

        String artistName = rs.getString("Artist_name");
        if (artistName != null) {
            Artist instructor = new Artist();
            instructor.setName(artistName);
            try {
                instructor.setContactEmail(rs.getString("Artist_email"));
                instructor.setCity(rs.getString("Artist_city"));
                Date birthDate = rs.getDate("Artist_Birth_Year");
                if (birthDate != null) instructor.setBirthYear(birthDate.toLocalDate().getYear());
                String discipline = rs.getString("Artist_Discipline");
                if (discipline != null && !discipline.isEmpty())
                    instructor.getDisciplines().add(new Discipline(discipline));
            } catch (SQLException ignored) {}
            w.setInstructor(instructor);
        }
        return w;
    }

    private String baseQuery() {
        return "SELECT w.Workshop_id, w.Workshop_Title, w.Workshop_Date, w.Workshop_Price, w.Workshop_Level, " +
               "a.Artist_name, a.Artist_email, a.Artist_city, a.Artist_Birth_Year, a.Artist_Discipline " +
               "FROM Workshop w JOIN Artist a ON w.Artist_id = a.Artist_id";
    }

    private int nextId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Workshop_id), 0) + 1 FROM Workshop")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int artistIdByName(Connection conn, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT Artist_id FROM Artist WHERE Artist_name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Artist not found: " + name);
    }

    @Override
    public Optional<Workshop> findById(Long id) {
        String sql = baseQuery() + " WHERE w.Workshop_id=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById workshop failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Workshop> findAll() {
        List<Workshop> result = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(baseQuery());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll workshops failed", e);
        }
        return result;
    }

    @Override
    public void save(Workshop workshop) {
        String sql = "INSERT INTO Workshop(Workshop_id, Workshop_Title, Workshop_Date, Workshop_Price, Workshop_Level, Artist_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nextId(conn));
            ps.setString(2, workshop.getTitle());
            ps.setTimestamp(3, workshop.getDate() != null ? Timestamp.valueOf(workshop.getDate()) : null);
            ps.setDouble(4, workshop.getPrice());
            ps.setString(5, workshop.getLevel());
            int artistId = (workshop.getInstructor() != null && workshop.getInstructor().getName() != null)
                    ? artistIdByName(conn, workshop.getInstructor().getName()) : 1;
            ps.setInt(6, artistId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save workshop failed", e);
        }
    }

    @Override
    public void update(Workshop workshop) {
        String sql = "UPDATE Workshop SET Workshop_Date=?, Workshop_Price=?, Workshop_Level=? WHERE Workshop_Title=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, workshop.getDate() != null ? Timestamp.valueOf(workshop.getDate()) : null);
            ps.setDouble(2, workshop.getPrice());
            ps.setString(3, workshop.getLevel());
            ps.setString(4, workshop.getTitle());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update workshop failed", e);
        }
    }

    @Override
    public void delete(String title) {
        String sqlParticipates = "DELETE FROM Participates WHERE Workshop_id = (SELECT Workshop_id FROM Workshop WHERE Workshop_Title=?)";
        String sqlWorkshop = "DELETE FROM Workshop WHERE Workshop_Title=?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlParticipates)) {
                    ps.setString(1, title);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlWorkshop)) {
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
            throw new RuntimeException("delete workshop failed", e);
        }
    }
}
