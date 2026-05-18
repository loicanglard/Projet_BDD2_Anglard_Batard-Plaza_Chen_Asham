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
        if (ts != null) {
            w.setDate(ts.toLocalDateTime());
        }
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
                if (birthDate != null) {
                    instructor.setBirthYear(birthDate.toLocalDate().getYear());
                }
                String discipline = rs.getString("Artist_Discipline");
                if (discipline != null && !discipline.isEmpty()) {
                    instructor.getDisciplines().add(new Discipline(discipline));
                }
            } catch (SQLException ignored) {
            }
            w.setInstructor(instructor);
        }
        return w;
    }

    private String baseQuery() {
        return "SELECT w.Workshop_id, w.Workshop_Title, w.Workshop_Date, w.Workshop_Price, w.Workshop_Level, " +
               "a.Artist_name, a.Artist_email, a.Artist_city, a.Artist_Birth_Year, a.Artist_Discipline " +
               "FROM Workshop w " +
               "JOIN Artist a ON w.Artist_id = a.Artist_id";
    }

    @Override
    public Optional<Workshop> findById(Long id) {
        String sql = baseQuery() + " WHERE w.Workshop_id=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById workshop failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Workshop> findAll() {
        List<Workshop> result = new ArrayList<>();
        String sql = baseQuery();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll workshops failed", e);
        }
        return result;
    }
}
