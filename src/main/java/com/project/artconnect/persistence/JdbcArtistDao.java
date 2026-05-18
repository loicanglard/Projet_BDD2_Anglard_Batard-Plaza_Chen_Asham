package com.project.artconnect.persistence;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcArtistDao implements ArtistDao {

    private Artist mapRow(ResultSet rs) throws SQLException {
        Artist a = new Artist();
        a.setName(rs.getString("Artist_name"));
        a.setContactEmail(rs.getString("Artist_email"));
        a.setCity(rs.getString("Artist_city"));
        Date birthDate = rs.getDate("Artist_Birth_Year");
        if (birthDate != null) {
            a.setBirthYear(birthDate.toLocalDate().getYear());
        }
        String discipline = rs.getString("Artist_Discipline");
        if (discipline != null && !discipline.isEmpty()) {
            a.getDisciplines().add(new Discipline(discipline));
        }
        a.setActive(true);
        return a;
    }

    private int nextId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Artist_id), 0) + 1 FROM Artist")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Override
    public List<Artist> findAll() {
        List<Artist> result = new ArrayList<>();
        String sql = "SELECT Artist_id, Artist_name, Artist_Birth_Year, Artist_Discipline, Artist_email, Artist_city FROM Artist";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findAll artists failed", e);
        }
        return result;
    }

    @Override
    public void save(Artist artist) {
        String sql = "INSERT INTO Artist(Artist_id, Artist_name, Artist_Birth_Year, Artist_Discipline, Artist_email, Artist_city) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int id = nextId(conn);
            ps.setInt(1, id);
            ps.setString(2, artist.getName());
            int birthYear = (artist.getBirthYear() != null) ? artist.getBirthYear() : 1900;
            ps.setDate(3, Date.valueOf(birthYear + "-01-01"));
            String discipline = artist.getDisciplines().isEmpty() ? "" : artist.getDisciplines().get(0).getName();
            ps.setString(4, discipline);
            ps.setString(5, artist.getContactEmail());
            ps.setString(6, artist.getCity());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save artist failed", e);
        }
    }

    @Override
    public void update(Artist artist) {
        String sql = "UPDATE Artist SET Artist_Birth_Year=?, Artist_Discipline=?, Artist_email=?, Artist_city=? WHERE Artist_name=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int birthYear = (artist.getBirthYear() != null) ? artist.getBirthYear() : 1900;
            ps.setDate(1, Date.valueOf(birthYear + "-01-01"));
            String discipline = artist.getDisciplines().isEmpty() ? "" : artist.getDisciplines().get(0).getName();
            ps.setString(2, discipline);
            ps.setString(3, artist.getContactEmail());
            ps.setString(4, artist.getCity());
            ps.setString(5, artist.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update artist failed", e);
        }
    }

    @Override
    public void delete(String artistName) {
        String sql = "DELETE FROM Artist WHERE Artist_name=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, artistName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete artist failed", e);
        }
    }

    @Override
    public List<Artist> findByCity(String city) {
        List<Artist> result = new ArrayList<>();
        String sql = "SELECT Artist_id, Artist_name, Artist_Birth_Year, Artist_Discipline, Artist_email, Artist_city FROM Artist WHERE Artist_city=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, city);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByCity artists failed", e);
        }
        return result;
    }
}
