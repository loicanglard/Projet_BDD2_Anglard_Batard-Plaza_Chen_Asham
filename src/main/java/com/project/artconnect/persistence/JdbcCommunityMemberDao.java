package com.project.artconnect.persistence;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCommunityMemberDao implements CommunityMemberDao {

    private CommunityMember mapRow(ResultSet rs) throws SQLException {
        CommunityMember m = new CommunityMember();
        m.setName(rs.getString("Member_name"));
        m.setEmail(rs.getString("Member_email"));
        m.setCity(rs.getString("Member_City"));
        return m;
    }

    private int nextId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(Member_id), 0) + 1 FROM Member_")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Override
    public Optional<CommunityMember> findById(Long id) {
        String sql = "SELECT Member_id, Member_name, Member_email, Member_City FROM Member_ WHERE Member_id=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById member failed", e);
        }
        return Optional.empty();
    }

    @Override
    public List<CommunityMember> findAll() {
        List<CommunityMember> result = new ArrayList<>();
        String sql = "SELECT Member_id, Member_name, Member_email, Member_City FROM Member_";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("findAll members failed", e);
        }
        return result;
    }

    @Override
    public void save(CommunityMember member) {
        String sql = "INSERT INTO Member_(Member_id, Member_name, Member_email, Member_City) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nextId(conn));
            ps.setString(2, member.getName());
            ps.setString(3, member.getEmail());
            ps.setString(4, member.getCity());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("save member failed", e);
        }
    }

    @Override
    public void update(CommunityMember member) {
        String sql = "UPDATE Member_ SET Member_email=?, Member_City=? WHERE Member_name=?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getEmail());
            ps.setString(2, member.getCity());
            ps.setString(3, member.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update member failed", e);
        }
    }

    @Override
    public void delete(String name) {
        // Remove participations first
        String sqlParticipates = "DELETE FROM Participates WHERE Member_id = (SELECT Member_id FROM Member_ WHERE Member_name=?)";
        String sqlMember = "DELETE FROM Member_ WHERE Member_name=?";
        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlParticipates)) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlMember)) {
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
            throw new RuntimeException("delete member failed", e);
        }
    }
}
