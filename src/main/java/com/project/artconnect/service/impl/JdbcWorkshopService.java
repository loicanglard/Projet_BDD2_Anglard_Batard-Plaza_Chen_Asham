package com.project.artconnect.service.impl;

import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JdbcWorkshopService implements WorkshopService {

    private final WorkshopDao workshopDao;

    public JdbcWorkshopService(WorkshopDao workshopDao) {
        this.workshopDao = workshopDao;
    }

    @Override
    public List<Workshop> getAllWorkshops() {
        return workshopDao.findAll();
    }

    @Override
    public Optional<Workshop> getWorkshopByTitle(String title) {
        return workshopDao.findAll().stream()
                .filter(w -> w.getTitle().equals(title))
                .findFirst();
    }

    @Override
    public void bookWorkshop(Workshop workshop, CommunityMember member) {
        if (workshop == null || member == null) return;

        String sqlMemberId   = "SELECT Member_id FROM Member_ WHERE Member_name=?";
        String sqlWorkshopId = "SELECT Workshop_id FROM Workshop WHERE Workshop_Title=?";
        String sqlInsert     = "INSERT IGNORE INTO Participates(Member_id, Workshop_id) VALUES (?, ?)";

        try (Connection conn = ConnectionManager.getConnection()) {
            int memberId;
            try (PreparedStatement ps = conn.prepareStatement(sqlMemberId)) {
                ps.setString(1, member.getName());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    memberId = rs.getInt(1);
                }
            }

            int workshopId;
            try (PreparedStatement ps = conn.prepareStatement(sqlWorkshopId)) {
                ps.setString(1, workshop.getTitle());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    workshopId = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                ps.setInt(1, memberId);
                ps.setInt(2, workshopId);
                ps.executeUpdate();
            }

            Booking booking = new Booking(workshop, member);
            member.addBooking(booking);

        } catch (SQLException e) {
            throw new RuntimeException("bookWorkshop failed", e);
        }
    }

    @Override
    public List<Booking> getBookingsByMember(CommunityMember member) {
        if (member == null) return Collections.emptyList();

        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT w.Workshop_Title, w.Workshop_Date, w.Workshop_Price, w.Workshop_Level, " +
                     "a.Artist_name, a.Artist_email, a.Artist_city, a.Artist_Birth_Year, a.Artist_Discipline " +
                     "FROM Participates p " +
                     "JOIN Member_ m ON p.Member_id = m.Member_id " +
                     "JOIN Workshop w ON p.Workshop_id = w.Workshop_id " +
                     "JOIN Artist a ON w.Artist_id = a.Artist_id " +
                     "WHERE m.Member_name=?";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Workshop w = new Workshop();
                    w.setTitle(rs.getString("Workshop_Title"));
                    Timestamp ts = rs.getTimestamp("Workshop_Date");
                    if (ts != null) w.setDate(ts.toLocalDateTime());
                    w.setPrice(rs.getDouble("Workshop_Price"));
                    w.setLevel(rs.getString("Workshop_Level"));

                    Artist instructor = new Artist();
                    instructor.setName(rs.getString("Artist_name"));
                    instructor.setContactEmail(rs.getString("Artist_email"));
                    instructor.setCity(rs.getString("Artist_city"));
                    w.setInstructor(instructor);

                    bookings.add(new Booking(w, member));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getBookingsByMember failed", e);
        }
        return bookings;
    }
}
